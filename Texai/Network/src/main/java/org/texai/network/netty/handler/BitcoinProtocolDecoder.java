/*
 * TaggedObjectDecoder.java
 *
 * Created on Feb 23, 2015, 3:50:15 PM
 *
 * Description: Provides an encoder which serializes a Bitcoin message into a {@link ChannelBuffer}.
 *
 * Copyright (C) 2015, Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import com.google.bitcoin.core.BitcoinSerializer;
import com.google.bitcoin.core.NetworkParameters;
import static com.google.bitcoin.core.Utils.bytesToHexString;
import com.google.bitcoin.params.MainNetParams;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.texai.util.ByteUtils;
import org.texai.util.TexaiException;

/**
 * Provides a Bitcoin protocol message decoder.
 *
 * @author reed
 */
@NotThreadSafe
public class BitcoinProtocolDecoder extends FrameDecoder {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(BitcoinProtocolDecoder.class);
  // the bitcoin serializer
  private final BitcoinSerializer bitcoinSerializer;

  /**
   * Creates a new decoder with the specified maximum object size.
   *
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   */
  public BitcoinProtocolDecoder(final NetworkParameters networkParameters) {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";

    bitcoinSerializer = new BitcoinSerializer(new MainNetParams());
  }

  /**
   * Decodes the received packets so far into an object.
   *
   * @param channelHandlerContext the context of this handler
   * @param channel the current channel
   * @param channelBuffer the cumulative buffer of received packets so far. Note that the buffer might be empty, which means you should not
   * make an assumption that the buffer contains at least one byte in your decoder implementation.
   *
   * byte 0 ... 1 (object serialization protocol for port unification) bytes 1-5 ... 32 bit integer length, the maximum is a bit more than
   * 8K
   *
   * @return the object if all its bytes were contained in the buffer. null if there's not enough data in the buffer to decode an object.
   * @throws IOException if an input/output error occurs
   * @throws ClassNotFoundException if the serialized object's class cannot be found
   */
  @Override
  protected Object decode(
          final ChannelHandlerContext channelHandlerContext,
          final Channel channel,
          final ChannelBuffer channelBuffer) throws IOException, ClassNotFoundException {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert channel != null : "channel must not be null";
    assert channelBuffer != null : "channelBuffer must not be null";

    LOGGER.info("Receiving message:  " + bytesToHexString(channelBuffer.toByteBuffer(0, channelBuffer.readableBytes()).array()));

    if (channelBuffer.readableBytes() < 20) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("not enough bytes received in the buffer to decode the payload length");
      }
      // indicate that this frame is incomplete
      return null;
    }
    // network magic bytes
    final byte protocolByte1 = channelBuffer.readByte();
    final byte protocolByte2 = channelBuffer.readByte();
    final byte protocolByte3 = channelBuffer.readByte();
    final byte protocolByte4 = channelBuffer.readByte();
    if ( // mainnet
            (protocolByte1 != -66
            && protocolByte2 != -7
            && protocolByte3 != -76
            && protocolByte4 != -39)
            || //TODO
            // testnet
            (protocolByte1 != -7
            && protocolByte2 != -66
            && protocolByte3 != -76
            && protocolByte4 != -39)) {
      LOGGER.warn("wrong Bitcoin protocol bytes");
    }

    // skip over the command bytes
    channelBuffer.skipBytes(12);
    final byte[] lengthBytes = new byte[4];
    lengthBytes[0] = channelBuffer.readByte();
    lengthBytes[1] = channelBuffer.readByte();
    lengthBytes[2] = channelBuffer.readByte();
    lengthBytes[3] = channelBuffer.readByte();
    final long payloadLength = ByteUtils.toUint32LittleEndian(lengthBytes);
    LOGGER.info("payloadLength: " + payloadLength);
    final long totalLength = payloadLength + 24;
    if (channelBuffer.readableBytes() < payloadLength + 4) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("not enough bytes received in the buffer, " + channelBuffer.readableBytes()
                + ",  to decode the Bitcoin message " + totalLength);
      }
      channelBuffer.resetReaderIndex();
      // indicate that this frame is incomplete
      return null;
    }

    // bypass the checksum which the bitcoin serializer will subsequently verify
    channelBuffer.skipBytes(4);
    // populate the byte buffer with the entire serialized Bitcoin message bytes.
    if (totalLength > Integer.MAX_VALUE) {
      throw new TexaiException("invalid Bitcoin message length " + totalLength);
    }
    final ByteBuffer byteBuffer = ByteBuffer.allocateDirect((int) totalLength);
    byteBuffer.mark();
    channelBuffer.resetReaderIndex();
    for (long i = 0; i < totalLength; i++) {
      final byte byte1 = channelBuffer.readByte();
      byteBuffer.put(byte1);
    }
    byteBuffer.reset();
    // return the deserialized Bitcoin message
    return bitcoinSerializer.deserialize(byteBuffer);
  }

}
