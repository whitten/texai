/*
 * HTTPSClientPipelineFactory.java
 *
 * Description: Configures a client pipeline to handle HTTP messages.
 *
 * Copyright (C) Feb 8, 2010 by Stephen Reed.
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
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.x509.X509SecurityInfo;

/**
 * Configures a client pipeline to handle HTTP messages.
 *
 * @author reed
 */
@NotThreadSafe
public final class HTTPSClientPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(HTTPSClientPipelineFactory.class);
  // the sharable HTTP request encoder
  private static final HttpRequestEncoder HTTP_REQUEST_ENCODER = new HttpRequestEncoder();
  // the sharable HTTP response decoder
  private static final ChannelHandler HTTP_RESPONSE_DECODER = new HttpResponseDecoder();

  /**
   * Prevents this utility class from being instantiated.
   */
  private HTTPSClientPipelineFactory() {
  }

  /**
   * Creates a client pipeline to handle HTTP messages.
   *
   * @param httpResponseHandler the HTTP response handler
   * @param x509SecurityInfo the X.509 security information
   *
   * @return the configured pipeline
   */
  public static ChannelPipeline getPipeline(
          final AbstractHTTPResponseHandler httpResponseHandler,
          final X509SecurityInfo x509SecurityInfo) {
    //Precondtions
    assert httpResponseHandler != null : "httpResponseHandler must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    final ChannelPipeline configuredPipeline = SSLPipelineFactory.getPipeline(
            true, // useClientMode
            x509SecurityInfo,
            false, // needClientAuth
            !x509SecurityInfo.isPublicCertificate()); // isStrongCiphers
    configuredPipeline.addLast("encoder", HTTP_REQUEST_ENCODER);
    configuredPipeline.addLast("decoder", HTTP_RESPONSE_DECODER);
    configuredPipeline.addLast("aggregator", new HttpChunkAggregator(1048576));
    configuredPipeline.addLast("http-handler", httpResponseHandler);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("configured HTTP client pipeline: " + configuredPipeline);
    }
    return configuredPipeline;
  }
}
