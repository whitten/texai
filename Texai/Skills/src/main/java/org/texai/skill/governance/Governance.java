/*
 * Governance.java
 *
 * Created on May 5, 2010, 1:47:02 PM
 *
 * Description: Provides a governance skill that ensures a node's behavior compilies with policy.
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
package org.texai.skill.governance;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 *
 * @author reed
 */
@ThreadSafe
public final class Governance extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(Governance.class);

  /** Constructs a new Governance instance. */
  public Governance() {
  }

  /** Gets the logger.
   * 
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
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
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        setSkillState(State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";
        setSkillState(State.READY);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
    }

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
}
