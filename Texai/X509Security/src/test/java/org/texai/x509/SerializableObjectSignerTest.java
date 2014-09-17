/*
 * SerializableObjectSignerTest.java
 *
 * Created on Jun 30, 2008, 3:22:40 PM
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
package org.texai.x509;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import javax.net.ssl.X509KeyManager;
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
public class SerializableObjectSignerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SerializableObjectSignerTest.class);
  /** the X509 certificate */
  private static X509Certificate x509Certificate;
  /** the private key */
  private static PrivateKey privateKey;

  public SerializableObjectSignerTest() {
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
    LOGGER.info("client certificate: " + x509Certificate);
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
   * Test of sign method, of class SerializableObjectSigner.
   */
  @Test
  public void testSign1() {
    LOGGER.info("sign");
    Serializable serializableObject = new MySerializableObject("abc", new InetSocketAddress("192.168.0.10", 443));
    assertEquals("abc /192.168.0.10:443", serializableObject.toString());
    byte[] signatureBytes = null;
    try {
      signatureBytes = SerializableObjectSigner.sign(serializableObject, privateKey);
    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(signatureBytes);
    LOGGER.info("verify");
    try {
      assertTrue(SerializableObjectSigner.verify(serializableObject, x509Certificate, signatureBytes));
      Serializable serializableObject2 = new MySerializableObject("abc", new InetSocketAddress("192.168.0.10", 443));
      assertTrue(SerializableObjectSigner.verify(serializableObject2, x509Certificate, signatureBytes));
      Serializable serializableObject3 = new MySerializableObject("def", new InetSocketAddress("192.168.0.10", 443));
      assertFalse(SerializableObjectSigner.verify(serializableObject3, x509Certificate, signatureBytes));
    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of sign method, of class SerializableObjectSigner.  When the signature bytes are embedded in the serializable
   * object, then then that field must be null when the signature is calculated or verified.
   */
  @Test
  public void testSign2() {
    LOGGER.info("sign");
    MySerializableObject serializableObject = new MySerializableObject("abc", new InetSocketAddress("192.168.0.10", 443));
    assertEquals("abc /192.168.0.10:443", serializableObject.toString());
    byte[] signatureBytes = null;
    try {
      signatureBytes = SerializableObjectSigner.sign(serializableObject, privateKey);
    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(signatureBytes);
    serializableObject.signatureBytes = signatureBytes;
    LOGGER.info("verify");
    try {
      assertFalse(SerializableObjectSigner.verify(serializableObject, x509Certificate, signatureBytes));
      serializableObject.signatureBytes = null;
      assertTrue(SerializableObjectSigner.verify(serializableObject, x509Certificate, signatureBytes));
    } catch (IOException | InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
      fail(ex.getMessage());
    }
  }

  static class MySerializableObject implements Serializable {

    /** the serial version UID */
    private static final long serialVersionUID = 1L;
    /** a test string field */
    private final String string;
    /** a test InetSocketAddress field */
    private final InetSocketAddress inetSocketAddress;
    /** the signature bytes */
    private byte[] signatureBytes;

    /** Constructs a new MySerializableObject instance.
     *
     * @param string a test string field
     * @param inetSocketAddress a test InetSocketAddress field
     */
    MySerializableObject(final String string, final InetSocketAddress inetSocketAddress) {
      //Preconditions
      assert string != null;
      assert inetSocketAddress != null;

      this.string = string;
      this.inetSocketAddress = inetSocketAddress;
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return string + " " + inetSocketAddress;
    }
  }
}
