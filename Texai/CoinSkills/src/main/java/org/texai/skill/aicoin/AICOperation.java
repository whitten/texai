package org.texai.skill.aicoin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.skill.aicoin.support.AICoinUtils;
import org.texai.network.netty.handler.BitcoinMessageReceiver;
import org.texai.skill.aicoin.support.LocalBitcoindAdapter;
import org.texai.util.EnvironmentUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Operates a slave bitcoind instance that runs in a separate continueConversation in the same container.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 */
@ThreadSafe
public final class AICOperation extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICOperation.class);
  // the path to the aicoin-qt configuration and data directory
  private static final String AICOIN_DIRECTORY_PATH = "../.aicoin";
  // the insight process
  private Process insightProcess;
  // the local bitcoind (aicoind) instance adapter dictionary, remote container name --> local bitcoind adapter
  protected final Map<String, LocalBitcoindAdapter> localBitcoindAdapterDictionary = new HashMap<>();

  /**
   * Constructs a new AICOperation instance.
   */
  public AICOperation() {
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Receives and attempts to continueConversation the given message. The skill is thread safe, given that any contained libraries are
   * single threaded with regard to the conversation.
   *
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = receivedMessage.getOperation();
    if (!isOperationPermitted(receivedMessage)) {
      sendOperationNotPermittedInfoMessage(receivedMessage);
      return;
    }

    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent AICNetworkOperationAgent.AICNetworkOperationRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent AICNetworkOperationAgent.AICNetworkOperationRole. It indicates that
       * the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the parent role. It commands this network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(receivedMessage);
        return;

      /**
       * Bitcoin Message Info
       *
       * This information message is sent from a peer AICOperation role. As a result this role sends the message to the local aicoind
       * instance.
       */
      case AHCSConstants.BITCOIN_MESSAGE_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        relayBitcoinMessage(receivedMessage);
        return;

      /**
       * Task Accomplished Info
       *
       * This information message is sent from this role's AICWriteConfigurationFile skill indicating that it has emitted the aicoin.conf
       * file.
       */
      case AHCSConstants.TASK_ACCOMPLISHED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        continueConversation(receivedMessage);
        return;

      /**
       * Shutdown Aicoind Task
       *
       * This task message is sent from AICNetworkOperationAgent.AICNetworkOperationRole when the application is shutting down. The child
       * processes must be shutdown to enable the Java application to exit.
       */
      case AHCSConstants.SHUTDOWN_AICOIND_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        shutdownAicoind();
        shutdownInsight();
        return;

      /**
       * Shutdown Aicoind Request Info
       *
       * This information message is sent from ContainerOperationAgent.ContainerOperationRole when the application is shutting down, as a
       * result of a loss of heartbeat communications. The child processes must be shutdown to enable the Java application to exit.
       */
      case AHCSConstants.SHUTDOWN_AICOIND_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        shutdownAicoind();
        shutdownInsight();
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(receivedMessage);
        return;

      // handle other operations ...
    }

    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

    sendDoNotUnderstandInfoMessage(receivedMessage);
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single threaded with regard
   * to the conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // handle operations
    return Message.notUnderstoodMessage(
            message, // receivedMessage
            this); // skill
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.BITCOIN_MESSAGE_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.SHUTDOWN_AICOIND_REQUEST_INFO,
      AHCSConstants.SHUTDOWN_AICOIND_TASK,
      AHCSConstants.TASK_ACCOMPLISHED_INFO
    };
  }

  /**
   * Perform this role's mission, which is to operate the local aicoind instance.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
    assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";

    LOGGER.info("performing the mission");
    // request that the sibling skill write the aicoind configuration file
    final Message writeConfigurationFileTaskmessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService
            getQualifiedName(), // recipientQualifiedName
            UUID.randomUUID(), // conversationId,
            UUID.randomUUID(), // replyWith,
            AICWriteConfigurationFile.class.getName(), // recipientService
            AHCSConstants.WRITE_CONFIGURATION_FILE_INFO); // operation
    writeConfigurationFileTaskmessage.put(AHCSConstants.WRITE_CONFIGURATION_FILE_INFO_DIRECTORY_PATH, AICOIN_DIRECTORY_PATH);
    // set timeout
    setMessageReplyTimeout(
            receivedMessage,
            writeConfigurationFileTaskmessage,
            10000, // timeoutMillis
            false, // isRecoverable
            null); // recoveryAction
    sendMessageViaSeparateThread(receivedMessage, writeConfigurationFileTaskmessage);
  }

  /**
   * Continues the conversation with the AICWriteConfigurationFile skill.
   *
   * @param message the received message
   */
  private synchronized void continueConversation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.getInReplyTo() != null : "message must have inReplyTo value " + message.toDetailedString();

    removeMessageTimeOut(message.getInReplyTo());
    LOGGER.info("The bitcoind configuration file has been written");
    launchAicoind();

    getNodeRuntime().getExecutor().execute(new SuperPeerConnector(this));

    if ("BlockchainExplorer".equals(getContainerName())) {
      // after a pause, launch the Insight blockchain explorer instance
      getNodeRuntime().getExecutor().execute(new InsightRunner());
    }
  }

  /** Provides a connection between the local bitcoind (aicoind) instance and a remote one at a network super peer. */
  static class SuperPeerConnection implements BitcoinMessageReceiver {

    // the AIC operation skill
    final private AICOperation aicOperation;
    // the super peer container name
    final private String superPeerContainerName;
    // the local bitcoind (aicoind) adapter
    private LocalBitcoindAdapter localBitcoindAdapter;

    /** Constructs a new SuperPeerConnection instance.
     *
     * @param aicOperation the AIC operation skill
     * @param superPeerContainerName the super peer container name
     */
    SuperPeerConnection(
            final AICOperation aicOperation,
            final String superPeerContainerName) {
      //Preconditions
      assert aicOperation != null : "aicOperation must not be null";
      assert StringUtils.isNonEmptyString(superPeerContainerName) : "superPeerContainerName must be a non-empty string";

      this.aicOperation = aicOperation;
      this.superPeerContainerName = superPeerContainerName;
    }

    @Override
    /**
     * Receives an outbound bitcoin message from the local peer.
     *
     * @param message the given bitcoin protocol message
     */
    public void receiveMessageFromBitcoind(final com.google.bitcoin.core.Message bitcoinProtocolMessage) {
      //Preconditions
      assert bitcoinProtocolMessage != null : "message must not be null";

      LOGGER.info("received from local aicoind for super peer:" + superPeerContainerName + ", message: " + bitcoinProtocolMessage);
      final String recipientQualifiedName = superPeerContainerName + ".AICOperationAgent.AICOperationRole";
      final Message relayMessage = aicOperation.makeMessage(
              recipientQualifiedName,
              AICOperation.class.getName(), // recipientService
              AHCSConstants.BITCOIN_MESSAGE_INFO); // operation
      relayMessage.put(AHCSConstants.BITCOIN_MESSAGE_INFO_Message, bitcoinProtocolMessage);

      aicOperation.sendMessage(
              null, // receivedMessage
              relayMessage); // message
    }

    /** Sets the local bitcoind (aicoind) adapter.
     *
     * @param localBitcoindAdapter the local bitcoind (aicoind) adapter
     */
    public void setLocalBitcoindAdapter(final LocalBitcoindAdapter localBitcoindAdapter) {
      //Preconditions
      assert localBitcoindAdapter != null : "localBitcoindAdapter must not be null";

      this.localBitcoindAdapter = localBitcoindAdapter;
    }
  }

  /**
   * Provides a runnable that connects this bitcoind (aicoind) to super peer bitcoind (aicoind) instances.
   */
  static class SuperPeerConnector implements Runnable {

    // the AIC operation skill
    final private AICOperation aicOperation;

    SuperPeerConnector(final AICOperation aicOperation) {
      //Preconditions
      assert aicOperation != null : "aicOperation must not be null";

      this.aicOperation = aicOperation;
    }

    /**
     * Connects to super peers.
     */
    @Override
    public void run() {
      // wait 10 seconds for the local bitcoind (aicoind) instance to open and initialize
      try {
        Thread.sleep(10000);
      } catch (InterruptedException ex) {
      }
      // Examine the containers with which this peer should connect. If the remote container's name is sorted less than this one's name, then
      // this one initiates the connection.
      final ContainerInfo containerInfo = aicOperation.getNodeRuntime().getContainerInfoAccess().getContainerInfo(aicOperation.getContainerName());
      assert containerInfo != null;
      final List<String> superPeerContainerNames = new ArrayList<>(containerInfo.getSuperPeerContainerNames());
      Collections.sort(superPeerContainerNames);
      LOGGER.info("superPeerContainerNames: " + superPeerContainerNames + "...");
      final String myContainerName = aicOperation.getContainerName();
      for (final String superPeerContainerName : superPeerContainerNames) {
        if (superPeerContainerName.compareTo(myContainerName) == -1) {
          LOGGER.info("  Connecting local bitcoind to " + superPeerContainerName);
          final SuperPeerConnection superPeerConnection = new SuperPeerConnection(
            aicOperation,
            superPeerContainerName);
          final LocalBitcoindAdapter localBitcoindAdapter = new LocalBitcoindAdapter(
                  ((NodeRuntime) aicOperation.getNodeRuntime()).getNetworkParameters(),
                  superPeerConnection, // bitcoinMessageReceiver
                  aicOperation.getNodeRuntime());

          aicOperation.localBitcoindAdapterDictionary.put(
                  superPeerContainerName,
                  localBitcoindAdapter);
          LOGGER.info("opening channel to aicoind");
          localBitcoindAdapter.startUp();

          // send a version messages to the local bitcoind (aicoind), the response will be relayed to the remote bitcoind
          localBitcoindAdapter.sendVersionMessageToLocalBitcoinCore();
        }
      }
    }

  }

  /**
   * Launches the aicoind instance.
   */
  private void launchAicoind() {
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    final String display = System.getenv("DISPLAY");
    LOGGER.info("X11 DISPLAY=" + display);
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    if (this.getContainerName().equals("TestContainer")) {
      stringBuilder.append("~/docker/SuperPeer/bin/aicoin-qt -debug -shrinkdebugfile=1 -datadir=").append(System.getProperty("user.home")).append("/.aicoin");
    } else {
      stringBuilder.append("../bin/aicoin-qt -debug -shrinkdebugfile=1 -datadir=").append(AICOIN_DIRECTORY_PATH);
    }
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the aicoin-qt instance.");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  class InsightRunner implements Runnable {

    @Override
    public void run() {
      try {
//        LOGGER.info("Waiting 5 minutes before launching the block explorer ...");
//        Thread.sleep(5 * 60 * 1000);
        LOGGER.info("Waiting 1 minute before launching the block explorer ...");
        Thread.sleep(60 * 1000);
      } catch (InterruptedException ex) {
      }
      launchInsight();
    }

  }

  /**
   * Launches the Insight blockchain explorer instance.
   */
  private void launchInsight() {
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    final String display = System.getenv("DISPLAY");
    LOGGER.info("X11 DISPLAY=" + display);
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("cd /root/insight && npm start");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the Insight blockchain explorer instance.");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    insightProcess = AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  /**
   * Shuts down the aicoind instance.
   */
  private void shutdownAicoind() {
    //Preconditions
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    LOGGER.info("Shutting down the aicoind instance.");
    synchronized (localBitcoindAdapterDictionary) {
      localBitcoindAdapterDictionary.values().stream().forEach((LocalBitcoindAdapter localBitcoindAdapter) -> {
        localBitcoindAdapter.shutDown();
      });
    }

    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("../bin/aicoin-cli -datadir=")
            .append(AICOIN_DIRECTORY_PATH)
            .append(" stop");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  /**
   * Shuts down the Insight blockchain explorer instance.
   */
  private void shutdownInsight() {
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }
    if (insightProcess != null) {
      LOGGER.info("Destroying the Insight process.");
      insightProcess.destroy();
    }
  }

  /**
   * Relays the Bitcoin message to the local aicoind instance.
   *
   * @param receivedMessage the given message, which contains the Bitcoin protocol message as a parameter
   */
  private void relayBitcoinMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    final com.google.bitcoin.core.Message bitcoinProtocolMessage
            = (com.google.bitcoin.core.Message) receivedMessage.get(AHCSConstants.BITCOIN_MESSAGE_INFO_Message);
    LOGGER.info("received bitcoinProtocolMessage from network: " + bitcoinProtocolMessage);
    assert bitcoinProtocolMessage != null;
    final String remoteContainerName = receivedMessage.getSenderContainerName();

    synchronized (localBitcoindAdapterDictionary) {
      final LocalBitcoindAdapter localBitcoindAdapter = localBitcoindAdapterDictionary.get(remoteContainerName);
      localBitcoindAdapter.sendBitcoinMessageToLocalBitcoind(bitcoinProtocolMessage);
    }
  }

}
