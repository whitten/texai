package org.texai.skill.aicoin.support;

import org.texai.network.netty.handler.BitcoinMessageReceiver;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import java.net.SocketAddress;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;

/**
 * BitcoinProtocolMessageHandler.java
 *
 * Description: Provides a proxy that mediates the Bitcoin protocol communications between a remote client peer, a wallet for example, and
 * the local bitcoind instance.
 *
 * Copyright (C) Mar 21, 2015, Stephen L. Reed.
 */
public class BitcoinProtocolMessageHandler extends AbstractBitcoinProtocolMessageHandler implements BitcoinMessageReceiver {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(BitcoinProtocolMessageHandler.class);
  // the local Bitcoind adapter
  private final LocalBitcoindAdapter localBitcoindAdapter;
  // the Bitcoin protocol message handler dictionary, remote socket address -> Bitcoin protocol message handler
  private final Map<SocketAddress, BitcoinProtocolMessageHandler> bitcoinProtocolMessageHandlerDictionary;
  // the remote socket address
  private SocketAddress socketAddress;
  // the remote peer channel
  private Channel remotePeerChannel;
  // the local bitcoind instance channel
  private Channel localBitcoindChannel;

  /**
   * Creates a new instance of BitcoinProtocolMessageHandler.
   *
   * @param networkParameters the network parameters, main net, test net, or regression test net
   * @param nodeRuntime the node runtime
   * @param bitcoinProtocolMessageProxyDictionary the Bitcoin protocol message proxy dictionary, remote socket address -> Bitcoin protocol
   * message proxy
   */
  public BitcoinProtocolMessageHandler(
          final NetworkParameters networkParameters,
          final BasicNodeRuntime nodeRuntime,
          final Map<SocketAddress, BitcoinProtocolMessageHandler> bitcoinProtocolMessageProxyDictionary) {
    super(networkParameters);
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_MAINNET) && MainNetParams.class.isAssignableFrom(networkParameters.getClass()))
            || (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_TESTNET) && TestNet3Params.class.isAssignableFrom(networkParameters.getClass()));
    assert bitcoinProtocolMessageProxyDictionary != null : "bitcoinProtocolMessageProxyDictionary must not be null";

    this.bitcoinProtocolMessageHandlerDictionary = bitcoinProtocolMessageProxyDictionary;
    localBitcoindAdapter = new LocalBitcoindAdapter(
            networkParameters,
            this, //  bitcoinMessageReceiver
            nodeRuntime);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[Inbound - Bitcoin protocol remote peer ")
            .append(remotePeerChannel.getRemoteAddress())
            .append(", local bitcoind instance ")
            .append(localBitcoindChannel.getRemoteAddress())
            .toString();
  }

  /**
   * Catches a channel exception.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    final Throwable throwable = exceptionEvent.getCause();
    LOGGER.info(StringUtils.getStackTraceAsString(throwable));

    // remove this handler from the dictionary
    if (socketAddress == null) {
      LOGGER.info("remote socket address is null, cannot remove from the Bitcoin protocol message handler dictionary");
      return;
    }
    synchronized (bitcoinProtocolMessageHandlerDictionary) {
      bitcoinProtocolMessageHandlerDictionary.remove(socketAddress);
    }
    if (remotePeerChannel != null) {
      remotePeerChannel.close();
    }
    localBitcoindAdapter.shutDown();
  }

  /**
   * Receives a Bitcoin protocol message from a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert messageEvent != null : "messageEvent must not be null";
    assert Message.class.isAssignableFrom(messageEvent.getMessage().getClass());

    final Channel channel = channelHandlerContext.getChannel();
    assert channel != null;

    if (socketAddress == null) {
      socketAddress = channelHandlerContext.getChannel().getRemoteAddress();
    } else {
      assert socketAddress == channelHandlerContext.getChannel().getRemoteAddress();
    }

    final Message message = (Message) messageEvent.getMessage();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("***** received from remote Bitcoin protocol peer: " + message);
    }

    boolean isNewConnection = false;
    synchronized (bitcoinProtocolMessageHandlerDictionary) {
      if (!bitcoinProtocolMessageHandlerDictionary.containsKey(socketAddress)) {
        isNewConnection = true;
        bitcoinProtocolMessageHandlerDictionary.put(socketAddress, this);
      }
    }

    if (isNewConnection) {
      remotePeerChannel = channel;
      localBitcoindAdapter.startUp();
      localBitcoindChannel = localBitcoindAdapter.getChannel();
    } else {
      assert socketAddress == channelHandlerContext.getChannel().getRemoteAddress();
      assert remotePeerChannel == channel;
    }

    if (Block.class.isAssignableFrom(message.getClass())) {
      LOGGER.info("dropping a " + message.getClass().getSimpleName() + " message from the remote peer");
    }
    localBitcoindChannel.write(message);

  }

  /**
   * Receives an outbound bitcoin message from the local bitcoind instance, and relays it to the remote peer.
   *
   * @param message the given bitcoin protocol message
   */
  @Override
  public void receiveMessageFromBitcoind(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("***** sending to remote Bitcoin protocol peer: " + message);
    }
    remotePeerChannel.write(message);
  }
}
