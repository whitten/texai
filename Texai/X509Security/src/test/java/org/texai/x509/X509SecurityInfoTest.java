/*
 * X509SecurityInfoTest.java
 *
 * Created on Jun 30, 2008, 3:04:12 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 19, 2010 reed.
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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.X509KeyManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//import sun.security.x509.X509CertImpl;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class X509SecurityInfoTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(X509SecurityInfoTest.class);

  public X509SecurityInfoTest() {
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
   * Test of getKeyStore method, of class X509SecurityInfo.
   */
  @Test
  public void testGetKeyStore() {
    LOGGER.info("getKeyStore");
    X509SecurityInfo instance = KeyStoreTestUtils.getClientX509SecurityInfo();
    KeyStore result = instance.getKeyStore();
    assertNotNull(result);
    try {
      LOGGER.info("key store alias: " + result.aliases());
      final Enumeration<String> aliasEnumeration = result.aliases();
      assertTrue(aliasEnumeration.hasMoreElements());
      while (aliasEnumeration.hasMoreElements()) {
        LOGGER.info("key store alias: " + aliasEnumeration.nextElement());
      }
      assertTrue(result.containsAlias(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
    } catch (KeyStoreException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getKeyManagers method, of class X509SecurityInfo.
   */
  @Test
  public void testGetKeyManagers() {
    LOGGER.info("getKeyManagers");
    X509SecurityInfo instance = KeyStoreTestUtils.getClientX509SecurityInfo();
    KeyManager[] result = instance.getKeyManagers();
    assertNotNull(result);
    assertEquals(1, result.length);
    final KeyManager keyManager = result[0];
    assertTrue(keyManager instanceof X509KeyManager);
    final X509KeyManager x509KeyManager = (X509KeyManager) keyManager;
    assertNotNull(x509KeyManager.getCertificateChain(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
    assertNotNull(x509KeyManager.getPrivateKey(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
  }

  /**
   * Test of getPrivateKey method, of class X509SecurityInfo.
   */
  @Test
  public void testGetPrivateKey() {
    LOGGER.info("getPrivateKey");
    X509SecurityInfo instance = KeyStoreTestUtils.getClientX509SecurityInfo();
    PrivateKey result = instance.getPrivateKey();
    assertNotNull(result);
    LOGGER.info("private key: \n" + result);
  }

  /**
   * Test of getX509Certificate method, of class X509SecurityInfo.
   */
  @Test
  public void testGetX509Certificate() {
    LOGGER.info("getX509Certificate");
    X509SecurityInfo instance = KeyStoreTestUtils.getClientX509SecurityInfo();
    assertNotNull(instance);
    X509Certificate result = instance.getX509Certificate();
    assertNotNull(result);
    LOGGER.info("client test certificate ...\n" + result);
    assertTrue(result.getSubjectX500Principal().toString().contains("CN=texai.org"));
    assertTrue(result.getSubjectX500Principal().toString().contains("DC=test client"));
  }

}
