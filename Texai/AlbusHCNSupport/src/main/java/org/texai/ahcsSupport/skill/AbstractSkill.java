/*
 * AbstractSkill.java
 *
 * Created on Jan 16, 2008, 10:57:22 AM
 *
 * Description: An abstract skill that provides behavior for a role.
 *
 * Copyright (C) Jan 16, 2008 Stephen L. Reed.
 */
package org.texai.ahcsSupport.skill;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.Message;
import static org.texai.ahcsSupport.Message.DEFAULT_VERSION;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.StringUtils;

/**
 * An abstract skill that provides behavior for a role.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public abstract class AbstractSkill {

  // the role containing this skill
  private Role role;
  // the skill state
  private State skillState = State.UNINITIALIZED;
  // the session manager skill, populated only for managed session skills so that they can notify the manager of disconnected sessions
  private SessionManagerSkill sessionManagerSkill;
  // the message timeout information dictionary, message reply-with --> message timeout info
  private final Map<UUID, MessageTimeOutInfo> messageTimeOutInfoDictionary = new HashMap<>();
  // the one-time operations - to receive one of these a second time is an error
  private static final Set<String> oneTimeOperations = new HashSet<>();

  static {
    oneTimeOperations.add(AHCSConstants.INITIALIZE_INFO);
    oneTimeOperations.add(AHCSConstants.INITIALIZE_TASK);
    oneTimeOperations.add(AHCSConstants.PERFORM_MISSION_TASK);
    oneTimeOperations.add(AHCSConstants.SINGLETON_AGENT_HOSTS_INFO);
    oneTimeOperations.add(AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK);
  }
  // the one-time operations - to receive one of these a second time is not an error, it is silently dropped
  private static final Set<String> oneTimeOperationsIgnoringRepeats = new HashSet<>();

  static {
    oneTimeOperationsIgnoringRepeats.add(AHCSConstants.SINGLETON_AGENT_HOSTS_INFO);
  }
  // the used one-time operations
  private final Set<String> usedOneTimeOperations = new HashSet<>();
  // the indicator whether this skill is undergoing unit test, in which case single threading is preferred
  private final AtomicBoolean isUnitTest = new AtomicBoolean(false);

  /**
   * Constructs a new Skill instance.
   */
  public AbstractSkill() {
  }

  /**
   * Verifies whether this skill permits the execution of the given operation.
   *
   * @param message the given message specifying the operation
   *
   * @return whether this skill permits the execution of the given operation
   */
  protected boolean isOperationPermitted(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (oneTimeOperations.contains(message.getOperation())) {
      synchronized (usedOneTimeOperations) {
        if (usedOneTimeOperations.contains(message.getOperation())) {
          // one-time operations are not allowed to be executed again
          return false;
        } else {
          usedOneTimeOperations.add(message.getOperation());
        }
      }
    }
    // verify consistency with the operations that this skill understands
    for (final String understoodOperation : getUnderstoodOperations()) {
      if (message.getOperation().equals(understoodOperation)) {
        return true;
      }
    }
    return true;
  }

  /** Returns whether the message should be silently dropped in the case that it is a redundant one-time operation.
   *
   * @param message the given message specifying the operation
   * @return whether the message should be silently dropped in the case that it is a redundant one-time operation
   */
  protected boolean isRedundantOneTimeOperationSilentlyDropped(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    return oneTimeOperationsIgnoringRepeats.contains(message.getOperation());
  }

  /**
   * Return whether this is a network singleton skill.
   *
   * @return whether this is a network singleton skill
   */
  public boolean isNetworkSingletonSkill() {
    return false;
  }

  /**
   * Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  protected RDFEntityManager getRDFEntityManager() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNodeRuntime().getRDFEntityManager();
  }

  /**
   * Gets the node runtime.
   *
   * @return the role containing this skill
   */
  public BasicNodeRuntime getNodeRuntime() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNodeRuntime();
  }

  /**
   * Gets the role containing this skill.
   *
   * @return the role containing this skill
   */
  public Role getRole() {
    return role;
  }

  /**
   * Sets the role containing this skill.
   *
   * @param role the role containing this skill
   */
  public void setRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    this.role = role;
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   */
  public abstract void receiveMessage(final Message message);

  /**
   * Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single threaded with regard
   * to the conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  public abstract Message converseMessage(final Message message);

  /**
   * Sends the given message via the node runtime.
   *
   * @param receivedMessage the received message which invoked the skill, which may be null
   * @param message the given message
   */
  protected void sendMessage(
          final Message receivedMessage,
          final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert role != null : "role must not be null for " + this;
    assert message.getSenderQualifiedName().equals(getQualifiedName()) : "message sender must match this role " + message + "\nrole: " + getRole();

    role.sendMessage(receivedMessage, message);
  }

  /**
   * Sends the given message via the node runtime, via a separate thread.
   *
   * @param receivedMessage the received message which invoked the skill, which may be null
   * @param message the given message
   */
  protected void sendMessageViaSeparateThread(
          final Message receivedMessage,
          final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert role != null : "role must not be null for " + this;
    assert message.getSenderQualifiedName().equals(getQualifiedName()) : "message sender must match this role " + message + "\nrole: " + getRole();

    role.sendMessageViaSeparateThread(receivedMessage, message);
  }

  /**
   * Makes a message given the recipient service within this role and the operation.
   *
   * @param recipientService the recipient service within this role
   * @param operation the operation
   * @param parameterDictionary the operation parameter dictionary, name --> value
   *
   * @return
   */
  protected Message makeMessage(
          final String recipientService,
          final String operation,
          final Map<String, Object> parameterDictionary) {
    //Preconditions
    assert recipientService == null || Message.isValidService(recipientService) : "the recipient service is not a found Java class " + recipientService;
    assert operation != null : "operation must not be null";
    assert parameterDictionary != null : "parameterDictionary must not be null";

    return new Message(
            role.getQualifiedName(), // senderQualifiedName,
            getClassName(), // senderService,
            role.getQualifiedName(), // recipientQualifiedName,
            recipientService,
            operation,
            parameterDictionary,
            DEFAULT_VERSION);
  }

  /**
   * Makes a message given the recipient and operation.
   *
   * @param recipientQualifiedName the recipient role's qualified name, container.nodename.rolename
   * @param recipientService the recipient service
   * @param operation the operation
   *
   * @return
   */
  protected Message makeMessage(
          final String recipientQualifiedName,
          final String recipientService,
          final String operation) {
    //Preconditions
    assert recipientQualifiedName != null : "recipientQualifiedName must not be null";
    assert recipientService == null || Message.isValidService(recipientService) : "the recipient service is not a found Java class " + recipientService;
    assert operation != null : "operation must not be null";

    return new Message(
            role.getQualifiedName(), // senderQualifiedName,
            getClassName(), // senderService,
            recipientQualifiedName,
            recipientService,
            operation,
            new HashMap<>(),
            DEFAULT_VERSION);
  }

  /**
   * Makes a message given the recipient and operation.
   *
   * @param recipientQualifiedName the recipient role's qualified name, container.nodename.rolename
   * @param conversationId the conversation id
   * @param recipientService the recipient service
   * @param operation the operation
   *
   * @return
   */
  protected Message makeMessage(
          final String recipientQualifiedName,
          final UUID conversationId,
          final String recipientService,
          final String operation) {
    //Preconditions
    assert recipientQualifiedName != null : "recipientQualifiedName must not be null";
    assert conversationId != null : "conversationId must not be null";
    assert recipientService == null || Message.isValidService(recipientService) : "the recipient service is not a found Java class " + recipientService;
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";

    return new Message(
            role.getQualifiedName(), // senderQualifiedName,
            getClassName(), // senderService,
            recipientQualifiedName,
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            recipientService,
            operation,
            new HashMap<>(), // parameterDictionary
            DEFAULT_VERSION); // version
  }

  /**
   * Makes a message given the recipient and operation.
   *
   * @param recipientQualifiedName the recipient role's qualified name, container.nodename.rolename
   * @param recipientService the recipient service
   * @param operation the operation
   * @param parameterDictionary the operation parameter dictionary, name --> value
   *
   * @return
   */
  protected Message makeMessage(
          final String recipientQualifiedName,
          final String recipientService,
          final String operation,
          final Map<String, Object> parameterDictionary) {
    //Preconditions
    assert recipientQualifiedName != null : "recipientQualifiedName must not be null";
    assert recipientService == null || Message.isValidService(recipientService) : "the recipient service is not a found Java class " + recipientService;
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";

    return new Message(
            role.getQualifiedName(), // senderQualifiedName,
            getClassName(), // senderService,
            recipientQualifiedName,
            recipientService,
            operation,
            parameterDictionary,
            DEFAULT_VERSION); // version
  }

  /**
   * Makes a reply message given the received message and operation
   *
   * @param receivedMessage the received message
   * @param operation the operation
   *
   * @return a reply message
   */
  protected Message makeReplyMessage(
          final Message receivedMessage,
          final String operation) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";

    return new Message(
            receivedMessage.getRecipientQualifiedName(), // senderQualifiedName
            receivedMessage.getRecipientService(), // senderService
            receivedMessage.getSenderQualifiedName(), // recipientQualifiedName
            receivedMessage.getConversationId(),
            null, // replyWith
            receivedMessage.getReplyWith(), // inReplyTo
            null, // replyByDateTime,
            receivedMessage.getSenderService(), // recipientService
            operation,
            new HashMap<>(), // parameterDictionary
            DEFAULT_VERSION); // version
  }

  /**
   * Makes an exception message given the received message and exception reason.
   *
   * @param receivedMessage the received message
   * @param reason the exception reason
   *
   * @return a reply message
   */
  protected Message makeExceptionMessage(
          final Message receivedMessage,
          final String reason) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert StringUtils.isNonEmptyString(reason) : "reason must be a non-empty string";

    final Map<String, Object> parameterDictionary = new HashMap<>();
    parameterDictionary.put(AHCSConstants.MSG_PARM_ORIGINAL_MESSAGE, receivedMessage);
    parameterDictionary.put(AHCSConstants.MSG_PARM_REASON, reason);
    return new Message(
            receivedMessage.getRecipientQualifiedName(), // senderQualifiedName
            receivedMessage.getRecipientService(), // senderService
            receivedMessage.getSenderQualifiedName(), // recipientQualifiedName
            receivedMessage.getConversationId(),
            null, // replyWith
            receivedMessage.getReplyWith(), // inReplyTo
            null, // replyByDateTime,
            receivedMessage.getSenderService(), // recipientService
            AHCSConstants.EXCEPTION_INFO, // operation
            parameterDictionary,
            DEFAULT_VERSION); // version
  }

  /**
   * Returns the class name of this skill.
   *
   * @return the class name of this skill
   */
  public String getClassName() {
    return getClass().getName();
  }

  /**
   * Returns the qualified agent.role name for the role that contains this skill.
   *
   * @return the qualified agent.role name
   */
  public String getQualifiedName() {
    return getRole().getQualifiedName();
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  public abstract String[] getUnderstoodOperations();

  /**
   * Returns whether the given operation is understood.
   *
   * @param operation the given operation
   *
   * @return whether the given operation is understood
   */
  public boolean isOperationUnderstood(final String operation) {
    //Preconditions
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";

    for (final String operation1 : getUnderstoodOperations()) {
      if (operation.equals(operation1)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('[');
    if (role != null) {
      stringBuilder.append(role.getQualifiedName()).append(':');
    }
    stringBuilder.append(getClass().getSimpleName()).append(']');
    return stringBuilder.toString();
  }

  /**
   * Propagates the given operation to the child roles, and to any service that understands the operation.
   *
   * @param receivedMessage the given received message
   */
  protected void propagateOperationToChildRoles(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert role != null : "role must not be null for " + this;

    role.propagateOperationToChildRoles(
            receivedMessage,
            this.getClassName()); // senderService
  }

  /**
   * Propagates the given operation to the child roles, and to any service that understands the operation, using separate threads.
   *
   * @param receivedMessage the given received message
   */
  protected void propagateOperationToChildRolesSeparateThreads(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert role != null : "role must not be null for " + this;

    if (isUnitTest.get()) {
      role.propagateOperationToChildRoles(
              receivedMessage,
            this.getClassName()); // senderService
    } else {
      role.propagateOperationToChildRolesSeparateThreads(
              receivedMessage,
            this.getClassName()); // senderService
    }
  }

  /**
   * Sends a not-understood message in response to the received message.
   *
   * @param receivedMessage the received message
   */
  protected void sendDoNotUnderstandInfoMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    final Message notUnderstoodInfoMessage = Message.notUnderstoodMessage(
            receivedMessage,
            this); // skill
    sendMessage(receivedMessage, notUnderstoodInfoMessage);
  }

  /**
   * Sends a not-understood message in response to the received message.
   *
   * @param receivedMessage the received message
   */
  protected void sendOperationNotPermittedInfoMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    final Message operationNotPermittedInfoMessage = Message.operationNotPermittedMessage(
              receivedMessage, // receivedMessage
              this); // skill
    sendMessage(receivedMessage, operationNotPermittedInfoMessage);
}

  /**
   * Sets a message reply timeout for the given sent-by-self message. Usually the reply is received before the timeout has elapsed, and
   * cancels the timer which is looked up by message reply-with.
   *
   * @param receivedMessage the received message which invoked the skill which sent the message
   * @param message the given sent message
   * @param timeoutMillis the number of milliseconds to wait before
   * @param isRecoverable the indicator whether to send a timeout status message back to the message sender.
   * @param recoveryAction an optional tag which indicates the recovery action
   */
  protected void setMessageReplyTimeout(
          final Message receivedMessage,
          final Message message,
          final long timeoutMillis,
          final boolean isRecoverable,
          final String recoveryAction) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.getSenderQualifiedName().equals(getQualifiedName()) : "message sender must match this role " + message + "\nrole: " + getRole();
    assert timeoutMillis >= 0 : "timeoutMillis must not be negative";
    assert message.getReplyWith() != null : "message must have a reply-with UUID value";

    final MessageTimeoutTask messageTimeoutTask = new MessageTimeoutTask(this);
    final MessageTimeOutInfo messageTimeOutInfo = new MessageTimeOutInfo(
            receivedMessage,
            message,
            timeoutMillis,
            isRecoverable,
            recoveryAction,
            messageTimeoutTask);
    messageTimeoutTask.messageTimeOutInfo = messageTimeOutInfo;
    synchronized (messageTimeOutInfoDictionary) {
      messageTimeOutInfoDictionary.put(message.getReplyWith(), messageTimeOutInfo);
    }
    role.getNodeRuntime().getTimer().schedule(
            messageTimeoutTask,
            timeoutMillis); // delay
  }

  /**
   * Removes a previously set replyMessage timeout using the given replyTo value of the sent message.
   *
   * @param replyWith the given replyTo value of the sent message
   */
  protected void removeMessageTimeOut(final UUID replyWith) {
    //Preconditions
    assert replyWith != null : "replyWith must not be null";

    synchronized (messageTimeOutInfoDictionary) {
      final MessageTimeOutInfo messageTimeOutInfo = messageTimeOutInfoDictionary.remove(replyWith);
      assert messageTimeOutInfo != null : "inReplyTo: " + replyWith
              + "\nmessageTimeOutInfoDictionary: " + messageTimeOutInfoDictionary;
      getLogger().info("removed message timeout for " + messageTimeOutInfo + ", \nkey replyWith: " + replyWith);
      messageTimeOutInfo.messageTimeoutTask.cancel();
    }
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  abstract protected Logger getLogger();

  /**
   * Gets whether this skill is undergoing unit test, in which case single threading is preferred.
   *
   * @return whether this skill is undergoing unit test
   */
  public boolean isUnitTest() {
    return isUnitTest.get();
  }

  /**
   * Sets whether this skill is undergoing unit test, in which case single threading is preferred.
   *
   * @param isUnitTest whether this skill is undergoing unit test
   */
  public void setIsUnitTest(final boolean isUnitTest) {
    this.isUnitTest.set(isUnitTest);
  }

  /**
   * Provides a message timeout task which executes unless this task is canceled beforehand.
   */
  static class MessageTimeoutTask extends TimerTask {

    // the message timeout info
    private MessageTimeOutInfo messageTimeOutInfo;
    // the skill
    private final AbstractSkill skill;

    /**
     * Constructs a new MessageTimeoutTask instance.
     *
     * @param skill the skill
     */
    MessageTimeoutTask(final AbstractSkill skill) {
      //Preconditions
      assert skill != null : "skill must not be null";

      this.skill = skill;
    }

    /**
     * Runs the message timeout task.
     */
    @Override
    public void run() {
      //Preconditions
      assert messageTimeOutInfo != null : "messageTimeOutInfo must not be null";

      skill.removeMessageTimeOut(messageTimeOutInfo.message.getReplyWith());
      final Message timeoutMessage;
      if (messageTimeOutInfo.isRecoverable) {
        // send MESSAGE_TIMEOUT_INFO message to self
        timeoutMessage = skill.makeMessage(
                skill.role.getQualifiedName(), // recipientQualifiedName
                skill.getClassName(), // recipientService
                AHCSConstants.MESSAGE_TIMEOUT_INFO); // operation
      } else {
        // send MESSAGE_TIMEOUT_ERROR_INFO to parent role
        timeoutMessage = new Message(
                skill.getQualifiedName(), // senderQualifiedName
                skill.getClassName(), // senderService
                skill.role.getParentQualifiedName(), // recipientQualifiedName
                null, // recipientService
                AHCSConstants.MESSAGE_TIMEOUT_INFO); // operation
      }
      timeoutMessage.put(AHCSConstants.MESSAGE_TIMEOUT_INFO_ORIGINAL_MESSAGE, messageTimeOutInfo.message);
      skill.sendMessageViaSeparateThread(
              messageTimeOutInfo.receivedMessage,
              timeoutMessage);
    }

  }

  /**
   * Provides a container for message timeout information, indexed by the reply-with UUID.
   */
  static class MessageTimeOutInfo {

    // the received message which invoked the skill which sent the message
    private final Message receivedMessage;
    // the sent message
    private final Message message;
    // the timeout milliseconds
    private final long timeoutMillis;
    // the indicator whether the skill can attempt recovery
    private final boolean isRecoverable;
    // the recovery action
    private final String recoveryAction;
    // the message timeout task
    private final MessageTimeoutTask messageTimeoutTask;

    MessageTimeOutInfo(
            final Message receivedMessage,
            final Message message,
            final long timeoutMillis,
            final boolean isRecoverable,
            final String recoveryAction,
            final MessageTimeoutTask messageTimeoutTask) {
      //Preconditions
      assert message != null : "message must not be null";
      assert timeoutMillis >= 0 : "timeoutMillis must not be negative";
      assert messageTimeoutTask != null : "messageTimeoutTask must not be null";

      this.receivedMessage = receivedMessage;
      this.message = message;
      this.timeoutMillis = timeoutMillis;
      this.isRecoverable = isRecoverable;
      this.recoveryAction = recoveryAction;
      this.messageTimeoutTask = messageTimeoutTask;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      if (StringUtils.isNonEmptyString(recoveryAction)) {
        return "[Message timeout millis: " + timeoutMillis + ", " + message.toBriefString() + ", recovery action: " + recoveryAction + "]";
      } else {
        return "[Message timeout millis: " + timeoutMillis + ", " + message.toBriefString() + "]";
      }
    }
  }

  /**
   * Returns whether some other object equals this one.
   *
   * @param obj the other object
   *
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj;
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 53 * hash + Objects.hashCode(this.role);
    hash = 53 * hash + Objects.hashCode(this.skillState);
    return hash;
  }

  /**
   * Gets the skill state.
   *
   * @return the skill state
   */
  public synchronized State getSkillState() {
    return skillState;
  }

  /**
   * Sets the skill state.
   *
   * @param skillState the skill state
   */
  public synchronized void setSkillState(final State skillState) {
    //Preconditions
    assert skillState != null : "skillState must not be null";

    this.skillState = skillState;
  }

  /**
   * Returns a description of the skill state for logging.
   *
   * @return a description of the given skill state
   */
  public String stateDescription() {
    return stateDescription(skillState);
  }

  /**
   * Returns a description of the given skill state for logging.
   *
   * @param skillState the given skill state
   *
   * @return a description of the given skill state
   */
  protected static String stateDescription(final State skillState) {
    //Preconditions
    assert skillState != null : "skillState must not be null";

    if (skillState.equals(State.UNINITIALIZED)) {
      // this skill is not yet initialized
      return "uninitialized";
    } else if (skillState.equals(State.ISOLATED_FROM_NETWORK)) {
      // this skill is not ready to perform its mission because local container agent /roles having network singleton parents have not yet
      // exchanged X.509 certificates with those network singleton parents.
      return "isolated from the network";
    } else if (skillState.equals(State.READY)) {
      // this skill is ready to help perform its containing role's mission
      return "ready";
    } else if (skillState.equals(State.INACTIVE)) {
      // this is the state of a container agent / role which is an instance of a network singleton agent / role, but is inactive
      // because the active network singleton is hosted by another container
      return "inactive";
    } else {
      assert false;
      return null;
    }
  }

  /**
   * Gets the node state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   *
   * @return the state value associated with the given variable name
   */
  protected Object getStateValue(final String stateVariableName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(stateVariableName) : "stateVariableName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    return role.getNode().getStateValue(stateVariableName);
  }

  /**
   * Sets the node state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   * @param value the state value
   */
  protected void setStateValue(final String stateVariableName, final Object value) {
    //Preconditions
    assert StringUtils.isNonEmptyString(stateVariableName) : "stateVariableName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    role.getNode().setStateValue(stateVariableName, value);
  }

  /**
   * Removes the node state value binding for the given variable.
   *
   * @param stateVariableName the variable name
   */
  protected void removeStateValueBinding(final String stateVariableName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(stateVariableName) : "stateVariableName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    role.removeStateValueBinding(stateVariableName);
  }

  /**
   * Returns the container name.
   *
   * @return the container name
   */
  protected String getContainerName() {
    return getNodeRuntime().getContainerName();
  }

  /**
   * Gets the session manager skill.
   *
   * @return the session manager skill
   */
  protected SessionManagerSkill getSessionManagerSkill() {
    return sessionManagerSkill;
  }

  /**
   * Sets the parent session manager skill for a managed session.
   *
   * @param sessionManagerSkill the session manager skill
   */
  protected void setSessionManagerSkill(final SessionManagerSkill sessionManagerSkill) {
    //Preconditions
    assert sessionManagerSkill != null : "sessionManagerSkill must not be null";

    this.sessionManagerSkill = sessionManagerSkill;
  }

  /**
   * Finds or creates a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   *
   * @return a sharable subskill instance
   */
  protected AbstractSubSkill findOrCreateSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    return getRole().findOrCreateSubSkill(subSkillClassName);
  }

  /**
   * Finds a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   *
   * @return a sharable subskill instance, or null if not found
   */
  protected AbstractSubSkill findSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    return getRole().findSubSkill(subSkillClassName);
  }

  /**
   * Receive the new parent role's acknowledgement of joining the network.
   *
   * @param receivedMessage the received perform mission task message
   */
  protected void joinAcknowledgedTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";

    getLogger().info("join acknowledged from " + receivedMessage.getSenderQualifiedName());
    final Message removeUnjoinedRoleInfoMessage = makeMessage(
            getContainerName() + ".ContainerOperationAgent.ContainerSingletonConfigurationRole", // recipientQualifiedName
            "org.texai.skill.singletonConfiguration.ContainerSingletonConfiguration", // recipientService
            AHCSConstants.REMOVE_UNJOINED_ROLE_INFO); // operation
    sendMessageViaSeparateThread(
            receivedMessage,
            removeUnjoinedRoleInfoMessage);
  }

  /**
   * Executes the given runnable on a separate thread.
   *
   * @param runnable the given runnable
   */
  protected void execute(final Runnable runnable) {
    //Preconditions
    assert runnable != null : "runnable must not be null";

    getNodeRuntime().getExecutor().execute(runnable);
  }
}
