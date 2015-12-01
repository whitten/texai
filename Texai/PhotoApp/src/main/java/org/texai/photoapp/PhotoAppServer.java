package org.texai.photoapp;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;
import org.texai.util.TexaiException;

/**
 * PhotoAppServer.java
 *
 * Description: Provides a websocket server for the AI Chain Photo Application.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class PhotoAppServer implements TexaiHTTPRequestHandler {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(PhotoAppServer.class);
  // the production or test object which handles the photo app actions, which is injected as a dependency
  private PhotoAppActions photoAppActions;

  /**
   * Creates a new instance of PhotoAppServer.
   */
  public PhotoAppServer() {
  }

  /**
   * Handles the HTTP request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   *
   * @return the indicator whether the HTTP request was handled
   */
  @Override
  public boolean httpRequestReceived(
          final HttpRequest httpRequest,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channel != null : "channel must not be null";

    LOGGER.info("httpRequest: " + httpRequest);
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("protocol version: " + httpRequest.getProtocolVersion());
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("uri: " + httpRequest.getUri());
    for (final String headerName : httpRequest.getHeaderNames()) {
      LOGGER.info("header: " + headerName + " " + httpRequest.getHeader(headerName));
    }
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Handles a received text web socket frame.
   *
   * @param channel the channel handler context
   * @param textWebSocketFrame the text web socket frame
   *
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
    LOGGER.info("********** server receiving ************");
    LOGGER.info("web socket text received: " + webSocketText);

    final JSONParser jsonParser = new JSONParser();
    final JSONObject responseJSONObject;
    try {
      responseJSONObject = (JSONObject) jsonParser.parse(webSocketText);
    } catch (ParseException ex) {
      throw new TexaiException(ex);
    }

    final String operation = (String) responseJSONObject.get("operation");
    switch (operation) {
      case "login": {
        final String username = (String) responseJSONObject.get("username");
        photoAppActions.loginUser(username, channel);
        break;
      }
      case "storePhoto": {
        final String photo = (String) responseJSONObject.get("photo");
        final String photoHash = (String) responseJSONObject.get("photoHash");
        photoAppActions.storePhoto(photo, photoHash, channel);
        break;
      }
      case "sendPhoto": {
        final String photoHash = (String) responseJSONObject.get("photoHash");
        final String recipient = (String) responseJSONObject.get("recipient");
        photoAppActions.sendPhoto(photoHash, recipient, channel);
        break;
      }
      default: {
        final String errorMessage = "invalid operation: '" + operation + "'";
        LOGGER.info(errorMessage);
        photoAppActions.sendErrorMessage(webSocketText, channel);
      }
    }

    return true;
  }

  /**
   * Sets the production or test object which handles the photo app actions
   *
   * @param photoAppActions the photoAppActions to set
   */
  public void setPhotoAppActions(final PhotoAppActions photoAppActions) {
    assert photoAppActions != null : "photoAppActions must not be null";

    this.photoAppActions = photoAppActions;
  }

}
