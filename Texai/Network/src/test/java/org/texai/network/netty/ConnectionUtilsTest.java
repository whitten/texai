/*
 * ConnectionUtilsTest.java
 *
 * Description: .
 *
 * Copyright (C) Apr 1, 2010 Stephen Reed.
 *
 */
package org.texai.network.netty;

import org.texai.network.netty.utils.ConnectionUtils;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.MockAlbusHCSMessageHandler;
import org.texai.network.netty.handler.MockAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.MockHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.MockHTTPResponseHandler;
import org.texai.x509.KeyStoreUtils;
import org.texai.x509.X509SecurityInfo;

/**
 *
 * @author reed
 */
public class ConnectionUtilsTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ConnectionUtilsTest.class);
  // the server bootstrap
  private static ServerBootstrap serverBootstrap;
  // the server port
  private static final int SERVER_PORT = 8088;
  // the executor
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();

  public ConnectionUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("createPortUnificationServer");
    final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory = new MockAlbusHCSMessageHandlerFactory();
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new MockHTTPRequestHandlerFactory();
    final X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getServerX509SecurityInfo();
    serverBootstrap = ConnectionUtils.createPortUnificationServer(
            SERVER_PORT,
            x509SecurityInfo,
            albusHCSMessageHandlerFactory,
            httpRequestHandlerFactory,
            EXECUTOR, // bossExecutor
            EXECUTOR, // workerExecutor
            true); // isHTTPS
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    LOGGER.info("tearDownClass");
    //ConnectionUtils.closePortUnificationServer(serverBootstrap);  // sometimes hangs up
    ((ExecutorService) EXECUTOR).shutdownNow();
    LOGGER.info("tearDownClass OK");
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of openAlbusHCSConnection method, of class ConnectionUtils.
   */
  @Test
  public void testOpenAlbusHCSConnection() {
    LOGGER.info("openAlbusHCSConnection");
    InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", SERVER_PORT);
    X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getClientX509SecurityInfo();
    final Object clientResume_lock = new Object();
    final AbstractAlbusHCSMessageHandler albusHCSMessageHandler = new MockAlbusHCSMessageHandler(clientResume_lock, 10);
    final Channel channel = ConnectionUtils.openAlbusHCSConnection(
            inetSocketAddress,
            x509SecurityInfo,
            albusHCSMessageHandler,
            EXECUTOR, // bossExecutor
            EXECUTOR); // workerExecutor
    assertNotNull(channel);
    assertEquals("localhost/127.0.0.1:8088", channel.getRemoteAddress().toString());
    LOGGER.info("closing channel");
    channel.close();
  }

  /**
   * Test of openHTTPConnection method, of class ConnectionUtils.
   */
  @Test
  public void testOpenHTTPConnection() {
    LOGGER.info("openHTTPConnection");
    InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", SERVER_PORT);
    X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getClientX509SecurityInfo();
    final Object clientResume_lock = new Object();
    final AbstractHTTPResponseHandler httpResponseHandler = new MockHTTPResponseHandler(clientResume_lock);
    final Channel channel = ConnectionUtils.openHTTPConnection(
            inetSocketAddress,
            x509SecurityInfo,
            httpResponseHandler,
            EXECUTOR, // bossExecutor
            EXECUTOR); // workerExecutor
    assertNotNull(channel);
    assertEquals("localhost/127.0.0.1:8088", channel.getRemoteAddress().toString());
    LOGGER.info("closing channel");
    channel.close();
  }

}
