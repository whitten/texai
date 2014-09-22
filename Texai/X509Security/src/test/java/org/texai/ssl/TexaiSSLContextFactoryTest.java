/*
 * TexaiSSLContextFactoryTest.java
 *
 * Created on Jun 30, 2008, 12:03:07 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 5, 2010 reed.
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
package org.texai.ssl;

import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
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
public class TexaiSSLContextFactoryTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(TexaiSSLContextFactoryTest.class);

  public TexaiSSLContextFactoryTest() {
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
   * Test of getSSLContext method, of class TexaiSSLContextFactory.
   */
  @Test
  public void testConfigureSSLEngine() {
    LOGGER.info("configureSSLEngine");
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    try {
      final SSLContext sslContext = TexaiSSLContextFactory.getSSLContext(x509SecurityInfo);
      SSLEngine sslEngine = sslContext.createSSLEngine();
      assertFalse(sslEngine.getNeedClientAuth());
      assertFalse(sslEngine.getUseClientMode());
      final List<String> enabledCipherSuites = new ArrayList<>();
      LOGGER.info("default ciphers ...");
      for (final String enabledCipherSuite : sslEngine.getEnabledCipherSuites()) {
        enabledCipherSuites.add(enabledCipherSuite);
        LOGGER.info("  " + enabledCipherSuite);
      }

      // client SSL engine
      boolean useClientMode = true;
      TexaiSSLContextFactory.configureSSLEngine(sslEngine, useClientMode, true);
      assertFalse(sslEngine.getNeedClientAuth());
      assertTrue(sslEngine.getUseClientMode());
      enabledCipherSuites.clear();
      LOGGER.info("configured client ciphers ...");
      for (final String enabledCipherSuite : sslEngine.getEnabledCipherSuites()) {
        enabledCipherSuites.add(enabledCipherSuite);
        LOGGER.info("  " + enabledCipherSuite);
      }

      // server SSL engine
      sslEngine = sslContext.createSSLEngine();
      assertFalse(sslEngine.getNeedClientAuth());
      assertFalse(sslEngine.getUseClientMode());
      useClientMode = false;
      TexaiSSLContextFactory.configureSSLEngine(sslEngine, useClientMode, true);
      assertTrue(sslEngine.getNeedClientAuth());
      assertFalse(sslEngine.getUseClientMode());
      enabledCipherSuites.clear();
      LOGGER.info("configured server ciphers ...");
      for (final String enabledCipherSuite : sslEngine.getEnabledCipherSuites()) {
        enabledCipherSuites.add(enabledCipherSuite);
        LOGGER.info("  " + enabledCipherSuite);
      }
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getSSLContext method, of class TexaiSSLContextFactory.
   */
  @Test
  public void testGetSSLContext() {
    LOGGER.info("getSSLContext");
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    try {
      final SSLContext sslContext = TexaiSSLContextFactory.getSSLContext(x509SecurityInfo);
      assertEquals("TLS", sslContext.getProtocol());
      assertEquals("SunJSSE version 1.8", sslContext.getProvider().toString());
      final SSLEngine sslEngine = sslContext.createSSLEngine();
      assertFalse(sslEngine.getNeedClientAuth());
      sslEngine.setNeedClientAuth(true);
      assertTrue(sslEngine.getNeedClientAuth());
    } catch (Exception ex) {
      fail(ex.getMessage());
    }

  }
}
