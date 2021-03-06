package org.texai.skill.network;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

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
      case AHCSConstants.INITIALIZE_TASK:
        /**
         * Initialize Task
         *
         * This task message is sent from the container-local parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the
         * first task message that this role receives and it results in the role being initialized.
         */
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
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
       *
       * As a result, a Perform Mission Task message is sent to the node runtime, which will open a message listening port
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
       * Restart Container Task
       *
       * This message is sent from the parent NetworkOperationAgent.NetworkOperationRole instructing the container to restart following a given
       * delay.
       *
       * As a result, this JVM exits, and the wrapping bash script restarts it
       */
      case AHCSConstants.RESTART_CONTAINER_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleRestartContainerTask(receivedMessage);
        return;

      /**
       * Restart Container Request Info
       *
       * This request message is sent from the ContainerHeartbeatRole when communications are lost with the Network Operation agent.
       *
       * As a result, this JVM exits, and the wrapping bash script restarts it
       */
      case AHCSConstants.RESTART_CONTAINER_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleRestartContainerRequestInfo(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;

      // handle other operations ...
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
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.OPERATION_NOT_PERMITTED_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.RESTART_CONTAINER_REQUEST_INFO,
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
    assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";

  }

  /**
   * Restarts the container after the specified delay.
   *
   * @param receivedMessage the Restart Container Task message
   */
  private void handleRestartContainerTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert receivedMessage.get(AHCSConstants.RESTART_CONTAINER_TASK_DELAY) != null :
            "the " + AHCSConstants.RESTART_CONTAINER_TASK_DELAY + " parameter must be present ...\n" + receivedMessage;

    LOGGER.info("Cancelling scheduled tasks.");
    getNodeRuntime().getTimer().cancel();

    final long delay = (long) receivedMessage.get(AHCSConstants.RESTART_CONTAINER_TASK_DELAY);
    LOGGER.info("Restarting the application after a pause of " + (delay/1000) + " seconds.");
    assert delay > 0 : AHCSConstants.RESTART_CONTAINER_TASK_DELAY + " must be positive";

    try {
      Thread.sleep(delay);
    } catch (InterruptedException ex) {
      // ignore
    }
    getNodeRuntime().restartJVM();
  }

  /**
   * Restarts the container after shutting down the aicoind instance.
   *
   * @param receivedMessage the Restart Container Task message
   */
  private void handleRestartContainerRequestInfo(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    LOGGER.info("Cancelling scheduled tasks.");
    getNodeRuntime().getTimer().cancel();

    // send a Shutdown Aicoind Request Info message to AICOperation
    final Message shutdownAicoindRequestMessage = makeMessage(
            getContainerName()+ ".AICOperationAgent.AICOperationRole",
            "org.texai.skill.aicoin.AICOperation",
            AHCSConstants.SHUTDOWN_AICOIND_REQUEST_INFO); // operation
    sendMessage(
            receivedMessage,
            shutdownAicoindRequestMessage); // message


    final long delay = 10000L;
    LOGGER.info("Restarting the application after a pause of " + (delay/1000) + " seconds.");

    try {
      Thread.sleep(delay);
    } catch (InterruptedException ex) {
      // ignore
    }
    getNodeRuntime().restartJVM();
  }

}
