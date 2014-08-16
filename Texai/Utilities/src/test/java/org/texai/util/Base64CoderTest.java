/*
 * Base64CoderTest.java
 *
 * Created on Jun 30, 2008, 2:09:06 PM
 *
 * Description: .
 *
 * Copyright (C) Dec 6, 2011 reed.
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
package org.texai.util;

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
public class Base64CoderTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Base64CoderTest.class);

  public Base64CoderTest() {
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
   * Test of decodeEntities method, of class Base64Coder.
   */
  @Test
  public void testDecodeEntities() {
    LOGGER.info("decodeEntities");

    String s = "";
    String expResult = "";
    String result = Base64Coder.decodeEntities(s);
    assertEquals(expResult, result);

    assertEquals("abc", Base64Coder.decodeEntities("abc"));
    assertEquals("ggg==", Base64Coder.decodeEntities("ggg%3D%3D"));
    assertEquals("data:image/png;base64,iVBOR", Base64Coder.decodeEntities("data%3Aimage%2Fpng%3Bbase64%2CiVBOR"));
    assertEquals("type=data&image=data:image/jpeg;base64,/9j/4AAQSk", Base64Coder.decodeEntities("type=data&image=data%3Aimage%2Fjpeg%3Bbase64%2C%2F9j%2F4AAQSk"));
  }

  /**
   * Test of encodeString method, of class Base64Coder.
   */
  @Test
  public void testEncodeString() {
    LOGGER.info("encodeString");
    String s = "abc";
    String expResult = "YWJj";
    String result = Base64Coder.encodeString(s);
    assertEquals(expResult, result);
  }

  /**
   * Test of decodeString method, of class Base64Coder.
   */
  @Test
  public void testDecodeString() {
    LOGGER.info("decodeString");
    String s = "YWJj";
    String expResult = "abc";
    String result = Base64Coder.decodeString(s);
    assertEquals(expResult, result);
  }

  /**
   * Test of encode method, of class Base64Coder.
   */
  @Test
  public void testEncode_byteArr() {
    LOGGER.info("encode");
    byte[] in = "abc".getBytes();
    String result = new String(Base64Coder.encode(in));
    assertEquals("YWJj", result);
  }

  /**
   * Test of encode method, of class Base64Coder.
   */
  @Test
  public void testEncode_byteArr_int() {
    LOGGER.info("encode");
    byte[] in = "abcd".getBytes();
    int iLen = 3;
    String result = new String(Base64Coder.encode(in, iLen));
    assertEquals("YWJj", result);
  }

  /**
   * Test of decode method, of class Base64Coder.
   */
  @Test
  public void testDecode_String() {
    LOGGER.info("decode");
    String s = "YWJj";
    byte[] result = Base64Coder.decode(s);
    assertEquals("abc", new String(result));
  }

  /**
   * Test of decode method, of class Base64Coder.
   */
  @Test
  public void testDecode_charArr() {
    LOGGER.info("decode");
    char[] in = "YWJj".toCharArray();
    byte[] result = Base64Coder.decode(in);
    assertEquals("abc", new String(result));
  }
}
