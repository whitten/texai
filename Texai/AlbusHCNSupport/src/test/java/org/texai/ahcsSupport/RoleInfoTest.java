/*
 * RoleInfoTest.java
 *
 * Created on Jun 30, 2008, 2:27:39 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 13, 2010 reed.
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
package org.texai.ahcsSupport;

import java.security.PrivateKey;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;
import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.net.ssl.X509KeyManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class RoleInfoTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RoleInfoTest.class);
  /** the certificate chain */
  private static X509Certificate[] certificateChain;
  /** the private key of the role */
  private static PrivateKey privateKey;

  public RoleInfoTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    final X509KeyManager x509KeyManager = (X509KeyManager) x509SecurityInfo.getKeyManagers()[0];
    certificateChain = x509KeyManager.getCertificateChain(X509Utils.ENTRY_ALIAS);
    assertNotNull(certificateChain);
    LOGGER.info("certificate chain length:\n" + certificateChain.length);
    assertEquals(2, certificateChain.length);
    LOGGER.info("end entity certificate: " + certificateChain[0]);
    LOGGER.info("trusted root certificate:\n" + certificateChain[1]);
    privateKey = x509KeyManager.getPrivateKey(X509Utils.ENTRY_ALIAS);
    assertNotNull(privateKey);
    LOGGER.info("private key: " + privateKey);
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
   * Test of getRoleId method, of class RoleInfo.
   */
  @Test
  @SuppressWarnings("null")
  public void testGetRoleId() {
    LOGGER.info("getRoleId");
    final URI roleId = new URIImpl(Constants.TEXAI_NAMESPACE + X509Utils.getUUID(certificateChain[0]).toString());
    assertEquals("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977", roleId.getLocalName());
    final UUID localAreaNetworkID = UUID.randomUUID();
    final String externalHostName = "texai.dyndns.org";
    final int externalPort = 5048;
    final String internalHostName = "turing";
    final int internalPort = 50000;
    CertificateFactory certificateFactory = null;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }
    final List<X509Certificate> x509Certificates = new ArrayList<>();
    for (final X509Certificate x509Certificate : certificateChain) {
      x509Certificates.add(x509Certificate);
    }
    assertEquals(2, x509Certificates.size());
    CertPath certPath = null;
    try {
      certPath = certificateFactory.generateCertPath(x509Certificates);
      assertEquals(2, certPath.getCertificates().size());
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }

    RoleInfo instance = new RoleInfo(
            roleId,
            certPath,
            privateKey,
            localAreaNetworkID,
            externalHostName,
            externalPort,
            internalHostName,
            internalPort);
    URI result = instance.getRoleId();
    assertEquals("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977", result.getLocalName());
  }

  /**
   * Test of getCertPath method, of class RoleInfo.
   */
  @Test
  @SuppressWarnings("null")
  public void testGetCertPath() {
    LOGGER.info("getCertPath");
    final URI roleId = new URIImpl(Constants.TEXAI_NAMESPACE + X509Utils.getUUID(certificateChain[0]).toString());
    assertEquals("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977", roleId.getLocalName());
    final UUID localAreaNetworkID = UUID.fromString("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977");
    final String externalHostName = "texai.dyndns.org";
    final int externalPort = 5048;
    final String internalHostName = "turing";
    final int internalPort = 50000;
    CertificateFactory certificateFactory = null;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }
    final List<X509Certificate> x509Certificates = new ArrayList<>();
    for (final X509Certificate x509Certificate : certificateChain) {
      x509Certificates.add(x509Certificate);
    }
    CertPath certPath = null;
    try {
      certPath = certificateFactory.generateCertPath(x509Certificates);
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }

    RoleInfo instance = new RoleInfo(
            roleId,
            certPath,
            privateKey,
            localAreaNetworkID,
            externalHostName,
            externalPort,
            internalHostName,
            internalPort);
    CertPath result = instance.getCertPath();
    LOGGER.info("certificate path:\n" + result.toString());
    assertNotNull(result);
  }

  /**
   * Test of toString method, of class RoleInfo.
   */
  @Test
  @SuppressWarnings("null")
  public void testToString() {
    LOGGER.info("toString");
    final URI roleId = new URIImpl(Constants.TEXAI_NAMESPACE + X509Utils.getUUID(certificateChain[0]).toString());
    assertEquals("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977", roleId.getLocalName());
    final UUID localAreaNetworkID = UUID.fromString("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977");
    final String externalHostName = "texai.dyndns.org";
    final int externalPort = 5048;
    final String internalHostName = "turing";
    final int internalPort = 50000;
    CertificateFactory certificateFactory = null;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }
    final List<X509Certificate> x509Certificates = new ArrayList<>();
    for (final X509Certificate x509Certificate : certificateChain) {
      x509Certificates.add(x509Certificate);
    }
    CertPath certPath = null;
    try {
      certPath = certificateFactory.generateCertPath(x509Certificates);
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }

    RoleInfo instance = new RoleInfo(
            roleId,
            certPath,
            privateKey,
            localAreaNetworkID,
            externalHostName,
            externalPort,
            internalHostName,
            internalPort);
    assertEquals("[RoleInfo http://texai.org/texai/b19d4261-3e4f-47c5-9d9b-6db6cdcfb977, lan: b19d4261-3e4f-47c5-9d9b-6db6cdcfb977, external host address: texai.dyndns.org:5048, internal host address: turing:50000]", instance.toString());
  }

  /**
   * Test of verify method, of class RoleInfo.
   */
  @Test
  @SuppressWarnings("null")
  public void testGetSignature() {
    LOGGER.info("getSignature");
    final URI roleId = new URIImpl(Constants.TEXAI_NAMESPACE + X509Utils.getUUID(certificateChain[0]).toString());
    assertEquals("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977", roleId.getLocalName());
    final UUID localAreaNetworkID = UUID.fromString("b19d4261-3e4f-47c5-9d9b-6db6cdcfb977");
    final String externalHostName = "texai.dyndns.org";
    final int externalPort = 5048;
    final String internalHostName = "turing";
    final int internalPort = 50000;
    CertificateFactory certificateFactory = null;
    try {
      certificateFactory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }
    final List<X509Certificate> x509Certificates = new ArrayList<>();
    for (final X509Certificate x509Certificate : certificateChain) {
      x509Certificates.add(x509Certificate);
    }
    CertPath certPath = null;
    try {
      certPath = certificateFactory.generateCertPath(x509Certificates);
    } catch (CertificateException ex) {
      fail(ex.getMessage());
    }

    RoleInfo instance = new RoleInfo(
            roleId,
            certPath,
            privateKey,
            localAreaNetworkID,
            externalHostName,
            externalPort,
            internalHostName,
            internalPort);
    try {
      assertTrue(instance.verify());
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }
}
