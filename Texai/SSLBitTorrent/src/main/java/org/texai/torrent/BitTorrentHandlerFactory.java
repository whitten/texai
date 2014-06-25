/*
 * BitTorrentHandlerFactory.java
 *
 * Created on Mar 2, 2010, 4:13:04 PM
 *
 * Description: Provides a bit torrent message handler factory for a server pipeline.
 *
 * Copyright (C) Mar 2, 2010 reed.
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
package org.texai.torrent;

import net.jcip.annotations.NotThreadSafe;
import org.texai.network.netty.handler.AbstractBitTorrentHandler;
import org.texai.network.netty.handler.AbstractBitTorrentHandlerFactory;

/** Provides a bit torrent message handler factory for a server pipeline.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentHandlerFactory extends AbstractBitTorrentHandlerFactory {

  /** the SSL torrent */
  private final SSLTorrent sslTorrent;

  /** Constructs a new BitTorrentHandlerFactory instance.
   *
   * @param sslTorrent the SSL torrent
   */
  public BitTorrentHandlerFactory(final SSLTorrent sslTorrent) {
    //Preconditions
    assert sslTorrent != null : "sslTorrent must not be null";

    this.sslTorrent = sslTorrent;
  }

  /** Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractBitTorrentHandler getHandler() {
    return new BitTorrentHandler(sslTorrent);
  }
}
