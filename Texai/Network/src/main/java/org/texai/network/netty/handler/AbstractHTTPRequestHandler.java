/*
 * AbstractHTTPRequestHandler.java
 *
 * Description: Provides an abstract HTTP request handler.
 *
 * Copyright (C) Feb 8, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.ThreadSafe;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Provides an abstract HTTP request handler. Subclasses must be thread-safe.
 *
 * @author reed
 */
@ThreadSafe
public abstract class AbstractHTTPRequestHandler extends SimpleChannelUpstreamHandler {

  /**
   * Constructs a new AbstractHTTPRequestHandler instance.
   */
  public AbstractHTTPRequestHandler() {
  }
}
