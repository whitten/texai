/*
 * PeerCoordinator - Coordinates which peers do what (up and downloading).
 * Copyright (C) 2003 Mark J. Wielaard
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */
package org.texai.torrent;

import org.texai.torrent.domainEntity.MetaInfo;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TreeMap;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.texai.torrent.message.BitTorrentHandshakeMessage;
import org.texai.util.ByteUtils;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

/** Coordinates which peers perform uploading or downloading actions. */
public final class PeerCoordinator {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PeerCoordinator.class);
  /** delay in milliseconds before the peer checker task is to be executed */
  public static final long CHECK_PERIOD = 20 * 1000; // 20 seconds
  /** the maximum number of connections */
  public static final int MAX_CONNECTIONS = 24;
  /** the maximum number of uploaders */
  public static final int MAX_UPLOADERS = 4;
  /** the torrent metainfo */
  private final MetaInfo metaInfo;
  /** the storage */
  private final Storage storage;
  /** an approximation of the number of current uploaders that is re-synced by PeerChecker once in a while */
  private int uploaders = 0;
  /** the total number of uploaded bytes of all peers */
  private long nbrBytesUploaded;
  /** the total number of downloaded bytes of all peers */
  private long nbrBytesDownloaded;
  /** the peer dictionary, peer identification bytes hex --> peer */
  private final Map<String, Peer> peerDictionary = new HashMap<>();
  /** the peer checker timer */
  private final Timer peerCheckerTimer = new Timer("peer checker timer", true);
  /** our BitTorrent client id bytes, randomly assigned */
  private final byte[] ourIdBytes = new byte[20];
  /** randomized wanted piece sequence numbers */
  private final List<Integer> wantedPieces;
  /** the indicator whether this peer is halted */
  private boolean isQuit = false;
  /** the tracker client */
  private TrackerClient trackerClient;
  /** the download statistics dictionary, tracked peer info --> nbr downloaded bytes */
  private final Map<TrackedPeerInfo, Long> downloadStatisticsDictionary = new HashMap<>();
  /** the upload statistics dictionary, tracked peer info --> nbr uploaded bytes */
  private final Map<TrackedPeerInfo, Long> uploadStatisticsDictionary = new HashMap<>();
  /** our tracked peer information */
  private final TrackedPeerInfo ourTrackedPeerInfo;
  /** the download listener, or null if our peer is a seed */
  private final DownloadListener downloadListener;
  /** the X.509 security information */
  private final X509SecurityInfo x509SecurityInfo;
  /** the SSL torrent */
  private final SSLTorrent sslTorrent;
  /** the client id candidate symbols */
  private static final byte[] CANDIDATE_ID_SYMBOLS = new byte[62];

  static {
    for (int i = 0; i < 10; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('0' + i);
    }
    for (int i = 10; i < 36; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('a' + i - 10);
    }
    for (int i = 36; i < 62; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('A' + i - 36);
    }
  }

  /** Constructs a new PeerCoordinator instance.
   *
   * @param metaInfo the torrent metainfo
   * @param storage the torrent file/directory storage
   * @param port the port for accepting peer connections
   * @param x509SecurityInfo the X.509 security information
   * @param downloadListener the download listener, or null if our peer is a seed
   * @param sslTorrent the SSL torrent
   */
  public PeerCoordinator(
          final MetaInfo metaInfo,
          final Storage storage,
          final int port,
          final X509SecurityInfo x509SecurityInfo,
          final DownloadListener downloadListener,
          final SSLTorrent sslTorrent) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    assert storage != null : "storage must not be null";
    assert port > 0 : "port must be positive";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
    assert sslTorrent != null : "sslTorrent must not be null";

    this.metaInfo = metaInfo;
    this.storage = storage;
    this.x509SecurityInfo = x509SecurityInfo;
    this.downloadListener = downloadListener;
    this.sslTorrent = sslTorrent;

    // create our peer id bytes
    int index = 0;
    ourIdBytes[index++] = '-';
    ourIdBytes[index++] = 'S';
    ourIdBytes[index++] = 'N';
    ourIdBytes[index++] = '1';
    ourIdBytes[index++] = '0';
    ourIdBytes[index++] = '0';
    ourIdBytes[index++] = '0';
    ourIdBytes[index++] = '-';
    final Random random = new Random();
    while (index < 20) {
      ourIdBytes[index++] = CANDIDATE_ID_SYMBOLS[random.nextInt(CANDIDATE_ID_SYMBOLS.length)];
    }
    ourTrackedPeerInfo = new TrackedPeerInfo(
            ourIdBytes,
            NetworkUtils.getLocalHostAddress(),
            port);
    try {
      LOGGER.info(this + " our peer id: " + new String(ourIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      ex.printStackTrace();
    }
    LOGGER.log(Level.DEBUG, "our peer tracked peer info hex: " + TrackedPeerInfo.hexEncode(ourIdBytes));

    // Make a random list of piece numbers.
    wantedPieces = new ArrayList<>();
    final BitField bitfield = storage.getBitField();
    for (int i = 0; i < metaInfo.getNbrPieces(); i++) {
      if (!bitfield.get(i)) {
        wantedPieces.add(i);
      }
    }
    //Collections.shuffle(wantedPieces);
    LOGGER.info(this + " indices of wanted pieces " + wantedPieces);

    // Install a timer to check the uploaders.
    peerCheckerTimer.schedule(
            new PeerCheckerTask(this),
            CHECK_PERIOD,
            CHECK_PERIOD);
    sslTorrent.addPeerCoordinator(metaInfo.getInfoHash(), this);
    LOGGER.info("created peer coordinator " + this);
  }

  /** Adds a peer that we contact.
   *
   * @param trackedPeerInfo the given peer tracked information
   * @return whether the peer was added
   */
  public boolean addPeerThatWeContact(final TrackedPeerInfo trackedPeerInfo) {
    //Preconditions
    assert trackedPeerInfo != null : "trackedPeerInfo must not be null";

    if (isQuit) {
      return false;
    }
    if (trackedPeerInfo.equals(ourTrackedPeerInfo)) {
      LOGGER.info("not adding our own peer listed at the tracker");
      return false;
    }

    for (final Peer peer : peerDictionary.values()) {
      if (trackedPeerInfo.equals(peer.getTrackedPeerInfo()) && peer.isConnected()) {
        LOGGER.info("already connected to " + peer);
        return false;
      }
    }
    final boolean areMorePeersNeeded;
    synchronized (peerDictionary) {
      areMorePeersNeeded = peerDictionary.size() < MAX_CONNECTIONS;
    }

    if (areMorePeersNeeded) {
      final Peer peer = new Peer(trackedPeerInfo, this);
      final String peerIdBytesHex = ByteUtils.toHex(peer.getTrackedPeerInfo().getPeerIdBytes());
      synchronized (peerDictionary) {
        peerDictionary.put(peerIdBytesHex, peer);
      }
      LOGGER.info("=========================================================================================================");
      try {
        LOGGER.info(this + " added peer to contact: " + peer + " " + new String(peer.getTrackedPeerInfo().getPeerIdBytes(), "US-ASCII"));
      } catch (UnsupportedEncodingException ex) {
        throw new TexaiException(ex);
      }
      LOGGER.info("=========================================================================================================");
      peer.sendHandshake();

      return true;
    } else {
      LOGGER.info("MAX_CONNECTIONS = " + MAX_CONNECTIONS + " not accepting extra peer: " + trackedPeerInfo);
      return false;
    }
  }

  /** Adds a peer that contacted us.
   *
   * @param inetAddress the peer's IP address
   * @param port the peer's port
   * @param channel the communication channel between us and the peer
   * @param bitTorrentHandshakeMessage the remote peer's handshake message
   * @return whether the peer was added
   */
  public boolean addPeerThatContactedUs(
          final InetAddress inetAddress,
          final int port,
          final Channel channel,
          final BitTorrentHandshakeMessage bitTorrentHandshakeMessage) {
    //Preconditions
    assert inetAddress != null : "inetAddress must not be null";
    assert port > 0 : "port must be positive";
    assert channel != null : "channel must not be null";
    assert bitTorrentHandshakeMessage != null : "bitTorrentHandshakeMessage must not be null";

    if (isQuit) {
      return false;
    }

    final TrackedPeerInfo trackedPeerInfo = new TrackedPeerInfo(
            bitTorrentHandshakeMessage.getPeerIdBytes(),
            inetAddress,
            port);
    if (trackedPeerInfo.equals(ourTrackedPeerInfo)) {
      LOGGER.info("not adding our own peer listed at the tracker");
      return false;
    }

    for (final Peer peer : peerDictionary.values()) {
      if (trackedPeerInfo.equals(peer.getTrackedPeerInfo()) && peer.isConnected()) {
        LOGGER.info("already connected to " + peer);
        return false;
      }
    }
    final boolean areMorePeersNeeded;
    synchronized (peerDictionary) {
      areMorePeersNeeded = peerDictionary.size() < MAX_CONNECTIONS;
    }

    if (areMorePeersNeeded) {
      final Peer peer = new Peer(
          inetAddress,
          port,
          channel,
          bitTorrentHandshakeMessage,
          this);
      final String peerIdBytesHex = ByteUtils.toHex(peer.getTrackedPeerInfo().getPeerIdBytes());
      synchronized (peerDictionary) {
        peerDictionary.put(peerIdBytesHex, peer);
      }
      LOGGER.info("=========================================================================================================");
      try {
        LOGGER.info(this + " added peer that contacted us: " + peer + " " + new String(peer.getTrackedPeerInfo().getPeerIdBytes(), "US-ASCII"));
      } catch (UnsupportedEncodingException ex) {
        throw new TexaiException(ex);
      }
      LOGGER.info("=========================================================================================================");
      peer.receiveHandshake(bitTorrentHandshakeMessage);
      return true;
    } else {
      LOGGER.info("MAX_CONNECTIONS = " + MAX_CONNECTIONS + " not accepting extra peer: " + trackedPeerInfo);
      return false;
    }
  }

  /** Processes the event in which the connection to the peer was terminated or the connection
   * handshake failed.
   *
   * @param peer the peer that just got disconnected.
   */
  public void peerDisconnectedEvent(final Peer peer) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";

    LOGGER.log(Level.DEBUG, "Disconnected " + peer);

    LOGGER.info("peerDisconnectedEvent started");
    synchronized (peerDictionary) {
      // Make sure it is no longer in our lists
      if (peerDictionary.remove(ByteUtils.toHex(peer.getTrackedPeerInfo().getPeerIdBytes())) != null) {
        // Unchoke some random other peer
        unchokePeer();
      }
    }
    LOGGER.info("peerDisconnectedEvent completed");
  }

  /** Processes the event in which we received a interested or unintrested message from a peer.
   *
   * @param peer the peer that sent the message
   * @param interest true when the peer sent a interested message, false when the peer sent an uninterested message
   */
  public void peerInterestedOrUninterestedEvent(
          final Peer peer,
          final boolean interest) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";

    if (interest) {
      LOGGER.info("peerInterestedOrUninterestedEvent started");
      synchronized (peerDictionary) {
        if (uploaders < MAX_UPLOADERS && peer.isChoking()) {
          uploaders++;
          peer.setChoking(false);
          LOGGER.log(Level.DEBUG, "Unchoke: " + peer);
        }
      }
      LOGGER.info("peerInterestedOrUninterestedEvent completed");
    }
  }

  /** Processes the event in which a complete piece is received from a peer. The piece must be
   * requested by Peer.request() first. If this method returns false that
   * means the Peer provided a corrupted piece and the connection will be
   * closed.
   *
   * @param peer the peer that sent the piece.
   * @param pieceIndex the piece index received.
   * @param pieceBuffer the byte array containing the piece.
   *
   * @return true when the bytes represent the piece, false otherwise.
   */
  public boolean peerCompletePieceEvent(
          final Peer peer,
          final int pieceIndex,
          final byte[] pieceBuffer) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert pieceBuffer != null : "pieceBuffer must not be equal to null";

    if (isQuit) {
      return true;
    }
    synchronized (wantedPieces) {
      if (!wantedPieces.contains(pieceIndex)) {
        LOGGER.info(peer + " piece " + pieceIndex + " is no longer needed");

        // No need to announce have piece to peers.
        // Assume we got a good piece, we don't really care anymore.
        return true;
      }
      try {
        if (storage.putPiece(pieceIndex, pieceBuffer)) {
          synchronized (downloadStatisticsDictionary) {
            Long nbrBytesDownloaded1 = downloadStatisticsDictionary.get(peer.getTrackedPeerInfo());
            if (nbrBytesDownloaded1 == null) {
              nbrBytesDownloaded1 = (long) pieceBuffer.length;
              downloadStatisticsDictionary.put(peer.getTrackedPeerInfo(), nbrBytesDownloaded1);
            } else {
              downloadStatisticsDictionary.put(peer.getTrackedPeerInfo(), nbrBytesDownloaded1 + (long) pieceBuffer.length);
            }
          }
          LOGGER.info(this + " received piece " + pieceIndex + " from " + peer);
        } else {
          nbrBytesDownloaded -= metaInfo.getPieceLength(pieceIndex);
          LOGGER.info("got BAD piece " + pieceIndex + " from " + peer);
          return false; // No need to announce BAD piece to peers.
        }
      } catch (final IOException ioe) {
        throw new TexaiException(ioe);
      }
      wantedPieces.remove(Integer.valueOf(pieceIndex));
    }

    // announce that we have it to our peers
    final List<Peer> myPeers = new ArrayList<>();
    LOGGER.debug("peerPieceEvent critical section started");
    synchronized (peerDictionary) {
      myPeers.addAll(peerDictionary.values());
    }
    LOGGER.debug("peerPieceEvent critical section completed");
    final Iterator<Peer> peers_iter = myPeers.iterator();
    while (!isQuit && peers_iter.hasNext()) {
      final Peer peer1 = peers_iter.next();
      if (peer1.isConnected()) {
        peer1.havePiece(pieceIndex);
      }
    }

    if (!isQuit && isCompleted()) {
      trackerClient.quit();
    }
    return true;
  }

  /** Gets the peer having the the given peer identification bytes.
   *
   * @param peerIdBytes the given peer identification bytes
   * @return the peer, or null if not found
   */
  public Peer getPeer(final byte[] peerIdBytes) {
    //Preconditions
    assert peerIdBytes != null : "peerIdBytes must not be null";

    return peerDictionary.get(ByteUtils.toHex(peerIdBytes));
  }

  /** Sets the tracker client.
   *
   * @param trackerClient the tracker client
   */
  public void setTrackerClient(final TrackerClient trackerClient) {
    //Preconditions
    assert trackerClient != null : "trackerClient must not be null";

    this.trackerClient = trackerClient;
  }

  /** Gets our peer identification bytes
   *
   * @return our peer identification bytes
   */
  public byte[] getPeerIdBytes() {
    return ourIdBytes;
  }

  /** Gets our URL encoded id bytes
   *
   * @return our URL encoded id bytes
   */
  public String getURLEncodedID() {
    return new String((new URLCodec()).encode(ourIdBytes));
  }

  /** Returns whether this storage contains all pieces in the MetaInfo.
   *
   * @return whether this storage contains all pieces in the MetaInfo
   */
  public boolean isCompleted() {
    return storage.isComplete();
  }

  /** Returns the number of peers.
   *
   * @return  the number of peers
   */
  public int getNbrPeers() {
    synchronized (peerDictionary) {
      return peerDictionary.size();
    }
  }

  /** Returns how many pieces are still missing.
   *
   * @return how many pieces are still missing
   */
  public int getNbrNeededPieces() {
    return storage.getNbrNeededPieces();
  }

  /** Returns approximately how many bytes are still needed to get the complete file.
   *
   * @return approximately how many bytes are still needed to get the complete file
   */
  public long getApproximateNbrBytesRemaining() {
    return storage.getNbrNeededPieces() * metaInfo.getPieceLength(0);
  }

  /** Returns the total number of uploaded bytes of all peers.
   *
   * @return the total number of uploaded bytes of all peers
   */
  public long getUploaded() {
    return nbrBytesUploaded;
  }

  /** Returns the total number of downloaded bytes of all peers.
   *
   * @return the total number of downloaded bytes of all peers
   */
  public long getDownloaded() {
    return nbrBytesDownloaded;
  }

  /** Returns whether more peers are needed.
   *
   * @return whether more peers are needed
   */
  public boolean areMorePeersNeeded() {
    synchronized (peerDictionary) {
      return !isQuit && peerDictionary.size() < MAX_CONNECTIONS;
    }
  }

  /** Quits the tracker client and peers. */
  public void quit() {
    isQuit = true;
    LOGGER.info("halting tracker client");
    trackerClient.quit();
    LOGGER.info("disconnecting the peers  ********");
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
    synchronized (peerDictionary) {
      LOGGER.info("obtained lock on the peers");

      // Stop peer checker task.
      peerCheckerTimer.cancel();

      // Stop peers.
      final Iterator<Peer> peers_iter = peerDictionary.values().iterator();
      while (peers_iter.hasNext()) {
        final Peer peer = peers_iter.next();
        LOGGER.info("disconnecting " + peer);
        peer.disconnect();
        peers_iter.remove();
      }
      LOGGER.info("peers disconnected  ********");
    }
  }

  /** Get the peers.
   *
   * @return the peers
   */
  public Collection<Peer> getPeers() {
    return peerDictionary.values();
  }

  /** Gets an approximation of the number of current uploaders that is re-synced by PeerChecker once in a while.
   *
   * @return an approximation of the number of current uploaders
   */
  public int getUploaders() {
    return uploaders;
  }

  /** Sets the uploaders.
   *
   * @param uploaders the uploaders to set
   */
  public void setUploaders(final int uploaders) {
    //Preconditions
    assert uploaders >= 0 : "uploaders must not be negative";

    this.uploaders = uploaders;
  }

  /** Reports the download statistics. */
  public void reportDownloadStatistics() {
    final SortedMap<Long, TrackedPeerInfo> sortedDownloadStatisticsDictionary = new TreeMap<>();
    synchronized (downloadStatisticsDictionary) {
      for (final Entry<TrackedPeerInfo, Long> entry : downloadStatisticsDictionary.entrySet()) {
        sortedDownloadStatisticsDictionary.put(entry.getValue(), entry.getKey());
      }
    }
    LOGGER.info("number bytes downloaded from peers ...");
    if (sortedDownloadStatisticsDictionary.isEmpty()) {
      LOGGER.info("  none");
    } else {
      for (final Entry<Long, TrackedPeerInfo> entry : sortedDownloadStatisticsDictionary.entrySet()) {
        LOGGER.info("  " + entry.getValue() + "  " + entry.getKey());
      }
    }
  }

  /** Reports the upload statistics. */
  public void reportUploadStatistics() {
    final SortedMap<Long, TrackedPeerInfo> sortedUploadStatisticsDictionary = new TreeMap<>();
    synchronized (uploadStatisticsDictionary) {
      for (final Entry<TrackedPeerInfo, Long> entry : uploadStatisticsDictionary.entrySet()) {
        sortedUploadStatisticsDictionary.put(entry.getValue(), entry.getKey());
      }
    }
    LOGGER.info("number bytes uploaded to peers ...");
    if (sortedUploadStatisticsDictionary.isEmpty()) {
      LOGGER.info("  none");
    } else {
      for (final Entry<Long, TrackedPeerInfo> entry : sortedUploadStatisticsDictionary.entrySet()) {
        LOGGER.info("  " + entry.getValue() + "  " + entry.getKey());
      }
    }
  }

  /** Optimistically unchokes the peers. */
  public synchronized void unchokePeer() {
    // linked list will contain all interested peers that we choke.
    // At the start are the peers that have us unchoked and at the end the
    // other peers that are interested, but are choking us.
    final List<Peer> interested = new LinkedList<>();
    final Iterator<Peer> peers_iter = peerDictionary.values().iterator();
    while (peers_iter.hasNext()) {
      final Peer peer = peers_iter.next();
      if (uploaders < MAX_UPLOADERS && peer.isChoking() && peer.isInterested()) {
        if (peer.isChoked()) {
          interested.add(peer);
        } else {
          interested.add(0, peer);
        }
      }
    }

    while (uploaders < MAX_UPLOADERS && !interested.isEmpty()) {
      final Peer peer = interested.remove(0);
      peer.setChoking(false);
      uploaders++;
    }
  }

  /** Gets the bit map.
   *
   * @return the bit map
   */
  public byte[] getBitMap() {
    return storage.getBitField().getFieldBytes();
  }

  /** Processes the event in which a peer sent a have piece message. If this method returns true
   * and the peer has not yet received a interested message or we indicated
   * earlier to be not interested, then an interested message will be sent.
   *
   * @param peer the peer that sent the message
   * @param piece the piece number that the peer just got
   *
   * @return true when it is a piece that we want, false if the piece is already here.
   */
  public boolean peerHavePieceEvent(
          final Peer peer,
          final int piece) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert piece >= 0 : "piece must not be negative";

    synchronized (wantedPieces) {
      return wantedPieces.contains(piece);
    }
  }

  /** Processes the event in which we received a bitfield message from a peer. If this method returns true an
   * interested message will be send back to the peer.
   *
   * @param peer the peer that sent the message
   * @param bitField a BitField containing the pieces that the other side has
   *
   * @return true when the BitField contains pieces we want, false if the piece is already known
   */
  public boolean peerBitFieldEvent(
          final Peer peer,
          final BitField bitField) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert bitField != null : "bitField must not be equal to null";

    synchronized (wantedPieces) {
      final Iterator<Integer> wantedPieces_iter = wantedPieces.iterator();
      while (wantedPieces_iter.hasNext()) {
        final int wantedPiece = wantedPieces_iter.next();
        if (bitField.get(wantedPiece)) {
          LOGGER.info(this + " we want piece: " + wantedPiece + " from " + peer);
          return true;
        }
      }
    }
    LOGGER.info(this + " no wanted pieces from " + peer);
    return false;
  }

  /** Processes the event in which we are downloading from the peer and need to ask for a new
   * piece. Might be called multiple times before <code>gotPiece()</code> is
   * called.
   *
   * @param peer the Peer that will be asked to provide the piece
   * @param bitfield a BitField containing the pieces that the other side has
   *
   * @return one of the pieces from the bitfield that we want or -1 if we are no longer interested in the peer
   */
  public int getWantedPiece(
          final Peer peer,
          final BitField bitfield) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert bitfield != null : "bitfield must not be equal to null";

    if (isQuit) {
      return -1;
    }

    synchronized (wantedPieces) {
      Integer piece = null;
      final Iterator<Integer> wantedPieces_iter = wantedPieces.iterator();
      while (piece == null && wantedPieces_iter.hasNext()) {
        final Integer wantedPiece = wantedPieces_iter.next();
        if (bitfield.get(wantedPiece)) {
          wantedPieces_iter.remove();
          piece = wantedPiece;
        }
      }

      if (piece == null) {
        return -1;
      }

      // We add it back at the back of the list. It will be removed
      // if gotPiece is called later. This means that the last
      // couple of pieces might very well be asked from multiple
      // peers but that is OK.
      wantedPieces.add(piece);

      return piece;
    }
  }

  /** Processes the event in which the peer wants (part of) a piece from us. Only called when
   * the peer is not choked by us (<code>peer.choke(false)</code> was
   * called).
   *
   * @param peer the peer that wants the piece.
   * @param piece the piece number requested.
   * @return a byte array containing the piece or null when the piece is not available (which is a protocol error).
   * @throws IOException when an input/output error occurs
   */
  public byte[] getPieceBytes(
          final Peer peer,
          final int piece)
          throws IOException {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert piece >= 0 : "piece must not be negative";

    if (isQuit) {
      return null;
    }

    try {
      return storage.getPiece(piece);
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Processes the event in which a (partial) piece has been uploaded to the peer.
   *
   * @param peer the peer to which size bytes were uploaded
   * @param size the number of bytes that were uploaded
   */
  public void uploaded(
          final Peer peer,
          final int size) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert size >= 0 : "size must not be negative";

    nbrBytesUploaded += size;
    synchronized (uploadStatisticsDictionary) {
      Long nbrBytesUploaded1 = uploadStatisticsDictionary.get(peer.getTrackedPeerInfo());
      if (nbrBytesUploaded1 == null) {
        nbrBytesUploaded1 = (long) size;
        uploadStatisticsDictionary.put(peer.getTrackedPeerInfo(), nbrBytesUploaded1);
      } else {
        uploadStatisticsDictionary.put(peer.getTrackedPeerInfo(), nbrBytesUploaded1 + (long) size);
      }
    }
  }

  /** Processes the event in which a (partial) piece has been downloaded from the peer.
   *
   * @param peer the Peer from which size bytes were downloaded.
   * @param size the number of bytes that were downloaded.
   */
  public void downloaded(
          final Peer peer,
          final int size) {
    //Preconditions
    assert peer != null : "peer must not be equal to null";
    assert size >= 0 : "size must not be negative";

    nbrBytesDownloaded += size;
  }

  /** Gets the torrent metainfo.
   *
   * @return the torrent metainfo
   */
  public MetaInfo getMetaInfo() {
    return metaInfo;
  }

  /** Gets the storage.
   *
   * @return the storage
   */
  public Storage getStorage() {
    return storage;
  }

  /** Gets our tracked peer info.
   *
   * @return our tracked peer info
   */
  public TrackedPeerInfo getOurTrackedPeerInfo() {
    return ourTrackedPeerInfo;
  }

  /** Gets the download listener.
   *
   * @return the download listener
   */
  public DownloadListener getDownloadListener() {
    return downloadListener;
  }

  /** Gets the X.509 security information.
   *
   * @return the X.509 security information
   */
  protected X509SecurityInfo getX509SecurityInfo() {
    return x509SecurityInfo;
  }

  /** Gets the SSL torrent.
   *
   * @return the sslTorrent
   */
  public SSLTorrent getSSLTorrent() {
    return sslTorrent;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[PeerCoordinator " + ourTrackedPeerInfo.toString() + "]";
  }


}
