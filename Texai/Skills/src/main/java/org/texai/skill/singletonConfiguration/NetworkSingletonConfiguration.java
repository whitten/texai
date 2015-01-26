package org.texai.skill.singletonConfiguration;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;

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

  /**
   * Constructs a new XTCNetworkConfiguration instance.
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
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Network Task
       *
       * This task message is sent from the local parent NetworkOperationAgent.NetworkOperationRole.
       *
       * As the result, a Join Network Task message is sent to its child SingletonConfiguration agent/role. The SingletonConfiguration
       * agent/role requests the singleton agent dictionary, agent name --> hosting container name, from a seed peer.
       */
      case AHCSConstants.JOIN_NETWORK_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinNetwork(message);
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
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
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
        LOGGER.info("Ignoring a configuration information timeout reported by " + message.getSenderQualifiedName());
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:

        LOGGER.warn(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      // REQUEST_TO_JOIN_INFO message from new node wanting to join the network.
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
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.DELEGATE_BECOME_READY_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_NETWORK_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.MESSAGE_TIMEOUT_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.TASK_ACCOMPLISHED_INFO
    };
  }

  /**
   * Joins the network.
   *
   * @param message the Join Network Task message
   */
  private void joinNetwork(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("joining the network");

    // send the join network task to the SingletonConfigurationAgent
    sendMessageViaSeparateThread(makeMessage(
            getRole().getChildQualifiedNameForAgent("SingletonConfigurationAgent"), // recipientQualifiedName
            SingletonConfiguration.class.getName(), // recipientService
            AHCSConstants.JOIN_NETWORK_TASK)); // operation
  }

  /**
   * Perform this role's mission, which is to configure the various singleton nomadic agents on containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("performing the mission");

    final String recipientQualifiedName = getRole().getFirstChildQualifiedNameForAgent("SingletonConfigurationAgent");
    final Message performMissionMessage = makeMessage(
            recipientQualifiedName,
            SingletonConfiguration.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    //TODO parent of this role should be NetworkOperations
  }

}
