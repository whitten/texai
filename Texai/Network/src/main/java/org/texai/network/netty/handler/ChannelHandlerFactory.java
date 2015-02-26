/*
 * PortUnificationChannelPipelineFactory.java
 *
 * Description: Defines a channel handler factory.
 *
 * Copyright (C) Feb 9, 2010 Stephen Reed
 *
 */
package org.texai.network.netty.handler;

import org.jboss.netty.channel.ChannelHandler;

/**
 * Defines a channel handler factory.
 *
 * @author reed
 */
public interface ChannelHandlerFactory {

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  ChannelHandler getHandler();

}
