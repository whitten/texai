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
package org.texai.network.netty.handler;

import java.io.IOException;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;

import java.io.ObjectOutputStream;

import java.io.Serializable;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.texai.network.netty.NetworkConstants;

/** An encoder which prepends a protocol identification byte and
 * serializes a Java object into a {@link ChannelBuffer}.
 * <p>
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * reformatted and commented by Stephen L. Reed, Texai.org, stephenreed@yahoo.com
 */
@Sharable
public class TaggedObjectEncoder extends OneToOneEncoder {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TaggedObjectEncoder.class);
  /** the length placeholder bytes */
  private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
  /** the estimated length of the encoded output */
  private final int estimatedLength;

  /** Creates a new encoder with the estimated length of 512 bytes. */
  public TaggedObjectEncoder() {
    this(512);
  }

  /** Creates a new encoder.
   *
   * @param estimatedLength
   *        the estimated byte length of the serialized form of an object.
   *        If the length of the serialized form exceeds this value, the
   *        internal buffer will be expanded automatically at the cost of
   *        memory bandwidth.  If this value is too big, it will also waste
   *        memory bandwidth.  To avoid unnecessary memory copy or allocation
   *        cost, please specify the properly estimated value.
   */
  public TaggedObjectEncoder(final int estimatedLength) {
    //Preconditions
    if (estimatedLength < 0) {
      throw new IllegalArgumentException("estimatedLength: " + estimatedLength);
    }

    this.estimatedLength = estimatedLength;
  }

  /** Encodes the given object into the channel buffer.  A protocol identification byte is prepended.
   *
   * @param channelHandlerContext the channel handler context
   * @param channel the channel
   * @param obj the serializable object to encode
   * @return a channel buffer containing the protocol byte and encoded object.
   * @throws IOException when an input/output error occurs
   */
  @Override
  protected Object encode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final Object obj) throws IOException {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert obj != null : "obj must not be null";
    assert obj instanceof Serializable : "obj must be serializable";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("encoding: " + obj);
    }
    final ChannelBufferOutputStream channelBufferOutputStream = new ChannelBufferOutputStream(dynamicBuffer(
            estimatedLength,
            channelHandlerContext.getChannel().getConfig().getBufferFactory()));
    channelBufferOutputStream.write(NetworkConstants.OBJECT_SERIALIZATION_PROTOCOL);
    channelBufferOutputStream.write(LENGTH_PLACEHOLDER);
    try (ObjectOutputStream objectOutputStream = new CompactObjectOutputStream(channelBufferOutputStream)) {
      objectOutputStream.writeObject(obj);
      objectOutputStream.flush();
    }

    final ChannelBuffer encoded = channelBufferOutputStream.buffer();
    encoded.setInt(1, encoded.writerIndex() - 5);
    return encoded;
  }
}
