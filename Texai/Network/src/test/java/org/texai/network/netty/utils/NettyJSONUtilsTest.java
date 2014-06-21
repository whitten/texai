/*
 * NettyJSONUtilsTest.java
 *
 * Created on Jun 30, 2008, 4:17:58 PM
 *
 * Description: .
 *
 * Copyright (C) Jan 31, 2012 reed.
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
package org.texai.network.netty.utils;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class NettyJSONUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NettyJSONUtilsTest.class);

  public NettyJSONUtilsTest() {
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
   * Test of getTexaiSessionCookie method, of class NettyJSONUtils.
   */
  @Test
  public void testGetTexaiSessionCookie() {
    LOGGER.info("getTexaiSessionCookie");
    String jsonText = "{\"type\":\"xxxx\", \"data\":{\"texai-session\":\"9fcad0a9-f2cc-4b5c-9438-c3f2439ecb56\"}}";
    String expResult = "9fcad0a9-f2cc-4b5c-9438-c3f2439ecb56";
    String result = NettyJSONUtils.getTexaiSessionCookie(jsonText);
    assertEquals(expResult, result);
    jsonText = "{\"type\":\"xxxx\", \"data\":{\"yyy\":\"zzz\"}}";
    assertNull(NettyJSONUtils.getTexaiSessionCookie(jsonText));
  }
}
