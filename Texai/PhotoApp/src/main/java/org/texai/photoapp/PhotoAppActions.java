package org.texai.photoapp;

import org.jboss.netty.channel.Channel;

/**
 * Created on Nov 30, 2015, 12:25:02 PM.
 *
 * Description: Defines the actions for the AI Chain Photo Application. An interface is used so that production and test behaviors may be
 * conveniently encapsulated in separate objects.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed, Texai.org.
 *
 * @author reed
 */
public interface PhotoAppActions {

  /**
   * Logs in the given user, and responds by provisioning the user.
   *
   * @param userName the given user name
   * @param channel the channel used for the response
   */
  public void loginUser(
          final String userName,
          final Channel channel);

  /** Stores the given photo in to the Amazon S3 cloud, and responds with an acknowledgement.
   *
   * @param encryptedPhoto photo encrypted with server public key, encoded in Base 64 notation
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param channel the channel used for the response
   */
  public void storePhoto(
          final String encryptedPhoto,
          final String photoHash,
          final Channel channel);


  /** Sends the specified photo from the Amazon cloud to the specified buddy user. The server verifies that the photo has not been
   * tampered with.
   *
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param recipient the user name of the recipient
   * @param channel the channel used for the response
   */
  public void sendPhoto(
          final String photoHash,
          final String recipient,
          final Channel channel);
}
