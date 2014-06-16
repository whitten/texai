/*
 * FileSignerTest.java
 *
 * Created on Jun 30, 2008, 1:35:45 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 27, 2010 reed.
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

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509KeyManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.util.ByteUtils;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class FileSignerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(FileSignerTest.class);
  /** the X509 certificate */
  private static X509Certificate x509Certificate;
  /** the private key */
  private static PrivateKey privateKey;

  public FileSignerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
    final X509KeyManager x509KeyManager = (X509KeyManager) x509SecurityInfo.getKeyManagers()[0];
    final X509Certificate[] certificateChain = x509KeyManager.getCertificateChain(X509Utils.ENTRY_ALIAS);
    assertNotNull(certificateChain);
    LOGGER.info("certificate chain length:\n" + certificateChain.length);
    assertEquals(2, certificateChain.length);
    x509Certificate = certificateChain[0];
    LOGGER.info("certificate: " + x509Certificate);
    privateKey = x509KeyManager.getPrivateKey(X509Utils.ENTRY_ALIAS);
    assertNotNull(privateKey);
    LOGGER.info("private key: " + privateKey);
    x509Certificate.checkValidity();
    // validate the certificate with the issuer's public key
    x509Certificate.verify(certificateChain[1].getPublicKey());
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
   * Test of sign method, of class FileSigner.
   */
  @Test
  public void testSign() {
    LOGGER.info("testSign");
    try {
      LOGGER.info("signing file");
      final String datafile = "data/SignatureTest.txt";
      byte[] signatureBytes = FileSigner.sign(datafile, privateKey);
      System.out.println("Signature(in hex):: " + ByteUtils.toHex(signatureBytes));

      LOGGER.info("verifying file");
      boolean result = FileSigner.verify(datafile, x509Certificate, signatureBytes);
      System.out.println("Signature Verification Result = " + x509Certificate);
      assertTrue(result);
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }
}
