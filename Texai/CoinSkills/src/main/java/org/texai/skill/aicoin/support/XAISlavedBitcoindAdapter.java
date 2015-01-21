package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.FilteredBlock;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerEventListener;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.VersionMessage;
import com.google.bitcoin.net.ClientConnectionManager;
import com.google.bitcoin.net.NioClientManager;
import com.google.bitcoin.utils.Threading;
import java.util.List;
import java.util.Timer;
import javax.annotation.Nullable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.TexaiException;

/**
 * Created on Sep 9, 2014, 7:41:47 AM.
 *
 * Description: Provides a Bitcoin protocol adapter to the slaved bitcoind instance.
 *
 * Copyright (C) Sep 9, 2014, Stephen L. Reed, Texai.org.
 */
@NotThreadSafe
public class XAISlavedBitcoindAdapter implements PeerEventListener {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(XAISlavedBitcoindAdapter.class);
  // the bitcoind slave
  private Peer slavePeer;
  // the bitcoin message receiver, which is the skill that handles outbound bitcoin messages from the slave peer
  private final XAIBitcoinMessageReceiver bitcoinMessageReceiver;
  // the client connection manager
  private final ClientConnectionManager clientConnectionManager;
  // the version message to use for the bitcoind connection
  private final VersionMessage versionMessage;
  // the minimum protocol version allowed for the bitcoind slave instance
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

  /**
   * Constructs a new SlavedBitcoindAdapter instance.
   *
   * @param networkParameters the network parameters, main net, test net, or regression test net
   * @param bitcoinMessageReceiver the bitcoin message receiver, which is the skill that handles outbound bitcoin messages from the
   * slave peer
   */
  public XAISlavedBitcoindAdapter(
          final NetworkParameters networkParameters,
          final XAIBitcoinMessageReceiver bitcoinMessageReceiver) {
    // Preconditions
    assert networkParameters != null : "networkParameters must not be null";
    assert bitcoinMessageReceiver != null : "bitcoinMessageReceiver must not be null";

    this.networkParameters = networkParameters;
    this.bitcoinMessageReceiver = bitcoinMessageReceiver;
    // We never request that the remote node wait for a bloom filter yet, as we have no wallets
    this.versionMessage = new VersionMessage(
            networkParameters,
            0, // newBestHeight
            true); // relayTxesBeforeFilter

    clientConnectionManager = new NioClientManager();
  }

  /**
   * Starts up this adapter by connecting to the slaved bitcoind instance and beginning to ping it.
   */
  protected void startUp() {
    clientConnectionManager.startAndWait();
    connectToSlave();
    pingTimer = new Timer(
            "Peer pinging thread", // name
            true); // isDaemon
  }

  /**
   * Shuts down this adapter by disconnecting from the slaved bitcoind instance.
   */
  protected void shutDown() {
    pingTimer.cancel();
    // Blocking close of all sockets.
    clientConnectionManager.stopAndWait();
  }

  /**
   * Connects to the slave bitcoind instance.
   */
  protected void connectToSlave() {
    final PeerAddress slavePeerAddress = PeerAddress.localhost(networkParameters);
    versionMessage.time = Utils.currentTimeMillis() / 1000;

    slavePeer = new Peer(
            networkParameters,
            versionMessage,
            slavePeerAddress,
            null, // chain
            null); // memoryPool
    // process the peer event listener methods on the same thread to keep them ordered and synchronous
    slavePeer.addEventListener(this, Threading.SAME_THREAD);
    slavePeer.setMinProtocolVersion(vMinRequiredProtocolVersion);

    try {
      clientConnectionManager.openConnection(slavePeerAddress.toSocketAddress(), slavePeer);
    } catch (Exception e) {
      throw new TexaiException("Failed to connect to " + slavePeerAddress + ": " + e.getMessage());
    }
    slavePeer.setSocketTimeout(connectTimeoutMillis);
  }

  /** Sends the given bitcoin message to the slave peer.
   *
   * @param message the given bitcoin protocol message
   */
  public void sendBitcoinMessageToSlave(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    slavePeer.sendMessage(message);
  }

  /** Receives the given message from the slave peer.
   *
   * @param message the given bitcoin protocol message
   */
  private void receiveBitcoinMessageFromSlave(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    bitcoinMessageReceiver.receiveBitcoinMessageFromSlave(message);
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
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Called when a download is started with the initial number of blocks to be downloaded.
   *
   * @param peer the peer receiving the block
   * @param blocksLeft the number of blocks left to download
   */
  @Override
  public void onChainDownloadStarted(final Peer peer, final int blocksLeft) {
    throw new UnsupportedOperationException("Not supported yet.");
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
    throw new UnsupportedOperationException("Not supported yet.");
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
    return null;
  }

}
