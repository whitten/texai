/*
 * ContainerSingletonConfiguration.java
 *
 * Created on Mar 14, 2012, 10:49:55 PM
 *
 * Description: Configures the roles in this container whose parents are nomadic singleton roles hosted
 * on a probably remote super-peer.
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.singletonConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.seed.SeedNodeInfo;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.governance.TopmostFriendship;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.MessageDigestUtils;

/**
 *
 * @author reed
 */
@ThreadSafe
public class ContainerSingletonConfiguration extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(ContainerSingletonConfiguration.class);
  // the locations and credentials for network seed nodes
  private Set<SeedNodeInfo> seedNodesInfos;
  // the SHA-512 hash of the seed node infos serialized file
  private final String seedNodeInfosFileHashString
          = "YUhf+GN2kmnkLzwDnkhibWxlbnfyW9UExRTg/c4njjP05jX0BvlunodyVz33dp8chbgSaheEEAqzyDtz2+bPcw==";
  // the indicator that network singleton configuration information has been received from a seed peer
  private final AtomicBoolean isSeedConfigurationInfoReceived = new AtomicBoolean(false);
  // the unjoined child roles
  private final Set<String> unjoinedChildQualifiedNames = new HashSet<>();

  /**
   * Constructs a new SingletonConfiguration instance.
   */
  public ContainerSingletonConfiguration() {
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   */
  @Override
  public void receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      if (message.getOperation().equals(AHCSConstants.SINGLETON_AGENT_HOSTS_INFO)) {
        LOGGER.info("Ignoring a reply from a subsequent seeding peer.");
        removeMessageTimeOut(message.getInReplyTo());
        return;
      } else {
        sendMessage(Message.operationNotPermittedMessage(
                message, // receivedMessage
                this)); // skill
        return;
      }
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.NetworkSingletonConfigurationRole. It is expected to be
       * the first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Network Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.NetworkConfigurationRole.
       *
       * The SingletonConfiguration agent/role requests the singleton agent dictionary, agent name --> hosting container name, from a seed
       * peer.
       */
      case AHCSConstants.JOIN_NETWORK_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinNetwork(message);
        return;

      /**
       * Singleton Agent Hosts Info
       *
       * This task message is sent from a peer SingletonConfigurationAgent.SingletonConfigurationRole.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       *
       * As a result, it sends a Singleton Agent Hosts Info message to the TopmostFriendshipAgent / role.
       *
       * The outbound message contains the singleton agent dictionary.
       */
      case AHCSConstants.SINGLETON_AGENT_HOSTS_INFO:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        handleSeedConnectionReply(message);
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton parent NetworkOperationAgent.NetworkSingletonConfigurationRole.
       * It indicates that the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton parent NetworkOperationAgent.NetworkSingletonConfigurationRole.
       * It commands this network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(message);
        return;

      /**
       * Seed Connection Request Info
       *
       * This information request message is sent from a peer SingletonConfiguration agent / role in another container.
       *
       * As parameters, it includes the IP address and port used by the peer, and also the X.509 certificate of the peer making the request
       * to obtain the singleton agent dictionary.
       *
       * As a result, a Singleton Agent Hosts Info message is sent back to the peer, with the SingletonAgentHostsInfo object as a parameter.
       */
      case AHCSConstants.SEED_CONNECTION_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        seedConnectionRequest(message);
        return;

      /**
       * Message Timeout Info
       *
       * This information message is sent from a this skill to itself when an expected reply from a seed peer has not been received.
       *
       * The original Seed Connection Request Info message is included as a parameter.
       *
       * As a result, if no seed peer has yet replied, the seed connection request info message is resent to the seed peer.
       */
      case AHCSConstants.MESSAGE_TIMEOUT_INFO:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) || getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        seedConnectionRequestTimeout(message);
        return;

      /**
       * Configure Singleton Agent Hosts Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.NetworkSingletonConfigurationRole.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       *
       * It results in the Configure Singleton Agent Hosts Task being sent to all child ConfigureParentToSingletonRoles, which every agent
       * has.
       *
       * As an exception to the rule whereby roles have child roles in subordinate agents, the ContainerOperationRole has a child
       * ConfigureParentToSingletonRole in the same agent ContainerOperationAgent.
       */
      case AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        configureSingletonAgentHostsTask(message);
        return;

      /**
       * Add Unjoined Role Info
       *
       * This message is sent from a child role informing that it will attempt to join the network.
       */
      case AHCSConstants.ADD_UNJOINED_ROLE_INFO:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";
        addUnjoinedRole(message);
        return;

      /**
       * Remove Unjoined Role Info
       *
       * This message is sent from a child role informing that has joined the network.
       *
       * As a result, when all child roles have joined the network, a Network Join Complete Info message is sent to network operations
       */
      case AHCSConstants.REMOVE_UNJOINED_ROLE_INFO:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";
        removeUnjoinedRole(message);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

    }
    // otherwise, the message is not understood
    sendMessage(Message.notUnderstoodMessage(
            message, // receivedMessage
            this)); // skill
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
  public Message converseMessage(Message message) {
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
      AHCSConstants.ADD_UNJOINED_ROLE_INFO,
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.JOIN_NETWORK_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.MESSAGE_TIMEOUT_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.REMOVE_UNJOINED_ROLE_INFO,
      AHCSConstants.SEED_CONNECTION_REQUEST_INFO,
      AHCSConstants.SINGLETON_AGENT_HOSTS_INFO,
      AHCSConstants.TASK_ACCOMPLISHED_INFO};
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
   * Initializes the seed node information.
   *
   * @param message the received initialization task message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO
  }

  /**
   * Joins the network.
   *
   * @param message the received Join Network Task message
   */
  @SuppressWarnings("unchecked")
  private void joinNetwork(final Message message) {
    //Preconditions
    assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";

    final String seedNodeIPAddress = System.getenv("AICOIN_SEED_IP");
    if (StringUtils.isNonEmptyString(seedNodeIPAddress)) {
      throw new TexaiException("AICOIN_SEED_IP is not defined in the environment");
    }
    LOGGER.info("initializing the seed node information ...");

    // deserialize the set of SeedNodeInfo objects from the specified file
    final String seedNodeInfosFilePath = "data/SeedNodeInfos.ser";
    LOGGER.info("seedNodeInfosFileHashString\n" + seedNodeInfosFileHashString);
    MessageDigestUtils.verifyFileHash(
            seedNodeInfosFilePath, // filePath
            seedNodeInfosFileHashString); // fileHashString
    try {
      try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(seedNodeInfosFilePath))) {
        seedNodesInfos = (Set<SeedNodeInfo>) objectInputStream.readObject();
      }
    } catch (IOException | ClassNotFoundException ex) {
      throw new TexaiException("cannot find " + seedNodeInfosFilePath);
    }

    LOGGER.info("The locations and credentials of the " + seedNodesInfos.size() + " network seed nodes ...");
    seedNodesInfos.stream().forEach((SeedNodeInfo seedNodeInfo) -> {
      if (getContainerName().equals(Node.extractContainerName(seedNodeInfo.getQualifiedName()))) {
        LOGGER.info("  " + seedNodeInfo + " - (me)");
      } else {
        LOGGER.info("  " + seedNodeInfo);
      }
    });

    seedNodesInfos.stream().forEach((SeedNodeInfo seedNodeInfo) -> {
      if (!getContainerName().equals(Node.extractContainerName(seedNodeInfo.getQualifiedName()))) {
        LOGGER.info("Connecting to seed " + seedNodeInfo);
        connectToSeedPeer(
                seedNodeInfo.getQualifiedName(),
                seedNodeInfo.getHostName(),
                seedNodeInfo.getPort());
      }
    });
  }

  /**
   * Connects to the given seed peer.
   *
   * @param peerQualifiedName the seed peer name
   * @param hostName the host name
   * @param port the port
   */
  private void connectToSeedPeer(
          final String peerQualifiedName,
          final String hostName,
          final int port) {
    //Preconditions
    assert StringUtils.isNonEmptyString(peerQualifiedName) : "peerQualifiedName must be a non-empty string";
    assert StringUtils.isNonEmptyString(hostName) : "hostName must be a non-empty string";
    assert port > 1024 : "port must be greater than 1024";
    assert getRole().getX509Certificate() != null : "X509Certificate must not be null";

    LOGGER.info("connecting to seed peer " + peerQualifiedName + " at " + hostName + ':' + port);
    //compose and send a message to the seed peer
    final Message connectionRequestMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService
            peerQualifiedName, // recipientQualifiedName
            UUID.randomUUID(), // conversationId,
            UUID.randomUUID(), // replyWith,
            getClassName(), // recipientService
            AHCSConstants.SEED_CONNECTION_REQUEST_INFO); // operation
    connectionRequestMessage.put(
            AHCSConstants.MSG_PARM_HOST_NAME,
            hostName);
    connectionRequestMessage.put(
            AHCSConstants.SEED_CONNECTION_REQUEST_INFO_PORT,
            port);
    connectionRequestMessage.put(
            AHCSConstants.MSG_PARM_X509_CERTIFICATE,
            getRole().getX509Certificate());
    // set timeout
    setMessageReplyTimeout(
            connectionRequestMessage,
            10000, // timeoutMillis
            true, // isRecoverable
            null); // recoveryAction
    sendMessageViaSeparateThread(connectionRequestMessage);
  }

  /**
   * Process the received connection request by responding with the current network configuration.
   *
   * @param message the received seed connection request message
   */
  private void seedConnectionRequest(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("received a seed connection request from " + message.getSenderContainerName());

    final SingletonAgentHosts singletonAgentHosts = singletonAgentHosts();
    LOGGER.info(singletonAgentHosts.toDetailedString());

    final Message singletonAgentHostsMessage = makeReplyMessage(
            message, // receivedMessage
            AHCSConstants.SINGLETON_AGENT_HOSTS_INFO); // operation
    singletonAgentHostsMessage.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);
    singletonAgentHostsMessage.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE, getRole().getX509Certificate());

    LOGGER.info("singletonAgentHostsMessage");

    sendMessage(singletonAgentHostsMessage);
  }

  /**
   * Handles the received connection reply.
   *
   * @param message the received seed connection reply message
   */
  private void handleSeedConnectionReply(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.getInReplyTo() != null : "message in-reply-to must not be null\n" + message;

    removeMessageTimeOut(message.getInReplyTo());
    LOGGER.info("received a seed connection reply from " + message.getSenderContainerName());

    if (isSeedConfigurationInfoReceived.getAndSet(true)) {
      LOGGER.info("ignoring a redundant seed connection reply from " + message.getSenderContainerName());
    } else {
      final SingletonAgentHosts singletonAgentHosts
              = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS);
      LOGGER.info(singletonAgentHosts.toDetailedString());

      // send an singletonAgentHosts_Info message to the TopmostFriendshipAgent.
      final String recipientQualifiedName = getRole().getNode().getNodeRuntime().getContainerName() + '.'
              + AHCSConstants.NODE_NAME_TOPMOST_FRIENDSHIP_ROLE;

      final Message singletonAgentHostsMessage = makeMessage(
              recipientQualifiedName,
              TopmostFriendship.class.getName(), // recipientService
              AHCSConstants.SINGLETON_AGENT_HOSTS_INFO); // operation

      singletonAgentHostsMessage.put(
              AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, // parameterName
              singletonAgentHosts); // parameterValue

      sendMessageViaSeparateThread(singletonAgentHostsMessage);
    }
  }

  /**
   * Handles a seed connection request timeout message.
   *
   * @param message the timeout message
   */
  private void seedConnectionRequestTimeout(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (isSeedConfigurationInfoReceived.get()) {
      // no need to retry the seed peer connection as the configuration was provided by another seed peer
      return;
    }

    final Message connectionRequestMessage = (Message) message.get(AHCSConstants.MESSAGE_TIMEOUT_INFO_ORIGINAL_MESSAGE);
    assert connectionRequestMessage.getOperation().equals(AHCSConstants.SEED_CONNECTION_REQUEST_INFO);
    final String peerQualifiedName = connectionRequestMessage.getRecipientQualifiedName();
    final String hostName = (String) connectionRequestMessage.get(AHCSConstants.MSG_PARM_HOST_NAME);
    final int port = (int) connectionRequestMessage.get(AHCSConstants.SEED_CONNECTION_REQUEST_INFO_PORT);

    connectToSeedPeer(
            peerQualifiedName,
            hostName,
            port);
  }

  /**
   * Propagate the task to configure roles for singleton agent hosts.
   *
   * @param message the configure singleton agent hosts task message
   */
  private void configureSingletonAgentHostsTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    getRole().getChildQualifiedNames().stream().sorted().forEach(childQualifiedName -> {
      final Message configureSingletonAgentHostsTask = makeMessage(
              childQualifiedName, // recipientQualifiedName
              ConfigureParentToSingleton.class.getName(), // recipientService
              AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
      configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);
      sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
    });
  }

  /**
   * Adds the given sender to the set of roles which have not yet joined the network.
   *
   * @param message the Add Unjoined Role Info message sent by
   */
  private void addUnjoinedRole(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert StringUtils.isNonEmptyString((String) message.get(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME)) :
            "message missing role qualified name parameter";
    assert !unjoinedChildQualifiedNames.contains((String) message.get(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME)) :
            "duplicate entry for " + message.getSenderQualifiedName();

    synchronized (unjoinedChildQualifiedNames) {
      unjoinedChildQualifiedNames.add((String) message.get(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME));
    }
    LOGGER.debug("added unjoined role " + message.getSenderQualifiedName() + ", count: " + unjoinedChildQualifiedNames.size());
  }

  /**
   * Removes the given sender from the set of roles which have not yet joined the network.
   *
   * @param message the Add Unjoined Role Info message sent by
   */
  private void removeUnjoinedRole(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";
    assert unjoinedChildQualifiedNames.contains(message.getSenderQualifiedName()) : "missing entry for " + message.getSenderQualifiedName();

    boolean isEmpty;
    synchronized (unjoinedChildQualifiedNames) {
      unjoinedChildQualifiedNames.remove(message.getSenderQualifiedName());
      isEmpty = unjoinedChildQualifiedNames.isEmpty();
      LOGGER.debug("removed unjoined role " + message.getSenderQualifiedName() + ", count remaining: " + unjoinedChildQualifiedNames.size());
      //LOGGER.debug(unjoinedChildQualifiedNames);
    }

    if (isEmpty) {
      LOGGER.info("all roles having network singleton parents, joined the network");
      // send a Network Join Complete Info message to NetworkOperationAgent.NetworkSingletonConfigurationRole
      sendMessageViaSeparateThread(makeMessage(
              getRole().getParentQualifiedName(), // recipientQualifiedName
              NetworkSingletonConfiguration.class.getName(), // recipientService
              AHCSConstants.NETWORK_JOIN_COMPLETE_INFO)); // operation
    }
  }

  /**
   * Returns a demo version of the singleton agent hosts assignments, which are all to the Mint peer.
   *
   * @return a demo version of the singleton agent hosts assignments
   */
  private SingletonAgentHosts singletonAgentHosts() {
    final Map<String, String> singletonAgentDictionary = new HashMap<>();

    singletonAgentDictionary.put("NetworkLogControlAgent", "Mint");
    singletonAgentDictionary.put("NetworkDeploymentAgent", "Mint");
    singletonAgentDictionary.put("NetworkFileTransferAgent", "Mint");
    singletonAgentDictionary.put("NetworkOperationAgent", "Mint");
    singletonAgentDictionary.put("NetworkSingletonConfigurationAgent", "Mint");
    singletonAgentDictionary.put("TopLevelGovernanceAgent", "Mint");
    singletonAgentDictionary.put("TopLevelHeartbeatAgent", "Mint");
    singletonAgentDictionary.put("TopmostFriendshipAgent", "Mint");
    singletonAgentDictionary.put("XAIFinancialAccountingAndControlAgent", "Mint");
    singletonAgentDictionary.put("XAIMintAgent", "Mint");
    singletonAgentDictionary.put("XAINetworkEpisodicMemoryAgent", "Mint");
    singletonAgentDictionary.put("XAINetworkOperationAgent", "Mint");
    singletonAgentDictionary.put("XAINetworkSeedAgent", "Mint");
    singletonAgentDictionary.put("XAIPrimaryAuditAgent", "Mint");
    singletonAgentDictionary.put("XAIRecoveryAgent", "Mint");
    singletonAgentDictionary.put("XAIRewardAllocationAgent", "Mint");

    final DateTime effectiveDateTime = new DateTime(
            2014, // year
            11, // monthOfYear,
            14, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    final DateTime terminationDateTime = new DateTime(
            2015, // year
            11, // monthOfYear,
            14, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    final DateTime createdDateTime = new DateTime(
            2014, // year
            11, // monthOfYear,
            13, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    final byte[] authorSignatureBytes = SingletonAgentHosts.signSingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime,
            getQualifiedName(), // authorQualifiedName,
            createdDateTime,
            getRole().getX509SecurityInfo().getPrivateKey());

    return new SingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime,
            getQualifiedName(), // authorQualifiedName,
            createdDateTime,
            authorSignatureBytes);
  }

  /**
   * Perform this role's mission, which is to configure the roles in this container whose parents are nomadic singleton roles hosted on a
   * probably remote super-peer.
   *
   * @param message the received perform mission task message
   */
  @SuppressWarnings("unchecked")
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("performing the mission");

  }

}