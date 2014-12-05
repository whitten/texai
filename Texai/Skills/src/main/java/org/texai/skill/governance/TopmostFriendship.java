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

import java.util.Iterator;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.network.NetworkOperation;
import org.texai.util.TexaiException;

/**
 * Provides topmost perception and friendship behavior.
 *
 * @author reed
 */
@ThreadSafe
public final class TopmostFriendship extends AbstractNetworkSingletonSkill {

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
      /**
       * Initialize Task
       *
       * This task message is sent from AIMain via the ContainerOperationAgent.ContainerOperationRole. It is expected to be the first task
       * message that this topmost role receives and it results in the role being initialized and propagation of that message to all agent /
       * roles.
       *
       * If this is the first container in the network, then a Perform Mission Task message is sent to the child NetworkOperation
       * agent/role. Otherwise a Join Network Task message is sent.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        return true;

      /**
       * Singleton Agent Hosts Info
       *
       * This task message is sent from the SingletonConfigurationAgent.SingletonConfigurationRole.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       *
       * A a result, a Delegate Configure Singleton Agent Hosts Task message is sent to network operations, which resends it to container
       * operations, which in turn sends a Configure Singleton Agent Hosts Task to all child ConfigureParentToSingletonRoles, which includes
       * every agent in the container.
       *
       * Each outbound message contains the singleton agent dictionary.
       *
       */
      case AHCSConstants.SINGLETON_AGENT_HOSTS_INFO:
        assert getSkillState().equals(State.ISOLATED_FROM_NETWORK) : "state must be ready";
        singletonAgentHosts(message);
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
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(message);
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
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleNetworkJoinCompleteSensation(message);
        return true;

      // handle other operations ...
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
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
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION,
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

    // initialize the child roles
    LOGGER.info("initializing child roles");
    propagateOperationToChildRoles(message.getOperation());

    if (getNodeRuntime().isFirstContainerInNetwork()) {
      setSkillState(State.READY);

      //TODO wait for child roles to be initialized
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ex) {
      }
      performMission();
    } else {
      setSkillState(State.ISOLATED_FROM_NETWORK);
      joinNetwork();
    }
  }

  /**
   * Receives a singleton agent hosts message from the SingletonConfiguration agent / role, whose contents map certain local agent/role
   * addresses to their network singleton counterparts. The message is propagated to the roles, who update their parent roles if the parent
   * is a network singleton. This action has the likely indirect effect of shutting this node off from the network.
   *
   * @param message the received singletonAgentHosts info message
   */
  private void singletonAgentHosts(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // send message to network operations --> resend to container operations --> child Configure roles.
    final Message configureSingletonAgentHostsTask = makeMessage(
            getRole().getChildQualifiedNameForAgent("NetworkOperationAgent"), // recipientQualifiedName
            NetworkOperation.class.getName(), // recipientService
            AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);

    sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
  }

  /**
   * Joins the network by sending a Join Network Task message to the NetworkOperation agent / role.
   */
  private void joinNetwork() {
    assert getSkillState().equals(State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";

    LOGGER.info("joining the network");
    // send a performMission task message to the NetworkOperationAgent
    final Message performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("NetworkOperationAgent"), // recipientQualifiedName,
            NetworkOperation.class.getName(), // recipientService
            AHCSConstants.JOIN_NETWORK_TASK); // operation
    sendMessage(performMissionMessage);
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
   * Handles the sensation that a new container has completed joining the network.
   *
   * @param message the Network Join Complete Sensation message
   */
  private void handleNetworkJoinCompleteSensation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String containerName = (String) message.get(AHCSConstants.MSG_PARM_CONTAINER_NAME);
    LOGGER.info("Sensed that container " + containerName + " has completely joined the network");

    // ensure that no child network-singleton roles belong to the joining container
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      final boolean isNetworkSingletonRole
              = getRole().isNetworkSingletonRole(getContainerName() + '.' + Node.extractAgentRoleName(childQualifiedName));
      if (containerName.equals(Node.extractContainerName(childQualifiedName)) && isNetworkSingletonRole) {
        throw new TexaiException("invalid network-singleton child role from joining container " + childQualifiedName);
      }
    });

    // synchronously propagate the Become Ready Task to the joined container's child roles
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      String operation = null;
      if (containerName.equals(Node.extractContainerName(childQualifiedName))) {
        operation = AHCSConstants.BECOME_READY_TASK;
      } else if (getRole().isNetworkSingletonRole(childQualifiedName)) {
        operation = AHCSConstants.DELEGATE_BECOME_READY_TASK;
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

    // synchronously propagate the Become Ready Task to the joined container's child roles
    getRole().getChildQualifiedNames().stream().forEach((String childQualifiedName) -> {
      String operation = null;
      if (containerName.equals(Node.extractContainerName(childQualifiedName))) {
        operation = AHCSConstants.PERFORM_MISSION_TASK;
      } else if (getRole().isNetworkSingletonRole(childQualifiedName)) {
        operation = AHCSConstants.DELEGATE_PERFORM_MISSION_TASK;
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
   * Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

}
