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
import java.io.UTFDataFormatException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.security.auth.x500.X500Principal;
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
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * X509 utilities adapted from "Beginning Cryptography With Java", David Hook, WROX.
 *
 *
 * @author reed
 */
public final class X509Utils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(X509Utils.class);
  // the default secure random serialization path
  public static final String DEFAULT_SECURE_RANDOM_PATH = "data/secure-random.ser";
  // the root certificate alias
  public static final String JAR_SIGNER_ALIAS = "jar-signer";
  // the period in which the certificate is valid
  private static final long VALIDITY_PERIOD = 10L * 365L * 24L * 60L * 60L * 1000L; // ten years
  // the Bouncy Castle cryptography provider
  public static final String BOUNCY_CASTLE_PROVIDER = "BC";
  // the digital signature algorithm
  public static final String DIGITAL_SIGNATURE_ALGORITHM = "SHA512withRSA";
  // the indicator whether the JCE unlimited strength jurisdiction policy files are installed
  private static boolean isJCEUnlimitedStrenthPolicy;

  static {
    try {
      setIsJCEUnlimitedStrengthPolicy(Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE);
      assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy must be in effect";
    } catch (NoSuchAlgorithmException ex) {
      throw new TexaiException(ex);
    }
  }

  static {
    addBouncyCastleSecurityProvider();
  }

  // the secure random
  private static SecureRandom secureRandom;
  // the secure random synchronization lock
  private static final Object secureRandom_lock = new Object();

  static {
    X509Utils.initializeSecureRandom(DEFAULT_SECURE_RANDOM_PATH);
  }

  /**
   * Prevents the instantiation of this utility class.
   */
  private X509Utils() {
  }

  /**
   * Adds the Bouncy Castle security provider library.
   */
  public static void addBouncyCastleSecurityProvider() {
    LOGGER.info("adding Bouncy Castle cryptography provider");
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * Makes a canonical X.509 certificate by serializing it to bytes and reconsituting it. This ensures that all issuer and subject names
   * have no space following the commas.
   *
   * @param x509Certificate the input certificate
   *
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

  /**
   * Gets the secure random, and lazily initializes it.
   *
   * @return the initialized secure random
   */
  public static SecureRandom getSecureRandom() {
    synchronized (secureRandom_lock) {
      if (secureRandom == null) {
        LOGGER.info("creating and seeding secure random");
        try {
          // on Linux, use /dev/urandom
          secureRandom = SecureRandom.getInstance("NativePRNG");
          // force it to seed itself
          secureRandom.nextInt();
        } catch (NoSuchAlgorithmException ex) {
          throw new TexaiException(ex);
        }
        secureRandom.nextInt();
      }
      //Postconditions
      assert secureRandom != null;

      return secureRandom;
    }
  }

  /**
   * Initializes the secure random from a serialized object.
   *
   * @param path the path to the previously serialized secure random
   *
   * @return the initialized secure random
   */
  public static SecureRandom initializeSecureRandom(final String path) {
    //Preconditions
    assert path != null : "path must not be null";
    assert !path.isEmpty() : "path must not be empty";

    final File file = new File(path);
    LOGGER.info("loading secure random from " + path);
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
      file.getParentFile().mkdirs();
      serializeSecureRandom(path);
    }
    return secureRandom;
  }

  /**
   * Serializes the secure random to a file for a subsequent restart.
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
      LOGGER.error("cannot write serialized secure random to " + path);
      LOGGER.error("working directory: " + System.getProperty("user.dir"));
      throw new TexaiException(ex);
    }
  }

  /**
   * Gets the indicator whether the JCE unlimited strength jurisdiction policy files are installed.
   *
   * @return the indicator whether the JCE unlimited strength jurisdiction policy files are installed
   */
  public static synchronized boolean isJCEUnlimitedStrengthPolicy() {
    return isJCEUnlimitedStrenthPolicy;
  }

  /**
   * Sets the indicator whether the JCE unlimited strength jurisdiction policy files are installed.
   *
   * @param _isJCEUnlimitedStrenthPolicy the indicator whether the JCE unlimited strength jurisdiction policy files are installed
   */
  public static synchronized void setIsJCEUnlimitedStrengthPolicy(boolean _isJCEUnlimitedStrenthPolicy) {
    isJCEUnlimitedStrenthPolicy = _isJCEUnlimitedStrenthPolicy;
    LOGGER.debug("isJCEUnlimitedStrenthPolicy: " + isJCEUnlimitedStrenthPolicy);
  }

  /**
   * Returns the maximum key length allowed by the ciphers on this JVM, which depends on whether the unlimited strength encryption policy
   * jar files have been downloaded and installed.
   *
   * @return the maximum allowed key size
   * @throws NoSuchAlgorithmException when the encryption algorithm cannot be found
   */
  public static int getMaxAllowedKeyLength() throws NoSuchAlgorithmException {
    return Cipher.getMaxAllowedKeyLength("AES");
  }

  /**
   * Logs the cryptography providers.
   */
  public static void logProviders() {
    LOGGER.info("cryptography providers ...");
    final Provider[] providers = Security.getProviders();
    for (int i = 0; i != providers.length; i++) {
      LOGGER.info("  Name: " + providers[i].getName() + StringUtils.makeBlankString(15 - providers[i].getName().length()) + " Version: " + providers[i].getVersion());
    }
  }

  /**
   * Logs the capabilities of the cryptography providers.
   *
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
    propertyStrings.stream().forEach((propertyString) -> {
      final String factoryClass = propertyString.substring(0, propertyString.indexOf('.'));
      final String name = propertyString.substring(factoryClass.length() + 1);
      LOGGER.info("  " + factoryClass + ": " + name);
    });
  }

  /**
   * Creates a random 3072 bit RSA key pair.
   *
   * @return a random 3072 bit RSA key pair
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws NoSuchProviderException when an invalid provider is given
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

  /**
   * Generates a self-signed end-entity certificate, which has no chain of trust from a root certificate, and can be used for authentication
   * and for message signing.
   *
   * @param keyPair the root public/private key pair
   * @param uid the subject UID, or null if not used
   * @param domainComponent the domain component, e.g. container-name.agent-name.role-name
   *
   * @return a self-signed end-use certificate
   *
   * @throws CertificateParsingException when the certificate cannot be parsed
   * @throws CertificateEncodingException when the certificate cannot be encoded
   * @throws NoSuchProviderException when an invalid provider is given
   * @throws NoSuchAlgorithmException when an invalid algorithm is given
   * @throws SignatureException when the an invalid signature is present
   * @throws InvalidKeyException when the given key is invalid
   * @throws IOException if an input/output error occurs while processing the serial number file
   */
  public static X509Certificate generateSelfSignedEndEntityX509Certificate(
          final KeyPair keyPair,
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
    assert keyPair != null : "keyPair must not be null";
    assert uid != null || StringUtils.isNonEmptyString(domainComponent) : "Either uid or domainComponent must be present";

    final StringBuilder stringBuilder = new StringBuilder();
    if (uid != null) {
      stringBuilder.append("UID=").append(uid).append(", ");
    }
    if (StringUtils.isNonEmptyString(domainComponent)) {
      stringBuilder.append("DC=").append(domainComponent).append(", ");
    }
    stringBuilder.append("CN=texai.org");
    final X500Principal x500Principal = new X500Principal(stringBuilder.toString());

    final X509v3CertificateBuilder x509v3CertificateBuilder = new X509v3CertificateBuilder(
            new X500Name(x500Principal.getName()), // issuer,
            getNextSerialNumber(), // serial
            new Date(System.currentTimeMillis() - 10000L), // notBefore,
            new Date(System.currentTimeMillis() + VALIDITY_PERIOD), // notAfter,
            new X500Name(x500Principal.getName()), // subject,
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
      // self signed
      final ContentSigner contentSigner = new JcaContentSignerBuilder(DIGITAL_SIGNATURE_ALGORITHM).setProvider(BOUNCY_CASTLE_PROVIDER).build(keyPair.getPrivate());
      final X509CertificateHolder x509CertificateHolder = x509v3CertificateBuilder.build(contentSigner);
      final JcaX509CertificateConverter jcaX509CertificateConverter = new JcaX509CertificateConverter();
      x509Certificate = makeCanonicalX509Certificate(jcaX509CertificateConverter.getCertificate(x509CertificateHolder));
    } catch (CertificateException | OperatorCreationException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    try {
      x509Certificate.checkValidity();
      // verify self-signature
      x509Certificate.verify(keyPair.getPublic());
    } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
      throw new TexaiException(ex);
    }
    assert x509Certificate.getKeyUsage()[0] : "must have digital signature key usage";

    return x509Certificate;
  }

  /**
   * Returns whether the given certificate has the given key usage bit set, i.e. digitalSignature usage.
   *
   * digitalSignature (0) nonRepudiation (1) keyEncipherment (2) dataEncipherment (3) keyAgreement (4) keyCertSign (5) cRLSign (6)
   * encipherOnly (7) decipherOnly (8)
   *
   * @param x509Certificate the given certificate
   * @param keyUsageBitMask the given key usage bit, i.e. KeyUsage.digitalSignature
   *
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

  /**
   * Gets the UUID from the subject name contained in the given X.509 certificate.
   *
   * @param x509Certificate the given X.509 certificate
   *
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

  /**
   * Reads a DER encoded certificate from an input stream.
   *
   * @param inputStream the input stream containing the DER encoded bytes of an X.509 certificate
   *
   * @return the certificate
   * @throws CertificateException when an invalid certificate is read
   * @throws NoSuchProviderException when the cryptography provider cannot be found
   */
  public static X509Certificate readX509Certificate(final InputStream inputStream) throws CertificateException, NoSuchProviderException {
    //Preconditions
    assert inputStream != null : "inputStream must not be null";

    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    return (X509Certificate) certificateFactory.generateCertificate(inputStream);

  }

  /**
   * Writes a DER encoded certificate to the given file path.
   *
   * @param x509Certificate the X.509 certificate
   * @param filePath the given file path
   *
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

  /**
   * Finds or creates the keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds or creates the jceks keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds or creates the uber keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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
      } catch (UTFDataFormatException ex) {
        LOGGER.error("filePath: " + filePath);
      }
    } else {
      keyStore.load(null, null);
      try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(keyStoreFile)) {
        keyStore.store(keyStoreOutputStream, password);
      }
    }
    return keyStore;
  }

  /**
   * Copies the given keystore from the .uber format to the .jceks format.
   *
   * @param uberKeyStorePath the .uber keystore path
   * @param uberKeyStorePassword the .uber keystore password
   * @param jceksKeyStorePath the .jceks keystore path
   * @param jceksKeyStorePassword the .jceks keystore password
   *
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds the keystore specified by the given path.
   *
   * @param filePath the file path to the keystore
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds or creates the PKCS12 keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .pkcs12 extension
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds or creates the BKS keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .bks extension
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Finds or creates the JKS keystore specified by the given path.
   *
   * @param filePath the file path to the keystore, having the .jks extension
   * @param password the keystore password
   *
   * @return the keystore
   * @throws KeyStoreException if no Provider supports a KeyStoreSpi implementation for the specified type
   * @throws IOException if there is an I/O or format problem with the keystore data, if a password is required but not given, or if the
   * given password was incorrect
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

  /**
   * Adds an entry to the specified keystore, creating the keystore if it does not already exist.
   *
   * @param keyStoreFilePath the file path to the keystore
   * @param keyStorePassword the keystore's password
   * @param certificateAlias the certificate entry alias
   * @param certPath the certificate path to add
   * @param privateKey the private key associated with the first certificate in the path
   *
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
          final String certificateAlias,
          final CertPath certPath,
          final PrivateKey privateKey)
          throws
          KeyStoreException,
          IOException,
          NoSuchAlgorithmException,
          CertificateException,
          NoSuchProviderException {
    //Preconditions
    assert StringUtils.isNonEmptyString(keyStoreFilePath) : "keyStoreFilePath must be a non-empty string";
    assert keyStorePassword != null : "keyStorePassword must not be null";
    assert StringUtils.isNonEmptyString(certificateAlias) : "certificateAlias must be a non-empty string";
    assert !certificateAlias.isEmpty() : "alias must not be empty";
    assert privateKey != null : "privateKey must not be null";
    assert certPath.getCertificates().size() == 1 : "certPath length must be 1, was " + certPath.getCertificates().size();

    final KeyStore keyStore = X509Utils.findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
    final Certificate[] certificateChain = new Certificate[1];
    certificateChain[0] = certPath.getCertificates().get(0);
    keyStore.setKeyEntry(
            certificateAlias,
            privateKey,
            keyStorePassword,
            certificateChain);
    keyStore.store(new FileOutputStream(keyStoreFilePath), keyStorePassword);

    //Postconditions
    assert keyStore != null : "keyStore must not be null";

    return keyStore;
  }

  /**
   * Resets the certificate serial number.
   *
   * @throws IOException if an input/output error occurred
   */
  protected static void resetSerialNumber() throws IOException {
    File serialNumberFile = new File("../X509Security/data/certificate-serial-nbr.txt");
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serialNumberFile))) {
      bufferedWriter.write("0");
    }
  }

  /**
   * Returns the next certificate serial number.
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

  /**
   * Returns a certificate path consisting of the given certificate array.
   *
   * @param certificates the given certificate array
   *
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

  /**
   * Returns a certificate path consisting of the given certificate list.
   *
   * @param certificateList the given certificate list
   *
   * @return the certificate path consisting of the given certificate list
   * @throws CertificateException if an invalid certificate is present
   * @throws NoSuchProviderException if the cryptography service provider is not found
   */
  public static CertPath generateCertPath(final List<Certificate> certificateList) throws CertificateException, NoSuchProviderException {
    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", X509Utils.BOUNCY_CASTLE_PROVIDER);
    return certificateFactory.generateCertPath(certificateList);
  }

  /**
   * Generates the X.509 security information.
   *
   * @param keyStore the keystore to which the generated certificate will be added
   * @param keystorePassword the keystore password
   * @param keyPair the key pair for the generated certificate
   * @param uid the generated certificate's subject UID, or null if not used
   * @param domainComponent the domain component, or null if not used
   * @param certificateAlias the X.509 certificate alias that indentifies the entry within the keystore
   *
   * @return the X509 security information
   */
  public static X509SecurityInfo generateX509SecurityInfo(
          final KeyStore keyStore,
          final char[] keystorePassword,
          final KeyPair keyPair,
          final UUID uid,
          final String domainComponent,
          final String certificateAlias) {
    //Preconditions
    assert keyPair != null : "keyPair must not be null";
    assert keystorePassword != null : "keystorePassword must not be null";
    assert StringUtils.isNonEmptyString(certificateAlias) : "certificateAlias must be a non-empty string";

    final X509SecurityInfo x509SecurityInfo;
    try {
      final X509Certificate x509Certificate = generateSelfSignedEndEntityX509Certificate(
              keyPair,
              uid,
              domainComponent);
      assert X509Utils.isJCEUnlimitedStrengthPolicy();

      keyStore.setKeyEntry(
              certificateAlias,
              keyPair.getPrivate(),
              keystorePassword,
              new Certificate[]{x509Certificate});

      x509SecurityInfo = new X509SecurityInfo(
              keyStore,
              keystorePassword,
              certificateAlias); // alias

      //Postconditions
      assert keyStore.containsAlias(certificateAlias);
      assert keyStore.isKeyEntry(certificateAlias);

      return x509SecurityInfo;

    } catch (NoSuchProviderException | NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException | KeyStoreException | CertificateException ex) {
      throw new TexaiException(ex);
    }

  }

  /**
   * Gets the X509 security information for the given keystore and private key alias.
   *
   * @param keyStore the keystore
   * @param keyStorePassword the keystore password
   * @param alias the private key entry alias
   *
   * @return the X509 security information, which includes the keystore, certificate and private key
   */
  public static X509SecurityInfo getX509SecurityInfo(
          final KeyStore keyStore,
          final char[] keyStorePassword,
          final String alias) {
    //Preconditions
    assert keyStore != null : "keyStore must not be null";
    assert keyStorePassword != null : "keyStorePassword must not be null";

    final X509SecurityInfo x509SecurityInfo = new X509SecurityInfo(
            keyStore,
            keyStorePassword,
            alias);

    //Postconditions
    assert x509SecurityInfo.getKeyStore().equals(keyStore);
    assert x509SecurityInfo.getPrivateKey() != null;
    assert x509SecurityInfo.getCertificateChain().length == 1;
    assert x509SecurityInfo.getX509Certificate() != null;

    return x509SecurityInfo;
  }

  /**
   * Returns whether the given keystore contains a private key entry for the given alias.
   *
   * @param keyStoreFilePath the file path to the keystore
   * @param keyStorePassword the keystore password
   * @param certificateAlias the private key entry alias
   *
   * @return the X509 security information for a test client
   */
  public static boolean keyStoreContains(
          final String keyStoreFilePath,
          final char[] keyStorePassword,
          final String certificateAlias) {
    //Preconditions
    assert keyStoreFilePath != null : "keyStoreFilePath must not be null";
    assert !keyStoreFilePath.isEmpty() : "keyStoreFilePath must not be empty";
    assert keyStoreFilePath.endsWith(".uber") || keyStoreFilePath.endsWith(".jceks") : "keystore file extension must be .uber or .jceks";
    assert keyStorePassword != null : "keyStorePassword must not be null";
    assert StringUtils.isNonEmptyString(certificateAlias) : "certificateAlias must not be empty";

    try {
      final KeyStore keyStore = findOrCreateKeyStore(keyStoreFilePath, keyStorePassword);
      return keyStore.containsAlias(certificateAlias);
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Logs the aliases contained in the given keystore.
   *
   * @param keyStore the given keystore
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

  /**
   * Logs the aliases contained in the given keystore to the given logger.
   *
   * @param keyStore the given keystore
   * @param logger the given logger
   */
  public static void logAliases(
          final KeyStore keyStore,
          final Logger logger) {
    Enumeration<String> aliases;
    try {
      aliases = keyStore.aliases();
    } catch (KeyStoreException ex) {
      throw new TexaiException(ex);
    }
    logger.info("aliases...");
    while (aliases.hasMoreElements()) {
      logger.info("  " + aliases.nextElement());
    }
  }

  /**
   * Returns the SHA-512 hash of the given file, encoded as a base 64 string.
   *
   * @param filePath the file path
   *
   * @return the SHA-512 hash of the given file
   */
  public static String fileHashString(final String filePath) {
    //Preconditions
    assert StringUtils.isNonEmptyString(filePath) : "nodesPath must not be empty";

    return fileHashString(new File(filePath));
  }

  /**
   * Returns the SHA-512 hash of the given file, encoded as a base 64 string.
   *
   * @param file the given file
   *
   * @return the SHA-512 hash of the given file
   */
  public static String fileHashString(final File file) {
    //Preconditions
    assert file != null : "file must not be null";

    final byte[] hashBytes;
    try {
      addBouncyCastleSecurityProvider();
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", BOUNCY_CASTLE_PROVIDER);
      final FileInputStream fileInputStream = new FileInputStream(file);
      final byte[] dataBytes = new byte[1024];
      int nread;
      while ((nread = fileInputStream.read(dataBytes)) != -1) {
        messageDigest.update(dataBytes, 0, nread);
      }
      hashBytes = messageDigest.digest();
      return new String(Base64Coder.encode(hashBytes));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Verifies the expected SHA-512 hash of the given file.
   *
   * @param filePath the file path
   * @param fileHashString the file SHA-512 hash encoded as a base 64 string, used to detect tampering
   */
  public static void verifyFileHash(
          final String filePath,
          final String fileHashString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(filePath) : "nodesPath must not be empty";
    assert StringUtils.isNonEmptyString(fileHashString) : "nodesFileHashString must not be empty";

    final String actualFileHashString = fileHashString(filePath);
    if (!actualFileHashString.equals(fileHashString)) {
      LOGGER.warn("actual encoded hash bytes:\n" + actualFileHashString);
      throw new TexaiException("file: " + filePath + " fails expected hash checksum");
    }
  }

}
