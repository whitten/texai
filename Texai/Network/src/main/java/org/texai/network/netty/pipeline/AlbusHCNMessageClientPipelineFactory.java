/*
 * AlbusHCNMessageClientPipelineFactory.java
 *
 * Description: Configures a client pipeline to handle Albus hierarchical control system messages, which are
 * serialized Java objects.
 *
 * Copyright (C) Feb 4, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.pipeline;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelPipeline;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.TaggedObjectDecoder;
import org.texai.network.netty.handler.TaggedObjectEncoder;
import org.texai.x509.X509SecurityInfo;

/**
 * Configures a client pipeline to handle Albus hierarchical control system messages, which are serialized Java objects.
 *
 * @author reed
 */
@NotThreadSafe
public final class AlbusHCNMessageClientPipelineFactory {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AlbusHCNMessageClientPipelineFactory.class);

  /**
   * Prevents this utility class from being instantiated.
   */
  private AlbusHCNMessageClientPipelineFactory() {
  }

  /**
   * Returns a client pipeline to handle Albus hierarchical control system messages, which are serialized Java objects.
   *
   * @param albusHCNMessageHandler the Albus HCN message handler
   * @param x509SecurityInfo the X.509 security information
   *
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
            true, // needClientAuth
            !x509SecurityInfo.isPublicCertificate()); // isStrongCiphers
    channelPipeline.addLast("decoder", new TaggedObjectDecoder());
    channelPipeline.addLast("encoder", new TaggedObjectEncoder());
    channelPipeline.addLast("albus-handler", albusHCNMessageHandler);
    LOGGER.info("configured Albus HCN pipeline: " + channelPipeline);
    return channelPipeline;
  }
}
