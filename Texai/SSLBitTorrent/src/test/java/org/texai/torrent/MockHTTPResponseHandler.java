/*
 * MockHTTPResponseHandler.java
 *
 * Created on Feb 4, 2010, 9:30:41 AM
 *
 * Description: .
 *
 * Copyright (C) Feb 4, 2010 reed.
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
package org.texai.torrent;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@NotThreadSafe
public final class MockHTTPResponseHandler extends AbstractHTTPResponseHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MockHTTPResponseHandler.class);
  /** the lock that allows the client to resume when the messaging is done */
  final Object clientResume_lock;

  /** Constructs a new MockHTTPResponseHandler instance.
   *
   * @param clientResume_lock the lock that allows the client to resume when the messaging is done, or
   * null if this is the client side handler
   */
  public MockHTTPResponseHandler(final Object clientResume_lock) {
    this.clientResume_lock = clientResume_lock;
  }

  /** Receives a message object from a remote peer.
   *
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

    LOGGER.info("client received messageEvent: " + messageEvent);
    final HttpResponse httpResponse = (HttpResponse) messageEvent.getMessage();

    LOGGER.info("STATUS: " + httpResponse.getStatus());
    LOGGER.info("VERSION: " + httpResponse.getProtocolVersion());
    LOGGER.info("");

    if (!httpResponse.getHeaderNames().isEmpty()) {
      for (final String name : httpResponse.getHeaderNames()) {
        for (final String value : httpResponse.getHeaders(name)) {
          LOGGER.info("HEADER: " + name + " = " + value);
        }
      }
      LOGGER.info("");
    }

    final ChannelBuffer content = httpResponse.getContent();
    if (content.readable()) {
      LOGGER.info("CONTENT {");
      LOGGER.info(content.toString(CharsetUtil.UTF_8));
      LOGGER.info("} END OF CONTENT");
    }
    // signal the client thread to finish
    synchronized (clientResume_lock) {
      clientResume_lock.notifyAll();
    }
  }

  /** Handles a caught exception.
   *
   * @param channelHandlerContext the channel handler event
   * @param exceptionEvent the exception event
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    LOGGER.error("exceptionEvent: " + exceptionEvent);
    throw new TexaiException(exceptionEvent.getCause());
  }
}
