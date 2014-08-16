/*
 * WebSocketSslServerHandler.java
 *
 * Created on Jan 30, 2012, 9:49:41 AM
 *
 * Description: Provides a web socket SSL server handler.
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.network.netty.handler;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
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

/** Provides a web socket SSL server handler. Each web socket connection has its own handler instance because the
 * handshaker is not shared.
 *
 * @author reed
 */
@NotThreadSafe
public class WebSocketSslServerHandler extends SimpleChannelUpstreamHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(WebSocketSslServerHandler.class);
  /** the web socket path */
  private static final String WEBSOCKET_PATH = "/websocket";
  /** the web socket server handshaker */
  private WebSocketServerHandshaker webSocketServerHandshaker;
  /** the parent HTTP request handler, that knows about the Texai HTTP request handler chain */
  private final HTTPRequestHandler httpRequestHandler;

  /** Constructs a new WebSocketSslServerHandler instance.
   *
   * @param httpRequestHandler the parent HTTP request handler, that knows about the Texai HTTP request handler chain
   */
  public WebSocketSslServerHandler(final HTTPRequestHandler httpRequestHandler) {
    this.httpRequestHandler = httpRequestHandler;
  }

  /** Performs the web socket handshake to upgrade the protocol from HTTP to web socket.
   *
   * @param httpRequest the HTTP request
   * @param channelHandlerContext the channel handler context
   */
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
      LOGGER.warn("web socket version is not supported\n" + httpRequest);
    } else {
      webSocketServerHandshaker.handshake(channelHandlerContext.getChannel(), httpRequest);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("web socket server handshake completed");
        LOGGER.debug("server pipeline ...\n" + channelHandlerContext.getChannel().getPipeline());
      }
    }
  }

  /** Receives a message from a remote peer.
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

    final Object message = messageEvent.getMessage();
    if (message instanceof WebSocketFrame) {
      handleWebSocketFrame(channelHandlerContext, (WebSocketFrame) message);
    } else {
      throw new TexaiException("message must be a WebSocketFrame, but was " + message.getClass().getName());
    }
  }

  /** Handles a received web socket frame.
   *
   * @param channelHandlerContext the channel handler context
   * @param webSocketFrame  the web socket frame
   */
  private void handleWebSocketFrame(
          final ChannelHandlerContext channelHandlerContext,
          final WebSocketFrame webSocketFrame) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert webSocketFrame != null : "webSocketFrame must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("handling web socket frame " + webSocketFrame.getClass().getSimpleName());
    }

    if (webSocketFrame instanceof CloseWebSocketFrame) {
      webSocketServerHandshaker.close(channelHandlerContext.getChannel(), (CloseWebSocketFrame) webSocketFrame);
      return;
    } else if (webSocketFrame instanceof PingWebSocketFrame) {
      channelHandlerContext.getChannel().write(new PongWebSocketFrame(webSocketFrame.getBinaryData()));
      return;
    } else if ((webSocketFrame instanceof TextWebSocketFrame)) {
      final List<TexaiHTTPRequestHandler> texaiHTTPRequestHandlers = httpRequestHandler.getTexaiHTTPRequestHandlers();
      synchronized (texaiHTTPRequestHandlers) {
        for (final TexaiHTTPRequestHandler texaiHTTPRequestHandler : texaiHTTPRequestHandlers) {
          final boolean isHandled = texaiHTTPRequestHandler.textWebSocketFrameReceived(
                  channelHandlerContext.getChannel(),
                  (TextWebSocketFrame) webSocketFrame);
          if (isHandled) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(texaiHTTPRequestHandler.getClass().getName() + " handled the request");
            }
            return;
          }
        }
      }
      assert false : "no handler for the web socket frame: " + webSocketFrame;
    } else {
      throw new UnsupportedOperationException(String.format("%s frame types not supported", webSocketFrame.getClass().getName()));
    }
  }

  /** Processes an exception not otherwise caught.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   * @throws Exception when an exception occurs
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "e must not be null";

    LOGGER.warn(channelHandlerContext.getChannel() + "\n" +
            StringUtils.getStackTraceAsString(exceptionEvent.getCause()));
    exceptionEvent.getChannel().close();
    throw new TexaiException(exceptionEvent.getCause());
  }
}
