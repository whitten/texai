/*
 * MockHTTPRequestHandlerFactory.java
 *
 * Description: .
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class MockHTTPRequestHandlerFactory extends AbstractHTTPRequestHandlerFactory {

  /**
   * Constructs a new MockHTTPRequestHandlerFactory instance.
   */
  public MockHTTPRequestHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractHTTPRequestHandler getHandler() {
    return new MockHTTPRequestHandler();
  }
}
