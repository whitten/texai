package org.texai.ahcs.skill;

import java.security.cert.X509Certificate;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * AbstractNetworkSingletonSkill.java
 *
 * Description: An abstract skill that provides behavior for a network singleton role.
 *
 * Copyright (C) Dec 5, 2014, Stephen L. Reed, Texai.org.
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
public abstract class AbstractNetworkSingletonSkill extends AbstractSkill {

  /**
   * Creates a new instance of AbstractNetworkSingletonSkill.
   */
  public AbstractNetworkSingletonSkill() {
  }

  /**
   * Return whether this is a network singleton skill.
   *
   * @return whether this is a network singleton skill
   */
  @Override
  public boolean isNetworkSingletonSkill() {
    assert getRole().isNetworkSingletonRole();
    return true;
  }

  /**
   * Handles the sender's request to join the network as child of this role..
   *
   * @param message the Join Network Singleton Agent Info message
   */
  protected void joinNetworkSingletonAgent(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    getLogger().info("child role joining this network singleton " + message.getSenderQualifiedName());
    final String childQualifiedName = message.getSenderQualifiedName();
    final X509Certificate x509Certificate = (X509Certificate) message.get(AHCSConstants.MSG_PARM_X509_CERTIFICATE);
    assert x509Certificate != null;

    getRole().getChildQualifiedNames().add(childQualifiedName);
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
   * Handles the Delegate Become Ready Task message by synchronously relaying it to child roles.
   *
   * @param message the received Delegate Become Ready Task message
   */
  protected void handleDelegateBecomeReadyTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    final String containerName = (String) message.get(AHCSConstants.MSG_PARM_CONTAINER_NAME);
    assert StringUtils.isNonEmptyString(containerName);

    getLogger().info("handling Delegate Become Ready Task message for joined container " + containerName + " ...");

    if (getLogger().isDebugEnabled()) {
      getRole().getChildQualifiedNames().stream().forEach(childQualifiedName -> {
        getLogger().debug("  child role " + childQualifiedName);
      });
    }

    // ensure that no child network-singleton roles belong to the joining container
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && isNetworkSingletonRole) {
        throw new TexaiException("invalid network-singleton child role from joining container " + childQualifiedName);
      }
    });

    getRole().getChildQualifiedNames().stream().forEach(childQualifiedName -> {
      String operation = null;
      // coerce remote role names to local role names for the purpose of determining network singletons
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && !isNetworkSingletonRole) {
        operation = AHCSConstants.BECOME_READY_TASK;
        getLogger().info(" sending Become Ready Task to " + childQualifiedName);
      } else if (isNetworkSingletonRole) {
        operation = AHCSConstants.DELEGATE_BECOME_READY_TASK;
        getLogger().info(" sending Delegate Become Ready Task to " + childQualifiedName);
      }
      if (operation != null) {
        final Message outboundMessage = makeMessage(
                childQualifiedName, // recipientQualifiedName
                null, // recipientService
                operation);
        outboundMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, message.get(AHCSConstants.MSG_PARM_CONTAINER_NAME));
        sendMessage(outboundMessage);
      }
    });
  }

  /**
   * Handles the Delegate Perform Mission Task message by synchronously relaying it to child roles.
   *
   * @param message the received Delegate Become Ready Task message
   */
  protected void handleDelegatePerformMissionTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    final String containerName = (String) message.get(AHCSConstants.MSG_PARM_CONTAINER_NAME);
    assert StringUtils.isNonEmptyString(containerName);

    getLogger().info("handling Delegate Perform Mission Task message for joined container " + containerName + " ...");

    getRole().getChildQualifiedNames().stream().forEach(childQualifiedName -> {
      String operation = null;
      // coerce remote role names to local role names for the purpose of determining network singletons
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && !isNetworkSingletonRole) {
        operation = AHCSConstants.PERFORM_MISSION_TASK;
        getLogger().info(" sending Perform Mission Task to " + childQualifiedName);
      } else if (isNetworkSingletonRole) {
        operation = AHCSConstants.DELEGATE_PERFORM_MISSION_TASK;
        getLogger().info(" sending Delegate Perform Mission Task to " + childQualifiedName);
      }
      if (operation != null) {
        final Message outboundMessage = makeMessage(
                childQualifiedName, // recipientQualifiedName
                null, // recipientService
                operation);
        outboundMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, message.get(AHCSConstants.MSG_PARM_CONTAINER_NAME));
        sendMessage(outboundMessage);
      }
    });
  }

}
