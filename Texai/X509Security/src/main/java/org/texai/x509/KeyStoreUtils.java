/*
 * KeyStoreUtils.java
 *
 * Created on Feb 5, 2010, 2:17:42 PM
 *
 * Description: Provides keystores and test passwords.
 *
 * Copyright (C) Feb 5, 2010 reed.
 */
package org.texai.x509;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides keystores and passwords for testing.
 *
 * @author reed
 */
@NotThreadSafe
public final class KeyStoreUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(KeyStoreUtils.class);
  // the server keystore password
  private static final char[] SERVER_KEYSTORE_PASSWORD = "server-keystore-password".toCharArray();
  // the client keystore password
  private static final char[] CLIENT_KEYSTORE_PASSWORD = "client-keystore-password".toCharArray();
  // the test certificate alias
  public static final String TEST_CERTIFICATE_ALIAS = "certificate";
  // the web server certificate alias
  public static final String WEB_SERVER_CERTIFICATE_ALIAS = "s1as";

  /**
   * Prevents the instantiation of this utility class.
   */
  private KeyStoreUtils() {
  }

  /**
   * Initializes the server test keystore.
   */
  public static synchronized void initializeServerTestKeyStore() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    final X509Certificate serverX509Certificate;
    try {
      // the test server keystore consists of the single test server X.509 certificate
      final KeyPair serverKeyPair = X509Utils.generateRSAKeyPair3072();
      serverX509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              serverKeyPair,
              UUID.randomUUID(), // uid
              "test server"); // domainComponent

      // proceed as though the JCE unlimited strength jurisdiction policy files are installed, which they will be on the
      // trusted development system.
      String filePath = "data/test-server-keystore.uber";
      File file = new File(filePath);
      if (file.exists()) {
        final boolean isOK = file.delete();
        assert isOK;
      }
      LOGGER.info("creating test-server-keystore.uber");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      final KeyStore serverKeyStoreUber = X509Utils.findOrCreateKeyStore(filePath, SERVER_KEYSTORE_PASSWORD);
      serverKeyStoreUber.setKeyEntry(
              TEST_CERTIFICATE_ALIAS, // certificateAlias
              serverKeyPair.getPrivate(),
              SERVER_KEYSTORE_PASSWORD,
              new Certificate[]{serverX509Certificate});
      try (final FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
        serverKeyStoreUber.store(fileOutputStream, SERVER_KEYSTORE_PASSWORD);
      }
      X509Utils.logAliases(serverKeyStoreUber);

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    final X509SecurityInfo x509SecurityInfo = getServerX509SecurityInfo();
    assert x509SecurityInfo != null;
    assert x509SecurityInfo.getX509Certificate().equals(serverX509Certificate);
  }

  /**
   * Initializes the web server keystore.
   *
   * @return the security information for the web server
   */
  public static synchronized X509SecurityInfo getWebServerX509SecurityInfo() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    final X509Certificate webServerX509Certificate;
    final String webServerKeyStorePassword;
    final KeyStore webServerKeyStore;
    try {
      // the server keystore consists of the single server X.509 certificate
      final String webServerKeyStoreFilePath = "data/web-server-keystore.uber";
      LOGGER.info("accessing " + webServerKeyStoreFilePath + " for the web server SSL certificate");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      File keystorePasswordFile = new File("../texai-keystore-password.txt");
      if (!keystorePasswordFile.exists()) {
        keystorePasswordFile = new File("/home/reed/texai-keystore-password.txt");
        assert keystorePasswordFile.exists();
      }
      try (final BufferedReader bufferedReader
              = new BufferedReader(new InputStreamReader(new FileInputStream(keystorePasswordFile), "UTF-8"))) {
        webServerKeyStorePassword = bufferedReader.readLine();
        if (!StringUtils.isNonEmptyString(webServerKeyStorePassword)) {
          throw new TexaiException("web server keystore password file not found");
        }
      }
      webServerKeyStore = X509Utils.getWebServerKeyStore(
              webServerKeyStoreFilePath,
              webServerKeyStorePassword.toCharArray());
      X509Utils.logAliases(webServerKeyStore);
//      Enumeration<String> aliases;
//      try {
//        aliases = webServerKeyStore.aliases();
//      } catch (KeyStoreException ex) {
//        throw new TexaiException(ex);
//      }
//      LOGGER.info("aliases...");
//      int aliasesCnt = 0;
//      while (aliases.hasMoreElements()) {
//        LOGGER.info("  " + aliases.nextElement());
//        aliasesCnt++;
//      }
//      LOGGER.info(aliasesCnt + " aliases found in the web server keystore");
      webServerX509Certificate = (X509Certificate) webServerKeyStore.getCertificate(WEB_SERVER_CERTIFICATE_ALIAS);
      assert webServerX509Certificate != null;
      LOGGER.debug("web server certificate: " + webServerX509Certificate.getSubjectDN());
      LOGGER.debug("web server certificate ...\n" + webServerX509Certificate);

    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    final X509SecurityInfo x509SecurityInfo = new X509SecurityInfo(
            webServerKeyStore,
            webServerKeyStorePassword.toCharArray(),
            WEB_SERVER_CERTIFICATE_ALIAS);

    //Postconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    assert x509SecurityInfo != null;

    return x509SecurityInfo;
  }

  /**
   * Initializes the test client keystore on the trusted development system, from where it is copied into the distributed code.
   */
  public static synchronized void initializeClientKeyStore() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    final X509Certificate clientX509Certificate;
    FileOutputStream fileOutputStream = null;
    try {
      // the test client keystore consists of the single test client X.509 certificate, which is generated and signed by
      // the Texai root certificate on the developement system that has the root private key.
      final KeyPair clientKeyPair = X509Utils.generateRSAKeyPair3072();
      clientX509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              clientKeyPair,
              UUID.randomUUID(), // uid
              "test client"); // domainComponent

      // proceed as though the JCE unlimited strength jurisdiction policy files are installed, which they will be on the
      // trusted development system.
      String filePath = "data/test-client-keystore.uber";
      File file = new File(filePath);
      if (file.exists()) {
        final boolean isOK = file.delete();
        if (!isOK) {
          throw new TexaiException("failed to delete " + file);
        }
      }
      LOGGER.info("creating test-client-keystore.uber");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      final KeyStore clientKeyStoreUber = X509Utils.findOrCreateKeyStore(filePath, CLIENT_KEYSTORE_PASSWORD);
      clientKeyStoreUber.setKeyEntry(
              TEST_CERTIFICATE_ALIAS, // certificateAlias
              clientKeyPair.getPrivate(),
              CLIENT_KEYSTORE_PASSWORD,
              new Certificate[]{clientX509Certificate});
      fileOutputStream = new FileOutputStream(filePath);
      clientKeyStoreUber.store(fileOutputStream, CLIENT_KEYSTORE_PASSWORD);
      assert "UBER".equals(clientKeyStoreUber.getType());

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    } finally {
      if (fileOutputStream != null) {
        try {
          fileOutputStream.close();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
    }

    //Postconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    final X509SecurityInfo x509SecurityInfo = getClientX509SecurityInfo();
    assert x509SecurityInfo != null;
    assert x509SecurityInfo.getX509Certificate().equals(clientX509Certificate);
  }

  /**
   * Gets the test server keystore.
   *
   * @return the test server keystore
   */
  public static KeyStore getServerKeyStore() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    // create server keystore
    String filePath = "data/test-server-keystore.uber";
    try {
      return X509Utils.findOrCreateKeyStore(filePath, SERVER_KEYSTORE_PASSWORD);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Gets the test client keystore.
   *
   * @return the test client keystore
   */
  public static KeyStore getClientKeyStore() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    String filePath = "data/test-client-keystore.uber";
    try {
      return X509Utils.findOrCreateKeyStore(filePath, CLIENT_KEYSTORE_PASSWORD);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Gets the X509 security information for a test client.
   *
   * @return the X509 security information for a test client
   */
  public static X509SecurityInfo getClientX509SecurityInfo() {
    return new X509SecurityInfo(
            getClientKeyStore(),
            CLIENT_KEYSTORE_PASSWORD,
            TEST_CERTIFICATE_ALIAS); // certificateAlias
  }

  /**
   * Gets the X509 security information for a test server.
   *
   * @return the X509 security information for a test server
   */
  public static X509SecurityInfo getServerX509SecurityInfo() {
    return new X509SecurityInfo(
            getServerKeyStore(),
            SERVER_KEYSTORE_PASSWORD,
            TEST_CERTIFICATE_ALIAS);
  }

  /**
   * Gets the test server keystore password.
   *
   * @return the test server keystore password
   */
  public static char[] getServerKeyStorePassword() {
    return SERVER_KEYSTORE_PASSWORD.clone();
  }

  /**
   * Gets the test client keystore password.
   *
   * @return the test client keystore password
   */
  public static char[] getClientKeyStorePassword() {
    return CLIENT_KEYSTORE_PASSWORD.clone();
  }

}
