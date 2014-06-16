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
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
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

  /** the logger */
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
   * Test of getTrustStore method, of class X509SecurityInfo.
   */
  @Test
  public void testGetTrustStore() {
    LOGGER.info("getTrustStore");
    X509SecurityInfo instance = KeyStoreTestUtils.getClientX509SecurityInfo();
    KeyStore result = instance.getTrustStore();
    assertNotNull(result);
    try {
      LOGGER.info("truststore alias: " + result.aliases());
      final Enumeration<String> aliasEnumeration = result.aliases();
      while (aliasEnumeration.hasMoreElements()) {
        LOGGER.info("truststore alias: " + aliasEnumeration.nextElement());
      }
      assertTrue(result.containsAlias(X509Utils.TRUSTSTORE_ENTRY_ALIAS));
    } catch (KeyStoreException ex) {
      fail(ex.getMessage());
    }
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
      assertTrue(result.containsAlias(X509Utils.ENTRY_ALIAS));
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
    assertNotNull(x509KeyManager.getCertificateChain(X509Utils.ENTRY_ALIAS));
    assertNotNull(x509KeyManager.getPrivateKey(X509Utils.ENTRY_ALIAS));
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
    X509Certificate result = instance.getX509Certificate();
    assertNotNull(result);
    assertEquals("CN=texai.org, UID=b31650cd-0efd-4b87-b347-62bf6a5b0db0", result.getSubjectX500Principal().toString());
  }

  /**
   * Test of getCertPath method, of class X509SecurityInfo.
   */
  @Test
  public void testGetCertPath() {
    LOGGER.info("getCertPath");
    X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();

    try {

      //  to turn on java security debugging, specify java.security.debug=certpath in the Texai POM

      final KeyStore keyStore = x509SecurityInfo.getKeyStore();
      final X509Certificate clientX509Certificate = (X509Certificate) keyStore.getCertificate(X509Utils.ENTRY_ALIAS);
      assertNotNull(clientX509Certificate);
      assertTrue(clientX509Certificate.getSubjectX500Principal().toString().indexOf("CN=texai.org") > -1);
      Certificate[] certificateChain = keyStore.getCertificateChain(X509Utils.ENTRY_ALIAS);
      assertEquals(2, certificateChain.length);
      assertEquals(clientX509Certificate, certificateChain[0]);
      final Certificate rootX509Certificate = certificateChain[1];
      assertTrue(rootX509Certificate instanceof X509Certificate);
      assertEquals("CN=texai.org, O=Texai Certification Authority, UID=233bfdb2-9287-4b41-b304-eb121ea7c4de", ((X509Certificate) rootX509Certificate).getSubjectX500Principal().toString());
      //assertTrue(X509CertImpl.isSelfIssued((X509Certificate) rootX509Certificate));
      assertEquals(3, ((X509Certificate) rootX509Certificate).getVersion());

      CertPath certPath = x509SecurityInfo.getCertPath();
      LOGGER.info("certPath: " + certPath);
      assertNotNull(certPath);
      @SuppressWarnings("unchecked")
      final List<X509Certificate> certificates = (List<X509Certificate>) certPath.getCertificates();
      assertEquals(1, certificates.size());
      assertEquals(clientX509Certificate, certificates.get(0));
      X509Utils.validateCertificatePath(certPath);
    } catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }
}
