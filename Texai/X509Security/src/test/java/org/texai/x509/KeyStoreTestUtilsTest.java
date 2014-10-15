/*
 * KeyStoreTestUtilsTest.java
 *
 * Created on Jun 30, 2008, 2:31:32 PM
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
package org.texai.x509;

import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
public class KeyStoreTestUtilsTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(KeyStoreTestUtilsTest.class);

  public KeyStoreTestUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    KeyStoreTestUtils.initializeClientKeyStore();
    KeyStoreTestUtils.initializeServerKeyStore();
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
   * Test of getServerKeyStore method, of class KeyStoreTestUtils.
   */
  @Test
  public void testGetServerKeyStore() {
    LOGGER.info("getStoredServerKeyStore");
    assertEquals("server-keystore-password", new String(KeyStoreTestUtils.getServerKeyStorePassword()));
    KeyStore result = KeyStoreTestUtils.getServerKeyStore();
    assertNotNull(result);
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      assertTrue(result.getProvider().toString().startsWith("BC version "));
      assertEquals("UBER", result.getType());
    } else {
      assertTrue(result.getProvider().toString().startsWith("SunJCE version "));
      assertEquals("JCEKS", result.getType());
    }
    try {
      assertTrue(result.containsAlias(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
      final X509Certificate serverX509Certificate = (X509Certificate) result.getCertificate(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS);
      assertTrue(serverX509Certificate.getSubjectX500Principal().toString().contains("CN=texai.org"));
      Certificate[] certificateChain = result.getCertificateChain(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS);
      assertEquals(1, certificateChain.length);
      assertEquals(serverX509Certificate, certificateChain[0]);
      serverX509Certificate.verify(serverX509Certificate.getPublicKey());
    } catch (InvalidKeyException | KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException | CertificateException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getClientKeyStore method, of class KeyStoreTestUtils.
   */
  @Test
  public void testGetClientKeyStore() {
    LOGGER.info("getClientKeyStore");
    assertEquals("client-keystore-password", new String(KeyStoreTestUtils.getClientKeyStorePassword()));
    KeyStore result = KeyStoreTestUtils.getClientKeyStore();
    assertNotNull(result);
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      assertEquals("UBER", result.getType());
    } else {
      assertEquals("JCEKS", result.getType());
    }
    try {
      assertTrue(result.containsAlias(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
      final X509Certificate clientX509Certificate = (X509Certificate) result.getCertificate(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS);
      assertTrue(clientX509Certificate.getSubjectX500Principal().toString().contains("CN=texai.org"));
      Certificate[] certificateChain = result.getCertificateChain(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS);
      assertEquals(1, certificateChain.length);
      assertEquals(clientX509Certificate, certificateChain[0]);
    } catch (KeyStoreException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getClientX509SecurityInfo() method, of class KeyStoreTestUtils.
   */
  @Test
  public void testGetClientX509SecurityInfo() {
    LOGGER.info("getClientX509SecurityInfo()");
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    LOGGER.info("client x509SecurityInfo...\n" + x509SecurityInfo);
  }

  /**
   * Test of getServerX509SecurityInfo() method, of class KeyStoreTestUtils.
   */
  @Test
  public void testGetServerX509SecurityInfo() {
    LOGGER.info("getServerX509SecurityInfo()");
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    LOGGER.info("server x509SecurityInfo...\n" + x509SecurityInfo);
  }

}
