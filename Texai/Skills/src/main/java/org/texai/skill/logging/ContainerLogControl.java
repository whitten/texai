/*
 * LifeCycleManagement.java
 *
 * Created on May 5, 2010, 1:47:14 PM
 *
 * Description: Governs the node logger role hierarchy within a particular JVM, and performs class level logging.
 *
 * Copyright (C) May 5, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.skill.logging;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
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

  // Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
  // with regard to the conversation.
  //
  // @param message the given message
  //
  // @return whether the message was successfully processed
  @Override
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.AHCS_INITIALIZE_TASK:
        initialization(message);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        ready(message);
        return true;

      case AHCSConstants.SET_LOGGING_LEVEL:
        setLoggingLevel((String) message.get(AHCSConstants.MSG_PARM_CLASS_NAME), // className
                (String) message.get(AHCSConstants.MSG_PARM_LOGGING_LEVEL)); // loggingLevel
        return true;

      case AHCSConstants.LOG_OPERATION_TASK:
        logOperation(message);
        return true;

      case AHCSConstants.UNLOG_OPERATION_TASK:
        unlogOperation(message);
        return true;

    }

    // otherwise not understood
    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  // Sets the logging level for the given class.
  //
  // @param className the class name
  // @param loggingLevel the logging level
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

  // Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single threaded with regard
  // to the conversation.
  //
  // @param message the given message
  //
  // @return the response message or null if not applicable
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations
    return notUnderstoodMessage(message);
  }

  // Returns the understood operations.
  //
  // @return the understood operations
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.AHCS_READY_TASK,
      AHCSConstants.LOG_OPERATION_TASK,
      AHCSConstants.UNLOG_OPERATION_TASK,};
  }

  // Performs the initialization operation.
  //
  // @param message the received initialization message
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    setSkillState(State.INITIALIZED);
  }

  // Performs the ready operation.
  // @param message the received ready message
  private void ready(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    setSkillState(State.READY);
  }

  // Records the operation for message logging.
  //
  // @param message the message containing the operation to be logged
  private void logOperation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    logOperation((String) message.get(AHCSConstants.LOG_OPERATION_TASK_LOGGED_OPERATION));
  }

  // Records the operation for message logging.
  //
  // @param loggedOperation the operation to be logged
  private void logOperation(final String loggedOperation) {
    //Preconditions
    assert loggedOperation != null : "loggedOperation must not be null";
    assert !loggedOperation.isEmpty() : "loggedOperation must not be empty";

    LOGGER.info("logging " + loggedOperation);
    assert loggedOperation != null;
    getNodeRuntime().addLoggedOperation(loggedOperation);
  }

  // Removes the operation for message logging.
  //
  // @param message the message containing the operation to be unlogged
  private void unlogOperation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    unlogOperation((String) message.get(AHCSConstants.UNLOG_OPERATION_TASK_UNLOGGED_OPERATION));
  }

  // Removes the operation for message logging.
  //
  // @param unloggedOperation the operation to be unlogged
  private void unlogOperation(final String unloggedOperation) {
    //Preconditions
    assert unloggedOperation != null : "unloggedOperation must not be null";
    assert !unloggedOperation.isEmpty() : "unloggedOperation must not be empty";

    LOGGER.info("unlogging " + unloggedOperation);
    assert unloggedOperation != null;
    getNodeRuntime().removeLoggedOperation(unloggedOperation);
  }

  /** Gets the logger.
   *
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

}
