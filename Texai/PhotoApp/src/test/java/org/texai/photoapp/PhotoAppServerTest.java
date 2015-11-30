package org.texai.photoapp;

import org.texai.util.EnvironmentUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.util.StringUtils;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;
import org.texai.network.netty.handler.AbstractWebSocketResponseHandler;
import org.texai.network.netty.pipeline.WebSocketClientPipelineFactory;
import org.texai.util.TexaiException;

/**
 * PhotoAppServerTest.java
 *
 * Description:
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class PhotoAppServerTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(PhotoAppServerTest.class);
  // the server port
  private static final int SERVER_PORT = 8088;

  @BeforeClass
  public static void setUpClass() throws Exception {
    // initialize Alice, Bob, and Server X.509 certificates with 1024-bit RSA
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
   * Creates a new instance of PhotoAppServerTest.
   */
  public PhotoAppServerTest() {
  }

  /**
   * Test of class PhotoAppServer.
   */
  @Test
  public void testPhotoAppServer() {
    LOGGER.info("testPhotoAppServer");

    if (EnvironmentUtils.isWindows()) {
      LOGGER.info("bypassing photo app server test on Windows");
      return;
    }

    // create the photo app server and inject dependencies
    final PhotoAppServer photoAppServer = new PhotoAppServer();
    photoAppServer.setPhotoAppActions(new TestPhotoAppActions());

    // configure the HTTP request handler by registering the photo app server
    final HTTPRequestHandler httpRequestHandler = HTTPRequestHandler.getInstance();
    httpRequestHandler.register(photoAppServer);

    // initialize the test keystores
    KeyStoreTestUtils.initializeServerKeyStore();
    KeyStoreTestUtils.initializeClientKeyStore();

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory();
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
            null, // albusHCNMessageHandlerFactory,
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

    // test photo app server with mock client
    nettyWebSocketClient();

    final Timer timer = new Timer();
    timer.schedule(new ShutdownTimerTask(), 3000);

    // shut down executor threads to exit
    LOGGER.info("releasing server resources");
    serverBootstrap.releaseExternalResources();
    timer.cancel();
  }

  /**
   * Provides a task to run when the external resources cannot be released.
   */
  private static final class ShutdownTimerTask extends TimerTask {

    /**
     * Runs the timer task.
     */
    @Override
    public void run() {
      LOGGER.info("server resources not released");
      System.exit(0);
    }
  }

  /**
   * Tests the Netty web socket request and response messages.
   */
  @SuppressWarnings("ThrowableResultIgnored")
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"})
  private void nettyWebSocketClient() {
    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    final Object clientResume_lock = new Object();
    URI uri = null;
    try {
      // wss URI scheme indicates web socket secure connection
      uri = new URI("wss://localhost:" + SERVER_PORT + "/data/test.txt");
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    @SuppressWarnings("null")
    final String protocol = uri.getScheme();
    if (!protocol.equals("wss")) {
      throw new TexaiException("Unsupported protocol: " + protocol);
    }

    // configure the client pipeline
    final WebSocketClientHandshaker webSocketClientHandshaker = new WebSocketClientHandshakerFactory().newHandshaker(
            uri, // webSocketURL
            WebSocketVersion.V08, // version
            null, // subprotocol
            false, // allowExtensions
            new HashMap<>()); // customHeaders
    final AbstractWebSocketResponseHandler webSocketResponseHandler = new MockWebSocketResponseHandler(
            webSocketClientHandshaker,
            clientResume_lock);
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    LOGGER.info("Netty websocket client x509SecurityInfo...\n" + x509SecurityInfo);
    final ChannelPipeline channelPipeline = WebSocketClientPipelineFactory.getPipeline(
            webSocketResponseHandler,
            x509SecurityInfo);
    clientBootstrap.setPipeline(channelPipeline);

    // start the connection attempt
    ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }
    LOGGER.info("web socket client connected");

    // start the handshake that upgrades HTTP to web socket protocol
    webSocketClientHandshaker.handshake(channel);

    // the message response handler will signal this thread when the web socket handshake is completed
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }
    LOGGER.info("web socket client handshake completed");

    LOGGER.info("client pipeline ...\n" + channel.getPipeline());

    // Ping
    LOGGER.info("web socket client sending ping");
    channel.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));

    // login
    final String loginMessage = "{ \"operation\": \"login\", \"username\": \"Alice\" }";
    LOGGER.info("client writing " + loginMessage);
    channel.write(new TextWebSocketFrame(loginMessage));

    // storePhoto
    final String storePhotoMessage = "{ \n"
            + "\"operation\": \"storePhoto\",\n"
            + "\"encryptedPhoto\": \"photo encrypted with server public key, encoded in Base 64 notation\",\n"
            + "\"photoHash\": \"the SHA-1 hash of the photo encoded in Base 64 notation\"\n"
            + "}";
    LOGGER.info("client writing " + storePhotoMessage);
    channel.write(new TextWebSocketFrame(storePhotoMessage));

    // sendPhoto
    final String sendPhotoMessage = "{ \n"
            + "\"operation\": \"sendPhoto\",\n"
            + "\"photoHash\": \"the SHA-1 hash of the selected photo encoded in Base 64 notation\",\n"
            + "\"recipient\": \"Bob\"\n"
            + "}";
    LOGGER.info("client writing " + sendPhotoMessage);
    channel.write(new TextWebSocketFrame(sendPhotoMessage));

    try {
      // wait three seconds for server to process
      Thread.sleep(3_000);
    } catch (InterruptedException ex) {
    }

    // Close
    LOGGER.info("web socket client sending close");
    channelFuture = channel.write(new CloseWebSocketFrame());
    channelFuture.awaitUninterruptibly(10_000); // wait at most 10 seconds

    // WebSocketClientHandler will close the connection when the server
    // responds to the CloseWebSocketFrame.
    LOGGER.info("waiting for the web socket connection to close");
    channel.getCloseFuture().awaitUninterruptibly();

    LOGGER.info("releasing web socket client resources");
    channel.close();
    clientBootstrap.releaseExternalResources();
  }

}
