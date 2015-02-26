/*
 * MockWebSocketSslServerHandler.java
 *
 * Created on Jan 30, 2012, 11:30:47 AM
 *
 * Description: .
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed.
 *
 */
package org.texai.network.netty.handler;

import java.util.Locale;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class MockWebSocketSslServerHandler extends WebSocketSslServerHandler {

  // the logger */
  private static final Logger LOGGER = Logger.getLogger(MockWebSocketSslServerHandler.class);
  // the web socket path
  private static final String WEBSOCKET_PATH = "/websocket";
  // the web socket server handshaker
  private WebSocketServerHandshaker webSocketServerHandshaker;

  /**
   * Constructs a new MockWebSocketSslServerHandler instance.
   *
   * @param httpRequestHandler the parent HTTP request handler
   */
  public MockWebSocketSslServerHandler(final HTTPRequestHandler httpRequestHandler) {
    super(httpRequestHandler);
  }

  /**
   * Performs the web socket handshake to upgrade the protocol from HTTP to web socket.
   *
   * @param httpRequest the HTTP request
   * @param channelHandlerContext the channel handler context
   */
  @Override
  public void handshake(
          final HttpRequest httpRequest,
          final ChannelHandlerContext channelHandlerContext) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert channelHandlerContext != null : "channelHandlerContext must not be null";

    final String webSocketURL = "wss://" + httpRequest.getHeader(HttpHeaders.Names.HOST) + WEBSOCKET_PATH;
    final WebSocketServerHandshakerFactory webSocketServerHandshakeFactory = new WebSocketServerHandshakerFactory(
            webSocketURL, // webSocketURL
            null, // subprotocols
            false); // allowExtensions
    webSocketServerHandshaker = webSocketServerHandshakeFactory.newHandshaker(httpRequest);
    if (webSocketServerHandshaker == null) {
      webSocketServerHandshakeFactory.sendUnsupportedWebSocketVersionResponse(channelHandlerContext.getChannel());
      LOGGER.info("web socket version is not supported\n" + httpRequest);
    } else {
      webSocketServerHandshaker.handshake(channelHandlerContext.getChannel(), httpRequest);
      LOGGER.info("web socket server handshake completed");
      LOGGER.info("server pipeline ...\n" + channelHandlerContext.getChannel().getPipeline());
    }
  }

  /**
   * Receives a message from a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   *
   * @throws Exception when an exception occurs
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    final Object message = messageEvent.getMessage();
    if (message instanceof WebSocketFrame) {
      handleWebSocketFrame(channelHandlerContext, (WebSocketFrame) message);
    } else {
      throw new TexaiException("message must be a WebSocketFrame, but was " + message.getClass().getName());
    }
  }

  /**
   * Handles a received web socket frame.
   *
   * @param channelHandlerContext the channel handler context
   * @param webSocketFrame the web socket frame
   */
  private void handleWebSocketFrame(
          final ChannelHandlerContext channelHandlerContext,
          final WebSocketFrame webSocketFrame) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert webSocketFrame != null : "webSocketFrame must not be null";

    LOGGER.info("handling web socket frame " + webSocketFrame.getClass().getSimpleName());

    // Check for closing frame
    if (webSocketFrame instanceof CloseWebSocketFrame) {
      this.webSocketServerHandshaker.close(channelHandlerContext.getChannel(), (CloseWebSocketFrame) webSocketFrame);
      return;
    } else if (webSocketFrame instanceof PingWebSocketFrame) {
      channelHandlerContext.getChannel().write(new PongWebSocketFrame(webSocketFrame.getBinaryData()));
      return;
    } else if (!(webSocketFrame instanceof TextWebSocketFrame)) {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", webSocketFrame.getClass().getName()));
    }

    // Send the uppercase string back.
    String request = ((TextWebSocketFrame) webSocketFrame).getText();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(String.format("Channel %s received %s", channelHandlerContext.getChannel().getId(), request));
    }
    channelHandlerContext.getChannel().write(new TextWebSocketFrame(request.toUpperCase(Locale.ENGLISH)));
  }

  /**
   * Processes an exception not otherwise caught.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   *
   * @throws Exception when an exception occurs
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "e must not be null";

    LOGGER.warn(StringUtils.getStackTraceAsString(exceptionEvent.getCause()));
    exceptionEvent.getChannel().close();
  }

}
