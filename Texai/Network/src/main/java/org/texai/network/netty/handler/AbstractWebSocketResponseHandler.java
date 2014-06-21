/*
 * AbstractWebSocketResponseHandler.java
 *
 * Created on Jan 30, 2012, 12:17:41 PM
 *
 * Description: Provides an abstract web socket response handler.
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/** Provides an abstract web socket response handler.
 *
 * @author reed
 */
@NotThreadSafe
public class AbstractWebSocketResponseHandler extends SimpleChannelUpstreamHandler {

  /** Constructs a new AbstractWebSocketResponseHandler instance. */
  public AbstractWebSocketResponseHandler() {
  }
}
