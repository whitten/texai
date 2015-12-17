package org.texai.network.netty.pipeline;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;

/**
 * HTTPServerPipelineFactory.java
 *
 * Description: Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * Copyright (C) Dec 14, 2015, Stephen L. Reed.
 */
public class HTTPServerPipelineFactory implements ChannelPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(HTTPServerPipelineFactory.class);
  // the HTTP request handler factory
  private final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory;

  /**
   * Constructs a new HTTPServerPipelineFactory instance.
   *
   * @param httpRequestHandlerFactory the HTTP request handler factory
   */
  public HTTPServerPipelineFactory(final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory) {
    //Preconditions
    assert httpRequestHandlerFactory != null : "httpRequestHandlerFactory must not be null";

    this.httpRequestHandlerFactory = httpRequestHandlerFactory;
  }

  /**
   * Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    final ChannelPipeline channelPipeline = pipeline();
    channelPipeline.addLast("encoder", new HttpResponseEncoder());
    channelPipeline.addLast("decoder", new HttpRequestDecoder());
    channelPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    channelPipeline.addLast("http-request-handler", httpRequestHandlerFactory.getHandler());
    return channelPipeline;
  }
}
