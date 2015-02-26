/*
 * ConnectionUtilsTest.java
 *
 * Created on Jun 30, 2008, 9:42:31 AM
 *
 * Description: .
 *
 * Copyright (C) Apr 1, 2010 Stephen Reed.
 *
 */
package org.texai.network.netty;

import com.google.bitcoin.core.BitcoinSerializer;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.VersionMessage;
import com.google.bitcoin.params.MainNetParams;
import org.texai.network.netty.utils.ConnectionUtils;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandlerFactory;
import org.texai.network.netty.handler.BitcoinProtocolDecoder;
import org.texai.network.netty.handler.BitcoinProtocolEncoder;
import org.texai.network.netty.handler.MockBitcoinProtocolMessageHandler;
import org.texai.network.netty.handler.MockBitcoinProtocolMessageHandlerFactory;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class ConnectionUtils2Test {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ConnectionUtils2Test.class);
  // the server bootstrap
  private static ServerBootstrap serverBootstrap;
  // the server port
  private static final int SERVER_PORT = 8088;
  // the executor
  private static final Executor EXECUTOR = Executors.newCachedThreadPool();
  // the client resume lock
  final static Object clientResume_lock = new Object();

  public ConnectionUtils2Test() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("createBitcoinProtocolServer");
    final AbstractBitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory
            = new MockBitcoinProtocolMessageHandlerFactory(clientResume_lock);
    serverBootstrap = ConnectionUtils.createBitcoinProtocolServer(
            SERVER_PORT,
            bitcoinProtocolMessageHandlerFactory,
            EXECUTOR, // bossExecutor
            EXECUTOR); // workerExecutor
    Logger.getLogger(BitcoinProtocolEncoder.class).setLevel(Level.DEBUG);
    Logger.getLogger(BitcoinProtocolDecoder.class).setLevel(Level.DEBUG);
    Logger.getLogger(BitcoinSerializer.class).setLevel(Level.DEBUG);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    LOGGER.info("tearDownClass");
    ConnectionUtils.closeBitcoinProtocolServer(serverBootstrap);  // sometimes hangs up
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
   * Test of openBitcoinProtocolConnection method, of class ConnectionUtils.
   */
  @Test
  public void testOpenBitcoinProtocolConnection() {
    LOGGER.info("openBitcoinProtocolConnection");
    InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", SERVER_PORT);
    final AbstractBitcoinProtocolMessageHandler bitcoinProtocolMessageHandler
            = new MockBitcoinProtocolMessageHandler(null);
    final Channel channel = ConnectionUtils.openBitcoinProtocolConnection(
            inetSocketAddress,
            bitcoinProtocolMessageHandler,
            EXECUTOR, // bossExecutor
            EXECUTOR); // workerExecutor
    assertNotNull(channel);
    assertEquals("localhost/127.0.0.1:8088", channel.getRemoteAddress().toString());
    // send a message
    final Message versionMessage = new VersionMessage(new MainNetParams(), 1);
    LOGGER.info("sending version message to the server\n" + versionMessage);
    final ChannelFuture channelFuture = channel.write(versionMessage);

    // wait for the outbound version message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      fail(channelFuture.getCause().getMessage());
    }

    // the message response handler will signal this thread when the test exchanges are completed
    LOGGER.info("waiting for server to receive the message ...");
    synchronized (clientResume_lock) {
      try {
        clientResume_lock.wait();
      } catch (InterruptedException ex) {
      }
    }
    LOGGER.info("releasing Bitcoin protocol client resources");
    channel.close();
  }

}
