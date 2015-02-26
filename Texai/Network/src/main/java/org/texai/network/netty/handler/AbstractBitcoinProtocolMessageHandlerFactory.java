/*
 * AbstractBitcoinProtocolMessageHandlerFactory.java
 *
 * Description: Provides a Bitcoin protocol message handler factory.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a Bitcoin protocol message handler factory.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractBitcoinProtocolMessageHandlerFactory implements ChannelHandlerFactory {

  /**
   * Constructs a new AbstractBitcoinProtocolMessageHandlerFactory instance.
   */
  public AbstractBitcoinProtocolMessageHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public abstract AbstractBitcoinProtocolMessageHandler getHandler();
}
