/*
 * Governance.java
 *
 * Created on May 5, 2010, 1:47:02 PM
 *
 * Description: Manages the container governorance agents to ensure friendly behavior.
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

import java.security.cert.X509Certificate;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.network.ContainerOperation;

/**
 * Manages the container governance agents to ensure friendly behavior.
 *
 * @author reed
 */
@ThreadSafe
public final class TopLevelGovernance extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TopLevelGovernance.class);

  /**
   * Constructs a new TopLevelGovernance instance.
   */
  public TopLevelGovernance() {
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
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

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
        assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(message.getOperation());
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(message);
        return true;

      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return true;

    }

    // otherwise not understood
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK
    };
  }

  /**
   * Performs the initialization operation.
   *
   * @param message the received initialization message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

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
   * Handles the sender's request to join the network as child of this role..
   *
   * @param message the Join Network Singleton Agent Info message
   */
  private void joinNetworkSingletonAgent(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("child role joining this network singleton " + message.getSenderQualifiedName());
    final String childQualifiedName = message.getSenderQualifiedName();
    final X509Certificate x509Certificate = (X509Certificate) message.get(AHCSConstants.MSG_PARM_X509_CERTIFICATE);
    assert x509Certificate != null;

    ((NodeRuntime) getNodeRuntime()).addX509Certificate(childQualifiedName, x509Certificate);

    // send a acknowledged_info message to the joined peer agent/role
    final Message acknowledgedInfoMessage = makeMessage(
            message.getSenderQualifiedName(), // recipientQualifiedName
            message.getSenderService(), // recipientService
            AHCSConstants.JOIN_ACKNOWLEDGED_TASK); // operation
    acknowledgedInfoMessage.put(
            AHCSConstants.MSG_PARM_X509_CERTIFICATE, // parameterName
            getRole().getX509Certificate()); // parameterValue
    sendMessage(acknowledgedInfoMessage);
  }

  /**
   * Receive the new parent role's acknowledgement of joining the network.
   *
   * @param message the received perform mission task message
   */
  private void joinAcknowledgedTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("join acknowledged from " + message.getSenderQualifiedName());
    final Message removeUnjoinedRoleInfoMessage = makeMessage(
            getContainerName() + ".ContainerOperationAgent.ContainerOperationRole", // recipientQualifiedName
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.REMOVE_UNJOINED_ROLE_INFO); // operation
    sendMessageViaSeparateThread(removeUnjoinedRoleInfoMessage);
  }
}
