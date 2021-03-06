package org.texai.skill.singletonConfiguration;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
import org.texai.skill.governance.TopmostFriendship;

/**
 * Created on Aug 29, 2014, 6:44:08 PM.
 *
 * Description: Configures the various singleton nomadic agents on containers.
 *
 * Copyright (C) Aug 29, 2014, Stephen L. Reed, Texai.org.
 */
@ThreadSafe
public final class NetworkSingletonConfiguration extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkSingletonConfiguration.class);
  // the container names
  private final Set<String> containerNames = new HashSet<>();

  /**
   * Constructs a new NetworkConfiguration instance.
   */
  public NetworkSingletonConfiguration() {
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
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param receivedMessage the received message
   */
  @Override
  public void receiveMessage(Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = receivedMessage.getOperation();
    if (!isOperationPermitted(receivedMessage)) {
      sendOperationNotPermittedInfoMessage(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(receivedMessage);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
          containerNames.add(getContainerName());
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Network Task
       *
       * This task message is sent from the local parent TopmostFriendshipAgent.TopmostFriendshipRole.
       *
       * As the result, a Join Network Task message is sent to the child NetworkSingletonConfiguration agent/role, which in turn sends a
       * Join Network Task message to its child SingletonConfiguration agent/role. The SingletonConfiguration agent/role requests the
       * singleton agent dictionary, agent name --> hosting container name, from a seed peer.
       */
      case AHCSConstants.JOIN_NETWORK_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinNetwork(receivedMessage);
        return;

      /**
       * Configure Singleton Agent Hosts Request Info
       *
       * This information message is sent from the ContainerOperationAgent.SingletonConfigurationRole.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       *
       * A a result, aConfigure Singleton Agent Hosts Task message is sent to container operations, which in turn sends a Configure
       * Singleton Agent Hosts Task to all child ConfigureParentToSingletonRoles, which includes every agent in the container.
       *
       * Each outbound message contains the singleton agent dictionary.
       *
       */
      case AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        handleSingletonAgentHostsInfo(receivedMessage);
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(receivedMessage);
        return;

      /**
       * Join Network Singleton Agent Info
       *
       * This task message is sent to this network singleton agent/role from a child role in another container.
       *
       * The sender is requesting to join the network as child of this role.
       *
       * The message parameter is the X.509 certificate belonging to the sender agent / role.
       *
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(receivedMessage);
        return;

      /**
       * Delegate Perform Mission Task
       *
       * A container has completed joining the network. Propagate a Delegate Perform Mission Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegatePerformMissionTask(receivedMessage);
        return;

      /**
       * Message Timeout Info
       *
       * This information message is sent from a child SingletonConfigurationRole to when an expected reply from a seed peer has not been
       * received.
       *
       * The original Seed Connection Request Info message is included as a parameter.
       *
       * This message is ignored because the child will keep requesting the seed information.
       */
      case AHCSConstants.MESSAGE_TIMEOUT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        LOGGER.info("Ignoring a configuration information timeout reported by " + receivedMessage.getSenderQualifiedName());
        return;

      /**
       * Network Join Complete Info
       *
       * This information message is sent from a child ContainerOperaton agent/role that has completed joining the network.
       *
       * It results in a Network Join Complete Sensation message sent to the parent TopmostFriendship agent / role.
       */
      case AHCSConstants.NETWORK_JOIN_COMPLETE_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleNetworkJoinCompleteInfo(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;

      // REQUEST_TO_JOIN_INFO message from new node wanting to join the network.
    }

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
      AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_REQUEST_INFO,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_NETWORK_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.MESSAGE_TIMEOUT_INFO,
      AHCSConstants.NETWORK_JOIN_COMPLETE_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.TASK_ACCOMPLISHED_INFO
    };
  }

  /**
   * Joins the network.
   *
   * @param receivedMessage the Join Network Task message
   */
  private void joinNetwork(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    LOGGER.info("joining the network");

    // send the join network task to the SingletonConfigurationAgent
    sendMessageViaSeparateThread(
            receivedMessage,
            makeMessage(
                    getRole().getChildQualifiedNameForAgentRole("ContainerOperationAgent.ContainerSingletonConfigurationRole"), // recipientQualifiedName
                    ContainerSingletonConfiguration.class.getName(), // recipientService
                    AHCSConstants.JOIN_NETWORK_TASK)); // operation
  }

  /**
   * Pass down the task to configure roles for singleton agent hosts.
   *
   * @param receivedMessage the configure singleton agent hosts task message
   */
  private void handleSingletonAgentHostsInfo(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) receivedMessage.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // send message to container operations to configure parent roles throughout the container.
    final Message configureSingletonAgentHostsTask = makeMessage(
            receivedMessage.getSenderQualifiedName(), // recipientQualifiedName
            receivedMessage.getSenderService(), // recipientService
            AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE, getRole().getX509Certificate());

    sendMessageViaSeparateThread(receivedMessage, configureSingletonAgentHostsTask);
  }

  /**
   * Perform this role's mission, which is to configure the various singleton nomadic agents on containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");
    propagateOperationToChildRolesSeparateThreads(receivedMessage);

    // every five minutes broadcast the network configuration node infos via child roles
    final BroadcastContainerInfos broadcastContainerInfos = new BroadcastContainerInfos(
            this); // networkSingletonConfiguration
    getNodeRuntime().getTimer().scheduleAtFixedRate(
            broadcastContainerInfos, // task
            300_000, // delay - 5 minutes
            300_000); // period - 5 minutes
  }

  /**
   * Handles the Network Join Complete information message, which is sent from a remote agent/role that has completed joining the network.
   * Each role in the joined container having a network singleton agent/role as a parent, now refers to the corresponding network singleton
   * agent / role and has exchanged X.509 certificates with it.
   *
   * @param receivedMessage the received Network Join Complete Info message
   */
  private void handleNetworkJoinCompleteInfo(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
    if (containerNames.contains(receivedMessage.getSenderContainerName())) {
      LOGGER.info(new StringBuilder().append("Container ").append(receivedMessage.getSenderContainerName()).append(" has rejoined the Network.").toString());
    } else {
      LOGGER.info(new StringBuilder().append("Container ").append(receivedMessage.getSenderContainerName()).append(" has joined the Network.").toString());
      containerNames.add(receivedMessage.getSenderContainerName());
    }

    /**
     * Notify the parent TopmostFriendship agent / role that a container has completed joining the network. The TopmostFriendship agent /
     * role synchronously sends a Become Ready Task message to all child roles in the container, who each set their State to initialized and
     * return immediately
     */
    final Message networkJoinCompleteSensationMessage = makeMessage(
            getRole().getParentQualifiedName(), // recipientQualifiedName
            TopmostFriendship.class.getName(), // recipientService
            AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION); // operation
    networkJoinCompleteSensationMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, receivedMessage.getSenderContainerName());
    sendMessageViaSeparateThread(receivedMessage, networkJoinCompleteSensationMessage);
  }

  /**
   * Periodically broadcasts the node info objects that inform containers how the network is configured.
   */
  protected static class BroadcastContainerInfos extends TimerTask {

    // the network singleton configuration skill
    private final NetworkSingletonConfiguration networkSingletonConfiguration;

    /**
     * Constructs a new HeartbeatProcessor instance.
     *
     * @param networkSingletonConfiguration the network singleton configuration skill
     */
    BroadcastContainerInfos(final NetworkSingletonConfiguration networkSingletonConfiguration) {
      //Preconditions
      assert networkSingletonConfiguration != null : "networkSingletonConfiguration must not be null";

      this.networkSingletonConfiguration = networkSingletonConfiguration;
    }

    /**
     * Broadcasts the node info objects that inform containers how the network is configured.
     */
    @Override
    public void run() {
      for (final String childQualifiedName : networkSingletonConfiguration.getRole().getChildQualifiedNames()) {
        final Message networkConfigurationMessage = networkSingletonConfiguration.makeMessage(
                childQualifiedName, // recipientQualifiedName
                ContainerSingletonConfiguration.class.getName(), // recipientService
                AHCSConstants.NETWORK_CONFIGURATION_TASK); // operation
        final SingletonAgentHosts singletonAgentHosts = networkSingletonConfiguration.getNodeRuntime().getSingletonAgentHosts();
        assert singletonAgentHosts != null;
        networkConfigurationMessage.put(
                AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS,
                singletonAgentHosts);
        networkConfigurationMessage.put(
                AHCSConstants.MSG_PARM_CONTAINER_INFOS,
                networkSingletonConfiguration.getNodeRuntime().getContainerInfos());
        if (networkSingletonConfiguration.isUnitTest()) {
          // use the same thread for deterministic unit tests
          networkSingletonConfiguration.sendMessage(
                  null, // receivedMessage, for conversation tracing
                  networkConfigurationMessage); // message
        } else {
          // use a separate thread - in case of an exception when sending, the original timer thread is preserved
          networkSingletonConfiguration.sendMessageViaSeparateThread(
                  null, // receivedMessage, for conversation tracing
                  networkConfigurationMessage); // message
        }
      }
    }
  }

}
