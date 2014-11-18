package org.texai.skill.network;

import java.util.HashMap;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.support.NodeRuntimeSkill;

/**
 * Created on Sep 1, 2014, 1:48:49 PM.
 *
 * Description: Manages the network, the containers, and the coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) Sep 1, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 *
 * Copyright (C) 2014 Texai
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
@ThreadSafe
public final class ContainerOperation extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerOperation.class);

  /**
   * Constructs a new ContainerOperation instance.
   */
  public ContainerOperation() {
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
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      case AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        configureSingletonAgentHostsTask(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      // handle other operations ...
    }

    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

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
      AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK
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
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    final Message performMissionMessage = makeMessage(
            NodeRuntimeSkill.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK, // operation
            new HashMap<>()); // parameterDictionary
    sendMessageViaSeparateThread(performMissionMessage);
  }

  /**
   * Pass down the task to configure roles for singleton agent hosts.
   *
   * @param message the confiure singleton agent hosts task message
   */
  private void configureSingletonAgentHostsTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    getRole().getChildQualifiedNames().stream().sorted().forEach(childQualifiedName -> {
      final Message configureSingletonAgentHostsTask = makeMessage(
              childQualifiedName, // recipientQualifiedName
              null, // recipientService
              AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
      configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);
      sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
    });
    propagateOperationToChildRoles(AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK);
  }

}
