/*
 * Copyright (C) 2015, Stephen Reed.
 */
package org.texai.network.netty.handler;

import com.google.bitcoin.core.BitcoinSerializer;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import java.io.IOException;
import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * Provides an encoder which serializes a Bitcoin message into a {@link ChannelBuffer}.
 * <p>
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * reformatted and commented by Stephen L. Reed, Texai.org, stephenreed@yahoo.com
 */
@Sharable
public class BitcoinProtocolEncoder extends OneToOneEncoder {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(BitcoinProtocolEncoder.class);
  // the bitcoin message serializer
  private final BitcoinSerializer bitcoinSerializer;

  /**
   * Creates a new BitcoinProtocolEncoder instance.
   *
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   */
  public BitcoinProtocolEncoder(final NetworkParameters networkParameters) {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";

    bitcoinSerializer = new BitcoinSerializer(networkParameters);
  }

  /**
   * Encodes the given Bitcoin message into the channel buffer. Four protocol identification bytes are prepended.
   *
   * byte 0-3 ... 0xD9B4BEF9 Bitcoin protocol magic bytes for mainnet.
   *
   * @param channelHandlerContext the channel handler context
   * @param channel the channel
   * @param obj the Bitcoin message to serialize in the Bitcoin wire protocol
   *
   * @return a channel buffer containing the serialized Bitcoin message.
   * @throws IOException when an input/output error occurs
   */
  @Override
  protected Object encode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final Object obj) throws IOException {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert obj != null : "obj must not be null";
    assert Message.class.isAssignableFrom(obj.getClass()) : "obj must be a Bitcoin protocol message";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("encoding: " + obj);
    }
    // 1 MB Bitcoin maximum block size;
    final int estimatedLength = 1024 * 1024;
    final ChannelBufferOutputStream channelBufferOutputStream = new ChannelBufferOutputStream(dynamicBuffer(
            estimatedLength,
            channelHandlerContext.getChannel().getConfig().getBufferFactory()));
    // serialize the given Bitcoin message to its wire protocol
    bitcoinSerializer.serialize((Message) obj, channelBufferOutputStream);
    final ChannelBuffer encoded = channelBufferOutputStream.buffer();
    return encoded;
  }
}
