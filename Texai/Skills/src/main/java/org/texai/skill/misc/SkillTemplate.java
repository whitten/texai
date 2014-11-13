/*
 * SkillTemplate.java
 *
 * Created on Mar 14, 2012, 10:49:55 PM
 *
 * Description: .
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.misc;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class SkillTemplate extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(SkillTemplate.class);

  /** Constructs a new SkillTemplate instance. */
  public SkillTemplate() {
  }

  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

        // OPTIONAL
        // initialize child roles
        propagateOperationToChildRoles(operation);

        setSkillState(State.READY);
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
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

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations

    return (notUnderstoodMessage(message));
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
              AHCSConstants.AHCS_INITIALIZE_TASK,
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

  /** Gets the logger.
   *
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

}
