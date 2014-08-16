/*
 * ChatServer.java
 *
 * Created on Apr 14, 2009, 2:37:17 PM
 *
 * Description: Handles HTTP requests specifically for chat dialog.
 *
 * Copyright (C) Apr 14, 2009 Stephen L. Reed.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;
import org.texai.network.netty.utils.NettyHTTPUtils;
import org.texai.network.netty.utils.NettyJSONUtils;
import org.texai.util.HTTPUtils;
import org.texai.util.OneWayEncryptionService;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Handles HTTP requests specifically for chat dialog.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class ChatServer implements TexaiHTTPRequestHandler {

  /** the username session key */
  public static final String USERNAME_SESSION_KEY = "username";
  /** the cookie session key */
  public static final String COOKIE_SESSION_KEY = "cookie";
  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(ChatServer.class);
  /** the fileInputStream cache, path --> fileInputStream bytes */
  private final ConcurrentHashMap<String, byte[]> fileCache = new ConcurrentHashMap<>();
  /** the root path */
  private static final File ROOT_PATH = new File("html/");
  /** the chat actions */
  private WebChatActions chatSession;
  /** the session dictionary dictionary, session cookie --> session dictionary (parameter --> value) */
  private final Map<String, ConcurrentHashMap<String, Object>> sessionDictionaryDictionary = new ConcurrentHashMap<>();
  /** the IP address / cookie dictionary, IP address --> session cookie */
  private final Map<String, String> ipAddressCookieDictionary = new ConcurrentHashMap<>();

  //TODO Because the ChatServer instance is shared among all HTTP requests, put a session cookie in the dialog HTML
  // client and pass it on each HTTP request.
  // The webcam page does not have its own cookie, but uses the cookie of the dialog page at that IP address.
  /** Constructs a new ChatServer instance. */
  public ChatServer() {
  }

  /** Handles the HTTP request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   * @return the indicator whether the HTTP request was handled
   */
  @Override
  public boolean httpRequestReceived(final HttpRequest httpRequest, final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";
    assert chatSession != null : "chatSession must not be null";

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("httpRequest: " + httpRequest);
      LOGGER.info("method: " + httpRequest.getMethod());
      LOGGER.info("protocol version: " + httpRequest.getProtocolVersion());
      LOGGER.info("method: " + httpRequest.getMethod());
      LOGGER.info("uri: " + httpRequest.getUri());
      for (final String headerName : httpRequest.getHeaderNames()) {
        LOGGER.info("header: " + headerName + " " + httpRequest.getHeader(headerName));
      }
    }
    final URI uri;
    try {
      uri = new URI(httpRequest.getUri());
    } catch (URISyntaxException ex) {
      throw new TexaiException(ex);
    }
    final String path = uri.getPath();
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info("path: " + path);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(httpRequest.getMethod() + " " + path);
      }
    }
    final Map<String, String> parameterDictionary = new HashMap<>();
    if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
      parameterDictionary.putAll(HTTPUtils.getQueryMap(uri.getRawQuery()));
    }

    final ConcurrentHashMap<String, Object> sessionDictionary = getSessionDictionary(
            null, // sessionCookie
            channel);
    if (path.equals("/clear-file-cache")) {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("clearing the file cache");
      }
      fileCache.clear();
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body><h2>File Cache Cleared</h2></body></html>",
              channel,
              null); // sessionCookie
      return true;
    }

    if (httpRequest.getMethod().equals(HttpMethod.POST) && path.equals("/upload-picture")) {
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body>image uploaded OK</body></html>",
              channel,
              null); // sessionCookie
      // uploaded webcam image
      chatSession.receiveWebcamImage(
              httpRequest,
              channel,
              sessionDictionary);
      return true;
    }

    //TODO some of the below actions have not been tested since the migration to the single page interface

    String nextTarget = null;
    try {
      if (path.equals("/availableUsername")) {
        // validate new username
        final String username = parameterDictionary.get("username");
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("availableUsername: " + username);
        }
        chatSession.determineUsernameAvailability(username, httpRequest, channel);
        return true;
      }

      if (path.equals("/availableEmailAddress")) {
        // validate new email address
        final String emailAddress = parameterDictionary.get("emailAddress");
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("availableEmailAddress: " + emailAddress);
        }
        chatSession.determineUsernameAvailability(emailAddress, httpRequest, channel);
        return true;
      }

      if (path.equals("/authenticate")) {
        // authenticate the username and password
        final String username = parameterDictionary.get("username");
        final String plainText = username + parameterDictionary.get("password");
        final String encryptedPassword = OneWayEncryptionService.getInstance().encrypt(plainText);
        chatSession.authenticate(
                username,
                encryptedPassword,
                sessionDictionary,
                httpRequest,
                channel);
        return true;
      }

      if (path.equals("/validateEmailOrUsername")) {
        // validate the email address or username, and if valid then send user their username and new password
        final String emailOrUsername = parameterDictionary.get("emailOrUsername");
        chatSession.validateEmailOrUsername(
                emailOrUsername,
                sessionDictionary,
                httpRequest, channel);
        return true;
      }

      if (httpRequest.getMethod().equals(HttpMethod.POST) && path.equals("/send-username-new-password")) {
        // forgot username or password
        nextTarget = "/forgot-username-password";

      } else if (httpRequest.getMethod().equals(HttpMethod.POST) && path.equals("/login")) {
        // login completion
        nextTarget = "/chat";

      } else if (httpRequest.getMethod().equals(HttpMethod.POST) && path.equals("/register")) {
        // new user registration
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("register: " + httpRequest);
        }
        final String username = parameterDictionary.get("username");
        final String plainText = username + parameterDictionary.get("password");
        final String encryptedPassword = OneWayEncryptionService.getInstance().encrypt(plainText);
        chatSession.registerUser(
                parameterDictionary.get("firstName"),
                parameterDictionary.get("lastName"),
                parameterDictionary.get("email"),
                username,
                encryptedPassword,
                httpRequest,
                channel);
        nextTarget = "/chat/email-confirmation";

      } else if (path.startsWith("/registration-confirmation&token=")) {
        // registration confirmation
        final String confirmationToken = path.substring(33);
        final boolean isError;
        if (confirmationToken == null || confirmationToken.isEmpty()) {
          isError = true;
        } else {
          isError = !chatSession.confirmEmailAddress(confirmationToken, sessionDictionary);
        }
        if (isError) {
          NettyHTTPUtils.writeHTMLResponse(
                  httpRequest,
                  "<html><body><h2>A registration confirmation error occured.</h2></body></html>",
                  channel,
                  null); // sessionCookie
        } else {
          nextTarget = "/chat";
        }
      }

      final String target;
      if (nextTarget == null) {
        target = path;
      } else {
        target = nextTarget;
      }
      final String filePath = target.replace('/', File.separatorChar);
      LOGGER.info("filePath: " + ROOT_PATH + filePath);
      if (new File(ROOT_PATH, filePath).isDirectory()) {
        NettyHTTPUtils.writeHTMLResponse(
                httpRequest,
                "<html><body><h2>You do not have permission to view this directory</h2></body></html>",
                channel,
                null); // sessionCookie
        return true;
      }

      // serve file
      byte[] cachedFileContent = fileCache.get(filePath);
      if (cachedFileContent == null) {
        File file;
        InputStream fileInputStream = null;
        try {
          file = new File(ROOT_PATH, filePath);
          LOGGER.info("serving file: " + file);
          fileInputStream = new FileInputStream(file);
        } catch (final FileNotFoundException ex) {
          if (fileInputStream != null) {
            fileInputStream.close();
          }
          return false;
        }
        assert file != null;
        assert fileInputStream != null;
        final byte[] chunk = new byte[(int) file.length()];
        int count;
        int pos = 0;

        while ((count = fileInputStream.read(chunk, pos, chunk.length - pos)) > 0) {
          pos += count;
        }
        cachedFileContent = chunk;
        fileCache.put(filePath, cachedFileContent);

      } else {
        LOGGER.info(" ...cached");
      }
      assert cachedFileContent != null;
      if (path.endsWith(".html")) {
        NettyHTTPUtils.writeHTMLResponse(
                httpRequest,
                new String(cachedFileContent, "US-ASCII"),
                channel,
                null); // sessionCookie
      } else if (path.endsWith(".css")) {
        NettyHTTPUtils.writeCSSResponse(httpRequest, new String(cachedFileContent, "US-ASCII"), channel);
      } else {
        NettyHTTPUtils.writeBinaryResponse(
                httpRequest,
                cachedFileContent,
                channel,
                null); // sessionCookie
      }
      return true;
    } catch (final IOException ex) {
      LOGGER.error("exception message: " + ex.getMessage());
      LOGGER.error("exception: " + ex);
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      NettyHTTPUtils.writeHTMLResponse(
              httpRequest,
              "<html><body><h2>An error occured.</h2></body></html>",
              channel,
              null); // sessionCookie
      return true;
    }
  }

  /** Gets the session dictionary given the cookie, and if the cookie is absent then the IP address is used to get
   * the associated dialog session's cookie.
   *
   * @param sessionCookie the session cookie
   * @param channel the channel
   * @return the session dictionary
   */
  private ConcurrentHashMap<String, Object> getSessionDictionary(
          final String sessionCookie,
          final Channel channel) {
    //Preconditions
    assert channel != null : "channel must not be null";

    ConcurrentHashMap<String, Object> sessionDictionary;
    String sessionCookie1;
    final String hostString = ((InetSocketAddress) channel.getRemoteAddress()).getHostString();
    synchronized (ipAddressCookieDictionary) {
      if (sessionCookie == null || sessionCookie.equals("undefined")) {
        // no session cookie - the request might be from the webcam so try getting the cookie of the dialog at the same IP address
        sessionCookie1 = ipAddressCookieDictionary.get(hostString);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("cookie from associated IP address: " + sessionCookie1);
        }
      } else {
        sessionCookie1 = sessionCookie;
      }
      if (sessionCookie1 == null) {
        // no session cookie so generate one
        sessionCookie1 = UUID.randomUUID().toString();
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info("generated cookie: " + sessionCookie1);
        }
      }
      // update the IP address / session cookie dictionary
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(hostString + " --> " + sessionCookie1);
      }
      ipAddressCookieDictionary.put(hostString, sessionCookie1);
    }
    // get the existing session dictionary or create a new empty one
    sessionDictionary = sessionDictionaryDictionary.get(sessionCookie1);
    if (sessionDictionary == null) {
      sessionDictionary = new ConcurrentHashMap<>();
      sessionDictionaryDictionary.put(sessionCookie1, sessionDictionary);
      // set the session cookie of the session dictionary
      sessionDictionary.put(COOKIE_SESSION_KEY, sessionCookie1);
    }

    //Postconditions
    assert sessionDictionary != null : "sessionDictionary must not be null";

    return sessionDictionary;
  }

  /** Gets the chat session.
   *
   * @return the chat session
   */
  public WebChatActions getChatSession() {
    return chatSession;
  }

  /** Sets the chat session.
   *
   * @param chatSession the chat session
   */
  public void setChatSession(final WebChatActions chatSession) {
    this.chatSession = chatSession;
  }

  /** Handles a received text web socket frame.
   *
   * @param channel the channel handler context
   * @param textWebSocketFrame  the text web socket frame
   * @return the indicator whether the web socket request was handled
   */
  @Override
  public boolean textWebSocketFrameReceived(
          final Channel channel,
          final TextWebSocketFrame textWebSocketFrame) {
    //Preconditions
    assert channel != null : "channel must not be null";
    assert textWebSocketFrame != null : "textWebSocketFrame must not be null";

    final String webSocketText = textWebSocketFrame.getText();
    LOGGER.info("web socket text received: " + webSocketText);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("web socket text received: " + webSocketText);
    }
    final ConcurrentHashMap<String, Object> sessionDictionary = getSessionDictionary(
            NettyJSONUtils.getTexaiSessionCookie(textWebSocketFrame.getText()), // sessionCookie
            channel);
    chatSession.receiveWebSocketText(
            webSocketText,
            channel,
            sessionDictionary);
    return true;
  }

  /** Gets the IP address / cookie dictionary, IP address --> session cookie.
   *
   * @return the IP address / cookie dictionary
   */
  public Map<String, String> getIpAddressCookieDictionary() {
    return ipAddressCookieDictionary;
  }
}
