/*
 * Peer - All public information concerning a peer. Copyright (C) 2003 Mark J.
 * Wielaard
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */
package org.texai.torrent;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.texai.network.netty.handler.AbstractBitTorrentHandler;
import org.texai.network.netty.pipeline.BitTorrentClientPipelineFactory;
import org.texai.torrent.message.BitTorrentBitFieldMessage;
import org.texai.torrent.message.BitTorrentCancelMessage;
import org.texai.torrent.message.BitTorrentChokeMessage;
import org.texai.torrent.message.BitTorrentHandshakeMessage;
import org.texai.torrent.message.BitTorrentHaveMessage;
import org.texai.torrent.message.BitTorrentInterestedMessage;
import org.texai.torrent.message.BitTorrentNotInterestedMessage;
import org.texai.torrent.message.BitTorrentPieceMessage;
import org.texai.torrent.message.BitTorrentRequestMessage;
import org.texai.torrent.message.BitTorrentUnchokeMessage;
import org.texai.util.ByteUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides public information concerning a peer. */
public final class Peer implements Comparable<Peer> {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Peer.class);
  /** the peer connection timeout milliseconds */
  private static final int PEER_CONNECTION_TIMEOUT = 20000;
  /** the tracked peer information */
  private final TrackedPeerInfo trackedPeerInfo;
  /** the indicator whether we sent our handshake message to this peer */
  private final AtomicBoolean isOurHandshakeSent = new AtomicBoolean(false);
  /** the indicator whether this peer has completed its handshake with us */
  private final AtomicBoolean hasHandshakeCompleted = new AtomicBoolean(false);
  /** the peer coordinator */
  private final PeerCoordinator peerCoordinator;
  // Interesting and choking describes whether we are interested in or
  // are choking the other side.
  /** the indicator whether we are interested in the peer */
  private final AtomicBoolean isPeerInterestingToUs = new AtomicBoolean(false);
  /** the indicator whether we are choking the peer */
  private final AtomicBoolean areWeChokingThePeer = new AtomicBoolean(true);
  // Interested and choked describes whether the other side is
  // interested in us or choked us.
  /** the indicator whether the peer is interested in us */
  private final AtomicBoolean isPeerInterestedInUs = new AtomicBoolean(false);
  /** the indicator that the peer is choking us */
  private final AtomicBoolean isPeerChokingUs = new AtomicBoolean(true);
  /** the total number of downloaded bytes */
  private final AtomicLong nbrBytesDownloaded = new AtomicLong(0L);
  /** the total number of uploaded bytes */
  private final AtomicLong nbrBytesUploaded = new AtomicLong(0L);
  /** the bitfield provided by the remote peer */
  private final AtomicReference<BitField> bitField = new AtomicReference<>(null);
  /** the outstanding requests */
  private final List<Request> outstandingRequests = new ArrayList<>();
  /** the last request */
  private Request lastRequest = null;
  /** the indicator whether we have to resend outstanding requests (true after we got choked) */
  private boolean resend = false;
  /** the maximum requests in the pipeline  */
  private static final int MAX_PIPELINE = 5;
  /** the 16K size */
  private static final int PARTSIZE = 16384; // 16K
  /** the indicator to quit processing */
  private boolean isQuit;
  /** the communication channel between us and the peer */
  private Channel channel;

  /** Creates a disconnected peer given its TrackedPeerInfo.
   *
   * @param trackedPeerInfo the tracked peer information
   * @param peerCoordinator the peer coordinator
   */
  public Peer(
          final TrackedPeerInfo trackedPeerInfo,
          final PeerCoordinator peerCoordinator) {
    //Preconditions
    assert trackedPeerInfo != null : "trackedPeerInfo must not be null";
    assert peerCoordinator != null : "peerCoordinator must not be null";

    this.trackedPeerInfo = trackedPeerInfo;
    this.peerCoordinator = peerCoordinator;
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " created disconnected peer: " + this);
  }

  /** Creates a connected peer.
   *
   * @param inetAddress the peer's IP address
   * @param port the peer's port
   * @param channel the communication channel between us and the peer
   * @param bitTorrentHandshakeMessage the remote peer's handshake message
   * @param peerCoordinator the peer coordinator
   */
  public Peer(
          final InetAddress inetAddress,
          final int port,
          final Channel channel,
          final BitTorrentHandshakeMessage bitTorrentHandshakeMessage,
          final PeerCoordinator peerCoordinator) {
    //Preconditions
    assert inetAddress != null : "inetAddress must not be null";
    assert port > 0 : "port must be positive";
    assert channel != null : "channel must not be null";
    assert bitTorrentHandshakeMessage != null : "bitTorrentHandshakeMessage must not be null";
    assert peerCoordinator != null : "peerCoordinator must not be null";

    this.channel = channel;
    this.peerCoordinator = peerCoordinator;

    final byte[] peerIdBytes = bitTorrentHandshakeMessage.getPeerIdBytes();
    this.trackedPeerInfo = new TrackedPeerInfo(peerIdBytes, inetAddress, port);
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " created connected peer: " + this);
  }

  /** Receives the handshake message sent by the peer, and replies with our handshake if not already sent.
   *
   * @param peerBitTorrentHandshakeMessage the remote peer's handshake message
   */
  public void receiveHandshake(final BitTorrentHandshakeMessage peerBitTorrentHandshakeMessage) {
    //Preconditions
    assert peerBitTorrentHandshakeMessage != null : "peerBitTorrentHandshakeMessage must not be null";
    assert channel != null : "channel must not be null";
    assert ByteUtils.areEqual(peerCoordinator.getMetaInfo().getInfoHash(), peerBitTorrentHandshakeMessage.getInfoHash()) :
            "peer info hash must match ours";

    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " receiving handshake from " + this);
    if (!isOurHandshakeSent.get()) {
      sendHandshake();
    }
    hasHandshakeCompleted.set(true);
    final BitTorrentBitFieldMessage bitTorrentBitFieldMessage = new BitTorrentBitFieldMessage(
            peerCoordinator.getBitMap(),
            peerCoordinator.getPeerIdBytes());
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending " + bitTorrentBitFieldMessage.toDetailedString() + " to " + this);
    channel.write(bitTorrentBitFieldMessage);
    // unchoke the peer
    peerCoordinator.unchokePeer();
  }

  /** Sends our handshake message to the remote peer. */
  @SuppressWarnings("ThrowableResultIgnored")
  public void sendHandshake() {
    //Preconditions
    assert !hasHandshakeCompleted.get() : "handshake must not have completed";

    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo() + " sending handshake to " + this);
    if (channel == null) {
      final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
              Executors.newCachedThreadPool(),
              Executors.newCachedThreadPool()));

      // configure the client pipeline
      final AbstractBitTorrentHandler bitTorrentHandler = new BitTorrentHandler(peerCoordinator.getSSLTorrent());
      final ChannelPipeline channelPipeline = BitTorrentClientPipelineFactory.getPipeline(
              bitTorrentHandler,
              peerCoordinator.getX509SecurityInfo());
      clientBootstrap.setPipeline(channelPipeline);

      // start the connection attempt
      final ChannelFuture channelFuture =
              clientBootstrap.connect(new InetSocketAddress("localhost", trackedPeerInfo.getPort()));

      // wait until the connection attempt succeeds or fails
      channel = channelFuture.awaitUninterruptibly().getChannel();
      if (!channelFuture.isSuccess()) {
        LOGGER.warn(StringUtils.getStackTraceAsString(channelFuture.getCause()));
        throw new TexaiException(channelFuture.getCause());
      }
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " connected to bit torrent client " + this);
    }

    final BitTorrentHandshakeMessage ourBitTorrentHandshakeMessage = new BitTorrentHandshakeMessage(
            peerCoordinator.getMetaInfo().getInfoHash(),
            peerCoordinator.getPeerIdBytes());
    channel.write(ourBitTorrentHandshakeMessage);
    isOurHandshakeSent.set(true);
  }

  /** Receives a bitfield message from the peer.
   *
   * @param bitTorrentBitFieldMessage the bitfield message
   */
  public synchronized void bitfieldMessageFromPeer(final BitTorrentBitFieldMessage bitTorrentBitFieldMessage) {
    //Preconditions
    assert bitTorrentBitFieldMessage != null : "bitTorrentBitFieldMessage must not be null";

    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received " + bitTorrentBitFieldMessage.toDetailedString() + " from " + this);
    if (bitField.get() != null) {
      LOGGER.debug(peerCoordinator.getOurTrackedPeerInfo().toString() + " received unexpected bitfield message from " + this);
      return;
    }

    bitField.set(new BitField(bitTorrentBitFieldMessage.getBitField(), peerCoordinator.getMetaInfo().getNbrPieces()));
    setInteresting(peerCoordinator.peerBitFieldEvent(this, bitField.get()));
  }

  /** Sets whether or not we are interested in pieces from this peer. Defaults
   * to false. When interest is true and this peer unchokes us then we start
   * downloading from it. Has no effect when not connected.
   *
   * @param isInterestingOrNot the indicator whether or not we are interested in pieces from this peer
   */
  public void setInteresting(final boolean isInterestingOrNot) {
    if (hasHandshakeCompleted.get()) {
      if (isInterestingOrNot != isPeerInterestingToUs.get()) {
        isPeerInterestingToUs.set(isInterestingOrNot);
        assert channel != null;
        if (isInterestingOrNot) {
          final BitTorrentInterestedMessage bitTorrentInterestedMessage =
                  new BitTorrentInterestedMessage(peerCoordinator.getPeerIdBytes());
          LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending interested to " + this);
          channel.write(bitTorrentInterestedMessage);
        } else {
          final BitTorrentNotInterestedMessage bitTorrentNotInterestedMessage =
                  new BitTorrentNotInterestedMessage(peerCoordinator.getPeerIdBytes());
          LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending not-interested to " + this);
          channel.write(bitTorrentNotInterestedMessage);
        }
        if (isPeerInterestingToUs.get() && !isPeerChokingUs.get()) {
          request();
        }
      }
    }
  }

  /** Receives an interested message from the peer. */
  public void interestedMessageFromPeer() {
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received interested from " + this);
    isPeerInterestedInUs.set(true);
    peerCoordinator.peerInterestedOrUninterestedEvent(this, true);
  }

  /** Receives a not-interested message from the peer. */
  public void notInterestedMessageFromPeer() {
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received not-interested from " + this);
    isPeerInterestedInUs.set(false);
    peerCoordinator.peerInterestedOrUninterestedEvent(this, false);
  }

  /** Sets whether or not we are choking the peer. Defaults to true. When choke
   * is false and the peer requests some pieces we upload them, otherwise
   * requests of this peer are ignored.
   *
   * @param choke the indicator whether or not we are choking the peer
   */
  public void setChoking(final boolean choke) {
    if (hasHandshakeCompleted.get()) {
      LOGGER.info(this + " setChoking(" + choke + ")");

      if (areWeChokingThePeer.get() != choke) {
        areWeChokingThePeer.set(choke);
        assert channel != null;
        if (choke) {
          LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending choke to " + this);
          final BitTorrentChokeMessage bitTorrentChokeMessage = new BitTorrentChokeMessage(peerCoordinator.getPeerIdBytes());
          channel.write(bitTorrentChokeMessage);
        } else {
          LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending unchoke to " + this);
          final BitTorrentUnchokeMessage bitTorrentUnchokeMessage = new BitTorrentUnchokeMessage(peerCoordinator.getPeerIdBytes());
          channel.write(bitTorrentUnchokeMessage);
        }
      }
    }
  }

  /** Receives a choke message from the peer. */
  public void chokeMessageFromPeer() {
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received choke from " + this);
    isPeerChokingUs.set(true);
    resend = true;
  }

  /** Receives an unchoke message from the peer. */
  public void unChokeMessageFromPeer() {
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received unchoke from " + this);
    isPeerChokingUs.set(false);
    resend = false;
    if (isPeerInterestingToUs.get()) {
      request();
    }
  }

  /** Receives a keep-alive message from the peer. */
  public void keepAliveMessageFromPeer() {
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received keep-alive from " + this);
  }

  /** Tells the peer we have another piece.
   *
   * @param pieceIndex the piece index
   */
  public synchronized void havePiece(final int pieceIndex) {
    //Preconditions
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert channel != null : "channel must not be null";

    if (lastRequest != null && lastRequest.getPieceIndex() == pieceIndex) {
      // tell the other side that we are no longer interested in any of the outstanding requests for this piece.
      lastRequest = null;
    }

    final Iterator<Request> outstandingRequests_iter = outstandingRequests.iterator();
    while (outstandingRequests_iter.hasNext()) {
      final Request outstandingRequest = outstandingRequests_iter.next();
      if (outstandingRequest.getPieceIndex() == pieceIndex) {
        outstandingRequests_iter.remove();
        LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending cancel to " + this);
        final BitTorrentCancelMessage bitTorrentCancelMessage = new BitTorrentCancelMessage(
          outstandingRequest.getPieceIndex(),
          outstandingRequest.getOffset(),
          outstandingRequest.getLength(),
          peerCoordinator.getPeerIdBytes());
        channel.write(bitTorrentCancelMessage);
      }
    }

    // Tell the other side that we really have this piece.
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending have piece " + pieceIndex + " to " + this);
    final BitTorrentHaveMessage bitTorrentHaveMessage = new BitTorrentHaveMessage(
          pieceIndex,
          peerCoordinator.getPeerIdBytes());
    channel.write(bitTorrentHaveMessage);

    // Request something else if necessary.
    addRequest();

    // Is the peer still interesting?
    if (lastRequest == null) {
      setInteresting(false);
    }
  }

  /** Receives a have message from the peer.
   *
   * @param bitTorrentHaveMessage the have message
   */
  public synchronized void haveMessageFromPeer(final BitTorrentHaveMessage bitTorrentHaveMessage) {
    //Preconditions
    assert bitTorrentHaveMessage != null : "bitTorrentHaveMessage must not be null";

    final int pieceIndex = bitTorrentHaveMessage.getPieceIndex();
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received have piece " + pieceIndex + " from " + this);
    // Sanity check
    if (pieceIndex < 0 || pieceIndex >= peerCoordinator.getMetaInfo().getNbrPieces()) {
      // XXX disconnect?
      LOGGER.log(Level.DEBUG, "Got strange 'have: " + pieceIndex + "' message from " + this);
      return;
    }

    // Can happen if the other side never send a bitfield message.
    if (bitField.get() == null) {
      bitField.set(new BitField(peerCoordinator.getMetaInfo().getNbrPieces()));
    }

    bitField.get().set(pieceIndex);

    if (peerCoordinator.peerHavePieceEvent(this, pieceIndex)) {
      setInteresting(true);
    }
  }

  /** Receives a request message from the peer.
   *
   * @param bitTorrentRequestMessage the bit torrent request message
   */
  public synchronized void requestMessageFromPeer(final BitTorrentRequestMessage bitTorrentRequestMessage) {
    //Preconditions
    assert bitTorrentRequestMessage != null : "bitTorrentRequestMessage must not be null";

    final int pieceIndex = bitTorrentRequestMessage.getPieceIndex();
    final int offset = bitTorrentRequestMessage.getOffset();
    final int length = bitTorrentRequestMessage.getLength();
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() +
            " received request(" + pieceIndex + ", " + offset + ", " + length + ") from " + this);
    if (areWeChokingThePeer.get()) {
      LOGGER.log(Level.DEBUG, "request received, but choking " + this);
      return;
    }

    // Sanity check
    if (pieceIndex < 0 ||
            pieceIndex >= peerCoordinator.getMetaInfo().getNbrPieces() ||
            offset < 0 ||
            offset > peerCoordinator.getMetaInfo().getPieceLength(pieceIndex) ||
            length <= 0 ||
            length > 4 * PARTSIZE) {
      LOGGER.log(Level.DEBUG, "Got strange 'request: " + pieceIndex + ", " + offset + ", " + length + "' message from " + this);
      return;
    }

    final byte[] pieceBytes;
    try {
      pieceBytes = peerCoordinator.getPieceBytes(this, pieceIndex);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    if (pieceBytes == null) {
      // XXX - Protocol error-> diconnect?
      LOGGER.log(Level.DEBUG, "Got request for unknown piece: " + pieceIndex);
      return;
    }

    // More sanity checks
    if (offset >= pieceBytes.length || offset + length > pieceBytes.length) {
      // XXX - Protocol error-> disconnect?
      LOGGER.log(Level.DEBUG, "Got out of range 'request: " + pieceIndex + ", " + offset + ", " + length + "' message from " + this);
      return;
    }

    final byte[] chunkBytes = new byte[length];
    System.arraycopy(
            pieceBytes, // source
            offset, // source offset
            chunkBytes, // destination
            0, // destination offset
            length); // length

    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending piece (" + pieceIndex + ", " + offset + ", " + length + ")" + " to " + this);
    final BitTorrentPieceMessage bitTorrentPieceMessage = new BitTorrentPieceMessage(
            pieceIndex,
            offset,
            chunkBytes,
            peerCoordinator.getPeerIdBytes());
    assert channel != null;
    channel.write(bitTorrentPieceMessage);
  }

  /** Processes a piece message sent by the peer and requeues/sends requests when they must have been lost.
   *
   * @param bitTorrentPieceMessage the piece message
   */
  public synchronized void pieceMessageFromPeer(final BitTorrentPieceMessage bitTorrentPieceMessage) {
    //Preconditions
    assert bitTorrentPieceMessage != null : "bitTorrentPieceMessage must not be null";

    final int pieceIndex = bitTorrentPieceMessage.getPieceIndex();
    final int offset = bitTorrentPieceMessage.getOffset();
    final int length = bitTorrentPieceMessage.getChunkBytes().length;
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() +
            " received piece (" + pieceIndex + "," + offset + "," + length + ") from " + this);
    int requestNbr = getFirstOutstandingRequest(pieceIndex);

    if (requestNbr == -1) {
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() +
              " unrequested piece (" + pieceIndex + ", " + offset + ", " + length + ") from " + this);
      nbrBytesDownloaded.set(0L);
      return;
    }

    // lookup the correct piece chunk request from the list
    Request request;
    request = outstandingRequests.get(requestNbr);
    while (request.getPieceIndex() == pieceIndex && request.getOffset() != offset && requestNbr < outstandingRequests.size() - 1) {
      requestNbr++;
      request = outstandingRequests.get(requestNbr);
    }

    if (request.getPieceIndex() != pieceIndex || request.getOffset() != offset || request.getLength() != length) {
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() +
              " unrequested or unneeded 'piece (" + pieceIndex + ", " + offset + ", " + length + ") received from " + this);
      LOGGER.info("  should match request: " + request);
      nbrBytesDownloaded.set(0L);
      return;
    }
    // copy the received chunk into the request's piece buffer
    System.arraycopy(
            bitTorrentPieceMessage.getChunkBytes(), // source
            0, // source offset
            request.getPieceBuffer(), // destination
            offset, // destination offset
            length); // length

    // report missing requests
    if (requestNbr != 0) {
      final StringBuffer errmsg = new StringBuffer("Some requests dropped, got " + request +
              ", wanted:");
      for (int i = 0; i < requestNbr; i++) {
        final Request droppedRequest = outstandingRequests.remove(0);
        outstandingRequests.add(droppedRequest);
        // keep waiting for missing requests as they will be rerequested when we get choked/unchoked again
        if (LOGGER.isDebugEnabled()) {
          errmsg.append(' ');
          errmsg.append(droppedRequest);
        }
      }
      if (LOGGER.isDebugEnabled()) {
        errmsg.append(' ');
        errmsg.append(this);
        LOGGER.log(Level.DEBUG, errmsg);
      }
    }
    outstandingRequests.remove(0);

    // request more if necessary to keep the pipeline filled.
    addRequest();

    final int pieceLength = request.getLength();
    nbrBytesDownloaded.addAndGet(pieceLength);
    peerCoordinator.downloaded(this, pieceLength);

    if (getFirstOutstandingRequest(request.getPieceIndex()) == -1) {
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " received all chunks for piece " + pieceIndex);
      if (!peerCoordinator.peerCompletePieceEvent(this, request.getPieceIndex(), request.getPieceBuffer())) {
        LOGGER.log(Level.DEBUG, "Got BAD " + request.getPieceIndex() + " from " + this);
        nbrBytesDownloaded.set(0L);
      }
    }
  }

  /** Processes a cancel message from the peer.
   *
   * @param bitTorrentCancelMessage the cancel message
   */
  public void cancelMessageFromPeer(final BitTorrentCancelMessage bitTorrentCancelMessage) {
    //Preconditions
    assert bitTorrentCancelMessage != null : "bitTorrentCancelMessage must not be null";

    final int pieceIndex = bitTorrentCancelMessage.getPieceIndex();
    final int offset = bitTorrentCancelMessage.getOffset();
    final int length = bitTorrentCancelMessage.getLength();
    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() +
            " received cancel (" + pieceIndex + ", " + offset + ", " + length + ") from " + this);
  }

  /** Closes the channel to the peer. */
  @SuppressWarnings("ThrowableResultIgnored")
  public void disconnect() {
    if (isQuit) {
      LOGGER.info("peer " + this + " is already disconected");
    } else {
      if (hasHandshakeCompleted.get()) {
        hasHandshakeCompleted.set(false);
      }
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " disconnect " + this + " started");
      isQuit = true;
      final ChannelFuture channelFuture = channel.close();

      // wait until the disconnection attempt succeeds or fails
      channelFuture.awaitUninterruptibly();
      if (!channelFuture.isSuccess()) {
        throw new TexaiException(channelFuture.getCause());
      }
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " disconnect from " + this + " completed");
    }
  }

  /** Gets the index to the first outstanding request having the given piece sequence number.
   *
   * @param piece the piece sequence number
   * @return the index to the first outstanding request having the given piece sequence number
   */
  private int getFirstOutstandingRequest(final int piece) {
    //Preconditions
    assert piece >= 0 : "piece must not be negative";

    for (int i = 0; i < outstandingRequests.size(); i++) {
      if ((outstandingRequests.get(i)).getPieceIndex() == piece) {
        return i;
      }
    }
    return -1;
  }

  /** Starts or resumes requesting pieces. */
  private void request() {
    // Are there outstanding requests that have to be resent?
    if (resend) {
      LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " resending outstanding requests to " + this);
      final Iterator<Request> requests_iter = outstandingRequests.iterator();
      while (requests_iter.hasNext()) {
        sendRequestToPeer(requests_iter.next());
      }
      resend = false;
    }

    // Add/Send some more requests if necessary.
    addRequest();
  }

  /** Adds a new request to the outstanding requests list. */
  private void addRequest() {
    boolean areMorePiecesRemaining = true;
    while (areMorePiecesRemaining) {
      areMorePiecesRemaining = outstandingRequests.size() < MAX_PIPELINE;

      if (areMorePiecesRemaining && lastRequest == null) {
        // we want something and we don't have outstanding requests
        areMorePiecesRemaining = requestNextPiece();
      } else if (areMorePiecesRemaining) {
        // we want something
        int pieceLength;
        boolean isLastChunk;
        pieceLength = peerCoordinator.getMetaInfo().getPieceLength(lastRequest.getPieceIndex());
        isLastChunk = lastRequest.getOffset() + lastRequest.getLength() == pieceLength;

        if (isLastChunk) {
          // last chunck of a piece
          areMorePiecesRemaining = requestNextPiece();
        } else {
          final int nextPiece = lastRequest.getPieceIndex();
          final int nextBegin = lastRequest.getOffset() + PARTSIZE;
          final byte[] pieceBuffer = lastRequest.getPieceBuffer();
          final int maxLength = pieceLength - nextBegin;
          final int nextLength = maxLength > PARTSIZE ? PARTSIZE : maxLength;
          final Request request = new Request(
                  nextPiece,
                  pieceBuffer,
                  nextBegin,
                  nextLength);
          outstandingRequests.add(request);
          if (!isPeerChokingUs.get()) {
            sendRequestToPeer(request);
          }
          lastRequest = request;
        }
      }
    }

    LOGGER.debug(this + " requests " + outstandingRequests);
  }

  /** Starts requesting first chunk of next piece. Returns true if something has been added to the requests,
   * false otherwise.
   *
   * @return whether something has been added to the requests
   */
  private boolean requestNextPiece() {
    // Check that we already know what the other side has.
    if (bitField.get() != null) {
      final int nextPiece = peerCoordinator.getWantedPiece(this, bitField.get());
      LOGGER.log(Level.DEBUG, this + " want piece " + nextPiece);
      synchronized (this) {
        if (nextPiece != -1 && (lastRequest == null || lastRequest.getPieceIndex() != nextPiece)) {
          final int piece_length = peerCoordinator.getMetaInfo().getPieceLength(nextPiece);
          final byte[] pieceBuffer = new byte[piece_length];

          final int length = Math.min(piece_length, PARTSIZE);
          final Request request = new Request(nextPiece, pieceBuffer, 0, length);
          outstandingRequests.add(request);
          if (!isPeerChokingUs.get()) {
            sendRequestToPeer(request);
          }
          lastRequest = request;
          return true;
        }
      }
    }

    return false;
  }

  /** Sends the given request to this peer.
   *
   * @param request the given request
   */
  private void sendRequestToPeer(final Request request) {
    //Preconditions
    assert request != null : "request must not be null";
    assert channel != null : "channel must not be null";

    LOGGER.info(peerCoordinator.getOurTrackedPeerInfo().toString() + " sending request " + request + " to " + this);
    final BitTorrentRequestMessage bitTorrentRequestMessage = new BitTorrentRequestMessage(
            request.getPieceIndex(),
            request.getOffset(),
            request.getLength(),
            peerCoordinator.getPeerIdBytes());
    channel.write(bitTorrentRequestMessage);
  }

  /** Returns the tracked peer information.
   *
   *
   * @return the tracked peer information
   */
  public TrackedPeerInfo getTrackedPeerInfo() {
    return trackedPeerInfo;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return trackedPeerInfo.toString();
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return trackedPeerInfo.hashCode();
  }

  /** Returns whether some other object equals this one.
   * Two Peers are equal when they have the same TrackedPeerInfo. All other properties
   * are ignored.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Peer) {
      final Peer peer = (Peer) obj;
      return trackedPeerInfo.equals(peer.trackedPeerInfo);
    } else {
      return false;
    }
  }

  /** Compares the PeerIDs.
   *
   * @param peer the other peer
   * @return -1 if this peer is less than the other peer, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final Peer peer) {
    return trackedPeerInfo.compareTo(peer.trackedPeerInfo);
  }

  /** Returns whether the peer is connected.
   *
   * @return whether the peer is connected
   */
  public boolean isConnected() {
    return hasHandshakeCompleted.get();
  }

  /** Returns whether or not the peer is interested in pieces we have. Returns false if
   * not connected.
   *
   * @return whether or not the peer is interested in pieces we have, false if
   * not connected.
   */
  public boolean isInterested() {
    return (hasHandshakeCompleted.get()) && isPeerInterestedInUs();
  }

  /** Returns whether or not the peer has pieces we want from it. Returns false if not
   * connected.
   *
   * @return  whether or not the peer has pieces we want from it, false if not
   * connected
   */
  public boolean isInteresting() {
    return hasHandshakeCompleted.get() && isPeerInterestingToUs();
  }

  /** Returns whether or not we are choking the peer. Returns true when not connected.
   *
   * @return whether or not we are choking the peer, true when not connected
   */
  public boolean isChoking() {
    return hasHandshakeCompleted.get() || areWeChokingThePeer();
  }

  /** Returns whether or not the peer choked us. Returns true when not connected.
   *
   * @return whether or not the peer choked us, true when not connected
   */
  public boolean isChoked() {
    return hasHandshakeCompleted.get() || isPeerChokingUs();
  }

  /** Returns the number of bytes that have been downloaded. Can be reset to
   * zero with <code>resetCounters()</code>/
   *
   * @return the number of bytes that have been downloaded
   */
  public long getDownloaded() {
    return hasHandshakeCompleted.get() ? 0 : getNbrBytesDownloaded();
  }

  /** Returns the number of bytes that have been uploaded. Can be reset to zero
   * with <code>resetCounters()</code>/
   *
   * @return the number of bytes that have been uploaded
   */
  public long getUploaded() {
    return hasHandshakeCompleted.get() ? 0 : getNbrBytesUploaded();
  }

  /** Resets the downloaded and uploaded counters to zero. */
  public void resetCounters() {
    if (hasHandshakeCompleted.get()) {
      setNbrBytesDownloaded(0);
      setNbrBytesUploaded(0);
    }
  }

  /** Records the number of bytes uploaded.
   *
   * @param size the number of bytes uploaded
   */
  public void uploaded(final int size) {
    //Preconditions
    assert size >= 0 : "size must not be negative";

    nbrBytesUploaded.addAndGet(size);
    peerCoordinator.uploaded(this, size);
  }

  /** Gets the indicator whether we are interested in the peer.
   *
   * @return the indicator whether we are interested in the peer
   */
  public boolean isPeerInterestingToUs() {
    return isPeerInterestingToUs.get();
  }

  /** Gets the indicator whether we are choking the peer.
   *
   * @return the indicator whether we are choking the peer
   */
  public boolean areWeChokingThePeer() {
    return areWeChokingThePeer.get();
  }

  /** Gets the indicator that the peer is choking us.  Not synchronized in order to avoid deadlock with
   * OutgoingPeerMessageHandler.run().
   *
   * @return the indicator that the peer is choking us
   */
  public boolean isPeerChokingUs() {
    return isPeerChokingUs.get();
  }

  /** Gets the total number of downloaded bytes.
   *
   * @return the total number of downloaded bytes
   */
  public long getNbrBytesDownloaded() {
    return nbrBytesDownloaded.get();
  }

  /** Sets the total number of downloaded bytes.
   *
   * @param nbrBytesDownloaded the total number of downloaded bytes
   */
  public void setNbrBytesDownloaded(final long nbrBytesDownloaded) {
    this.nbrBytesDownloaded.set(nbrBytesDownloaded);
  }

  /** Gets the total number of uploaded bytes.
   *
   * @return the total number of uploaded bytes
   */
  public long getNbrBytesUploaded() {
    return nbrBytesUploaded.get();
  }

  /** Sets the total number of uploaded bytes.
   *
   * @param nbrBytesUploaded the total number of uploaded bytes
   */
  public void setNbrBytesUploaded(final long nbrBytesUploaded) {
    //Preconditions
    assert nbrBytesUploaded >= 0 : "nbrBytesUploaded must not be negative";

    this.nbrBytesUploaded.set(nbrBytesUploaded);
  }

  /** Gets whether the peer is interested in us.
   *
   * @return whether the peer is interested in us
   */
  public boolean isPeerInterestedInUs() {
    return isPeerInterestedInUs.get();
  }

  /** Gets the communication channel between us and the peer.
   *
   * @return the communication channel between us and the peer
   */
  public Channel getChannel() {
    return channel;
  }

  /** Sets the communication channel between us and the peer.
   *
   * @param channel the communication channel between us and the peer
   */
  public void setChannel(final Channel channel) {
    //Preconditions
    assert channel != null : "channel must not be null";

    this.channel = channel;
  }
}
