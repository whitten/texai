/*
 * HTTPClientPipelineFactory.java
 *
 * Created on Feb 8, 2010, 1:04:11 PM
 *
 * Description: Configures a client pipeline to handle HTTP messages.
 *
 * Copyright (C) Feb 8, 2010 reed.
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
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.x509.X509SecurityInfo;

/** Configures a client pipeline to handle HTTP messages.
 *
 * @author reed
 */
@NotThreadSafe
public final class HTTPClientPipelineFactory {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPClientPipelineFactory.class);
  /** the sharable HTTP request encoder */
  private static final HttpRequestEncoder HTTP_REQUEST_ENCODER = new HttpRequestEncoder();
  /** the sharable HTTP response decoder */
  private static final ChannelHandler HTTP_RESPONSE_DECODER = new HttpResponseDecoder();

  /** Prevents this utility class from being instantiated. */
  private HTTPClientPipelineFactory() {
  }

  /** Creates a client pipeline to handle HTTP messages.
   *
   * @param httpResponseHandler the HTTP response handler
   * @param x509SecurityInfo the X.509 security information
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
            false); // needClientAuth
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
