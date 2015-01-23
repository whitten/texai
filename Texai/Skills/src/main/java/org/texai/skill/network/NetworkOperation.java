package org.texai.skill.network;

import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.governance.TopmostFriendship;
import org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration;

/**
 * Created on Aug 30, 2014, 11:30:19 PM.
 *
 * Description: Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
 */
@ThreadSafe
public final class NetworkOperation extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkOperation.class);
  // the container names
  private final Set<String> containerNames = new HashSet<>();

  /**
   * Constructs a new NetworkOperation instance.
   */
  public NetworkOperation() {
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
   * @param message the given message
   */
  @Override
  public void receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(Message.operationNotPermittedMessage(
              message, // receivedMessage
              this)); // skill
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent TopmostFriendshipAgent.TopmostFriendshipRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(operation);
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
        joinNetwork();
        return;

      /**
       * Delegate Configure Singleton Agent Hosts Task
       *
       * This task message is sent from the local parent TopmostFriendshipAgent.TopmostFriendshipRole.
       *
       * The task is delegated to the container operation agent/role which in turn delegates it to the singleton configuration agent/role.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       */
      case AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        configureSingletonAgentHostsTask(message);
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        performMission(message);
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
        joinNetworkSingletonAgent(message);
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
        handleNetworkJoinCompleteInfo(message);
        return;

      /**
       * Delegate Become Ready Task
       *
       * A container has completed joining the network. Propagate a Delegate Become Ready Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_BECOME_READY_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegateBecomeReadyTask(message);
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
        handleDelegatePerformMissionTask(message);
        return;

      /**
       * Network Restart Request Info
       *
       * The child network deployment agent has completed deploying software and data files to all containers, and is requesting a network
       * restart.
       *
       * This results in a restart container task being sent to all containers. Termination of each container follows a certain delay in
       * order for all containers to receive the message before this one terminates.
       */
      case AHCSConstants.NETWORK_RESTART_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleNetworkRestartRequestInfo(message);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      // handle other operations ...
    }

    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.DELEGATE_BECOME_READY_TASK,
      AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.JOIN_NETWORK_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.NETWORK_JOIN_COMPLETE_INFO,
      AHCSConstants.NETWORK_RESTART_REQUEST_INFO
    };
  }

  /**
   * Joins the network.
   *
   */
  private void joinNetwork() {
    //Preconditions
    assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";

    LOGGER.info("joining the network");

    // send the join network task to the NetworkSingletonConfigurationAgent
    sendMessageViaSeparateThread(makeMessage(
            getRole().getChildQualifiedNameForAgent("NetworkSingletonConfigurationAgent"), // recipientQualifiedName
            NetworkSingletonConfiguration.class.getName(), // recipientService
            AHCSConstants.JOIN_NETWORK_TASK)); // operation
  }

  /**
   * Handles the Network Join Complete information message, which is sent from a ContainerOperation agent/role that has completed joining
   * the network. Each role in the joined container having a network singleton agent/role as a parent, now refers to the corresponding
   * network singleton agent / role and has exchanged X.509 certificates with it.
   *
   * @param message the received Network Join Complete Info message
   */
  private void handleNetworkJoinCompleteInfo(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
    if (containerNames.contains(message.getSenderContainerName())) {
      LOGGER.info(new StringBuilder().append("Container ").append(message.getSenderContainerName()).append(" has rejoined the Network.").toString());
    } else {
      LOGGER.info(new StringBuilder().append("Container ").append(message.getSenderContainerName()).append(" has joined the Network.").toString());
      containerNames.add(message.getSenderContainerName());
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
    networkJoinCompleteSensationMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, message.getSenderContainerName());
    sendMessageViaSeparateThread(networkJoinCompleteSensationMessage);
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");

    // send the perform mission task to the XAINetworkOperationAgent
    Message performMissionMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("XAINetworkOperationAgent"), // recipientQualifiedName,
            "org.texai.skill.aicoin.XAINetworkOperation", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    // send the perform mission task to the NetworkSingletonConfigurationAgent
    performMissionMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("NetworkSingletonConfigurationAgent"), // recipientQualifiedName,
            "org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    // send the perform mission task to the ContainerOperationAgent
    performMissionMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("ContainerOperationAgent"), // recipientQualifiedName,
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    // send the perform mission task to the NetworkDeploymentAgent
    performMissionMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("NetworkDeploymentAgent"), // recipientQualifiedName,
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param message the received perform mission task message
   */
  private void handleNetworkRestartRequestInfo(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("Handling a network restart request");

    // send the restart container task to every child container operation role.
    getRole().getChildQualifiedNamesForAgent("ContainerOperationAgent").forEach((String childQualifiedName) -> {
      final Message restartContainerTaskMessage = new Message(
              getQualifiedName(), // senderQualifiedName
              getClassName(), // senderService
              childQualifiedName, // recipientQualifiedName
              ContainerOperation.class.getName(), // recipientService
              AHCSConstants.RESTART_CONTAINER_TASK); // operation
      restartContainerTaskMessage.put(AHCSConstants.RESTART_CONTAINER_TASK_DELAY, 5000);
      sendMessage(restartContainerTaskMessage);
    });
  }

  /**
   * Pass down the task to configure roles for singleton agent hosts.
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

    // send message to container operations to configure parent roles throughout the container.
    final Message configureSingletonAgentHostsTask = makeMessage(
            getRole().getChildQualifiedNameForAgent("ContainerOperationAgent"), // recipientQualifiedName
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);

    sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
  }

}
