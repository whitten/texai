/*
 * BitTorrentDecoder.java
 *
 * Created on Feb 11, 2010, 1:11:10 PM
 *
 * Description: Provides a bit torrent message decoder.
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
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.jboss.netty.handler.codec.replay.VoidEnum;
import org.texai.torrent.message.BitTorrentBitFieldMessage;
import org.texai.torrent.message.BitTorrentCancelMessage;
import org.texai.torrent.message.BitTorrentChokeMessage;
import org.texai.torrent.message.BitTorrentHandshakeMessage;
import org.texai.torrent.message.BitTorrentHaveMessage;
import org.texai.torrent.message.BitTorrentInterestedMessage;
import org.texai.torrent.message.BitTorrentKeepAliveMessage;
import org.texai.torrent.message.BitTorrentNotInterestedMessage;
import org.texai.torrent.message.BitTorrentPieceMessage;
import org.texai.torrent.message.BitTorrentRequestMessage;
import org.texai.torrent.message.BitTorrentUnchokeMessage;
import org.texai.torrent.support.BitTorrentUtils;
import org.texai.util.ByteUtils;

/** Provides a bit torrent message decoder.
 *
 * @author reed
 */
@NotThreadSafe
public final class BitTorrentDecoder extends ReplayingDecoder<VoidEnum> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentDecoder.class);

  /** Constructs a new BitTorrentDecoder instance. */
  public BitTorrentDecoder() {
  }

  /** Decodes the received packets so far into a bit torrent message.
   *
   * @param channelHandlerContext      the context of this handler
   * @param channel  the current channel
   * @param channelBuffer   the cumulative buffer of received packets so far.
   *                 Note that the buffer might be empty, which means you
   *                 should not make an assumption that the buffer contains
   *                 at least one byte in your decoder implementation.
   * @param state    the current decoder state ({@code null} if unused)
   *
   * @return the bit torrent message
   */
  @Override
  protected Object decode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final ChannelBuffer channelBuffer,
          final VoidEnum state) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert channelBuffer != null : "channelBuffer must not be null";

    final int magic1 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex());
    final int magic2 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex() + 1);
    final int magic3 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex() + 2);
    final int magic4 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex() + 3);
    final int magic5 = channelBuffer.getUnsignedByte(channelBuffer.readerIndex() + 4);

    if (BitTorrentUtils.isBitTorrentCancel(magic1, magic2, magic3, magic4, magic5)) {
      // cancel
      final byte[] messageBytes = new byte[37];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentCancelMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentChoke(magic1, magic2, magic3, magic4, magic5)) {
      // choke
      final byte[] messageBytes = new byte[25];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentChokeMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentHave(magic1, magic2, magic3, magic4, magic5)) {
      // have
      final byte[] messageBytes = new byte[29];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentHaveMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentInterested(magic1, magic2, magic3, magic4, magic5)) {
      // interested
      final byte[] messageBytes = new byte[25];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentInterestedMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentNotInterested(magic1, magic2, magic3, magic4, magic5)) {
      // not interested
      final byte[] messageBytes = new byte[25];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentNotInterestedMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentRequest(magic1, magic2, magic3, magic4, magic5)) {
      // request
      final byte[] messageBytes = new byte[37];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentRequestMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentUnchoke(magic1, magic2, magic3, magic4, magic5)) {
      // unchoke
      final byte[] messageBytes = new byte[25];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentUnchokeMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentKeepAlive(magic1, magic2, magic3, magic4)) {
      // keep-alive
      final byte[] messageBytes = new byte[24];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentKeepAliveMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentHandshake(magic1)) {
      // handshake
      final byte[] messageBytes = new byte[68];
      channelBuffer.readBytes(messageBytes);
      return BitTorrentHandshakeMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentPiece(magic5)) {
      // piece
      final byte[] messageLengthBytes = new byte[4];
      channelBuffer.readBytes(messageLengthBytes);
      final int messageLength = ByteUtils.byteArrayToInt(messageLengthBytes);
      // message bytes constist of the message length field followed by the message-length number of bytes
      final byte[] messageBytes = new byte[4 + messageLength];
      messageBytes[0] = messageLengthBytes[0];
      messageBytes[1] = messageLengthBytes[1];
      messageBytes[2] = messageLengthBytes[2];
      messageBytes[3] = messageLengthBytes[3];
      channelBuffer.readBytes(messageBytes, 4, messageLength);
      return BitTorrentPieceMessage.decode(messageBytes);

    } else if (BitTorrentUtils.isBitTorrentBitfield(magic5)) {
      // bitfield
      final byte[] messageLengthBytes = new byte[4];
      channelBuffer.readBytes(messageLengthBytes);
      final int messageLength = ByteUtils.byteArrayToInt(messageLengthBytes);
      // message bytes constist of the message length field followed by the message-length number of bytes
      final byte[] messageBytes = new byte[4 + messageLength];
      messageBytes[0] = messageLengthBytes[0];
      messageBytes[1] = messageLengthBytes[1];
      messageBytes[2] = messageLengthBytes[2];
      messageBytes[3] = messageLengthBytes[3];
      channelBuffer.readBytes(messageBytes, 4, messageLength);
      return BitTorrentBitFieldMessage.decode(messageBytes);

    } else {
      assert false : "first 5 bytes: " + magic1 + ", " + magic2 + ", " + magic3 + ", " + magic4 + ", " + magic5;
      return null;
    }
  }

    /** Handles disconnection from remote peer event.
   *
   * @param ctx the channel handler context
   * @param e the channel state event
   * @throws Exception if an exception occurs
   */
    @Override
    public void channelDisconnected(
            final ChannelHandlerContext ctx,
            final ChannelStateEvent e) throws Exception {
    }

    /** Handles closed channel event.
     *
   * @param ctx the channel handler context
   * @param e the channel state event
   * @throws Exception if an exception occurs
     */
    @Override
    public void channelClosed(
            final ChannelHandlerContext ctx,
            final ChannelStateEvent e) throws Exception {
    }
}
