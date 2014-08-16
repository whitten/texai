/*
 * MockWebSocketResponseHandler.java
 *
 * Created on Jan 30, 2012, 12:20:45 PM
 *
 * Description: .
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed, Texai.org.
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
 *
 * @author reed
 */
@NotThreadSafe
public class MockWebSocketResponseHandler extends AbstractWebSocketResponseHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MockWebSocketResponseHandler.class);
  /** the web socket client handshaker */
  private final WebSocketClientHandshaker webSocketClientHandshaker;
  /** the synchronization lock to resume the client when the handshake is completed */
  final Object clientResume_lock;

  /** Constructs a new MockWebSocketResponseHandler instance.
   *
   * @param webSocketClientHandshaker the web socket client handshaker
   * @param clientResume_lock the synchronization lock to resume the client when the handshake is completed
   */
  public MockWebSocketResponseHandler(
          final WebSocketClientHandshaker webSocketClientHandshaker,
          final Object clientResume_lock) {
    //Preconditions
    assert webSocketClientHandshaker != null : "webSocketClientHandshaker must not be null";
    assert clientResume_lock != null : "clientResume_lock must not be null";

    this.webSocketClientHandshaker = webSocketClientHandshaker;
    this.clientResume_lock = clientResume_lock;
  }

  /** Handles a received web socket message.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   * @throws Exception when an exception occurs
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    final Channel channel = channelHandlerContext.getChannel();
    if (!webSocketClientHandshaker.isHandshakeComplete()) {
      LOGGER.info("finishing web socket client handshake");
      webSocketClientHandshaker.finishHandshake(channel, (HttpResponse) messageEvent.getMessage());
      LOGGER.info("web socket client handshake completed");
      synchronized (clientResume_lock) {
        // resume the waiting client thread
        clientResume_lock.notify();
      }
      return;
    }

    LOGGER.info("received " + messageEvent.getMessage().getClass().getSimpleName());
    if (messageEvent.getMessage() instanceof HttpResponse) {
      final HttpResponse httpResponse = (HttpResponse) messageEvent.getMessage();
      throw new TexaiException("Unexpected HttpResponse (status=" + httpResponse.getStatus() + ", content="
              + httpResponse.getContent().toString(CharsetUtil.UTF_8) + ")");
    }

    final WebSocketFrame webSocketFrame = (WebSocketFrame) messageEvent.getMessage();
    if (webSocketFrame instanceof TextWebSocketFrame) {
      TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) webSocketFrame;
      LOGGER.info("web socket client received message: " + textWebSocketFrame.getText());
    } else if (webSocketFrame instanceof PongWebSocketFrame) {
      LOGGER.info("web socket client received pong");
    } else if (webSocketFrame instanceof CloseWebSocketFrame) {
      LOGGER.info("web socket client received closing");
      channel.close();
    }
  }

  /** Closes the channel.
   *
   * @param channelHandlerContext the channel handler context
   * @param channelStateEvent the channel state event
   * @throws Exception when an exception occurs
   */
  @Override
  public void channelClosed(
          final ChannelHandlerContext channelHandlerContext,
          final ChannelStateEvent channelStateEvent) throws Exception {
    LOGGER.info("web socket client disconnected");
  }

  /** Catches an otherwise uncaught exception.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent
   * @throws Exception
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    LOGGER.info(StringUtils.getStackTraceAsString(exceptionEvent.getCause()));
    exceptionEvent.getChannel().close();
  }
}
