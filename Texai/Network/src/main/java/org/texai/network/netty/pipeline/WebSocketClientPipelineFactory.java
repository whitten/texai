/*
 * WebSocketClientPipelineFactory.java
 *
 * Description: Provides a web socket client pipeline factory.
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed.
 *
 */
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.texai.network.netty.handler.AbstractWebSocketResponseHandler;
import org.texai.x509.X509SecurityInfo;

/**
 * Provides a web socket client pipeline factory.
 *
 * @author reed
 */
@NotThreadSafe
public class WebSocketClientPipelineFactory {

  // the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPSClientPipelineFactory.class);
  // the sharable HTTP request encoder */
  private static final HttpRequestEncoder HTTP_REQUEST_ENCODER = new HttpRequestEncoder();
  // the sharable HTTP response decoder */
  private static final ChannelHandler HTTP_RESPONSE_DECODER = new HttpResponseDecoder();

  /**
   * Prevents the construction of an instance.
   */
  private WebSocketClientPipelineFactory() {
  }

  /**
   * Creates a client pipeline to handle HTTP messages.
   *
   * @param webSocketResponseHandler the web socket response handler
   * @param x509SecurityInfo the X.509 security information
   *
   * @return the configured pipeline
   */
  public static ChannelPipeline getPipeline(
          final AbstractWebSocketResponseHandler webSocketResponseHandler,
          final X509SecurityInfo x509SecurityInfo) {
    //Precondtions
    assert webSocketResponseHandler != null : "webSocketResponseHandler must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    final ChannelPipeline configuredPipeline = SSLPipelineFactory.getPipeline(
            true, // useClientMode
            x509SecurityInfo,
            false, // needClientAuth
            true); // isStrongCiphers
    configuredPipeline.addLast("encoder", HTTP_REQUEST_ENCODER);
    configuredPipeline.addLast("decoder", HTTP_RESPONSE_DECODER);
    configuredPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    configuredPipeline.addLast("ws-handler", webSocketResponseHandler);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("configured HTTP client pipeline: " + configuredPipeline);
    }
    return configuredPipeline;
  }
}
