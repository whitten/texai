/*
 * ExecutorUtilsTest.java
 *
 * Created on Jun 30, 2008, 10:31:10 AM
 *
 * Description: .
 *
 * Copyright (C) Mar 30, 2010 reed.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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
public class ExecutorUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ExecutorUtilsTest.class);

  public ExecutorUtilsTest() {
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
   * Test of getExecutorService method, of class ExecutorUtils.
   */
  @Test
  public void testGetExecutorService() {
    LOGGER.info("getExecutorService");
    ExecutorService result = ExecutorUtils.getExecutorService();
    assertNotNull(result);
    result.execute(new TestRunnable());
    assertFalse(result.isShutdown());
    assertFalse(result.isTerminated());
    result.shutdown();
    try {
      result.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException ex) {
      fail(ex.getMessage());
    }
    assertTrue(result.isShutdown());
    assertTrue(result.isTerminated());
  }

  /** Provides a test Runnable. */
  static class TestRunnable implements Runnable {

    /** Executes this test Runnable. */
    @Override
    public void run() {
      LOGGER.info("executing test runnable");
    }

  }

}
