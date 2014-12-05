/*
 * ContainerGovernance.java
 *
 * Created on Sep 18, 2014, 8:03:23 AM
 *
 * Description: Manages a container's agents to ensure friendly behavior.
 *
 * Copyright (C) 2014 Stephen L. Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.texai.skill.governance;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 * Manages a container's agents to ensure friendly behavior.
 *
 * @author reed
 */
@ThreadSafe
public final class ContainerGovernance extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerGovernance.class);

  /**
   * Constructs a new ContainerGovernance instance.
   */
  public ContainerGovernance() {
  }

  /** Gets the logger.
   *
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries
   * are single threaded with regard to the conversation.
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
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent TopLevelGovernanceAgent.TopLevelGovernanceRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent TopLevelGovernanceAgent.TopLevelGovernanceRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent TopLevelGovernanceAgent.TopLevelGovernanceRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
    }

    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single
   * threaded with regard to the conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations
    return notUnderstoodMessage(message);
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
    };
  }

}
