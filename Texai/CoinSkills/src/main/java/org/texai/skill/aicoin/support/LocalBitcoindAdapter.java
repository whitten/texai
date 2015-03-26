package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.GetAddrMessage;
import com.google.bitcoin.core.MemoryPoolMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.Ping;
import com.google.bitcoin.core.VersionMessage;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import java.net.InetSocketAddress;
import java.util.Random;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.utils.ConnectionUtils;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Created on Sep 9, 2014, 7:41:47 AM.
 *
 * Description: Provides a Bitcoin protocol adapter to the local bitcoind instance.
 *
 * Copyright (C) Sep 9, 2014, Stephen L. Reed, Texai.org.
 */
@NotThreadSafe
public class LocalBitcoindAdapter extends SimpleChannelHandler {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(LocalBitcoindAdapter.class);
  // the bitcoin message receiver, which is the skill that handles outbound bitcoin messages from the local peer
  private final BitcoinMessageReceiver bitcoinMessageReceiver;
  // the network parameters, main net, test net, or regression test net
  private final NetworkParameters networkParameters;
  // the Bitcoin protocol message handler
  private final BitcoinProtocolMessageHandler bitcoinProtocolMessageHandler;
  // the node runtime
  private final BasicNodeRuntime nodeRuntime;
  // the communications channel with the local bitcoind instance
  private Channel channel;
  // the random number used for ping nonces
  private final Random random = new Random();

  /**
   * Constructs a new LocalBitcoindAdapter instance.
   *
   * @param networkParameters the network parameters, main net, test net, or regression test net
   * @param bitcoinMessageReceiver the bitcoin message receiver, which is the skill that handles outbound bitcoin messages from the local
   * peer
   * @param nodeRuntime the node runtime, used to supply executors
   */
  public LocalBitcoindAdapter(
          final NetworkParameters networkParameters,
          final BitcoinMessageReceiver bitcoinMessageReceiver,
          final BasicNodeRuntime nodeRuntime) {
    // Preconditions
    assert networkParameters != null : "networkParameters must not be null";
    assert bitcoinMessageReceiver != null : "bitcoinMessageReceiver must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_MAINNET) && MainNetParams.class.isAssignableFrom(networkParameters.getClass()))
            || (nodeRuntime.getNetworkName().equals(NetworkUtils.TEXAI_TESTNET) && TestNet3Params.class.isAssignableFrom(networkParameters.getClass()));

    this.networkParameters = networkParameters;
    this.bitcoinMessageReceiver = bitcoinMessageReceiver;
    this.nodeRuntime = nodeRuntime;
    bitcoinProtocolMessageHandler = new BitcoinProtocolMessageHandler(
            this,
            networkParameters);
  }

  /**
   * Starts up this adapter by connecting to the local bitcoind instance and beginning to ping it.
   */
  public void startUp() {
    LOGGER.info("startUp");
    channel = connectToLocalBitcoind();
  }

  /**
   * Shuts down this adapter by disconnecting from the local bitcoind instance.
   */
  public void shutDown() {
    LOGGER.info("shutDown");
    getChannel().close();
  }

  /**
   * Gets the communications channel with the local bitcoind instance.
   *
   * @return the communications channel with the local bitcoind instance
   */
  protected Channel getChannel() {
    return channel;
  }

  /**
   * Provides a Bitcoin protocol message handler.
   */
  static class BitcoinProtocolMessageHandler extends AbstractBitcoinProtocolMessageHandler {

    // the parent local bitcoind adapter
    final LocalBitcoindAdapter localBitcoindAdapter;

    /**
     * Constructs a new BitcoinProtocolMessageHandler instance.
     *
     * @param localBitcoindAdapter the parent local bitcoind adapter
     * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
     */
    BitcoinProtocolMessageHandler(
            final LocalBitcoindAdapter localBitcoindAdapter,
            final NetworkParameters networkParameters) {
      super(networkParameters);
      //Preconditions
      assert localBitcoindAdapter != null : "localBitcoindAdapter must not be null";

      this.localBitcoindAdapter = localBitcoindAdapter;
    }

    /**
     * Catches a channel exception.
     *
     * @param channelHandlerContext the channel handler context
     * @param exceptionEvent the exception event
     */
    @Override
    @SuppressWarnings("ThrowableResultIgnored")
    public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {
      //Preconditions
      assert channelHandlerContext != null : "channelHandlerContext must not be null";
      assert exceptionEvent != null : "exceptionEvent must not be null";

      final Throwable throwable = exceptionEvent.getCause();
      LOGGER.info(throwable.getMessage());

    }

    /**
     * Receives a bitcoin protocol message.
     *
     * @param channelHandlerContext the channel handler context
     * @param messageEvent the message event
     */
    @Override
    public void messageReceived(
            final ChannelHandlerContext channelHandlerContext,
            final MessageEvent messageEvent) {
      //Preconditions
      assert messageEvent != null : "messageEvent must not be null";
      assert Message.class.isAssignableFrom(messageEvent.getMessage().getClass());

      final Message message = (Message) messageEvent.getMessage();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("received from the local bitcoind instance: " + message);
      }
      localBitcoindAdapter.bitcoinMessageReceiver.receiveMessageFromLocalBitcoind(message);

    }
  }

  /**
   * Connects to the local bitcoind instance.
   *
   * @return the communications channel with the local bitcoind instance
   */
  protected Channel connectToLocalBitcoind() {

    final InetSocketAddress inetSocketAddress = PeerAddress.localhost(networkParameters).toSocketAddress();
    final Channel channel1 = ConnectionUtils.openBitcoinProtocolConnection(
            inetSocketAddress,
            bitcoinProtocolMessageHandler,
            nodeRuntime.getExecutor(), // bossExecutor
            nodeRuntime.getExecutor()); // workerExecutor

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Connected to bitcoind.");
    }
    return channel1;
  }

  /**
   * Sends the given bitcoin message to the local bitcoind instance.
   *
   * @param message the given bitcoin protocol message
   */
  public void sendBitcoinMessageToLocalBitcoind(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getChannel() != null : "channel must not be null";

    LOGGER.info("send bitcoin protocol message to local bitcoind instance: " + message);
    final ChannelFuture channelFuture = getChannel().write(message);

    // wait for the outbound version message to be sent
    channelFuture.awaitUninterruptibly();
    if (!channelFuture.isSuccess()) {
      LOGGER.info(StringUtils.getStackTraceAsString(channelFuture.getCause()));
      throw new TexaiException(channelFuture.getCause());
    }
  }

  /**
   * Sends a version message to the local aicoind instance.
   */
  public void sendVersionMessageToLocalBitcoinCore() {
    //Preconditions
    assert getChannel() != null : "channel must not be null";

    final Message versionMessage = new VersionMessage(
            networkParameters, // params
            0); // newBestHeight
    sendBitcoinMessageToLocalBitcoind(versionMessage);
  }

  /**
   * Sends a getaddr message to the local aicoind instance.
   */
  public void sendGetAddressMessageToLocalBitcoinCore() {
    //Preconditions
    assert getChannel() != null : "channel must not be null";

    final Message getAddrMessage = new GetAddrMessage(
            networkParameters); // params

    sendBitcoinMessageToLocalBitcoind(getAddrMessage);
  }

  /**
   * Sends a mempool message to the local aicoind instance.
   */
  public void sendMemoryPoolMessageToLocalBitcoinCore() {
    //Preconditions
    assert getChannel() != null : "channel must not be null";

    final Message memoryPoolMessage = new MemoryPoolMessage();

    sendBitcoinMessageToLocalBitcoind(memoryPoolMessage);
  }

  /**
   * Sends a ping message to the local aicoind instance.
   */
  public void sendPingMessageToLocalBitcoinCore() {
    //Preconditions
    assert getChannel() != null : "channel must not be null";

    final Message pingMessage = new Ping(random.nextLong());

    sendBitcoinMessageToLocalBitcoind(pingMessage);
  }

}
