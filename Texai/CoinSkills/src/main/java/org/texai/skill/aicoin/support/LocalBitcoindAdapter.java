package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.FilteredBlock;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Pong;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.VersionMessage;
import com.google.bitcoin.net.ClientConnectionManager;
import com.google.bitcoin.net.NioClientManager;
import com.google.bitcoin.utils.Threading;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nullable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.ahcs.NodeRuntime;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.utils.ConnectionUtils;
import org.texai.util.TexaiException;

/**
 * Created on Sep 9, 2014, 7:41:47 AM.
 *
 * Description: Provides a Bitcoin protocol adapter to the local bitcoind instance.
 *
 * Copyright (C) Sep 9, 2014, Stephen L. Reed, Texai.org.
 */
@NotThreadSafe
public class LocalBitcoindAdapter implements PeerEventListener {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(LocalBitcoindAdapter.class);
  // the bitcoind local instance
  private Peer localPeer;
  // the bitcoin message receiver, which is the skill that handles outbound bitcoin messages from the local peer
  private final BitcoinMessageReceiver bitcoinMessageReceiver;
  // the version message to use for the bitcoind connection
  private final VersionMessage versionMessage;
  // the minimum protocol version allowed for the bitcoind local instance
  private volatile int vMinRequiredProtocolVersion = FilteredBlock.MIN_PROTOCOL_VERSION;
  // Runs a background thread that we use for scheduling pings to our peers, so we can measure their performance and network latency. We
  // ping peers every pingIntervalMsec milliseconds.
  private volatile Timer pingTimer;
  // How many milliseconds to wait after receiving a pong before sending another ping.
  public static final long DEFAULT_PING_INTERVAL_MSEC = 2000;
  private final long pingIntervalMsec = DEFAULT_PING_INTERVAL_MSEC;
  // The default timeout between when a connection attempt begins and version message exchange completes
  public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
  private volatile int connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS;
  // the network parameters, main net, test net, or regression test net
  private final NetworkParameters networkParameters;
  // the Bitcoin protocol message handler
  private final BitcoinProtocolMessageHandler bitcoinProtocolMessageHandler;
  // the node runtime
  private final NodeRuntime nodeRuntime;
  // the communications channel with the local bitcoind instance
  private Channel channel;

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
          final NodeRuntime nodeRuntime) {
    // Preconditions
    assert networkParameters != null : "networkParameters must not be null";
    assert bitcoinMessageReceiver != null : "bitcoinMessageReceiver must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.networkParameters = networkParameters;
    this.bitcoinMessageReceiver = bitcoinMessageReceiver;
    this.nodeRuntime = nodeRuntime;

    // We never request that the remote node wait for a bloom filter yet, as we have no wallets
    this.versionMessage = new VersionMessage(
            networkParameters,
            0, // newBestHeight
            true); // relayTxesBeforeFilter
    bitcoinProtocolMessageHandler = new BitcoinProtocolMessageHandler(this);
  }

  /**
   * Starts up this adapter by connecting to the local bitcoind instance and beginning to ping it.
   */
  public void startUp() {
    LOGGER.info("startUp");
    connectToLocalBitcoind();
    pingTimer = new Timer(
            "Peer pinging thread", // name
            true); // isDaemon
  }

  /**
   * Shuts down this adapter by disconnecting from the local bitcoind instance.
   */
  public void shutDown() {
    LOGGER.info("shutDown");
    if (pingTimer != null) { // unit testing will not have started the ping timer
      pingTimer.cancel();
    }
    // Blocking close of all sockets.
  }

  /** Provides a Bitcoin protocol message handler. */
  static class BitcoinProtocolMessageHandler extends AbstractBitcoinProtocolMessageHandler {

    // the parent local bitcoind adapter
    final LocalBitcoindAdapter localBitcoindAdapter;

    /** Constructs a new BitcoinProtocolMessageHandler instance.
     *
     * @param localBitcoindAdapter the parent local bitcoind adapter
     */
    BitcoinProtocolMessageHandler(final LocalBitcoindAdapter localBitcoindAdapter) {
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
     * Receives a Netty message object from a remote message router peer. The received message is verified before relaying to the role.
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
      assert messageEvent.getMessage() instanceof Message;

      final Message message = (Message) messageEvent.getMessage();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("***** received from remote message router: " + message);
      }
    }
  }

  /**
   * Connects to the local bitcoind instance.
   * @return the communications channel with the local bitcoind instance
   */
  protected  Channel connectToLocalBitcoind() {

    final InetSocketAddress inetSocketAddress = PeerAddress.localhost(networkParameters).toSocketAddress();
    final Channel channel = ConnectionUtils.openBitcoinProtocolConnection(
          inetSocketAddress,
          bitcoinProtocolMessageHandler,
          nodeRuntime.getExecutor(), // bossExecutor
          nodeRuntime.getExecutor()); // workerExecutor

    versionMessage.time = Utils.currentTimeMillis() / 1000;

    LOGGER.info("connected to aicoined");
    return channel;
  }

  /**
   * Sends the given bitcoin message to the local peer.
   *
   * @param message the given bitcoin protocol message
   */
  public void sendBitcoinMessageToLocalBitcionCore(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("sendBitcoinMessageToLocalBitcionCore: " + message);
    localPeer.sendMessage(message);
  }

  /**
   * Receives the given message from the local peer.
   *
   * @param message the given bitcoin protocol message
   */
  public void receiveBitcoinMessageFromLocalBitcionCore(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("receiveBitcoinMessageFromLocalBitcionCore: " + message);
    bitcoinMessageReceiver.receiveMessageFromLocalBitcoind(message);
  }

  /**
   * Called on a Peer thread when a block is received.<p>
   *
   * The block may have transactions or may be a header only once getheaders is implemented.
   *
   * @param peer the peer receiving the block
   * @param block the downloaded block
   * @param blocksLeft the number of blocks left to download
   */
  @Override
  public void onBlocksDownloaded(final Peer peer, final Block block, final int blocksLeft) {
    LOGGER.info("onChainDownloadStarted: " + blocksLeft);
  }

  /**
   * Called when a download is started with the initial number of blocks to be downloaded.
   *
   * @param peer the peer receiving the block
   * @param blocksLeft the number of blocks left to download
   */
  @Override
  public void onChainDownloadStarted(final Peer peer, final int blocksLeft) {
    LOGGER.info("onChainDownloadStarted: " + blocksLeft);
  }

  /**
   * Called when a peer is connected. If this listener is registered to a {@link Peer} instead of a {@link PeerGroup}, peerCount will always
   * be 1.
   *
   * @param peer the connected peer
   * @param peerCount the total number of connected peers
   */
  @Override
  public void onPeerConnected(final Peer peer, final int peerCount) {
    LOGGER.info("onPeerDisconnected: " + peerCount);
  }

  /**
   * Called when a peer is disconnected. Note that this won't be called if the listener is registered on a {@link PeerGroup} and the group
   * is in the process of shutting down. If this listener is registered to a {@link Peer} instead of a {@link PeerGroup}, peerCount will
   * always be 0.
   *
   * @param peer the disconnected peer
   * @param peerCount the total number of connected peers
   */
  @Override
  public void onPeerDisconnected(final Peer peer, final int peerCount) {
    LOGGER.info("onPeerDisconnected: " + peerCount);
  }

  /**
   * Called when a message is received by a peer, before the message is processed. The returned message is processed instead. Returning null
   * will cause the message to be ignored by the Peer returning the same message object allows you to see the messages received but not
   * change them.
   *
   * @param peer the peer receiving the message
   * @param message the message
   *
   * @return a message to be processed instead. Returning null will cause the message to be ignored by the Peer
   */
  @Override
  public Message onPreMessageReceived(final Peer peer, final Message message) {
    LOGGER.info("onPreMessageReceived: " + message);
    return message;
  }

  /**
   * Called when a new transaction is broadcast over the network.
   *
   * @param peer the peer that is broadcasting the transaction
   * @param transaction the transaction
   */
  @Override
  public void onTransaction(final Peer peer, final Transaction transaction) {
    LOGGER.info("onTransaction: " + transaction);
  }

  /**
   * <p>
   * Called when a peer receives a getdata message, usually in response to an "inv" being broadcast. Return as many items as possible which
   * appear in the {@link GetDataMessage}, or null if you're not interested in responding.</p>
   *
   * <p>
   * Note that this will never be called if registered with any executor other than
   * {@link com.google.bitcoin.utils.Threading#SAME_THREAD}</p>
   *
   * @param peer the peer receiving the getdata message
   * @param message the getdata message
   *
   * @return as many items as possible which appear in the {@link GetDataMessage}, or null if not interested in responding
   */
  @Nullable
  @Override
  public List<Message> getData(final Peer peer, final GetDataMessage message) {
    LOGGER.info("getData: " + message);
    return null;
  }

  /**
   * Setup pinging for the local peer.
   */
  private void setupPingingLocalPeer() {
    if (localPeer.getPeerVersionMessage().clientVersion < Pong.MIN_PROTOCOL_VERSION) {
      return;
    }
    // Start the process of pinging the peer. Do a ping right now and then ensure there's a fixed delay between
    // each ping. If the peer is taken out of the peers list then the cycle will stop.
    //
    // TODO: This should really be done by a timer integrated with the network thread to avoid races.
    final Runnable[] pingRunnable = new Runnable[1];
    pingRunnable[0] = new Runnable() {
      private boolean firstRun = true;

      @Override
      public void run() {
        // Ensure that the first ping happens immediately and later pings after the requested delay.
        if (firstRun) {
          firstRun = false;
          try {
            localPeer.ping().addListener(this, Threading.SAME_THREAD);
          } catch (Exception e) {
            LOGGER.warn("Exception whilst trying to ping peer: " + localPeer + "\n" + e.toString());
            return;
          }
          return;
        }

        final long interval = 10000L; // 10 seconds
        if (interval <= 0) {
          return;  // Disabled.
        }
        final TimerTask task = new TimerTask() {
          @Override
          public void run() {
            try {
              localPeer.ping().addListener(pingRunnable[0], Threading.SAME_THREAD);
            } catch (Exception e) {
              LOGGER.warn("Exception whilst trying to ping peer: " + localPeer + "\n" + e.toString());
            }
          }
        };
        try {
          pingTimer.schedule(task, interval);
        } catch (IllegalStateException ignored) {
          // This can happen if there's a shutdown race and this runnable is executing whilst the timer is
          // simultaneously cancelled.
        }
      }
    };
    pingRunnable[0].run();
  }
}
