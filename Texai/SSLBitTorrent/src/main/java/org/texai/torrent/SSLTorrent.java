/*
 * SSLTorrent.java
 *
 * Created on Jan 31, 2010, 8:06:37 PM
 *
 * Description: Provides a multiplexed bit torrent framework, in which peers exchange pieces of multiple files
 * using Secure Socket Layer (SSL) over HTTP protocol, with a single shared server socket that handles incomming
 * peer connections.
 *
 * Copyright (C) Jan 31, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.torrent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.torrent.domainEntity.MetaInfo;
import org.texai.util.ByteUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

/** Provides a multiplexed bit torrent framework, in which peers exchange pieces of multiple files
 * using Secure Socket Layer (SSL) over HTTP protocol, with a single shared Netty socket that handles
 * incomming peer connections.
 *
 * @author reed
 */
@NotThreadSafe
public final class SSLTorrent implements DownloadListener {

  /** the logger */
  private final static Logger LOGGER = Logger.getLogger(SSLTorrent.class);
  /** the peer coordinator dictionary, info hash hex --> peer coordinator */
  private final Map<String, PeerCoordinator> peerCoordinatorDictionary = new HashMap<>();
  /** the peers dictionary, tracked peer information --> peer */
  private final Map<TrackedPeerInfo, Peer> peersDictionary = new HashMap<>();
  /** our tracked peer information */
  private TrackedPeerInfo trackedPeerInfo;
  /** the port for accepting peer connections */
  private final int ourConnectionPort;
  /** the tracker client that obtains information on new peers. */
  private TrackerClient trackerClient;
  /** the download listener, or null if seeding */
  private DownloadListener downloadListener;
  /** the X.509 security information */
  final X509SecurityInfo x509SecurityInfo;

  /** Constructs a new SSLTorrent instance.
   *
   * @param ourConnectionPort the port for accepting peer connections
   * @param x509SecurityInfo X.509 security information
   */
  public SSLTorrent(
          final int ourConnectionPort,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert ourConnectionPort > 0 : "ourConnectionPort must be positive";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    this.ourConnectionPort = ourConnectionPort;
    this.x509SecurityInfo = x509SecurityInfo;
  }

  /** Returns whether this SSLTorrent is seeding the file or directory specified by the given meta info.
   *
   * @param metaInfo the given meta info
   * @return whether this SSLTorrent is seeding the file or directory
   */
  public boolean isSeeding(final MetaInfo metaInfo) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";

    final PeerCoordinator peerCoordinator;
    synchronized (peerCoordinatorDictionary) {
      peerCoordinator = peerCoordinatorDictionary.get(ByteUtils.toHex(metaInfo.getInfoHash()));
    }
    if (peerCoordinator == null) {
      return false;
    } else {
      return peerCoordinator.isCompleted();
    }
  }

  /** Returns whether this SSLTorrent is downloading the file or directory specified by the given meta info.
   *
   * @param metaInfo the given meta info
   * @return whether this SSLTorrent is downloading the file or directory
   */
  public boolean isDownloading(final MetaInfo metaInfo) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";

    final PeerCoordinator peerCoordinator;
    synchronized (peerCoordinatorDictionary) {
      peerCoordinator = peerCoordinatorDictionary.get(ByteUtils.toHex(metaInfo.getInfoHash()));
    }
    if (peerCoordinator == null) {
      return false;
    } else {
      return !peerCoordinator.isCompleted();
    }
  }

  /** Begins seeding, i.e. sharing, the file or directory that is described by the given meta info.
   *
   * @param metaInfo the given meta info
   * @param storage the torrent file/directory storage
   */
  public void beginSeeding(final MetaInfo metaInfo, final Storage storage) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    assert storage != null : "storage must not be null";
    if (isSeeding(metaInfo)) {
      LOGGER.info("ignoring duplicate seed");
      return;
    }
    if (isDownloading(metaInfo)) {
      LOGGER.info("ignoring seeding when downloading the same file");
      return;
    }

    LOGGER.info("begin seeding: " + metaInfo.getName());
    final PeerCoordinator peerCoordinator = new PeerCoordinator(
            metaInfo,
            storage,
            ourConnectionPort,
            x509SecurityInfo,
            this, // downloadListener
            this); // sslTorrent
    synchronized (peerCoordinatorDictionary) {
      peerCoordinatorDictionary.put(ByteUtils.toHex(metaInfo.getInfoHash()), peerCoordinator);
    }
    trackerClient = new TrackerClient(
            metaInfo,
            peerCoordinator,
            ourConnectionPort);
    final Thread trackerClientThread = new Thread(trackerClient);
    trackerClientThread.start();
    peerCoordinator.setTrackerClient(trackerClient);

    //Postconditions
    assert isSeeding(metaInfo);
  }

  /** Ends seeding, i.e. sharing, the file or directory that is described by the given meta info.
   *
   * @param metaInfo the given meta info
   */
  public void endSeeding(final MetaInfo metaInfo) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    assert isSeeding(metaInfo) : "not seeding: " + metaInfo.getName();

    final String infoHashHex = ByteUtils.toHex(metaInfo.getInfoHash());
    final PeerCoordinator peerCoordinator;
    synchronized (peerCoordinatorDictionary) {
      peerCoordinator = peerCoordinatorDictionary.get(infoHashHex);
    }
    peerCoordinator.quit();
    peerCoordinatorDictionary.remove(infoHashHex);
  }

  /** Downloads, the file or directory that is described by the given meta info.
   *
   * @param metaInfo the given meta info
   * @param downloadDirectoryPath the download directory
   * @param downloadListener the download listener, which is notified when the download completes
   */
  public void download(
          final MetaInfo metaInfo,
          final String downloadDirectoryPath,
          final DownloadListener downloadListener) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    if (isDownloading(metaInfo)) {
      LOGGER.info("ignoring duplicate download");
      return;
    }
    if (isSeeding(metaInfo)) {
      LOGGER.info("ignoring download when seeding the same file");
      return;
    }

    this.downloadListener = downloadListener;
    LOGGER.info("downloading: " + metaInfo.getName());
    final Storage storage;
    try {
      storage = new Storage(metaInfo, downloadDirectoryPath);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    final PeerCoordinator peerCoordinator = new PeerCoordinator(
            metaInfo,
            storage,
            ourConnectionPort,
            x509SecurityInfo,
            this, // downloadListener
            this); // sslTorrent
    synchronized (peerCoordinatorDictionary) {
      peerCoordinatorDictionary.put(ByteUtils.toHex(metaInfo.getInfoHash()), peerCoordinator);
    }
    trackerClient = new TrackerClient(
            metaInfo,
            peerCoordinator,
            ourConnectionPort);
    final Thread trackerClientThread = new Thread(trackerClient);
    trackerClientThread.start();
    peerCoordinator.setTrackerClient(trackerClient);

  }

  /** Gets our tracked peer information.
   *
   * @return our tracked peer information
   */
  public TrackedPeerInfo getOurTrackedPeerInfo() {
    return trackedPeerInfo;
  }

  /** Gets the peers dictionary, tracked peer information --> peer.
   *
   * @return the peers dictionary
   */
  public Map<TrackedPeerInfo, Peer> getPeersDictionary() {
    return peersDictionary;
  }

  /** Receives notification that the associated download has completed.
   *
   * @param metaInfo the meta info
   */
  @Override
  public void downloadCompleted(final MetaInfo metaInfo) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";

    final PeerCoordinator peerCoordinator;
    synchronized (peerCoordinatorDictionary) {
      peerCoordinator = peerCoordinatorDictionary.get(ByteUtils.toHex(metaInfo.getInfoHash()));
    }
    peerCoordinator.quit();
    if (downloadListener != null) {
      downloadListener.downloadCompleted(metaInfo);
    }
  }

  /** Adds a peer coordinator.
   *
   * @param infoHash
   * @param peerCoordinator
   */
  public void addPeerCoordinator(
          final byte[] infoHash,
          final PeerCoordinator peerCoordinator) {
    //Preconditions
    assert infoHash != null : "infoHash must not be null";
    assert peerCoordinator != null : "peerCoordinator must not be null";

    synchronized (peerCoordinatorDictionary) {
      peerCoordinatorDictionary.put(ByteUtils.toHex(infoHash), peerCoordinator);
    }
  }

  /** Gets the peer coordinator for the given info hash, that identifies the torrent.
   *
   * @param infoHash the given info hash that identifies the torrent
   * @return the peer coordinator for the given info hash
   */
  public PeerCoordinator getPeerCoordinator(final byte[] infoHash) {
    //Preconditions
    assert infoHash != null : "infoHash must not be null";

    synchronized (peerCoordinatorDictionary) {
      return peerCoordinatorDictionary.get(ByteUtils.toHex(infoHash));
    }
  }
}
