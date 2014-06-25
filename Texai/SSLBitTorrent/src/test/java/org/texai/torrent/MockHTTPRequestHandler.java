/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.texai.torrent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;
import org.texai.network.netty.handler.AbstractHTTPRequestHandler;

/** A HTTPS request handler.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author Trustin Lee (tlee@redhat.com)
 *
 * @version $Rev: 1685 $, $Date: 2009-08-28 16:15:49 +0900 (금, 28 8 2009) $
 */
public final  class MockHTTPRequestHandler extends AbstractHTTPRequestHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MockHTTPRequestHandler.class);
  /** the HTTP request */
  private volatile HttpRequest httpRequest;
  /** the indicator that we are reading chunks */
  private volatile boolean isReadingChunks;
  /** the response content */
  private final StringBuilder responseContent = new StringBuilder();

  /** Constructs a new MockHTTPRequestHandler instance. */
  public MockHTTPRequestHandler() {
  }

  /** Handles the message object (e.g: {@link ChannelBuffer}) that was received from a remote peer.
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    if (isReadingChunks) {
      final HttpChunk httpChunk = (HttpChunk) messageEvent.getMessage();
      if (httpChunk.isLast()) {
        isReadingChunks = false;
        responseContent.append("END OF CONTENT\r\n");
        writeResponse(messageEvent);
      } else {
        responseContent.append("CHUNK: ").append(httpChunk.getContent().toString(CharsetUtil.UTF_8)).append("\r\n");
      }
    } else {
      httpRequest = (HttpRequest) messageEvent.getMessage();

      LOGGER.info("httpRequest: " + httpRequest);
      LOGGER.info("method: " + httpRequest.getMethod());
      LOGGER.info("protocol version: " + httpRequest.getProtocolVersion());
      LOGGER.info("method: " + httpRequest.getMethod());
      LOGGER.info("uri: " + httpRequest.getUri());
      for (final String headerName : httpRequest.getHeaderNames()) {
        LOGGER.info("header: " + headerName + " " + httpRequest.getHeader(headerName));
      }

      responseContent.append("WELCOME TO THE WILD WILD WEB SERVER\r\n");
      responseContent.append("===================================\r\n");

      responseContent.append("VERSION: ").append(httpRequest.getProtocolVersion().getText()).append("\r\n");

      if (httpRequest.containsHeader(HttpHeaders.Names.HOST)) {
        responseContent.append("HOSTNAME: ").append(httpRequest.getHeader(HttpHeaders.Names.HOST)).append("\r\n");
      }

      responseContent.append("REQUEST_URI: ").append(httpRequest.getUri()).append("\r\n\r\n");

      if (!httpRequest.getHeaderNames().isEmpty()) {
        for (final String headerName : httpRequest.getHeaderNames()) {
          for (final String headerValue : httpRequest.getHeaders(headerName)) {
            responseContent.append("HEADER: ").append(headerName).append(" = ").append(headerValue).append("\r\n");
          }
        }
        responseContent.append("\r\n");
      }

      final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.getUri());
      final Map<String, List<String>> parameterDictionary = queryStringDecoder.getParameters();
      if (!parameterDictionary.isEmpty()) {
        for (final Entry<String, List<String>> parameter : parameterDictionary.entrySet()) {
          final String key = parameter.getKey();
          final List<String> values = parameter.getValue();
          for (final String value : values) {
            responseContent.append("PARAM: ").append(key).append(" = ").append(value).append("\r\n");
          }
        }
        responseContent.append("\r\n");
      }

      if (httpRequest.isChunked()) {
        isReadingChunks = true;
      } else {
        final ChannelBuffer content = httpRequest.getContent();
        if (content.readable()) {
          responseContent.append("CONTENT: ").append(content.toString(CharsetUtil.UTF_8)).append("\r\n");
        }
        writeResponse(messageEvent);
      }
    }
  }

  /** Writes the response.
   *
   * @param messageEvent the message event
   */
  private void writeResponse(final MessageEvent messageEvent) {
    //Preconditions
    assert messageEvent != null : "messageEvent must not be null";

    // convert the response content to a channel buffer
    final ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(responseContent.toString(), CharsetUtil.UTF_8);
    responseContent.setLength(0);

    // decide whether to close the connection or not
    final boolean isConnectionToBeClosed =
            HttpHeaders.Values.CLOSE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) ||
            httpRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION));

    // build the response object
    final HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    httpResponse.setContent(channelBuffer);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

    if (!isConnectionToBeClosed) {
      // There's no need to add 'Content-Length' header
      // if this is the last response.
      httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));
    }

    final String cookieString = httpRequest.getHeader(HttpHeaders.Names.COOKIE);
    if (cookieString != null) {
      final CookieDecoder cookieDecoder = new CookieDecoder();
      final Set<Cookie> cookies = cookieDecoder.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        final CookieEncoder cookieEncoder = new CookieEncoder(true);
        for (final Cookie cookie : cookies) {
          cookieEncoder.addCookie(cookie);
        }
        httpResponse.addHeader(HttpHeaders.Names.SET_COOKIE, cookieEncoder.encode());
      }
    }

    // write the response
    final ChannelFuture channelFuture = messageEvent.getChannel().write(httpResponse);

    // lose the connection after the write operation is done if necessary
    if (isConnectionToBeClosed) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Handles an exception that was raised by an I/O thread or a {@link ChannelHandler}.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {
    exceptionEvent.getCause().printStackTrace();
    exceptionEvent.getChannel().close();
  }
}
