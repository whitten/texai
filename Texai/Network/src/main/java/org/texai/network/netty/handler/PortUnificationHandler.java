/*
 * Copyright 2010 Red Hat, Inc.
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
package org.texai.network.netty.handler;

import java.util.Collection;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.texai.network.netty.NetworkConstants;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Manipulates the current pipeline dynamically to switch protocols that share the single port. Web socket protocol
 * switching is performed within the HTTP request handler.
 *
 * @author reed
 *
 */
public class PortUnificationHandler extends FrameDecoder {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PortUnificationHandler.class);
  // dependency injection of the business logic handlers enables the substitution of stubs for unit testing this class
  /** the Albus hierarchical control network channel handler */
  private AbstractAlbusHCSMessageHandler albusHCNMessageHandler;
  /** the HTTP request handler */
  private AbstractHTTPRequestHandler httpRequestHandler;

  /** Constructs a new PortUnificationHandler instance. */
  public PortUnificationHandler() {
  }

  /** Recognizes the protocol and switches the pipeline to handle it.
   *
   * @param channelHandlerContext the context of this handler
   * @param channel  the current channel
   * @param channelBuffer the cumulative buffer of received packets so far.
   *
   * @return the channel buffer after reading a possible object serialization protocol identification byte.
   *         {@code null} if there's not enough data in the buffer to recognize the protocol.
   */
  @Override
  protected Object decode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final ChannelBuffer channelBuffer) {

    final int readableBytes = channelBuffer.readableBytes();
    LOGGER.info("readable bytes: " + readableBytes);

    // use the first five bytes of the channel buffer to detect the protocol
    if (readableBytes < 5) {
      return null;
    }

    final int magic1 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex());
    final int magic2 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex() + 1);

    if (isSerializedObject(magic1)) {
      switchToAlbusHCN(channelHandlerContext);
    } else if (isHttp(magic1, magic2)) {
      switchToHttp(channelHandlerContext);
    } else {
      LOGGER.info("unknown protocol");
      // unknown protocol; discard everything and close the connection
      channelBuffer.skipBytes(channelBuffer.readableBytes());
      channelHandlerContext.getChannel().close();
      return null;
    }

    // forward the current read buffer as-is to the new handlers
    return channelBuffer.readBytes(channelBuffer.readableBytes());
  }

  /** Returns whether this is a serialized object, e.g. a Texai node-to-node message.
   *
   * @param magic1 the first byte of the message
   * @return whether this is a serialized object
   */
  private boolean isSerializedObject(final int magic1) {
    return magic1 == NetworkConstants.OBJECT_SERIALIZATION_PROTOCOL;
  }

  /** Returns whether this is an HTTP message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @return whether this is an HTTP message
   */
  private boolean isHttp(final int magic1, final int magic2) {
    return magic1 == 'G' && magic2 == 'E' || // GET
            magic1 == 'P' && magic2 == 'O' || // POST
            magic1 == 'P' && magic2 == 'U' || // PUT
            magic1 == 'H' && magic2 == 'E' || // HEAD
            magic1 == 'O' && magic2 == 'P' || // OPTIONS
            magic1 == 'P' && magic2 == 'A' || // PATCH
            magic1 == 'D' && magic2 == 'E' || // DELETE
            magic1 == 'T' && magic2 == 'R' || // TRACE
            magic1 == 'C' && magic2 == 'O';   // CONNECT
    }

  /** Dynamically switches the channel pipeline to handle an HTTP message.
   *
   * @param channelHandlerContext the channel handler context
   */
  private void switchToHttp(final ChannelHandlerContext channelHandlerContext) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";

    final ChannelPipeline channelPipeline = channelHandlerContext.getPipeline();
    LOGGER.info("switching to HTTP channel pipeline from: " + channelPipeline);
    final Collection<ChannelHandler> channelHandlers = channelPipeline.toMap().values();
    for (final ChannelHandler channelHandler : channelHandlers) {
      if (!(channelHandler instanceof SslHandler ||
              channelHandler instanceof PortUnificationHandler)) {
        channelPipeline.remove(channelHandler);
      }
    }
    channelPipeline.addLast("encoder", new HttpResponseEncoder());
    channelPipeline.addLast("decoder", new HttpRequestDecoder());
    channelPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    channelPipeline.addLast("http-request-handler", httpRequestHandler);
    channelPipeline.remove(this);
    LOGGER.info("HTTP channel pipeline: " + channelPipeline);
  }

  /** Dynamically switches the channel pipeline to handle a serialized object message sent from one node to another
   * in an Albus hierarchical control network.
   *
   * @param channelHandlerContext the channel handler context
   */
  private void switchToAlbusHCN(final ChannelHandlerContext channelHandlerContext) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert albusHCNMessageHandler != null : "albusHCNMessageHandler must not be null";

    final ChannelPipeline channelPipeline = channelHandlerContext.getPipeline();
    LOGGER.info("switching to Albus HCN channel pipeline from: " + channelPipeline);
    final Collection<ChannelHandler> channelHandlers = channelPipeline.toMap().values();
    for (final ChannelHandler channelHandler : channelHandlers) {
      if (!(channelHandler instanceof SslHandler ||
              channelHandler instanceof PortUnificationHandler)) {
        channelPipeline.remove(channelHandler);
      }
    }

    channelPipeline.addLast("decoder", new TaggedObjectDecoder());
    channelPipeline.addLast("encoder", new TaggedObjectEncoder());
    channelPipeline.addLast("albus-handler", albusHCNMessageHandler);
    channelPipeline.remove(this);
    LOGGER.info("Albus HCN pipeline: " + channelPipeline.toString());
  }

  /** Handles a caught exception.
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

    final Throwable throwable = exceptionEvent.getCause();
    switch (throwable.getMessage()) {
      case "Received close_notify during handshake":
        LOGGER.info("ignoring warning: " + throwable.getMessage());
        break;

      case "Connection reset by peer":
        LOGGER.info("connection reset by client web browser");
        break;

      default:
        LOGGER.error(StringUtils.getStackTraceAsString(throwable));
        LOGGER.error("exception: " + throwable.getMessage());
        throw new TexaiException(throwable);
    }
  }

  /** Sets the Albus hierarchical control network channel handler.
   *
   * @param albusHCNMessageHandler the Albus hierarchical control network channel handler
   */
  public void setAlbusHCNMessageHandler(final AbstractAlbusHCSMessageHandler albusHCNMessageHandler) {
    //Preconditions
    assert albusHCNMessageHandler != null : "albusHCNMessageHandler must not be null";

    this.albusHCNMessageHandler = albusHCNMessageHandler;
  }

  /** Sets the HTTP request handler.
   *
   * @param httpRequestHandler the HTTP request handler
   */
  public void setHttpRequestHandler(final AbstractHTTPRequestHandler httpRequestHandler) {
    //Preconditions
    assert httpRequestHandler != null : "httpRequestHandler must not be null";

    this.httpRequestHandler = httpRequestHandler;
  }
}
