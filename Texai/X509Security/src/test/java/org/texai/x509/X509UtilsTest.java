/*
 * X509UtilsTest.java
 *
 * Created on Jun 30, 2008, 4:15:56 PM
 *
 * Description: .
 *
 * Copyright (C) Jan 19, 2010 reed.
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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import org.texai.util.StringUtils;
import java.security.SecureRandom;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.KeyUsage;
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
public class X509UtilsTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(X509UtilsTest.class);

  public X509UtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    X509Utils.logProviders();
    for (final Provider provider : Security.getProviders()) {
      X509Utils.logProviderCapabilities(provider.getName());
    }
    //X509Utils.initializeInstallerKeyStore();
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
   * Test of generateX509SecurityInfo method of class X509Utils.
   */
  @Test
  public void testGenerateX509SecurityInfo() {
    LOGGER.info("generateX509SecurityInfo");
    try {
      final String keyStoreFilePath;
      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        keyStoreFilePath = "data/keystore.uber";
      } else {
        keyStoreFilePath = "data/keystore.jceks";
      }
      final File keyStoreFile = new File(keyStoreFilePath);
      if (keyStoreFile.exists()) {
        final boolean isFileDeleted = keyStoreFile.delete();
        if (!isFileDeleted) {
          fail("keystore file not deleted: " + keyStoreFilePath);
        }
      }
      KeyStore keyStore;
      final KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uid = UUID.randomUUID();
      final char[] keyStorePassword = "my-password".toCharArray();
      keyStore = X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
      X509SecurityInfo x509SecurityInfo = X509Utils.generateX509SecurityInfo(
              keyStore,
              keyStorePassword,
              keyPair,
              uid,
              null, // domainComponent
              KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS); // certificateAlias
      assertNotNull(x509SecurityInfo.getCertPath());
      assertNotNull(x509SecurityInfo.getKeyManagers());
      assertNotNull(x509SecurityInfo.getKeyStore());
      assertNotNull(x509SecurityInfo.getPrivateKey());
      assertNotNull(x509SecurityInfo.getX509Certificate());

      LOGGER.info("aliases" + keyStore.aliases());
      try (final FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFilePath)) {
        keyStore.store(fileOutputStream, keyStorePassword);
      }
      assertTrue(keyStore.containsAlias(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
      assertTrue(keyStore.isKeyEntry(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));

      final KeyStore keyStore2 = X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
      assertTrue(keyStore2.containsAlias(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));
      assertTrue(keyStore2.isKeyEntry(KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS));

    } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of isJCEUnlimitedStrengthPolicy method, of class X509Utils.
   */
  @Test
  public void testIsJCEUnlimitedStrengthPolicy() {
    LOGGER.info("isJCEUnlimitedStrengthPolicy");
    try {
      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        LOGGER.info("JCE unlimited strength jurisdiction policy files are installed");
      } else {
        LOGGER.info("JCE unlimited strength jurisdiction policy files are not installed");
      }

    } catch (Exception ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of keyStoreContains method, of class X509Utils.
   */
  @Test
  public void testKeyStoreContains() {
    LOGGER.info("keyStoreContains");
    // delete an existing keystore
    final String keyStoreFilePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      keyStoreFilePath = "data/keystore.uber";
    } else {
      keyStoreFilePath = "data/keystore.jceks";
    }
    final File keyStoreFile = new File(keyStoreFilePath);
    if (keyStoreFile.exists()) {
      final boolean isFileDeleted = keyStoreFile.delete();
      if (!isFileDeleted) {
        fail("keystore file not deleted: " + keyStoreFilePath);
      }
    }
    final char[] keyStorePassword = "password".toCharArray();
    final String alias = "test-alias";
    assertFalse(X509Utils.keyStoreContains(
            keyStoreFilePath,
            keyStorePassword,
            alias));
    try {
      X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
      assertFalse(X509Utils.keyStoreContains(
              keyStoreFilePath,
              keyStorePassword,
              alias));

      // create a certificate path
      final KeyPair keyPair;
      keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uuid = UUID.randomUUID();
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      keyPair,
                      uuid,
                      "TestComponent");
      final List<Certificate> certificateList = new ArrayList<>();
      certificateList.add(x509Certificate);
      CertPath certPath = X509Utils.generateCertPath(certificateList);

      // add the certificate path entry to the keystore
      X509Utils.addEntryToKeyStore(
              keyStoreFilePath,
              keyStorePassword,
              alias,
              certPath,
              keyPair.getPrivate());

      assertTrue(X509Utils.keyStoreContains(
              keyStoreFilePath,
              keyStorePassword,
              alias));

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getMaxAllowedKeyLength method, of class X509Utils.
   */
  @Test
  public void testGetMaxAllowedKeyLength() {
    LOGGER.info("getMaxAllowedKeyLength");
    try {
      final int maxKeyLength = X509Utils.getMaxAllowedKeyLength();
      LOGGER.info("maximum allowed key length: " + maxKeyLength);
      if (maxKeyLength == Integer.MAX_VALUE) {
        LOGGER.info("JCE unlimited strength jurisdiction policy files are installed");
      }
    } catch (NoSuchAlgorithmException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of generateRSAKeyPair1024 method, of class X509Utils.
   */
  @Test
  public void testGenerateRSAKeyPair1024() {
    LOGGER.info("generateRSAKeyPair");
    KeyPair result = null;
    try {
      result = X509Utils.generateRSAKeyPair1024();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(result);
    final PrivateKey privateKey = result.getPrivate();
    assertNotNull(privateKey);
    final PublicKey publicKey = result.getPublic();
    assertNotNull(publicKey);
    assertEquals("RSA", result.getPrivate().getAlgorithm());
    assertEquals("RSA", result.getPublic().getAlgorithm());
    assertEquals("PKCS#8", result.getPrivate().getFormat());
    assertEquals("X.509", result.getPublic().getFormat());
  }

  /**
   * Test of generateRSAKeyPair3072 method, of class X509Utils.
   */
  @Test
  public void testGenerateRSAKeyPair() {
    LOGGER.info("generateRSAKeyPair");
    KeyPair result = null;
    try {
      result = X509Utils.generateRSAKeyPair3072();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(result);
    final PrivateKey privateKey = result.getPrivate();
    assertNotNull(privateKey);
    final PublicKey publicKey = result.getPublic();
    assertNotNull(publicKey);
    assertEquals("RSA", result.getPrivate().getAlgorithm());
    assertEquals("RSA", result.getPublic().getAlgorithm());
    assertEquals("PKCS#8", result.getPrivate().getFormat());
    assertEquals("X.509", result.getPublic().getFormat());
  }

  /**
   * Test of getNextSerialNumber method of class X509Utils.
   */
  @Test
  public void testGetNextSerialNumber() {
    LOGGER.info("getNextSerialNumber");
    try {
      final BigInteger serialNumber1 = X509Utils.getNextSerialNumber();
      LOGGER.info("serialNumber1: " + serialNumber1);
      final BigInteger serialNumber2 = X509Utils.getNextSerialNumber();
      LOGGER.info("serialNumber2: " + serialNumber2);
      assertEquals(serialNumber1.add(BigInteger.ONE), serialNumber2);
    } catch (IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of encrypt and decrypt methods of class X509Utils.
   */
  @Test
  public void testEncryptAndDecrypt() {
    LOGGER.info("encrypt & decrypt");
    try {
      final KeyPair keyPair = X509Utils.generateRSAKeyPair1024();
      final PublicKey publicKey = keyPair.getPublic();
      final PrivateKey privateKey = keyPair.getPrivate();
      final String plainText = "test plain text";
      final byte[] cipherText = X509Utils.encrypt(plainText, publicKey);
      final String decryptedPlainText = X509Utils.decrypt(cipherText, privateKey);
      assertEquals(plainText, decryptedPlainText);
    } catch (NoSuchAlgorithmException |
            NoSuchProviderException |
            InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of generateSelfSignedEndEntityX509Certificate method, of class X509Utils.
   */
  @Test
  public void testGenerateSelfSignedEndEntityX509Certificate() {
    LOGGER.info("generateSelfSignedEndEntityX509Certificate");
    final int nbrIterations = 30;
    long startMillis = System.currentTimeMillis();
    for (int i = 0; i < nbrIterations; i++) {
      try {
        final KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
        final UUID uuid = UUID.randomUUID();
        LOGGER.info("generating self-signed certificate number " + (i + 1));
        X509Certificate x509Certificate
                = X509Utils.generateSelfSignedEndEntityX509Certificate(
                        keyPair,
                        uuid,
                        "TestComponent");
        if (i == 0) {
          LOGGER.info("generated self-signed end-entity certificate...\n" + x509Certificate);
          assertNotNull(x509Certificate);
          assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, DC=TestComponent, UID="));
          assertEquals(x509Certificate.getIssuerDN().toString(), x509Certificate.getSubjectDN().toString());
          assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
          assertEquals(keyPair.getPublic(), x509Certificate.getPublicKey());
          x509Certificate.verify(keyPair.getPublic());
          LOGGER.info("principal: " + x509Certificate.getSubjectX500Principal().toString());
          assertTrue(x509Certificate.getSubjectX500Principal().toString().contains("UID="));
        }
      } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException ex) {
        LOGGER.info(StringUtils.getStackTraceAsString(ex));
        fail(ex.getMessage());
      }
    }
    long endMillis = System.currentTimeMillis();
    LOGGER.info("number of self-signed certificates generated: " + nbrIterations);
    LOGGER.info("seconds per certificate to generate: " + ((float) (endMillis - startMillis) / 1000.0 / (float) nbrIterations));
  }

  /**
   * Test of writeX509Certificate method and readX509Certificate method, of class X509Utils.
   */
  @Test
  public void testWriteX509Certificate() {
    LOGGER.info("writeX509Certificate");
    try {
      final KeyPair keyPair;
      keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uuid = UUID.randomUUID();
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      keyPair,
                      uuid,
                      "TestComponent");

      LOGGER.info("certificate...\n" + x509Certificate);
      assertNotNull(x509Certificate);
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
      X509Utils.writeX509Certificate(x509Certificate, "data/testCertificate.crt");

      LOGGER.info("readX509Certificate");
      InputStream certificateInputStream = new FileInputStream("data/testCertificate.crt");
      X509Certificate x509Certificate1 = X509Utils.readX509Certificate(certificateInputStream);
      assertNotNull(x509Certificate1);
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate1.getSigAlgName());
      assertEquals(x509Certificate, x509Certificate1);
    } catch (IOException |
            CertificateException |
            NoSuchProviderException |
            InvalidKeyException |
            SignatureException |
            NoSuchAlgorithmException |
            InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of makeCanonicalX509Certificate method of class X509Utils.
   */
  @Test
  public void testMakeCanonicalX509Certificate() {
    LOGGER.info("makeCanonicalX509Certificate");
    try {
      final KeyPair keyPair;
      keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uuid = UUID.randomUUID();
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      keyPair,
                      uuid,
                      "TestComponent");

      LOGGER.info("generated certificate...\n" + x509Certificate);
      assertNotNull(x509Certificate);
      assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, DC=TestComponent, UID="));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), x509Certificate.getPublicKey());
      LOGGER.info("principal: " + x509Certificate.getSubjectX500Principal().toString());
      assertTrue(x509Certificate.getSubjectX500Principal().toString().contains("UID="));

      final X509Certificate canonicalX509Certificate = X509Utils.makeCanonicalX509Certificate(x509Certificate);
      LOGGER.info("canonical certificate...\n" + canonicalX509Certificate);
      assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, DC=TestComponent, UID="));
      assertTrue(canonicalX509Certificate.getSubjectDN().toString().contains("CN=texai.org"));

      assertEquals(canonicalX509Certificate.getSubjectDN().toString(), x509Certificate.getSubjectDN().toString());

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getX509SecurityInfo method of class X509Utils.
   */
  @Test
  public void testGetX509SecurityInfo() {
    LOGGER.info("getX509SecurityInfo");

    final String keyStoreFilePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      keyStoreFilePath = "data/test-client-keystore.uber";
    } else {
      keyStoreFilePath = "data/test-client-keystore.jceks";
    }
    final char[] keyStorePassword = KeyStoreTestUtils.getClientKeyStorePassword();
    KeyStore keyStore = null;
    try {
      keyStore = X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      fail(ex.getMessage());
    }
    X509SecurityInfo x509SecurityInfo = X509Utils.getX509SecurityInfo(
            keyStore,
            KeyStoreTestUtils.getClientKeyStorePassword(),
            KeyStoreTestUtils.TEST_CERTIFICATE_ALIAS); // alias
    assertTrue(x509SecurityInfo.getKeyManagers().length > 0);
  }

  /**
   * Test of findOrCreateKeyStore method of class X509Utils.
   */
  @Test
  public void testFindOrCreateKeyStore() {
    LOGGER.info("findOrCreateKeyStore");
    final String filePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      filePath = "data/keystore.uber";
    } else {
      filePath = "data/keystore.jceks";
    }
    final File keyStoreFile = new File(filePath);
    if (keyStoreFile.exists()) {
      final boolean isFileDeleted = keyStoreFile.delete();
      if (!isFileDeleted) {
        fail("keystore file not deleted: " + filePath);
      }
    }
    KeyStore keyStore;
    try {
      keyStore = X509Utils.findOrCreateKeyStore(filePath, "password".toCharArray());
      assertNotNull(keyStore);
      assertEquals(0, keyStore.size());
      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        assertEquals(X509Utils.BOUNCY_CASTLE_PROVIDER, keyStore.getProvider().getName());
        assertEquals("UBER", keyStore.getType());
      } else {
        assertEquals("SunJCE", keyStore.getProvider().getName());
        assertEquals("JCEKS", keyStore.getType());
      }
      assertTrue(keyStoreFile.exists());
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      fail(ex.getMessage());
    }

  }

  /**
   * Test of findOrCreatePKCS12KeyStore method of class X509Utils.
   */
  @Test
  public void testFindOrCreatePKCS12KeyStore() {
    LOGGER.info("findOrCreatePKCS12KeyStore");
    final String filePath;
    filePath = "data/keystore.p12";
    final File keyStoreFile = new File(filePath);
    if (keyStoreFile.exists()) {
      final boolean isFileDeleted = keyStoreFile.delete();
      if (!isFileDeleted) {
        fail("keystore file not deleted: " + filePath);
      }
    }
    KeyStore keyStore;
    try {
      keyStore = X509Utils.findOrCreatePKCS12KeyStore(filePath, "password".toCharArray());
      assertNotNull(keyStore);
      assertEquals(0, keyStore.size());
      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        assertEquals("BC", keyStore.getProvider().getName());
      } else {
        assertEquals("SunJSSE", keyStore.getProvider().getName());
      }
      assertEquals("pkcs12", keyStore.getType());
      assertTrue(keyStoreFile.exists());
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of addEntryToKeyStore method of class X509Utils.
   */
  @Test
  public void testAddEntryToKeyStore() {
    LOGGER.info("addEntryToKeyStore");
    // create a keystore
    final String filePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      filePath = "data/keystore.uber";
    } else {
      filePath = "data/keystore.jceks";
    }
    final File keyStoreFile = new File(filePath);
    if (keyStoreFile.exists()) {
      final boolean isFileDeleted = keyStoreFile.delete();
      if (!isFileDeleted) {
        fail("keystore file not deleted: " + filePath);
      }
    }
    KeyStore keyStore;
    try {
      final char[] keyStorePassword = "password".toCharArray();
      keyStore = X509Utils.findOrCreateKeyStore(filePath, keyStorePassword);
      assertNotNull(keyStore);
      assertEquals(0, keyStore.size());
      assertTrue(keyStoreFile.exists());

      // create a certificate path
      final KeyPair keyPair;
      keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uuid = UUID.randomUUID();
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      keyPair,
                      uuid,
                      "TestComponent");
      final List<Certificate> certificateList = new ArrayList<>();
      certificateList.add(x509Certificate);
      CertPath certPath = X509Utils.generateCertPath(certificateList);

      // add the certificate path entry to the keystore
      final String alias = X509Utils.getUUID(x509Certificate).toString();
      final KeyStore keyStore1 = X509Utils.addEntryToKeyStore(
              filePath,
              keyStorePassword,
              alias,
              certPath,
              keyPair.getPrivate());
      assertNotNull(keyStore1);
      assertEquals(1, keyStore1.size());
      assertTrue(keyStore1.containsAlias(alias));
      assertFalse(keyStore1.isCertificateEntry(alias));
      assertTrue(keyStore1.isKeyEntry(alias));
      assertTrue(keyStore1.getCertificate(alias) instanceof X509Certificate);
      X509Certificate x509Certificate1 = (X509Certificate) keyStore1.getCertificate(alias);
      assertEquals(x509Certificate, x509Certificate1);
      assertEquals(alias, X509Utils.getUUID(x509Certificate1).toString());

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getUUID method of class X509Utils.
   */
  @Test
  public void testGetUUID() {
    LOGGER.info("getUUID");

    try {
      final KeyPair keyPair;
      keyPair = X509Utils.generateRSAKeyPair3072();
      final UUID uuid = UUID.randomUUID();
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      keyPair,
                      uuid,
                      "TestComponent");
      LOGGER.info("certificate: " + x509Certificate);
      assertEquals(X509Utils.getUUID(x509Certificate).toString().length(), 36);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of copyKeyStoreUberToJceks method of class X509Utils.
   */
  @Test
  public void testCopyKeyStoreUberToJceks() {
    LOGGER.info("copyKeyStoreUberToJceks");
    final String uberKeyStorePath = "data/keystore.uber";
    final char[] keyStorePassword = "password".toCharArray();
    final String jceksKeyStorePath = "data/keystore-copy.jceks";
    try {
      X509Utils.copyKeyStoreUberToJceks(
              uberKeyStorePath,
              keyStorePassword,
              jceksKeyStorePath,
              keyStorePassword);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | UnrecoverableEntryException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of initializeSecureRandom method of class X509Utils.
   */
  @Test
  public void testInitializeSecureRandom() {
    LOGGER.info("initializeSecureRandom");
    SecureRandom secureRandom = X509Utils.initializeSecureRandom("data/secure-random.ser");
    assertNotNull(secureRandom);
    assertTrue((new File("data/secure-random.ser")).exists());
    secureRandom = X509Utils.initializeSecureRandom("data/secure-random.ser");
    assertNotNull(secureRandom);
    assertTrue((new File("data/secure-random.ser")).exists());
  }

  /**
   * Test of hasKeyUsage method of class X509Utils.
   */
  @Test
  public void testHasKeyUsage() {
    LOGGER.info("hasKeyUsage");

    try {
      X509Certificate x509Certificate
              = X509Utils.generateSelfSignedEndEntityX509Certificate(
                      X509Utils.generateRSAKeyPair3072(),
                      UUID.randomUUID(),
                      "TestComponent");

      LOGGER.info("generated certificate:\n" + x509Certificate);
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.digitalSignature));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.nonRepudiation));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.keyEncipherment));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.dataEncipherment));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.keyAgreement));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.keyCertSign));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.cRLSign));
      assertTrue(X509Utils.hasKeyUsage(x509Certificate, KeyUsage.encipherOnly));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

}
