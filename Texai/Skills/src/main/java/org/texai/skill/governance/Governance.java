/*
 * Governance.java
 *
 * Created on May 5, 2010, 1:47:02 PM
 *
 * Description: Provides a governance skill that ensures a node's behavior compilies with policy.
 *
 * Copyright (C) May 5, 2010, Stephen L. Reed.
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

  /**
   * Constructs a new Governance instance.
   */
  public Governance() {
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
      sendMessage(operationNotPermittedMessage(message));
      return;
    }
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent ContainerGovernanceAgent.ContainerGovernanceRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent ContainerGovernanceAgent.ContainerGovernanceRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(State.READY);
        LOGGER.info("now ready");
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;
    }

    sendMessage(notUnderstoodMessage(message));
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
    };
  }
}
