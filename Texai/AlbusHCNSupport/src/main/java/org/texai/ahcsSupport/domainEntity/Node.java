/*
 * Node.java
 *
 * Created on Jan 16, 2008, 10:57:07 AM
 *
 * Description: Provides a node in an Albus Hierarchical Control System.
 *
 * Copyright (C) Jan 16, 2008 Stephen L. Reed.
 */
package org.texai.ahcsSupport.domainEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;

/**
 * Provides a node in an Albus Hierarchical Control System.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class Node implements CascadePersistence, Comparable<Node> {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  private URI id;
  // the node name which must end in "Agent"
  @RDFProperty()
  private final String name;
  // the node's mission described in English
  @RDFProperty
  private final String missionDescription;
  // the roles
  @RDFProperty(predicate = "texai:ahcsNode_role")
  private final Set<Role> roles;
  // the persistent node state variables and their respective values
  @RDFProperty()
  private final Set<StateValueBinding> stateValueBindings;
  // the indicator whether this node is a singleton nomadic agent, in which case only one container in the network hosts
  // the active node and all other nodes having the same name are inactive
  @RDFProperty()
  private final boolean isNetworkSingleton;
  // the node state variable dictionary, state variable name --> state value binding
  private final Map<String, StateValueBinding> stateVariableDictionary = new HashMap<>();
  // the logger
  public static final Logger LOGGER = Logger.getLogger(Node.class);
  // the node runtime
  private transient BasicNodeRuntime nodeRuntime;

  /**
   * Constructs a new Node instance. Used by the persistence framework.
   */
  public Node() {
    name = null;
    missionDescription = null;
    roles = null;
    stateValueBindings = null;
    nodeRuntime = null;
    isNetworkSingleton = false;
  }

  /**
   * Constructs a new Node instance.
   *
   * @param name the node name which must end in "Agent"
   * @param missionDescription the node's mission described in English
   * @param roles the roles
   * @param isNetworkSingleton the indicator whether this node is a singleton nomadic agent, in which case only one container in the network
   * hosts the active node and all other nodes having the same name are inactive
   */
  public Node(
          final String name,
          final String missionDescription,
          final Set<Role> roles,
          final boolean isNetworkSingleton) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-null string";
    assert StringUtils.isNonEmptyString(missionDescription) : "name must be a non-null missionDescription";
    assert roles != null : "roles must not be null";
    assert !roles.isEmpty() : "roles must not be empty";

    this.name = name;
    this.missionDescription = missionDescription;
    this.roles = roles;
    this.isNetworkSingleton = isNetworkSingleton;
    this.stateValueBindings = new HashSet<>();
  }

  /**
   * Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
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
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        stateValueBindings.stream().forEach((stateValueBinding) -> {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        });
      }
      final StateValueBinding stateValueBinding = stateVariableDictionary.get(stateVariableName);
      if (stateValueBinding == null) {
        return null;
      } else {
        return stateValueBinding.getValue();
      }
    }
  }

  /**
   * Sets the node state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        stateValueBindings.stream().forEach((stateValueBinding) -> {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        });
      }
      StateValueBinding stateValueBinding = stateVariableDictionary.get(variableName);
      if (stateValueBinding == null) {
        stateValueBinding = new StateValueBinding(variableName, value);
        stateVariableDictionary.put(variableName, stateValueBinding);
        stateValueBindings.add(stateValueBinding);
      } else {
        stateValueBinding.setValue(value);
      }
    }
  }

  /**
   * Removes the node state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        stateValueBindings.stream().forEach((stateValueBinding) -> {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        });
      }
      final StateValueBinding stateValueBinding = stateVariableDictionary.remove(variableName);
      if (stateValueBinding != null) {
        final boolean isRemoved = stateValueBindings.remove(stateValueBinding);
        assert isRemoved;
      }
    }
  }

  /**
   * Returns an unmodifiable view of the set of roles.
   *
   * @return an unmodifiable view of the set of roles
   */
  public Collection<Role> getRoles() {
    return Collections.unmodifiableCollection(roles);
  }

  /**
   * Gets the role having the given role name.
   *
   * @param roleName the given role name
   * @return the role having the given role name if found, otherwise return null
   */
  public Role getRole(final String roleName) {
    for (final Role role : roles) {
      if (roleName.equals(Role.extractRoleName(role.getQualifiedName()))) {
        return role;
      }
    }
    return null;
  }

  /**
   * Returns whether this node is a singleton nomadic agent, in which case only one container in the network hosts the active node and all
   * other nodes having the same name are inactive.
   *
   * @return whether this node is a singleton nomadic agent
   */
  public boolean isNetworkSingleton() {
    return isNetworkSingleton;
  }

  /**
   * Returns a predicate for whether this node is a singleton nomadic agent.
   *
   * @return a predicate for whether this node is a singleton nomadic agent
   */
  public static Predicate<Node> isNetworkSingletonNode() {
    return node -> node.isNetworkSingleton();
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return name;
  }

  /**
   * Extracts the container name from the given qualified name string, container-name.agent-name.role-name .
   *
   * @param qualifiedName the given qualified name string, container-name.agent-name.role-name
   *
   * @return the container name
   */
  public static String extractContainerName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non empty string";

    final String[] names = qualifiedName.split("\\.");
    return names[0];
  }

  /**
   * Extracts the container and agent name from the given qualified name string, container-name.agent-name.role-name .
   *
   * @param qualifiedName the given qualified name string, container-name.agent-name.role-name
   *
   * @return the container and agent name, container-name.agent-name
   */
  public static String extractContainerAgentName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non empty string";

    final String[] names = qualifiedName.split("\\.");
    assert names.length == 3;
    return names[0] + '.' + names[1];
  }

  /**
   * Extracts the agent name from this agent's qualified name string, container-name.agent-name.role-name.
   *
   * @return the agent name
   */
  public String extractAgentName() {

    final String[] names = name.split("\\.");
    assert names.length > 1;

    return names[1];
  }

  /**
   * Extracts the agent name from the given qualified name string, container-name.agent-name.role-name .
   *
   * @param qualifiedName the given qualified name string, container-name.agent-name.role-name
   *
   * @return the agent name
   */
  public static String extractAgentName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non empty string";

    final String[] names = qualifiedName.split("\\.");
    assert names.length == 3;

    return names[1];
  }

  /**
   * Extracts the agent name and role from the given qualified name string, container-name.agent-name.role-name .
   *
   * @param qualifiedName the given qualified name string, container-name.agent-name.role-name
   *
   * @return the agent name and role
   */
  public static String extractAgentRoleName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non empty string";

    int index = qualifiedName.indexOf('.');
    assert index > -1;
    return qualifiedName.substring(index + 1);
  }

  /**
   * Extracts the role name from the given qualified name string, container-name.agent-name.role-name .
   *
   * @param qualifiedName the given qualified name string, container-name.agent-name.role-name
   *
   * @return the role name
   */
  public static String extractRoleName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non empty string";

    final String[] names = qualifiedName.split("\\.");
    assert names.length == 3;
    return names[2];
  }

  /**
   * Gets the node runtime.
   *
   * @return the node runtime
   */
  public BasicNodeRuntime getNodeRuntime() {
    return nodeRuntime;
  }

  /**
   * Sets the node runtime.
   *
   * @param nodeRuntime the node runtime
   */
  public void setNodeRuntime(final BasicNodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.nodeRuntime = nodeRuntime;
  }

  /**
   * Gets the node name.
   *
   * @return the node name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the mission description.
   *
   * @return the mission description
   */
  public String getMissionDescription() {
    return missionDescription;
  }

  /**
   * Compares another node to this one.
   *
   * @param that the other node
   *
   * @return -1 if less than, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final Node that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.name.compareTo(that.name);
  }

  /**
   * Returns whether some other object equals this one.
   *
   * @param obj the other object
   *
   * @return whether the other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    return Objects.equals(this.name, ((Node) obj).name);
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + Objects.hashCode(this.id);
    hash = 29 * hash + Objects.hashCode(this.name);
    return hash;
  }

  /**
   * Ensures that this persistent object is fully instantiated.
   */
  @Override
  public void instantiate() {
    //Preconditions
    assert roles != null : "roles must not be null";
    assert !roles.isEmpty() : "roles must not be empty for node " + name;

    stateValueBindings.stream().forEach((stateValueBinding) -> {
      stateValueBinding.instantiate();
    });
    roles.stream().forEach((role) -> {
      role.instantiate();
      // ensure that the referred-to node is not a copy
      role.setNode(this);
    });
  }

  /**
   * Recursively persists this RDF entity and all its components.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  public void cascadePersist(
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadePersist(this, rdfEntityManager, overrideContext);
  }

  /**
   * Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager, URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";
    assert roles != null : "roles must not be null";
    assert !roles.isEmpty() : "roles must not be empty for node " + name;

    stateValueBindings.stream().forEach((stateValueBinding) -> {
      stateValueBinding.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    });
    roles.stream().forEach((role) -> {
      role.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    });
    rdfEntityManager.persist(this, overrideContext);
  }

  /**
   * Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";
    assert roles != null : "roles must not be null";
    assert !roles.isEmpty() : "roles must not be empty for node " + name;

    stateValueBindings.stream().forEach((stateValueBinding) -> {
      stateValueBinding.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    });
    roles.stream().forEach((role) -> {
      role.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    });
    rdfEntityManager.remove(this);
  }

}
