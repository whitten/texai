/*
 * WebSocketServerPipelineFactory.java
 *
 * Created on Jul 8, 2012, 8:07:03 PM
 *
 * Description: Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * Copyright (C) Jul 8, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.network.netty.pipeline;

import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import static org.jboss.netty.channel.Channels.pipeline;

/** Initializes the ChannelPipeline of the child channel accepted by a ServerChannel.
 *
 * @author reed
 */
@NotThreadSafe
public class WebSocketServerPipelineFactory implements ChannelPipelineFactory {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(WebSocketServerPipelineFactory.class);
  /** the HTTP request handler factory */
  private final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory;


  /** Constructs a new WebSocketServerPipelineFactory instance.
   *
   * @param httpRequestHandlerFactory the HTTP request handler factory
   */
  public WebSocketServerPipelineFactory(final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory) {
    //Preconditions
    assert httpRequestHandlerFactory != null : "httpRequestHandlerFactory must not be null";

    this.httpRequestHandlerFactory = httpRequestHandlerFactory;
  }

  /** Returns a newly created {@link ChannelPipeline}.
   *
   * @return a channel pipeline for the child channel accepted by a server channel
   */
  @Override
  public ChannelPipeline getPipeline() {
    // if this pipeline only expects HTTP messages, then configure it not to require client X509 certificates
    final ChannelPipeline channelPipeline = pipeline();
    channelPipeline.addLast("encoder", new HttpResponseEncoder());
    channelPipeline.addLast("decoder", new HttpRequestDecoder());
    channelPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    channelPipeline.addLast("http-request-handler", httpRequestHandlerFactory.getHandler());
    return channelPipeline;
  }
}
