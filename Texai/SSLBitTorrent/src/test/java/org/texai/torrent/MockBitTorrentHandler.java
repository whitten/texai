/*
 * MockBitTorrentHandler.java
 *
 * Created on Feb 3, 2010, 11:51:24 PM
 *
 * Description: A channel handler for a bit torrent bitfield message.
 *
 * Copyright (C) Feb 3, 2010 reed.
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
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.network.netty.handler.AbstractBitTorrentHandler;
import org.texai.torrent.message.BitTorrentBitFieldMessage;
import org.texai.torrent.message.BitTorrentCancelMessage;
import org.texai.torrent.message.BitTorrentChokeMessage;
import org.texai.torrent.message.BitTorrentHandshakeMessage;
import org.texai.torrent.message.BitTorrentHaveMessage;
import org.texai.torrent.message.BitTorrentInterestedMessage;
import org.texai.torrent.message.BitTorrentKeepAliveMessage;
import org.texai.torrent.message.BitTorrentMessage;
import org.texai.torrent.message.BitTorrentNotInterestedMessage;
import org.texai.torrent.message.BitTorrentPieceMessage;
import org.texai.torrent.message.BitTorrentRequestMessage;
import org.texai.torrent.message.BitTorrentUnchokeMessage;
import org.texai.util.TexaiException;

/** A channel handler for a bit torrent bitfield message.
 *
 * @author reed
 */
@NotThreadSafe
public final class MockBitTorrentHandler extends AbstractBitTorrentHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MockBitTorrentHandler.class);
  /** the lock that allows the client to resume when the messaging is done */
  final Object clientResume_lock;
  /** the test iteration limit */
  final int iterationLimit;
  /** the peer id bytes */
  final byte[] peerIdBytes;

  /** Constructs a new MockBitTorrentHandler instance.
   *
   * @param peerIdBytes the peer id bytes
   * @param clientResume_lock the lock that allows the client to resume when the messaging is done, or
   * null if this is the client side handler
   * @param iterationLimit the test iteration limit
   */
  public MockBitTorrentHandler(
          final byte[] peerIdBytes,
          final Object clientResume_lock,
          final int iterationLimit) {
    //Preconditions
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert iterationLimit >= 0 : "iterationLimit must not be negative";

    this.peerIdBytes = peerIdBytes;
    this.clientResume_lock = clientResume_lock;
    this.iterationLimit = iterationLimit;
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
    assert messageEvent.getMessage() instanceof BitTorrentMessage;

    final Channel channel = channelHandlerContext.getChannel();
    final BitTorrentMessage bitTorrentMessage = (BitTorrentMessage) messageEvent.getMessage();

    final boolean isClient = clientResume_lock != null;
    if (isClient) {
      LOGGER.info("client received messageEvent: " + messageEvent);
      if (bitTorrentMessage instanceof BitTorrentHandshakeMessage) {
        final BitTorrentHandshakeMessage bitTorrentHandshakeMessage = (BitTorrentHandshakeMessage) bitTorrentMessage;
        LOGGER.info("handling server's handshake: " + bitTorrentHandshakeMessage);
        final byte[] bitField = {0x01, 0x02};
        final BitTorrentBitFieldMessage bitTorrentBitfieldMessage = new BitTorrentBitFieldMessage(bitField, null);
        LOGGER.info("writing client's bitfield: " + bitTorrentBitfieldMessage);
        channel.write(bitTorrentBitfieldMessage);

      } else if (bitTorrentMessage instanceof BitTorrentUnchokeMessage) {
        LOGGER.info("handling server's unchoke: " + bitTorrentMessage);
        synchronized (clientResume_lock) {
          // notify the waiting client thread that processing is done
          clientResume_lock.notifyAll();
        }
      } else {
        assert false;
      }

    } else {
      LOGGER.info("server received messageEvent: " + messageEvent);
      if (bitTorrentMessage instanceof BitTorrentHandshakeMessage) {
        final BitTorrentHandshakeMessage bitTorrentHandshakeMessage = (BitTorrentHandshakeMessage) bitTorrentMessage;
        LOGGER.info("handling client's handshake: " + bitTorrentHandshakeMessage);
        final BitTorrentHandshakeMessage replyBitTorrentHandshakeMessage = new BitTorrentHandshakeMessage(
                bitTorrentHandshakeMessage.getInfoHash(),
                peerIdBytes);
        LOGGER.info("writing server's handshake: " + replyBitTorrentHandshakeMessage);
        channel.write(replyBitTorrentHandshakeMessage);

      } else if (bitTorrentMessage instanceof BitTorrentBitFieldMessage) {
        final BitTorrentBitFieldMessage bitTorrentBitfieldMessage = (BitTorrentBitFieldMessage) bitTorrentMessage;
        LOGGER.info("handling bitfield: " + bitTorrentBitfieldMessage);
        final BitTorrentUnchokeMessage bitTorrentUnchokeMessage = new BitTorrentUnchokeMessage(null);
        LOGGER.info("writing server's unchoke: " + bitTorrentUnchokeMessage);
        channel.write(bitTorrentUnchokeMessage);

      } else if (bitTorrentMessage instanceof BitTorrentCancelMessage) {
        final BitTorrentCancelMessage bitTorrentCancelMessage = (BitTorrentCancelMessage) bitTorrentMessage;
        LOGGER.info("handling cancel: " + bitTorrentCancelMessage);

      } else if (bitTorrentMessage instanceof BitTorrentChokeMessage) {
        final BitTorrentChokeMessage bitTorrentChokeMessage = (BitTorrentChokeMessage) bitTorrentMessage;
        LOGGER.info("handling choke: " + bitTorrentChokeMessage);

      } else if (bitTorrentMessage instanceof BitTorrentHaveMessage) {
        final BitTorrentHaveMessage bitTorrentHaveMessage = (BitTorrentHaveMessage) bitTorrentMessage;
        LOGGER.info("handling have: " + bitTorrentHaveMessage);

      } else if (bitTorrentMessage instanceof BitTorrentInterestedMessage) {
        final BitTorrentInterestedMessage bitTorrentInterestedMessage = (BitTorrentInterestedMessage) bitTorrentMessage;
        LOGGER.info("handling interested: " + bitTorrentInterestedMessage);

      } else if (bitTorrentMessage instanceof BitTorrentKeepAliveMessage) {
        final BitTorrentKeepAliveMessage bitTorrentKeepAliveMessage = (BitTorrentKeepAliveMessage) bitTorrentMessage;
        LOGGER.info("handling keep-alive: " + bitTorrentKeepAliveMessage);

      } else if (bitTorrentMessage instanceof BitTorrentNotInterestedMessage) {
        final BitTorrentNotInterestedMessage bitTorrentNotInterestedMessage = (BitTorrentNotInterestedMessage) bitTorrentMessage;
        LOGGER.info("handling not-interested: " + bitTorrentNotInterestedMessage);

      } else if (bitTorrentMessage instanceof BitTorrentPieceMessage) {
        final BitTorrentPieceMessage bitTorrentPieceMessage = (BitTorrentPieceMessage) bitTorrentMessage;
        LOGGER.info("handling piece: " + bitTorrentPieceMessage);

      } else if (bitTorrentMessage instanceof BitTorrentRequestMessage) {
        final BitTorrentRequestMessage bitTorrentRequestMessage = (BitTorrentRequestMessage) bitTorrentMessage;
        LOGGER.info("handling request: " + bitTorrentRequestMessage);

      } else if (bitTorrentMessage instanceof BitTorrentUnchokeMessage) {
        final BitTorrentUnchokeMessage bitTorrentUnchokeMessage = (BitTorrentUnchokeMessage) bitTorrentMessage;
        LOGGER.info("handling unchoke: " + bitTorrentUnchokeMessage);

      } else {
        assert false;
      }
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
