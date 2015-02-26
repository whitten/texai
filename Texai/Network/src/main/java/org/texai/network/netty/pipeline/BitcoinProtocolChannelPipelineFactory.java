/*
 * BitcoinProtocolChannelPipelineFactory.java
 *
 * Description: Initializes the channel pipeline for Bitcoin protocol messages.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandlerFactory;
import org.texai.network.netty.handler.BitcoinProtocolDecoder;
import org.texai.network.netty.handler.BitcoinProtocolEncoder;

/**
 * Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * @author reed
 */
@NotThreadSafe
public class BitcoinProtocolChannelPipelineFactory implements ChannelPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(BitcoinProtocolChannelPipelineFactory.class);
  // the Bitcoin protocol message handler factory
  private final AbstractBitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory;

  /**
   * Constructs a new PortUnificationChannelPipelineFactory instance.
   *
   * @param bitcoinProtocolMessageHandlerFactory the Bitcoin protocol message handler factory
   *
   */
  public BitcoinProtocolChannelPipelineFactory(final AbstractBitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory) {
    //Preconditions
    assert bitcoinProtocolMessageHandlerFactory != null : "x509SecurityInfo must not be null";

    this.bitcoinProtocolMessageHandlerFactory = bitcoinProtocolMessageHandlerFactory;
  }

  /**
   * Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    final ChannelPipeline channelPipeline = Channels.pipeline();
    channelPipeline.addLast("encoder", new BitcoinProtocolEncoder());
    channelPipeline.addLast("decoder", new BitcoinProtocolDecoder());
    channelPipeline.addLast("bitcoin-handler", bitcoinProtocolMessageHandlerFactory.getHandler());
    LOGGER.info(channelPipeline);
    return channelPipeline;
  }
}
