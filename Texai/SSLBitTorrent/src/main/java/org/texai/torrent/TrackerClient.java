/*
 * TrackerClient - Class that informs a tracker and gets new peers. Copyright
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

import java.net.MalformedURLException;
import org.texai.torrent.domainEntity.MetaInfo;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.pipeline.HTTPClientPipelineFactory;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Informs metainfo tracker of events and gets new peers for peer coordinator.
 *
 * @author Mark Wielaard (mark@klomp.org)
 */
public final class TrackerClient extends AbstractHTTPResponseHandler implements Runnable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrackerClient.class);
  /** the maximum number of times that we are allowed to fail to make an
   * initial contact with the tracker before we bail
   */
  private static final int MAX_FAILURE_COUNT = 2;
  /** sleep interval */
  private static final int SLEEP = 1; // Check in with tracker every minute
  /** the torrent metainfo */
  private final MetaInfo metaInfo;
  /** the peer coordinator */
  private final PeerCoordinator peerCoordinator;
  /** our port that accepts connections from peers */
  private final int ourConnectionPort;
  /** the indicator whether to stop the tracker client */
  private boolean isQuit = false;
  /** the tracker request interval milliseconds */
  private long trackerRequestIntervalMillis;
  /** the last tracker request time */
  private long lastTrackerRequestTime;
  /** the tracker host */
  private final String trackerHost;
  /** the tracker port */
  private final int trackerPort;
  /** the tracker info */
  private TrackerInfo trackerInfo;
  /** the tracker info lock */
  private final Object trackerInfo_lock = new Object();

  /** Creates a new TrackerClient instance.
   *
   * @param metaInfo the torrent metainfo
   * @param peerCoordinator the peer coordinator
   * @param ourConnectionPort our port that accepts connections from peers
   */
  public TrackerClient(
          final MetaInfo metaInfo,
          final PeerCoordinator peerCoordinator,
          final int ourConnectionPort) {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    assert peerCoordinator != null : "peerCoordinator must not be null";
    assert ourConnectionPort > 0 : "ourConnectionPort must be positive";

    this.metaInfo = metaInfo;
    this.peerCoordinator = peerCoordinator;
    final URL announceURL;
    try {
      announceURL = new URL(metaInfo.getAnnounceURLString());
    } catch (MalformedURLException ex) {
      throw new TexaiException(ex);
    }
    trackerHost = announceURL.getHost() == null ? "localhost" : announceURL.getHost();
    trackerPort = announceURL.getPort();
    Thread.currentThread().setName("TrackerClient-" + peerCoordinator.getURLEncodedID());
    this.ourConnectionPort = ourConnectionPort;
  }

  /** Interrupts this thread to stop it. */
  public void quit() {
    isQuit = true;
    Thread.currentThread().interrupt();
  }

  /** Executes this thread. */
  @Override
  @SuppressWarnings("SleepWhileInLoop")
  public void run() {
    LOGGER.info("running " + this);
    final String announceURLString = metaInfo.getAnnounceURLString();
    final String urlEncodedInfoHash = metaInfo.getURLEncodedInfoHash();
    LOGGER.info("url encoded info hash " + urlEncodedInfoHash);
    final String urlEncodedPeerID = peerCoordinator.getURLEncodedID();

    boolean isCompletedYetToBeSent = !peerCoordinator.isCompleted();
    boolean isTrackerContacted = false;
    long uploaded = peerCoordinator.getUploaded();
    long downloaded = peerCoordinator.getDownloaded();
    long left = peerCoordinator.getApproximateNbrBytesRemaining();

    String peerIPAddress = null;
    try {
      final InetAddress localHostAddress = NetworkUtils.getLocalHostAddress();
      if (localHostAddress.isLoopbackAddress()) {
        LOGGER.info("local host address is the loopback address: " + localHostAddress);
      } else {
        peerIPAddress = localHostAddress.getHostAddress();
        LOGGER.info("local host address: " + peerIPAddress);
      }

      int failures = 0;
      while (!isTrackerContacted && failures < MAX_FAILURE_COUNT) {
        // send start to tracker
        request(
                announceURLString,
                urlEncodedInfoHash,
                urlEncodedPeerID,
                uploaded,
                downloaded,
                left,
                peerIPAddress,
                Tracker.STARTED_EVENT);

        synchronized (trackerInfo_lock) {
          trackerInfo_lock.wait(30000);
        }
        if (trackerInfo == null) {
          LOGGER.warn("Could not contact tracker at " + announceURLString);
          failures++;
          continue;
        }

        // process response
        final TrackedPeerInfo ourTrackedPeerInfo = peerCoordinator.getOurTrackedPeerInfo();
        final Set<TrackedPeerInfo> trackedPeerInfos = trackerInfo.getTrackedPeerInfos();
        if (trackedPeerInfos != null) {
          final Iterator<TrackedPeerInfo> trackedPeerInfos_iter = trackedPeerInfos.iterator();
          while (trackedPeerInfos_iter.hasNext()) {
            final TrackedPeerInfo trackedPeerInfo = trackedPeerInfos_iter.next();
            if (!trackedPeerInfo.equals(ourTrackedPeerInfo)) {
              peerCoordinator.addPeerThatWeContact(trackedPeerInfo);
            }
          }
        }
        isTrackerContacted = true;

        if (!isTrackerContacted) {
          failures++;
          LOGGER.info("     Retrying in 2s...");
          try {
            // Sleep two seconds...
            Thread.sleep(2 * 1000);
          } catch (InterruptedException interrupt) {
            LOGGER.debug("ignored " + interrupt.getMessage());
          }
        }
      }

      if (failures >= MAX_FAILURE_COUNT) {
        throw new IOException("Could not establish initial connection");
      }

      // periodically, e.g. every 15 minutes, contact the tracker
      while (!isQuit) {
        uploaded = peerCoordinator.getUploaded();
        downloaded = peerCoordinator.getDownloaded();
        left = peerCoordinator.getApproximateNbrBytesRemaining();
        final String event;
        if (isCompletedYetToBeSent && peerCoordinator.isCompleted()) {
          isCompletedYetToBeSent = false;
          event = Tracker.COMPLETED_EVENT;
        } else {
          event = Tracker.NO_EVENT;
        }
        if (event.equals(Tracker.COMPLETED_EVENT) ||
                (peerCoordinator.areMorePeersNeeded() &&
                System.currentTimeMillis() > lastTrackerRequestTime + trackerRequestIntervalMillis)) {
          request(
                  announceURLString,
                  urlEncodedInfoHash,
                  urlEncodedPeerID,
                  uploaded,
                  downloaded,
                  left,
                  peerIPAddress,
                  event);

          synchronized (trackerInfo_lock) {
            trackerInfo_lock.wait(30000);
          }
          if (trackerInfo == null) {
            LOGGER.warn("Could not contact tracker at " + announceURLString);
            failures++;
            continue;
          }
          final Iterator<TrackedPeerInfo> trackedPeerInfos_iter = trackerInfo.getTrackedPeerInfos().iterator();
          while (trackedPeerInfos_iter.hasNext()) {
            peerCoordinator.addPeerThatWeContact(trackedPeerInfos_iter.next());
          }
          final DownloadListener downloadListener = peerCoordinator.getDownloadListener();
          if (event.equals(Tracker.COMPLETED_EVENT) && downloadListener != null) {
            downloadListener.downloadCompleted(metaInfo);
          }
        }
        try {
          // Sleep some minutes...
          Thread.sleep(SLEEP * 60 * 1000);
        } catch (InterruptedException interrupt) {
          LOGGER.debug("ignored " + interrupt.getMessage());
        }
      }

    } catch (IOException | InterruptedException t) {
      LOGGER.log(Level.ERROR, "fatal exception in TrackerClient", t);
    } finally {
      try {
        if (isTrackerContacted) {
          request(
                  announceURLString,
                  urlEncodedInfoHash,
                  urlEncodedPeerID,
                  uploaded,
                  downloaded,
                  left,
                  peerIPAddress,
                  Tracker.STOPPED_EVENT);
        }
      } catch (IOException ioe) { /* ignored */
        LOGGER.debug("ignored " + ioe.getMessage());
      }
    }

  }

  /** Requests peer information from the tracker, and updates the tracker regarding our status.
   *
   * @param announceURLString the tracker announce URL in string form
   * @param urlEncodedInfoHash the URL-encoded info hash
   * @param peerID the peer id
   * @param uploaded the total uploaded bytes
   * @param downloaded the total downloaded bytes
   * @param left the file bytes remaining
   * @param peerIPAddress the peer IP address
   * @param event the event
   * @throws IOException when an input/output error occurs
   */
  @SuppressWarnings({"ThrowableResultIgnored", "null"})
  private void request(
          final String announceURLString,
          final String urlEncodedInfoHash,
          final String peerID,
          final long uploaded,
          final long downloaded,
          final long left,
          final String peerIPAddress,
          final String event)
          throws IOException {
    //Preconditions
    assert announceURLString != null : "announceURLString must not be null";
    assert !announceURLString.isEmpty() : "announceURLString must not be empty";
    assert urlEncodedInfoHash != null : "infoHash must not be null";
    assert !urlEncodedInfoHash.isEmpty() : "infoHash must not be empty";
    assert peerID != null : "peerID must not be null";
    assert !peerID.isEmpty() : "peerID must not be empty";
    assert uploaded >= 0 : "uploaded must not be negative";
    assert downloaded >= 0 : "downloaded must not be negative";
    assert left >= 0 : "left must not be negative";
    assert peerIPAddress != null : "peerIPAddress must not be null";
    assert !peerIPAddress.isEmpty() : "peerIPAddress must not be empty";
    assert "started".equals(event) || "completed".equals(event) || "stopped".equals(event) || event.isEmpty() : "invalid event: " + event;

    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    // configure the client pipeline
    final ChannelPipeline channelPipeline = HTTPClientPipelineFactory.getPipeline(
            this,
            peerCoordinator.getX509SecurityInfo());
    clientBootstrap.setPipeline(channelPipeline);
    LOGGER.info("pipeline: " + channelPipeline.toString());

    // start the connection attempt
    ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(trackerHost, trackerPort));

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      LOGGER.warn(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      throw new TexaiException(channelFuture.getCause());
    }
    LOGGER.info("HTTP client connected");

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(announceURLString);
    stringBuilder.append("?info_hash=");
    stringBuilder.append(urlEncodedInfoHash);
    stringBuilder.append("&peer_id=");
    stringBuilder.append(peerID);
    stringBuilder.append("&port=");
    stringBuilder.append(ourConnectionPort);
    stringBuilder.append("&uploaded=");
    stringBuilder.append(uploaded);
    stringBuilder.append("&downloaded=");
    stringBuilder.append(downloaded);
    stringBuilder.append("&left=");
    stringBuilder.append(left);
    stringBuilder.append("&compact=1&ip=");
    if (peerIPAddress != null) {
      stringBuilder.append(peerIPAddress);
    }
    if (!event.equals(Tracker.NO_EVENT)) {
      stringBuilder.append("&event=");
      stringBuilder.append(event);
    }
    LOGGER.info("event: " + event);

    final URI uri;
    try {
      uri = new URI(stringBuilder.toString());
    } catch (URISyntaxException ex) {
      throw new TexaiException(ex);
    }
    final HttpRequest httpRequest = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            uri.toASCIIString());
    httpRequest.setHeader(HttpHeaders.Names.HOST, trackerHost);
    httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    LOGGER.info("httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.warn(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      throw new TexaiException(channelFuture.getCause());
    }
  }

  /** Asynchronously receives a tracker HTTP response message.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    LOGGER.info("received messageEvent: " + messageEvent);
    final HttpResponse httpResponse = (HttpResponse) messageEvent.getMessage();

    LOGGER.info("STATUS: " + httpResponse.getStatus());
    LOGGER.info("VERSION: " + httpResponse.getProtocolVersion());
    LOGGER.info("");

    if (!httpResponse.getHeaderNames().isEmpty()) {
      for (final String name : httpResponse.getHeaderNames()) {
        for (final String value : httpResponse.getHeaders(name)) {
          LOGGER.info("HEADER: " + name + " = " + value);
        }
      }
      LOGGER.info("");
    }

    final ChannelBuffer trackerResponseContent = httpResponse.getContent();
    if (trackerResponseContent.readable()) {
      LOGGER.info("CONTENT {");
      LOGGER.info(trackerResponseContent.toString(CharsetUtil.UTF_8));
      LOGGER.info("} END OF CONTENT");
    }

    synchronized (trackerInfo_lock) {
      try {
        trackerInfo = new TrackerInfo(trackerResponseContent);
        trackerInfo_lock.notifyAll();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    LOGGER.info("TrackerClient response: " + trackerInfo);
    lastTrackerRequestTime = System.currentTimeMillis();

    final String failure = trackerInfo.getFailureReason();
    if (failure != null) {
      throw new TexaiException(failure);
    }

    trackerRequestIntervalMillis = (long) trackerInfo.getInterval() * 1000L;
    LOGGER.info("will contact tracker again in " + trackerInfo.getInterval() + " seconds");
  }

  /** Handles a caught exception.
   *
   * @param channelHandlerContext the channel handler event
   * @param exceptionEvent the exception event
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    LOGGER.error("exceptionEvent: " + exceptionEvent);
    throw new TexaiException(exceptionEvent.getCause());
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[TrackerClient " + peerCoordinator.getOurTrackedPeerInfo() + "]";
  }
}
