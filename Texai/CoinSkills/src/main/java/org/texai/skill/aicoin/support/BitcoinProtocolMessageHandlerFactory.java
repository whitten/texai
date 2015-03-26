package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandlerFactory;
import org.texai.network.netty.utils.ConnectionUtils;
import org.texai.util.NetworkUtils;

/**
 * BitcoinProtocolMessageHandlerFactory.java

 Description: Opens a listening socket for Bitcoin protocol messages, and creates Bitcoin protocol message handling proxies for each new
 connection.
 *
 * Copyright (C) Mar 21, 2015, Stephen L. Reed.
 */
public class BitcoinProtocolMessageHandlerFactory extends AbstractBitcoinProtocolMessageHandlerFactory {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(BitcoinProtocolMessageHandlerFactory.class);
  // the server bootstrap
  private ServerBootstrap serverBootstrap;
  // the node runtime
  private final BasicNodeRuntime nodeRuntime;
  // the network parameters, main net, test net, or regression test net
  private final NetworkParameters networkParameters;
  // the Bitcoin protocol message handler dictionary, remote socket address -> Bitcoin protocol message handler
  private final Map<SocketAddress, BitcoinProtocolMessageHandler> bitcoinProtocolMessageHandlerDictionary = new HashMap<>();

  /**
   * Creates a new instance of RemoteBitcoinProtocolPeerListener.
   *
   * @param networkParameters the network parameters, main net, test net, or regression test net
   * @param nodeRuntime the node runtime
   */
  public BitcoinProtocolMessageHandlerFactory(
          final NetworkParameters networkParameters,
          final BasicNodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "the nodeRuntime must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_MAINNET) && MainNetParams.class.isAssignableFrom(networkParameters.getClass()))
            || (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_TESTNET) && TestNet3Params.class.isAssignableFrom(networkParameters.getClass()));

    this.networkParameters = networkParameters;
    this.nodeRuntime = nodeRuntime;
  }

  /**
   * Opens the Bitcoin protocol listening socket.
   */
  public void openBitcoinProtocolListeningSocket() {
    serverBootstrap = ConnectionUtils.createBitcoinProtocolServer(
            networkParameters.getPort(),
            this, // bitcoinProtocolMessageHandlerFactory
            networkParameters,
            nodeRuntime.getExecutor(), // bossExecutor
            nodeRuntime.getExecutor()); // workerExecutor
  }

  /**
   * Closes the Bitcoin protocol listening socket.
   */
  public void closeBitcoinProtocolListeningSocket() {
    ConnectionUtils.closeBitcoinProtocolServer(serverBootstrap);  // sometimes hangs up
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractBitcoinProtocolMessageHandler getHandler() {
    return new BitcoinProtocolMessageHandler(
            networkParameters,
            nodeRuntime,
            bitcoinProtocolMessageHandlerDictionary);
  }

  /**
   * Gets the Bitcoin protocol message proxy dictionary.
   *
   * @return the Bitcoin protocol message proxy dictionary
   */
  protected Map<SocketAddress, BitcoinProtocolMessageHandler> getBitcoinProtocolMessageProxyDictionary() {
    return bitcoinProtocolMessageHandlerDictionary;
  }
}
