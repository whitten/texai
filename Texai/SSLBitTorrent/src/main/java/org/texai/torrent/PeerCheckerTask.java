/*
 * PeerCheckTasks - TimerTask that checks for good/bad up/downloaders. Copyright
 * (C) 2003 Mark J. Wielaard
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/** TimerTask that checks for good/bad up/downloader. Works together with the
 * PeerCoordinator to select which Peers get (un)choked.
 */
public final class PeerCheckerTask extends TimerTask {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PeerCheckerTask.class);
  /** the kilo per second check period */
  private static final long KILOPERSECOND = 1024 * (PeerCoordinator.CHECK_PERIOD / 1000);
  /** the peer coordinator */
  private final PeerCoordinator coordinator;

  /** Constructs a new PeerCheckerTask instance.
   *
   * @param coordinator the peer coordinator
   */
  PeerCheckerTask(final PeerCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  /** Runs the peer checker task. */
  @Override
  public void run() {
    synchronized (coordinator.getPeers()) {
      // Calculate total uploading and worst downloader.
      long wordDownloaded = Long.MAX_VALUE;
      Peer worstDownloadPeer = null;

      int uploaders = 0;
      int interested = 0;

      // Keep track of peers we remove now,
      // we will add them back to the end of the list.
      final List<Peer> removedPeers = new ArrayList<>();

      final Iterator<Peer> peers_iter = coordinator.getPeers().iterator();
      while (peers_iter.hasNext()) {
        final Peer peer = peers_iter.next();

        // Remove dying peers
        if (!peer.isConnected()) {
          peers_iter.remove();
          continue;
        }

        if (!peer.isChoking()) {
          uploaders++;
        }
        if (peer.isInterested()) {
          interested++;
        }

        // XXX - We should calculate the up/download rate a bit
        // more intelligently
        final long uploaded = peer.getUploaded();
        final long downloaded = peer.getDownloaded();
        peer.resetCounters();

        LOGGER.log(Level.DEBUG, peer + ":" + " ul: " + uploaded / KILOPERSECOND + " dl: " +
                downloaded / KILOPERSECOND + " i: " + peer.isInterested() + " I: " + peer.isInteresting() +
                " c: " + peer.isChoking() + " C: " + peer.isChoked());

        // If we are at our max uploaders and we have lots of other
        // interested peers try to make some room.
        // (Note use of coordinator.uploaders)
        if (coordinator.getUploaders() >= PeerCoordinator.MAX_UPLOADERS && interested > PeerCoordinator.MAX_UPLOADERS && !peer.isChoking()) {
          // Check if it still wants pieces from us.
          if (!peer.isInterested()) {    // NOPMD
            LOGGER.log(Level.DEBUG, "Choke uninterested peer: " + peer);
            peer.setChoking(true);
            uploaders--;
            coordinator.setUploaders(coordinator.getUploaders() - 1);

            // Put it at the back of the list
            peers_iter.remove();
            removedPeers.add(peer);
          } else if (peer.isChoked()) {
            // If they are choking us make someone else a downloader
            LOGGER.log(Level.DEBUG, "Choke choking peer: " + peer);
            peer.setChoking(true);
            uploaders--;
            coordinator.setUploaders(coordinator.getUploaders() - 1);

            // Put it at the back of the list
            peers_iter.remove();
            removedPeers.add(peer);
          } else if (peer.isInteresting() && !peer.isChoked() && downloaded == 0) {
            // We are downloading but didn't receive anything...
            LOGGER.log(Level.DEBUG,
                    "Choke downloader that doesn't deliver:" + peer);
            peer.setChoking(true);
            uploaders--;
            coordinator.setUploaders(coordinator.getUploaders() - 1);

            // Put it at the back of the list
            peers_iter.remove();
            removedPeers.add(peer);
          } else if (!peer.isChoking() && downloaded < wordDownloaded) {
            // Make sure download is good if we are uploading
            wordDownloaded = downloaded;
            worstDownloadPeer = peer;
          }
        }
      }

      // Resync actual uploaders value
      // (can shift a bit by disconnecting peers)
      coordinator.setUploaders(uploaders);

      // Remove the worst downloader if needed.
      if (uploaders >= PeerCoordinator.MAX_UPLOADERS && interested > PeerCoordinator.MAX_UPLOADERS && worstDownloadPeer != null) {
        LOGGER.log(Level.DEBUG, "Choke worst downloader: " + worstDownloadPeer);

        worstDownloadPeer.setChoking(true);
        coordinator.setUploaders(coordinator.getUploaders() - 1);

        // Put it at the back of the list
        coordinator.getPeers().remove(worstDownloadPeer);
        removedPeers.add(worstDownloadPeer);
      }

      // Optimistically unchoke a peer
      coordinator.unchokePeer();

      // Put peers back at the end of the list that we removed earlier.
      coordinator.getPeers().addAll(removedPeers);
    }
  }
}
