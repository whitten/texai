/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.skill.aicoin.support;

import org.texai.network.netty.handler.BitcoinMessageReceiver;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.params.MainNetParams;
import java.io.File;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.ahcs.NodeRuntime;
import org.texai.util.NetworkUtils;

/**
 *
 * @author reed
 */
public class LocalBitcoindAdapterTest implements BitcoinMessageReceiver {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(LocalBitcoindAdapterTest.class);
  // the indicator whether the test computer is configured to run this unit test
  private static boolean isConfiguredForTestSuperPeer;

  public LocalBitcoindAdapterTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    final File testSuperPeerDirectory = new File(System.getProperty("user.home") + "/SuperPeer");
    isConfiguredForTestSuperPeer = testSuperPeerDirectory.exists();
    if (!isConfiguredForTestSuperPeer) {
      return;
    }
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("~/SuperPeer/bin/aicoin-qt -debug -shrinkdebugfile=1 -datadir=").append(System.getProperty("user.home")).append("/.aicoin");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the aicoin-qt instance.");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);

    // wait 10 seconds for the QT wallet to open and initialize
    try {
      Thread.sleep(10000);
    } catch (InterruptedException ex) {
    }
  }

  @AfterClass
  public static void tearDownClass() {
    if (!isConfiguredForTestSuperPeer) {
      return;
    }
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("~/SuperPeer/bin/aicoin-cli -datadir=")
            .append(System.getProperty("user.home")).append("/.aicoin")
            .append(" stop");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of class LocalBitcoindAdapter.
   */
  @Test
  public void testLocalBitcoindAdapter() {
    LOGGER.info("testLocalBitcoindAdapter");
    if (isConfiguredForTestSuperPeer) {
      LOGGER.info("test super peer directory is present");
    } else {
      LOGGER.info("test super peer directory is not present");
    }
    final String containerName = "Test";
    final NodeRuntime nodeRuntime = new MockNodeRuntime(
            containerName,
            NetworkUtils.TEXAI_MAINNET); // networkName

    final NetworkParameters networkParameters = new MainNetParams();
    final LocalBitcoindAdapter localBitcoindAdapter = new LocalBitcoindAdapter(
            networkParameters,
            this, // bitcoinMessageReceiver
            nodeRuntime,
            false); // isVersionMessageDropped
    LOGGER.info("opening channel to aicoind");
    localBitcoindAdapter.startUp();

    // conveniently get the block height from the local instance instead of from a peer
    final BitcoinRPCAccess bitcoinRPCAcess = new BitcoinRPCAccess(networkParameters);
    final int newBestHeight = bitcoinRPCAcess.getBlocks();

    // exchange version messages with bitcoind
    localBitcoindAdapter.sendVersionMessageToLocalBitcoinCore(newBestHeight);

    try {
      // wait  30 seconds for a the version and addr response messages
      Thread.sleep(30000);
    } catch (InterruptedException ex) {
      // ignore
    }

    // send a mempool message to bitcoind
    localBitcoindAdapter.sendMemoryPoolMessageToLocalBitcoinCore();
    try {
      // wait 2 seconds for a response message
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
      // ignore
    }

    // send a ping message to bitcoind
    localBitcoindAdapter.sendPingMessageToLocalBitcoinCore();
    try {
      // wait at most 5 seconds for a response message
      Thread.sleep(5000);
    } catch (InterruptedException ex) {
      // ignore
    }

    localBitcoindAdapter.shutDown();
    LOGGER.info("test completed");
  }

  /**
   * Receives an outbound bitcoin message from the local peer.
   *
   * @param message the given bitcoin protocol message
   */
  @Override
  public void receiveMessageFromBitcoind(Message message) {
    LOGGER.info("received: " + message);
  }

  class MockNodeRuntime extends NodeRuntime {

    /**
     * Constructs a new singleton NodeRuntime instance.
     *
     * @param containerName the container name
     * @param networkName the network name, mainnet or testnet
     */
    MockNodeRuntime(
            final String containerName,
            final String networkName) {
      super(containerName, networkName);
    }

  }

}
