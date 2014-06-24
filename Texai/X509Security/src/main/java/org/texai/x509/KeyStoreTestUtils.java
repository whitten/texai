/*
 * KeyStoreTestUtils.java
 *
 * Created on Feb 5, 2010, 2:17:42 PM
 *
 * Description: Provides keystores and passwords for testing.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.TexaiException;
import static org.texai.x509.X509Utils.isTrustedDevelopmentSystem;

/** Provides keystores and passwords for testing.
 *
 * @author reed
 */
@NotThreadSafe
public final class KeyStoreTestUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(KeyStoreTestUtils.class);
  /** the server keystore password */
  private static final char[] SERVER_KEYSTORE_PASSWORD = "server-keystore-password".toCharArray();
  /** the client keystore password */
  private static final char[] CLIENT_KEYSTORE_PASSWORD = "client-keystore-password".toCharArray();

  /** Prevents the instantiation of this utility class. */
  private KeyStoreTestUtils() {
  }

  /** Initializes the test server keystore on the trusted development system, from where it is copied into the distributed code. */
  public static synchronized void initializeServerKeyStore() {
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    try {
      // the test server keystore consists of the single test server X.509 certificate, which is generated and signed by
      // the Texai root certificate on the developement system that has the root private key.
      final KeyPair serverKeyPair = X509Utils.generateRSAKeyPair2048();
      final X509Certificate serverX509Certificate = X509Utils.generateX509Certificate(
              serverKeyPair.getPublic(),
              X509Utils.getRootPrivateKey(),
              X509Utils.getRootX509Certificate(), null);

      // proceed as though the JCE unlimited strength jurisdiction policy files are installed, which they will be on the
      // trusted development system.
      String filePath = "data/test-server-keystore.uber";
      File file = new File(filePath);
      if (file.exists()) {
        // do not overwrite it

        //Postconditions
        assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
        return;
      }
      LOGGER.info("creating test-server-keystore.uber");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      KeyStore serverKeyStore = X509Utils.findOrCreateKeyStore(filePath, SERVER_KEYSTORE_PASSWORD);
      serverKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              serverKeyPair.getPrivate(),
              SERVER_KEYSTORE_PASSWORD,
              new Certificate[]{serverX509Certificate, X509Utils.getRootX509Certificate()});
      serverKeyStore.store(new FileOutputStream(filePath), SERVER_KEYSTORE_PASSWORD);

      // then proceed after disabling the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(false);
      filePath = "data/test-server-keystore.jceks";
      LOGGER.info("creating test-server-keystore.jceks");
      serverKeyStore = X509Utils.findOrCreateKeyStore(filePath, SERVER_KEYSTORE_PASSWORD);
      serverKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              serverKeyPair.getPrivate(),
              SERVER_KEYSTORE_PASSWORD,
              new Certificate[]{serverX509Certificate, X509Utils.getRootX509Certificate()});
      serverKeyStore.store(new FileOutputStream(filePath), SERVER_KEYSTORE_PASSWORD);
      // restore the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(true);

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
  }

  /** Initializes the test client keystore on the trusted development system, from where it is copied into the distributed code. */
  public static synchronized void initializeClientKeyStore() {
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }

    try {
      // the test client keystore consists of the single test client X.509 certificate, which is generated and signed by
      // the Texai root certificate on the developement system that has the root private key.
      final KeyPair clientKeyPair = X509Utils.generateRSAKeyPair2048();
      final X509Certificate clientX509Certificate = X509Utils.generateX509Certificate(
              clientKeyPair.getPublic(),
              X509Utils.getRootPrivateKey(),
              X509Utils.getRootX509Certificate(), null);

      // proceed as though the JCE unlimited strength jurisdiction policy files are installed, which they will be on the
      // trusted development system.
      String filePath = "data/test-client-keystore.uber";
      File file = new File(filePath);
      if (file.exists()) {
        file.delete();
      }
      LOGGER.info("creating test-client-keystore.uber");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      KeyStore clientKeyStore = X509Utils.findOrCreateKeyStore(filePath, CLIENT_KEYSTORE_PASSWORD);
      clientKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              clientKeyPair.getPrivate(),
              CLIENT_KEYSTORE_PASSWORD,
              new Certificate[]{clientX509Certificate, X509Utils.getRootX509Certificate()});
      clientKeyStore.store(new FileOutputStream(filePath), CLIENT_KEYSTORE_PASSWORD);
      assert "UBER".equals(clientKeyStore.getType());

      // then proceed after disabling the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(false);
      filePath = "data/test-client-keystore.jceks";
      LOGGER.info("creating test-client-keystore.jceks");
      clientKeyStore = X509Utils.findOrCreateKeyStore(filePath, CLIENT_KEYSTORE_PASSWORD);
      clientKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              clientKeyPair.getPrivate(),
              CLIENT_KEYSTORE_PASSWORD,
              new Certificate[]{clientX509Certificate, X509Utils.getRootX509Certificate()});
      clientKeyStore.store(new FileOutputStream(filePath), CLIENT_KEYSTORE_PASSWORD);
      assert "JCEKS".equals(clientKeyStore.getType());
      // restore the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(true);

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
  }

  /** Gets the test server keystore.
   *
   * @return the test server keystore
   */
  public static KeyStore getServerKeyStore() {
    // create server keystore
    String filePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      filePath = "../X509Security/data/test-server-keystore.uber";
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = "../Texai/X509Security/data/test-server-keystore.uber";
      }
      file = new File(filePath);
      if (!file.exists()) {
        // for MessageRouter
        filePath = "data/test-server-keystore.uber";
      }
    } else {
      filePath = "../X509Security/data/test-server-keystore.jceks";
      File file = new File(filePath);
      if (!file.exists()) {
        filePath = "../Texai/X509Security/data/test-server-keystore.jceks";
      }
      file = new File(filePath);
      if (!file.exists()) {
        // for MessageRouter
        filePath = "data/test-server-keystore.jceks";
      }
    }
    try {
      return X509Utils.findOrCreateKeyStore(filePath, SERVER_KEYSTORE_PASSWORD);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the test client keystore.
   *
   * @return the test client keystore
   */
  public static KeyStore getClientKeyStore() {
    String filePath;
    if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
      filePath = "data/test-client-keystore.uber";
    } else {
      filePath = "data/test-client-keystore.jceks";
    }
    LOGGER.info("test-client-keystore path: " + filePath);
    try {
      final KeyStore clientKeyStore = X509Utils.findOrCreateKeyStore(filePath, CLIENT_KEYSTORE_PASSWORD);
      X509Utils.logAliases(clientKeyStore);

      //Postconditions
      assert !filePath.endsWith("uber") || clientKeyStore.getType().equals("UBER") :
              "keystore type is " + clientKeyStore.getType() + ", must be UBER";

      return clientKeyStore;

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }

  }

  /** Gets the X509 security information for a test client.
   *
   * @return the X509 security information for a test client
   */
  public static X509SecurityInfo getClientX509SecurityInfo() {
    return new X509SecurityInfo(
            X509Utils.getTruststore(),
            getClientKeyStore(),
            CLIENT_KEYSTORE_PASSWORD, null);
  }

  /** Gets the X509 security information for a test server.
   *
   * @return the X509 security information for a test server
   */
  public static X509SecurityInfo getServerX509SecurityInfo() {
    return new X509SecurityInfo(
            X509Utils.getTruststore(),
            getServerKeyStore(),
            SERVER_KEYSTORE_PASSWORD, null);
  }

  /** Gets the test server keystore password.
   *
   * @return the test server keystore password
   */
  public static char[] getServerKeyStorePassword() {
    return SERVER_KEYSTORE_PASSWORD;
  }

  /** Gets the test client keystore password.
   *
   * @return the test client keystore password
   */
  public static char[] getClientKeyStorePassword() {
    return CLIENT_KEYSTORE_PASSWORD;
  }

}
