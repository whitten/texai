/*
 * TexaiHTTPRequestHandler.java
 *
 * Created on Feb 8, 2010, 9:30:14 PM
 *
 * Description: Defines a Texai HTTP request handler.
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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

/** Defines a Texai HTTP request handler.  The handlers are arranged in a chain such that a handler returns false if
 * it cannot process the given HTTP request, and returns true after successfully handling the request.  The driving
 * HTTP request handler that contains the chain stops processing when a handler in the chain returns true.
 *
 * @author reed
 */
public interface TexaiHTTPRequestHandler {

  /** Handles the HTTP request.
   *
   * @param httpRequest the HTTP request
   * @param channel the channel
   * @return the indicator whether the HTTP request was handled
   */
  boolean httpRequestReceived(
          final HttpRequest httpRequest,
          final Channel channel);

  /** Handles a received text web socket frame.
   *
   * @param channel the channel handler context
   * @param textWebSocketFrame  the text web socket frame
   * @return the indicator whether the web socket request was handled
   */
  boolean textWebSocketFrameReceived(
          final Channel channel,
          final TextWebSocketFrame textWebSocketFrame);
}
