/*
 * Tracker - Keeps track of clients sharing a particular torrent MetaInfo.
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
 * for consistency with the Texai project.  Converted to a private torrent protocol that
 * uses a single SSL socket.
 */
package org.texai.torrent;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;
import org.texai.network.netty.utils.NettyHTTPUtils;
import org.texai.torrent.bencode.BEncoder;
import org.texai.torrent.domainEntity.MetaInfo;
import org.texai.util.HTTPUtils;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;

/** Keeps track of clients sharing a particular torrent MetaInfo.
 * The tracker shares a multiplexed port with the local peer. */
public final class Tracker implements TexaiHTTPRequestHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Tracker.class);
  /** no event */
  public static final String NO_EVENT = "";
  /** started event */
  public static final String STARTED_EVENT = "started";
  /** completed event */
  public static final String COMPLETED_EVENT = "completed";
  /** stopped event */
  public static final String STOPPED_EVENT = "stopped";
  /** the 15 minute interval between tracker requests from the peer */
  private static final int REQUEST_INTERVAL_SECONDS = 15 * 60;
  /** the peer expiration cushion factor */
  private static final double PEER_EXPIRATION_CUSHION_FACTOR = 1.5d;
  /** the metainfo dictionary, URL-encoded torrent hash --> metainfo */
  private final Map<String, MetaInfo> metaInfoDictionary = new HashMap<>();
  /** the peers dictionary, URL-encoded torrent hash --> (map of tracked peer info --> tracked peer status) */
  private final Map<String, Map<TrackedPeerInfo, TrackedPeerStatus>> trackedPeerInfosDictionary
          = new HashMap<>();
  /** the completions dictionary, URL-encoded torrent hash --> number of download completions */
  private final Map<String, Integer> completionsDictionary = new HashMap<>();

  /** Creates a new Tracker instance. */
  public Tracker() {
    HTTPRequestHandler.getInstance().register(this);
  }

  /** Handles the HTTP request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   * @return the indicator whether the HTTP request was handled
   */
  @Override
  public boolean httpRequestReceived(
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final String uriString = httpRequest.getUri();
    LOGGER.info("received HTTP request: " + uriString);
    if (uriString.endsWith("/torrent-tracker/statistics")) {
      handleStatisticsRequest(httpRequest, channel);
      return true;
    } else if (uriString.endsWith("/torrent-tracker/scrape")) {
      handleScrapeRequest(httpRequest, channel);
      return true;
    } else if (uriString.contains("/torrent-tracker/announce")) {
      handleAnnounceRequest(httpRequest, channel);
      return true;
    } else {
      return false;
    }
  }

  /** Gets the torrent metainfo given the hash.
   *
   * @param urlEncodedInfoHash the URL-encoded info hash
   * @return the torrent metainfo given the hash
   */
  public MetaInfo getMetaInfo(final String urlEncodedInfoHash) {
    //Preconditions
    assert urlEncodedInfoHash != null : "urlEncodedInfoHash must not be null";
    assert !urlEncodedInfoHash.isEmpty() : "urlEncodedInfoHash must not be empty";

    return metaInfoDictionary.get(urlEncodedInfoHash);
  }

  /** Adds a new info hash to the tracker.
   *
   * @param urlEncodedInfoHash the new URL-encoded info hash
   */
  public void addInfoHash(final String urlEncodedInfoHash) {
    //Preconditions
    if (urlEncodedInfoHash == null || urlEncodedInfoHash.isEmpty()) {
      throw new IllegalArgumentException("invalid hash");
    }

    synchronized (trackedPeerInfosDictionary) {
      trackedPeerInfosDictionary.put(urlEncodedInfoHash, new HashMap<>());
    }
  }

  /** Adds a new metainfo to the tracker.
   *
   * @param metaInfo the new metainfo
   */
  public void addMetainfo(final MetaInfo metaInfo) {
    //Preconditions
    if (metaInfo == null) {
      throw new IllegalArgumentException("invalid metaInfo");
    }

    final String urlEncodedInfoHash = metaInfo.getURLEncodedInfoHash();
    addInfoHash(urlEncodedInfoHash);
    metaInfoDictionary.put(urlEncodedInfoHash, metaInfo);
  }

  /** Handles the statistics request, which is not part of the BitTorrent protocol.  It returns an
   * HTML statistics page.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  public void handleStatisticsRequest(
          final HttpRequest httpRequest,
          final Channel channel) {
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final StringBuilder responseContent = new StringBuilder();
    responseContent.append("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\n");
    responseContent.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n");
    responseContent.append("<html lang=\"en-US\" xml:lang=\"en-US\" xmlns=\"http://www.w3.org/1999/xhtml\">\n");
    responseContent.append("  <head>\n");
    responseContent.append("    <title>Snark</title>\n");
    responseContent.append("  </head>\n");
    responseContent.append("  <body>\n");
    responseContent.append("    <h2>Snark BitTorrent Tracker</h2>\n");
    responseContent.append("    <table border=\"1\">\n");
    responseContent.append("      <tr>\n");
    responseContent.append("        <th>Torrent Hash</th>\n");
    responseContent.append("        <th>Finished</th>\n");
    responseContent.append("        <th>Seeders</th>\n");
    responseContent.append("        <th>Leechers</th>\n");
    responseContent.append("      </tr>\n");
    Map<byte[], Map<String, Object>> filesDictionary = null;

    final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());
    final Map<String, List<String>> parameterDictionary = queryStringDecoder.getParameters();
    try {
      filesDictionary = gatherFilesDictionary(parameterDictionary);
    } catch (DecoderException ex) {
      throw new TexaiException(ex);
    }
    for (final Entry<byte[], Map<String, Object>> filesDictionaryEntry : filesDictionary.entrySet()) {
      responseContent.append("      <tr>\n");
      responseContent.append("        <td>");
      responseContent.append(new String((new URLCodec()).encode(filesDictionaryEntry.getKey())));
      responseContent.append("</td>\n");
      final Map<String, Object> peersDictionary = filesDictionaryEntry.getValue();
      responseContent.append("        <td>");
      responseContent.append(peersDictionary.get("downloaded"));
      responseContent.append("</td>\n");
      responseContent.append("        <td>");
      responseContent.append(peersDictionary.get("complete"));
      responseContent.append("</td>\n");
      responseContent.append("        <td>");
      responseContent.append(peersDictionary.get("incomplete"));
      responseContent.append("</td>\n");
      responseContent.append("      </tr>\n");
    }
    responseContent.append("    </table>\n");
    responseContent.append("  </body>\n");
    responseContent.append("</html>\n");

    NettyHTTPUtils.writeHTMLResponse(httpRequest, responseContent, channel);
  }

  /** Handles the scrape request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  public void handleScrapeRequest(
          final HttpRequest httpRequest,
          final Channel channel) {
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());
    final Map<String, List<String>> parameterDictionary = queryStringDecoder.getParameters();
    final InetSocketAddress remoteAddress = (InetSocketAddress) channel.getRemoteAddress();
    LOGGER.warn("tracker scrape request: " + remoteAddress + " -> " + parameterDictionary);
    try {
      NettyHTTPUtils.writeBinaryResponse(
              httpRequest,
              BEncoder.bencode(gatherFilesDictionary(parameterDictionary)),
              channel,
              null); // sessionCookie
    } catch (DecoderException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Handles the scrape request.
   *
   * @param parameterDictionary the parameter dictionary
   * @return the bencoded response
   * @throws DecoderException when a hash cannot be URL-decoded
   */
  @SuppressWarnings("unchecked")
  public Map<byte[], Map<String, Object>> gatherFilesDictionary(
          final Map<String, List<String>> parameterDictionary) throws DecoderException {
    //Preconditions
    assert parameterDictionary != null : "parameterDictionary must not be null";

    final Map<byte[], Map<String, Object>> filesDictionary = new HashMap<>();
    final List<String> requestedURLEncodedInfoHashes = new ArrayList<>();
    final Object infoHashValue = parameterDictionary.get("info_hash");
    if (infoHashValue == null) {
      // when the optional parameter is absent, then reply with all tracked info hashes
      requestedURLEncodedInfoHashes.addAll(trackedPeerInfosDictionary.keySet());
    } else if (infoHashValue instanceof List<?>) {
      requestedURLEncodedInfoHashes.addAll((List<String>) infoHashValue);
    } else {
      assert infoHashValue instanceof String;
      requestedURLEncodedInfoHashes.add((String) infoHashValue);
    }
    LOGGER.warn("reviewing tracked peers for statistics");
    for (final String requestedURLEncodedInfoHash : requestedURLEncodedInfoHashes) {
      if (trackedPeerInfosDictionary.containsKey(requestedURLEncodedInfoHash)) {
        LOGGER.warn("  requested info hash: " + requestedURLEncodedInfoHash);
        final byte[] key;
        key = URLCodec.decodeUrl(requestedURLEncodedInfoHash.getBytes());
        final Map<String, Object> peersDictionary = new HashMap<>();

        // total number of times the tracker has registered a completion ("event=complete", i.e. a client finished downloading the torrent)
        final Integer nbrDownloaded = completionsDictionary.get(requestedURLEncodedInfoHash);
        LOGGER.warn("  nbrDownloaded: " + nbrDownloaded);
        if (nbrDownloaded == null) {
          peersDictionary.put("downloaded", 0);
        } else {
          peersDictionary.put("downloaded", nbrDownloaded);
        }

        int nbrLeechers = 0;
        int nbrSeeders = 0;
        synchronized (trackedPeerInfosDictionary) {
          final Map<TrackedPeerInfo, TrackedPeerStatus> trackedPeerInfosMap = trackedPeerInfosDictionary.get(requestedURLEncodedInfoHash);
          for (final TrackedPeerStatus trackedPeerStatus : trackedPeerInfosMap.values()) {
            LOGGER.warn("  trackedPeerInfo: " + trackedPeerStatus);
            switch (trackedPeerStatus.event) {
              case STARTED_EVENT:
                nbrLeechers++;
                break;
              case COMPLETED_EVENT:
                nbrSeeders++;
                break;
              default:
                assert false;
                break;
            }
          }
        }
        // number of non-seeder peers, aka "leechers"
        peersDictionary.put("incomplete", nbrLeechers);

        // number of peers with the entire file, i.e. seeders
        peersDictionary.put("complete", nbrSeeders);

        // (optional) the torrent's internal name, as specified by the "name" file in the info section of the metainfo
        final MetaInfo metaInfo = metaInfoDictionary.get(requestedURLEncodedInfoHash);
        if (metaInfo != null) {
          assert metaInfo.getName() != null;
          peersDictionary.put("name", metaInfo.getName());
        }
        filesDictionary.put(key, peersDictionary);
      }
    }
    LOGGER.warn("filesDictionary: " + filesDictionary);
    return filesDictionary;
  }

  /** Returns whether this tracker is tracking the given URL-encoded info hash.
   *
   * @param requestedURLEncodedInfoHash the given URL-encoded info hash
   * @return whether this tracker is tracking the given URL-encoded info hash
   */
  public boolean isTracking(final String requestedURLEncodedInfoHash) {
    //Preconditions
    assert requestedURLEncodedInfoHash != null : "requestedURLEncodedInfoHash must not be null";

    synchronized (trackedPeerInfosDictionary) {
      return trackedPeerInfosDictionary.containsKey(requestedURLEncodedInfoHash);
    }
  }

  /** Returns whether this tracker has peers for the given URL-encoded info hash.
   *
   * @param requestedURLEncodedInfoHash the given URL-encoded info hash
   * @return whether this tracker is tracking the given URL-encoded info hash
   */
  public boolean hasPeers(final String requestedURLEncodedInfoHash) {
    //Preconditions
    assert requestedURLEncodedInfoHash != null : "requestedURLEncodedInfoHash must not be null";

    synchronized (trackedPeerInfosDictionary) {
      if (trackedPeerInfosDictionary.containsKey(requestedURLEncodedInfoHash)) {
        return !trackedPeerInfosDictionary.get(requestedURLEncodedInfoHash).isEmpty();
      } else {
        return false;
      }
    }
  }

  /** Handles the announce request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  public void handleAnnounceRequest(
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final URI uri;
    try {
      uri = new URI(httpRequest.getUri());
    } catch (URISyntaxException ex) {
      throw new TexaiException(ex);
    }
    final Map<String, String> parameterDictionary = HTTPUtils.getQueryMap(uri.getRawQuery());
    LOGGER.warn("tracker announce request: " + channel.getRemoteAddress() + " -> " + parameterDictionary);

    // info hash
    final String requestedUrlEncodedInfoHash = parameterDictionary.get("info_hash");
    LOGGER.debug("  requestedUrlEncodedInfoHash: " + requestedUrlEncodedInfoHash);
    if (requestedUrlEncodedInfoHash == null) {
      failure(httpRequest, "No info_hash given", channel);
      return;
    }
    boolean found = false;
    LOGGER.debug("trackedPeerInfosDictionary: " + trackedPeerInfosDictionary);
    synchronized (trackedPeerInfosDictionary) {
      for (String urlEncodedInfoHash : trackedPeerInfosDictionary.keySet()) {
        if (urlEncodedInfoHash.equals(requestedUrlEncodedInfoHash)) {
          found = true;
        }
      }
    }
    if (!found) {
      failure(httpRequest, "Tracker doesn't handle given info_hash", channel);
      return;
    }

    // peer id
    byte[] peerIdBytes;
    final String peerIdValue = parameterDictionary.get("peer_id");
    if (peerIdValue == null) {
      failure(httpRequest, "No peer_id given", channel);
      return;
    }
    try {
      peerIdBytes = (new URLCodec()).decode(peerIdValue.getBytes());
    } catch (DecoderException ex) {
      failure(httpRequest, "cannot decode peer id value: " + peerIdValue, channel);
      return;
    }
    if (peerIdBytes.length != 20) {
      failure(httpRequest, "peer_id must be 20 bytes long", channel);
      return;
    }

    // port
    @SuppressWarnings("UnusedAssignment")
    int peerPort = 0;
    final String peerPortValue = parameterDictionary.get("port");
    if (peerPortValue == null) {
      failure(httpRequest, "No port given", channel);
      return;
    }
    try {
      peerPort = Integer.parseInt(peerPortValue);
    } catch (NumberFormatException nfe) {
      failure(httpRequest, "port not a number: " + nfe, channel);
      return;
    }

    // ip
    final InetAddress inetAddress = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
    InetAddress inetAddress1 = null;
    if (NetworkUtils.isPrivateNetworkAddress(inetAddress)) {
      // See http://wiki.theory.org/BitTorrentSpecification#Tracker_Response .
      // Handle the case where the peer and ourselves are both behind the same NAT router.  The ip address from the
      // socket connection will be a private address which indicates the peer is on our private network behind the
      // internet-facing router.  The optional ip parameter contains what the peer found as its local host address.
      final String peerSuppliedIPAddress = parameterDictionary.get("ip");
      if (peerSuppliedIPAddress == null) {
        failure(httpRequest, "No ip address given", channel);
        return;
      } else {
        try {
          inetAddress1 = InetAddress.getByName(peerSuppliedIPAddress);
        } catch (UnknownHostException ex) {
          LOGGER.warn("invalid ip parameter supplied by peer: '" + peerSuppliedIPAddress + "'");
        }
      }
      if (inetAddress1 == null) {
        inetAddress1 = inetAddress;
      }
    } else {
      inetAddress1 = inetAddress;
    }
    final TrackedPeerInfo trackedPeerInfo = new TrackedPeerInfo(peerIdBytes, inetAddress1, peerPort);

    // downloaded
    final int downloaded;
    final String downloaded_value = parameterDictionary.get("downloaded");
    if (downloaded_value == null) {
      failure(httpRequest, "No downloaded given", channel);
      return;
    }
    try {
      downloaded = Integer.parseInt(downloaded_value);
    } catch (NumberFormatException nfe) {
      failure(httpRequest, "downloaded not a number: " + nfe, channel);
      return;
    }

    // event
    final Map<String, Object> responseDictionary = new HashMap<>();
    final Map<TrackedPeerInfo, TrackedPeerStatus> trackedPeerInfosMap = trackedPeerInfosDictionary.get(requestedUrlEncodedInfoHash);
    final String event = parameterDictionary.get("event");
    if (event == null || event.isEmpty()) {
      synchronized (trackedPeerInfosMap) {
        final TrackedPeerStatus trackedPeerStatus = trackedPeerInfosMap.get(trackedPeerInfo);
        if (trackedPeerStatus == null) {
          failure(httpRequest, "peer never started", channel);
          return;
        }
        LOGGER.warn("updating peer " + trackedPeerInfo + " expiration");
        trackedPeerStatus.updatePeerExpirationMilliseconds();
      }
    } else {
      if (event.equals(COMPLETED_EVENT)) {
        final TrackedPeerStatus trackedPeerStatus = trackedPeerInfosMap.get(trackedPeerInfo);
        if (trackedPeerStatus == null) {
          failure(httpRequest, "peer never started", channel);
          return;
        }
        trackedPeerStatus.event = COMPLETED_EVENT;
        if (downloaded == 0) {
          LOGGER.warn("seeding without download: " + trackedPeerInfo);
        } else {
          LOGGER.warn("seeding after completed download: " + trackedPeerInfo);
          synchronized (completionsDictionary) {
            final Integer nbrDownloaded = completionsDictionary.get(requestedUrlEncodedInfoHash);
            if (nbrDownloaded == null) {
              completionsDictionary.put(requestedUrlEncodedInfoHash, 1);
            } else {
              completionsDictionary.put(requestedUrlEncodedInfoHash, nbrDownloaded + 1);
            }
          }
        }
      } else {
        synchronized (trackedPeerInfosMap) {
          switch (event) {
            case STOPPED_EVENT:
              LOGGER.warn("removing stopped peer " + trackedPeerInfo);
              trackedPeerInfosMap.remove(trackedPeerInfo);
              break;
            case STARTED_EVENT:
              LOGGER.warn("adding new peer " + trackedPeerInfo);
              trackedPeerInfosMap.put(trackedPeerInfo, new TrackedPeerStatus(trackedPeerInfo, event));
              break;
            default:
              failure(httpRequest, "invalid event", channel);
              return;
          }
        }
      }
    }

    // compose tracker response
    responseDictionary.put("interval", REQUEST_INTERVAL_SECONDS);
    final List<Map<String, Object>> peerList = new ArrayList<>();
    final Iterator<TrackedPeerStatus> trackedPeerInfos_iter = trackedPeerInfosMap.values().iterator();
    try {
      LOGGER.warn("client peer id " + new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.warn("tracked peers ...");
    while (trackedPeerInfos_iter.hasNext()) {
      final TrackedPeerStatus trackedPeerStatus = trackedPeerInfos_iter.next();
      final TrackedPeerInfo trackedPeerInfo1 = trackedPeerStatus.trackedPeerInfo;
      LOGGER.warn("");
      LOGGER.warn("    peer id    " + trackedPeerInfo1.toIDString());
      LOGGER.warn("    ip         " + trackedPeerInfo1.getInetAddress().getHostAddress());
      LOGGER.warn("    port       " + trackedPeerInfo1.getPort());
      if (trackedPeerStatus.peerExpirationMilliseconds < System.currentTimeMillis()) {
        LOGGER.warn("expiring peer " + trackedPeerInfo1);
        trackedPeerInfos_iter.remove();
      } else if (trackedPeerInfo1.equals(trackedPeerInfo)) {
        LOGGER.warn("omitting self-peer from the peer list");
      } else {
        final Map<String, Object> map = new HashMap<>();
        map.put("peer id", trackedPeerInfo1.getPeerIdBytes());
        map.put("ip", trackedPeerInfo1.getInetAddress().getHostAddress());
        map.put("port", trackedPeerInfo1.getPort());
        peerList.add(map);
      }
    }
    responseDictionary.put("peers", peerList);

    LOGGER.log(Level.DEBUG, "Tracker response: " + responseDictionary);
    NettyHTTPUtils.writeBinaryResponse(
            httpRequest,
            BEncoder.bencode(responseDictionary),
            channel,
            null); // sessionCookie
  }

  /** Returns a bencoded failure reason.
   *
   * @param httpRequest the HTTP request
   * @param reason the failure reason
   * @param channel the channel
   */
  private static void failure(
          final HttpRequest httpRequest,
          final String reason,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert reason != null : "reason must not be null";
    assert !reason.isEmpty() : "reason must not be empty";
    assert channel != null : "channel must not be null";

    final Map<String, String> map = new HashMap<>();
    map.put("failure reason", reason);

    NettyHTTPUtils.writeBinaryResponse(
            httpRequest,
            BEncoder.bencode(map),
            channel,
            null); // sessionCookie
  }

  /** Handles a received text web socket frame.
   *
   * @param channel the channel handler context
   * @param textWebSocketFrame  the text web socket frame
   * @return the indicator whether the web socket request was handled
   */
  @Override
  public boolean textWebSocketFrameReceived(
          final Channel channel,
          final TextWebSocketFrame textWebSocketFrame) {
    throw new UnsupportedOperationException("Not supported yet.");

  }

  /** Contains tracked peer status. */
  private static final class TrackedPeerStatus {

    /** the peer ID */
    private final TrackedPeerInfo trackedPeerInfo;
    /** the peer expiration time in milliseconds */
    private long peerExpirationMilliseconds;
    /** the last tracked event, either started, completed or stopped */
    private String event;    // NOPMD

    /** Constructs a new TrackedPeerStatus instance for a started peer.
     *
     * @param trackedPeerInfo the tracked peer information
     * @param event the last tracked event, either started, complete or stopped
     */
    TrackedPeerStatus(final TrackedPeerInfo trackedPeerInfo, final String event) {
      //Preconditions
      assert trackedPeerInfo != null : "trackedPeerInfo must not be null";
      assert event != null : "event must not be null";
      assert event.equals(STARTED_EVENT)
              || event.equals(COMPLETED_EVENT)
              || event.equals(STOPPED_EVENT) : "event must be either started, completed or stopped";

      this.trackedPeerInfo = trackedPeerInfo;
      this.event = event;
      updatePeerExpirationMilliseconds();
    }

    /** Updates the peer expiration time in milliseconds. */
    public void updatePeerExpirationMilliseconds() {
      peerExpirationMilliseconds = System.currentTimeMillis() + (long) (PEER_EXPIRATION_CUSHION_FACTOR * (REQUEST_INTERVAL_SECONDS * 1000L));
    }

    /** Returns whether some other TrackedPeerStatus has the same peer ID as this one.
     *
     * @param obj the other object
     * @return whether some other object equals this one
     */
    @Override
    public boolean equals(final Object obj) {
      if (obj instanceof TrackedPeerStatus) {
        final TrackedPeerStatus that = (TrackedPeerStatus) obj;
        return this.trackedPeerInfo.equals(that.trackedPeerInfo);
      } else {
        return false;
      }
    }

    /** Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      return trackedPeerInfo.hashCode();
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[TrackedPeerStatus ");
      stringBuilder.append(trackedPeerInfo);
      stringBuilder.append(' ');
      stringBuilder.append(event);
      stringBuilder.append(']');
      return stringBuilder.toString();
    }
  }
}
