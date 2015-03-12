/*
 * NodeRuntimeSkill.java
 *
 * Created on Oct 19, 2014, 4:06:00 PM
 *
 * Description: Provides runtime support for nodes in a container.
 *
 * Copyright (C) 2014 Texai
 */
package org.texai.skill.support;

import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.NetworkUtils;

/**
 *
 * @author reed
 */
public class NodeRuntimeSkill extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeRuntimeSkill.class);
  // the node runtime
  private BasicNodeRuntime nodeRuntime;

  /**
   * Sets the role containing this skill.
   *
   * @param role the role containing this skill
   */
  @Override
  public void setRole(final Role role) {
    super.setRole(role);

    nodeRuntime = role.getNodeRuntime();
    assert nodeRuntime != null;
    nodeRuntime.setNodeRuntimeSkill(this);
  }

  /**
   * Constructs a new NodeRuntimeSkill instance.
   */
  public NodeRuntimeSkill() {
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
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    final String operation = receivedMessage.getOperation();
    if (!isOperationPermitted(receivedMessage)) {
      sendOperationNotPermittedInfoMessage(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the
       * first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton parent ContainerOperationAgent.ContainerOperationRole.
       *
       * It results in the skill listening for connections from peers.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";
        listenForConnections(receivedMessage);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;

    }
    sendDoNotUnderstandInfoMessage(receivedMessage);
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

    // handle operations
    return Message.notUnderstoodMessage(
            message, // receivedMessage
            this); // skill
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK
    };
  }

  /**
   * Start listening for incomming connections.
   *
   * @param message the received Listen For Connections task message
   */
  private void listenForConnections(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert nodeRuntime instanceof NodeRuntime;

    final int port = NetworkUtils.toNetworkPort(nodeRuntime.getNetworkName());
    LOGGER.info("listening for inbound connections on port " + port);
    ((NodeRuntime) nodeRuntime).listenForIncommingConnections(port);
  }
}
