/*
 * VNCOperation.java
 *
 * Created on Mar 14, 2012, 12:07:07 PM
 *
 * Description: Provides VNC (remote UI) operation behavior for a particular remote UI session.
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.vncrobot;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.ManagedSessionSkill;
import org.texai.ahcsSupport.Message;

/** Provides VNC (remote UI) operation behavior for a particular remote UI session.
 *
 * @author reed
 */
@NotThreadSafe
@ManagedSessionSkill
public class VNCOperation extends AbstractSkill {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(VNCOperation.class);

  /** Constructs a new VNCOperation instance. */
  public VNCOperation() {
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("receiveMessage: " + message);
    }
    final String operation = message.getOperation();
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        // initialize child roles
        propagateOperationToChildRoles(operation);
        setSkillState(State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        assert getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";
        // ready child roles
        propagateOperationToChildRoles(operation);
        setSkillState(State.READY);
        return true;
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
              AHCSConstants.AHCS_READY_TASK,
            };
  }
}
