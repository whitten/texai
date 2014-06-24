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
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import sun.security.x509.X509CertImpl;

/** X509 utilities adapted from "Beginning Cryptography With Java", David Hook, WROX.
 *
 * How to regenerate the root X.509 certificate on the development system.
 * (1) delete /home/reed/texai-keystore.jceks
 * (2) run the JUnit test X509UtilsTest.java - expect several unit test failures [none on most recent regeneration]
 * (3) copy the byte array values from the unit test output (root certificate bytes...)
 *     into the array initialialization value for ROOT_CERTIFICATE_BYTES.
 * (4) delete X509Security/data/truststore.*, test-client-keystore.*, test-server-keystore.*
 * (5) re-run the unit test correcting for the new root UID
 * (6) likewise correct KeyStoreTestUtilsTest, X509SecurityInfoTest and TexaiSSLContextFactoryTest
 * (7) ensure that Git updates the new keystore files when committing
 * (7) copy truststore.uber and truststore.jks files to the Network, AlbusHCNSupport and X509CertificateServer
 *     data directories
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
  public static final String DIGITAL_SIGNATURE_ALGORITHM = "SHA512withRSA";
  /** the indicator whether the JCE unlimited strength jurisdiction policy files are installed */
  private static boolean isJCEUnlimitedStrenthPolicy;
  /** the root certificate bytes */
  private final static byte[] ROOT_CERTIFICATE_BYTES = {
    48, -126, 4, -94, 48, -126, 3, 10, -96, 3, 2, 1, 2, 2, 2, 3, -72, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13,
    1, 1, 13, 5, 0, 48, 114, 49, 52, 48, 50, 6, 10, 9, -110, 38, -119, -109, -14, 44, 100, 1, 1, 12, 36, 101, 100,
    54, 100, 54, 55, 49, 56, 45, 56, 48, 100, 101, 45, 52, 56, 52, 56, 45, 97, 102, 52, 51, 45, 102, 101, 100, 55,
    98, 100, 98, 97, 51, 99, 51, 54, 49, 38, 48, 36, 6, 3, 85, 4, 10, 12, 29, 84, 101, 120, 97, 105, 32, 67, 101, 114,
    116, 105, 102, 105, 99, 97, 116, 105, 111, 110, 32, 65, 117, 116, 104, 111, 114, 105, 116, 121, 49, 18, 48, 16, 6,
    3, 85, 4, 3, 12, 9, 116, 101, 120, 97, 105, 46, 111, 114, 103, 48, 30, 23, 13, 49, 52, 48, 54, 50, 48, 49, 54, 50,
    57, 49, 53, 90, 23, 13, 50, 52, 48, 54, 49, 55, 49, 54, 50, 57, 50, 53, 90, 48, 114, 49, 52, 48, 50, 6, 10, 9, -110,
    38, -119, -109, -14, 44, 100, 1, 1, 12, 36, 101, 100, 54, 100, 54, 55, 49, 56, 45, 56, 48, 100, 101, 45, 52, 56, 52,
    56, 45, 97, 102, 52, 51, 45, 102, 101, 100, 55, 98, 100, 98, 97, 51, 99, 51, 54, 49, 38, 48, 36, 6, 3, 85, 4, 10, 12,
    29, 84, 101, 120, 97, 105, 32, 67, 101, 114, 116, 105, 102, 105, 99, 97, 116, 105, 111, 110, 32, 65, 117, 116, 104,
    111, 114, 105, 116, 121, 49, 18, 48, 16, 6, 3, 85, 4, 3, 12, 9, 116, 101, 120, 97, 105, 46, 111, 114, 103, 48, -126,
    1, -94, 48, 13, 6, 9, 42, -122, 72, -122, -9, 13, 1, 1, 1, 5, 0, 3, -126, 1, -113, 0, 48, -126, 1, -118, 2, -126, 1,
    -127, 0, -125, 91, 21, -120, -83, -108, -52, 15, -18, -128, -104, -68, -70, -9, 58, 58, -111, 5, -47, -7, -119, 110,
    63, -24, 89, -61, -98, -108, -53, -84, 56, -65, 62, -40, 31, -97, -99, -121, 64, -77, 43, -15, -12, -16, -108, 110,
    101, -12, -7, -98, -50, -62, -121, -41, -69, -118, 37, 19, -3, 119, 26, 111, -80, 62, 124, -99, -42, 86, 125, 32,
    -74, 97, 79, -26, 3, -30, 13, -61, 35, -54, 44, 114, -43, 84, -13, -33, 80, 22, 73, 93, 77, 90, 12, -117, 50, -104,
    63, 38, 99, -109, -119, -118, 19, 86, 18, -48, 87, -14, 119, 69, -67, -69, -74, -13, 24, 31, 60, -79, -62, -2, -114,
    -118, -15, 121, 68, 116, 67, -97, -9, -69, -36, -94, -33, -93, 12, -46, -105, -92, 21, 27, 120, -58, -37, 5, 47, -21,
    106, 25, -101, 3, -104, 31, -5, 60, -89, -74, 20, 25, 65, -116, -75, 48, -8, 50, -11, 70, 108, -49, -43, -35, 67, 106,
    -51, 127, 39, 87, -93, 71, -10, 103, -13, -54, 101, -80, 15, 11, 112, 19, -107, -44, -49, -63, 86, -112, -74, 9, 102,
    -124, 81, 74, -98, -109, 44, 29, 37, 42, 106, 87, -58, -128, -58, 67, 73, -39, -103, -30, -2, -13, 121, -90, -95, 120,
    -4, 20, 114, 8, 97, 40, -26, 38, -96, -87, -4, 6, -87, -48, -53, 72, 10, 1, -62, -15, -2, 54, -67, 3, 4, -115, -90, 31,
    -25, 102, -30, 89, 124, -46, -91, -83, 83, 95, -39, -70, 57, -121, -13, -35, 105, -84, -33, -30, -93, -94, -79, -7,
    -15, 21, -15, 36, -11, -92, 90, 36, 61, 110, 103, 66, 31, 103, -71, 24, 4, 45, -72, -60, 26, 45, -123, 11, 0, 97, -34,
    -113, -99, -99, -33, -71, 102, 127, 29, 36, 95, -17, 0, -3, -97, -124, 117, -52, -92, -23, -41, -45, 76, -115, 61, -38,
    -44, 52, -51, 94, -118, 110, -126, -7, -51, -44, -69, -19, 88, -25, -28, -89, -75, -113, 26, -99, -16, -90, 97, 56,
    -91, 26, -58, 22, 81, 48, -92, -65, -95, -28, 50, -73, 110, -63, -63, 43, 111, 76, 58, 102, 96, 25, -4, 48, -52, -20,
    123, 100, 21, 116, -26, 65, 115, -107, 2, 3, 1, 0, 1, -93, 66, 48, 64, 48, 29, 6, 3, 85, 29, 14, 4, 22, 4, 20, 1, 119,
    4, -4, 27, -14, 82, -42, -46, -98, -16, -87, -109, -19, -27, -102, 72, -75, -126, -44, 48, 15, 6, 3, 85, 29, 19, 1, 1,
    -1, 4, 5, 48, 3, 1, 1, -1, 48, 14, 6, 3, 85, 29, 15, 1, 1, -1, 4, 4, 3, 2, 1, 6, 48, 13, 6, 9, 42, -122, 72, -122, -9,
    13, 1, 1, 13, 5, 0, 3, -126, 1, -127, 0, 42, -128, -7, -36, -35, -1, -54, -87, 47, -61, -18, 77, 93, 80, -116, -62, 110,
    2, 97, -79, -54, -29, 68, 0, 87, 25, -77, -4, -81, 78, 65, -14, 30, 127, 28, -2, -36, 82, -97, 38, -119, -68, -25, 100,
    -41, -102, -11, 101, -60, -26, 0, 58, 35, -50, 11, 98, -59, 23, 56, -89, 99, 26, 91, -80, -122, -83, -64, -99, 84, 9,
    -94, 37, -63, 114, 126, -123, -110, -60, 111, -102, -36, -114, -58, 82, -106, -73, -56, 86, -67, 105, -122, -67, -41,
    32, -47, -75, -30, -44, -114, -22, -121, -95, 78, -76, 70, -66, 32, 78, 37, 101, 5, 121, -120, 124, -90, -13, 84, -62,
    -73, 84, -84, 28, -54, 113, 98, 29, 62, 14, -61, 80, 13, 107, 85, 65, -114, -36, -103, 50, 114, 52, 56, 125, -1, 97,
    38, 106, -6, -73, -102, -60, 109, -115, -86, -37, 107, -67, -36, 80, 45, -53, -124, -85, -21, -101, 13, 67, -70, -38,
    -78, 54, -53, -128, -93, -98, 52, 34, 23, -74, -95, -49, 115, 45, 96, 21, 85, -31, 106, -128, -28, 15, -67, 79, 52,
    -89, -111, -93, -41, -3, 6, -118, -64, -73, -16, 106, 100, -86, -61, 41, 25, -48, 113, -69, -63, 80, 77, 28, 12, 2,
    120, 26, -7, 51, 69, 34, -101, 25, -68, -25, -43, -8, 35, -26, -38, -101, 26, -120, -38, 86, -82, 89, -75, 31, 84,
    101, 27, -95, 86, -114, 30, -12, -120, -16, 49, 67, 32, -1, 111, 87, 101, 119, 127, 43, -9, -39, 68, 127, -74, 34,
    54, -25, 58, -91, -69, -26, -7, -31, 73, 127, -105, -34, 8, 19, 52, 127, 3, -55, -115, 56, 100, 29, 94, -32, 80,
    108, 47, 84, -103, 12, -118, 99, 29, -67, 88, -95, -126, -66, -16, 35, 62, -59, -10, -95, -50, 111, -76, 112, 84,
    77, -21, -100, 98, -95, 13, -56, -86, -60, -28, -94, -4, 93, 25, 19, -89, -38, 126, 80, -24, 20, -109, -19, 95,
    -42, -48, 23, -7, -36, 105, -33, 60, 3, -24, -62, 76, 89, 9, 7, -64, -123, 123, 22, 26, 96, -24, -40, 117, 118,
    126, 60, -118, -91, -61, 20, 105, -72, -3, 113, -35, 66, -36, 117, -45, -120, 31, -85
  };
  /** the root certificate */
  private static final X509Certificate ROOT_X509_CERTIFICATE;
  /** the truststore entry alias */
  public static final String TRUSTSTORE_ENTRY_ALIAS = "texai root certificate";
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
      assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    } catch (NoSuchAlgorithmException ex) {
      throw new TexaiException(ex);
    }
  }

  static {
    LOGGER.info("adding Bouncy Castle cryptography provider");
    Security.addProvider(new BouncyCastleProvider());

    LOGGER.info("initializing the root X.509 certificate");

    try {
      ROOT_X509_CERTIFICATE = new X509CertImpl(ROOT_CERTIFICATE_BYTES);
    } catch (CertificateException ex) {
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

  /** Makes a canonical X.509 certificate by serializing it to bytes and reconsituting it. This ensures
   * that all issuer and subject names have no space following the commas.

   * @param x509Certificate the input certificate
   * @return the canonical certificate
   */
  public static X509Certificate makeCanonicalX509Certificate(final X509Certificate x509Certificate) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";

    X509Certificate canonicalX509Certificate;
    try {
      final byte[] certificateBytes = x509Certificate.getEncoded();
      final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(certificateBytes);
      canonicalX509Certificate = readX509Certificate(byteArrayInputStream);
    } catch (CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }

    LOGGER.debug("x509Certificate (" + x509Certificate.getClass().getName() + ")...\n" + x509Certificate
            + "\ncanonicalX509Certificate(" + canonicalX509Certificate.getClass().getName() + ")...\n" + canonicalX509Certificate);

    //Postconditions
    assert canonicalX509Certificate.equals(x509Certificate) :
            "canonicalX509Certificate must equal x509Certificate,\ncanonicalX509Certificate...\n" + canonicalX509Certificate
            + "\nx509Certificate...\n" + x509Certificate;

    return canonicalX509Certificate;
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
    LOGGER.debug("isJCEUnlimitedStrenthPolicy: " + isJCEUnlimitedStrenthPolicy);
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
    //Preconditions
    assert (new File("data/truststore.uber")).exists() || (new File("data/truststore.jceks")).exists() :
            "truststore file must exist";
    assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

    String filePath = "";
    if (truststore == null) {

      if (X509Utils.isJCEUnlimitedStrengthPolicy()) {
        filePath = "data/truststore.uber";
      } else {
        filePath = "data/truststore.jceks";
      }
      LOGGER.info("reading truststore from " + filePath);
      try {
        truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
        throw new TexaiException(ex);
      }
    }

    //Postconditions
    assert truststore != null : "truststore must not be null";
    assert X509Utils.BOUNCY_CASTLE_PROVIDER.equals(truststore.getProvider().getName()) : "truststore type must be " + BOUNCY_CASTLE_PROVIDER + ", but was " + truststore.getProvider().getName() + ", filePath: " + filePath;
    assert !filePath.endsWith(".uber") || truststore.getType().equals("UBER") : "truststore type must be UBER, but was " + truststore.getType() + ", filePath: " + filePath;

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

    //final X500Name issuer = new X500Name(issuerCertificate.getSubjectX500Principal().getName());
    final X500Name issuer = new X500Name(StringUtils.reverseCommaDelimitedString(issuerCertificate.getSubjectX500Principal().getName()));
    final UUID intermediateUUID = UUID.randomUUID();
    // provide items to X500Principal in reverse order
    final X500Principal x500Principal = new X500Principal("UID=" + intermediateUUID + ", DC=IntermediateCertificate, CN=texai.org");
    final X500Name subject = new X500Name(x500Principal.getName());
    SubjectPublicKeyInfo publicKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(myPublicKey.getEncoded()));
    final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
            issuer,
            getNextSerialNumber(), // serial
            new Date(System.currentTimeMillis() - 10000L), // notBefore,
            new Date(System.currentTimeMillis() + VALIDITY_PERIOD), // notAfter,
            subject,
            publicKeyInfo);

    // see http://www.ietf.org/rfc/rfc3280.txt
    // see http://stackoverflow.com/questions/20175447/creating-certificates-for-ssl-communication
    final JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();

    // Add authority key identifier
    x509v3CertificateBuilder.addExtension(
            Extension.authorityKeyIdentifier,
            false, // isCritical
            jcaX509ExtensionUtils.createAuthorityKeyIdentifier(issuerCertificate));

    // Add subject key identifier
    x509v3CertificateBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false, // isCritical
            jcaX509ExtensionUtils.createSubjectKeyIdentifier(myPublicKey));

    // add basic constraints
    x509v3CertificateBuilder.addExtension(
            Extension.basicConstraints,
            true, // isCritical
            new BasicConstraints(pathLengthConstraint)); // is a CA certificate with specified certification path length

    // add key usage
    final KeyUsage keyUsage = new KeyUsage(
            // the keyCertSign bit indicates that the subject public key may be used for verifying a signature on
            // certificates
            KeyUsage.keyCertSign
            | // the cRLSign indicates that the subject public key may be used for verifying a signature on revocation
            // information
            KeyUsage.cRLSign);

    x509v3CertificateBuilder.addExtension(
            Extension.keyUsage,
            true, // isCritical
            keyUsage);

    X509Certificate x509Certificate;
    try {
      final ContentSigner contentSigner = new JcaContentSignerBuilder(DIGITAL_SIGNATURE_ALGORITHM).setProvider(BOUNCY_CASTLE_PROVIDER).build(issuerPrivateKey);
      final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
      final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
      x509Certificate = makeCanonicalX509Certificate(jcaX509CertificateConverter.getCertificate(x509CertificateHolder));
    } catch (CertificateException | OperatorCreationException ex) {
      throw new TexaiException(ex);
    }

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

    final String x500PrincipalString;
    // provide items to X500Principal in reverse order
    if (domainComponent == null || domainComponent.isEmpty()) {
      x500PrincipalString = "UID=" + uid + ", CN=texai.org";
    } else {
      x500PrincipalString = "UID=" + uid + ", DC=" + domainComponent + " ,CN=texai.org";
    }
    final X500Principal x500Principal = new X500Principal(x500PrincipalString);

    LOGGER.info("issuer: " + issuerCertificate.getIssuerX500Principal().getName());

    final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
            new X500Name(StringUtils.reverseCommaDelimitedString(issuerCertificate.getSubjectX500Principal().getName())), // issuer,
            getNextSerialNumber(), // serial
            new Date(System.currentTimeMillis() - 10000L), // notBefore,
            new Date(System.currentTimeMillis() + VALIDITY_PERIOD), // notAfter,
            new X500Name(x500Principal.getName()), // subject,
            new SubjectPublicKeyInfo(ASN1Sequence.getInstance(myPublicKey.getEncoded()))); // publicKeyInfo

    // see http://www.ietf.org/rfc/rfc3280.txt
    // see http://stackoverflow.com/questions/20175447/creating-certificates-for-ssl-communication
    final JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();
    // Add authority key identifier
    x509v3CertificateBuilder.addExtension(
            Extension.authorityKeyIdentifier,
            false, // isCritical
            jcaX509ExtensionUtils.createAuthorityKeyIdentifier(issuerCertificate));

    // Add subject key identifier
    x509v3CertificateBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false, // isCritical
            jcaX509ExtensionUtils.createSubjectKeyIdentifier(myPublicKey));

    // add basic constraints
    x509v3CertificateBuilder.addExtension(
            Extension.basicConstraints,
            true, // isCritical
            new BasicConstraints(false)); // is not a CA certificate

    // add key usage
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
    x509v3CertificateBuilder.addExtension(
            Extension.keyUsage,
            true, // isCritical
            keyUsage);

    X509Certificate x509Certificate;
    try {
      final ContentSigner contentSigner = new JcaContentSignerBuilder(DIGITAL_SIGNATURE_ALGORITHM).setProvider(BOUNCY_CASTLE_PROVIDER).build(issuerPrivateKey);
      final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
      final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
      x509Certificate = makeCanonicalX509Certificate(jcaX509CertificateConverter.getCertificate(x509CertificateHolder));
    } catch (CertificateException | OperatorCreationException ex) {
      throw new TexaiException(ex);
    }

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
    final String uuidString = subjectString.substring(index + 4, index + 40);
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

    try {
      return new X509CertImpl(IOUtils.toByteArray(inputStream));
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
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

    //Postconditions
    assert !filePath.endsWith(".uber") || keyStore.getType().equals("UBER") :
            "keyStore type is " + keyStore.getType() + ", expected UBER, filePath: " + filePath;

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

    //Postconditions
    assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
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

    final UUID rootUUID = UUID.randomUUID();
    // provide items to X500Principal in reverse order
    final X500Principal rootX500Principal
            = new X500Principal("UID=" + rootUUID + ", O=Texai Certification Authority, CN=texai.org");
    final X500Name subject = new X500Name(rootX500Principal.getName());
    final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
            new X500Name(rootX500Principal.getName()), // issuer,
            getNextSerialNumber(), // serial
            new Date(System.currentTimeMillis() - 10000L), // notBefore,
            new Date(System.currentTimeMillis() + VALIDITY_PERIOD), // notAfter,
            subject,
            new SubjectPublicKeyInfo(ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()))); // publicKeyInfo

    // see http://www.ietf.org/rfc/rfc3280.txt
    // see http://stackoverflow.com/questions/20175447/creating-certificates-for-ssl-communication
    final JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();

    // Add subject key identifier
    x509v3CertificateBuilder.addExtension(
            Extension.subjectKeyIdentifier,
            false, // isCritical
            jcaX509ExtensionUtils.createSubjectKeyIdentifier(keyPair.getPublic()));

    // add basic constraints
    x509v3CertificateBuilder.addExtension(
            Extension.basicConstraints,
            true, // isCritical
            new BasicConstraints(true)); // is a CA certificate with an unlimited certification path length

    final KeyUsage keyUsage = new KeyUsage(
            // the keyCertSign bit indicates that the subject public key may be used for verifying a signature on
            // certificates
            KeyUsage.keyCertSign
            | // the cRLSign indicates that the subject public key may be used for verifying a signature on revocation
            // information
            KeyUsage.cRLSign);

    // add key usage
    x509v3CertificateBuilder.addExtension(
            Extension.keyUsage,
            true, // isCritical
            keyUsage);

    X509Certificate rootX509Certificate;
    try {
      final ContentSigner contentSigner = new JcaContentSignerBuilder(DIGITAL_SIGNATURE_ALGORITHM).setProvider(BOUNCY_CASTLE_PROVIDER).build(keyPair.getPrivate());
      final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
      final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
      rootX509Certificate = jcaX509CertificateConverter.getCertificate(x509CertificateHolder);
    } catch (CertificateException | OperatorCreationException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    try {
      rootX509Certificate.checkValidity();
      rootX509Certificate.verify(keyPair.getPublic());

      return rootX509Certificate;
    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchProviderException | SignatureException | CertificateException ex) {
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
      LOGGER.info("root certificate...\n" + rootX509Certificate);
      final StringBuilder stringBuilder = new StringBuilder();
      for (final byte b : rootX509Certificate.getEncoded()) {
        stringBuilder.append(Byte.toString(b));
        stringBuilder.append(", ");
      }
      LOGGER.info("root certificate...\n" + rootX509Certificate);
      LOGGER.info("\nroot certificate bytes...\n" + stringBuilder.toString());
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
    } catch (IOException ex) {
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
      assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";

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

    try {
      LOGGER.info("creating truststore.uber");
      (new File(filePath)).delete();
      truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      final FileOutputStream fileOutputStream = new FileOutputStream(filePath);
      truststore.store(fileOutputStream, TRUSTSTORE_PASSWORD);
      assert "UBER".equals(truststore.getType());
      Enumeration<String> aliases = truststore.aliases();
      int aliasCnt = 0;
      while (aliases.hasMoreElements()) {
        aliasCnt++;
        aliases.nextElement();
      }
      assert aliasCnt > 0;

      // then proceed after disabling the JCE unlimited strength jurisdiction policy files indicator
      setIsJCEUnlimitedStrengthPolicy(false);
      filePath = "data/truststore.jceks";
      (new File(filePath)).delete();
      LOGGER.info("creating truststore.jceks");
      truststore = X509Utils.findOrCreateKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);
      assert "JCEKS".equals(truststore.getType());
      aliases = truststore.aliases();
      aliasCnt = 0;
      while (aliases.hasMoreElements()) {
        aliasCnt++;
        aliases.nextElement();
      }
      assert aliasCnt > 0;

      filePath = "data/truststore.jks";
      (new File(filePath)).delete();
      LOGGER.info("creating truststore.jks");
      truststore = X509Utils.findOrCreateJKSKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);
      assert "JKS".equals(truststore.getType());
      aliases = truststore.aliases();
      aliasCnt = 0;
      while (aliases.hasMoreElements()) {
        aliasCnt++;
        aliases.nextElement();
      }
      assert aliasCnt > 0;

      filePath = "data/truststore.bks";
      (new File(filePath)).delete();
      LOGGER.info("creating truststore.bks");
      truststore = X509Utils.findOrCreateBKSKeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);
      assert "BKS".equals(truststore.getType());
      aliases = truststore.aliases();
      aliasCnt = 0;
      while (aliases.hasMoreElements()) {
        aliasCnt++;
        aliases.nextElement();
      }
      assert aliasCnt > 0;

      // create the PKCS12 keystore from which the trusted certificate can be imported into a web browser
      filePath = "data/truststore.p12";
      (new File(filePath)).delete();
      LOGGER.info("creating truststore.p12");
      truststore = X509Utils.findOrCreatePKCS12KeyStore(filePath, TRUSTSTORE_PASSWORD);
      truststore.setCertificateEntry(
              TRUSTSTORE_ENTRY_ALIAS,
              getRootX509Certificate());
      truststore.store(new FileOutputStream(filePath), TRUSTSTORE_PASSWORD);
      assert truststore != null;
      assert "pkcs12".equals(truststore.getType());
      aliases = truststore.aliases();
      aliasCnt = 0;
      while (aliases.hasMoreElements()) {
        aliasCnt++;
        aliases.nextElement();
      }
      assert aliasCnt > 0;

    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    } finally {
      // restore the JCE unlimited strength jurisdiction policy files indicator
      setIsJCEUnlimitedStrengthPolicy(true);
      // refresh the cached reference to the truststore
      truststore = null;
      getTruststore();

      //Postconditions
      assert !isTrustedDevelopmentSystem() || X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    }
  }

  /** Logs the aliases contained in the given keystore.

  @param keyStore the given keystore
  */
  public static void logAliases(final KeyStore keyStore) {
    Enumeration<String> aliases;
    try {
      aliases = keyStore.aliases();
    } catch (KeyStoreException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("aliases...");
    while (aliases.hasMoreElements()) {
      LOGGER.info("  " + aliases.nextElement());
    }
  }
}
