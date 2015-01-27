/*
 * AlbusHCNMessageClientPipelineFactoryTest.java
 *
 * Created on Jun 30, 2008, 9:50:09 PM
 *
 * Description: Configures a given pipeline, or initializes a new pipeline, so that it consists of two handlers:
 * (1) SslHandler, (2) AlbusHCNMessageHandler.
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

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.network.netty.handler.MockAlbusHCSMessageHandler;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class AlbusHCNMessageClientPipelineFactoryTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(AlbusHCNMessageClientPipelineFactoryTest.class);

  public AlbusHCNMessageClientPipelineFactoryTest() {
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
   * Test of getPipeline method, of class AlbusHCNMessageClientPipelineFactory.
   */
  @Test
  public void testGetPipeline() {
    LOGGER.info("getPipeline");
    // test creation of new pipeline
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    ChannelPipeline channelPipeline = AlbusHCNMessageClientPipelineFactory.getPipeline(
           new MockAlbusHCSMessageHandler(null, 0),
            x509SecurityInfo);
    assertEquals("DefaultChannelPipeline{(ssl = org.jboss.netty.handler.ssl.SslHandler), (decoder = org.texai.network.netty.handler.TaggedObjectDecoder), (encoder = org.texai.network.netty.handler.TaggedObjectEncoder), (albus-handler = org.texai.network.netty.handler.MockAlbusHCSMessageHandler)}", channelPipeline.toString());

  }
}
