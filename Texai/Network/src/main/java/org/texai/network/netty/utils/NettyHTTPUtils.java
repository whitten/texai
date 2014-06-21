/*
 * NettyHTTPUtils.java
 *
 * Created on Mar 5, 2010, 12:12:19 PM
 *
 * Description: Netty-related HTTP utilities.
 *
 * Copyright (C) Mar 5, 2010 reed.
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
package org.texai.network.netty.utils;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

/** Netty-related HTTP utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class NettyHTTPUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NettyHTTPUtils.class);

  /** Prevents the instantiation of this utility class. */
  private NettyHTTPUtils() {
  }

  /** Returns the Texai session cookie from the given HTTP request, or null if not found.
   *
   * @param httpRequest the given HTTP request
   * @return the Texai session cookie or null if not found
   */
  public static String getTexaiSessionCookie(final HttpRequest httpRequest) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";

    final List<String> cookieHeaders = httpRequest.getHeaders("Cookie");
    for (final String cookieHeader : cookieHeaders) {
      // texai-session=9ec68e1d-8fd2-4ade-8c06-651cc7ee4c7c
      if (cookieHeader.startsWith("texai-session=")) {
        return cookieHeader.substring(14);
      }
    }
    return null;
  }

  /** Writes a HTML text response.
   *
   * @param httpRequest the HTTP request
   * @param responseContent the response content
   * @param channel the channel
   */
  public static void writeHTMLResponse(
          final HttpRequest httpRequest,
          final StringBuilder responseContent,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert responseContent != null : "responseContent must not be null";
    assert responseContent.length() > 0 : "responseContent must not be empty";
    assert channel != null : "channel must not be null";

    writeHTMLResponse(
            httpRequest,
            responseContent.toString(),
            channel,
            null); // sessionCookie
  }


  /** Writes a HTML response.
   *
   * @param httpRequest the HTTP request
   * @param responseContent the response content
   * @param channel the channel
   * @param sessionCookie the session cookie
   */
  public static void writeHTMLResponse(
          final HttpRequest httpRequest,
          final String responseContent,
          final Channel channel,
          final String sessionCookie) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert responseContent != null : "responseContent must not be null";
    assert !responseContent.isEmpty() : "responseContent must not be empty";
    assert channel != null : "channel must not be null";

    // convert the response content to a channel buffer
    final ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(responseContent, CharsetUtil.UTF_8);

    // decide whether to close the connection or not
    final boolean isConnectionToBeClosed =
            HttpHeaders.Values.CLOSE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) ||
            httpRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION));

    // build the response object
    final HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    httpResponse.setContent(channelBuffer);
    if (sessionCookie != null) {
      httpResponse.setHeader(HttpHeaders.Names.SET_COOKIE, "texai-session=" + sessionCookie);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("writeHTMLResponse, sessionCookie=" + sessionCookie);
    }
    setAccessControl(httpRequest, httpResponse);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));

    // write the response
    final ChannelFuture channelFuture = channel.write(httpResponse);

    // close the connection after the write operation is done if necessary
    if (isConnectionToBeClosed) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Writes a text response.
   *
   * @param httpRequest the HTTP request
   * @param responseContent the response content
   * @param channel the channel
   */
  public static void writeTextResponse(
          final HttpRequest httpRequest,
          final String responseContent,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert responseContent != null : "responseContent must not be null";
    assert !responseContent.isEmpty() : "responseContent must not be empty";
    assert channel != null : "channel must not be null";

    // convert the response content to a channel buffer
    final ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(responseContent, CharsetUtil.UTF_8);

    // decide whether to close the connection or not
    final boolean isConnectionToBeClosed =
            HttpHeaders.Values.CLOSE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) ||
            httpRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION));

    // build the response object
    final HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    httpResponse.setContent(channelBuffer);
    setAccessControl(httpRequest, httpResponse);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));

    // write the response
    final ChannelFuture channelFuture = channel.write(httpResponse);

    // close the connection after the write operation is done if necessary
    if (isConnectionToBeClosed) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Writes a CSS response.
   *
   * @param httpRequest the HTTP request
   * @param responseContent the response content
   * @param channel the channel
   */
  public static void writeCSSResponse(
          final HttpRequest httpRequest,
          final String responseContent,
          final Channel channel) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert responseContent != null : "responseContent must not be null";
    assert !responseContent.isEmpty() : "responseContent must not be empty";
    assert channel != null : "channel must not be null";

    // convert the response content to a channel buffer
    final ChannelBuffer channelBuffer = ChannelBuffers.copiedBuffer(responseContent, CharsetUtil.UTF_8);

    // decide whether to close the connection or not
    final boolean isConnectionToBeClosed =
            HttpHeaders.Values.CLOSE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) ||
            httpRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION));

    // build the response object
    final HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    httpResponse.setContent(channelBuffer);
    setAccessControl(httpRequest, httpResponse);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/css; charset=UTF-8");
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));

    // write the response
    final ChannelFuture channelFuture = channel.write(httpResponse);

    // close the connection after the write operation is done if necessary
    if (isConnectionToBeClosed) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Writes a binary response.
   *
   * @param httpRequest the HTTP request
   * @param responseContentBytes the response content bytes
   * @param channel the channel
   * @param sessionCookie the session cookie
   */
  public static void writeBinaryResponse(
          final HttpRequest httpRequest,
          final byte[] responseContentBytes,
          final Channel channel,
          final String sessionCookie) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert responseContentBytes != null : "responseContentBytes must not be null";
    assert responseContentBytes.length > 0 : "responseContentBytes must not be empty";
    assert channel != null : "channel must not be null";

    // convert the response content to a channel buffer
    final ChannelBuffer channelBuffer = ChannelBuffers.wrappedBuffer(responseContentBytes);

    // decide whether to close the connection or not
    final boolean isConnectionToBeClosed =
            HttpHeaders.Values.CLOSE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) ||
            httpRequest.getProtocolVersion().equals(HttpVersion.HTTP_1_0) &&
            !HttpHeaders.Values.KEEP_ALIVE.equalsIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION));

    // build the response object
    final HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    httpResponse.setContent(channelBuffer);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/octet-stream");
    if (sessionCookie != null) {
      httpResponse.setHeader(HttpHeaders.Names.SET_COOKIE, "texai-session=" + sessionCookie);
    }
    setAccessControl(httpRequest, httpResponse);
    httpResponse.setChunked(false);
    httpResponse.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(channelBuffer.readableBytes()));
    // when debugging multi-threading issues, it is sometimes useful to echo the requesting user-agent
    //httpResponse.setHeader(HttpHeaders.Names.USER_AGENT, httpRequest.getHeader(HttpHeaders.Names.USER_AGENT));

    // write the response
    final ChannelFuture channelFuture = channel.write(httpResponse);

    // close the connection after the write operation is done if necessary
    if (isConnectionToBeClosed) {
      channelFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /** Set access control headers on the HTTP response.
   *
   * @param httpRequest the HTTP request
   * @param httpResponse the HTTP response
   */
  private static void setAccessControl(
          final HttpRequest httpRequest,
          final HttpResponse httpResponse) {
    //Preconditions
    assert httpRequest != null : "httpRequest must not be null";
    assert httpResponse != null : "httpResponse must not be null";

    String origin = httpRequest.getHeader(HttpHeaders.Names.ORIGIN);
    if (origin == null) {
      origin = "*";
    }
    httpResponse.setHeader("Access-Control-Allow-Origin", origin);
    httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
  }

}
