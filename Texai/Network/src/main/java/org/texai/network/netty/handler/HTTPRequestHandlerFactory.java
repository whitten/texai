/*
 * HTTPRequestHandlerFactory.java
 *
 * Description: Provides a HTTP request handler factory.
 *
 * Copyright (C) Feb 11, 2010 by Stephen Reed.
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
public class HTTPRequestHandlerFactory extends AbstractHTTPRequestHandlerFactory {

  /**
   * Constructs a new HTTPRequestHandlerFactory instance.
   */
  public HTTPRequestHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractHTTPRequestHandler getHandler() {
    return HTTPRequestHandler.getInstance();
  }
}
