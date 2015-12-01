package org.texai.photoapp;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.joda.time.DateTime;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

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
  // the users initialization object
  private final InitializedUsers initializedUsers = new InitializedUsers();
  // the user dictionary, channel --> username
  private final Map<Channel, String> userDictionary = new HashMap<>();
  // the channel dictionary, username --> channel
  private final Map<String, Channel> channelDictionary = new HashMap<>();

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
      sendErrorMessage(
              "username must be a non-empty string",
              channel);
      return;
    }
    if (channel == null) {
      sendErrorMessage(
              "channel must not be null",
              channel);
      return;
    }
    LOGGER.info("operation: loginUser");
    LOGGER.info("username: " + username);
    userDictionary.put(channel, username);
    channelDictionary.put(username, channel);

    LOGGER.info("********** server sending ************");
    // provisionUser
    //    {
    //      "operation": "provisionUser",
    //      "buddyList”: {"1": "Bob"}
    //    }
    final String buddyUsername;
    if (username.equals("Alice")) {
      buddyUsername = "Bob";
    } else {
      buddyUsername = "Alice";
    }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"provisionUser\"\n")
            .append("\"buddyList\": {\"1\": \"")
            .append(buddyUsername)
            .append("\"}\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Sends an error message to the client.
   *
   * @param errorMessage the error message
   * @param channel the channel used for the response
   */
  @Override
  public void sendErrorMessage(
          final String errorMessage,
          final Channel channel) {
    //Preconditions
    assert StringUtils.isNonEmptyString(errorMessage) : "errorMessage must be a non-empty string";
    assert channel != null : "channel must not be null";

    LOGGER.info("********** server sending ************");
    // storageResponse
    //  {
    //    "operation": "errorNotification",
    //    "errorMessage": "[for example] recipient not logged in"
    //  }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"errorNotification\",\n")
            .append("\"errorMessage\": \"")
            .append(errorMessage)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Stores the given photo in to the Amazon S3 cloud, and responds with an acknowledgement.
   *
   * @param photo photo encoded in Base 64 notation
   * @param photoHash the SHA-1 hash of the photo encoded in Base 64 notation
   * @param channel the channel used for the response
   */
  @Override
  public void storePhoto(
          final String photo,
          final String photoHash,
          final Channel channel) {
    if (!StringUtils.isNonEmptyString(photo)) {
      sendErrorMessage(
              "photo must be a non-empty string",
              channel);
      return;
    }
    if (!StringUtils.isNonEmptyString(photoHash)) {
      sendErrorMessage(
              "photoHash must be a non-empty string",
              channel);
      return;
    }
    if (channel == null) {
      throw new TexaiException("channel must not be null");
    }
    final String username = userDictionary.get(channel);
    if (username == null) {
      sendErrorMessage(
              "user not logged in",
              channel);
      return;
    }
    LOGGER.info("logged in user: " + username);
    LOGGER.info("operation: storePhoto");
    LOGGER.info("photo: " + photo);
    LOGGER.info("photoHash: " + photoHash);

    final byte[] photoBytes = Base64Coder.decode(photo);
    try {
      FileUtils.writeByteArrayToFile(new File("data/orb-test.jpg"), photoBytes);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }

    LOGGER.info("********** server sending ************");
    // storageResponse
    //    {
    //      "operation": "storageResponse",
    //      "timestamp": "the date and time that the photo was stored in the Amazon cloud, as a UTC string"
    //      "duplicate": "yes or no"
    //    }
    final StringBuilder stringBuilder = new StringBuilder();
    final String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"storageResponse\",\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"duplicate\": \"yes\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Sends the specified photo from the Amazon cloud to the specified buddy user. The server verifies that the photo has not been tampered
   * with.
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
      sendErrorMessage(
              "photoHash must be a non-empty string",
              channel);
      return;
    }
    if (!StringUtils.isNonEmptyString(recipient)) {
      throw new TexaiException("recipient must be a non-empty string");
    }
    if (channel == null) {
      sendErrorMessage(
              "channel must not be null",
              channel);
      return;
    }
    final String username = userDictionary.get(channel);
    if (username == null) {
      sendErrorMessage(
              "user not logged in",
              channel);
      return;
    }
    LOGGER.info("logged in user: " + username);
    LOGGER.info("operation: sendPhoto");
    LOGGER.info("photoHash: " + photoHash);
    LOGGER.info("recipient: " + recipient);

    LOGGER.info("********** server sending to " + recipient + " ************");
    //  {
    //    "operation": "receivePhoto",
    //    "photo": "photo encoded in Base 64 notation",
    //    "photoHash": "the SHA-1 hash of the selected photo encoded in Base 64 notation",
    //    "timestamp": "the date and time that the photo was stored in the Amazon cloud, as a UTC string"
    //    "sender": "Alice"
    //  }
    final Channel recipientChannel = channelDictionary.get(recipient);
    if (recipientChannel == null) {
      sendErrorMessage(
              "recipient channel must not be null",
              channel);
      return;
    }
    final StringBuilder stringBuilder = new StringBuilder();
    String jsonString = stringBuilder
            .append("{\n")
            .append("\"operation\": \"receivePhoto\",\n")
            .append("\"photo\": \"")
            .append(initializedUsers.getPhotoBase64())
            .append("\",\n")
            .append("\"photoHash\": \"")
            .append(initializedUsers.getPhotoHashBase64())
            .append("\"\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"sender\": \"")
            .append(username)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    recipientChannel.write(new TextWebSocketFrame(jsonString));

    LOGGER.info("********** server sending to " + username + " ************");
    //    {
    //      "operation": "photoDelivered",
    //      "photoHash": "the SHA-1 hash of the selected photo encoded in Base 64 notation",
    //      "timestamp": "the date and time that the photo was delivered, as a UTC string"
    //      "recipient": "Bob"
    //    }
    jsonString = (new StringBuilder())
            .append("{\n")
            .append("\"operation\": \"photoDelivered\",\n")
            .append("\"photoHash\": \"")
            .append(initializedUsers.getPhotoHashBase64())
            .append("\"\n")
            .append("\"timestamp\": \"")
            .append((new DateTime()).toString())
            .append("\",\n")
            .append("\"recipient\": \"")
            .append(recipient)
            .append("\"\n")
            .append("}\n")
            .toString();
    LOGGER.info("server sending: " + jsonString);
    channel.write(new TextWebSocketFrame(jsonString));
  }

  /**
   * Gets the users initialization object.
   *
   * @return the users initialization object
   */
  public InitializedUsers getInitializedUsers() {
    return initializedUsers;
  }

}
