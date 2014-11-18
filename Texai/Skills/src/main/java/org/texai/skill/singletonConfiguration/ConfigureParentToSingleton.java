package org.texai.skill.singletonConfiguration;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.util.StringUtils;

/**
 * ConfigureParentToSingleton.java
 *
 * Description: Configures the parent role to refer to a given network singleton role.
 *
 * Copyright (C) Nov 15, 2014, Stephen L. Reed, Texai.org.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
@ThreadSafe
public class ConfigureParentToSingleton extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(ConfigureParentToSingleton.class);

  /**
   * Creates a new instance of ChangeParentToSingleton.
   */
  public ConfigureParentToSingleton() {
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
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        // initialize child roles
        propagateOperationToChildRoles(operation);
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        configureSingletonAgentHosts(message);
        return true;

    }
    // otherwise, the message is not understood
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
  public Message converseMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations
    return (notUnderstoodMessage(message));
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
      AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK
    };
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
   * Configures the parent roles of this agent with regard to network singleton agent/roles.
   *
   * @param message the configure singleton agent hosts task message
   */
  private void configureSingletonAgentHosts(final Message message) {

    //Preconditions
    assert message != null : "message must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("\nconfiguring parent roles for ")
            .append(Node.extractContainerAgentName(getRole().getQualifiedName()))
            .append(" ...");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // iterate over all roles of this agent
    getRole().getNode().getRoles().stream().sorted().forEach((Role role) -> {
      final String parentQualifiedName = role.getParentQualifiedName();
      assert StringUtils.isNonEmptyString(parentQualifiedName);
      if (singletonAgentHosts.isNetworkSingleton(parentQualifiedName)) {
        // a parent agent/role needs to be changed to the corresponding network singleton agent/role
        final String newParentQualifiedName = singletonAgentHosts.mapNetworkSingleton(parentQualifiedName);
        assert StringUtils.isNonEmptyString(newParentQualifiedName);
        stringBuilder
                .append("\n  old parent role: ")
                .append(parentQualifiedName)
                .append("\n  new parent role: ")
                .append(newParentQualifiedName);
        role.setParentQualifiedName(newParentQualifiedName);
        LOGGER.info(stringBuilder.toString());

        // send a join info message to the new parent agent/role
        final Message joinMessage = new Message(
                role.getQualifiedName(), // senderQualifiedName
                null, // senderService
                newParentQualifiedName, // recipientQualifiedName
                null, // recipientService
                AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO); // operation
        assert role.getX509Certificate() != null;
        joinMessage.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE, role.getX509Certificate());

        role.sendMessageViaSeparateThread(joinMessage);
      }
    });

    LOGGER.info(stringBuilder.toString());
  }
}
