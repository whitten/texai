/*
 * AbstractHTTPRequestHandler.java
 *
 * Created on Feb 8, 2010, 9:30:14 PM
 *
 * Description: Provides an abstract HTTP request handler.
 *
 * Copyright (C) Feb 8, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.ThreadSafe;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/** Provides an abstract HTTP request handler. Subclasses must be thread-safe.
 *
 * @author reed
 */
@ThreadSafe
public abstract class AbstractHTTPRequestHandler extends SimpleChannelUpstreamHandler {

  /** Constructs a new AbstractHTTPRequestHandler instance. */
  public AbstractHTTPRequestHandler() {
  }
}
