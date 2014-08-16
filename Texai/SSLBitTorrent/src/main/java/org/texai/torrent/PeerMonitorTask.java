/*
 * PeerMonitorTasks - TimerTask that monitors the peers and total up/down speed
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

import java.util.Iterator;
import java.util.TimerTask;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** TimerTask that monitors the peers and total up/download speeds. Works
 * together with the main Snark class to report periodical statistics.
 */
public final class PeerMonitorTask extends TimerTask {

  /** the monitor period */
  public static final long MONITOR_PERIOD = 10 * 1000; // Ten seconds.
  /** the kilos per second */
  private static final long KILOPERSECOND = 1024 * (MONITOR_PERIOD / 1000);
  /** the peer coordinator */
  private final PeerCoordinator peerCoordinator;
  /** the last total number of downloaded bytes of all peers */
  private long lastDownloaded = 0;
  /** the last total number of uploaded bytes of all peers */
  private long lastUploaded = 0;
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PeerMonitorTask.class);

  /** Constructs a new PeerMonitorTask instance.
   *
   * @param peerCoordinator the peer coordinator
   */
  public PeerMonitorTask(final PeerCoordinator peerCoordinator) {
    //Preconditions
    assert peerCoordinator != null : "coordinator must not be null";

    this.peerCoordinator = peerCoordinator;
  }

  /** Gathers bittorent statistics. */
  @Override
  public void run() {
    // Get some statistics
    int peers = 0;
    int uploaders = 0;
    int downloaders = 0;
    int interested = 0;
    int interesting = 0;
    int choking = 0;
    int choked = 0;

    synchronized (peerCoordinator.getPeers()) {
      final Iterator<Peer> peers_iter = peerCoordinator.getPeers().iterator();
      while (peers_iter.hasNext()) {
        final Peer peer = peers_iter.next();

        // Don't list dying peers
        if (!peer.isConnected()) {
          continue;
        }

        peers++;

        if (!peer.isChoking()) {
          uploaders++;
        }
        if (!peer.isChoked() && peer.isInteresting()) {
          downloaders++;
        }
        if (peer.isInterested()) {
          interested++;
        }
        if (peer.isInteresting()) {
          interesting++;
        }
        if (peer.isChoking()) {
          choking++;
        }
        if (peer.isChoked()) {
          choked++;
        }
      }
    }

    // Print some statistics
    final long downloaded = peerCoordinator.getDownloaded();
    String totalDown;
    if (downloaded >= 10 * 1024 * 1024) {
      totalDown = (downloaded / (1024 * 1024)) + "MB";
    } else {
      totalDown = (downloaded / 1024) + "KB";
    }
    final long uploaded = peerCoordinator.getUploaded();
    final String totalUp;
    if (uploaded >= 10 * 1024 * 1024) {
      totalUp = (uploaded / (1024 * 1024)) + "MB";
    } else {
      totalUp = (uploaded / 1024) + "KB";
    }

    final int needP = peerCoordinator.getStorage().getNbrNeededPieces();
    final long needMB = needP * peerCoordinator.getMetaInfo().getPieceLength(0) / (1024 * 1024);
    final int totalP = peerCoordinator.getMetaInfo().getNbrPieces();
    final long totalMB = peerCoordinator.getMetaInfo().getTotalLength() / (1024 * 1024);

    LOGGER.log(Level.INFO, "down: " + (downloaded - lastDownloaded) / KILOPERSECOND + "KB/s" + " (" + totalDown + ")" +
            " Up: " + (uploaded - lastUploaded) / KILOPERSECOND + "KB/s" + " (" + totalUp + ")" +
            " Need " + needP + " (" + needMB + "MB)" + " of " + totalP + " (" + totalMB + "MB)" + " pieces");
    LOGGER.log(Level.INFO, "peers: " + peers +
            " download-from: " + downloaders +
            " upload-to: " + uploaders +
            " interested in us: " + interested +
            " interesting to us: " + interesting +
            " choking us: " + choking +
            " choked by us: " + choked);
    lastDownloaded = downloaded;
    lastUploaded = uploaded;
  }
}
