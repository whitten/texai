/*
 * AbstractAlbusHCSMessageHandlerFactory.java
 *
 * Description: Provides an Albus hierarchical control network message handler factory.
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 * Provides an Albus hierarchical control network message handler factory.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractAlbusHCSMessageHandlerFactory implements ChannelHandlerFactory {

  /**
   * Constructs a new AbstractAlbusHCSMessageHandlerFactory instance.
   */
  public AbstractAlbusHCSMessageHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public abstract AbstractAlbusHCSMessageHandler getHandler();
}
