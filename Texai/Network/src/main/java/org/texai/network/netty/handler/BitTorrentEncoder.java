/*
 * BitTorrentEncoder.java
 *
 * Created on Feb 11, 2010, 3:47:24 PM
 *
 * Description: Provides a bit torrent encoder.
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

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.texai.torrent.message.BitTorrentMessage;
import org.texai.util.ByteUtils;

/** Provides a bit torrent encoder.
 *
 * @author reed
 */
@NotThreadSafe
@Sharable
public class BitTorrentEncoder extends OneToOneEncoder {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentEncoder.class);

  /** Constructs a new BitTorrentEncoder instance. */
  public BitTorrentEncoder() {
  }

  /** Transforms the specified bit torrent message into encoded bytes.
   *
   * @param channelHandlerContext the channel handler context
   * @param channel the channel
   * @param obj the bit torrent message
   * @return the channel buffer containing the encoded bit torrent message
   */
  @Override
  protected Object encode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final Object obj) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert obj != null : "obj must not be null";
    assert obj instanceof BitTorrentMessage : "bitTorrentMessage must be an instance of BitTorrentMessage";

    final BitTorrentMessage bitTorrentMessage = (BitTorrentMessage) obj;
    final byte[] bytes = bitTorrentMessage.encode();
    LOGGER.info("message: " + bitTorrentMessage + ", bytes length: " + bytes.length + ", " + ByteUtils.toHex(bytes));
    return ChannelBuffers.wrappedBuffer(bytes);
  }
}
