/*
 * TopmostFriendship.java
 *
 * Created on Jun 23, 2010, 10:58:37 AM
 *
 * Description: Provides topmost perception and friendship behavior.
 *
 * Copyright (C) Jun 23, 2010, Stephen L. Reed.
 */
package org.texai.skill.governance;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration;
import org.texai.util.TexaiException;

/**
 * Provides topmost perception and friendship behavior.
 *
 * @author reed
 */
@ThreadSafe
public final class TopmostFriendship extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TopmostFriendship.class);

  /**
   * Constructs a new TopmostFriendship instance.
   */
  public TopmostFriendship() {
  }

  /**
   * Receives and attempts to process the given message.
   *
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
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
       * This task message is sent from AIMain via the ContainerOperationAgent.ContainerOperationRole. It is expected to be the first task
       * message that this topmost role receives and it results in the role being initialized and propagation of that message to all agent /
       * roles.
       *
       * If this is the first container in the network, then a Perform Mission Task message is sent to the child NetworkOperation
       * agent/role. Otherwise a Join Network Task message is sent.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(receivedMessage);
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
      case AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleNetworkJoinCompleteSensation(receivedMessage);
        return;

      // handle other operations ...
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;

    }

    // not understood
    sendMessage(
            receivedMessage,
            Message.notUnderstoodMessage(
            receivedMessage, // receivedMessage
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
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION,
      AHCSConstants.SINGLETON_AGENT_HOSTS_INFO
    };
  }

  /**
   * Performs the initialization operation.
   *
   * @param receivedMessage the received initialization task message
   */
  private void initialization(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    // initialize the child roles
    LOGGER.info("initializing child roles");
    propagateOperationToChildRoles(receivedMessage);

    if (getNodeRuntime().isFirstContainerInNetwork()) {
      setSkillState(State.READY);

      //TODO wait for child roles to be initialized
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ex) {
      }
      performMission(receivedMessage);
    } else {
      setSkillState(State.ISOLATED_FROM_NETWORK);
      joinNetwork(receivedMessage);
    }
  }

  /**
   * Joins the network by sending a Join Network Task message to the NetworkOperationAgent.NetworkSingletonConfigurationRole.
   *
   * @param receivedMessage the received message
   */
  private void joinNetwork(final Message receivedMessage) {
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";

    LOGGER.info("joining the network");
    // send a performMission task message to the NetworkOperationAgent
    final Message performMissionMessage = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgentRole("NetworkOperationAgent.NetworkSingletonConfigurationRole"), // recipientQualifiedName,
            NetworkSingletonConfiguration.class.getName(), // recipientService
            AHCSConstants.JOIN_NETWORK_TASK); // operation
    sendMessage(receivedMessage, performMissionMessage);
  }

  /**
   * Perform this role's mission, which is to provide topmost perception and friendship behavior.
   *
   * @param receivedMessage the received message
   */
  private void performMission(final Message receivedMessage) {
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(State.READY) : "state must be ready";
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");
    propagateOperationToChildRolesSeparateThreads(receivedMessage);
  }

  /**
   * Handles the sensation that a new container has completed joining the network.
   *
   * @param receivedMessage the Network Join Complete Sensation message
   */
  private void handleNetworkJoinCompleteSensation(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    final String containerName = (String) receivedMessage.get(AHCSConstants.MSG_PARM_CONTAINER_NAME);
    LOGGER.info("Sensed that container " + containerName + " has completely joined the network");

    // ensure that no child network-singleton roles belong to the joining container
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && isNetworkSingletonRole) {
        throw new TexaiException("invalid network-singleton child role from joining container " + childQualifiedName);
      }
    });

    // synchronously propagate the Perform Mission Task to the joined container's child roles
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      String operation = null;
      if (containerName.equals(Node.extractContainerName(childQualifiedName))) {
        // the joined container hosts this child role - make it ready and command it to perform its mission
        operation = AHCSConstants.PERFORM_MISSION_TASK;
      } else if (getRole().isNetworkSingletonRole(childQualifiedName)) {
        // this role is a network singleton - it delegates the command to its child roles
        operation = AHCSConstants.DELEGATE_PERFORM_MISSION_TASK;
      }
      if (operation != null) {
        final Message outboundMessage = makeMessage(
                childQualifiedName, // recipientQualifiedName
                null, // recipientService
                operation);
        outboundMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, receivedMessage.get(AHCSConstants.MSG_PARM_CONTAINER_NAME));
        sendMessage(receivedMessage, outboundMessage);
      }
    });
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

}
