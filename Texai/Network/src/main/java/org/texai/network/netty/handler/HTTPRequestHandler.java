/*
 * HTTPRequestHandler.java
 *
 * Created on Feb 11, 2010, 7:58:40 PM
 *
 * Description: Provides a multiplexed HTTP request handler.
 *
 * Copyright (C) Feb 11, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.network.netty.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides a multiplexed HTTP request handler.
 *
 * @author reed
 */
@NotThreadSafe
public final class HTTPRequestHandler extends AbstractHTTPRequestHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPRequestHandler.class);
  /** the Texai HTTP request handler chain */
  private final List<TexaiHTTPRequestHandler> texaiHTTPRequestHandlers = new ArrayList<>();
  /**  the singleton HTTP request handler instance */
  private static final HTTPRequestHandler httpRequestHandler = new HTTPRequestHandler();

  /** Constructs a new HTTPRequestHandler instance. */
  private HTTPRequestHandler() {
  }

  /** Gets the singleton HTTP request handler instance.
   *
   * @return the singleton HTTP request handler instance
   */
  public static HTTPRequestHandler getInstance() {
    return httpRequestHandler;
  }

  /** Handles the message object that was received from a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert messageEvent != null : "messageEvent must not be null";

    final HttpRequest httpRequest = (HttpRequest) messageEvent.getMessage();
    LOGGER.info("---------------------------------------------------------------------");
    LOGGER.info("httpRequest: " + httpRequest);
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("protocol version: " + httpRequest.getProtocolVersion());
    LOGGER.info("method: " + httpRequest.getMethod());
    LOGGER.info("uri: " + httpRequest.getUri());
    boolean isUpgradeWebsocket = false;
    for (final String headerName : httpRequest.getHeaderNames()) {
      final String header = httpRequest.getHeader(headerName);
      LOGGER.info("header: " + headerName + " " + header);
      if (headerName.equals("Upgrade") && header.toLowerCase(Locale.ENGLISH).equals("websocket")) {
        isUpgradeWebsocket = true;
      }
    }

    if (isUpgradeWebsocket) {
      switchToWebSocket(httpRequest, channelHandlerContext);
      return;
    }

    final Channel channel = messageEvent.getChannel();
    synchronized (texaiHTTPRequestHandlers) {
      for (final TexaiHTTPRequestHandler texaiHTTPRequestHandler : texaiHTTPRequestHandlers) {
        final boolean isHandled = texaiHTTPRequestHandler.httpRequestReceived(httpRequest, channel);
        if (isHandled) {
          LOGGER.info(texaiHTTPRequestHandler.getClass().getName() + " handled the request");
          return;
        }
      }
    }
    LOGGER.info("no handler for the request: " + httpRequest);
    throw new TexaiException("no handler for the request: " + httpRequest);
  }

  /** Registers the given Texai HTTP request handler.
   *
   * @param texaiHTTPRequestHandler the given Texai HTTP request handler
   */
  public void register(final TexaiHTTPRequestHandler texaiHTTPRequestHandler) {
    //Preconditions
    assert texaiHTTPRequestHandler != null : "texaiHTTPRequestHandler must not be null";

    synchronized (texaiHTTPRequestHandlers) {
      texaiHTTPRequestHandlers.add(texaiHTTPRequestHandler);
    }
  }

  /** Deregisters the given Texai HTTP request handler.
   *
   * @param texaiHTTPRequestHandler the given Texai HTTP request handler
   */
  public void deregister(final TexaiHTTPRequestHandler texaiHTTPRequestHandler) {
    //Preconditions
    assert texaiHTTPRequestHandler != null : "texaiHTTPRequestHandler must not be null";

    synchronized (texaiHTTPRequestHandlers) {
      texaiHTTPRequestHandlers.remove(texaiHTTPRequestHandler);
    }
  }

  /** Handles an exception that was raised by an I/O thread or a {@link ChannelHandler}.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    LOGGER.error(StringUtils.getStackTraceAsString(exceptionEvent.getCause()));
    exceptionEvent.getChannel().close();
  }

  /** Dynamically switches the channel pipeline to handle the web socket protocol.
   *
   * @param httpRequest the HTTP request
   * @param channelHandlerContext the channel handler context
   */
  private void switchToWebSocket(
          final HttpRequest httpRequest,
          final ChannelHandlerContext channelHandlerContext) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";

    final ChannelPipeline channelPipeline = channelHandlerContext.getPipeline();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("switching to web socket channel pipeline from: " + channelPipeline);
    }
    final Collection<ChannelHandler> channelHandlers = channelPipeline.toMap().values();
    for (final ChannelHandler channelHandler : channelHandlers) {
      if (!(channelHandler instanceof SslHandler
              || channelHandler instanceof PortUnificationHandler)) {
        channelPipeline.remove(channelHandler);
      }
    }
    final WebSocketSslServerHandler webSocketSslServerHandler = new WebSocketSslServerHandler(this);
    channelPipeline.addLast("encoder", new HttpResponseEncoder());
    channelPipeline.addLast("decoder", new HttpRequestDecoder());
    channelPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    channelPipeline.addLast("ws-request-handler", webSocketSslServerHandler);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("web socket channel pipeline: " + channelPipeline);
    }

    webSocketSslServerHandler.handshake(httpRequest, channelHandlerContext);
  }

  /** Gets the Texai HTTP request handler chain. Caller should synchronized on this.
   *
   * @return the Texai HTTP request handler chain
   */
  public List<TexaiHTTPRequestHandler> getTexaiHTTPRequestHandlers() {
    return texaiHTTPRequestHandlers;
  }
}
