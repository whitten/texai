/*
 * EnvironmentUtilsTest.java
 *
 * Created on Jun 30, 2008, 8:10:15 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 25, 2010 reed.
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
public class EnvironmentUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(EnvironmentUtilsTest.class);

  public EnvironmentUtilsTest() {
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
   * Test of logRuntimeEnvironment method, of class EnvironmentUtils.
   */
  @Test
  public void testIsCorrectJavaVersion() {
    LOGGER.info("isCorrectJavaVersion");
    assertTrue(EnvironmentUtils.isCorrectJavaVersion());
  }

  /**
   * Test of logRuntimeEnvironment method, of class EnvironmentUtils.
   */
  @Test
  public void testLogRuntimeEnvironment() {
    LOGGER.info("logRuntimeEnvironment");
    EnvironmentUtils.logRuntimeEnvironment();
  }

  /**
   * Test of logSystemProperties method, of class EnvironmentUtils.
   */
  @Test
  public void testLogSystemProperties() {
    LOGGER.info("logSystemProperties");
    EnvironmentUtils.logSystemProperties();
  }

  /**
   * Test of certificateServerHost method, of class NetworkUtils.
   */
  @Test
  public void testCertificateServerHost() {
    LOGGER.info("certificateServerHost");
    final String certificateServerHost = EnvironmentUtils.certificateServerHost();
    LOGGER.info("certificateServerHost: " + certificateServerHost);
    assertTrue(
            certificateServerHost.equals("turing") ||
            certificateServerHost.equals("localhost") ||
            certificateServerHost.startsWith("192.168"));
  }

  /**
   * Test of logSystemMonitor method, of class EnvironmentUtils.
   */
  @Test
  public void testLogSystemMonitor() {
    LOGGER.info("logSystemMonitor");
    EnvironmentUtils.logSystemMonitor();
  }

}
