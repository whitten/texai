/*
 * HTTPClientPipelineFactoryTest.java
 *
 * Created on Jun 30, 2008, 1:14:38 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 8, 2010 reed.
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
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class HTTPClientPipelineFactoryTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPClientPipelineFactoryTest.class);

  public HTTPClientPipelineFactoryTest() {
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
   * Test of configurePipeline method, of class HTTPClientPipelineFactory.
   */
  @Test
  public void testConfigureClientPipeline() {
    LOGGER.info("configurePipeline");
    final AbstractHTTPResponseHandler httpResponseHandler = new MockHTTPResponseHandler(null);
    // test creation of new pipeline
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    ChannelPipeline channelPipeline = HTTPClientPipelineFactory.getPipeline(
            httpResponseHandler,
            x509SecurityInfo);
    assertEquals("DefaultChannelPipeline{(ssl = org.jboss.netty.handler.ssl.SslHandler), (encoder = org.jboss.netty.handler.codec.http.HttpRequestEncoder), (decoder = org.jboss.netty.handler.codec.http.HttpResponseDecoder), (aggregator = org.jboss.netty.handler.codec.http.HttpChunkAggregator), (http-handler = org.texai.network.netty.handler.MockHTTPResponseHandler)}", channelPipeline.toString());
  }

}
