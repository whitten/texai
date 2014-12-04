/*
 * XAINetworkOperation.java
 *
 * Created on Sep 18, 2014, 8:10:02 AM
 *
 * Description: Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
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
package org.texai.skill.aicoin;

import java.security.cert.X509Certificate;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.network.ContainerOperation;

/**
 * Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * @author reed
 */
@ThreadSafe
public final class XAINetworkOperation extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAINetworkOperation.class);

  /**
   * Constructs a new XTCNetworkOperation instance.
   */
  public XAINetworkOperation() {
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
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first
       * task message that this role receives and it results in the role being initialized.
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
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole.
       *
       * It indicates that the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole.
       * It commands this network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      /**
       * Join Network Singleton Agent Info
       *
       * This task message is sent to this network singleton agent/role from a child role in another container.
       *
       * The sender is requesting to join the network as child of this role.
       *
       * The message parameter is the X.509 certificate belonging to the sender agent / role.
       *
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509
       * certificate as the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(message);
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
      // handle other operations ...
    }

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
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    LOGGER.info("performing the mission");

    // the operation agent launches the aicoin-qt instance in each container
    sendMessageViaSeparateThread(makeMessage(
            getRole().getFirstChildQualifiedNameForAgent("XAIOperationAgent"), // recipientQualifiedName
            "org.texai.skill.aicoin.XAIOperation", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK)); // operation

    // the mint agent commands its aicoin-qt instance to generate a block every 10 minutes
    sendMessageViaSeparateThread(makeMessage(
            getRole().getFirstChildQualifiedNameForAgent("XAIMintAgent"), // recipientQualifiedName
            "org.texai.skill.aicoin.XAIMint", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK)); // operation
  }

  /**
   * Handles a join request from child role in another container. It responds by sending
   * an acknowledgement message to the requesting child role.
   *
   * @param message the join network singleton agent info message
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
