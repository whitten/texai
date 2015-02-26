/*
 * AbstractHTTPRequestHandlerFactory.java
 *
 * Description: Provides a HTTP request handler factory.
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a HTTP request handler factory.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractHTTPRequestHandlerFactory implements ChannelHandlerFactory {

  /**
   * Constructs a new AbstractHTTPRequestHandlerFactory instance.
   */
  public AbstractHTTPRequestHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public abstract AbstractHTTPRequestHandler getHandler();
}
