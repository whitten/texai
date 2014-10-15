/*
 * LifeCycleManagement.java
 *
 * Created on May 5, 2010, 1:47:14 PM
 *
 * Description: Governs the logger role hierarchy over one or more JVMs.
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
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;

/** Governs the logger role hierarchy over one or more JVMs.
 *
 * @author reed
 */
@ThreadSafe
public class NetworkLogControl extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkLogControl.class);

  /** Constructs a new LifeCycleManagement instance. */
  public NetworkLogControl() {
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
    return new String[]{
              AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
              AHCSConstants.AHCS_INITIALIZE_TASK,
              AHCSConstants.AHCS_READY_TASK
            };
  }

  /** Performs the initialization operation. */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    // initialize child JVM logger management roles
    LOGGER.info("initializing");
    propagateOperationToChildRoles(ContainerLogControl.class.getName(), // service
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation
    setSkillState(State.INITIALIZED);
  }

  /** Performs the ready operation. */
  private void ready(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    // ready child JVM logger management roles
    propagateOperationToChildRoles(ContainerLogControl.class.getName(), // service
            AHCSConstants.AHCS_READY_TASK); // operation
    setSkillState(State.READY);
  }
}
