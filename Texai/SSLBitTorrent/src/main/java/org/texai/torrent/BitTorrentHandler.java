/*
 * BitTorrentHandler.java
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

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
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
import org.texai.util.ByteUtils;
import org.texai.util.TexaiException;

/** A channel handler for a bit torrent bitfield message.
 *
 * @author reed
 */
@NotThreadSafe
public final class BitTorrentHandler extends AbstractBitTorrentHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentHandler.class);
  /** the channel dictionary, channel --> peer coordinator */
  private final Map<Channel, PeerCoordinator> channelDictionary = new HashMap<>();
  /** the SSL torrent */
  private final SSLTorrent sslTorrent;

  /** Constructs a new BitTorrentHandler instance.
   *
   * @param sslTorrent the SSL torrent
   */
  public BitTorrentHandler(final SSLTorrent sslTorrent) {
    //Preconditions
    assert sslTorrent != null : "sslTorrent must not be null";

    this.sslTorrent = sslTorrent;
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

    final BitTorrentMessage bitTorrentMessage = (BitTorrentMessage) messageEvent.getMessage();
    LOGGER.info("server received messageEvent: " + messageEvent);
    final Channel channel = channelHandlerContext.getChannel();
    assert channel != null;

    if (bitTorrentMessage instanceof BitTorrentHandshakeMessage) {
      // handshake
      final BitTorrentHandshakeMessage bitTorrentHandshakeMessage = (BitTorrentHandshakeMessage) bitTorrentMessage;
      LOGGER.info("handling client's handshake: " + bitTorrentHandshakeMessage);
      final PeerCoordinator peerCoordinator = getPeerCoordinator(bitTorrentHandshakeMessage.getInfoHash(), channel);
      if (peerCoordinator != null) {
        channelDictionary.put(channel, peerCoordinator);
        final Peer peer = peerCoordinator.getPeer(bitTorrentMessage.getPeerIdBytes());
        if (peer == null) {
          final InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
          peerCoordinator.addPeerThatContactedUs(
                  inetSocketAddress.getAddress(),
                  inetSocketAddress.getPort(),
                  channel,
                  bitTorrentHandshakeMessage);
        } else {
          peer.receiveHandshake(bitTorrentHandshakeMessage);
        }
      }
      return;
    }

    final PeerCoordinator peerCoordinator = channelDictionary.get(channelHandlerContext.getChannel());
    if (peerCoordinator == null) {
      LOGGER.info("no peer coordinator to handle channel, disconnecting from peer");
      channel.close();
    }
    @SuppressWarnings("null")
    final Peer peer = peerCoordinator.getPeer(bitTorrentMessage.getPeerIdBytes());
    if (peer == null) {
      final InetSocketAddress localInetSocketAddress = (InetSocketAddress) channel.getLocalAddress();
      final InetSocketAddress remoteInetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
      try {
        //final InetSocketAddress inetSocketAddress = (InetSocketAddress) channel.getRemoteAddress();
        LOGGER.info(localInetSocketAddress.getHostName() + ":" + localInetSocketAddress.getPort() +
                " no active handshake, disconnecting from peer: " + remoteInetSocketAddress.getAddress() + ":" + remoteInetSocketAddress.getPort() +
                " id: " + new String(bitTorrentMessage.getPeerIdBytes(), "US-ASCII"));
      } catch (UnsupportedEncodingException ex) {
        throw new TexaiException(ex);
      }
      channel.close();
      return;
    }

    if (bitTorrentMessage instanceof BitTorrentBitFieldMessage) {
      // bitfield
      final BitTorrentBitFieldMessage bitTorrentBitfieldMessage = (BitTorrentBitFieldMessage) bitTorrentMessage;
      LOGGER.info("handling bitfield: " + bitTorrentBitfieldMessage);
      peer.bitfieldMessageFromPeer(bitTorrentBitfieldMessage);

    } else if (bitTorrentMessage instanceof BitTorrentCancelMessage) {
      // cancel
      final BitTorrentCancelMessage bitTorrentCancelMessage = (BitTorrentCancelMessage) bitTorrentMessage;
      LOGGER.info("handling cancel: " + bitTorrentCancelMessage);
      peer.cancelMessageFromPeer(bitTorrentCancelMessage);

    } else if (bitTorrentMessage instanceof BitTorrentChokeMessage) {
      // choke
      final BitTorrentChokeMessage bitTorrentChokeMessage = (BitTorrentChokeMessage) bitTorrentMessage;
      LOGGER.info("handling choke: " + bitTorrentChokeMessage);
      peer.chokeMessageFromPeer();

    } else if (bitTorrentMessage instanceof BitTorrentHaveMessage) {
      // have
      final BitTorrentHaveMessage bitTorrentHaveMessage = (BitTorrentHaveMessage) bitTorrentMessage;
      LOGGER.info("handling have: " + bitTorrentHaveMessage);
      peer.haveMessageFromPeer(bitTorrentHaveMessage);

    } else if (bitTorrentMessage instanceof BitTorrentInterestedMessage) {
      // interested
      final BitTorrentInterestedMessage bitTorrentInterestedMessage = (BitTorrentInterestedMessage) bitTorrentMessage;
      LOGGER.info("handling interested: " + bitTorrentInterestedMessage);
      peer.interestedMessageFromPeer();

    } else if (bitTorrentMessage instanceof BitTorrentKeepAliveMessage) {
      // keep-alive
      final BitTorrentKeepAliveMessage bitTorrentKeepAliveMessage = (BitTorrentKeepAliveMessage) bitTorrentMessage;
      LOGGER.info("handling keep-alive: " + bitTorrentKeepAliveMessage);
      peer.keepAliveMessageFromPeer();

    } else if (bitTorrentMessage instanceof BitTorrentNotInterestedMessage) {
      // not-interested
      final BitTorrentNotInterestedMessage bitTorrentNotInterestedMessage = (BitTorrentNotInterestedMessage) bitTorrentMessage;
      LOGGER.info("handling not-interested: " + bitTorrentNotInterestedMessage);
      peer.notInterestedMessageFromPeer();

    } else if (bitTorrentMessage instanceof BitTorrentPieceMessage) {
      // piece
      final BitTorrentPieceMessage bitTorrentPieceMessage = (BitTorrentPieceMessage) bitTorrentMessage;
      LOGGER.info("handling piece: " + bitTorrentPieceMessage);
      peer.pieceMessageFromPeer(bitTorrentPieceMessage);

    } else if (bitTorrentMessage instanceof BitTorrentRequestMessage) {
      // request
      final BitTorrentRequestMessage bitTorrentRequestMessage = (BitTorrentRequestMessage) bitTorrentMessage;
      LOGGER.info("handling request: " + bitTorrentRequestMessage);
      peer.requestMessageFromPeer(bitTorrentRequestMessage);

    } else if (bitTorrentMessage instanceof BitTorrentUnchokeMessage) {
      // unchoke
      final BitTorrentUnchokeMessage bitTorrentUnchokeMessage = (BitTorrentUnchokeMessage) bitTorrentMessage;
      LOGGER.info("handling unchoke: " + bitTorrentUnchokeMessage);
      peer.unChokeMessageFromPeer();

    } else {
      assert false;
    }
  }

  /** Gets the peer coordinator that handles the given info hash, that identifies the torrent.  Closes the
   * peer connection if no peer coordinator is found.
   *
   * @param infoHash the given info hash, that identifies the torrent
   * @param channel the communication channel between us and the peer
   * @return the peer coordinator that handles the given info hash, or null if not found
   */
  private PeerCoordinator getPeerCoordinator(
          final byte[] infoHash,
          final Channel channel) {
    //Preconditions
    assert infoHash != null : "infoHash must not be null";
    assert channel != null : "channel must not be null";

    final PeerCoordinator peerCoordinator = sslTorrent.getPeerCoordinator(infoHash);
    if (peerCoordinator == null) {
      LOGGER.info("no peer coordinator to handle info hash: " + ByteUtils.toHex(infoHash) + ", disconnecting from peer");
      channel.close();
    }
    return peerCoordinator;
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
