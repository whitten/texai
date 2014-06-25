/*
 * WebChatSession.java
 *
 * Created on Apr 15, 2009, 11:45:23 AM
 *
 * Description: Defines web chat actions.
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

import java.util.Map;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;

/** Defines web chat actions.
 *
 * @author Stephen L. Reed.
 */
public interface WebChatActions {

  /** Determines whether the given username is available.
   *
   * @param username the user name
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  void determineUsernameAvailability(
          final String username,
          final HttpRequest httpRequest,
          final Channel channel);

  /** Determines whether the given email address is available.
   *
   * @param emailAddress the email address
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  void determineEmailAddressAvailability(
          final String emailAddress,
          final HttpRequest httpRequest,
          final Channel channel);

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
  void registerUser(
          final String firstName,
          final String lastName,
          final String emailAddress,
          final String username,
          final String encryptedPassword,
          final HttpRequest httpRequest,
          final Channel channel);

  /** Confirms the user's email address.
   *
   * @param confirmationToken the confirmation token
   * @param sessionDictionary the session dictionary, parameter --> value
   * @return whether the email address was confirmed OK
   */
  boolean confirmEmailAddress(
          final String confirmationToken,
          final Map<String, Object> sessionDictionary);

  /** Authenticates the username and encrypted password.
   *
   * @param username the user ID
   * @param encryptedPassword the encrypted password
   * @param sessionDictionary the session dictionary, parameter --> value
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  void authenticate(
          final String username,
          final String encryptedPassword,
          final Map<String, Object> sessionDictionary,
          final HttpRequest httpRequest,
          final Channel channel);

  /** Validates the given email address or username.
   *
   * @param emailOrUsername the email address or username
   * @param sessionDictionary the session dictionary, parameter --> value
   * @param httpRequest the HTTP request
   * @param channel the channel
   */
  void validateEmailOrUsername(
          final String emailOrUsername,
          final Map<String, Object> sessionDictionary,
          final HttpRequest httpRequest,
          final Channel channel);

  /** Receives the webcam image message from the web client.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   * @param sessionDictionary the session dictionary, parameter --> value
   */
  void receiveWebcamImage(
          final HttpRequest httpRequest,
          final Channel channel,
          final Map<String, Object> sessionDictionary);

  /** Receives the web socket text from the web client.
   *
   * @param webSocketText the web socket text
   * @param channel the channel
   * @param sessionDictionary the session dictionary, parameter --> value
   */
  void receiveWebSocketText(
          final String webSocketText,
          final Channel channel,
          final Map<String, Object> sessionDictionary);
}
