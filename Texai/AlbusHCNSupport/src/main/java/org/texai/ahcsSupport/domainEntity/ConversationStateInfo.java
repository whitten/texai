/*
 * StateInfo.java
 *
 * Created on Mar 13, 2010, 6:22:22 PM
 *
 * Description: Provides persistent state for a conversation.
 *
 * Copyright (C) Mar 13, 2010 reed.
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
package org.texai.ahcsSupport.domainEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides persistent state for a conversation.
 *
 * @author reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class ConversationStateInfo implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the node */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_node")
  private final Node node;
  /** the role */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_role")
  private final Role role;
  /** the skill class name */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_skillClassName")
  private final String skillClassName;
  /** the conversation id */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_conversationId")
  private final UUID conversationId;
  /** the state variable name list */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_stateVariableNameList")
  private List<String> stateVariableNames;
  /** the state value list */
  @RDFProperty(predicate = "texai:ahcsConversationStateInfo_stateValueList")
  private List<Serializable> stateValues;
  /** the transient state variable dictionary, state variable name --> value */
  private final Map<String, Serializable> stateVariableDictionary = new HashMap<>();

  /** Constructs a new ConversationStateInfo instance. */
  public ConversationStateInfo() {
    node = null;
    role = null;
    skillClassName = null;
    conversationId = null;
  }

  /** Constructs a new StateInfo instance.
   *
   * @param node the node
   * @param role the role
   * @param skillClassName the skill class name
   * @param conversationId the conversation id
   * @param stateVariableDictionary the state variable dictionary
   */
  public ConversationStateInfo(
          final Node node,
          final Role role,
          final String skillClassName,
          final UUID conversationId,
          final Map<String, Serializable> stateVariableDictionary) {
    //Preconditions
    assert node != null : "node must not be null";
    assert role != null : "role must not be null";
    assert skillClassName != null : "skillClassName must not be null";
    assert !skillClassName.isEmpty() : "skillClassName must not be empty";
    assert conversationId != null : "conversationId must not be null";
    assert stateVariableDictionary != null : "stateVariableDictionary must not be null";
    assert !stateVariableDictionary.isEmpty() : "stateVariableDictionary must not be empty";

    this.node = node;
    this.role = role;
    this.skillClassName = skillClassName;
    this.conversationId = conversationId;
    putAllPrivate(stateVariableDictionary);
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the node.
   *
   * @return the node
   */
  public Node getNode() {
    return node;
  }

  /** Gets the role.
   *
   * @return the role
   */
  public Role getRole() {
    return role;
  }

  /** Gets the skill class name.
   *
   * @return the skill class name
   */
  public String getSkillClassName() {
    return skillClassName;
  }

  /** Gets the conversation id.
   *
   * @return the conversation id
   */
  public UUID getConversationId() {
    return conversationId;
  }

  /** Gets the state value for the given state variable name.
   *
   * @param stateVariableName the given state variable name
   * @return the state value
   */
  public Object getValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";

    populateStateVariableDictionary();
    return stateVariableDictionary.get(stateVariableName);
  }

  /** Puts the entries of the given state variable dictionary into the respective name and value lists
   * for persistence.
   *
   * @param stateVariableDictionary the given state variable dictionary
   */
  private void putAllPrivate(final Map<String, Serializable> stateVariableDictionary) {
    //Preconditions
    assert stateVariableDictionary != null : "stateVariableDictionary must not be null";

    putAll(stateVariableDictionary);
  }

  /** Puts the entries of the given state variable dictionary into the respective name and value lists
   * for persistence.
   *
   * @param stateVariableDictionary the given state variable dictionary
   */
  public void putAll(final Map<String, Serializable> stateVariableDictionary) {
    //Preconditions
    assert stateVariableDictionary != null : "stateVariableDictionary must not be null";

    this.stateVariableDictionary.putAll(stateVariableDictionary);
    if (stateVariableNames == null) {
      final int stateVariableDictionary_size = stateVariableDictionary.size();
      stateVariableNames = new ArrayList<>(stateVariableDictionary_size);
      stateValues = new ArrayList<>(stateVariableDictionary_size);
    }
    for (final Entry<String, Serializable> entry : stateVariableDictionary.entrySet()) {
      stateVariableNames.add(entry.getKey());
      stateValues.add(entry.getValue());
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  @Override
  public String toString() {
    populateStateVariableDictionary();
    return "[ConversationStateInfo " + stateVariableDictionary + "]";
  }

  /** Ensures that the state variable dictionary is populated, e.g. following instantiation of this object
   * from the KB.
   */
  private void populateStateVariableDictionary() {
    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty()) {
        for (int i = 0; i < stateVariableNames.size(); i++) {
          stateVariableDictionary.put(stateVariableNames.get(i), stateValues.get(i));
        }
      }
    }
  }
}
