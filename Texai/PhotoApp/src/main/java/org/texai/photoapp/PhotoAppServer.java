package org.texai.photoapp;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;

/**
 * PhotoAppServer.java
 *
 * Description: Provides a websocket server for the AI Chain Photo Application.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class PhotoAppServer implements TexaiHTTPRequestHandler {

  /**
   * the log4j logger
   */
  private static final Logger LOGGER = Logger.getLogger(PhotoAppServer.class);

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
    LOGGER.info("web socket text received: " + webSocketText);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("web socket text received: " + webSocketText);
    }

    return true;
  }

}
