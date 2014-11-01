/*
 * AbstractSkill.java
 *
 * Created on Jan 16, 2008, 10:57:22 AM
 *
 * Description: An abstract skill that provides behavior for a role.
 *
 * Copyright (C) Jan 16, 2008 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.ahcsSupport;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.texai.ahcsSupport.AHCSConstants.State;
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

  /**
   * Constructs a new Skill instance.
   */
  public AbstractSkill() {
  }

  /**
   * Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  public RDFEntityManager getRDFEntityManager() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNodeRuntime().getRdfEntityManager();
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
   * Receives and attempts to process the given message. The skill is thread
   * safe, given that any contained libraries are single threaded with regard to
   * the conversation.
   *
   * @param message the given message
   *
   * @return whether the message was successfully processed
   */
  public abstract boolean receiveMessage(final Message message);

  /**
   * Synchronously processes the given message. The skill is thread safe, given
   * that any contained libraries are single threaded with regard to the
   * conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  public abstract Message converseMessage(final Message message);

  /**
   * Sends the given message via the node runtime.
   *
   * @param message the given message
   */
  public void sendMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    //Preconditions
    assert role != null : "role must not be null for " + this;

    role.sendMessage(message);
  }

  /**
   * Sends the given message via the node runtime.
   *
   * @param message the given message
   */
  public void sendMessageViaSeparateThread(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole() != null : "role must not be null";

    getNodeRuntime().getExecutor().execute(new MessageSendingRunnable(
            message,
            getRole()));
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
   * Propagates the given operation to the child roles, and to any service that
   * understands the operation.
   *
   * @param operation the given operation
   */
  public void propagateOperationToChildRoles(final String operation) {
    //Preconditions
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    role.propagateOperationToChildRoles(
            operation,
            getClassName(), // senderService
            null); // service
  }

  /**
   * Propagates the given operation to the child roles.
   *
   * @param service the recipient service, which if null indicates that any
   * service that understands the operation will receive the message
   * @param operation the given operation
   */
  public void propagateOperationToChildRoles(
          final String service,
          final String operation) {
    //Preconditions
    assert StringUtils.isNonEmptyString(operation) : "operation must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    role.propagateOperationToChildRoles(
            operation,
            getClassName(), // senderService
            service);
  }

  /**
   * Sends a not-understood message in response to the received message.
   *
   * @param receivedMessage the received message
   */
  protected void sendDoNotUnderstandMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    sendMessage(notUnderstoodMessage(receivedMessage));
  }

  /**
   * Returns a not-understood message for replying to the sender of the given
   * message.
   *
   * @param receivedMessage the given message
   *
   * @return a not-understood message for replying to the sender of the given
   * message
   */
  protected Message notUnderstoodMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert role != null : "role must not be null";

    final Message message = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            receivedMessage.getSenderQualifiedName(), // recipientQualifiedName
            receivedMessage.getSenderService(), // service
            AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO); // operation
    message.put(AHCSConstants.AHCS_ORIGINAL_MESSAGE, receivedMessage);
    return message;
  }

  /**
   * Sets a message reply timeout for the given sent message. Usually the reply
   * is received before the timeout has elapsed, and cancels the timer which is
   * looked up by message reply-with.
   *
   * @param message the given sent message
   * @param timeoutMillis the number of milliseconds to wait before
   * @param isRecoverable the indicator whether to send a timeout status message
   * back to the message sender.
   * @param recoveryAction an optional tag which indicates the recovery action
   */
  protected void setMessageReplyTimeout(
          final Message message,
          final long timeoutMillis,
          final boolean isRecoverable,
          final String recoveryAction) {
    //Preconditions
    assert message != null : "message must not be null";
    assert timeoutMillis >= 0 : "timeoutMillis must not be negative";

    final MessageTimeoutTask messageTimeoutTask = new MessageTimeoutTask(this);
    role.getNodeRuntime().getTimer().schedule(
            messageTimeoutTask, 
            0L); // delay
    final MessageTimeOutInfo messageTimeOutInfo = new MessageTimeOutInfo(
            message,
            timeoutMillis,
            isRecoverable,
            recoveryAction,
            messageTimeoutTask);
    messageTimeoutTask.messageTimeOutInfo = messageTimeOutInfo;
    synchronized (messageTimeOutInfoDictionary) {
      messageTimeOutInfoDictionary.put(message.getReplyWith(), messageTimeOutInfo);
    }
  }

  /**
   * Removes a previously set message timeout.
   *
   * @param message the given set message
   */
  protected void removeMessageTimeOut(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    synchronized (messageTimeOutInfoDictionary) {
      messageTimeOutInfoDictionary.remove(message.getReplyWith());
    }
  }

  /**
   * Provides a message timeout task which executes unless this task is canceled
   * beforehand.
   */
  class MessageTimeoutTask extends TimerTask {

    // the message timeout info
    private MessageTimeOutInfo messageTimeOutInfo;
    // the skill
    private final AbstractSkill skill;


    /** Constructs a new MessageTimeoutTask instance.
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

      skill.removeMessageTimeOut(messageTimeOutInfo.message);
      final Message timeoutMessage;
      if (messageTimeOutInfo.isRecoverable) {
        // send MESSAGE_TIMEOUT_INFO message to self
        timeoutMessage = new Message(
          messageTimeOutInfo.message.getRecipientQualifiedName(), // senderQualifiedName
          messageTimeOutInfo.message.getRecipientService(), // senderService
          messageTimeOutInfo.message.getRecipientQualifiedName(), // recipientQualifiedName
          messageTimeOutInfo.message.getRecipientService(), // recipientService
          AHCSConstants.MESSAGE_TIMEOUT_INFO);
        
      } else {
        // send MESSAGE_TIMEOUT_ERROR_INFO to parent role
        timeoutMessage = new Message(
          messageTimeOutInfo.message.getRecipientQualifiedName(), // senderQualifiedName
          messageTimeOutInfo.message.getRecipientService(), // senderService
          skill.role.getParentQualifiedName(),  // recipientQualifiedName
          null, // recipientService
          AHCSConstants.MESSAGE_TIMEOUT_ERROR_INFO);
      }
      skill.sendMessageViaSeparateThread(timeoutMessage);
    }

  }

  /**
   * Provides a container for message timeout information, indexed by the
   * reply-with UUID.
   */
  class MessageTimeOutInfo {

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
            final Message message,
            final long timeoutMillis,
            final boolean isRecoverable,
            final String recoveryAction,
            final MessageTimeoutTask messageTimeoutTask) {
      //Preconditions
      assert message != null : "message must not be null";
      assert timeoutMillis >= 0 : "timeoutMillis must not be negative";
      assert message != null : "message must not be null";

      this.message = message;
      this.timeoutMillis = timeoutMillis;
      this.isRecoverable = isRecoverable;
      this.recoveryAction = recoveryAction;
      this.messageTimeoutTask = messageTimeoutTask;
    }

    /** Returns a string representation of this object.
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return "[MessageTimeOutInfo " + message + "]";
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
  public State getSkillState() {
    return skillState;
  }

  /**
   * Sets the skill state.
   *
   * @param skillState the skill state
   */
  public void setSkillState(final State skillState) {
    //Preconditions
    assert skillState != null : "skillState must not be null";

    this.skillState = skillState;
  }

  /**
   * Returns a description of the given skill state for logging.
   *
   * @param skillState the given skill state
   *
   * @return a description of the given skill state
   */
  public static String stateDescription(final State skillState) {
    if (skillState.equals(State.INITIALIZED)) {
      return "initialized";
    } else if (skillState.equals(State.READY)) {
      return "ready";
    } else if (skillState.equals(State.SHUTDOWN)) {
      return "shutdown";
    } else if (skillState.equals(State.UNINITIALIZED)) {
      return "uninitialized";
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
  public Object getStateValue(final String stateVariableName) {
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
  public void setStateValue(final String stateVariableName, final Object value) {
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
  public void removeStateValueBinding(final String stateVariableName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(stateVariableName) : "stateVariableName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    role.removeStateValueBinding(stateVariableName);
  }

  /**
   * Gets the session manager skill.
   *
   * @return the session manager skill
   */
  public SessionManagerSkill getSessionManagerSkill() {
    return sessionManagerSkill;
  }

  /**
   * Sets the parent session manager skill for a managed session.
   *
   * @param sessionManagerSkill the session manager skill
   */
  public void setSessionManagerSkill(final SessionManagerSkill sessionManagerSkill) {
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
  public AbstractSubSkill findOrCreateSubSkill(final String subSkillClassName) {
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
  public AbstractSubSkill findSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    return getRole().findSubSkill(subSkillClassName);
  }

  /**
   * Provides a message sending runnable.
   */
  public static class MessageSendingRunnable implements Runnable {

    /**
     * the message to send
     */
    final Message message;
    /**
     * the sender role
     */
    final Role role;

    /**
     * Constructs a new MessageSendingRunnable instance.
     *
     * @param message the message to send
     * @param role the sender role
     */
    public MessageSendingRunnable(
            final Message message,
            final Role role) {
      //Preconditions
      assert message != null : "message must not be null";
      assert role != null : "role must not be null";

      this.message = message;
      this.role = role;
    }

    /**
     * Sends the message.
     */
    @Override
    public void run() {
      role.sendMessage(message);
    }
  }

}
