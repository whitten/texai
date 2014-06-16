/*
 * X509Utils.java
 *
 * Created on Jan 19, 2010, 3:12:10 PM
 *
 * Description: X509 utilities adapted from "Beginning Cryptography With Java", David Hook, WROX.
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


// refer to http://www.bouncycastle.org/docs/docs1.5on/index.html for substitutes for deprecated references


package org.texai.x509;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** X509 utilities adapted from "Beginning Cryptography With Java", David Hook, WROX.
 *
 * How to regenerate the root X.509 certificate on the development system.
 * (1) delete /home/reed/texai-keystore.jceks
 * (2) run the JUnit test X509UtilsTest.java - expect several unit test failures
 * (3) copy the byte array values from the unit test output into the array initialialization value for ROOT_CERTIFICATE_BYTES.
 * (4) delete /home/reed/svn/Texai/X509Security/data/truststore.*, test-client-keystore.*, test-server-keystore.*
 * (5) re-run the unit test correcting for the new root UID
 * (6) likewise correct KeyStoreTestUtilsTest, X509SecurityInfoTest and TexaiSSLContextFactoryTest
 * (7) ensure that subversion updates the new keystore files when committing
 *
 * @author reed
 */
public final class X509Utils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(X509Utils.class);
  /** the default secure random serialization path */
  public static final String DEFAULT_SECURE_RANDOM_PATH = "data/secure-random.ser";
  /** the root certificate alias */
  public static final String ROOT_ALIAS = "root";
  /** the root certificate alias */
  public static final String JAR_SIGNER_ALIAS = "jar-signer";
  /** the period in which the certificate is valid */
  private static final long VALIDITY_PERIOD = 10L * 365L * 24L * 60L * 60L * 1000L; // ten years
  /** the Bouncy Castle cryptography provider */
  public static final String BOUNCY_CASTLE_PROVIDER = "BC";
  /** the digital signature algorithm */
  public static final String DIGITAL_SIGNATURE_ALGORITHM = "SHA512WithRSAEncryption";
  /** the indicator whether the JCE unlimited strength jurisdiction policy files are installed */
  private static boolean isJCEUnlimitedStrenthPolicy;
  /** the root certificate bytes */
  private final static byte[] ROOT_CERTIFICATE_BYTES = {
    48, -126, 4, -95, 48, -126, 3, 9, -96, 3, 2, 1, 2, 2, 1, 1, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 13, 5,
    0, 48, 114, 49, 52, 48, 50, 6, 10, 9, -110, 38, -119, -109, -14, 44, 100, 1, 1, 19, 36, 50, 51, 51, 98, 102, 100,
    98, 50, 45, 57, 50, 56, 55, 45, 52, 98, 52, 49, 45, 98, 51, 48, 52, 45, 101, 98, 49, 50, 49, 101, 97, 55, 99, 52,
    100, 101, 49, 38, 48, 36, 6, 3, 85, 4, 10, 19, 29, 84, 101, 120, 97, 105, 32, 67, 101, 114, 116, 105, 102, 105, 99,
    97, 116, 105, 111, 110, 32, 65, 117, 116, 104, 111, 114, 105, 116, 121, 49, 18, 48, 16, 6, 3, 85, 4, 3, 19, 9, 116,
    101, 120, 97, 105, 46, 111, 114, 103, 48, 30, 23, 13, 49, 48, 48, 53, 49, 50, 49, 56, 50, 50, 50, 57, 90, 23, 13,
    50, 48, 48, 53, 48, 57, 49, 56, 50, 50, 51, 57, 90, 48, 114, 49, 52, 48, 50, 6, 10, 9, -110, 38, -119, -109, -14,
    44, 100, 1, 1, 19, 36, 50, 51, 51, 98, 102, 100, 98, 50, 45, 57, 50, 56, 55, 45, 52, 98, 52, 49, 45, 98, 51, 48, 52,
    45, 101, 98, 49, 50, 49, 101, 97, 55, 99, 52, 100, 101, 49, 38, 48, 36, 6, 3, 85, 4, 10, 19, 29, 84, 101, 120, 97,
    105, 32, 67, 101, 114, 116, 105, 102, 105, 99, 97, 116, 105, 111, 110, 32, 65, 117, 116, 104, 111, 114, 105, 116,
    121, 49, 18, 48, 16, 6, 3, 85, 4, 3, 19, 9, 116, 101, 120, 97, 105, 46, 111, 114, 103, 48, -126, 1, -94, 48, 13, 6,
    9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, -113, 0, 48, -126, 1, -118, 2, -126, 1, -127, 0, -71, -99,
    17, 77, 28, -109, -123, 86, 126, 123, 72, 111, -126, -32, -43, -107, 111, 60, -15, 30, -22, 92, -79, -4, -35, -90,
    -125, -43, -46, 105, -80, -4, 118, -40, 20, 60, -55, -90, -2, -3, 93, -79, -126, 79, 20, 35, -70, 110, 72, -65, 97,
    -14, 127, 60, -28, 5, -83, -49, 118, 21, 120, 113, 109, -99, 13, 83, 45, -31, 69, 60, -57, -41, -59, -99, -68, 54,
    -14, 43, 30, 35, -3, 72, -25, -64, -58, 56, -90, 75, 85, 80, -52, -59, -86, -119, -89, -26, 57, -38, 64, -107, -56,
    89, -92, -1, -104, -27, 6, 11, -96, 21, -16, 35, 83, -49, 58, 49, 9, -127, 86, -78, -75, -123, -63, -35, -95, -85,
    6, 41, -80, -79, 104, -103, 5, 55, -10, -114, 88, -23, -107, -46, -37, 72, 103, 110, -64, 106, -89, -90, -101, 49,
    -31, 26, -41, -127, 111, -55, 10, -40, -56, -113, -12, -71, -85, -53, 92, 2, 109, -15, 40, 52, -115, 5, -33, -39,
    -30, -15, 114, 120, 47, -102, 42, 54, -14, -118, 90, 78, -25, 65, 16, -107, 16, -29, 78, 111, 81, -32, 123, -104,
    -108, 56, 30, -56, 86, -18, 23, -55, 105, -4, 10, 113, -92, -96, 103, -95, 61, 32, -124, 124, -120, 73, 3, -22,
    -125, -41, 79, 56, 29, 96, 127, -120, 54, -100, 29, 125, -46, 48, 38, -25, 22, 33, -18, -72, -32, -76, 93, 36, -55,
    105, 112, -50, -51, -128, 15, -35, -68, -21, -45, 6, -50, 41, -13, 63, 58, -80, -40, 78, -42, -107, 104, 114, 63,
    60, 89, 74, 95, -128, 88, 44, -72, 90, -16, 107, -66, -32, -21, 88, 2, -35, 64, 50, 110, 84, 1, -43, -9, -122, 65,
    101, 118, 74, -44, 45, 123, 44, -120, -64, -46, 61, -53, -76, 103, -91, -128, 117, -16, 7, 83, 15, -81, -90, 47,
    -55, 32, -84, -50, -30, -38, 2, -16, 112, 100, 53, 120, 41, -47, 6, -123, 86, -55, 67, -62, -22, 99, 112, 88, -74,
    -15, -62, -7, 3, -94, 59, -90, -69, -16, 60, 79, 26, -16, 44, -127, 105, -99, -56, 47, 8, -39, 115, 119, 90, 67, 91,
    121, 106, -109, 47, -68, 73, 86, 0, 86, -4, -48, 33, 2, 3, 1, 0, 1, -93, 66, 48, 64, 48, 29, 6, 3, 85, 29, 14, 4,
    22, 4, 20, -101, -55, 78, 40, 117, -42, -27, 117, 25, 22, -91, -100, 87, 86, -43, 23, -120, -61, 96, -92, 48, 15, 6,
    3, 85, 29, 19, 1, 1, -1, 4, 5, 48, 3, 1, 1, -1, 48, 14, 6, 3, 85, 29, 15, 1, 1, -1, 4, 4, 3, 2, 1, 6, 48, 13, 6, 9,
    42, -122, 72, -122, -9, 13, 1, 1, 13, 5, 0, 3, -126, 1, -127, 0, 99, 117, 41, 103, 37, -7, 122, -20, -127, 90, -107,
    88, 110, 7, -55, 7, 71, -70, 42, -58, -5, 6, -87, 99, 91, -61, 2, -79, 38, -75, -69, -75, 99, -84, 58, 113, 119,
    -67, -120, -67, 36, 39, -125, 23, 109, 19, 22, 19, -122, -26, 68, -114, 74, 23, -63, -54, -42, -126, -22, 19, -51,
    66, 100, 19, -46, 124, 116, 27, 31, -21, 112, 125, -126, 25, -28, -102, -67, 27, 77, -81, -41, 19, 75, 21, 19, 3,
    -84, 4, -92, 126, 114, -122, -90, -56, -7, 15, -108, 9, 77, 10, 44, 6, -115, -40, 10, -75, -59, 64, 59, -65, 99,
    -100, 11, 69, -31, 3, 103, 101, 67, -61, -40, 100, 27, -48, 105, -73, -122, 18, -106, 110, 50, 108, 119, 87, -33,
    113, -35, 44, 42, -50, 93, 118, -106, 53, -127, 27, 42, -67, -48, -44, 121, 0, 7, -112, 82, 24, -45, -2, -47, 83,
    101, -39, 126, -127, -82, -33, 103, 76, 14, -35, 47, -92, 103, -17, 90, 36, -53, -28, -70, -79, 118, -74, 41, 93,
    -119, -30, 95, -19, -31, 119, 102, 117, 53, -26, 7, 75, 55, 11, -22, 13, -70, -51, -62, -21, -40, 101, -63, 25, -71,
    -67, -59, 52, 1, 103, 82, 98, 58, 98, 27, 106, -52, 101, -48, 121, -68, -82, 110, -119, 19, 23, -116, 29, -44, 21,
    -111, 116, 52, 82, 38, -105, 106, -124, 114, -118, -39, -71, 78, -114, 80, -59, 15, 111, -1, -96, -83, 8, 58, -8,
    -7, -128, -114, 21, 102, 10, 127, 106, 77, 22, 6, -60, -68, 83, -93, 89, -34, -66, -72, 96, -23, 124, -77, -24, 86,
    -44, -6, 74, -127, -52, 52, -8, 24, -72, -44, 30, 94, -4, 7, -18, 82, 72, -128, 65, 125, 51, -36, 118, 18, -47, 124,
    89, -62, 110, -41, -82, -47, -59, -24, -105, 42, -110, 110, -65, -22, 62, 19, 98, 5, -54, 11, -68, -125, 34, -52,
    99, -69, 111, 93, -7, -106, 106, 118, 113, 99, -115, -80, -21, 97, 78, 120, 32, -85, 27, -45, -4, -44, -80, -123,
    105, 90, 121, -75, 83, -51, -17, 58, 40, 15, 66, -32, -48, 12, 76, -125, -116, 111, -50, -74, -48, -5, -2, 75, -20,
    -123, 6};
  /** the root certificate */
  private static final X509Certificate ROOT_X509_CERTIFICATE;
  /** the truststore entry alias */
  public static final String TRUSTSTORE_ENTRY_ALIAS = "Texai root certificate";
  /** the truststore */
  private static KeyStore truststore;
  /** the truststore password */
  public static final char[] TRUSTSTORE_PASSWORD = "truststore-password".toCharArray();
  /** the certificate entry alias */
  public static final String ENTRY_ALIAS = "certificate";
  /** the installer keystore password */
  public static final char[] INSTALLER_KEYSTORE_PASSWORD = "installer-keystore-password".toCharArray();
  /** the intermediate, signing entry alias */
  public static final String INTERMEDIATE_ENTRY_ALIAS = "Texai intermediate certificate";

  static {
    try {
      setIsJCEUnlimitedStrengthPolicy(Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE);
    } catch (NoSuchAlgorithmException ex) {
      throw new TexaiException(ex);
    }
  }

  static {
    LOGGER.info("adding Bouncy Castle cryptography provider");
    Security.addProvider(new BouncyCastleProvider());
    try {
      LOGGER.info("initializing the root X.509 certificate");
      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(ROOT_CERTIFICATE_BYTES);
      ROOT_X509_CERTIFICATE = readX509Certificate(byteArrayInputStream);
    } catch (CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }
  /** the secure random */
  private static SecureRandom secureRandom;
  /** the secure random synchronization lock */
  private static final Object secureRandom_lock = new Object();

  static {
    X509Utils.initializeSecureRandom(DEFAULT_SECURE_RANDOM_PATH);
  }

  /** Prevents the instantiation of this utility class. */
  private X509Utils() {
  }

  /** Gets the secure random, and lazily initializes it.
   *
   * @return the initialized secure random
   */
  public static SecureRandom getSecureRandom() {
    synchronized (secureRandom_lock) {
      if (secureRandom == null) {
        LOGGER.info("creating and seeding secure random");
        try {
          secureRandom = SecureRandom.getInstance("SHA1PRNG");
          secureRandom.nextInt();
        } catch (NoSuchAlgorithmException ex) {
          throw new TexaiException(ex);
        }
        secureRandom.nextInt();
      }
      return secureRandom;
    }
  }

  /** Initializes the secure random from a serialized object.
   *
   * @param path the path to the previously serialized secure random
   * @return the initialized secure random
   */
  public static SecureRandom initializeSecureRandom(final String path) {
    //Preconditions
    assert path != null : "path must not be null";
    assert !path.isEmpty() : "path must not be empty";

    final File file = new File(path);
    if (file.exists()) {
      // read the secure random from a file to avoid the potentially long delay of creating and initializing it
      try {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(path))) {
          synchronized (secureRandom_lock) {
            secureRandom = (SecureRandom) in.readObject();
          }
          LOGGER.info("secure random loaded from " + path);
        }
      } catch (IOException | ClassNotFoundException ex) {
        throw new TexaiException(ex);
      }
    } else {
      serializeSecureRandom(path);
    }
    return secureRandom;
  }

  /** Serializes the secure random to a file for a subsequent restart.
   *
   * @param path the path to the previously serialized secure random
   */
  public static void serializeSecureRandom(final String path) {
    //Preconditions
    assert path != null : "path must not be null";
    assert !path.isEmpty() : "path must not be empty";

    try {
      // serialize the secure random in a file for reuse during a restart
      try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
        out.writeObject(getSecureRandom());
      }
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the root X509 certificate.
   *
   * @return the root X509 certificate
   */
  public static X509Certificate getRootX509Certificate() {
    return ROOT_X509_CERTIFICATE;
  }

  /** Gets the indicator whether the JCE unlimited strength jurisdiction policy files are installed.
   *
   * @return the indicator whether the JCE unlimited strength jurisdiction policy files are installed
   */
  public static synchronized boolean isJCEUnlimitedStrengthPolicy() {
    return isJCEUnlimitedStrenthPolicy;
  }

  /** Sets the indicator whether the JCE unlimited strength jurisdiction policy files are installed.
   *
   * @param _isJCEUnlimitedStrenthPolicy the indicator whether the JCE unlimited strength jurisdiction policy files are installed
   */
  public static synchronized void setIsJCEUnlimitedStrengthPolicy(boolean _isJCEUnlimitedStrenthPolicy) {
    isJCEUnlimitedStrenthPolicy = _isJCEUnlimitedStrenthPolicy;
  }

  /** Returns the maximum key length allowed by the ciphers on this JVM, which depends on whether the unlimited
   * strength encryption policy jar files have been downloaded and installed.
   *
   * @return the maximum allowed key size
   * @throws NoSuchAlgorithmException when the encryption algorithm cannot be found
   */
  public static int getMaxAllowedKeyLength() throws NoSuchAlgorithmException {
    return Cipher.getMaxAllowedKeyLength("AES");
  }

  /** Logs the cryptography providers. */
  public static void logProviders() {
    LOGGER.info("cryptography providers ...");
    final Provider[] providers = Security.getProviders();
    for (int i = 0; i != providers.length; i++) {
      LOGGER.info("  Name: " + providers[i].getName() + StringUtils.makeBlankString(15 - providers[i].getName().length()) + " Version: " + providers[i].getVersion());
    }
  }

  /** Logs the capabilities of the cryptography providers.
   * @param providerString the provider identifier
   */
  public static void logProviderCapabilities(final String providerString) {
    //Preconditions
    assert providerString != null : "providerString must not be null";
    assert !providerString.isEmpty() : "providerString must not be empty";

    final Provider provider = Security.getProvider(providerString);

    final Iterator<Object> propertyKey_iter = provider.keySet().iterator();

    LOGGER.info("cryptography provider " + providerString + " capabilities ...");
    final List<String> propertyStrings = new ArrayList<>();
    while (propertyKey_iter.hasNext()) {
      String propertyString = (String) propertyKey_iter.next();
      if (propertyString.startsWith("Alg.Alias.")) {
        // this indicates the entry refers to another entry
        propertyString = propertyString.substring("Alg.Alias.".length());
      }
      propertyStrings.add(propertyString);
    }
    Collections.sort(propertyStrings);
    for (final String propertyString : propertyStrings) {
      final String factoryClass = propertyString.substring(0, propertyString.indexOf('.'));
      final String name = propertyString.substring(factoryClass.length() + 1);
      LOGGER.info("  " + factoryClass + ": " + name);
    }
  }

  /** Creates a random 3072 bit RSA key pair.
   * @return a random 3072 bit RSA key pair
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws NoSuchProviderException  when an invalid provider is given
   * @throws InvalidAlgorithmParameterException when an invalid algorithm parameter is given
   */
  public static KeyPair generateRSAKeyPair3072() throws
          NoSuchAlgorithmException,
          NoSuchProviderException,
          InvalidAlgorithmParameterException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BOUNCY_CASTLE_PROVIDER);
    final AlgorithmParameterSpec algorithmParameterSpec = new RSAKeyGenParameterSpec(3072, RSAKeyGenParameterSpec.F4);
    keyPairGenerator.initialize(algorithmParameterSpec, getSecureRandom());
    return keyPairGenerator.generateKeyPair();
  }

  /** Creates a random 2048 bit RSA key pair.
   * @return a random 2048 bit RSA key pair
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws NoSuchProviderException  when an invalid provider is given
   * @throws InvalidAlgorithmParameterException when an invalid algorithm parameter is given
   */
  public static KeyPair generateRSAKeyPair2048() throws
          NoSuchAlgorithmException,
          NoSuchProviderException,
          InvalidAlgorithmParameterException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", BOUNCY_CASTLE_PROVIDER);
    final AlgorithmParameterSpec algorithmParameterSpec = new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4);
    keyPairGenerator.initialize(algorithmParameterSpec, getSecureRandom());
    return keyPairGenerator.generateKeyPair();
  }

  /** Gets the truststore that contains the single trusted root X.509 certificate.
   *
   * @return the truststore
   */
  public static synchronized KeyStore getTruststore() {
    if (truststore == null) {
      LOGGER.info("reading truststore");

      String filePath;
      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        filePath = "../X509Security/data/truststore.uber";
        File file = new File(filePath);
        if (!file.exists()) {
          filePath = "../Texai/X509Security/data/truststore.uber";
        }
        file = new File(filePath);
        if (!file.exists()) {
          // for MessageRouter & X.509 certificate server
          filePath = "data/truststore.uber";
        }
      } else {
        filePath = "../X509Security/data/truststore.jceks";
        File file = new File(filePath);
        if (!file.exists()) {
          filePath = "../Texai/X509Security/data/truststore.jceks";
        }
        file = new File(filePath);
        if (!file.exists()) {
          // for MessageRouter
          filePath = "data/truststore.jceks";
        }
      }
      final File serverKeyStoreFile = new File(filePath);
      assert serverKeyStoreFile.exists();
      try {
        truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
        throw new TexaiException(ex);
      }
    }
    //Postconditions
    assert truststore != null : "truststore must not be null";

    return truststore;
  }

  /** Generates an intermediate CA certificate, that is to be used to sign end-use certificates.
   *
   * @param myPublicKey the public key for this certificate
   * @param issuerPrivateKey the issuer's private key
   * @param issuerCertificate the issuer's certificate, which is either the root CA certificate or another intermediate
   * CA certificate
   * @param pathLengthConstraint the maximum number of CA certificates that may follow this certificate in a certification
   * path. (Note: One end-entity certificate will follow the final CA certificate in the path. The last certificate in a path
   * is considered an end-entity certificate, whether the subject of the certificate is a CA or not.)
   * @return an intermediate CA certificate
   *
   * @throws CertificateParsingException when the certificate cannot be parsed
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   */
  public static X509Certificate generateIntermediateX509Certificate(
          final PublicKey myPublicKey,
          final PrivateKey issuerPrivateKey,
          final X509Certificate issuerCertificate,
          int pathLengthConstraint)
          throws
          CertificateParsingException,
          CertificateEncodingException,
          NoSuchProviderException,
          NoSuchAlgorithmException,
          SignatureException,
          InvalidKeyException,
          IOException {
    //Preconditions
    assert myPublicKey != null : "myPublicKey must not be null";
    assert issuerPrivateKey != null : "issuerPrivateKey must not be null";
    assert issuerCertificate != null : "issuerCertificate must not be null";

    final X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
    x509V3CertificateGenerator.setSerialNumber(getNextSerialNumber());
    x509V3CertificateGenerator.setIssuerDN(issuerCertificate.getSubjectX500Principal());
    x509V3CertificateGenerator.setNotBefore(new Date(System.currentTimeMillis() - 10000L));
    final Date notAfterDate = new Date(System.currentTimeMillis() + VALIDITY_PERIOD);
    x509V3CertificateGenerator.setNotAfter(notAfterDate);
    final UUID rootUUID = UUID.randomUUID();
    final X500Principal x500Principal = new X500Principal("CN=texai.org,DC=IntermediateCertificate,UID=" + rootUUID);
    x509V3CertificateGenerator.setSubjectDN(x500Principal);
    x509V3CertificateGenerator.setPublicKey(myPublicKey);
    x509V3CertificateGenerator.setSignatureAlgorithm(DIGITAL_SIGNATURE_ALGORITHM);

    // see http://www.ietf.org/rfc/rfc3280.txt

    x509V3CertificateGenerator.addExtension(
            X509Extensions.AuthorityKeyIdentifier,
            false,
            new AuthorityKeyIdentifierStructure(issuerCertificate));
    x509V3CertificateGenerator.addExtension(
            X509Extensions.SubjectKeyIdentifier,
            false,
            new SubjectKeyIdentifierStructure(myPublicKey));
    x509V3CertificateGenerator.addExtension(
            X509Extensions.BasicConstraints,
            true,
            new BasicConstraints(pathLengthConstraint)); // is a CA certificate with specified certification path length
    final KeyUsage keyUsage = new KeyUsage(
            // the keyCertSign bit indicates that the subject public key may be used for verifying a signature on
            // certificates
            KeyUsage.keyCertSign
            | // the cRLSign indicates that the subject public key may be used for verifying a signature on revocation
            // information
            KeyUsage.cRLSign);
    x509V3CertificateGenerator.addExtension(
            X509Extensions.KeyUsage,
            true,
            keyUsage);
    final X509Certificate x509Certificate = x509V3CertificateGenerator.generate(issuerPrivateKey, BOUNCY_CASTLE_PROVIDER);

    //Postconditions
    try {
      x509Certificate.checkValidity();
      x509Certificate.verify(issuerCertificate.getPublicKey());
    } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
      throw new TexaiException(ex);
    }

    return x509Certificate;
  }

  /** Generates a signed end-use certificate that cannot be used to sign other certificates.
   *
   * @param myPublicKey the public key for this certificate
   * @param issuerPrivateKey the issuer's private key
   * @param issuerCertificate the issuer's certificate
   * @param domainComponent the domain component
   * @return a signed end-use certificate
   *
   * @throws CertificateParsingException when the certificate cannot be parsed
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   */
  public static X509Certificate generateX509Certificate(
          final PublicKey myPublicKey,
          final PrivateKey issuerPrivateKey,
          final X509Certificate issuerCertificate,
          final String domainComponent)
          throws
          CertificateParsingException,
          CertificateEncodingException,
          NoSuchProviderException,
          NoSuchAlgorithmException,
          SignatureException,
          InvalidKeyException,
          IOException {
    //Preconditions
    assert myPublicKey != null : "myPublicKey must not be null";
    assert issuerPrivateKey != null : "issuerPrivateKey must not be null";
    assert issuerCertificate != null : "issuerCertificate must not be null";

    return generateX509Certificate(
            myPublicKey,
            issuerPrivateKey,
            issuerCertificate,
            UUID.randomUUID(),
            domainComponent);
  }

  /** Generates a certificate path for a signed end-use certificate that cannot be used to sign other certificates, but can be used for authentication
   * and for message signing.
   *
   * @param myPublicKey the public key for this certificate
   * @param issuerPrivateKey the issuer's private key
   * @param issuerCertificate the issuer's X.509 certificate
   * @param issuerCertPath the issuer's certificate path
   * @param domainComponent the domain component, e.g. NodeRuntime
   * @return a certificate path for a signed end-use certificate
   *
   * @throws CertificateParsingException when the certificate cannot be parsed
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   * @throws CertificateException when an invalid certificate is present
   */
  public static CertPath generateX509CertificatePath(
          final PublicKey myPublicKey,
          final PrivateKey issuerPrivateKey,
          final X509Certificate issuerCertificate,
          final CertPath issuerCertPath,
          final String domainComponent)
          throws
          CertificateParsingException,
          CertificateEncodingException,
          NoSuchProviderException,
          NoSuchAlgorithmException,
          SignatureException,
          InvalidKeyException,
          IOException,
          CertificateException {
    //Preconditions
    assert myPublicKey != null : "myPublicKey must not be null";
    assert issuerPrivateKey != null : "issuerPrivateKey must not be null";
    assert issuerCertificate != null : "issuerCertificate must not be null";
    assert issuerCertPath != null : "issuerCertPath must not be null";

    final X509Certificate generatedCertificate = generateX509Certificate(
            myPublicKey,
            issuerPrivateKey,
            issuerCertificate,
            UUID.randomUUID(),
            domainComponent);
    final List<Certificate> certificateList = new ArrayList<>();
    certificateList.add(generatedCertificate);
    certificateList.addAll(issuerCertPath.getCertificates());
    return generateCertPath(certificateList);
  }

  /** Generates a signed end-use certificate that cannot be used to sign other certificates, but can be used for authentication
   * and for message signing.
   *
   * @param myPublicKey the public key for this certificate
   * @param issuerPrivateKey the issuer's private key
   * @param issuerCertificate the issuer's certificate
   * @param uid the subject UID
   * @param domainComponent the domain component, e.g. TexaiLauncher or NodeRuntime
   * @return a signed end-use certificate
   *
   * @throws CertificateParsingException when the certificate cannot be parsed
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   */
  public static X509Certificate generateX509Certificate(
          final PublicKey myPublicKey,
          final PrivateKey issuerPrivateKey,
          final X509Certificate issuerCertificate,
          final UUID uid,
          final String domainComponent)
          throws
          CertificateParsingException,
          CertificateEncodingException,
          NoSuchProviderException,
          NoSuchAlgorithmException,
          SignatureException,
          InvalidKeyException,
          IOException {
    //Preconditions
    assert myPublicKey != null : "myPublicKey must not be null";
    assert issuerPrivateKey != null : "issuerPrivateKey must not be null";
    assert issuerCertificate != null : "issuerCertificate must not be null";
    assert uid != null : "uid must not be null";

    final X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
    x509V3CertificateGenerator.setSerialNumber(getNextSerialNumber());
    x509V3CertificateGenerator.setIssuerDN(issuerCertificate.getSubjectX500Principal());
    x509V3CertificateGenerator.setNotBefore(new Date(System.currentTimeMillis() - 10000L));
    x509V3CertificateGenerator.setNotAfter(new Date(System.currentTimeMillis() + VALIDITY_PERIOD));
    final String x500PrincipalString;
    if (domainComponent == null || domainComponent.isEmpty()) {
      x500PrincipalString = "CN=texai.org,UID=" + uid;
    } else {
      x500PrincipalString = "CN=texai.org,DC=" + domainComponent + ",UID=" + uid;
    }
    final X500Principal x500Principal = new X500Principal(x500PrincipalString);
    x509V3CertificateGenerator.setSubjectDN(x500Principal);
    x509V3CertificateGenerator.setPublicKey(myPublicKey);
    x509V3CertificateGenerator.setSignatureAlgorithm(DIGITAL_SIGNATURE_ALGORITHM);

    // see http://www.ietf.org/rfc/rfc3280.txt

    x509V3CertificateGenerator.addExtension(
            X509Extensions.AuthorityKeyIdentifier,
            false,
            new AuthorityKeyIdentifierStructure(issuerCertificate));
    x509V3CertificateGenerator.addExtension(
            X509Extensions.SubjectKeyIdentifier,
            false,
            new SubjectKeyIdentifierStructure(myPublicKey));
    x509V3CertificateGenerator.addExtension(
            X509Extensions.BasicConstraints,
            true,
            new BasicConstraints(false)); // is not a CA certificate
    final KeyUsage keyUsage = new KeyUsage(
            // the digitalSignature usage indicates that the subject public key may be used with a digital signature
            // mechanism to support security services other than non-repudiation, certificate signing, or revocation
            // information signing
            KeyUsage.digitalSignature
            | // the nonRepudiation usage indicates that the subject public key may be used to verify digital signatures
            // used to provide a non-repudiation service which protects against the signing entity falsely denying some
            // action, excluding certificate or CRL signing
            KeyUsage.nonRepudiation
            | // the keyEncipherment usage indicates that the subject public key may be used for key transport, e.g. the
            // exchange of efficient symmetric keys in SSL
            KeyUsage.keyEncipherment
            | // the dataEncipherment usage indicates that the subject public key may be used for enciphering user data,
            // other than cryptographic keys
            KeyUsage.dataEncipherment
            | // the keyAgreement usage indicates that the subject public key may be used for key agreement, e.g. when a
            // Diffie-Hellman key is to be used for key management
            KeyUsage.keyAgreement
            | // the keyCertSign bit indicates that the subject public key may be used for verifying a signature on
            // certificates
            KeyUsage.keyCertSign
            | // the cRLSign indicates that the subject public key may be used for verifying a signature on revocation
            // information
            KeyUsage.cRLSign
            | // see http://www.docjar.com/html/api/sun/security/validator/EndEntityChecker.java.html - bit 0 needs to set for SSL
            // client authorization
            KeyUsage.encipherOnly);
    x509V3CertificateGenerator.addExtension(
            X509Extensions.KeyUsage,
            true,
            keyUsage);
    final X509Certificate x509Certificate = x509V3CertificateGenerator.generate(issuerPrivateKey, BOUNCY_CASTLE_PROVIDER);

    //Postconditions
    try {
      x509Certificate.checkValidity();
      x509Certificate.verify(issuerCertificate.getPublicKey());
    } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
      throw new TexaiException(ex);
    }
    assert x509Certificate.getKeyUsage()[0] : "must have digital signature key usage";

    return x509Certificate;
  }


  /** Returns whether the given certificate has the given key usage bit set, i.e. digitalSignature usage.
   *
   *       digitalSignature        (0)
   *       nonRepudiation          (1)
   *       keyEncipherment         (2)
   *       dataEncipherment        (3)
   *       keyAgreement            (4)
   *       keyCertSign             (5)
   *       cRLSign                 (6)
   *       encipherOnly            (7)
   *       decipherOnly            (8)
   *
   * @param x509Certificate the given certificate
   * @param keyUsageBitMask the given key usage bit, i.e. KeyUsage.digitalSignature
   * @return whether the given certificate has the given key usage bit set
   */
  public static boolean hasKeyUsage(
          final X509Certificate x509Certificate,
          final int keyUsageBitMask) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";

    final boolean[] keyUsage = x509Certificate.getKeyUsage();
    int certificateKeyUsageBitmask = 0;
    final int keyUsage_len = keyUsage.length - 1; // ignore pad bit
    for (int i = 0; i < keyUsage_len; i++) {
      if (keyUsage[i]) {
        certificateKeyUsageBitmask += Math.pow(2, (keyUsage_len - i - 1));
      }
    }

    return (certificateKeyUsageBitmask & keyUsageBitMask) != 0;
  }

  /** Gets the UUID from the subject name contained in the given X.509 certificate.
   *
   * @param x509Certificate the given X.509 certificate
   * @return the UUID
   */
  public static UUID getUUID(final X509Certificate x509Certificate) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";

    final String subjectString = x509Certificate.getSubjectX500Principal().toString();
    assert !subjectString.isEmpty() : "subject DN must not be empty";
    final int index = subjectString.indexOf("UID=");
    assert index > -1 : "UID not found in the subject DN";
    final String uuidString = subjectString.substring(index + 4);
    return UUID.fromString(uuidString);
  }

  /** Reads a DER encoded certificate from an input stream.
   * @param inputStream the input stream containing the DER encoded bytes of an X.509 certificate
   * @return the certificate
   * @throws CertificateException when an invalid certificate is read
   * @throws NoSuchProviderException when the cryptography provider cannot be found
   */
  public static X509Certificate readX509Certificate(final InputStream inputStream) throws CertificateException, NoSuchProviderException {
    //Preconditions
    assert inputStream != null : "inputStream must not be null";

    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", BOUNCY_CASTLE_PROVIDER);
    return (X509Certificate) certificateFactory.generateCertificate(inputStream);
  }

  /** Writes a DER encoded certificate to the given file path.
   * @param x509Certificate the X.509 certificate
   * @param filePath the given file path
   * @throws CertificateEncodingException if the certificate cannot be encoded
   * @throws IOException when an input/output error occurs
   */
  public static void writeX509Certificate(
          final X509Certificate x509Certificate,
          final String filePath)
          throws
          CertificateEncodingException,
          IOException {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";
    assert filePath != null : "filePath must not be null";
    assert !filePath.isEmpty() : "filePath must not be empty";
    try (OutputStream certificateOutputStream = new FileOutputStream(filePath)) {
      certificateOutputStream.write(x509Certificate.getEncoded());
      certificateOutputStream.flush();
    }
  }

  /** Finds or creates the keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreateKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    if (isJCEUnlimitedStrengthPolicy()) {
      assert filePath.endsWith(".uber") : "file extension must be .uber";
    } else {
      assert filePath.endsWith(".jceks") : "file extension must be .jceks";
    }
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    KeyStore keyStore;
    if (isJCEUnlimitedStrengthPolicy()) {
      keyStore = KeyStore.getInstance("UBER", BOUNCY_CASTLE_PROVIDER);
    } else {
      keyStore = KeyStore.getInstance("JCEKS");
    }
    if (keyStoreFile.exists()) {
      try (final FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(keyStoreInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(keyStoreOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Finds or creates the jceks keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreateJceksKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".jceks") : "file extension must be .jceks";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    final KeyStore keyStore = KeyStore.getInstance("JCEKS");
    if (keyStoreFile.exists()) {
      try (final FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(keyStoreInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(keyStoreOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Finds or creates the uber keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreateUberKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".uber") : "file extension must be .uber";
    assert isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy file must be installed";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    final KeyStore keyStore = KeyStore.getInstance("UBER", BOUNCY_CASTLE_PROVIDER);
    if (keyStoreFile.exists()) {
      try (final FileInputStream keyStoreInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(keyStoreInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(keyStoreOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Copies the given keystore from the .uber format to the .jceks format.
   *
   * @param uberKeyStorePath the .uber keystore path
   * @param uberKeyStorePassword the .uber keystore password
   * @param jceksKeyStorePath the .jceks keystore path
   * @param jceksKeyStorePassword the .jceks keystore password
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   * @throws UnrecoverableEntryException if the keystore entry cannot be recovered with the provided password and alias
   */
  public static synchronized void copyKeyStoreUberToJceks(
          final String uberKeyStorePath,
          final char[] uberKeyStorePassword,
          final String jceksKeyStorePath,
          final char[] jceksKeyStorePassword) throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException,
          UnrecoverableEntryException {
    //Preconditions
    assert uberKeyStorePath != null : "uberKeyStorePath must not be null";
    assert !uberKeyStorePath.isEmpty() : "uberKeyStorePath must not be empty";
    assert uberKeyStorePath.endsWith(".uber") : "uber keystore file extension must be .uber";
    assert uberKeyStorePassword != null : "uberKeyStorePassword must not be null";
    assert jceksKeyStorePath != null : "jceksKeyStorePath must not be null";
    assert !jceksKeyStorePath.isEmpty() : "jceksKeyStorePath must not be empty";
    assert jceksKeyStorePath.endsWith(".jceks") : "jceks keystore file extension must be .jceks";
    assert uberKeyStorePassword != null : "uberKeyStorePassword must not be null";

    LOGGER.info("copying keystore contents of " + uberKeyStorePath + " to " + jceksKeyStorePath);
    final KeyStore uberKeyStore = findOrCreateUberKeyStore(uberKeyStorePath, uberKeyStorePassword);
    final KeyStore jceksKeyStore = findOrCreateJceksKeyStore(jceksKeyStorePath, jceksKeyStorePassword);
    final Enumeration<String> aliases_enumeration = uberKeyStore.aliases();
    final PasswordProtection uberPasswordProtection = new PasswordProtection(uberKeyStorePassword);
    final PasswordProtection jceksPasswordProtection = new PasswordProtection(jceksKeyStorePassword);
    while (aliases_enumeration.hasMoreElements()) {
      final String alias = aliases_enumeration.nextElement();
      final KeyStore.Entry entry = uberKeyStore.getEntry(alias, uberPasswordProtection);
      assert entry != null;
      jceksKeyStore.setEntry(alias, entry, jceksPasswordProtection);
      LOGGER.info("  copied entry: " + alias);
    }
    jceksKeyStore.store(new FileOutputStream(jceksKeyStorePath), jceksKeyStorePassword);
  }

  /** Finds the keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".uber") || filePath.endsWith(".jceks") : "file extension must be .uber or .jceks";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    KeyStore keyStore;
    if (filePath.endsWith(".uber")) {
      assert isJCEUnlimitedStrengthPolicy() : "must have unlimited security policy files installed";
      keyStore = KeyStore.getInstance("UBER", BOUNCY_CASTLE_PROVIDER);
    } else {
      keyStore = KeyStore.getInstance("JCEKS");
    }
    if (keyStoreFile.exists()) {
      try (FileInputStream keyStoreStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(keyStoreStream, password);
      }
      return keyStore;
    } else {
      return null;
    }
  }

  /** Finds or creates the PKCS12 keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .pkcs12 extension
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreatePKCS12KeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".p12") : "file extension must be .p12";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    final KeyStore keyStore;
    if (isJCEUnlimitedStrengthPolicy()) {
      keyStore = KeyStore.getInstance("pkcs12", BOUNCY_CASTLE_PROVIDER);
    } else {
      keyStore = KeyStore.getInstance("pkcs12");
    }
    if (keyStoreFile.exists()) {
      try (final FileInputStream fileInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(fileInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(fileOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Finds or creates the BKS keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .bks extension
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreateBKSKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".bks") : "file extension must be .bks";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    final KeyStore keyStore;
    keyStore = KeyStore.getInstance("BKS");
    if (keyStoreFile.exists()) {
      try (final FileInputStream fileInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(fileInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(fileOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Finds or creates the JKS keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .jks extension
   * @param password the keystore password
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data,
   * if a password is required but not given, or if the given password was incorrect
   * @throws NoSuchAlgorithmException if the algorithm used to check the integrity of the keystore cannot be found
   * @throws CertificateException if any of the certificates in the keystore could not be loaded
   * @throws NoSuchProviderException if the cryptography provider cannot be found
   */
  public static KeyStore findOrCreateJKSKeyStore(
          final String filePath,
          final char[] password)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert filePath != null : "filePath must not be null";
    assert filePath.endsWith(".jks") : "file extension must be .jks";
    assert password != null : "password must not be null";
    assert password.length > 0 : "password must not be empty";

    final File keyStoreFile = new File(filePath);
    final KeyStore keyStore;
    keyStore = KeyStore.getInstance("JKS");
    if (keyStoreFile.exists()) {
      try (final FileInputStream fileInputStream = new FileInputStream(keyStoreFile)) {
        keyStore.load(fileInputStream, password);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream fileOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(fileOutputStream, password);
      }
    }
    return keyStore;
  }

  /** Adds an entry to the specified keystore, creating the keystore if it does not already exist.
   *
   * @param keyStoreFilePath the file path to the keystore
   * @param keyStorePassword the keystore's password
   * @param certPath the certificate path to add
   * @param privateKey the private key associated with the first certificate in the path
   * @return the keystore
   * @throws KeyStoreException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws NoSuchProviderException
   */
  public static KeyStore addEntryToKeyStore(
          final String keyStoreFilePath,
          final char[] keyStorePassword,
          final CertPath certPath,
          final PrivateKey privateKey)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert keyStoreFilePath != null : "keyStoreFilePath must not be null";
    assert !keyStoreFilePath.isEmpty() : "keyStoreFilePath must not be empty";
    assert keyStorePassword != null : "keyStorePassword must not be null";

    return addEntryToKeyStore(
            keyStoreFilePath,
            keyStorePassword,
            X509Utils.ENTRY_ALIAS,
            certPath,
            privateKey);
  }

  /** Adds an entry to the specified keystore, creating the keystore if it does not already exist.
   *
   * @param keyStoreFilePath the file path to the keystore
   * @param keyStorePassword the keystore's password
   * @param alias the entry alias
   * @param certPath the certificate path to add
   * @param privateKey the private key associated with the first certificate in the path
   * @return the keystore
   * @throws KeyStoreException
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws CertificateException
   * @throws NoSuchProviderException
   */
  public static KeyStore addEntryToKeyStore(
          final String keyStoreFilePath,
          final char[] keyStorePassword,
          final String alias,
          final CertPath certPath,
          final PrivateKey privateKey)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert keyStoreFilePath != null : "keyStoreFilePath must not be null";
    assert !keyStoreFilePath.isEmpty() : "keyStoreFilePath must not be empty";
    assert keyStorePassword != null : "keyStorePassword must not be null";
    assert alias != null : "alias must not be null";
    assert !alias.isEmpty() : "alias must not be empty";

    final KeyStore keyStore = X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
    final Certificate[] certificateChain = new Certificate[certPath.getCertificates().size() + 1];
    for (int i = 0; i < certPath.getCertificates().size(); i++) {
      certificateChain[i] = certPath.getCertificates().get(i);
    }
    certificateChain[certPath.getCertificates().size()] = X509Utils.getRootX509Certificate();
    keyStore.setKeyEntry(
            alias,
            privateKey,
            keyStorePassword,
            certificateChain);
    keyStore.store(new FileOutputStream(keyStoreFilePath), keyStorePassword);

    //Postconditions
    assert keyStore != null : "keyStore must not be null";

    return keyStore;
  }

  /** Resets the certificate serial number.
   *
   * @throws IOException if an input/output error occurred
   */
  protected static void resetSerialNumber() throws IOException {
    File serialNumberFile = new File("../X509Security/data/certificate-serial-nbr.txt");
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serialNumberFile))) {
      bufferedWriter.write("0");
    }
  }

  /** Returns the next certificate serial number.
   *
   * @return the next certificate serial number
   * @throws IOException if an input/output error occurred
   */
  protected static BigInteger getNextSerialNumber() throws IOException {
    @SuppressWarnings("UnusedAssignment")
    File serialNumberFile = null;
    File dataDirectoryFile = new File("../X509Security/data");
    if (dataDirectoryFile.exists()) {
      serialNumberFile = new File("../X509Security/data/certificate-serial-nbr.txt");
      if (!serialNumberFile.exists()) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serialNumberFile))) {
          bufferedWriter.write("0");
        }
      }
    } else {
      // for testing the message router
      dataDirectoryFile = new File("data");
      assert dataDirectoryFile.exists();
      serialNumberFile = new File("data/certificate-serial-nbr.txt");
      if (!serialNumberFile.exists()) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serialNumberFile))) {
          bufferedWriter.write("0");
        }
      }
    }
    final String line;
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(serialNumberFile))) {
      line = bufferedReader.readLine();
    }
    final Long nextSerialNumber = Long.valueOf(line.trim()) + 1L;
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serialNumberFile))) {
      bufferedWriter.write(String.valueOf(nextSerialNumber));
    }
    return new BigInteger(String.valueOf(nextSerialNumber));
  }

  /** Returns whether this is the trusted development system.
   *
   * @return whether this is the trusted development system
   */
  public static boolean isTrustedDevelopmentSystem() {
    final File keyStorePasswordFile = new File(System.getenv("SECURITY_DIR") + "/texai-keystore-password.txt");
    return keyStorePasswordFile.exists();
  }

  /** Returns a certificate path consisting of the given certificate array.
   *
   * @param certificates the given certificate array
   * @return the certificate path consisting of the given certificate list
   * @throws CertificateException if an invalid certificate is present
   * @throws NoSuchProviderException if the cryptography service provider is not found
   */
  public static CertPath generateCertPath(final Certificate[] certificates) throws CertificateException, NoSuchProviderException {
    final List<Certificate> certificateList = new ArrayList<>();
    for (final Certificate certificate : certificates) {
      certificateList.add(certificate);
    }
    return generateCertPath(certificateList);
  }

  /** Returns a certificate path consisting of the given certificate list.
   *
   * @param certificateList the given certificate list
   * @return the certificate path consisting of the given certificate list
   * @throws CertificateException if an invalid certificate is present
   * @throws NoSuchProviderException if the cryptography service provider is not found
   */
  public static CertPath generateCertPath(final List<Certificate> certificateList) throws CertificateException, NoSuchProviderException {
    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", X509Utils.BOUNCY_CASTLE_PROVIDER);
    return certificateFactory.generateCertPath(certificateList);
  }

  /** Validates the given X.509 certificate path, throwing an exception if the path is invalid.
   *
   * @param certPath the given X.509 certificate path, which does not include the trust anchor in contrast to a
   * certificate chain that does
   *
   * @throws InvalidAlgorithmParameterException if an invalid certificate path validation parameter is provided
   * @throws NoSuchAlgorithmException if an invalid encryption algorithm is specified
   * @throws CertPathValidatorException if the given x.509 certificate path is invalid
   */
  public static void validateCertificatePath(final CertPath certPath) throws
          InvalidAlgorithmParameterException,
          NoSuchAlgorithmException,
          CertPathValidatorException {
    //Preconditions
    assert certPath != null : "certPath must not be null";

    final Set<TrustAnchor> trustAnchors = new HashSet<>();
    trustAnchors.add(new TrustAnchor(
            X509Utils.getRootX509Certificate(),
            null)); // nameConstraints
    final PKIXParameters params = new PKIXParameters(trustAnchors);
    params.setSigProvider(BOUNCY_CASTLE_PROVIDER);
    params.setRevocationEnabled(false);
    final CertPathValidator certPathValidator = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
    certPathValidator.validate(certPath, params);
  }

  /** Generates the X.509 security information.
   *
   * @param keyPair the key pair for the generated certificate
   * @param issuerPrivateKey the issuer's private key
   * @param issuerCertificate the issuer's certificate
   * @param uid the generated certificate's subject UID
   * @param keystorePassword the keystore password
   * @param isJCEUnlimitedStrengthPolicy the indicator whether the generated X.509 security information will be
   * hosted on a system with unlimited strength policy
   * @param domainComponent the domain component
   * @return the X509 security information
   */
  public static X509SecurityInfo generateX509SecurityInfo(
          final KeyPair keyPair,
          final PrivateKey issuerPrivateKey,
          final X509Certificate issuerCertificate,
          final UUID uid,
          final char[] keystorePassword,
          final boolean isJCEUnlimitedStrengthPolicy,
          final String domainComponent) {
    //Preconditions
    assert keyPair != null : "keyPair must not be null";
    assert issuerPrivateKey != null : "issuerPrivateKey must not be null";
    assert issuerCertificate != null : "issuerCertificate must not be null";
    assert uid != null : "uid must not be null";
    assert keystorePassword != null : "keystorePassword must not be null";

    try {
      final X509Certificate x509Certificate = generateX509Certificate(
              keyPair.getPublic(),
              issuerPrivateKey,
              issuerCertificate,
              uid,
              domainComponent);
      assert X509Utils.isJCEUnlimitedStrengthPolicy();

      final KeyStore keyStore;
      if (isJCEUnlimitedStrengthPolicy) {
        keyStore = KeyStore.getInstance("UBER", BOUNCY_CASTLE_PROVIDER);
      } else {
        keyStore = KeyStore.getInstance("JCEKS");
      }
      keyStore.load(null, null);
      keyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              keyPair.getPrivate(),
              keystorePassword,
              new Certificate[]{x509Certificate, X509Utils.getRootX509Certificate()});

      return new X509SecurityInfo(
              X509Utils.getTruststore(),
              keyStore,
              keystorePassword, null);
    } catch (NoSuchProviderException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the X509 security information for the given keystore and private key alias.
   *
   * @param keyStoreFilePath the file path to the keystore
   * @param keyStorePassword the keystore password
   * @param alias the private key entry alias
   * @return the X509 security information for a test client
   */
  public static X509SecurityInfo getX509SecurityInfo(
          final String keyStoreFilePath,
          final char[] keyStorePassword,
          final String alias) {
    //Preconditions
    assert keyStoreFilePath != null : "keyStoreFilePath must not be null";
    assert !keyStoreFilePath.isEmpty() : "keyStoreFilePath must not be empty";
    assert keyStoreFilePath.endsWith(".uber") || keyStoreFilePath.endsWith(".jceks") : "keystore file extension must be .uber or .jceks";
    assert keyStorePassword != null : "keyStorePassword must not be null";

    try {
      return new X509SecurityInfo(
              X509Utils.getTruststore(),
              findOrCreateKeyStore(keyStoreFilePath, keyStorePassword), // keyStore
              keyStorePassword,
              alias);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Initializes the installer keystore on the trusted development system, from where it is copied into the distributed code. */
  public static synchronized void initializeInstallerKeyStore() {
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    String filePath = "data/installer-keystore.uber";
    File file = new File(filePath);
    if (file.exists()) {
      // do not overwrite it
      return;
    }
    try {
      LOGGER.info("creating the installer keystores");
      // the installer keystore consists of the single client X.509 certificate, which is generated and signed by
      // the Texai root certificate on the developement system that has the root private key.
      final KeyPair installerKeyPair = X509Utils.generateRSAKeyPair2048();
      final X509Certificate installerX509Certificate = X509Utils.generateX509Certificate(
              installerKeyPair.getPublic(),
              X509Utils.getRootPrivateKey(),
              X509Utils.getRootX509Certificate(), null);


      // proceed as though the JCE unlimited strength jurisdiction policy files are installed, which they will be on the
      // trusted development system.
      LOGGER.info("creating installer-keystore.uber");
      assert X509Utils.isJCEUnlimitedStrengthPolicy();
      KeyStore installerKeyStore = X509Utils.findOrCreateKeyStore(filePath, INSTALLER_KEYSTORE_PASSWORD);
      installerKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              installerKeyPair.getPrivate(),
              INSTALLER_KEYSTORE_PASSWORD,
              new Certificate[]{installerX509Certificate, X509Utils.getRootX509Certificate()});
      installerKeyStore.store(new FileOutputStream(filePath), INSTALLER_KEYSTORE_PASSWORD);

      // then proceed after disabling the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(false);
      filePath = "data/installer-keystore.jceks";
      LOGGER.info("creating installer-keystore.jceks");
      installerKeyStore = X509Utils.findOrCreateKeyStore(filePath, INSTALLER_KEYSTORE_PASSWORD);
      installerKeyStore.setKeyEntry(
              X509Utils.ENTRY_ALIAS,
              installerKeyPair.getPrivate(),
              INSTALLER_KEYSTORE_PASSWORD,
              new Certificate[]{installerX509Certificate, X509Utils.getRootX509Certificate()});
      installerKeyStore.store(new FileOutputStream(filePath), INSTALLER_KEYSTORE_PASSWORD);
      // restore the JCE unlimited strength jurisdiction policy files indicator
      X509Utils.setIsJCEUnlimitedStrengthPolicy(true);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }
  }

  /** Generates a self-signed certificate to use as a CA root certificate.
   *
   * @param keyPair the root public/private key pair
   * @return a self-signed CA root certificate
   *
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   */
  protected static X509Certificate generateRootX509Certificate(final KeyPair keyPair)
          throws
          CertificateEncodingException,
          NoSuchProviderException,
          NoSuchAlgorithmException,
          SignatureException,
          InvalidKeyException,
          IOException {
    //Preconditions
    assert keyPair != null : "keyPair must not be null";

    final X509V3CertificateGenerator x509V3CertificateGenerator = new X509V3CertificateGenerator();
    resetSerialNumber();
    x509V3CertificateGenerator.setSerialNumber(getNextSerialNumber());
    final UUID rootUUID = UUID.randomUUID();
    final X500Principal rootX500Principal = new X500Principal("CN=texai.org,O=Texai Certification Authority,UID=" + rootUUID);
    x509V3CertificateGenerator.setIssuerDN(rootX500Principal);
    x509V3CertificateGenerator.setNotBefore(new Date(System.currentTimeMillis() - 10000L));
    final Date notAfterDate = new Date(System.currentTimeMillis() + VALIDITY_PERIOD);
    x509V3CertificateGenerator.setNotAfter(notAfterDate);
    x509V3CertificateGenerator.setSubjectDN(rootX500Principal);
    x509V3CertificateGenerator.setPublicKey(keyPair.getPublic());
    x509V3CertificateGenerator.setSignatureAlgorithm(DIGITAL_SIGNATURE_ALGORITHM);

    try {
      x509V3CertificateGenerator.addExtension(
              X509Extensions.SubjectKeyIdentifier,
              false,
              new SubjectKeyIdentifierStructure(keyPair.getPublic()));
      x509V3CertificateGenerator.addExtension(
              X509Extensions.BasicConstraints,
              true,
              new BasicConstraints(true)); // is a CA certificate with an unlimited certification path length
      final KeyUsage keyUsage = new KeyUsage(
              // the keyCertSign bit indicates that the subject public key may be used for verifying a signature on
              // certificates
              KeyUsage.keyCertSign
              | // the cRLSign indicates that the subject public key may be used for verifying a signature on revocation
              // information
              KeyUsage.cRLSign);
      x509V3CertificateGenerator.addExtension(
              X509Extensions.KeyUsage,
              true,
              keyUsage);
      final X509Certificate rootX509Certificate = x509V3CertificateGenerator.generate(keyPair.getPrivate(), BOUNCY_CASTLE_PROVIDER);

      //Postconditions
      rootX509Certificate.checkValidity();
      rootX509Certificate.verify(keyPair.getPublic());

      return rootX509Certificate;
    } catch (IllegalStateException | NoSuchProviderException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | CertificateException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Creates the Texai root X.509 certificate keystore on the trusted development system.  This
   * keystore also includes a jar-signing certificate.
   */
  protected static synchronized void createTexaiRootKeyStore() {
    //Preconditions
    assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy();

    if (!isTrustedDevelopmentSystem()) {
      return;
    }
    final char[] keyStorePassword = getRootKeyStorePassword();
    assert keyStorePassword != null;
    final String filePath = System.getenv("SECURITY_DIR") + "/texai-keystore.jceks";
    final File serverKeyStoreFile = new File(filePath);
    if (serverKeyStoreFile.exists()) {
      // do not overwrite it
      return;
    }
    try {
      LOGGER.info("creating Texai root key pair");
      final KeyPair rootKeyPair = generateRSAKeyPair3072();
      LOGGER.info("creating Texai root X.509 certificate");
      final X509Certificate rootX509Certificate = generateRootX509Certificate(rootKeyPair);
      LOGGER.info("root certificate:\n" + rootX509Certificate);
      final StringBuilder stringBuilder = new StringBuilder();
      for (final byte b : rootX509Certificate.getEncoded()) {
        stringBuilder.append(Byte.toString(b));
        stringBuilder.append(", ");
      }
      LOGGER.info("\n" + stringBuilder.toString());
      LOGGER.info("creating Texai root X.509 certificate keystore");
      final KeyStore rootKeyStore = X509Utils.findOrCreateJceksKeyStore(filePath, keyStorePassword);
      rootKeyStore.setKeyEntry(
              ROOT_ALIAS,
              rootKeyPair.getPrivate(),
              keyStorePassword,
              new Certificate[]{rootX509Certificate});

      // create and store the jar-signer certificate
      LOGGER.info("creating jar-signer key pair");
      final KeyPair jarSignerKeyPair = generateRSAKeyPair2048();
      LOGGER.info("creating jar-signer X.509 certificate");
      final UUID jarSignerUUID = UUID.randomUUID();
      LOGGER.info("jar-signer UUID: " + jarSignerUUID);
      final X509Certificate jarSignerX509Certificate = generateX509Certificate(
              jarSignerKeyPair.getPublic(),
              rootKeyPair.getPrivate(),
              rootX509Certificate,
              jarSignerUUID,
              "RootCertificate"); // domainComponent
      LOGGER.info("jar-signer certificate:\n" + jarSignerX509Certificate);
      rootKeyStore.setKeyEntry(
              JAR_SIGNER_ALIAS,
              jarSignerKeyPair.getPrivate(),
              keyStorePassword,
              new Certificate[]{jarSignerX509Certificate, rootX509Certificate});
      rootKeyStore.store(new FileOutputStream(filePath), keyStorePassword);

      //Postconditions
      final PrivateKey privateKey = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, keyStorePassword);
      assert privateKey != null;

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException | UnrecoverableKeyException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the root keystore password, or null if not executing on a trusted root system.
   *
   * @return the root keystore password, or null if not executing on a trusted root system
   */
  protected static char[] getRootKeyStorePassword() {
    if (!isTrustedDevelopmentSystem()) {
      return null;
    }
    File keyStorePasswordFile = new File(System.getenv("SECURITY_DIR") + "/texai-keystore-password.txt");
    try {
      final char[] keyStorePassword;
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(keyStorePasswordFile))) {
        final String keyStorePasswordString = bufferedReader.readLine().trim();
        keyStorePassword = keyStorePasswordString.toCharArray();
        assert keyStorePassword != null;
        assert keyStorePassword.length > 0;
      }
      return keyStorePassword;
    } catch (Exception ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the root private key or null if not executing on a trusted root system.
   *
   * @return the root private key or null if not executing on a trusted root system
   */
  public static PrivateKey getRootPrivateKey() {
    if (!isTrustedDevelopmentSystem()) {
      return null;
    }
    try {
      final String filePath = System.getenv("SECURITY_DIR") + "/texai-keystore.jceks";
      final File serverKeyStoreFile = new File(filePath);
      assert serverKeyStoreFile.exists();
      final char[] keyStorePassword = getRootKeyStorePassword();
      assert keyStorePassword != null;
      final boolean isJCEUnlimitedStrenthPolicy1 = isJCEUnlimitedStrengthPolicy();
      setIsJCEUnlimitedStrengthPolicy(true);
      final KeyStore rootKeyStore = findKeyStore(System.getenv("SECURITY_DIR") + "/texai-keystore.jceks", getRootKeyStorePassword());
      setIsJCEUnlimitedStrengthPolicy(isJCEUnlimitedStrenthPolicy1);
      assert rootKeyStore != null;
      final PrivateKey privateKey = (PrivateKey) rootKeyStore.getKey(ROOT_ALIAS, keyStorePassword);

      //Postconditions
      assert privateKey != null : "privateKey must not be null";

      return privateKey;
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | UnrecoverableKeyException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Initializes the truststore on the trusted development system, from where the truststore is copied to the
   * code repository. */
  public static synchronized void initializeTrustore() {
    if (!isTrustedDevelopmentSystem()) {
      return;
    }

    // The truststore consists of the single Texai root X.509 certificate, which was generated and self-signed on the
    // developement system that has its private key. Proceed as though the JCE unlimited strength jurisdiction policy
    // files are installed, which they will be on the trusted development system.
    assert isJCEUnlimitedStrengthPolicy();
    String filePath = "data/truststore.uber";
    File truststoreFile = new File(filePath);


//    if (truststoreFile.exists()) {
//      // do not overwrite it
//      return;
//    }

    try {
      LOGGER.info("creating truststore.uber");
      truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);

      // then proceed after disabling the JCE unlimited strength jurisdiction policy files indicator
      setIsJCEUnlimitedStrengthPolicy(false);
      filePath = "data/truststore.jceks";
      LOGGER.info("creating truststore.jceks");
      truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);

      filePath = "data/truststore.bks";
      LOGGER.info("creating truststore.bks");
      truststore = X509Utils.findOrCreateBKSKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);
      // restore the JCE unlimited strength jurisdiction policy files indicator
      setIsJCEUnlimitedStrengthPolicy(true);

      // create the PKCS12 keystore from which the trusted certificate can be imported into a web browser
      final String pkcs12FilePath = "data/truststore.p12";
      final File pkcs12TruststoreFile = new File(pkcs12FilePath);
      if (pkcs12TruststoreFile.exists()) {
        // do not overwrite it
        return;
      }
      LOGGER.info("creating truststore.p12");
      final KeyStore pkcs12Truststore = X509Utils.findOrCreatePKCS12KeyStore(pkcs12FilePath, TRUSTSTORE_PASSWORD);
      pkcs12Truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      pkcs12Truststore.store(new FileOutputStream(pkcs12FilePath), TRUSTSTORE_PASSWORD);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }
}
