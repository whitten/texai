/*
 * SerialExecutorTest.java
 *
 * Created on Jun 30, 2008, 6:04:42 PM
 *
 * Description: .
 *
 * Copyright (C) Mar 17, 2010 reed.
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
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class SerialExecutorTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SerialExecutorTest.class);
  /** the executor service */
  private final ExecutorService executorService = Executors.newCachedThreadPool();
  /** the task counter */
  private static AtomicInteger counter = new AtomicInteger(0);

  public SerialExecutorTest() {
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
   * Test of execute method, of class SerialExecutor.
   */
  @Test
  public void testExecute() {
    LOGGER.info("execute");
    SerialExecutor instance = new SerialExecutor(executorService);
    for (int i = 0; i < 100; i++) {
      instance.execute(new TestTask());
    }
  }

  /** Provides a test task runnable that increments and logs the task counter. */
  static class TestTask implements Runnable {

    /** Constructs a new TestTask instance. */
    TestTask() {
    }

    /** Executes the test task. */
    @Override
    public void run() {
      counter.addAndGet(1);
      LOGGER.info("counter: " + counter.toString());
    }
  }
}
