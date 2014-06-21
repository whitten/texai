/*
 * BitTorrentClientPipelineFactoryTest.java
 *
 * Created on Jun 30, 2008, 5:46:29 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 11, 2010 reed.
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
import static org.junit.Assert.*;
import org.texai.network.netty.handler.AbstractBitTorrentHandler;
import org.texai.network.netty.handler.MockBitTorrentHandler;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;

/**
 *
 * @author reed
 */
public class BitTorrentClientPipelineFactoryTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentClientPipelineFactoryTest.class);

  public BitTorrentClientPipelineFactoryTest() {
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
   * Test of getPipeline method, of class BitTorrentClientPipelineFactory.
   */
  @Test
  public void testGetPipeline() {
    LOGGER.info("getPipeline");
    // test creation of new pipeline
    final AbstractBitTorrentHandler bitTorrentHandler = new MockBitTorrentHandler(
            null, // clientResume_lock,
            0);  // iterationLimit
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    ChannelPipeline channelPipeline = BitTorrentClientPipelineFactory.getPipeline(
            bitTorrentHandler,
            x509SecurityInfo);
    assertEquals("DefaultChannelPipeline{(ssl = org.jboss.netty.handler.ssl.SslHandler), (decoder = org.texai.network.netty.handler.BitTorrentDecoder), (encoder = org.texai.network.netty.handler.BitTorrentEncoder), (torrent-handler = org.texai.network.netty.handler.MockBitTorrentHandler)}", channelPipeline.toString());
  }

}
