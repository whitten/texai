/*
 * AbstractBitcoinProtocolMessageHandler.java
 *
 * Description: Provides an abstract Bitcoin protocol message handler.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import com.google.bitcoin.core.NetworkParameters;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * Provides an abstract Bitcoin protocol message handler.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractBitcoinProtocolMessageHandler extends SimpleChannelHandler {

  // the network parameters, e.g. MainNetParams or TestNet3Params
  private final NetworkParameters networkParameters;

  /**
   * Constructs a new AbstractBitcoinProtocolMessageHandler instance.
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   */
  public AbstractBitcoinProtocolMessageHandler(final NetworkParameters networkParameters) {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";

    this.networkParameters = networkParameters;
  }

  /** Gets the network parameters, e.g. MainNetParams or TestNet3Params.
   * @return the network parameters
   */
  public NetworkParameters getNetworkParameters() {
    return networkParameters;
  }
}
