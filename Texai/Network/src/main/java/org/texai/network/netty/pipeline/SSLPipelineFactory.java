/*
 * SSLPipelineFactory.java
 *
 * Description: Configures a given pipeline, or initializes a new pipeline, so that it consists of a single
 * SslHandler.
 *
 * Copyright (C) Feb 4, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import static org.jboss.netty.channel.Channels.pipeline;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.ssl.SslHandler;
import org.texai.ssl.TexaiSSLContextFactory;
import org.texai.x509.X509SecurityInfo;

/**
 * Configures a given pipeline, or initializes a new pipeline, so that it consists of a single SslHandler.
 *
 * @author reed
 */
@NotThreadSafe
public final class SSLPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SSLPipelineFactory.class);

  /**
   * Prevents the instantiation of this utility class.
   */
  private SSLPipelineFactory() {
  }

  /**
   * Creates a new pipeline in which a SslHandler is the sole handler.
   *
   * @param useClientMode the indicator whether the SSL engine is operating in client mode
   * @param x509SecurityInfo the X.509 security information
   * @param needClientAuth the indicator whether the server authenticates the client's SSL certificate
   *
   * @return the configured pipeline having a SslHandler is the sole handler
   */
  public static ChannelPipeline getPipeline(
          final boolean useClientMode,
          final X509SecurityInfo x509SecurityInfo,
          final boolean needClientAuth) {
    // Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    final ChannelPipeline configuredPipeline;
    // create and configure a new pipeline
    configuredPipeline = pipeline();
    final SSLEngine sslEngine = getConfiguredSSLEngine(
            useClientMode,
            x509SecurityInfo,
            needClientAuth);
    final SslHandler sslHandler = new SslHandler(sslEngine);
    configuredPipeline.addFirst("ssl", sslHandler);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("configured new SSL pipeline: " + configuredPipeline);
    }
    return configuredPipeline;
  }

  /**
   * Creates and configures an SSL engine.
   *
   * @param useClientMode the indicator whether the SSL engine is operating in client mode
   * @param x509SecurityInfo the X.509 security information
   * @param needClientAuth te indicator whether the SSL client is authenticated by the server
   *
   * @return the configured SSL engine
   */
  private static SSLEngine getConfiguredSSLEngine(
          final boolean useClientMode,
          final X509SecurityInfo x509SecurityInfo,
          final boolean needClientAuth) {
    // Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
    assert x509SecurityInfo.isDigitialSigniatureCertificate() : "x509SecurityInfo must be usable for digital signatures";

    final SSLContext sslContext = TexaiSSLContextFactory.getSSLContext(x509SecurityInfo);
    final SSLEngine sslEngine = sslContext.createSSLEngine();
    TexaiSSLContextFactory.configureSSLEngine(sslEngine, useClientMode, needClientAuth);
    return sslEngine;
  }
}
