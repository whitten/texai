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
  // the protocol magic bytes
  final byte[] magicBytes = new byte[4];

  /**
   * Creates a new decoder with the specified maximum object size.
   *
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   */
  public BitcoinProtocolDecoder(final NetworkParameters networkParameters) {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";

    bitcoinSerializer = new BitcoinSerializer(networkParameters);
    magicBytes[0] = (byte) (0xFF & networkParameters.getPacketMagic() >>> (3 * 8));
    magicBytes[1] = (byte) (0xFF & networkParameters.getPacketMagic() >>> (2 * 8));
    magicBytes[2] = (byte) (0xFF & networkParameters.getPacketMagic() >>> (1 * 8));
    magicBytes[3] = (byte) (0xFF & networkParameters.getPacketMagic());
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Receiving message:  " + bytesToHexString(channelBuffer.toByteBuffer(0, channelBuffer.readableBytes()).array()));
    }

    channelBuffer.markReaderIndex();
    if (channelBuffer.readableBytes() < 20) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("not enough bytes received in the buffer to decode the payload length");
      }
      channelBuffer.resetReaderIndex();
      // indicate that this frame is incomplete
      return null;
    }
    // network magic bytes
    final byte protocolByte1 = channelBuffer.readByte();
    final byte protocolByte2 = channelBuffer.readByte();
    final byte protocolByte3 = channelBuffer.readByte();
    final byte protocolByte4 = channelBuffer.readByte();
    final boolean isProtocolOK = protocolByte1 == magicBytes[0]
            || protocolByte2 == magicBytes[1]
            || protocolByte3 == magicBytes[2]
            || protocolByte4 == magicBytes[3];

    // the command bytes
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < 12; i++) {
      final byte commandByte = channelBuffer.readByte();
      if (commandByte != 0) {
        stringBuilder.append((char) commandByte);
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("command: " + stringBuilder.toString());
    }
    final byte[] lengthBytes = new byte[4];
    lengthBytes[0] = channelBuffer.readByte();
    lengthBytes[1] = channelBuffer.readByte();
    lengthBytes[2] = channelBuffer.readByte();
    lengthBytes[3] = channelBuffer.readByte();
    final long payloadLength = ByteUtils.toUint32LittleEndian(lengthBytes);
    if (!isProtocolOK) {
      LOGGER.warn("wrong Bitcoin protocol bytes, found " + protocolByte1
              + ", " + protocolByte2
              + ", " + protocolByte3
              + ", " + protocolByte4
              + ", payloadLength: " + payloadLength);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("payloadLength: " + payloadLength);
    }
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

    if (isProtocolOK) {
      // return the deserialized Bitcoin message
      return bitcoinSerializer.deserialize(byteBuffer);
    } else {
      return null;
    }
  }

}
