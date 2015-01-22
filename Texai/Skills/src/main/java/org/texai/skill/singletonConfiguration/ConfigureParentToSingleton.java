package org.texai.skill.singletonConfiguration;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.skill.domainEntity.SingletonAgentHosts;
import org.texai.skill.network.ContainerOperation;
import org.texai.util.StringUtils;

/**
 * ConfigureParentToSingleton.java
 *
 * Description: Configures the parent role to refer to a given network singleton role.
 *
 * Copyright (C) Nov 15, 2014, Stephen L. Reed, Texai.org.
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
   */
  @Override
  public void receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(Message.operationNotPermittedMessage(
              message, // receivedMessage
              this)); // skill
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent ContainerOperationAgent.ContainerOperationRole. It is expected to be the
       * first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Configure Singleton Agent Hosts Task
       *
       * This task message is sent from the container-local parent ContainerOperationAgent.ContainerOperationRole.
       *
       * Its parameter is the SingletonAgentHosts object, which contains the the singleton agent dictionary, agent name --> hosting
       * container name.
       *
       * The result is that any affected parent roles in the containing agent are revised to refer to the respective network singleton
       * agent/roles. Furthermore a Join Network Singleton Agent Info message is sent to the affected role's new parent, with the affected
       * role specified as the sender. As a parameter, the affected role's X509Certificate is included in the message to the new parent, who
       * needs it to verify the message.
       *
       */
      case AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState() + ", in " + getRole().getQualifiedName();
        configureSingletonAgentHosts(message);
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent ContainerOperationAgent.ContainerOperationRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

    }
    // otherwise, the message is not understood
    sendMessage(Message.notUnderstoodMessage(
            message, // receivedMessage
            this)); // skill
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
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.BECOME_READY_TASK,
      AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
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
            .append("configuring parent roles for ")
            .append(Node.extractContainerAgentName(getRole().getQualifiedName()));
    if (LOGGER.isDebugEnabled()) {
      stringBuilder.append(" ...");
    } else {
      LOGGER.info(stringBuilder.toString());
    }
    final SingletonAgentHosts singletonAgentHosts
            = (SingletonAgentHosts) message.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS); // parameterName
    assert singletonAgentHosts != null;

    // iterate over all roles of this agent
    getRole().getNode().getRoles().stream().sorted().forEach((Role role) -> {
      final String parentQualifiedName = role.getParentQualifiedName();
      if (StringUtils.isNonEmptyString(parentQualifiedName)) {
        if (singletonAgentHosts.isNetworkSingleton(parentQualifiedName)) {
          final String newParentQualifiedName = singletonAgentHosts.mapNetworkSingleton(parentQualifiedName);
          assert StringUtils.isNonEmptyString(newParentQualifiedName);
          assert !newParentQualifiedName.equals(parentQualifiedName);
          // a parent agent/role needs to be changed to the corresponding network singleton agent/role
          if (LOGGER.isDebugEnabled()) {
            stringBuilder
                    .append("\n  old parent role: ")
                    .append(parentQualifiedName)
                    .append("\n  new parent role: ")
                    .append(newParentQualifiedName);
            LOGGER.debug(stringBuilder.toString());
          }
          role.setParentQualifiedName(newParentQualifiedName);

          if (!getRole().isNetworkSingletonRole()) {
            // synchronously send an Add Unjoined Role Info message to the ContainerOperations agent/role
            final Message addUnjoinedRoleInfoMessage = makeMessage(
                    getRole().getParentQualifiedName(), // recipientQualifiedName
                    ContainerOperation.class.getName(), // recipientService
                    AHCSConstants.ADD_UNJOINED_ROLE_INFO); // operation
            addUnjoinedRoleInfoMessage.put(AHCSConstants.MSG_PARM_ROLE_QUALIFIED_NAME, role.getQualifiedName());
            sendMessage(addUnjoinedRoleInfoMessage);

            // send a join info message to the new parent agent/role
            final Message joinMessage = new Message(
                    role.getQualifiedName(), // senderQualifiedName
                    null, // senderService
                    newParentQualifiedName, // recipientQualifiedName
                    null, // recipientService
                    AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO); // operation
            assert role.getX509Certificate() != null;
            joinMessage.put(AHCSConstants.MSG_PARM_X509_CERTIFICATE, role.getX509Certificate());

            //TODO set and handle timeout
            role.sendMessageViaSeparateThread(joinMessage);
          }
        }
      } else {
        LOGGER.info("  no parent role for " + role);
      }
    });
  }

}
