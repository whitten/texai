/*
 * TestChatSession.java
 *
 * Created on Apr 15, 2009, 12:03:00 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 15, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.webserver;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.texai.network.netty.utils.NettyHTTPUtils;
import org.texai.util.OneWayEncryptionService;

/**
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class TestChatSession implements WebChatActions {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(TestChatSession.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the indicator whether info logging is enabled */
  private static final boolean IS_INFO_LOGGING_ENABLED = LOGGER.isEnabledFor(Level.INFO);
  /** the registered user dictionary, username --> registered user */
  private final Map<String, RegisteredUser> registeredUserDictionaryByUsername = new HashMap<>();
  /** the registered user dictionary, email address --> registered user */
  private final Map<String, RegisteredUser> registeredUserDictionaryByEmailAddress = new HashMap<>();
  /** the confirmation token dictionary, confirmation token --> registered user */
  private final Map<String, RegisteredUser> confirmationTokenDictionary = new HashMap<>();
  /** the cached texai@texai.org password */
  private String texaiPassword = null;
  /** the random number generator */
  private final Random random = new Random();

  /** Constructs a new TestChatSession instance. */
  public TestChatSession() {
  }

  /** Determines whether the given username is available.
   *
   * @param username the user name
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  @Override
  public void determineUsernameAvailability(
          final String username,
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert username != null : "username must not be null";
    assert !username.isEmpty() : "username must not be empty";
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("username: " + username);
    }
    final String responseString;
    if (registeredUserDictionaryByUsername.containsKey(username)) {
      responseString = "false";
    } else {
      responseString = "true";
    }
    NettyHTTPUtils.writeHTMLResponse(httpRequest, responseString, channel, null);
  }

  /** Determines whether the given email address is available.
   *
   * @param emailAddress the email address
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  @Override
  public void determineEmailAddressAvailability(
          final String emailAddress,
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert emailAddress != null : "emailAddress must not be null";
    assert !emailAddress.isEmpty() : "emailAddress must not be empty";
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("emailAddress: " + emailAddress);
    }
    final String responseString;
    if (registeredUserDictionaryByEmailAddress.containsKey(emailAddress)) {
      responseString = "false";
    } else {
      responseString = "true";
    }
    NettyHTTPUtils.writeHTMLResponse(httpRequest, responseString, channel, null);
  }

  /** Registers the given user.
   *
   * @param firstName the first name
   * @param lastName the last name
   * @param emailAddress the email address
   * @param username the user name
   * @param encryptedPassword the encrypted password
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  @Override
  public void registerUser(
          final String firstName,
          final String lastName,
          final String emailAddress,
          final String username,
          final String encryptedPassword,
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert firstName != null : "firstName must not be null";
    assert !firstName.isEmpty() : "firstName must not be empty";
    assert lastName != null : "lastName must not be null";
    assert !lastName.isEmpty() : "lastName must not be empty";
    assert emailAddress != null : "emailAddress must not be null";
    assert !registeredUserDictionaryByEmailAddress.containsKey(emailAddress);
    assert !emailAddress.isEmpty() : "emailAddress must not be empty";
    assert username != null : "username must not be null";
    assert !username.isEmpty() : "username must not be empty";
    assert !registeredUserDictionaryByUsername.containsKey(username);
    assert encryptedPassword != null : "encryptedPassword must not be null";
    assert !encryptedPassword.isEmpty() : "encryptedPassword must not be empty";
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("firstName: " + firstName);
      LOGGER.debug("lastName: " + lastName);
      LOGGER.debug("emailAddress: " + emailAddress);
      LOGGER.debug("username: " + username);
      LOGGER.debug("encryptedPassword: " + encryptedPassword);
    }
    final RegisteredUser registeredUser = new TestRegisteredUser(
            firstName,
            lastName,
            emailAddress,
            username,
            encryptedPassword);
    final String confirmationToken = String.valueOf(Math.abs(random.nextLong()));
    confirmationTokenDictionary.put(confirmationToken, registeredUser);

    // send confirmation email in both text and html format
    final String confirmationURL = "http://localhost:8001/registration-confirmation&token=" + confirmationToken;
    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("confirmationURL: " + confirmationURL);
    }
    final String htmlContent =
            "<html><body>" +
            "To complete your Texai.org registration, please visit the following link." +
            "<p>" +
            "<a href=\"" + confirmationURL + "\">" + confirmationURL + "</a>" +
            "<p>" +
            "Please disregard this message if you did not intend to register for teaching me." +
            "<p>" +
            "-Texai" +
            "</body></html>";
    final String textContent =
            "To complete your Texai.org registration, please visit the following link.\n\n" +
            confirmationURL + "\n\n" +
            "Please disregard this message if you did not intend to register for teaching me.\n\n" +
            "-Texai";
    try {
      sendEmailMessage(
              emailAddress,
              "Texai registration confirmation",
              htmlContent,
              textContent);
    } catch (final MessagingException | IOException ex) {
      LOGGER.error("exception message: " + ex.getMessage());
      LOGGER.error("exception: " + ex);
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body><h2>An error occured.</h2></body></html>",
              channel, null);
    }
  }

  /** Confirms the user's email address.
   *
   * @param confirmationToken the confirmation token
   * @param sessionDictionary the session dictionary, parameter --> value
   * @return whether the email address was confirmed OK
   */
  @SuppressWarnings("unchecked")
  @Override
  public boolean confirmEmailAddress(
          final String confirmationToken,
          final Map<String, Object> sessionDictionary) {
    //Preconditions
    assert confirmationToken != null : "confirmationToken must not be null";
    assert !confirmationToken.isEmpty() : "confirmationToken must not be empty";
    assert sessionDictionary != null : "sessionDictionary must not be null";

    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("confirmationToken: " + confirmationToken);
    }
    final RegisteredUser registeredUser = confirmationTokenDictionary.get(confirmationToken);
    if (registeredUser == null) {
      return false;
    } else {
      final String username = registeredUser.getUsername();
      registeredUserDictionaryByUsername.put(username, registeredUser);
      registeredUserDictionaryByEmailAddress.put(registeredUser.getEmailAddress(), registeredUser);
      sessionDictionary.put(ChatServer.USERNAME_SESSION_KEY, username);
      return true;
    }
  }

  /** Authenticates the username and encrypted password.
   *
   * @param username the user ID
   * @param encryptedPassword the encrypted password
   * @param sessionDictionary the session dictionary, parameter --> value
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  @SuppressWarnings("unchecked")
  @Override
  public void authenticate(
          final String username,
          final String encryptedPassword,
          final Map<String, Object> sessionDictionary,
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert username != null : "username must not be null";
    assert !username.isEmpty() : "username must not be empty";
    assert encryptedPassword != null : "encryptedPassword must not be null";
    assert !encryptedPassword.isEmpty() : "encryptedPassword must not be empty";
    assert sessionDictionary != null : "sessionDictionary must not be null";
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final String responseString;
    final RegisteredUser registeredUser = registeredUserDictionaryByUsername.get(username);
    if (registeredUser == null) {
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("authenticate: " + username + " failed");
        LOGGER.debug("username: " + username + " is not on file");
      }
      responseString = "false";
    } else if (registeredUser.getEncryptedPassword().equals(encryptedPassword)) {
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("authenticate: " + username + " OK");
      }
      sessionDictionary.put(ChatServer.USERNAME_SESSION_KEY, registeredUser.getUsername());
      responseString = "true";
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("authenticate: " + username + " failed");
        LOGGER.debug("encrypted password: " + encryptedPassword);
        LOGGER.debug("on file:            " + registeredUser.getEncryptedPassword());
      }
    } else {
      responseString = "false";
    }
    NettyHTTPUtils.writeHTMLResponse(
            httpRequest,
            responseString,
            channel, null);
  }

  /** Validates the given email address or username.
   *
   * @param emailOrUsername the email address or username
   * @param sessionDictionary the session dictionary, parameter --> value
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  @SuppressWarnings("unchecked")
  @Override
  public void validateEmailOrUsername(
          final String emailOrUsername,
          final Map<String, Object> sessionDictionary,
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert emailOrUsername != null : "emailOrUsername must not be null";
    assert !emailOrUsername.isEmpty() : "emailOrUsername must not be empty";
    assert sessionDictionary != null : "sessionDictionary must not be null";
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    final String responseString;
    RegisteredUser registeredUser = registeredUserDictionaryByUsername.get(emailOrUsername);
    if (registeredUser == null) {
      registeredUser = registeredUserDictionaryByEmailAddress.get(emailOrUsername);
    }
    if (registeredUser == null) {
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("validateEmailOrUserName: " + emailOrUsername + " failed");
      }
      responseString = "false";
    } else {
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("validateEmailOrUserName: " + emailOrUsername + " OK");
      }
      sessionDictionary.put(ChatServer.USERNAME_SESSION_KEY, registeredUser.getUsername());
      responseString = "true";
    }
    NettyHTTPUtils.writeHTMLResponse(
            httpRequest,
            responseString,
            channel, null);
    if (responseString.equals("false")) {
      return;
    }

    // send confirmation email in both text and html format
    @SuppressWarnings("null")
    final String username = registeredUser.getUsername();
    final String newPassword = OneWayEncryptionService.getInstance().encrypt(random.nextLong()).substring(0, 12);
    final String plainText = username + newPassword;
    registeredUser.setEncryptedPassword(OneWayEncryptionService.getInstance().encrypt(plainText));
    final String htmlContent =
            "<html><body>" +
            registeredUser.getFirstName() + " " + registeredUser.getLastName() + ":" +
            "<p>" +
            "As you requested, here is your new Texai login information:" +
            "<p>" +
            "Username: <b>" + username + "</b><br>" +
            "Password: <b>" + newPassword + "</b><br>" +
            "<p>" +
            "Thanks for teaching me.<br>" +
            "-Texai" +
            "</body></html>";
    final String textContent =
            registeredUser.getFirstName() + " " + registeredUser.getLastName() + ":\n\n" +
            "As you requested, here is your new Texai login information:\n\n" +
            "Username: " + username + "\n" +
            "Password: " + newPassword + "\n\n" +
            "Thanks for teaching me.\n" +
            "-Texai";
    try {
      sendEmailMessage(
              registeredUser.getEmailAddress(),
              "Texai username and new password",
              htmlContent,
              textContent);
    } catch (final MessagingException | IOException ex) {
      LOGGER.error(ex.getMessage());
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body><h2>An error occured during email or username validation.</h2></body></html>",
              channel,
              null);
    }
  }

  /** Sends an email message.
   *
   * @param emailAddress the email address
   * @param subject the subject
   * @param htmlContent the HTML content
   * @param textContent the text content
   * @throws MessagingException when a mail messaging error occurs
   * @throws IOException when an input/output error occurs
   */
  private void sendEmailMessage(
          final String emailAddress,
          final String subject,
          final String htmlContent,
          final String textContent) throws MessagingException, IOException {
    //Preconditions
    assert emailAddress != null : "emailAddress must not be null";
    assert !emailAddress.isEmpty() : "emailAddress must not be empty";
    assert subject != null : "subject must not be null";
    assert !subject.isEmpty() : "subject must not be empty";
    assert htmlContent != null : "htmlContent must not be null";
    assert !htmlContent.isEmpty() : "htmlContent must not be empty";
    assert textContent != null : "textContent must not be null";
    assert !textContent.isEmpty() : "textContent must not be empty";

    // send confirmation email in both text and html format
    final Properties props = System.getProperties();
    final String smtpServer = "mail.texai.org";
    props.put("mail.smtp.host", smtpServer);
    final javax.mail.Session mailSession = javax.mail.Session.getDefaultInstance(props, null);
    //mailSession.setDebug(true);
    mailSession.getProperties().put("mail.smtp.auth", "true");
    final Message message = new MimeMessage(mailSession);
    message.setFrom(new InternetAddress("texai@texai.org"));
    message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(emailAddress, false));
    message.setSubject(subject);
    final MimeMultipart mimeMultipart = new MimeMultipart("alternative");
    final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
    final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
    textMimeBodyPart.setText(textContent);
    textMimeBodyPart.setHeader("MIME-Version", "1.0");
    textMimeBodyPart.setHeader("Content-Type", "text/plain");
    htmlMimeBodyPart.setContent(htmlContent, "text/html");
    htmlMimeBodyPart.setHeader("MIME-Version", "1.0");
    htmlMimeBodyPart.setHeader("Content-Type", "text/html");
    mimeMultipart.addBodyPart(textMimeBodyPart);
    mimeMultipart.addBodyPart(htmlMimeBodyPart);
    message.setContent(mimeMultipart);
    message.setHeader("MIME-Version", "1.0");
    message.setHeader("Content-Type", mimeMultipart.getContentType());
    message.setSentDate(new Date());
    final Transport transport = mailSession.getTransport("smtp");
    transport.connect(smtpServer, 26, "texai+texai.org", getTexaiPassword());
    transport.sendMessage(message, message.getAllRecipients());
    transport.close();
    if (IS_DEBUG_LOGGING_ENABLED) {
      LOGGER.debug("sent email to: " + emailAddress + " subject: " + subject);
    }
  }

  /** Gets the texai@texai.org password.
   *
   * @return the texai@texai.org password
   * @throws IOException when an input/output error occurs
   */
  private String getTexaiPassword() throws IOException {
    if (texaiPassword == null) {
      try (BufferedReader bufferedReader = new BufferedReader(new FileReader(System.getProperty("user.home") + "/jabber-client.txt"))) {
        bufferedReader.readLine();
        texaiPassword = bufferedReader.readLine();
      }
    }
    return texaiPassword;
  }

  /** Receives the webcam image message from the web client.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   * @param sessionDictionary the session dictionary, parameter --> value
   */
  @Override
  public void receiveWebcamImage(
          final HttpRequest httpRequest,
          final Channel channel, Map<String, Object> sessionDictionary) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Receives the web socket text from the web client.
   *
   * @param webSocketText the web socket text
   * @param channel the channel
   * @param sessionDictionary the session dictionary, parameter --> value
   */
  @Override
  public void receiveWebSocketText(
          final String webSocketText,
          final Channel channel,
          final Map<String, Object> sessionDictionary) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
