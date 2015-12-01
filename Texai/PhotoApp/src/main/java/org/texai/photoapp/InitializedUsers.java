package org.texai.photoapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import static org.texai.x509.X509Utils.BOUNCY_CASTLE_PROVIDER;
import static org.texai.x509.X509Utils.addBouncyCastleSecurityProvider;

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
  // the photo SHA-1 hash bytes
  private byte[] photoHash;
  // the photo SHA-1 hash bytes encoded in base 64 notation
  private String photoHashBase64;

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
      photoBase64 = new String(Base64Coder.encode(photoBytes));
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("photo " + photoPath + " length: " + photoBytes.length + " bytes");

    try {
      addBouncyCastleSecurityProvider();
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1", BOUNCY_CASTLE_PROVIDER);
      messageDigest.reset();
      photoHash = messageDigest.digest(photoBytes);
      photoHashBase64 = new String(Base64Coder.encode(photoHash));
    } catch (NoSuchAlgorithmException | NoSuchProviderException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert StringUtils.isNonEmptyString(photoBase64) : "photoBase64 must not be an empty string";
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
   * Gets the photo SHA-1 hash bytes.
   *
   * @return the photo SHA-1 hash bytes
   */
  public byte[] getPhotoHash() {
    return photoHash;
  }

  /**
   * Gets the photo SHA-1 hash bytes encoded in base 64 notation.
   *
   * @return the photo SHA-1 hash bytes encoded in base 64 notation
   */
  public String getPhotoHashBase64() {
    return photoHashBase64;
  }

}
