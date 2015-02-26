/*
 * AbstractBitcoinProtocolMessageHandler.java
 *
 * Description: Provides an abstract Bitcoin protocol message handler.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Provides an abstract Bitcoin protocol message handler.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractBitcoinProtocolMessageHandler extends SimpleChannelHandler {

  /**
   * Constructs a new AbstractBitcoinProtocolMessageHandler instance.
   */
  public AbstractBitcoinProtocolMessageHandler() {
  }
}
