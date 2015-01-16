/**
 * ContainerDeployment.java
 *
 * Description: Performs software deployment at the container level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerDeployment extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeployment.class);

  /**
   * Constructs a new SkillTemplate instance.
   */
  public ContainerDeployment() {
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   *
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      case AHCSConstants.DEPLOY_FILE_TASK:
        deployFile(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      // handle other operations ...
    }
    // otherwise, the message is not understood
    sendMessage(notUnderstoodMessage(message));
    return true;
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
  public Message converseMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations
    return (notUnderstoodMessage(message));
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
      AHCSConstants.DEPLOY_FILE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
    };
  }

  /**
   * Perform this role's mission.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

  }

  /**
   * Perform the specified file deployment task.
   *
   * @param message the received perform mission task message
   */
  private void deployFile(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("deploying files ...");
    final String command = (String) message.get(AHCSConstants.DEPLOY_FILE_TASK_COMMAND);
    LOGGER.info("  command: " + command);
    final String fileToDeployPath = (String) message.get(AHCSConstants.DEPLOY_FILE_TASK_PATH);
    LOGGER.info("  path:    " + fileToDeployPath);
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
