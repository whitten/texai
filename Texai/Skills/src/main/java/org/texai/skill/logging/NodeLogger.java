/*
 * NodeLifeCycle.java
 *
 * Created on Oct 12, 2011, 4:10:37 PM
 *
 * Description: Provides node logging behavior.
 *
 * Copyright (C) Oct 12, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.logging;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;

/** Provides node logging behavior.
 *
 * @author reed
 */
@NotThreadSafe
public class NodeLogger extends AbstractSkill {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeLogger.class);

  /** Constructs a new NodeLifeCycle instance. */
  public NodeLogger() {
  }
  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
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

    }

    assert getSkillState().equals(State.READY) : "must be in the ready state";
    // other operations ...

    // not understood
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
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations

    return notUnderstoodMessage(message);
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[] {
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.AHCS_READY_TASK
    };
  }

  /** Performs the initialization operation. */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    setSkillState(State.INITIALIZED);
  }

  /** Performs the ready operation. */
  private void ready(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    setSkillState(State.READY);
  }
}
