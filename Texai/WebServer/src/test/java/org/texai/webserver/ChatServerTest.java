/*
 * ChatServerTest.java
 *
 * Created on Jun 30, 2008, 10:37:06 AM
 *
 * Description: .
 *
 * Copyright (C) Mar 5, 2010 reed.
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
package org.texai.webserver;

import org.texai.util.EnvironmentUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
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
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.pipeline.HTTPClientPipelineFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.util.StringUtils;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class ChatServerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ChatServerTest.class);
  /** the server port */
  private static final int SERVER_PORT = 8088;

  public ChatServerTest() {
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
   * Test of class ChatServer.
   */
  @Test
  public void testChatServer() {
    LOGGER.info("testChatServer");

    if (EnvironmentUtils.isWindows()) {
      LOGGER.info("bypassing chat server test on Windows");
      return;
    }

    // create the chat server and inject dependencies
    final ChatServer chatServer = new ChatServer();
    chatServer.setChatSession(new TestChatSession());

    // configure the HTTP request handler by registering the chat server
    final HTTPRequestHandler httpRequestHandler = HTTPRequestHandler.getInstance();
    httpRequestHandler.register(chatServer);

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory();
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
            null, // albusHCNMessageHandlerFactory,
            null, // bitTorrentHandlerFactory,
            httpRequestHandlerFactory,
            x509SecurityInfo);

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    assertEquals("{}", serverBootstrap.getOptions().toString());
    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    serverBootstrap.bind(new InetSocketAddress(SERVER_PORT));

    // test chat server with mock client
    mockHTTPClient();

    final Timer timer = new Timer();
    timer.schedule(new ShutdownTimerTask(), 3000);

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
  private void mockHTTPClient() {
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
    LOGGER.info("client pipeline: " + channelPipeline.toString());

    // start the connection attempt
    ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress("localhost", SERVER_PORT));

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      LOGGER.warn(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }
    LOGGER.info("client connected");

    URI uri = null;
    HttpRequest httpRequest;
    String host;

    // send the clear-file-cache request
    try {
      uri = new URI("https://localhost:" + SERVER_PORT + "/clear-file-cache");
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
    LOGGER.info("client httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.warn(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }

    // the message response handler will signal this thread when the test exchanges are completed
    LOGGER.info("client waiting for server to process the request");
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }

    LOGGER.info("client releasing HTTP resources");
    channel.close();
    clientBootstrap.releaseExternalResources();
  }
}
