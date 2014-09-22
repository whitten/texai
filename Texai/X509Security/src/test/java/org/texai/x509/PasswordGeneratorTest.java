/*
 * PasswordGeneratorTest.java
 *
 * Created on Jun 30, 2008, 11:20:48 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 26, 2010 reed.
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
package org.texai.x509;

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
public class PasswordGeneratorTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(PasswordGeneratorTest.class);

  public PasswordGeneratorTest() {
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
   * Test of getNbrCombinations method, of class PasswordGenerator.
   */
  @Test
  public void testGetNbrCombinations() {
    LOGGER.info("getNbrCombinations");
    PasswordGenerator instance = new PasswordGenerator();
    double result = instance.getNbrCombinations(10);
    LOGGER.info("number of combinations, length 10: " + result);
    result = instance.getNbrCombinations(20);
    LOGGER.info("number of combinations, length 20: " + result);
  }

  /**
   * Test of generate method, of class PasswordGenerator.
   */
  @Test
  public void testGenerate() {
    LOGGER.info("generate");
    int length = 10;
    PasswordGenerator instance = new PasswordGenerator();
    String result = instance.generate(length);
    assertNotNull(result);
    assertEquals(length, result.length());
    LOGGER.info("password: " + result);

    length = 15;
    result = instance.generate(length);
    assertNotNull(result);
    assertEquals(length, result.length());
    LOGGER.info("password: " + result);

    result = instance.generate(length);
    assertNotNull(result);
    assertEquals(length, result.length());
    LOGGER.info("password: " + result);

    result = instance.generate(length);
    assertNotNull(result);
    assertEquals(length, result.length());
    LOGGER.info("password: " + result);

    result = instance.generate(length);
    assertNotNull(result);
    assertEquals(length, result.length());
    LOGGER.info("password: " + result);
  }
}
