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
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import org.texai.util.StringUtils;
import java.security.SecureRandom;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
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

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(X509UtilsTest.class);

  public X509UtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    X509Utils.logProviders();
    X509Utils.logProviderCapabilities(X509Utils.BOUNCY_CASTLE_PROVIDER);
    X509Utils.createTexaiRootKeyStore();
    X509Utils.initializeInstallerKeyStore();
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
   * Test of initializeTrustore method of class X509Utils.
   */
  @Test
  public void testInitializeTrustore() {
    LOGGER.info("initializeTrustore");

    X509Utils.initializeTrustore();
  }

  /**
   * Test of getTruststore method of class X509Utils.
   */
  @Test
  public void testGetTruststore() {
    LOGGER.info("getTruststore");

    final KeyStore truststore = X509Utils.getTruststore();
    assertNotNull(truststore);
    try {
      assertTrue(truststore.containsAlias(X509Utils.TRUSTSTORE_ENTRY_ALIAS));
      final X509Certificate rootX509Certificate = (X509Certificate) truststore.getCertificate(X509Utils.TRUSTSTORE_ENTRY_ALIAS);
      LOGGER.info("rootX509Certificate from bytes...\n + " + X509Utils.getRootX509Certificate());
      assertEquals(X509Utils.getRootX509Certificate(), rootX509Certificate);
    } catch (KeyStoreException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getRootKeyStorePassword method of class X509Utils.
   */
  @Test
  public void testGetRootKeyStorePassword() {
    LOGGER.info("getRootKeyStorePassword");

    final char[] rootKeyStorePassword = X509Utils.getRootKeyStorePassword();
    if (rootKeyStorePassword != null) {
      LOGGER.info("root keystore password: '" + new String(rootKeyStorePassword) + "'");
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
   * Test of generateRSAKeyPair2048 method, of class X509Utils.
   */
  @Test
  public void testGenerateRSAKeyPair() {
    LOGGER.info("generateRSAKeyPair");
    KeyPair result = null;
    try {
      result = X509Utils.generateRSAKeyPair2048();
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
   * Test of generateRootX509Certificate method, of class X509Utils.
   */
  @Test
  public void testGenerateRootX509Certificate() {
    LOGGER.info("generateRootX509Certificate");
    KeyPair keyPair;
    try {
      keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate rootX509Certificate = X509Utils.generateRootX509Certificate(keyPair);
      LOGGER.info("root certificate:\n" + rootX509Certificate);
      assertNotNull(rootX509Certificate);
      assertTrue(rootX509Certificate.getIssuerDN().toString().contains("CN=texai.org, O=Texai Certification Authority"));
      assertTrue(rootX509Certificate.getSubjectDN().toString().contains("CN=texai.org, O=Texai Certification Authority"));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, rootX509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), rootX509Certificate.getPublicKey());
      //assertTrue(X509CertImpl.isSelfIssued(rootX509Certificate));
      assertTrue(rootX509Certificate.getSubjectX500Principal().toString().contains("UID="));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getRootX509Certificate method, of class X509Utils.
   */
  @Test
  public void testGetRootX509Certificate() {
    LOGGER.info("getRootX509Certificate");
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      LOGGER.info("root certificate:\n" + rootX509Certificate);
      assertNotNull(rootX509Certificate);
      assertEquals("CN=texai.org, O=Texai Certification Authority, UID=ed6d6718-80de-4848-af43-fed7bdba3c36", rootX509Certificate.getIssuerDN().toString());
      assertEquals("CN=texai.org, O=Texai Certification Authority, UID=ed6d6718-80de-4848-af43-fed7bdba3c36", rootX509Certificate.getSubjectDN().toString());
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, rootX509Certificate.getSigAlgName());
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of generateX509Certificate method, of class X509Utils.
   */
  @Test
  public void testGenerateX509Certificate() {
    LOGGER.info("generateX509Certificate");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      LOGGER.info("root certificate (" + rootX509Certificate.getClass().getName() + "):\n" + rootX509Certificate);
      X509Certificate x509Certificate =
              X509Utils.generateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, "TestComponent");

      LOGGER.info("generated certificate...\n" + x509Certificate);
      assertNotNull(x509Certificate);
      assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, O=Texai Certification Authority, UID="));
      assertTrue(x509Certificate.getSubjectDN().toString().contains("CN=texai.org"));
      assertTrue(!x509Certificate.getSubjectDN().toString().contains("O=Texai Certification Authority"));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), x509Certificate.getPublicKey());
      x509Certificate.verify(rootX509Certificate.getPublicKey());
      LOGGER.info("principal: " + x509Certificate.getSubjectX500Principal().toString());
      assertTrue(x509Certificate.getSubjectX500Principal().toString().contains("UID="));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of generateX509CertificatePath method, of class X509Utils.
   */
  @Test
  public void testGenerateX509CertificatePath() {
    LOGGER.info("generateX509CertificatePath");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      LOGGER.info("root certificate (" + rootX509Certificate.getClass().getName() + "):\n" + rootX509Certificate);
      final CertPath certPath = X509Utils.generateX509CertificatePath(
              publicKey,
              certificateAuthorityPrivateKey,
              rootX509Certificate, // issuerCertificate
              X509Utils.generateCertPath(new ArrayList<>()), null);  // issuerCertPath
      assertEquals(1, certPath.getCertificates().size());
      X509Certificate x509Certificate = (X509Certificate) certPath.getCertificates().get(0);
      LOGGER.info("generated certificate...\n" + x509Certificate);
      assertNotNull(x509Certificate);
      assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, O=Texai Certification Authority, UID="));
      assertTrue(x509Certificate.getSubjectDN().toString().contains("CN=texai.org"));
      assertTrue(!x509Certificate.getSubjectDN().toString().contains("O=Texai Certification Authority"));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), x509Certificate.getPublicKey());
      x509Certificate.verify(rootX509Certificate.getPublicKey());
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateException | SignatureException | InvalidKeyException | IOException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }

  /**
   * Test of generateIntermediateX509Certificate method, of class X509Utils.
   */
  @Test
  public void testGenerateIntermediateX509Certificate() {
    LOGGER.info("generateIntermediateX509Certificate");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      final KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      X509Certificate intermediateX509Certificate =
              X509Utils.generateIntermediateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, 0);

      LOGGER.info("generated intermediate certificate:\n" + intermediateX509Certificate);
      assertNotNull(intermediateX509Certificate);
      LOGGER.info("intermediateX509Certificate.getIssuerDN(): " + intermediateX509Certificate.getIssuerDN().toString());
      assertTrue(intermediateX509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, O=Texai Certification Authority, UID="));
      assertTrue(intermediateX509Certificate.getSubjectDN().toString().contains("CN=texai.org"));
      assertTrue(!intermediateX509Certificate.getSubjectDN().toString().contains("O=Texai Certification Authority"));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, intermediateX509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), intermediateX509Certificate.getPublicKey());
      //assertFalse(X509CertImpl.isSelfIssued(intermediateX509Certificate));
      intermediateX509Certificate.verify(rootX509Certificate.getPublicKey());
      LOGGER.info("principal: " + intermediateX509Certificate.getSubjectX500Principal().toString());
      assertTrue(intermediateX509Certificate.getSubjectX500Principal().toString().contains("UID="));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of writeX509Certificate method and readX509Certificate method, of class X509Utils.
   */
  @Test
  public void testWriteX509Certificate() {
    LOGGER.info("writeX509Certificate");
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      LOGGER.info("root certificate...\n" + rootX509Certificate);
      assertNotNull(rootX509Certificate);
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, rootX509Certificate.getSigAlgName());
      X509Utils.writeX509Certificate(rootX509Certificate, "data/testRootCertificate.crt");

      LOGGER.info("readX509Certificate");
      InputStream certificateInputStream = new FileInputStream("data/testRootCertificate.crt");
      X509Certificate rootX509Certificate1 = X509Utils.readX509Certificate(certificateInputStream);
      assertNotNull(rootX509Certificate1);
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, rootX509Certificate1.getSigAlgName());
      assertEquals(rootX509Certificate, rootX509Certificate1);
    } catch (IOException | CertificateException | NoSuchProviderException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of makeCanonicalX509Certificate method of class X509Utils.
   */
  @Test
  public void testMakeCanonicalX509Certificate() {
    LOGGER.info("makeCanonicalX509Certificate");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      LOGGER.info("root certificate (" + rootX509Certificate.getClass().getName() + "):\n" + rootX509Certificate);
      X509Certificate x509Certificate =
              X509Utils.generateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, "TestComponent");

      LOGGER.info("generated certificate...\n" + x509Certificate);
      assertNotNull(x509Certificate);
      assertTrue(x509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, O=Texai Certification Authority, UID="));
      assertTrue(x509Certificate.getSubjectDN().toString().contains("CN=texai.org"));
      assertTrue(!x509Certificate.getSubjectDN().toString().contains("O=Texai Certification Authority"));
      assertEquals(X509Utils.DIGITAL_SIGNATURE_ALGORITHM, x509Certificate.getSigAlgName());
      assertEquals(keyPair.getPublic(), x509Certificate.getPublicKey());
      x509Certificate.verify(rootX509Certificate.getPublicKey());
      LOGGER.info("principal: " + x509Certificate.getSubjectX500Principal().toString());
      assertTrue(x509Certificate.getSubjectX500Principal().toString().contains("UID="));

      final X509Certificate canonicalX509Certificate = X509Utils.makeCanonicalX509Certificate(x509Certificate);
      LOGGER.info("canonical certificate...\n" + canonicalX509Certificate);
      assertTrue(canonicalX509Certificate.getIssuerDN().toString().startsWith("CN=texai.org, O=Texai Certification Authority, UID="));
      assertTrue(canonicalX509Certificate.getSubjectDN().toString().contains("CN=texai.org"));

      assertEquals(canonicalX509Certificate.getSubjectDN().toString(), x509Certificate.getSubjectDN().toString());
      assertEquals(canonicalX509Certificate.getIssuerDN().toString(), x509Certificate.getIssuerDN().toString());

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
    X509SecurityInfo x509SecurityInfo = X509Utils.getX509SecurityInfo(
            keyStoreFilePath,
            KeyStoreTestUtils.getClientKeyStorePassword(), null);
    assertNotNull(x509SecurityInfo.getTrustStore());
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      assertEquals(X509Utils.BOUNCY_CASTLE_PROVIDER, x509SecurityInfo.getTrustStore().getProvider().getName());
      assertEquals("UBER", x509SecurityInfo.getTrustStore().getType());
    } else {
      assertEquals("SunJCE", x509SecurityInfo.getTrustStore().getProvider().getName());
      assertEquals("JCEKS", x509SecurityInfo.getTrustStore().getType());
    }
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
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }

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
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      final PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      PublicKey publicKey = keyPair.getPublic();
      X509Certificate x509Certificate =
              X509Utils.generateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, null);
      x509Certificate.verify(rootX509Certificate.getPublicKey());
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
      final X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      LOGGER.info("root certificate: " + rootX509Certificate);
      assertEquals("ed6d6718-80de-4848-af43-fed7bdba3c36", X509Utils.getUUID(rootX509Certificate).toString());
    } catch (Exception ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of copyKeyStoreUberToJceks method of class X509Utils.
   */
  @Test
  public void testCopyKeyStoreUberToJceks() {
    LOGGER.info("copyKeyStoreUberToJceks");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }

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
   * Test of generateX509SecurityInfo method of class X509Utils.
   */
  @Test
  public void testGenerateX509SecurityInfo() {
    LOGGER.info("generateX509SecurityInfo");
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      final KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      final PrivateKey issuerPrivateKey = X509Utils.getRootPrivateKey();
      final X509Certificate issuerCertificate = X509Utils.getRootX509Certificate();
      final UUID uid = UUID.randomUUID();
      final char[] keystorePassword = "my-password".toCharArray();
      final boolean isJCEUnlimitedStrengthPolicy = true;
      X509SecurityInfo x509SecurityInfo = X509Utils.generateX509SecurityInfo(
              keyPair,
              issuerPrivateKey,
              issuerCertificate,
              uid,
              keystorePassword,
              isJCEUnlimitedStrengthPolicy,
              null); // domainComponent
      assertNotNull(x509SecurityInfo.getCertPath());
      assertNotNull(x509SecurityInfo.getKeyManagers());
      assertNotNull(x509SecurityInfo.getKeyStore());
      assertNotNull(x509SecurityInfo.getPrivateKey());
      assertNotNull(x509SecurityInfo.getTrustStore());
      assertNotNull(x509SecurityInfo.getX509Certificate());
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of validateCertificatePath method of class X509Utils.
   */
  @Test
  public void testValidateCertificatePath() {
    LOGGER.info("validateCertificatePath");


    // debug by setting -Djava.security.debug=certpath in the Texai POM


    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      final PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();


      // generate and validate an intermediate certificate
      final KeyPair intermediateKeyPair = X509Utils.generateRSAKeyPair3072();
      final PublicKey intermediatePublicKey = intermediateKeyPair.getPublic();
      final PrivateKey intermediatePrivateKey = intermediateKeyPair.getPrivate();
      final X509Certificate intermediateX509Certificate = X509Utils.generateIntermediateX509Certificate(
              intermediatePublicKey,
              certificateAuthorityPrivateKey,
              rootX509Certificate,
              0); // pathLengthConstraint
      intermediateX509Certificate.verify(rootX509Certificate.getPublicKey());

      // generate and validate an end-entity certificate
      final KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      final PublicKey publicKey = keyPair.getPublic();
      final X509Certificate x509Certificate = X509Utils.generateX509Certificate(
              publicKey,
              intermediatePrivateKey,
              intermediateX509Certificate,
              null); // domainComponent
      x509Certificate.verify(intermediateX509Certificate.getPublicKey());

      // validate a certificate path consisting of three certificates: root, intermediate, and end-entity certificates
      final List<Certificate> certificateList = new ArrayList<>();

      LOGGER.info("validating path of length 3");

      LOGGER.info("rootX509Certificate...\n" + rootX509Certificate);
      LOGGER.info("intermediateX509Certificate...\n" + intermediateX509Certificate);
      LOGGER.info("x509Certificate...\n" + x509Certificate);

      certificateList.clear();
      certificateList.add(x509Certificate);
      certificateList.add(intermediateX509Certificate);
      certificateList.add(rootX509Certificate);
      final CertPath certPath = X509Utils.generateCertPath(certificateList);
      X509Utils.validateCertificatePath(certPath);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException | CertPathValidatorException ex) {
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

    X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
    LOGGER.info("root certificate:\n" + rootX509Certificate);
    LOGGER.info("root certificate class: " + rootX509Certificate.getClass().getName());
    LOGGER.info("key usage bits: " + StringUtils.booleanArrayToBitString(rootX509Certificate.getKeyUsage()));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.digitalSignature));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.nonRepudiation));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.keyEncipherment));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.dataEncipherment));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.keyAgreement));
    assertTrue(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.keyCertSign));
    assertTrue(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.cRLSign));
    assertFalse(X509Utils.hasKeyUsage(rootX509Certificate, KeyUsage.encipherOnly));


    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      PublicKey publicKey = keyPair.getPublic();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      X509Certificate intermediateX509Certificate =
              X509Utils.generateIntermediateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, 0);

      LOGGER.info("generated intermediate certificate:\n" + intermediateX509Certificate);
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.digitalSignature));
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.nonRepudiation));
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.keyEncipherment));
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.dataEncipherment));
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.keyAgreement));
      assertTrue(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.keyCertSign));
      assertTrue(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.cRLSign));
      assertFalse(X509Utils.hasKeyUsage(intermediateX509Certificate, KeyUsage.encipherOnly));


      keyPair = X509Utils.generateRSAKeyPair2048();
      publicKey = keyPair.getPublic();
      certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      LOGGER.info("root certificate (" + rootX509Certificate.getClass().getName() + "):\n" + rootX509Certificate);
      X509Certificate x509Certificate =
              X509Utils.generateX509Certificate(publicKey, certificateAuthorityPrivateKey, rootX509Certificate, null);

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
