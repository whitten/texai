/*
 * AbstractHTTPResponseHandler.java
 *
 * Description: Provides an abstract HTTP response handler.
 *
 * Copyright (C) Feb 8, 2010 by Stephen Reed
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * Provides an abstract HTTP response handler.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractHTTPResponseHandler extends SimpleChannelUpstreamHandler {

  /**
   * Constructs a new AbstractHTTPResponseHandler instance.
   */
  public AbstractHTTPResponseHandler() {
  }
}
