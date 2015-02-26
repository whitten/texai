/*
 * MockBitcoinProtocolMessageHandler.java
 *
 * Description: A channel handler that transports Bitcoin protocol messages.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import com.google.bitcoin.core.Message;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.util.TexaiException;

/**
 * A channel handler that transports Bitcoin protocol messages.
 *
 * @author reed
 */
@NotThreadSafe
public final class MockBitcoinProtocolMessageHandler extends AbstractBitcoinProtocolMessageHandler {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(MockBitcoinProtocolMessageHandler.class);
  // the lock that allows the client to resume when the messaging is done
  final Object clientResume_lock;

  /**
   * Constructs a new MockBitcoinProtocolMessageHandler instance.
   *
   * @param clientResume_lock the lock that allows the client to resume when the messaging is done, or null if this is the client side
   * handler
   */
  public MockBitcoinProtocolMessageHandler(final Object clientResume_lock) {

    this.clientResume_lock = clientResume_lock;
  }

  /**
   * Receives a message object from a remote peer.
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

    final boolean isClient = clientResume_lock == null;
    if (isClient) {
      LOGGER.info("client received messageEvent: " + messageEvent);
    } else {
      LOGGER.info("server received messageEvent: " + messageEvent);
    }
    final Channel channel = channelHandlerContext.getChannel();
    assert messageEvent.getMessage() instanceof Message;
    final Message message = (Message) messageEvent.getMessage();
    LOGGER.info("Bitcoin message received...\n" + message);
    if (clientResume_lock != null) {
      synchronized (clientResume_lock) {
        clientResume_lock.notifyAll();
      }
    }
  }

  /**
   * Writes a message event to a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void writeRequested(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    LOGGER.info("write requested messageEvent: " + messageEvent);
    LOGGER.info("pipeline: " + channelHandlerContext.getPipeline().toString());
    channelHandlerContext.sendDownstream(messageEvent);
  }

  /**
   * Handles a caught exception.
   *
   * @param channelHandlerContext the channel handler event
   * @param exceptionEvent the exception event
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    if (clientResume_lock == null) {
      LOGGER.error("server exceptionEvent: " + exceptionEvent);
    } else {
      LOGGER.error("client exceptionEvent: " + exceptionEvent);
    }
    throw new TexaiException(exceptionEvent.getCause());
  }
}
