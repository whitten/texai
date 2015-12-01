package org.texai.ahcs.skill;

import java.security.cert.X509Certificate;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.util.StringUtils;

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
   * @param receivedMessage the Join Network Singleton Agent Info message
   */
  @SuppressWarnings("unchecked")
  protected void joinNetworkSingletonAgent(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    getLogger().info("child role joining this network singleton " + receivedMessage.getSenderQualifiedName());
    final String childQualifiedName = receivedMessage.getSenderQualifiedName();
    final X509Certificate x509Certificate = (X509Certificate) receivedMessage.get(AHCSConstants.MSG_PARM_X509_CERTIFICATE);
    assert x509Certificate != null;

    getRole().getChildQualifiedNames().add(childQualifiedName);
    final BasicNodeRuntime nodeRuntime = getNodeRuntime();
    if (nodeRuntime instanceof NodeRuntime) {
      ((NodeRuntime) nodeRuntime).addX509Certificate(childQualifiedName, x509Certificate);
    } else {
      assert false;
    }

    // send a acknowledged_info message to the joined peer agent/role
    final Message acknowledgedInfoMessage = makeMessage(
            receivedMessage.getSenderQualifiedName(), // recipientQualifiedName
            receivedMessage.getSenderService(), // recipientService
            AHCSConstants.JOIN_ACKNOWLEDGED_TASK); // operation
    acknowledgedInfoMessage.put(
            AHCSConstants.MSG_PARM_X509_CERTIFICATE, // parameterName
            getRole().getX509Certificate()); // parameterValue
    sendMessage(receivedMessage, acknowledgedInfoMessage);
  }

  /**
   * Handles the Delegate Perform Mission Task message by synchronously relaying it to child roles.receivedMessage.
   *
   * @param receivedMessage the received Delegate Become Ready Task message
   */
  protected void handleDelegatePerformMissionTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    final String containerName = (String) receivedMessage.get(AHCSConstants.MSG_PARM_CONTAINER_NAME);
    assert StringUtils.isNonEmptyString(containerName);

    getLogger().info("handling Delegate Perform Mission Task message for joined container " + containerName + " ...");

    getRole().getChildQualifiedNames().stream().forEach(childQualifiedName -> {
      String operation = null;
      // coerce remote role names to local role names for the purpose of determining network singletons
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && !isNetworkSingletonRole) {
        operation = AHCSConstants.PERFORM_MISSION_TASK;
        getLogger().info(" sending Delegate Perform Mission Task to " + childQualifiedName);
      } else if (isNetworkSingletonRole) {
        operation = AHCSConstants.DELEGATE_PERFORM_MISSION_TASK;
        getLogger().info(" sending Delegate Perform Mission Task to " + childQualifiedName);
      }
      if (operation != null) {
        final Message outboundMessage = makeMessage(
                childQualifiedName, // recipientQualifiedName
                null, // recipientService
                operation);
        outboundMessage.put(AHCSConstants.MSG_PARM_CONTAINER_NAME, receivedMessage.get(AHCSConstants.MSG_PARM_CONTAINER_NAME));
        sendMessage(receivedMessage, outboundMessage);
      }
    });
  }

}
