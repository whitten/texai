/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.AddressMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.Pong;
import com.google.bitcoin.core.VersionMessage;
import java.io.File;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
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
  // the received message from bitcoind
  private Message receivedMessage;
  // the message response lock
  private final Object response_lock = new Object();

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

    final LocalBitcoindAdapter localBitcoindAdapter = new LocalBitcoindAdapter(
            new XAIMainNetParams(), // networkParameters
            this, // bitcoinMessageReceiver
            nodeRuntime);
    LOGGER.info("opening channel to aicoind");
    localBitcoindAdapter.startUp();

    // exchange version messages with bitcoind
    localBitcoindAdapter.sendVersionMessageToLocalBitcoinCore();
    synchronized (response_lock) {
      try {
        // wait at most 5 seconds for a response message
        response_lock.wait(5000);
      } catch (InterruptedException ex) {
        // ignore
      }
    }
    LOGGER.info("receivedMessage: (" + receivedMessage.getClass().getName() + ") " + receivedMessage);
    assertTrue(VersionMessage.class.isAssignableFrom(receivedMessage.getClass()));

    // send a getaddr message to bitcoind
    receivedMessage = null;
    localBitcoindAdapter.sendGetAddressMessageToLocalBitcoinCore();
    synchronized (response_lock) {
      try {
        // wait at most 5 seconds for a response message
        response_lock.wait(5000);
      } catch (InterruptedException ex) {
        // ignore
      }
    }
    assertNotNull(receivedMessage);
    LOGGER.info("receivedMessage: (" + receivedMessage.getClass().getName() + ") " + receivedMessage);
    assertTrue(AddressMessage.class.isAssignableFrom(receivedMessage.getClass()));
    final AddressMessage addressMessage = (AddressMessage) receivedMessage;
    LOGGER.info("peer addresses ...");
    addressMessage.getAddresses().stream().forEach((PeerAddress peerAddress) -> {
      LOGGER.info("  " + peerAddress);
    });

    // send a mempool message to bitcoind
    receivedMessage = null;
    localBitcoindAdapter.sendMemoryPoolMessageToLocalBitcoinCore();
    synchronized (response_lock) {
      try {
        // wait at most 2 seconds for a response message
        response_lock.wait(2000);
      } catch (InterruptedException ex) {
        // ignore
      }
    }
    if (receivedMessage == null) {
      LOGGER.info("no response to mempool request meesage");
    } else {
      LOGGER.info("receivedMessage: (" + receivedMessage.getClass().getName() + ") " + receivedMessage);
    final Pong pongMessage = (Pong) receivedMessage;
    assertTrue(Pong.class.isAssignableFrom(pongMessage.getClass()));
    }

    // send a ping message to bitcoind
    receivedMessage = null;
    localBitcoindAdapter.sendPingMessageToLocalBitcoinCore();
    synchronized (response_lock) {
      try {
        // wait at most 5 seconds for a response message
        response_lock.wait(5000);
      } catch (InterruptedException ex) {
        // ignore
      }
    }
    LOGGER.info("receivedMessage: (" + receivedMessage.getClass().getName() + ") " + receivedMessage);

    localBitcoindAdapter.shutDown();
    LOGGER.info("test completed");
  }

  /**
   * Receives an outbound bitcoin message from the local peer.
   *
   * @param message the given bitcoin protocol message
   */
  @Override
  public void receiveMessageFromLocalBitcoind(Message message) {
    LOGGER.info("received: " + message);
    this.receivedMessage = message;
    synchronized (response_lock) {
      if (response_lock != null) {
        // release the waiting main test thread
        response_lock.notifyAll();
      }
    }
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
