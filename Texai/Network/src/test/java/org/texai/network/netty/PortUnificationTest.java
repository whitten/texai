/*
 * PortUnificationTest.java
 *
 * Description: .
 *
 * Copyright (C) Feb 2, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty;

import org.texai.network.netty.utils.ConnectionUtils;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.texai.network.netty.handler.MockWebSocketResponseHandler;
//import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.texai.network.netty.pipeline.WebSocketClientPipelineFactory;
import org.texai.network.netty.handler.AbstractWebSocketResponseHandler;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.texai.util.TexaiException;
import org.texai.util.StringUtils;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.ahcsSupport.Message;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.MockAlbusHCSMessageHandler;
import org.texai.network.netty.handler.MockAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.MockHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.MockHTTPResponseHandler;
import org.texai.network.netty.pipeline.HTTPClientPipelineFactory;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public final class PortUnificationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(PortUnificationTest.class);
  // the server port
  private static final int SERVER_PORT = 8088;
  // the client executor
  private final Executor clientExecutor = Executors.newCachedThreadPool();

  /**
   * sets debugging
   */
//  static {
//    System.setProperty("javax.net.debug", "all");
//  }
  public PortUnificationTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    KeyStoreTestUtils.initializeClientKeyStore();
    KeyStoreTestUtils.initializeServerKeyStore();
    //Logger.getLogger(TaggedObjectEncoder.class).setLevel(Level.DEBUG);
    //Logger.getLogger(TaggedObjectDecoder.class).setLevel(Level.DEBUG);
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
   * Test of port unification ability to recognize and process a serialized object.
   */
  @Test
  public void testPortUnification() {
    LOGGER.info("port unification");

    // configure the server channel pipeline factory
    final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory = new MockAlbusHCSMessageHandlerFactory();
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new MockHTTPRequestHandlerFactory();
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    LOGGER.info("server x509SecurityInfo...\n" + x509SecurityInfo);

    final Executor serverExecutor = Executors.newCachedThreadPool();
    ConnectionUtils.createPortUnificationServer(
            SERVER_PORT,
            x509SecurityInfo,
            albusHCSMessageHandlerFactory,
            httpRequestHandlerFactory,
            serverExecutor, // bossExecutor
            serverExecutor); // workerExecutor

    LOGGER.info("testing clients");
    // test clients
    albusClientBigMessage();
    httpClient();
    nettyWebSocketClient();
    albusClient();
    httpClient();
    nettyWebSocketClient();
    nettyWebSocketClient();
    albusClient();
    httpClient();
    httpClient();
    albusClient();
    albusClient();
    nettyWebSocketClient();

    // shut down executor threads to exit
//    LOGGER.info("releasing server resources");
//    serverBootstrap.releaseExternalResources();   sometimes hangs
  }

  /**
   * Tests the exchange of serialized object messages.
   */
  @SuppressWarnings("ThrowableResultIgnored")
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"})
  private void albusClient() {
    // configure the client pipeline
    final Object clientResume_lock = new Object();
    final AbstractAlbusHCSMessageHandler albusHCSMessageHandler = new MockAlbusHCSMessageHandler(clientResume_lock, 10);
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();

    final Channel channel = ConnectionUtils.openAlbusHCSConnection(
            new InetSocketAddress("localhost", SERVER_PORT),
            x509SecurityInfo,
            albusHCSMessageHandler,
            clientExecutor, // bossExecutor
            clientExecutor); // workerExecutor
    LOGGER.info("Albus client connected");

    // send a message
    final String senderQualifiedName = "test-container1.test-agent1.test-role1";
    final String recipientQualifiedName = "test-container2.test-agent2.test-role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final String operation = "Echo_Task";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";

    Message message = new Message(
            senderQualifiedName,
            "TestSenderService",
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            null, // replyByDateTime,
            service,
            operation,
            parameterDictionary,
            "1.0.0"); // version
    String parameterName = "count";
    Object parameterValue = 0;
    message.put(parameterName, parameterValue);
    final ChannelFuture channelFuture = channel.write(message);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }

    // the message response handler will signal this thread when the test exchanges are completed
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }
    LOGGER.info("releasing Albus client resources");
    channel.close();
  }

  /**
   * Tests the exchange of serialized object messages.
   */
  @SuppressWarnings("ThrowableResultIgnored")
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"})
  private void albusClientBigMessage() {
    // configure the client pipeline
    final Object clientResume_lock = new Object();
    final AbstractAlbusHCSMessageHandler albusHCSMessageHandler = new MockAlbusHCSMessageHandler(clientResume_lock, 10);
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();

    final Channel channel = ConnectionUtils.openAlbusHCSConnection(
            new InetSocketAddress("localhost", SERVER_PORT),
            x509SecurityInfo,
            albusHCSMessageHandler,
            clientExecutor, // bossExecutor
            clientExecutor); // workerExecutor
    LOGGER.info("Albus client connected");

    // send a message
    final String senderQualifiedName = "test-container1.test-agent1.test-role1";
    final String recipientQualifiedName = "test-container2.test-agent2.test-role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    final String operation = "Echo_Task";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";

    Message message = new Message(
            senderQualifiedName,
            "TestSenderService",
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            null, // replyByDateTime
            service,
            operation,
            parameterDictionary,
            "1.0.0"); // version
    String parameterName = "count";
    Object parameterValue = 0;
    message.put(parameterName, parameterValue);
    message.put("bytes", new byte[15 * 1024]);
    final ChannelFuture channelFuture = channel.write(message);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }

    // the message response handler will signal this thread when the test exchanges are completed
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }
    LOGGER.info("releasing Albus client resources");
    channel.close();
  }

  /**
   * Tests the HTTP request and response messages.
   */
  @SuppressWarnings("ThrowableResultIgnored")
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP"})
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

    // start the connection attempt
    ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress("localhost", SERVER_PORT));

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }
    LOGGER.info("HTTP client connected");

    // send the HTTP request
    URI uri = null;
    try {
      uri = new URI("https://localhost:" + SERVER_PORT + "/data/test.txt");
    } catch (URISyntaxException ex) {
      fail(ex.getMessage());
    }
    @SuppressWarnings("null")
    final String host = uri.getHost() == null ? "localhost" : uri.getHost();
    final HttpRequest httpRequest = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.GET,
            uri.toASCIIString());
    httpRequest.setHeader(HttpHeaders.Names.HOST, host);
    httpRequest.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
    final CookieEncoder httpCookieEncoder = new CookieEncoder(false);
    httpCookieEncoder.addCookie("my-cookie", "foo");
    httpCookieEncoder.addCookie("another-cookie", "bar");
    httpRequest.setHeader(HttpHeaders.Names.COOKIE, httpCookieEncoder.encode());
    LOGGER.info("httpRequest ...\n" + httpRequest);
    channel.write(httpRequest);

    // wait for the request message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
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

    // Send 10 messages and wait for responses
    LOGGER.info("web socket client sending text frame messages ...");
    for (int i = 0; i < 10; i++) {
      final String message = "Message #" + i;
      LOGGER.info("  " + message);
      channel.write(new TextWebSocketFrame(message));
    }

    // Ping
    LOGGER.info("web socket client sending ping");
    channel.write(new PingWebSocketFrame(ChannelBuffers.copiedBuffer(new byte[]{1, 2, 3, 4, 5, 6})));

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
