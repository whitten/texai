/*
 * AlbusHCSMessageHandler.java
 *
 * Created on Feb 8, 2010, 9:42:40 PM
 *
 * Description: Provides an Albus hierarchical control system message handler.
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
package org.texai.ahcs;

import org.texai.ahcsSupport.Message;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;

/** Provides an Albus hierarchical control system message handler.
 *
 * @author reed
 */
@ThreadSafe
public class AlbusHCSMessageHandler extends AbstractAlbusHCSMessageHandler {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(AlbusHCSMessageHandler.class);
  /** the recipient dictionary, role info -->   TODO */


  /** Constructs a new AlbusHCSMessageHandler instance. */
  public AlbusHCSMessageHandler() {
  }

  /** Receives a message object from a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";
    assert messageEvent.getMessage() instanceof Message;

    final Message message = (Message) messageEvent.getMessage();
    final Channel channel = channelHandlerContext.getChannel();

    //TODO
  }

}
