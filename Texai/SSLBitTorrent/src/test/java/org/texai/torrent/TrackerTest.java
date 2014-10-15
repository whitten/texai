/*
 * TrackerTest.java
 *
 * Created on Feb 2, 2010, 10:26:58 AM
 *
 * Description: .
 *
 * Copyright (C) Feb 2, 2010 reed.
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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import org.apache.commons.codec.net.URLCodec;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.network.netty.handler.AbstractBitTorrentHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.pipeline.HTTPClientPipelineFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.util.NetworkUtils;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public final class TrackerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TrackerTest.class);
  /** the server port */
  private static final int SERVER_PORT = 8088;
  /** the info hash */
  private static final byte[] INFO_HASH = {
    0x01, 0x02, 0x03, 0x04,
    0x05, 0x06, 0x07, 0x08,
    0x09, 0x0A, 0x0B, 0x0C,
    0x0D, 0x0E, 0x0F, 0x10,
    0x11, 0x12, 0x13, 0x14};

  /** sets debugging */
//  static {
//    System.setProperty("javax.net.debug", "all");
//  }
  public TrackerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    KeyStoreTestUtils.initializeClientKeyStore();
    KeyStoreTestUtils.initializeServerKeyStore();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of bit torrent tracker.
   */
  @Test
  public void testTracker() {
    LOGGER.info("tracker");

    // configure the HTTP request handler by registering the tracker
    final HTTPRequestHandler httpRequestHandler = HTTPRequestHandler.getInstance();
    final Tracker tracker = new Tracker();
    tracker.addInfoHash(new String((new URLCodec()).encode(INFO_HASH)));
    httpRequestHandler.register(tracker);

    // configure the server channel pipeline factory
    final AbstractBitTorrentHandlerFactory bitTorrentHandlerFactory = new MockBitTorrentHandlerFactory();
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory();
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
          null, // albusHCNMessageHandlerFactory,
          bitTorrentHandlerFactory,
          httpRequestHandlerFactory,
          x509SecurityInfo);

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    assertEquals("{}", serverBootstrap.getOptions().toString());
    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    serverBootstrap.bind(new InetSocketAddress("localhost", SERVER_PORT));

    // test tracker client
    httpClient();

    final Timer timer = new Timer();
    timer.schedule(new ShutdownTimerTask(), 5000);

    // shut down executor threads to exit
    LOGGER.info("releasing server resources");
    serverBootstrap.releaseExternalResources();
    timer.cancel();
  }

  /** Provides a task to run when the external resources cannot be released. */
  private static final class ShutdownTimerTask extends TimerTask {

    /** Runs the timer task. */
    @Override
    public void run() {
      LOGGER.info("server resources not released");
      System.exit(0);
    }

  }


  /** Tests the HTTP request and response messages. */
  @SuppressWarnings({"ThrowableResultIgnored", "null"})
  private void httpClient() {
    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    // configure the client pipeline
    final Object clientResume_lock = new Object();
    final AbstractHTTPResponseHandler httpResponseHandler = new MockHTTPResponseHandler(clientResume_lock);
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    final ChannelPipeline channelPipeline = HTTPClientPipelineFactory.getPipeline(
            httpResponseHandler,
            x509SecurityInfo);
    clientBootstrap.setPipeline(channelPipeline);
    LOGGER.info("pipeline: " + channelPipeline.toString());

    // start the connection attempt
    ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress("localhost", SERVER_PORT));

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      channelFuture.getCause().printStackTrace();
      fail(channelFuture.getCause().getMessage());
    }
    LOGGER.info("HTTP client connected");

    URI uri = null;
    HttpRequest httpRequest;
    String host;

    // send the statistics request
    try {
      uri = new URI("https://localhost:" + SERVER_PORT + "/torrent-tracker/statistics");
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    httpRequest = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            uri.toASCIIString());
    host = uri.getHost() == null ? "localhost" : uri.getHost();
    httpRequest.setHeader(HttpHeaders.Names.HOST, host);
    httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    LOGGER.info("httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      channelFuture.getCause().printStackTrace();
      fail(channelFuture.getCause().getMessage());
    }

    // send the scrape request
    try {
      uri = new URI("https://localhost:" + SERVER_PORT + "/torrent-tracker/scrape");
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    httpRequest = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            uri.toASCIIString());
    host = uri.getHost() == null ? "localhost" : uri.getHost();
    httpRequest.setHeader(HttpHeaders.Names.HOST, host);
    httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    LOGGER.info("httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      channelFuture.getCause().printStackTrace();
      fail(channelFuture.getCause().getMessage());
    }

    // send the announce request
    final byte[] myPeerIdBytes = {
      0x14, 0x13, 0x12, 0x11,
      0x10, 0x0F, 0x0E, 0x0D,
      0x0C, 0x0B, 0x0A, 0x09,
      0x08, 0x07, 0x06, 0x05,
      0x04, 0x03, 0x02, 0x01};
    final int nbrBytesUploaded = 0;
    final int nbrBytesDownloaded = 0;
    final int nbrBytesLeftToDownloaded = 1024;
    final String event = "started";
    final String myIPAddress = NetworkUtils.getLocalHostAddress().getHostAddress();
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("https://localhost:");
    stringBuilder.append(SERVER_PORT);
    stringBuilder.append("/torrent-tracker/announce");
    stringBuilder.append('?');
    stringBuilder.append("info_hash=");
    stringBuilder.append(new String((new URLCodec()).encode(INFO_HASH)));
    stringBuilder.append("&peer_id=");
    stringBuilder.append(new String((new URLCodec()).encode(myPeerIdBytes)));
    stringBuilder.append("&port=");
    stringBuilder.append(SERVER_PORT);
    stringBuilder.append("&uploaded=");
    stringBuilder.append(nbrBytesUploaded);
    stringBuilder.append("&downloaded=");
    stringBuilder.append(nbrBytesDownloaded);
    stringBuilder.append("&left=");
    stringBuilder.append(nbrBytesLeftToDownloaded);
    stringBuilder.append("&event=");
    stringBuilder.append(event);
    stringBuilder.append("&ip=");
    stringBuilder.append(myIPAddress);
    try {
      uri = new URI(stringBuilder.toString());
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    httpRequest = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            uri.toASCIIString());
    host = uri.getHost() == null ? "localhost" : uri.getHost();
    httpRequest.setHeader(HttpHeaders.Names.HOST, host);
    httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
    LOGGER.info("httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      channelFuture.getCause().printStackTrace();
      fail(channelFuture.getCause().getMessage());
    }

    // the message response handler will signal this thread when the test exchanges are completed
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }
    LOGGER.info("releasing HTTP client resources");
    channel.close();
    clientBootstrap.releaseExternalResources();
  }


}
