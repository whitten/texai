/*
 * HTTPSClientPipelineFactoryTest.java
 *
 * Created on Jun 30, 2008, 1:14:38 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 8, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.handler.MockHTTPResponseHandler;
import org.texai.x509.KeyStoreUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class HTTPSClientPipelineFactoryTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(HTTPSClientPipelineFactoryTest.class);

  public HTTPSClientPipelineFactoryTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
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
   * Test of configurePipeline method, of class HTTPSClientPipelineFactory.
   */
  @Test
  public void testConfigureClientPipeline() {
    LOGGER.info("configurePipeline");
    final AbstractHTTPResponseHandler httpResponseHandler = new MockHTTPResponseHandler(null);
    // test creation of new pipeline
    final X509SecurityInfo x509SecurityInfo = KeyStoreUtils.getClientX509SecurityInfo();
    ChannelPipeline channelPipeline = HTTPSClientPipelineFactory.getPipeline(
            httpResponseHandler,
            x509SecurityInfo);
    assertEquals("DefaultChannelPipeline{(ssl = org.jboss.netty.handler.ssl.SslHandler), (encoder = org.jboss.netty.handler.codec.http.HttpRequestEncoder), (decoder = org.jboss.netty.handler.codec.http.HttpResponseDecoder), (aggregator = org.jboss.netty.handler.codec.http.HttpChunkAggregator), (http-handler = org.texai.network.netty.handler.MockHTTPResponseHandler)}", channelPipeline.toString());
  }

}
