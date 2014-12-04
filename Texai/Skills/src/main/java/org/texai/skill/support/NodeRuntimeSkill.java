/*
 * NodeRuntimeSkill.java
 *
 * Created on Oct 19, 2014, 4:06:00 PM
 *
 * Description: Provides runtime support for nodes in a container.
 *
 * Copyright (C) 2014 Texai
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
package org.texai.skill.support;

import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

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
   * Receives and attempts to process the given message. The skill is thread
   * safe, given that any contained libraries are single threaded with regard to
   * the conversation.
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
    switch (operation) {
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

    }
    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given
   * that any contained libraries are single threaded with regard to the
   * conversation.
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

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,};
  }

  /**
   * Perform this role's mission.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");
    final String portString = System.getenv("LISTENING_PORT");
    if (!StringUtils.isNonEmptyString(portString)) {
      throw new TexaiException("missing the environment variable LISTENING_PORT");
    }
    final int port;
    try {
    port = Integer.parseInt(portString);
    } catch (NumberFormatException ex) {
      throw new TexaiException("LISTENING_PORT not a valid number " + portString);
    }
    if (port < 1025) {
      throw new TexaiException("LISTENING_PORT must have a value higher than 1024, was " + port);
    }
    LOGGER.info("listening for inbound connections on port " + port);
    ((NodeRuntime) nodeRuntime).listenForIncommingConnections(port);
  }
}
