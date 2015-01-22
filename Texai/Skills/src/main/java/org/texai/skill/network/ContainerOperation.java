package org.texai.skill.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.singletonConfiguration.ConfigureParentToSingleton;
import org.texai.skill.support.NodeRuntimeSkill;
import org.texai.util.StringUtils;

/**
 * Created on Sep 1, 2014, 1:48:49 PM.
 *
 * Description: Manages the network, the containers, and the coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) Sep 1, 2014, Stephen L. Reed, Texai.org.
 */
@ThreadSafe
public final class ContainerOperation extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerOperation.class);
  // the unjoined child roles
  private final Set<String> unjoinedChildQualifiedNames = new HashSet<>();

  /**
   * Constructs a new ContainerOperation instance.
   */
  public ContainerOperation() {
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
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        /**
         * Initialize Task
         *
         * This task message is sent from the container-local parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the
         * first task message that this role receives and it results in the role being initialized.
         */
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Delegate Configure Singleton Agent Hosts Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.NetworkOperationRole.
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
      case AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        configureSingletonAgentHostsTask(message);
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
        joinAcknowledgedTask(message);
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent NetworkOperationAgent.NetworkOperationRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       *
       * As a result, a Perform Mission Task message is sent to the node runtime, which will open a message listening port
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        performMission(message);
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
        LOGGER.warn(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      // handle other operations ...
      // handle other operations ...
    }

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
      AHCSConstants.ADD_UNJOINED_ROLE_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.BECOME_READY_TASK,
      AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.OPERATION_NOT_PERMITTED_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.REMOVE_UNJOINED_ROLE_INFO
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // send a Listen For Connections Task message to the node runtime, which will open a message listening port
    final Message performMissionMessage = makeMessage(
            NodeRuntimeSkill.class.getName(), // recipientService
            AHCSConstants.LISTEN_FOR_CONNECTIONS_TASK, // operation
            new HashMap<>()); // parameterDictionary
    sendMessageViaSeparateThread(performMissionMessage);
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
    assert StringUtils.isNonEmptyString((String) message.get(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME)) : "message missing role qualified name parameter";
    assert !unjoinedChildQualifiedNames.contains((String) message.get(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME)) : "duplicate entry for " + message.getSenderQualifiedName();

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
      // send a Network Join Complete Info message to network operations
      sendMessageViaSeparateThread(makeMessage(
              getRole().getParentQualifiedName(), // recipientQualifiedName
              NetworkOperation.class.getName(), // recipientService
              AHCSConstants.NETWORK_JOIN_COMPLETE_INFO)); // operation
    }
  }

}
