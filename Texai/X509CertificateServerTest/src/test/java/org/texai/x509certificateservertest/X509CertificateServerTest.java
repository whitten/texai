/*
 * X509CertificateServerTest.java
 *
 * Created on May 14, 2010, 3:32:25 PM
 *
 * Description: .
 *
 * Copyright (C) May 14, 2010, Stephen L. Reed.
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
package org.texai.x509certificateservertest;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
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
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.PortUnificationHandler;
import org.texai.network.netty.pipeline.HTTPClientPipelineFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.network.netty.pipeline.SSLPipelineFactory;
import org.texai.ssl.TexaiSSLContextFactory;
import org.texai.util.Base64Coder;
import org.texai.util.ByteUtils;
import org.texai.util.EnvironmentUtils;
import org.texai.util.NetworkUtils;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class X509CertificateServerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(X509CertificateServerTest.class);
  /** the server port */
  private static final int SERVER_PORT = 443;
  /** the number certificates served */
  private final AtomicInteger nbrKeyPairsGenerated = new AtomicInteger(0);
  /** the certificate generation duration milliseconds */
  private final AtomicLong keyPairGenerationDurationMillis = new AtomicLong(0L);
  // for SSL debugging
//  static {
//    System.setProperty("javax.net.debug", "all");
//  }

  /** Constructs a new X509CertificateServerTest instance. */
  public X509CertificateServerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    /** sets debugging */
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
  public void testX509CertificateServer() {
    LOGGER.info("testX509CertificateServer");
    Logger.getLogger(PortUnificationHandler.class).setLevel(Level.WARN);
    Logger.getLogger(PortUnificationChannelPipelineFactory.class).setLevel(Level.WARN);

    Logger.getLogger(HTTPRequestHandler.class).setLevel(Level.WARN);
    Logger.getLogger(TexaiSSLContextFactory.class).setLevel(Level.WARN);
    Logger.getLogger(SSLPipelineFactory.class).setLevel(Level.WARN);
    Logger.getLogger(HTTPClientPipelineFactory.class).setLevel(Level.WARN);

    final String host = EnvironmentUtils.certificateServerHost();
    LOGGER.info("certificate server host: " + host);
    assertTrue(NetworkUtils.isHostAvailable(host, SERVER_PORT));

    // test chat server with mock clients
    int nbrThreads;
    if (X509Utils.isTrustedDevelopmentSystem()) {
      nbrThreads = Runtime.getRuntime().availableProcessors();
      LOGGER.info("launching " + nbrThreads + " test thread(s");
    } else {
      nbrThreads = 1;
      LOGGER.info("launching 1 test thread");
    }
    final CountDownLatch countDownLatch = new CountDownLatch(nbrThreads);
    for (int i = 0; i < nbrThreads; i++) {
      final MockHTTPClientTask mockHTTPClientTask = new MockHTTPClientTask(host, countDownLatch, i + 1);
      new Thread(mockHTTPClientTask).start();
    }

    try {
      countDownLatch.await();
    } catch (InterruptedException ex) {
      LOGGER.info(ex.getMessage());
    }

    LOGGER.info("nbr of key pairs generated: " + nbrKeyPairsGenerated.toString());
    LOGGER.info("key pair duration milliseconds: " + keyPairGenerationDurationMillis.toString());
    final long averageKeyPairGenerationMillis =
            keyPairGenerationDurationMillis.get() / nbrKeyPairsGenerated.longValue();
    LOGGER.info("average duration per key pair generated: " + averageKeyPairGenerationMillis);

  }

  /** Provides a task that runs a mock HTTP client for a certain number of iterations. */
  class MockHTTPClientTask implements Runnable {

    final String host;
    final CountDownLatch countDownLatch;
    final int threadNbr;
    int requestNbr = 0;

    MockHTTPClientTask(
            final String host,
            final CountDownLatch countDownLatch,
            final int threadNbr) {
      this.host = host;
      this.countDownLatch = countDownLatch;
      this.threadNbr = threadNbr;
    }

    @Override
    public void run() {
      Thread.currentThread().setName("thread " + threadNbr);
      LOGGER.info("**** starting " + Thread.currentThread().getName());
      //for (int i = 0; i < 1; i++) {
      for (int i = 0; i < 20; i++) {
        mockHTTPClient();
        LOGGER.debug(Thread.currentThread().getName() + " generated certificate " + String.valueOf(i + 1));
      }
      LOGGER.info("**** finishing " + Thread.currentThread().getName());
      countDownLatch.countDown();
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
      try {
        LOGGER.debug("trusted certificate: " + x509SecurityInfo.getTrustStore().getCertificate(X509Utils.TRUSTSTORE_ENTRY_ALIAS).toString());
      } catch (KeyStoreException ex) {
        ex.printStackTrace();
        fail(ex.getMessage());
      }
      final ChannelPipeline channelPipeline = HTTPClientPipelineFactory.getPipeline(
              httpResponseHandler,
              x509SecurityInfo);
      clientBootstrap.setPipeline(channelPipeline);
      LOGGER.debug("client pipeline: " + channelPipeline.toString());

      // start the connection attempt
      SocketAddress socketAddress = new InetSocketAddress(host, SERVER_PORT);
      ChannelFuture channelFuture = clientBootstrap.connect(socketAddress);

      // wait until the connection attempt succeeds or fails
      final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
      if (!channelFuture.isSuccess()) {
        channelFuture.getCause().printStackTrace();
        fail(channelFuture.getCause().getMessage());
      }
      LOGGER.debug(Thread.currentThread().getName() + " connected to " + socketAddress);

      URI uri = null;
      HttpRequest httpRequest;

      // send the certificate request
      try {
        uri = new URI("https://" + host + ":" + SERVER_PORT + "/CA/certificate-request");
      } catch (URISyntaxException ex) {
        fail(ex.getMessage());
      }
      httpRequest = new DefaultHttpRequest(
              HttpVersion.HTTP_1_1,
              HttpMethod.POST,
              uri.toASCIIString());
      httpRequest.setHeader(HttpHeaders.Names.HOST, host);
      httpRequest.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
      httpRequest.setHeader(HttpHeaders.Names.CONTENT_TRANSFER_ENCODING, HttpHeaders.Values.BINARY);
      httpRequest.setHeader(HttpHeaders.Names.USER_AGENT, Thread.currentThread().getName());

      final long startTimeMillis = System.currentTimeMillis();
      KeyPair clientKeyPair = null;
      try {
        clientKeyPair = X509Utils.generateRSAKeyPair2048();
      } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
        fail(ex.getMessage());
      }
      assertNotNull(clientKeyPair);
      nbrKeyPairsGenerated.getAndIncrement();
      keyPairGenerationDurationMillis.addAndGet(System.currentTimeMillis() - startTimeMillis);
      final byte[] serializedClientPublicKey = ByteUtils.serialize(clientKeyPair.getPublic());
      final char[] base64SerializedClientPublicKey = Base64Coder.encode(serializedClientPublicKey);
      LOGGER.debug("base64SerializedClientPublicKey length: " + base64SerializedClientPublicKey.length);
      final String base64SerializedClientPublicKeyString = new String(base64SerializedClientPublicKey);
      final ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(base64SerializedClientPublicKeyString.getBytes());
      LOGGER.debug("content: " + new String(channelBuffer.array()));
      LOGGER.debug("content length: " + channelBuffer.array().length);
      httpRequest.setContent(channelBuffer);
      httpRequest.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.array().length));
      channel.write(httpRequest);

      // wait for the request message to be sent
      channelFuture.awaitUninterruptibly();
      if (!channelFuture.isSuccess()) {
        channelFuture.getCause().printStackTrace();
        fail(channelFuture.getCause().getMessage());
      }

      // the message response handler will signal this thread when the test exchange is completed
      LOGGER.debug(Thread.currentThread().getName() + " client waiting for server to process the request");
      synchronized (clientResume_lock) {
        try {
          clientResume_lock.wait();
        } catch (InterruptedException ex) {
        }
      }
      LOGGER.debug("client releasing HTTP resources");
      channel.close();
      clientBootstrap.releaseExternalResources();
    }
  }
}
