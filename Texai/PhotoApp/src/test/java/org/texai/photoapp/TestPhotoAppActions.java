package org.texai.photoapp;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * TestPhotoAppActions.java
 *
 * Description:
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class TestPhotoAppActions implements PhotoAppActions {
  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(TestPhotoAppActions.class);

  /**
   * Creates a new instance of TestPhotoAppActions.
   */
  public TestPhotoAppActions() {
  }

  /**
   * Logs in the given user, and responds by provisioning the user.
   *
   * @param username the given user name
   * @param channel the channel used for the response
   */
  @Override
  public void loginUser(
          final String username,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(username)) {
      throw new TexaiException("username must be a non-empty string");
    }
    if (channel == null) {
      throw new TexaiException("channel must not be null");
    }
    LOGGER.info("**********************");
    LOGGER.info("operation: loginUser");
    LOGGER.info("username: " + username);

    // provisionUser
    //    {
    //	"operation": "provisionUser",
    //  "userPublicKey": "user 1024-bit RSA user public key encoded in Base 64 notation",
    //	"userPrivateKey”: "user 1024-bit RSA private key encoded in Base 64 notation",
    //	"serverPublicKey”: "server 1024-bit RSA public key encoded in Base 64 notation",
    //	"buddyList”: {"1": "Bob"}
    //    }

  }

  /** Stores the given photo in to the Amazon S3 cloud, and responds with an acknowledgement.
   *
   * @param encryptedPhoto photo encrypted with server public key, encoded in Base 64 notation
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param channel the channel used for the response
   */
  @Override
  public void storePhoto(
          final String encryptedPhoto,
          final String photoHash,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(encryptedPhoto)) {
      throw new TexaiException("encryptedPhoto must be a non-empty string");
    }
    if (!StringUtils.isNonEmptyString(photoHash)) {
      throw new TexaiException("photoHash must be a non-empty string");
    }
    if (channel == null) {
      throw new TexaiException("channel must not be null");
    }
    LOGGER.info("**********************");
    LOGGER.info("operation: storePhoto");
    LOGGER.info("encryptedPhoto: " + encryptedPhoto);
    LOGGER.info("photoHash: " + photoHash);
  }

  /** Sends the specified photo from the Amazon cloud to the specified buddy user. The server verifies that the photo has not been
   * tampered with.
   *
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param recipient the user name of the recipient
   * @param channel the channel used for the response
   */
  @Override
  public void sendPhoto(
          final String photoHash,
          final String recipient,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(photoHash)) {
      throw new TexaiException("photoHash must be a non-empty string");
    }
    if (!StringUtils.isNonEmptyString(recipient)) {
      throw new TexaiException("recipient must be a non-empty string");
    }
    if (channel == null) {
      throw new TexaiException("channel must not be null");
    }
    LOGGER.info("**********************");
    LOGGER.info("operation: sendPhoto");
    LOGGER.info("photoHash: " + photoHash);
    LOGGER.info("recipient: " + recipient);
  }

}
