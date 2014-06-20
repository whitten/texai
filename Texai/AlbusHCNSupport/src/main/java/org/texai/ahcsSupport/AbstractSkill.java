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

import java.util.Objects;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.StringUtils;

/** An abstract skill that provides behavior for a role.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public abstract class AbstractSkill {

  /** the role containing this skill */
  private Role role;
  /** the skill state */
  private State skillState = State.UNINITIALIZED;
  /** the session manager skill, populated only for managed session skills so that they can notify the manager of disconnected sessions */
  private SessionManagerSkill sessionManagerSkill;

  /** Constructs a new Skill instance. */
  public AbstractSkill() {
  }

  /** Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  public RDFEntityManager getRDFEntityManager() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNodeRuntime().getRdfEntityManager();
  }

  /** Gets the node runtime.
   *
   * @return the role containing this skill
   */
  public NodeRuntime getNodeRuntime() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNodeRuntime();
  }

  /** Gets the role containing this skill.
   *
   * @return the role containing this skill
   */
  public Role getRole() {
    return role;
  }

  /** Sets the role containing this skill.
   *
   * @param role the role containing this skill
   */
  public void setRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    this.role = role;
  }

  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  public abstract boolean receiveMessage(final Message message);

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return the response message or null if not applicable
   */
  public abstract Message converseMessage(final Message message);

  /** Sends the given message via the node runtime.
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

  /** Sends the given message via the node runtime.
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

  /** Returns the id of the containing role.
   *
   * @return the id of the containing role
   */
  public URI getRoleId() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getId();
  }

  /** Returns the id of the role having the given type contained in the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @param roleTypeName the given role type
   * @return the id of the role having the given type contained in the node having the given nickname, or null if not found
   */
  public URI getRoleId(
          final String nodeNickname,
          final String roleTypeName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(nodeNickname) : "nodeNickname must be a non-empty string";
    assert StringUtils.isNonEmptyString(roleTypeName) : "roleTypeName must be a non-empty string";

    return getNodeRuntime().getRoleId(nodeNickname, roleTypeName);
  }

  /** Returns the class name of this skill.
   *
   * @return the class name of this skill
   */
  public String getClassName() {
    return getClass().getName();
  }

  /** Returns the containing node's nickname.
   *
   * @return the containing node's nickname
   */
  public String getNodeNickname() {
    //Preconditions
    assert role != null : "role must not be null for " + this;

    return role.getNode().getNodeNickname();
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  public abstract String[] getUnderstoodOperations();

  /** Returns whether the given operation is understood.
   *
   * @param operation the given operation
   * @return whether the given operation is understood
   */
  public boolean isOperationUnderstood(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    for (final String operation1 : getUnderstoodOperations()) {
      if (operation.equals(operation1)) {
        return true;
      }
    }
    return false;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('[');
    if (role != null) {
      final Node node = role.getNode();
      if (node != null) {
        stringBuilder.append(node.getNodeNickname()).append(':');
      }
      stringBuilder.append(role.getRoleTypeName()).append(':');
    }
    stringBuilder.append(getClass().getSimpleName()).append(']');
    return stringBuilder.toString();
  }

  /** Returns a qualified name for the given role id if local, otherwise returns the URI string.
   *
   * @param roleId the role id
   * @return a qualified name for the given role id if local, otherwise returns the URI string
   */
  public String getRoleQualifiedName(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    final Role localRole = getRole().getNodeRuntime().getLocalRole(roleId);
    if (localRole == null) {
      return roleId.toString();
    } else {
      return localRole.toString();
    }
  }

  /** Propagates the given operation to the child roles, and to any service that understands the operation.
   *
   * @param operation the given operation
   */
  public void propagateOperationToChildRoles(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";
    assert role != null : "role must not be null for " + this;

    role.propagateOperationToChildRoles(
            operation,
            getClassName(), // senderService
            null); // service
  }

  /** Propagates the given operation to the child roles.
   *
   * @param service the recipient service, which if null indicates that any service that understands the operation will receive the message
   * @param operation the given operation
   */
  public void propagateOperationToChildRoles(
          final String service,
          final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";
    assert role != null : "role must not be null for " + this;

    role.propagateOperationToChildRoles(
            operation,
            getClassName(), // senderService
            service);
  }

  /** Sends a not-understood message in response to the received message.
   *
   * @param receivedMessage  the received message
   */
  protected void sendDoNotUnderstandMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    sendMessage(notUnderstoodMessage(receivedMessage));
  }

  /** Returns a not-understood message for replying to the sender of the given message.
   *
   * @param receivedMessage the given message
   * @return a not-understood message for replying to the sender of the given message
   */
  protected Message notUnderstoodMessage(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert role != null : "role must not be null";

    final Message message = new Message(
            getRoleId(), // senderRoleId
            getClassName(), // senderService,
            receivedMessage.getSenderRoleId(), // recipientRoleId
            receivedMessage.getSenderService(), // service
            AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO); // operation
    message.put(AHCSConstants.AHCS_ORIGINAL_MESSAGE, receivedMessage);
    return message;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    return this == obj;
  }

  /** Returns a hash code for this object.
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

  /** Gets the skill state.
   *
   * @return the skill state
   */
  public State getSkillState() {
    return skillState;
  }

  /** Sets the skill state.
   *
   * @param skillState the skill state
   */
  public void setSkillState(final State skillState) {
    //Preconditions
    assert skillState != null : "skillState must not be null";

    this.skillState = skillState;
  }

  /** Returns a description of the given skill state for logging.
   *
   * @param skillState the given skill state
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

  /** Gets the node state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   * @return the state value associated with the given variable name
   */
  public Object getNodeStateValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";
    assert role != null : "role must not be null for " + this;

    return role.getNodeStateValue(stateVariableName);
  }

  /** Sets the node state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setNodeStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";
    assert role != null : "role must not be null for " + this;

    role.setNodeStateValue(variableName, value);
  }

  /** Removes the node state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeNodeStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";
    assert role != null : "role must not be null for " + this;

    role.removeNodeStateValueBinding(variableName);
  }

  /** Gets the role state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   * @return the state value associated with the given variable name
   */
  public Object getRoleStateValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";
    assert role != null : "role must not be null for " + this;

    return role.getRoleStateValue(stateVariableName);
  }

  /** Sets the role state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setRoleStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";
    assert role != null : "role must not be null for " + this;

    role.setRoleStateValue(variableName, value);
  }

  /** Removes the role state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeRoleStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";
    assert role != null : "role must not be null for " + this;

    role.removeRoleStateValueBinding(variableName);
  }

  /** Gets the session manager skill.
   *
   * @return the session manager skill
   */
  public SessionManagerSkill getSessionManagerSkill() {
    return sessionManagerSkill;
  }

  /** Sets the parent session manager skill for a managed session.
   *
   * @param sessionManagerSkill the session manager skill
   */
  public void setSessionManagerSkill(final SessionManagerSkill sessionManagerSkill) {
    //Preconditions
    assert sessionManagerSkill != null : "sessionManagerSkill must not be null";

    this.sessionManagerSkill = sessionManagerSkill;
  }

  /** Finds or creates a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   * @return a sharable subskill instance
   */
  public AbstractSubSkill findOrCreateSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";
    assert role != null : "role must not be null for " + this;

    return getRole().findOrCreateSubSkill(subSkillClassName);
  }

  /** Finds a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   * @return a sharable subskill instance, or null if not found
   */
  public AbstractSubSkill findSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    return getRole().findSubSkill(subSkillClassName);
  }

  /** Provides a message sending runnable. */
  public static class MessageSendingRunnable implements Runnable {

    /** the message to send */
    final Message message;
    /** the sender role */
    final Role role;

    /** Constructs a new MessageSendingRunnable instance.
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

    /** Sends the message. */
    @Override
    public void run() {
      role.sendMessage(message);
    }
  }

}
