/*
 * SingletonConfiguration.java
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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
import org.texai.skill.network.ContainerOperation;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
@ThreadSafe
public class SingletonConfiguration extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(SingletonConfiguration.class);
  // the locations and credentials for network seed nodes
  private Set<SeedNodeInfo> seedNodesInfos;
  // the SHA-512 hash of the seed node infos serialized file
  private final String seedNodeInfosFileHashString
          = "CqpjZ7ef3wZllB+MNAlMk/THJOHVMDN4sXqLoixftqOtFvBJ/Yo/f8J4uBPMZUMZSXrHgTpsT9iB1JETvffaYQ==";

  /**
   * Constructs a new SingletonConfiguration instance.
   */
  public SingletonConfiguration() {
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   *
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent NetworkConfigurationAgent.NetworkConfigurationRole.
       * It is expected to be the first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      /**
       * Join Network Task
       *
       * This task message is sent from the container-local parent NetworkConfigurationAgent.NetworkConfigurationRole.
       *
       * The SingletonConfiguration agent/role requests the singleton agent dictionary, agent name  --> hosting container name,
       * from a seed peer.
       */
      case AHCSConstants.JOIN_NETWORK_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinNetwork(message);
        return true;

      /**
       * Singleton Agent Hosts Info
       *
       * This task message is sent from a peer SingletonConfigurationAgent.SingletonConfigurationRole.
       *
       * Its parameter is the SingletonAgentHosts object,
       * which contains the the singleton agent dictionary, agent name  --> hosting container name.
       *
       * As a result, it sends a Singleton Agent Hosts Info message to the TopmostFriendshipAgent / role.
       *
       * The outbound message contains the singleton agent dictionary.
       */
      case AHCSConstants.SINGLETON_AGENT_HOSTS_INFO:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        handleSeedConnectionReply(message);
        return true;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton parent NetworkSingletonConfigurationAgent.NetworkSingletonConfigurationRole.
       * It indicates that the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton parent NetworkSingletonConfigurationAgent.NetworkSingletonConfigurationRole.
       * It commands this network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(message);
        return true;

      /**
       * Seed Connection Request Info
       *
       * This information request message is sent from a peer SingletonConfiguration agent / role in another container.
       *
       * As parameters, it includes the IP address and port used by the peer, and also the X.509 certificate
       * of the peer making the request to obtain the singleton agent dictionary.
       *
       * As a result, a Singleton Agent Hosts Info message is sent back to the peer,
       * with the SingletonAgentHostsInfo object as a parameter.
       */
      case AHCSConstants.SEED_CONNECTION_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        seedConnectionRequest(message);
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

    }
    // otherwise, the message is not understood
    sendMessage(notUnderstoodMessage(message));
    return true;
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

    //TODO handle operations
    return (notUnderstoodMessage(message));
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.SEED_CONNECTION_REQUEST_INFO,
      AHCSConstants.SINGLETON_AGENT_HOSTS_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.JOIN_NETWORK_TASK,
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

    LOGGER.info("initializing the seed node information ...");

    // deserialize the set of SeedNodeInfo objects from the specified file
    final String seedNodeInfosFilePath = "data/SeedNodeInfos.ser";
    LOGGER.info("seedNodeInfosFileHashString\n" + seedNodeInfosFileHashString);
    X509Utils.verifyFileHash(
            seedNodeInfosFilePath, // filePath
            seedNodeInfosFileHashString); // fileHashString
    try {
      try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(seedNodeInfosFilePath))) {
        seedNodesInfos = (Set<SeedNodeInfo>) objectInputStream.readObject();
      }
    } catch (IOException | ClassNotFoundException ex) {
      throw new TexaiException("cannot find " + seedNodeInfosFilePath);
    }

    final AtomicBoolean isSeedNode = new AtomicBoolean(false);
    LOGGER.info("the locations and credentials of the network seed nodes ...");
    seedNodesInfos.stream().forEach((SeedNodeInfo seedNodeInfo) -> {
      if (getContainerName().equals(Node.extractContainerName(seedNodeInfo.getQualifiedName()))) {
        LOGGER.info("  " + seedNodeInfo + " - (me)");
        isSeedNode.set(true);
      } else {
        LOGGER.info("  " + seedNodeInfo);
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
    final Map<String, Object> parameterDictionary = new HashMap<>();
    parameterDictionary.put(
            AHCSConstants.SEED_CONNECTION_REQUEST_INFO_HOST_NAME,
            hostName);
    parameterDictionary.put(
            AHCSConstants.SEED_CONNECTION_REQUEST_INFO_PORT,
            port);
    parameterDictionary.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE,
            getRole().getX509Certificate());
    final Message connectionRequestMessage = makeMessage(
            peerQualifiedName, // recipientQualifiedName
            getClassName(), // recipientService
            AHCSConstants.SEED_CONNECTION_REQUEST_INFO, // operation
            parameterDictionary);
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

    final Map<String, Object> parameterDictionary = new HashMap<>();
    final SingletonAgentHosts singletonAgentHosts = singletonAgentHosts();
    parameterDictionary.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);
    parameterDictionary.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE, getRole().getX509Certificate());
    LOGGER.info(singletonAgentHosts.toDetailedString());

    final Message singletonAgentHostsMessage = makeMessage(
            message.getSenderQualifiedName(), // recipientQualifiedName
            message.getSenderService(), // recipientService
            AHCSConstants.SINGLETON_AGENT_HOSTS_INFO, // operation
            parameterDictionary); // parameterDictionary

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

    LOGGER.info("received a seed connection reply from " + message.getSenderContainerName());
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

  /**
   * Returns a demo version of the singleton agent hosts assignments, which are all to the Mint peer.
   *
   * @return a demo version of the singleton agent hosts assignments
   */
  private SingletonAgentHosts singletonAgentHosts() {
    final Map<String, String> singletonAgentDictionary = new HashMap<>();
    singletonAgentDictionary.put("NetworkLogControlAgent", "Mint");
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
            getRole().getQualifiedName(), // authorQualifiedName,
            createdDateTime,
            getRole().getX509SecurityInfo().getPrivateKey());

    return new SingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime,
            getRole().getQualifiedName(), // authorQualifiedName,
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

  /**
   * Receive the new parent role's acknowledgement of joining the network.
   *
   * @param message the received perform mission task message
   */
  private void joinAcknowledgedTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("join acknowledged from " + message.getSenderQualifiedName());
    final Message removeUnjoinedRoleInfoMessage = makeMessage(
            getContainerName() + ".ContainerOperationAgent.ContainerOperationRole", // recipientQualifiedName
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.REMOVE_UNJOINED_ROLE_INFO); // operation
    sendMessageViaSeparateThread(removeUnjoinedRoleInfoMessage);
  }

}
