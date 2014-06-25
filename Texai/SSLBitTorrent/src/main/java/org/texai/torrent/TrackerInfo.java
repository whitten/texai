/*
 * TrackerInfo - Holds information returned by a tracker, mainly the peer list.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.texai.torrent.bencode.BDecoder;
import org.texai.torrent.bencode.BEValue;
import org.texai.torrent.bencode.InvalidBEncodingException;

/** Holds information returned by a tracker, mainly the peer list */
public final class TrackerInfo {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrackerInfo.class);
  /** the failure reason */
  private final String failure_reason;
  /** the tracking interval */
  private final int interval;
  /** the tracked peer infos */
  private final Set<TrackedPeerInfo> trackedPeerInfos;

  /** Constructs a new TrackerInfo instance.
   *
   * @param trackerResponseContent the tracker response content
   * @throws IOException when an input/output error occurs
   */
  public TrackerInfo(final ChannelBuffer trackerResponseContent) throws IOException {
    //Preconditions
    assert trackerResponseContent != null : "trackerReponseContent must not be null";
    assert trackerResponseContent.readableBytes() > 0 : "trackerResponseContent must not be empty";

    final byte[] trackerResponseBytes = new byte[trackerResponseContent.readableBytes()];
    trackerResponseContent.getBytes(0, trackerResponseBytes);
    final InputStream inputStream = new ByteArrayInputStream(trackerResponseBytes);
    final Map<String, BEValue> map = new BDecoder(inputStream).bdecodeMap().getMap();
    LOGGER.info("map: " + map);
    final BEValue reason = map.get("failure reason");
    if (reason == null) {
      failure_reason = null;
      final BEValue beInterval = map.get("interval");
      if (beInterval == null) {
        throw new InvalidBEncodingException("No interval given");
      } else {
        interval = beInterval.getInt();
      }
      final BEValue bePeers = map.get("peers");
      if (bePeers == null) {
        throw new InvalidBEncodingException("No peer list");
      } else {
        assert bePeers.getValue() instanceof List<?>;
        trackedPeerInfos = getTrackedPeerInfos(bePeers.getList());
      }
    } else {
      failure_reason = reason.getString();
      interval = -1;
      trackedPeerInfos = null;
    }

  }

  /** Gets the tracked peer infos.
   *
   * @param beValueList the list of BEValues
   * @return tthe tracked peer infos
   * @throws IOException when an error occurs
   */
  public static Set<TrackedPeerInfo> getTrackedPeerInfos(final List<BEValue> beValueList) throws IOException {
    //Preconditions
    assert beValueList != null : "beValueList must not be null";

    final Set<TrackedPeerInfo> trackedPeerInfos = new HashSet<>(beValueList.size());
    final Iterator<BEValue> beValueList_iter = beValueList.iterator();
    while (beValueList_iter.hasNext()) {
      final TrackedPeerInfo trackedPeerInfo = new TrackedPeerInfo((beValueList_iter.next()).getMap());
      trackedPeerInfos.add(trackedPeerInfo);
    }

    return trackedPeerInfos;
  }

  /** Gets the tracked peer infos.
   *
   * @return the tracked peer infos
   */
  public Set<TrackedPeerInfo> getTrackedPeerInfos() {
    return trackedPeerInfos;
  }

  /** Gets the failure reason.
   *
   * @return the failure reason
   */
  public String getFailureReason() {
    return failure_reason;
  }

  /** Gets the tracking interval.
   *
   * @return the tracking interval
   */
  public int getInterval() {
    return interval;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    if (failure_reason == null) {
      return "[TrackerInfo interval: " + interval + ", tracked peer infos: " + trackedPeerInfos + "]";
    } else {
      return "[TrackerInfo FAILED: " + failure_reason + "]";
    }
  }
}
