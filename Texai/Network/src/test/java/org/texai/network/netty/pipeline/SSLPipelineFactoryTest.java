/*
 * SSLPipelineFactoryTest.java
 *
 * Created on Jun 30, 2008, 6:49:43 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 4, 2010 reed.
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
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class SSLPipelineFactoryTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SSLPipelineFactoryTest.class);


  public SSLPipelineFactoryTest() {
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
   * Test of getPipeline method, of class SSLPipelineFactory.
   */
  @Test
  public void testGetPipeline() {
    LOGGER.info("getPipeline");
    final boolean useClientMode = true;
    // test creation of new pipeline
    KeyStoreTestUtils.initializeClientKeyStore();
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    LOGGER.info("getting channelPipeline");
    ChannelPipeline channelPipeline = SSLPipelineFactory.getPipeline(
          useClientMode,
          x509SecurityInfo,
          true); // needClientAuth
    assertEquals("DefaultChannelPipeline{(ssl = org.jboss.netty.handler.ssl.SslHandler)}", channelPipeline.toString());
  }

 }
