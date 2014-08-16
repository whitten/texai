/*
 * BitTorrentClientPipelineFactory.java
 *
 * Created on Feb 11, 2010, 5:02:32 PM
 *
 * Description: Provides a bit torrent client pipeline factory.
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
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.texai.network.netty.handler.AbstractBitTorrentHandler;
import org.texai.network.netty.handler.BitTorrentDecoder;
import org.texai.network.netty.handler.BitTorrentEncoder;
import org.texai.x509.X509SecurityInfo;

/** Provides a bit torrent client pipeline factory.
 *
 * @author reed
 */
@NotThreadSafe
public final class BitTorrentClientPipelineFactory {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentClientPipelineFactory.class);

  /** Prevents this utility class from being instantiated. */
  private BitTorrentClientPipelineFactory() {
  }

  /** Returns a client pipeline to handle bit torrent messages.
   *
   * @param bitTorrentHandler the bit torrent message handler
   * @param x509SecurityInfo the X.509 security information
   * @return the configured pipeline
   */
  public static ChannelPipeline getPipeline(
          final AbstractBitTorrentHandler bitTorrentHandler,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert bitTorrentHandler != null : "bitTorrentHandler must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    final ChannelPipeline channelPipeline = SSLPipelineFactory.getPipeline(
            true, // useClientMode
            x509SecurityInfo,
            true); // needClientAuth
    channelPipeline.addLast("decoder", new BitTorrentDecoder());
    channelPipeline.addLast("encoder", new BitTorrentEncoder());
    channelPipeline.addLast("torrent-handler", bitTorrentHandler);
    LOGGER.info("configured bit torrent pipeline: " + channelPipeline);
    return channelPipeline;
  }
}
