/*
 * BitcoinProtocolMessageClientPipelineFactory.java
 *
 * Description: Configures a client pipeline to handle Bitcoin protocol messages.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.handler.BitcoinProtocolDecoder;
import org.texai.network.netty.handler.BitcoinProtocolEncoder;

/**
 * Configures a client pipeline to handle Bitcoin protocol messages.
 *
 * @author reed
 */
@NotThreadSafe
public final class BitcoinProtocolMessageClientPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(BitcoinProtocolMessageClientPipelineFactory.class);

  /**
   * Prevents this utility class from being instantiated.
   */
  private BitcoinProtocolMessageClientPipelineFactory() {
  }

  /**
   * Returns a client pipeline to handle Bitcoin protocol messages.
   *
   * @param bitcoinProtcolMessageHandler the Bitcoin protocol message handler
   *
   * @return the configured pipeline
   */
  public static ChannelPipeline getPipeline(final AbstractBitcoinProtocolMessageHandler bitcoinProtcolMessageHandler) {
    //Preconditions
    assert bitcoinProtcolMessageHandler != null : "bitcoinProtcolMessageHandler must not be null";

    final ChannelPipeline channelPipeline = Channels.pipeline();
    channelPipeline.addLast("decoder", new BitcoinProtocolDecoder());
    channelPipeline.addLast("encoder", new BitcoinProtocolEncoder());
    channelPipeline.addLast("bitcoin-handler", bitcoinProtcolMessageHandler);
    LOGGER.info("configured Bitcoin protocol message pipeline: " + channelPipeline);
    return channelPipeline;
  }
}
