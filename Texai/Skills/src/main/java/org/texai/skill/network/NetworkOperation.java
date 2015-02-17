package org.texai.skill.network;

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.Node;

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
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent TopmostFriendshipAgent.TopmostFriendshipRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        LOGGER.info("initializing with: " + receivedMessage);

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
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It indicates that the
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
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
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
        handleNetworkRestartRequestInfo(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.NETWORK_RESTART_REQUEST_INFO,
      AHCSConstants.TRANSFER_FILE_REQUEST_INFO
    };
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");

    propagateOperationToChildRolesSeparateThreads(receivedMessage);
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void handleNetworkRestartRequestInfo(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("Handling a network restart request");

    // send the restart container task to XAI network operation which will task each XAI operation role to shut down aicoind instances.
    final String recipientQualifiedName
            = getRole().getChildQualifiedNameForAgentRole("XAINetworkOperationAgent.XAINetworkOperationRole");
    final Message restartContainerTaskMessage1 = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService
            recipientQualifiedName,
            "org.texai.skill.aicoin.XAINetworkOperation", // recipientService
            AHCSConstants.RESTART_CONTAINER_TASK); // operation
    sendMessage(receivedMessage, restartContainerTaskMessage1);

    try {
      // pause 10 seconds
      Thread.sleep(10000);
    } catch (InterruptedException ex) {
      // ignore
    }

    // send the restart container task to every child container operation role.
    if (LOGGER.isDebugEnabled()) {
      final List<String> childQualifiedNames = new ArrayList<>(getRole().getChildQualifiedNames());
      LOGGER.debug("childQualifiedNames...");
      childQualifiedNames.stream().sorted().forEach((String childQualifiedName) -> {
        LOGGER.debug("  " + childQualifiedName);
      });
    }
    final List<String> childQualifiedNames = new ArrayList<>(getRole().getChildQualifiedNames());
    LOGGER.info("childQualifiedNames...");
    childQualifiedNames.stream().sorted().forEach((String childQualifiedName) -> {
      LOGGER.info("  " + childQualifiedName);
    });
   // restart every other container with a 60 second delay
    getRole().getChildQualifiedNamesForAgent("ContainerOperationAgent").forEach((String childQualifiedName) -> {
      final Message restartContainerTaskMessage2 = new Message(
              getQualifiedName(), // senderQualifiedName
              getClassName(), // senderService
              childQualifiedName, // recipientQualifiedName
              ContainerOperation.class.getName(), // recipientService
              AHCSConstants.RESTART_CONTAINER_TASK); // operation
      if (!Node.extractContainerName(childQualifiedName).equals(getContainerName())) {
        restartContainerTaskMessage2.put(AHCSConstants.RESTART_CONTAINER_TASK_DELAY, 60000L);
      }
      sendMessageViaSeparateThread(receivedMessage, restartContainerTaskMessage2);
    });
    try {
      // pause 10 seconds to ensure that the restart messages all get sent
      Thread.sleep(10000);
    } catch (InterruptedException ex) {
      //ignore
    }
   // restart this container with a 15 second delay
    final Message restartContainerTaskMessage2 = new Message(
            getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService
            getContainerName() + ".ContainerOperationAgent.ContainerOperationRole", // recipientQualifiedName
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.RESTART_CONTAINER_TASK); // operation
    restartContainerTaskMessage2.put(AHCSConstants.RESTART_CONTAINER_TASK_DELAY, 15000L);
    sendMessageViaSeparateThread(receivedMessage, restartContainerTaskMessage2);
  }

}
