/*
 * AlbusHCNMessageClientPipelineFactory.java
 *
 * Created on Feb 4, 2010, 5:24:17 PM
 *
 * Description: Configures a client pipeline to handle Albus hierarchical control system messages, which are
 * serialized Java objects.
 *
 * Copyright (C) Feb 4, 2010 reed.
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
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.TaggedObjectDecoder;
import org.texai.network.netty.handler.TaggedObjectEncoder;
import org.texai.x509.X509SecurityInfo;

/** Configures a client pipeline to handle Albus hierarchical control system messages, which are
 * serialized Java objects.
 *
 * @author reed
 */
@NotThreadSafe
public final class AlbusHCNMessageClientPipelineFactory {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(AlbusHCNMessageClientPipelineFactory.class);

  /** Prevents this utility class from being instantiated. */
  private AlbusHCNMessageClientPipelineFactory() {
  }

  /** Returns a client pipeline to handle Albus hierarchical control system messages, which are
   * serialized Java objects.
   *
   * @param albusHCNMessageHandler the Albus HCN message handler
   * @param x509SecurityInfo the X.509 security information
   * @return the configured pipeline
   */
  public static ChannelPipeline getPipeline(
          final AbstractAlbusHCSMessageHandler albusHCNMessageHandler,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert albusHCNMessageHandler != null : "albusHCNMessageHandler must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    final ChannelPipeline channelPipeline = SSLPipelineFactory.getPipeline(
            true, // useClientMode
            x509SecurityInfo,
            true); // needClientAuth
    channelPipeline.addLast("decoder", new TaggedObjectDecoder());
    channelPipeline.addLast("encoder", new TaggedObjectEncoder());
    channelPipeline.addLast("albus-handler", albusHCNMessageHandler);
    LOGGER.info("configured Albus HCN pipeline: " + channelPipeline);
    return channelPipeline;
  }
}
