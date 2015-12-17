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

  // the HTTP request handler
  final HTTPRequestHandler httpRequestHandler;

  /**
   * Constructs a new HTTPRequestHandlerFactory instance.
   *
   * @param httpRequestHandler the HTTP request handler
   */
  public HTTPRequestHandlerFactory(final HTTPRequestHandler httpRequestHandler) {
    //Preconditions
    assert httpRequestHandler != null : "httpRequestHandler must not be null";

    this.httpRequestHandler = httpRequestHandler;
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractHTTPRequestHandler getHandler() {
    return httpRequestHandler;
  }
}
