/*
 * AbstractAlbusHCSMessageHandler.java
 *
 * Description: Provides an abstract Albus hierarchical control network message handler.
 *
 * Copyright (C) Feb 8, 2010 by Stephen Reed.
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Provides an abstract Albus hierarchical control network message handler.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractAlbusHCSMessageHandler extends SimpleChannelHandler {

  /**
   * Constructs a new AbstractAlbusHCSMessageHandler instance.
   */
  public AbstractAlbusHCSMessageHandler() {
  }
}
