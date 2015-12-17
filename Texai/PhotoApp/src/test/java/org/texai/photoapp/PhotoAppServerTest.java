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
import org.texai.x509.KeyStoreUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;
import org.texai.network.netty.handler.AbstractWebSocketResponseHandler;
import org.texai.network.netty.pipeline.WebSocketClientPipelineFactory;
import org.texai.util.TexaiException;

/**
 * PhotoAppServerTest.java
 *
 * Description: Test the photo app server.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class PhotoAppServerTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(PhotoAppServerTest.class);
  // the test server port
  private static final int SERVER_PORT = 8087;
  // the photo app server test instance
  private static PhotoAppServer photoAppServer;

  @BeforeClass
  public static void setUpClass() throws Exception {
    photoAppServer = new PhotoAppServer();
    photoAppServer.setIsUnitTest(true);
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

    // configure the HTTP request handler by registering the photo app server
    final HTTPRequestHandler httpRequestHandler = new HTTPRequestHandler();
    httpRequestHandler.register(photoAppServer);

    // initialize the test keystores
    KeyStoreUtils.initializeServerTestKeyStore();
    KeyStoreUtils.initializeClientKeyStore();

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory(httpRequestHandler);
    final X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getServerX509SecurityInfo();
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
            null, // albusHCNMessageHandlerFactory,
            httpRequestHandlerFactory,
            x509SecurityInfo,
            true); // isHTTP

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    assertEquals("{}", serverBootstrap.getOptions().toString());
    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    serverBootstrap.bind(new InetSocketAddress(SERVER_PORT));

    // test photo app server with mock clients, Alice, and Bob who starts first and waits on Alice to send a photo
    final WebSocketClient bobWebSocketClient = new WebSocketClient("Bob");
    final Thread bobClientThread = new Thread(bobWebSocketClient);
    bobClientThread.start();

    final WebSocketClient aliceWebSocketClient = new WebSocketClient("Alice");
    final Thread aliceClientThread = new Thread(aliceWebSocketClient);
    aliceClientThread.start();

    // wait for the client threads to complete
    try {
      Thread.sleep(20_000);
    } catch (InterruptedException ex) {
    }

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

  private class WebSocketClient implements Runnable {

    // the username
    private final String username;

    WebSocketClient(final String username) {
      //Preconditions
      assert StringUtils.isNonEmptyString(username) : "username must be a non-empty string";

      this.username = username;
    }

    /**
     * Executes this runnable.
     */
    @Override
    public void run() {
      if (username.equals("Alice")) {
        // wait on Bob to initialize
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException ex) {
        }
      }

      LOGGER.info("********** starting client " + username + " ************");

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
              clientResume_lock,
              photoAppServer.getInitializedUsers());
      final X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getClientX509SecurityInfo();
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
      LOGGER.info("********** client " + username + " sending ************");
      LOGGER.info("client sending ping");
      channel.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
      }

      // login
      LOGGER.info("********** client " + username + " sending ************");
      final String loginMessage = "{ \"operation\": \"login\", \"username\": \"" + username + "\" }";
      LOGGER.info("client writing " + loginMessage);
      channel.write(new TextWebSocketFrame(loginMessage));
      try {
        Thread.sleep(500);
      } catch (InterruptedException ex) {
      }

      if (username.equals("Alice")) {

        // storePhoto
        LOGGER.info("********** client Alice sending ************");
        final String storePhotoMessage = (new StringBuilder())
                .append("{\n")
                .append("\"operation\": \"storePhoto\",\n")
                .append("\"photo\": \"")
                .append(photoAppServer.getInitializedUsers().getPhotoBase64())
                .append("\",\n")
                .append("\"photoHash\": \"")
                .append(photoAppServer.getInitializedUsers().getPhotoHashBase64())
                .append("\"\n")
                .append("}\n")
                .toString();
        LOGGER.info("client writing " + storePhotoMessage);
        channel.write(new TextWebSocketFrame(storePhotoMessage));
        try {
          Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }

        // sendPhoto
        LOGGER.info("********** client Alice sending ************");
        final String sendPhotoMessage = (new StringBuilder())
                .append("{\n")
                .append("\"operation\": \"sendPhoto\",\n")
                .append("\"photoHash\": \"")
                .append(photoAppServer.getInitializedUsers().getPhotoHashBase64())
                .append("\",\n")
                .append("\"recipient\": \"Bob\"\n")
                .append("}\n")
                .toString();
        LOGGER.info("client writing " + sendPhotoMessage);
        channel.write(new TextWebSocketFrame(sendPhotoMessage));

        try {
          Thread.sleep(3_000);
        } catch (InterruptedException ex) {
        }

      } else {
        // Bob sleeps a while longer than Alice
        try {
          Thread.sleep(10_000);
        } catch (InterruptedException ex) {
        }
      }

      // Close
      LOGGER.info("********** client " + username + " sending ************");
      LOGGER.info("client sending close");
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

}
