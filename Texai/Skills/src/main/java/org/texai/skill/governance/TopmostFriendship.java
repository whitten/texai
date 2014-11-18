/*
 * TopmostFriendship.java
 *
 * Created on Jun 23, 2010, 10:58:37 AM
 *
 * Description: Provides topmost perception and friendship behavior.
 *
 * Copyright (C) Jun 23, 2010, Stephen L. Reed.
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
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.network.NetworkOperation;

/**
 * Provides topmost perception and friendship behavior.
 *
 * @author reed
 */
@ThreadSafe
public final class TopmostFriendship extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TopmostFriendship.class);

  /**
   * Constructs a new TopmostFriendship instance.
   */
  public TopmostFriendship() {
  }

  /**
   * Receives and attempts to process the given message.
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
        initialization(message);
        return true;

      case AHCSConstants.SINGLETON_AGENT_HOSTS_INFO:
        singletonAgentHosts(message);
        return true;

      // handle other operations ...
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

    // handle operations ...
    // not understood
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
      AHCSConstants.SINGLETON_AGENT_HOSTS_INFO
    };
  }

  /**
   * Performs the initialization operation.
   *
   * @param message the received initialization task message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    // initialize the child roles
    LOGGER.info("initializing child roles");
    propagateOperationToChildRoles(message.getOperation());
    setSkillState(State.READY);

    //TODO wait for child roles to be initialized
    try {
      Thread.sleep(2000);
    } catch (InterruptedException ex) {
    }
    performMission();
  }

  /**
   * Receives a singleton agent hosts message, whose contents map certain local agent/role addresses to their network singleton
   * counterparts. The message is propagated to the child roles, who update their parent roles if the parent is a network singleton. This
   * action has the likely effect of shutting this node off from the network.
   *
   * @param message the received singletonAgentHosts info message
   */
  private void singletonAgentHosts(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "state must be ready";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // send message to network operations -- resend to container operations --> child Configure roles.
    final Message configureSingletonAgentHostsTask = makeMessage(
            getRole().getChildQualifiedNameForAgent("NetworkOperationAgent"), // recipientQualifiedName
            NetworkOperation.class.getName(), // recipientService
            AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);

    sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
  }

  /**
   * Become passive as the network singleton assumes responsibility for this role's child roles.
   */
  private void becomePassive() {
  }

  /**
   * Perform this role's mission, which is to provide topmost perception and friendship behavior.
   */
  private void performMission() {
    assert getSkillState().equals(State.READY) : "state must be ready";

    LOGGER.info("performing the mission");
    // send a performMission task message to the NetworkOperationAgent
    final Message performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("NetworkOperationAgent"), // recipientQualifiedName,
            NetworkOperation.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessage(performMissionMessage);

    //TODO same for the remaining child agent roles
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

}
