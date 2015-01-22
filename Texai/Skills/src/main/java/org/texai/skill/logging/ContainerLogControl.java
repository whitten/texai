/*
 * LifeCycleManagement.java
 *
 * Created on May 5, 2010, 1:47:14 PM
 *
 * Description: Governs the node logger role hierarchy within a particular JVM, and performs class level logging.
 *
 * Copyright (C) May 5, 2010, Stephen L. Reed.
 */
package org.texai.skill.logging;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 * Governs the node logger role hierarchy within a particular JVM, and performs class level logging.
 *
 * @author reed
 */
@ThreadSafe
public class ContainerLogControl extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerLogControl.class);

  // Constructs a new LifeCycleManagement instance.
  public ContainerLogControl() {
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   */
  @Override
  public void receiveMessage(final Message message) {
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
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent NetworkLogControlAgent.NetworkLogControlRole. It is expected to be the
       * first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent NetworkLogControlAgent.NetworkLogControlRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(State.READY);
        LOGGER.info("now ready");
        return;

      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(message);
        return;

      case AHCSConstants.SET_LOGGING_LEVEL:
        setLoggingLevel((String) message.get(AHCSConstants.MSG_PARM_CLASS_NAME), // className
                (String) message.get(AHCSConstants.MSG_PARM_LOGGING_LEVEL)); // loggingLevel
        return;

      case AHCSConstants.LOG_OPERATION_TASK:
        logOperation(message);
        return;

      case AHCSConstants.UNLOG_OPERATION_TASK:
        unlogOperation(message);
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkLogControlAgent.NetworkLogControlRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return;

    }

    // otherwise not understood
    sendMessage(Message.notUnderstoodMessage(
            message, // receivedMessage
            this)); // skill
  }

  /**
   * Sets the logging level for the given class.
   *
   * @param className the class name
   * @param loggingLevel the logging level
   */
  private void setLoggingLevel(
          final String className,
          final String loggingLevel) {
    //Preconditions
    assert className != null : "className must not be null";
    assert !className.isEmpty() : "className must not be empty";
    assert loggingLevel != null : "loggingLevel must not be null";
    assert !loggingLevel.isEmpty() : "loggingLevel must not be empty";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    LOGGER.info("setting " + className + " log level to " + loggingLevel);
    final Level level;
    switch (loggingLevel) {
      case "all":
        level = Level.ALL;
        break;
      case "debug":
        level = Level.DEBUG;
        break;
      case "error":
        level = Level.ERROR;
        break;
      case "fatal":
        level = Level.FATAL;
        break;
      case "info":
        level = Level.INFO;
        break;
      case "trace":
        level = Level.TRACE;
        break;
      case "warn":
        level = Level.WARN;
        break;
      default:
        level = null;
        assert false : "invalid logLevel: " + loggingLevel;
    }
    Logger.getLogger(className).setLevel(level);
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
      AHCSConstants.BECOME_READY_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.LOG_OPERATION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.UNLOG_OPERATION_TASK
    };
  }

  /**
   * Performs the initialization operation.
   *
   * @param message the received initialization message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (getNodeRuntime().isFirstContainerInNetwork()) {
      setSkillState(AHCSConstants.State.READY);
    } else {
      setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
    }
  }

  /**
   * Perform this role's mission, which is to manage a container's agents to ensure friendly behavior.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

  }

  /**
   * Records the operation for message logging.
   *
   * @param message the message containing the operation to be logged
   */
  private void logOperation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    logOperation((String) message.get(AHCSConstants.LOG_OPERATION_TASK_LOGGED_OPERATION));
  }

  /**
   * Records the operation for message logging.
   *
   * @param loggedOperation the operation for message logging
   */
  private void logOperation(final String loggedOperation) {
    //Preconditions
    assert loggedOperation != null : "loggedOperation must not be null";
    assert !loggedOperation.isEmpty() : "loggedOperation must not be empty";

    LOGGER.info("logging " + loggedOperation);
    assert loggedOperation != null;
    getNodeRuntime().addLoggedOperation(loggedOperation);
  }

  /**
   * Removes the operation for message logging.
   *
   * @param message the message containing the operation to be unlogged
   */
  private void unlogOperation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    unlogOperation((String) message.get(AHCSConstants.UNLOG_OPERATION_TASK_UNLOGGED_OPERATION));
  }

  /**
   * Removes the operation for message logging.
   *
   * @param unloggedOperation the operation to be unlogged
   */
  private void unlogOperation(final String unloggedOperation) {
    //Preconditions
    assert unloggedOperation != null : "unloggedOperation must not be null";
    assert !unloggedOperation.isEmpty() : "unloggedOperation must not be empty";

    LOGGER.info("unlogging " + unloggedOperation);
    assert unloggedOperation != null;
    getNodeRuntime().removeLoggedOperation(unloggedOperation);
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
