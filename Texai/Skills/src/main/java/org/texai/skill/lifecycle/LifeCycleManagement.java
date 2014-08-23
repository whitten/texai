/*
 * LifeCycleManagement.java
 *
 * Created on May 5, 2010, 1:47:14 PM
 *
 * Description: Governs the node life cycle role hierarchy.
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
package org.texai.skill.lifecycle;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeAccess;

/**
 * Governs the node life cycle role hierarchy.
 *
 * @author reed
 */
@ThreadSafe
public class LifeCycleManagement extends AbstractSkill {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(LifeCycleManagement.class);
  /**
   * the shared subskill that edits role types and roles
   */
  /**
   * the node access object
   */
  private NodeAccess nodeAccess;

  /**
   * Constructs a new LifeCycleManagement instance.
   */
  public LifeCycleManagement() {
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

      case AHCSConstants.CREATE_NODE_TASK:
        assert getSkillState().equals(State.READY) : "must be in the ready state";
        NodeAccess.createNode(
                (String) message.get(AHCSConstants.MSG_PARM_NODE_TYPE_NAME), // nodeTypeName
                (String) message.get(AHCSConstants.MSG_PARM_NODE_NICKNAME), // nodeNickname
                getRole().getNodeRuntime());
        return true;

      case AHCSConstants.CONNECT_CHILD_ROLE_TO_PARENT_TASK:
        assert getSkillState().equals(State.READY) : "must be in the ready state";
        NodeAccess.connectChildRoleToParent(
                (URI) message.get(AHCSConstants.MSG_PARM_CHILD_ROLE_ID), // childRoleId
                (URI) message.get(AHCSConstants.MSG_PARM_PARENT_ROLE_ID), // parentRoleId
                getRole().getNodeRuntime());
        return true;

      case AHCSConstants.AHCS_RESTART_TASK:
        assert getSkillState().equals(State.READY) : "must be in the ready state";
        restart(message);
        return true;

      case AHCSConstants.SHUTDOWN_NODE_RUNTIME:
        assert getSkillState().equals(State.READY) : "must be in the ready state";
        getNodeRuntime().shutdown();
        return true;

    }
    // not understood
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
      AHCSConstants.AHCS_READY_TASK,
      AHCSConstants.CREATE_NODE_TASK,
      AHCSConstants.CONNECT_CHILD_ROLE_TO_PARENT_TASK,
      AHCSConstants.SHUTDOWN_NODE_RUNTIME,
      AHCSConstants.UTTERANCE_MEANING_SENSATION
    };
  }

  /**
   * Performs the initialization operation.
   *
   * @param message the recieved initialization message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    // register the life cycle management role for remote communications with the launcher
    getRole().enableRemoteComunications();

    nodeAccess = new NodeAccess(getRDFEntityManager());

    // initialize child node life cycle roles
    propagateOperationToChildRoles(
            NodeLifeCycle.class.getName(), // service
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation
    setSkillState(State.INITIALIZED);
  }

  /**
   * Performs the ready operation.
   *
   * @param message the received ready message
   */
  private void ready(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    // ready child node life cycle roles
    propagateOperationToChildRoles(
            NodeLifeCycle.class.getName(), // service
            AHCSConstants.AHCS_READY_TASK); // operation
    setSkillState(State.READY);
  }

  /**
   * Sends a restart message to the launcher and restarts the node runtime.
   *
   * @param message the received restart message
   */
  private void restart(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final boolean areRepositoriesBackedUp = (boolean) message.get(AHCSConstants.AHCS_RESTART_TASK_ARE_REPOSITORIES_BACKED_UP);
    final boolean isRemoteDebuggingEnabled = (boolean) message.get(AHCSConstants.AHCS_RESTART_TASK_IS_REMOTE_DEBUGGING_ENABLED);
    if (areRepositoriesBackedUp) {
      LOGGER.info("backing up the repositories and restarting Texai");
    } else if (isRemoteDebuggingEnabled) {
      LOGGER.info("restarting Texai with remote debugging");
    } else {
      LOGGER.info("restarting Texai");
    }
    final Message message1 = new Message(
            getRoleId(), // senderRoleId
            getClass().getName(), // senderService
            getNodeRuntime().getLauncherRoleId(), // recipientRoleId
            null, // service
            AHCSConstants.AHCS_RESTART_TASK); // operation
    message1.put(
            AHCSConstants.AHCS_RESTART_TASK_NODE_RUNTIME_ID, // parameterName
            getNodeRuntime().getNodeRuntimeId()); // parameterValue
    message1.put(AHCSConstants.AHCS_RESTART_TASK_ARE_REPOSITORIES_BACKED_UP, areRepositoriesBackedUp);
    message1.put(AHCSConstants.AHCS_RESTART_TASK_IS_REMOTE_DEBUGGING_ENABLED, isRemoteDebuggingEnabled);
    //Logger.getLogger("org.texai.ahcs.router.MessageRouter").setLevel(Level.DEBUG);
    sendMessage(message1);
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  protected Logger getLogger() {
    return LOGGER;
  }
}
