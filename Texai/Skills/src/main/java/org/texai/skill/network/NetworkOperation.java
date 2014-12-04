package org.texai.skill.network;

import java.security.cert.X509Certificate;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration;

/**
 * Created on Aug 30, 2014, 11:30:19 PM.
 *
 * Description: Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
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
public final class NetworkOperation extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkOperation.class);

  /**
   * Constructs a new NetworkOperation instance.
   */
  public NetworkOperation() {
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
       * This task message is sent from the parent TopmostFriendshipAgent.TopmostFriendshipRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      /**
       * Join Network Task
       *
       * This task message is sent from the local parent TopmostFriendshipAgent.TopmostFriendshipRole.
       *
       * As the result, a Join Network Task message is sent to the child NetworkSingletonConfiguration agent/role,
       * which in turn sends a Join Network Task message to its child SingletonConfiguration agent/role. The
       * SingletonConfiguration agent/role requests the singleton agent dictionary, agent name  --> hosting container name,
       * from a seed peer.
       */
      case AHCSConstants.JOIN_NETWORK_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinNetwork();
        return true;

      /**
       * Delegate Configure Singleton Agent Hosts Task
       *
       * This task message is sent from the local parent TopmostFriendshipAgent.TopmostFriendshipRole.
       *
       * The task is delegated to the container operation agent/role which in turn delegates it to the singleton configuration agent/role.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       */
      case AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        configureSingletonAgentHostsTask(message);
        return true;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
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
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(message);
        return true;

      /**
       * Network Join Complete Info
       *
       * This information message is sent from a child ContainerOperaton agent/role that has completed joining the network.
       *
       * It results in a message synchronously propagated to all the roles in the sender's container that initializes their
       * state. Then, after all those roles are initialized, a Delegate Perform Mission Task message is propagated to child network
       * singleton agent / roles with the joined container's name as a parameter. A Perform Mission Task message is sent as a
       * response to the joined child ContainerOperaton agent/role. A Become Inactive Info message is sent to the joined child
       * TopmostFriendship agent/role, who propagates Become Inactive Task messages to agent / roles in the joined container
       * who have been superceded by network singletons.
       */
      case AHCSConstants.NETWORK_JOIN_COMPLETE_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleNetworkJoinCompleteInfo(message);
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
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
      AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.JOIN_NETWORK_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.NETWORK_JOIN_COMPLETE_INFO
    };
  }

  /**
   * Joins the network.
   *
   */
  private void joinNetwork() {
    //Preconditions
    assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "state must be isolated-from-network";

    LOGGER.info("joining the network");

    // send the join network task to the NetworkSingletonConfigurationAgent
    sendMessageViaSeparateThread(makeMessage(
            getRole().getChildQualifiedNameForAgent("NetworkSingletonConfigurationAgent"), // recipientQualifiedName
            NetworkSingletonConfiguration.class.getName(), // recipientService
            AHCSConstants.JOIN_NETWORK_TASK)); // operation
  }

  /**
   * Handles the Network Join Complete information message, which is sent from a ContainerOperation agent/role that has completed
   * joining the network. Each role in the joined container having a network singleton agent/role as a parent, now refers to the
   * corresponding network singleton agent / role and has exchanged X.509 certificates with it.
   *
   * @param message the received Network Join Complete Info message
   */
  private void handleNetworkJoinCompleteInfo(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    //TODO
    // synchronously send a Network Join Complete Task message to all child roles in the container, who each set their State to initialized and return immediately
    // asynchronously send a Perform Mission Task message to all child roles in the container, which have a separate thread that enables blocking if necessary
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");

    // send the perform mission task to the XAINetworkOperationAgent
    Message performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("XAINetworkOperationAgent"), // recipientQualifiedName,
            "org.texai.skill.aicoin.XAINetworkOperation", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    // send the perform mission task to the NetworkSingletonConfigurationAgent
    performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("NetworkSingletonConfigurationAgent"), // recipientQualifiedName,
            "org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);

    // send the perform mission task to the ContainerOperationAgent
    performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("ContainerOperationAgent"), // recipientQualifiedName,
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessageViaSeparateThread(performMissionMessage);
  }

  /**
   * Pass down the task to configure roles for singleton agent hosts.
   *
   * @param message the configure singleton agent hosts task message
   */
  private void configureSingletonAgentHostsTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("configuring the child roles with singleton agent hosts");
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // send message to container operations to configure parent roles throughout the container.
    final Message configureSingletonAgentHostsTask = makeMessage(
            getRole().getChildQualifiedNameForAgent("ContainerOperationAgent"), // recipientQualifiedName
            ContainerOperation.class.getName(), // recipientService
            AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK); // operation
    configureSingletonAgentHostsTask.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts);

    sendMessageViaSeparateThread(configureSingletonAgentHostsTask);
  }

  /** Handles the sender's request to join the network as child of this role..
   *
   * @param message the Join Network Singleton Agent Info message
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
