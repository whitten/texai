/*
 * VNCRobot.java
 *
 * Created on Aug 12, 2011, 10:08:36 PM
 *
 * Description: Provides lower level sensor and actuator functions for a VNC robot.
 *
 * Copyright (C) Aug 12, 2011, Stephen L. Reed, Texai.org.
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

/** Provides lower level sensor and actuator functions for a VNC robot.
 *
 * @author reed
 */
@NotThreadSafe
@ManagedSessionSkill
public class VNCRobot extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(VNCRobot.class);

  /** Constructs a new VNCRobot instance. */
  public VNCRobot() {
  }

    // get the screen from the VNC server

    // assume Ubuntu OS ...

    // detect the location of the mouse by moving it

    // detect the desktop task bar

    // detect the virtual screens

    // detect the applications running on the current screen

    // detect the open windows

    // detect the window decoration controls of the visible windows

    // ...

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
