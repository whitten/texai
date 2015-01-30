/*
 * TaggedObjectDecoder.java
 *
 * Created on Feb 10, 2010, 4:43:37 PM
 *
 * Description: Provides a tagged object decoder.
 *
 * Copyright (C) Feb 10, 2010 reed.
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

import java.io.IOException;
import java.io.StreamCorruptedException;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.texai.network.netty.NetworkConstants;

/**
 * Provides a tagged object decoder. A tagged object is a serialized object with a protocol identification byte prepended.
 *
 * @author reed
 */
@NotThreadSafe
public class TaggedObjectDecoder extends FrameDecoder {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(TaggedObjectDecoder.class);
  /**
   * the maximum object size
   */
  private final int maxObjectSize;

  /**
   * Creates a new TaggedObjectDecoder having a maximum object size. If the size of the received object is greater than the maximum bytes, a
   * StreamCorruptedException will be raised.
   */
  public TaggedObjectDecoder() {
    this(1048576);
  }

  /**
   * Creates a new decoder with the specified maximum object size.
   *
   * @param maxObjectSize the maximum byte length of the serialized object. if the length of the received object is greater than this value,
   * {@link StreamCorruptedException} will be raised.
   */
  public TaggedObjectDecoder(final int maxObjectSize) {
    //Preconditions
    if (maxObjectSize <= 0) {
      throw new IllegalArgumentException("maxObjectSize: " + maxObjectSize);
    }

    this.maxObjectSize = maxObjectSize;
  }

  /**
   * Decodes the received packets so far into an object.
   *
   * @param channelHandlerContext the context of this handler
   * @param channel the current channel
   * @param channelBuffer the cumulative buffer of received packets so far. Note that the buffer might be empty, which means you should not
   * make an assumption that the buffer contains at least one byte in your decoder implementation.
   *
   * byte 0     ... 1 (object serialization protocol for port unification)
   * bytes 1-5  ... 32 bit integer length, the maximum is a bit more than 8K
   *
   * @return the object if all its bytes were contained in the buffer. null if there's not enough data in the buffer to decode an object.
   * @throws IOException if an input/output error occurs
   * @throws ClassNotFoundException if the serialized object's class cannot be found
   */
  @Override
  protected Object decode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final ChannelBuffer channelBuffer) throws IOException, ClassNotFoundException {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert channelBuffer != null : "channelBuffer must not be null";

    if (channelBuffer.readableBytes() < 5) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("not enough bytes received in the buffer to decode");
      }
      return null;
    }
    final byte protocolByte = channelBuffer.readByte();
    assert protocolByte == NetworkConstants.OBJECT_SERIALIZATION_PROTOCOL : "protocolByte: " + protocolByte;

    final int dataLen = channelBuffer.getInt(channelBuffer.readerIndex());
    if (dataLen <= 0) {
      throw new StreamCorruptedException("invalid data length: " + dataLen);
    }
    if (dataLen > maxObjectSize) {
      throw new StreamCorruptedException(
              "data length too big: " + dataLen + " (max: " + maxObjectSize + ')');
    }

    if (channelBuffer.readableBytes() < dataLen + 4) {
      return null;
    }

    // skip over the data length
    channelBuffer.skipBytes(4);
    // return the deserialized object
    return new CompactObjectInputStream(new ChannelBufferInputStream(channelBuffer, dataLen)).readObject();
  }
}
