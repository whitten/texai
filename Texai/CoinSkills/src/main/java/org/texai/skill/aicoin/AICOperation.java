package org.texai.skill.aicoin;

import com.google.bitcoin.core.AddressMessage;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.VersionAck;
import com.google.bitcoin.core.VersionMessage;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import net.jcip.annotations.ThreadSafe;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.skill.aicoin.support.AICoinUtils;
import org.texai.network.netty.handler.BitcoinMessageReceiver;
import org.texai.skill.aicoin.support.BitcoinRPCAccess;
import org.texai.skill.aicoin.support.LocalBitcoindAdapter;
import org.texai.util.EnvironmentUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Operates a bitcoind (aicoind) instance that runs in the same container.
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
  // the bitcoind (aicoind) RPC access object
  private BitcoinRPCAccess bitcoinRPCAccess;

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
        bitcoinRPCAccess = new BitcoinRPCAccess(((NodeRuntime) getNodeRuntime()).getNetworkParameters());
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
       * Connection Request Info
       *
       * This information message is sent from a peer AICOperation role. As a result this role replies with the Connection Request Approved
       * Info message.
       */
      case AHCSConstants.CONNECTION_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        handleConnectionRequest(receivedMessage);
        return;

      /**
       * Connection Request Approved Info
       *
       * This information message is a reply from a peer AICOperation role in response to a connection request. As a result this role
       * establishes a connection between the local bitcoind (aicoind) instance and the peer.
       */
      case AHCSConstants.CONNECTION_REQUEST_APPROVED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        handleConnectionRequestApproved(receivedMessage);
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
        continueAfterWritingConfiguration(receivedMessage);
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
      AHCSConstants.CONNECTION_REQUEST_INFO,
      AHCSConstants.CONNECTION_REQUEST_APPROVED_INFO,
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
   * @param receivedMessage the received message
   */
  private synchronized void continueAfterWritingConfiguration(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert receivedMessage.getInReplyTo() != null : "receivedMessage must have inReplyTo value " + receivedMessage.toDetailedString();

    removeMessageTimeOut(receivedMessage.getInReplyTo());
    LOGGER.info("The bitcoind configuration file has been written");
    launchAicoind();

    getNodeRuntime().getExecutor().execute(new SuperPeerConnector(this));

    if (getNodeRuntime().isBlockExplorer()) {
      // after a pause, launch the Insight blockchain explorer instance
      getNodeRuntime().getExecutor().execute(new InsightRunner());
    }
  }

  /**
   * Handles a Connection Request Info message.
   *
   * @param receivedMessage the received message
   */
  private synchronized void handleConnectionRequest(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    LOGGER.info("Approving a connection request from " + receivedMessage.getSenderContainerName());

    // get the block height from the local bitcoind (aicoind) instance for the version message that will be sent to the remote peer
    final int newBestBlockHeight = bitcoinRPCAccess.getBlocks();

    final Message connectionRequestApprovedInfoMessage = makeReplyMessage(
            receivedMessage,
            AHCSConstants.CONNECTION_REQUEST_APPROVED_INFO);
    connectionRequestApprovedInfoMessage.put(
            AHCSConstants.CONNECTION_REQUEST_APPROVED_INFO_NEW_BEST_HEIGHT,
            newBestBlockHeight);

    final X509SecurityInfo x509SecurityInfo = getX509SecurityInfo();
    connectionRequestApprovedInfoMessage.put(
            AHCSConstants.MSG_PARM_X509_CERTIFICATE,
            x509SecurityInfo.getX509Certificate());
    sendMessage(
            receivedMessage,
            connectionRequestApprovedInfoMessage); // message
  }

  /**
   * Handles a Connection Request Approved Info message by making a connection between the local bitcoind (aicoind) instance and a remote
   * one at the sender network super peer.
   *
   * @param receivedMessage the received message
   */
  private void handleConnectionRequestApproved(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    final int newBestHeight = (int) receivedMessage.get(AHCSConstants.CONNECTION_REQUEST_APPROVED_INFO_NEW_BEST_HEIGHT);
    assert newBestHeight >= 0;
    final String superPeerContainerName = receivedMessage.getSenderContainerName();
    LOGGER.info("  Connecting local bitcoind to " + superPeerContainerName + ", having block height " + newBestHeight);
    final SuperPeerConnection superPeerConnection = new SuperPeerConnection(
            this,
            superPeerContainerName);
    final LocalBitcoindAdapter localBitcoindAdapter = new LocalBitcoindAdapter(
            ((NodeRuntime) getNodeRuntime()).getNetworkParameters(),
            superPeerConnection, // bitcoinMessageReceiver
            getNodeRuntime(),
            false); // isVersionMessageDropped, the peer originating the connection sends the version with its block height

    synchronized (localBitcoindAdapterDictionary) {
      localBitcoindAdapterDictionary.put(superPeerContainerName, localBitcoindAdapter);
      LOGGER.info("opening channel to bitcoind (aicoind)");
      localBitcoindAdapter.startUp();
      // send a version message to the local bitcoind (aicoind)
      localBitcoindAdapter.sendVersionMessageToLocalBitcoinCore(newBestHeight);
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
        Thread.sleep(20000);
      } catch (InterruptedException ex) {
      }
      final ContainerInfo containerInfo = aicOperation.getNodeRuntime().getContainerInfoAccess().getContainerInfo(aicOperation.getContainerName());
      assert containerInfo != null;
      final List<String> superPeerContainerNames = new ArrayList<>();
      if (containerInfo.isSuperPeer()) {
        // super peers are connected to each other
        superPeerContainerNames.addAll(aicOperation.getNodeRuntime().getContainerInfoAccess().getAllSuperPeerContainerNames());
      } else {
        // ordinary peers are connected to certain super peers
        superPeerContainerNames.addAll(containerInfo.getSuperPeerContainerNames());
      }

      Collections.sort(superPeerContainerNames);
      LOGGER.info("superPeerContainerNames: " + superPeerContainerNames + "...");
      final String myContainerName = aicOperation.getContainerName();
      LOGGER.info("  myContainerName: " + myContainerName);
      for (final String superPeerContainerName : superPeerContainerNames) {
        LOGGER.info("  superPeerContainerName: " + superPeerContainerName);
        if (!superPeerContainerName.equals(myContainerName)) {
          try {
            LOGGER.info("  requesting connection with " + superPeerContainerName);
            final Message connectionRequestInfoMessage = aicOperation.makeMessage(
                    superPeerContainerName + ".AICOperationAgent.AICOperationRole", // recipientQualifiedName
                    AICOperation.class.getName(), // recipientService
                    AHCSConstants.CONNECTION_REQUEST_INFO); // operation
            final X509SecurityInfo x509SecurityInfo = aicOperation.getX509SecurityInfo();
            connectionRequestInfoMessage.put(
                    AHCSConstants.MSG_PARM_X509_CERTIFICATE,
                    x509SecurityInfo.getX509Certificate());

            aicOperation.sendMessageViaSeparateThread(
                    null, // received message
                    connectionRequestInfoMessage); // message
          } catch (TexaiException ex) {
            LOGGER.info("  cannot connect with " + superPeerContainerName);
          }
        }
      }
    }
  }

  /**
   * Provides a connection between the local bitcoind (aicoind) instance and a remote one at a network super peer.
   */
  static class SuperPeerConnection implements BitcoinMessageReceiver {

    // the AIC operation skill
    final private AICOperation aicOperation;
    // the super peer container name
    final private String superPeerContainerName;
    // the local bitcoind (aicoind) adapter
    private LocalBitcoindAdapter localBitcoindAdapter;

    /**
     * Constructs a new SuperPeerConnection instance.
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

      if (bitcoinProtocolMessage instanceof AddressMessage) {
        LOGGER.info("dropping addr message from the local bitcoind (aicoind) instance");
        return;
      }
      LOGGER.info("received from local bitcoind (aicoind) instance for super peer:" + superPeerContainerName + ", message: " + bitcoinProtocolMessage);
      final String recipientQualifiedName = superPeerContainerName + ".AICOperationAgent.AICOperationRole";
      final Message relayMessage = aicOperation.makeMessage(
              recipientQualifiedName,
              AICOperation.class.getName(), // recipientService
              AHCSConstants.BITCOIN_MESSAGE_INFO); // operation
      relayMessage.put(AHCSConstants.BITCOIN_MESSAGE_INFO_MESSAGE, bitcoinProtocolMessage);

      aicOperation.sendMessage(
              null, // receivedMessage
              relayMessage); // message
    }

    /**
     * Sets the local bitcoind (aicoind) adapter.
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

  /**
   * Launchers the Insight blockchain explorer after a delay.
   */
  class InsightRunner implements Runnable {

    @Override
    public void run() {
      try {
//        LOGGER.info("Waiting 5 minutes before launching the block explorer ...");
//        Thread.sleep(5 * 60 * 1000);
        LOGGER.info("Waiting 1 minute before launching the block explorer ...");
        Thread.sleep(60 * 1000);
      } catch (InterruptedException ex) {
        // ignore exception
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
    stringBuilder.append("cd /insight && npm start");
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
  private synchronized void relayBitcoinMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    final com.google.bitcoin.core.Message bitcoinProtocolMessage
            = (com.google.bitcoin.core.Message) receivedMessage.get(AHCSConstants.BITCOIN_MESSAGE_INFO_MESSAGE);
    LOGGER.info("received bitcoinProtocolMessage from network: " + bitcoinProtocolMessage);
    assert bitcoinProtocolMessage != null;
    final String remoteContainerName = receivedMessage.getSenderContainerName();

    LocalBitcoindAdapter localBitcoindAdapter;
    synchronized (localBitcoindAdapterDictionary) {
      localBitcoindAdapter = localBitcoindAdapterDictionary.get(remoteContainerName);
      if (localBitcoindAdapter == null) {
        // the remote peer is a new connection and the first message must be a version message or version ack message
        if (!bitcoinProtocolMessage.getClass().isAssignableFrom(VersionMessage.class)
                && !bitcoinProtocolMessage.getClass().isAssignableFrom(VersionAck.class)) {
          throw new TexaiException("expected version message or version acknowledge message, but received " + bitcoinProtocolMessage);
        }
        LOGGER.info("Connecting local bitcoind to " + remoteContainerName);
        final SuperPeerConnection superPeerConnection = new SuperPeerConnection(
                this,
                remoteContainerName);
        // the version message response is dropped because the remote peer's connection was initiated by its local adapter using
        // a constructed version message having this peer's block height
        localBitcoindAdapter = new LocalBitcoindAdapter(
                ((NodeRuntime) getNodeRuntime()).getNetworkParameters(),
                superPeerConnection, // bitcoinMessageReceiver
                getNodeRuntime(),
                true); // isVersionMessageDropped

        localBitcoindAdapterDictionary.put(remoteContainerName, localBitcoindAdapter);
        LOGGER.info("opening channel to bitcoind (aicoind)");
        localBitcoindAdapter.startUp();
      }
    }

    if (bitcoinProtocolMessage instanceof Transaction) {
      // if minting container, then send the inbound transaction to the mint agent in this container
      // otherwise, route the inbound transaction to the mint container's AIC operation agent
      LOGGER.info("received Bitcoin protocol transaction message: " + bitcoinProtocolMessage);
    }

    localBitcoindAdapter.sendBitcoinMessageToLocalBitcoind(bitcoinProtocolMessage);
  }

  public static void main(String[] args) throws Exception {
    String rawTx = "0100000001fb8843c05fbf5e541ffe2af853c7cd2fc5127edd03dfda4fa50cf34a43f7ec3f2800000000ffffffff02801a0600000000001976a914b6a9eaa8cf3d2f31b01fa594dcb99f8aa7bc542488ac1e250000000000001976a9145c249db249b7946ee2b83a611d535cce9b7fe7ae88ac00000000";
    String msg = "hello world!";
    String txWithOPReturn = addOPReturn(rawTx, msg);
    LOGGER.info(txWithOPReturn);
  }

  private static String addOPReturn(
          final String rawTx,
          final String msg) {
    //Preconditions
    assert StringUtils.isNonEmptyString(rawTx) : "rawTx must be an non-empty string";
    assert StringUtils.isNonEmptyString(msg) : "msg must be an non-empty string";
    assert msg.length() <= 40 : "msg must not be more than 40 bytes in length";

    final String satoshiPaid = "0000000000000000";
    final String opReturn = "6a";
    // first, separate the inputs and outputs portions of the transaction
    final String outputs = rawTx.substring(rawTx.indexOf("ffffffff"));
    final String inputs = rawTx.substring(0, rawTx.indexOf("ffffffff"));
    // now we get the number of outputs
    final String numOuts = outputs.substring(8, 10);
    // add one to deal with the OP_RETURN (cheating because we know the value in numOuts is an integer)
    int outsHex = Integer.parseInt(numOuts, 16);
    outsHex++;
    final String totalOuts = Integer.toHexString(outsHex);
    // pad a leading 0
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(totalOuts);
    if (stringBuilder.length() < 2) {
      stringBuilder.insert(0, '0');
    }
    final String paddedOuts = stringBuilder.toString();
    // turn the message into hex
    final String msgHex;
    try {
      msgHex = Hex.encodeHexString(msg.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    final int msgBytes = msg.length();
    final String paddedMsgLength = pad(Integer.toHexString(msgBytes));
    // calculate the total script length for OP_RETURN as 2 bytes + msg length
    final int scriptSize = msgBytes + 2;
    final String paddedScriptSize = pad(Integer.toHexString(scriptSize));
    // put the OP_RETURN script string together and then splice it into the existing rawTX
    final StringBuilder opReturnScript = new StringBuilder();
    opReturnScript.append(satoshiPaid).append(paddedScriptSize).append(opReturn)
            .append(paddedMsgLength).append(msgHex);
    final String outsScript = outputs.substring(10, outputs.length() - 8);
    final StringBuilder opReturnRawTx = new StringBuilder();
    opReturnRawTx.append(inputs).append("ffffffff").append(paddedOuts)
            .append(outsScript)
            .append(opReturnScript)
            .append("00000000");
    return opReturnRawTx.toString();
  }

  private static String pad(String value) {
    StringBuilder sb = new StringBuilder();
    sb.append(value);
    if (sb.length() < 2) {
      sb.insert(0, '0');
    }
    return sb.toString();
  }

}
