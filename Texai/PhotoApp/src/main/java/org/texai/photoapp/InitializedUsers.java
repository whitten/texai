package org.texai.photoapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * InitializedUsers.java
 *
 * Description:
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class InitializedUsers {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(InitializedUsers.class);
  // the photo bytes
  private byte[] photoBytes;
  // the photo encoded in base 64 notation
  private String photoBase64;
  // Alice's public key
  private PublicKey alicePublicKey;
  // Alice's public key encoded in base 64 notation
  private String alicePublicKeyBase64;
  // Alice's private key
  private PrivateKey alicePrivateKey;
  // Alice's private key encoded in base 64 notation
  private String alicePrivateKeyBase64;
  // the X.509 certificate for Alice
  private X509Certificate aliceX509Certificate;
  // Bob's public key
  private PublicKey bobPublicKey;
  // Bob's public key encoded in base 64 notation
  private String bobPublicKeyBase64;
  // Bob's private key
  private PrivateKey bobPrivateKey;
  // Bob's private key encoded in base 64 notation
  private String bobPrivateKeyBase64;
  // the X.509 certificate for Bob
  private X509Certificate bobX509Certificate;
  // Server's public key
  private PublicKey serverPublicKey;
  // Server's public key encoded in base 64 notation
  private String serverPublicKeyBase64;
  // Server's private key
  private PrivateKey serverPrivateKey;

  /**
   * Creates a new instance of InitializedUsers.
   */
  public InitializedUsers() {
    initialize();
  }

  /**
   * Initializes the test provisioning data, for example the X.509 certificates and photo.
   */
  private void initialize() {
    final String photoPath;
    try {
      photoPath = "data/orb.jpg";
      final InputStream inputStream = new FileInputStream(photoPath);
      photoBytes = IOUtils.toByteArray(inputStream);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("photo " + photoPath + " length: " + photoBytes.length + " bytes");
    photoBase64 = new String(Base64Coder.encode(photoBytes));
    LOGGER.info("photoBase64 " + photoBase64);

    try {
      final KeyPair aliceKeyPair = X509Utils.generateRSAKeyPair1024();
      alicePublicKey = aliceKeyPair.getPublic();
      alicePublicKeyBase64 = new String(Base64Coder.encode(alicePublicKey.getEncoded()));
      alicePrivateKey = aliceKeyPair.getPrivate();
      alicePrivateKeyBase64 = new String(Base64Coder.encode(alicePrivateKey.getEncoded()));
      aliceX509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              aliceKeyPair,
              UUID.randomUUID(),
              "Alice");
      final KeyPair bobKeyPair = X509Utils.generateRSAKeyPair1024();
      bobPublicKey = bobKeyPair.getPublic();
      bobPublicKeyBase64 = new String(Base64Coder.encode(bobPublicKey.getEncoded()));
      bobPrivateKey = bobKeyPair.getPrivate();
      bobPrivateKeyBase64 = new String(Base64Coder.encode(bobPrivateKey.getEncoded()));
      bobX509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              bobKeyPair,
              UUID.randomUUID(),
              "Bob");
      final KeyPair serverKeyPair = X509Utils.generateRSAKeyPair1024();
      serverPublicKey = serverKeyPair.getPublic();
      serverPublicKeyBase64 = new String(Base64Coder.encode(serverPublicKey.getEncoded()));
      serverPrivateKey = serverKeyPair.getPrivate();
    } catch (NoSuchAlgorithmException |
            NoSuchProviderException |
            IOException |
            InvalidKeyException |
            CertificateParsingException |
            CertificateEncodingException |
            SignatureException |
            InvalidAlgorithmParameterException ex) {
      throw new TexaiException(ex);
    }
    //Postconditions
    assert StringUtils.isNonEmptyString(photoBase64) : "photoBase64 must not be an empty string";
    assert StringUtils.isNonEmptyString(alicePublicKeyBase64) : "alicePublicKeyBase64 must not be an empty string";
    assert StringUtils.isNonEmptyString(alicePrivateKeyBase64) : "alicePrivateKeyBase64 must not be an empty string";
    assert StringUtils.isNonEmptyString(bobPublicKeyBase64) : "alicePublicKeyBase64 must not be an empty string";
    assert StringUtils.isNonEmptyString(bobPrivateKeyBase64) : "alicePrivateKeyBase64 must not be an empty string";
    assert StringUtils.isNonEmptyString(serverPublicKeyBase64) : "alicePublicKeyBase64 must not be an empty string";
    assert Arrays.areEqual(alicePublicKey.getEncoded(), Base64Coder.decode(alicePublicKeyBase64)) : "alicePublicKey decode failed";
    assert Arrays.areEqual(alicePrivateKey.getEncoded(), Base64Coder.decode(alicePrivateKeyBase64)) : "alicePrivateKey decode failed";
    assert Arrays.areEqual(bobPublicKey.getEncoded(), Base64Coder.decode(bobPublicKeyBase64)) : "bobPublicKey decode failed";
    assert Arrays.areEqual(bobPrivateKey.getEncoded(), Base64Coder.decode(bobPrivateKeyBase64)) : "bobPrivateKey decode failed";
    assert Arrays.areEqual(serverPublicKey.getEncoded(), Base64Coder.decode(serverPublicKeyBase64)) : "serverPublicKey decode failed";
  }

  /**
   * Gets the photo bytes.
   *
   * @return the photo bytes
   */
  public byte[] getPhotoBytes() {
    return photoBytes;
  }

  /**
   * Gets the photo encoded in base 64 notation.
   *
   * @return the photo encoded in base 64 notation
   */
  public String getPhotoBase64() {
    return photoBase64;
  }

  /**
   * Gets Alice's public key.
   *
   * @return Alice's public key
   */
  public PublicKey getAlicePublicKey() {
    return alicePublicKey;
  }

  /**
   * Gets Alice's public key encoded in base 64 notation.
   *
   * @return Alice's public key encoded in base 64 notation
   */
  public String getAlicePublicKeyBase64() {
    return alicePublicKeyBase64;
  }

  /**
   * Gets Alice's private key.
   *
   * @return Alice's private key
   */
  public PrivateKey getAlicePrivateKey() {
    return alicePrivateKey;
  }

  /**
   * Gets Alice's private key encoded in base 64 notation.
   *
   * @return Alice's private key encoded in base 64 notation
   */
  public String getAlicePrivateKeyBase64() {
    return alicePrivateKeyBase64;
  }

  /**
   * Gets the X.509 certificate for Alice.
   *
   * @return the X.509 certificate for Alice
   */
  public X509Certificate getAliceX509Certificate() {
    return aliceX509Certificate;
  }

  /**
   * Gets Bob's public key.
   *
   * @return Bob's public key
   */
  public PublicKey getBobPublicKey() {
    return bobPublicKey;
  }

  /**
   * Gets Bob's public key encoded in base 64 notation.
   *
   * @return Bob's public key encoded in base 64 notation
   */
  public String getBobPublicKeyBase64() {
    return bobPublicKeyBase64;
  }

  /**
   * Gets Bob's private key.
   *
   * @return Bob's private key
   */
  public PrivateKey getBobPrivateKey() {
    return bobPrivateKey;
  }

  /**
   * Gets Bob's private key encoded in base 64 notation.
   *
   * @return Bob's private key encoded in base 64 notation
   */
  public String getBobPrivateKeyBase64() {
    return bobPrivateKeyBase64;
  }

  /**
   * Gets the X.509 certificate for Bob.
   *
   * @return the X.509 certificate for Bob
   */
  public X509Certificate getBobX509Certificate() {
    return bobX509Certificate;
  }

  /**
   * Gets Server's public key.
   *
   * @return Server's public key
   */
  public PublicKey getServerPublicKey() {
    return serverPublicKey;
  }

  /**
   * Gets Server's public key encoded in base 64 notation.
   *
   * @return Server's public key encoded in base 64 notation
   */
  public String getServerPublicKeyBase64() {
    return serverPublicKeyBase64;
  }

  /**
   * Gets Server's private key
   *
   * @return Server's private key
   */
  public PrivateKey getServerPrivateKey() {
    return serverPrivateKey;
  }

}
