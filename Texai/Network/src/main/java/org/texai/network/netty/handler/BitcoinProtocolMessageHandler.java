package org.texai.network.netty.handler;

import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

/**
 * BitcoinProtocolMessageHandler.java
 *
 * Description: Provides a Bitcoin protocol message handler.
 *
 * Copyright (C) Apr 6, 2015, Stephen L. Reed.
 */
public class BitcoinProtocolMessageHandler extends AbstractBitcoinProtocolMessageHandler {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(BitcoinProtocolMessageHandler.class);
  // the bitcoin message receiver
  final BitcoinMessageReceiver bitcoinMessageReceiver;

  /**
   * Constructs a new BitcoinProtocolMessageHandler instance.
   *
   * @param bitcoinMessageReceiver the bitcoin message receiver
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   */
  BitcoinProtocolMessageHandler(
          final BitcoinMessageReceiver bitcoinMessageReceiver,
          final NetworkParameters networkParameters) {
    super(networkParameters);
    //Preconditions
    assert bitcoinMessageReceiver != null : "bitcoinMessageReceiver must not be null";

    this.bitcoinMessageReceiver = bitcoinMessageReceiver;
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
    LOGGER.info(throwable.getMessage());

  }

  /**
   * Receives a bitcoin protocol message.
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

    final Message message = (Message) messageEvent.getMessage();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("received from the local bitcoind instance: " + message);
    }
    bitcoinMessageReceiver.receiveMessageFromBitcoind(message);

  }
}
