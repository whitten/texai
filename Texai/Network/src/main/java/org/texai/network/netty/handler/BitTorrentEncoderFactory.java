/*
 * BitTorrentEncoderFactory.java
 *
 * Created on Feb 11, 2010, 3:47:37 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 11, 2010 reed.
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

import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author reed
 */
@NotThreadSafe
public final class BitTorrentEncoderFactory implements ChannelHandlerFactory {

  /** Constructs a new BitTorrentEncoderFactory instance. */
  public BitTorrentEncoderFactory() {
  }

  /** Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public BitTorrentEncoder getHandler() {
    return new BitTorrentEncoder();
  }
}
