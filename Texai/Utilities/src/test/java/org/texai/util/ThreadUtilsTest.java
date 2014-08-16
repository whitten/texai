/*
 * ThreadUtilsTest.java
 *
 * Created on Jun 30, 2008, 12:21:48 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 26, 2010 reed.
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

import java.util.List;
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
public class ThreadUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ThreadUtilsTest.class);

  public ThreadUtilsTest() {
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
   * Test of getAllThreads method, of class ThreadUtils.
   */
  @Test
  public void testGetAllThreads() {
    LOGGER.info("getAllThreads");
    List<Thread> result = ThreadUtils.getAllThreads();
    LOGGER.info("  all threads: " + result.toString());
    assertTrue(result.toString().contains("main"));
    assertTrue(result.toString().contains("Reference Handler"));
    assertTrue(result.toString().contains("Finalizer"));
    assertTrue(result.toString().contains("Signal Dispatcher"));
  }

  /**
   * Test of getRootThreadGroup method, of class ThreadUtils.
   */
  @Test
  public void testGetRootThreadGroup() {
    LOGGER.info("getRootThreadGroup");
    ThreadGroup result = ThreadUtils.getRootThreadGroup();
    LOGGER.info("  root thread group: " + result.toString());
    assertEquals("system", result.getName());
  }

  /**
   * Test of logThreads method, of class ThreadUtils.
   */
  @Test
  public void testLogThreads() {
    LOGGER.info("logThreads");
    ThreadUtils.logThreads();
  }
}
