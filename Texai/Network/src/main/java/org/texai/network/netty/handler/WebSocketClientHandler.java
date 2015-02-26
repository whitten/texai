/*
 * WebSocketClientHandler.java
 *
 * Created on Jan 29, 2012, 7:28:36 PM
 *
 * Description: Provides a web socket client handler.
 *
 * Copyright (C) Jan 29, 2012, Stephen L. Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides a web socket client handler.
 *
 * @author reed
 */
@NotThreadSafe
public class WebSocketClientHandler extends SimpleChannelUpstreamHandler {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(WebSocketClientHandler.class);
  // the web socket client handshaker
  private final WebSocketClientHandshaker webSocketClientHandshaker;

  /**
   * Constructs a new WebSocketClientHandler instance.
   *
   * @param webSocketClientHandshaker the web socket client handshaker
   */
  public WebSocketClientHandler(final WebSocketClientHandshaker webSocketClientHandshaker) {
    //Preconditions
    assert webSocketClientHandshaker != null : "webSocketClientHandshaker must not be null";

    this.webSocketClientHandshaker = webSocketClientHandshaker;
  }

  /**
   * Notifies when a {@link Channel} was closed and all its related resources were released.
   *
   * @param ctx the channel handler context
   * @param e the channel state event
   *
   * @throws Exception when an exception occurs
   */
  @Override
  public void channelClosed(
          final ChannelHandlerContext ctx,
          final ChannelStateEvent e) throws Exception {
    LOGGER.info("WebSocket Client disconnected!");
  }

  /**
   * Receives a message object from a remote peer.
   *
   * @param ctx the channel handler context
   * @param e the channel state event
   *
   * @throws Exception
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext ctx,
          final MessageEvent e) throws Exception {
    final Channel channel = ctx.getChannel();
    if (!webSocketClientHandshaker.isHandshakeComplete()) {
      webSocketClientHandshaker.finishHandshake(channel, (HttpResponse) e.getMessage());
      LOGGER.info("WebSocket Client connected!");
      return;
    }

    if (e.getMessage() instanceof HttpResponse) {
      HttpResponse response = (HttpResponse) e.getMessage();
      throw new TexaiException("Unexpected HttpResponse (status=" + response.getStatus() + ", content="
              + response.getContent().toString(CharsetUtil.UTF_8) + ")");
    }

    final WebSocketFrame webSocketFrame = (WebSocketFrame) e.getMessage();
    if (webSocketFrame instanceof TextWebSocketFrame) {
      TextWebSocketFrame textFrame = (TextWebSocketFrame) webSocketFrame;
      LOGGER.info("WebSocket Client received message: " + textFrame.getText());
    } else if (webSocketFrame instanceof PongWebSocketFrame) {
      LOGGER.info("WebSocket Client received pong");
    } else if (webSocketFrame instanceof CloseWebSocketFrame) {
      LOGGER.info("WebSocket Client received closing");
      channel.close();
    }
  }

  /**
   * Receives a raised exception.
   *
   * @param ctx the channel handler context
   * @param e the channel state event
   *
   * @throws Exception
   */
  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
    LOGGER.info(StringUtils.getStackTraceAsString(e.getCause()));
    e.getChannel().close();
  }
}
