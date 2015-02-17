/*
 * XAINetworkOperation.java
 *
 * Created on Sep 18, 2014, 8:10:02 AM
 *
 * Description: Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) 2014 Stephen L. Reed
 *
 */
package org.texai.skill.aicoin;

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;

/**
 * Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * @author reed
 */
@ThreadSafe
public final class XAINetworkOperation extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAINetworkOperation.class);

  /**
   * Constructs a new XTCNetworkOperation instance.
   */
  public XAINetworkOperation() {
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
   * @param receivedMessage the given message
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
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first
       * task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(receivedMessage);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole.
       * It indicates that the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole.
       * It commands this network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
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
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509
       * certificate as the message parameter.
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
       * Restart Container Task
       *
       * This message is sent the parent XAINetworkOperationAgent.XAINetworkOperationRole instructing the container to restart following a given
       * delay.
       *
       * As a result, a Shutdown Aicoind Task is sent to each child XAIOperationRole.
       */
      case AHCSConstants.RESTART_CONTAINER_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleRestartContainerTask(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
      // handle other operations ...
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
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.RESTART_CONTAINER_TASK
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");
    propagateOperationToChildRolesSeparateThreads(receivedMessage);
  }

  /**
   * Handles the received Restart Container Task by sending a Shutdown Aicoind Task to each child XAIOperationRole.
   *
   * @param receivedMessage the received Restart Container Task message
   */
  private void handleRestartContainerTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    // send the restart container task to every child container operation role.
    final List<String> childQualifiedNames = new ArrayList<>(getRole().getChildQualifiedNames());
    LOGGER.info("childQualifiedNames...");
    childQualifiedNames.stream().sorted().forEach((String childQualifiedName) -> {
      LOGGER.info("  " + childQualifiedName);
    });
    getRole().getChildQualifiedNamesForAgent("XAIOperationAgent").forEach((String childQualifiedName) -> {
      final Message restartContainerTaskMessage2 = new Message(
              getQualifiedName(), // senderQualifiedName
              getClassName(), // senderService
              childQualifiedName, // recipientQualifiedName
              XAIOperation.class.getName(), // recipientService
              AHCSConstants.SHUTDOWN_AICOIND_TASK); // operation
      sendMessage(receivedMessage, restartContainerTaskMessage2);
    });
  }

}
