/*
 * PortUnificationChannelPipelineFactory.java
 *
 * Description: Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.PortUnificationHandler;
import org.texai.x509.X509SecurityInfo;

/**
 * Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * @author reed
 */
@NotThreadSafe
public class PortUnificationChannelPipelineFactory implements ChannelPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(PortUnificationChannelPipelineFactory.class);
  // the Albus HCN message handler factory
  private final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory;
  // the HTTP request handler factory
  private final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory;
  // the X.509 security information
  private final X509SecurityInfo x509SecurityInfo;

  /**
   * Constructs a new PortUnificationChannelPipelineFactory instance.
   *
   * @param albusHCSMessageHandlerFactory the Albus HCN message handler factory
   * @param httpRequestHandlerFactory the HTTP request handler factory
   * @param x509SecurityInfo the X.509 security information
   *
   */
  public PortUnificationChannelPipelineFactory(
          final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory,
          final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    this.albusHCSMessageHandlerFactory = albusHCSMessageHandlerFactory;
    this.httpRequestHandlerFactory = httpRequestHandlerFactory;
    this.x509SecurityInfo = x509SecurityInfo;
  }

  /**
   * Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    final PortUnificationHandler portUnificationHandler = new PortUnificationHandler();
    if (albusHCSMessageHandlerFactory != null) {
      portUnificationHandler.setAlbusHCNMessageHandler(albusHCSMessageHandlerFactory.getHandler());
    }
    if (httpRequestHandlerFactory != null) {
      portUnificationHandler.setHttpRequestHandler(httpRequestHandlerFactory.getHandler());
    }
    // if this pipeline only expects HTTP messages, then configure it not to require client X509 certificates
    final boolean needClientAuth = albusHCSMessageHandlerFactory != null
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
