/*
 * PortUnificationChannelPipelineFactory.java
 *
 * Created on Feb 9, 2010, 11:05:07 AM
 *
 * Description: Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * Copyright (C) Feb 9, 2010 reed.
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
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractBitTorrentHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.PortUnificationHandler;
import org.texai.x509.X509SecurityInfo;

/** Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * @author reed
 */
@NotThreadSafe
public class PortUnificationChannelPipelineFactory implements ChannelPipelineFactory {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PortUnificationChannelPipelineFactory.class);
  /** the Albus HCN message handler factory */
  private final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory;
  /** the bit torrent handler factory */
  private final AbstractBitTorrentHandlerFactory bitTorrentHandlerFactory;
  /** the HTTP request handler factory */
  private final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory;
  /** the X.509 security information */
  private final X509SecurityInfo x509SecurityInfo;

  /** Constructs a new PortUnificationChannelPipelineFactory instance.
   * @param albusHCSMessageHandlerFactory the Albus HCN message handler factory
   * @param bitTorrentHandlerFactory the bit torrent handler factory
   * @param httpRequestHandlerFactory the HTTP request handler factory
   * @param x509SecurityInfo the X.509 security information
   *
   */
  public PortUnificationChannelPipelineFactory(
          final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory,
          final AbstractBitTorrentHandlerFactory bitTorrentHandlerFactory,
          final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    this.albusHCSMessageHandlerFactory = albusHCSMessageHandlerFactory;
    this.bitTorrentHandlerFactory = bitTorrentHandlerFactory;
    this.httpRequestHandlerFactory = httpRequestHandlerFactory;
    this.x509SecurityInfo = x509SecurityInfo;
  }

  /** Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    final PortUnificationHandler portUnificationHandler = new PortUnificationHandler();
    if (albusHCSMessageHandlerFactory != null) {
      portUnificationHandler.setAlbusHCNMessageHandler(albusHCSMessageHandlerFactory.getHandler());
    }
    if (bitTorrentHandlerFactory != null) {
      portUnificationHandler.setBitTorrentHandler(bitTorrentHandlerFactory.getHandler());
    }
    if (httpRequestHandlerFactory != null) {
      portUnificationHandler.setHttpRequestHandler(httpRequestHandlerFactory.getHandler());
    }
    // if this pipeline only expects HTTP messages, then configure it not to require client X509 certificates
    final boolean needClientAuth = albusHCSMessageHandlerFactory != null
            || bitTorrentHandlerFactory != null
            || httpRequestHandlerFactory == null;
    final ChannelPipeline channelPipeline = SSLPipelineFactory.getPipeline(
            false, // useClientMode
            x509SecurityInfo,
            needClientAuth);
    channelPipeline.addLast("port-unification", portUnificationHandler);
    LOGGER.info(channelPipeline);
    return channelPipeline;
  }
}
