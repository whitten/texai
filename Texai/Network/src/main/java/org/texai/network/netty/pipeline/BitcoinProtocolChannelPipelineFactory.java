/*
 * BitcoinProtocolChannelPipelineFactory.java
 *
 * Description: Initializes the channel pipeline for Bitcoin protocol messages.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import com.google.bitcoin.core.NetworkParameters;
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
  // the network parameters, e.g. MainNetParams or TestNet3Params
  private final NetworkParameters networkParameters;

  /**
   * Constructs a new PortUnificationChannelPipelineFactory instance.
   *
   * @param bitcoinProtocolMessageHandlerFactory the Bitcoin protocol message handler factory
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   *
   */
  public BitcoinProtocolChannelPipelineFactory(
          final AbstractBitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory,
          final NetworkParameters networkParameters
  ) {
    //Preconditions
    assert bitcoinProtocolMessageHandlerFactory != null : "x509SecurityInfo must not be null";
    assert networkParameters != null : "networkParameters must not be null";

    this.bitcoinProtocolMessageHandlerFactory = bitcoinProtocolMessageHandlerFactory;
    this.networkParameters = networkParameters;
  }

  /**
   * Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    final ChannelPipeline channelPipeline = Channels.pipeline();
    channelPipeline.addLast("encoder", new BitcoinProtocolEncoder(networkParameters));
    channelPipeline.addLast("decoder", new BitcoinProtocolDecoder(networkParameters));
    channelPipeline.addLast("bitcoin-handler", bitcoinProtocolMessageHandlerFactory.getHandler());
    LOGGER.info(channelPipeline);
    return channelPipeline;
  }
}
