/*
 * Node.java
 *
 * Created on Jan 16, 2008, 10:57:07 AM
 *
 * Description: Provides a node in an Albus Hierarchical Control System.
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
package org.texai.ahcsSupport.domainEntity;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a node in an Albus Hierarchical Control System.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class Node implements CascadePersistence, Comparable<Node> {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the node type URI, note that NodeType is persisted in a different repository */
  @RDFProperty()
  private final URI nodeTypeURI;
  /** the node type name */
  @RDFProperty()
  private final String nodeTypeName;
  /** the node nickname */
  @RDFProperty()
  private String nodeNickname;
  /** the roles */
  @RDFProperty(predicate = "texai:ahcsNode_role")
  private final Set<Role> roles = new HashSet<>();
  /** the state values */
  @RDFProperty()
  private final Set<StateValueBinding> stateValueBindings = new HashSet<>();
  /** the node state variable dictionary, state variable name --> state value binding */
  private final Map<String, StateValueBinding> stateVariableDictionary = new HashMap<>();
  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(Node.class);
  /** the role id dictionary, role id --> role */
  private final Map<URI, Role> roleIdDictionary = new HashMap<>();
  /** the role type name dictionary, role type name --> role */
  private final Map<String, Role> roleTypeNameDictionary = new HashMap<>();
  /** the node runtime */
  private transient NodeRuntime nodeRuntime;

  /** Constructs a new Node instance. */
  public Node() {
    nodeTypeURI = null;
    nodeTypeName = null;
    nodeNickname = null;
    nodeRuntime = null;
  }

  /** Constructs a new Node instance.
   *
   * @param nodeType the node
   * @param nodeRuntime the node runtime
   */
  public Node(
          final NodeType nodeType,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeType != null : "nodeType must not be null";
    assert nodeType.getId() != null : "node type id must not be null";
    assert nodeType.getTypeName() != null : "node type name must not be null";

    this.nodeTypeURI = nodeType.getId();
    this.nodeTypeName = nodeType.getTypeName();
    this.nodeRuntime = nodeRuntime;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the node type URI.
   *
   * @return the node type URI
   */
  public URI getNodeTypeURI() {
    return nodeTypeURI;
  }

  /** Returns whether this is the top friendship node.
   *
   * @return whether this is the top friendship node
   */
  public boolean isTopFriendshipNode() {
    return this.nodeTypeName.equals(AHCSConstants.TOP_FRIENDSHIP_NODE_TYPE);
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

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        for (final StateValueBinding stateValueBinding : stateValueBindings) {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        }
      }
      final StateValueBinding stateValueBinding = stateVariableDictionary.get(stateVariableName);
      if (stateValueBinding == null) {
        return null;
      } else {
        return stateValueBinding.getValue();
      }
    }
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

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        for (final StateValueBinding stateValueBinding : stateValueBindings) {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        }
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

  /** Removes the node state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeNodeStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        for (final StateValueBinding stateValueBinding : stateValueBindings) {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        }
      }
      final StateValueBinding stateValueBinding = stateVariableDictionary.remove(variableName);
      if (stateValueBinding != null) {
        final boolean isRemoved = stateValueBindings.remove(stateValueBinding);
        assert isRemoved;
      }
    }
  }

  /** Installs the roles for this node.
   *
   * @param nodeAccess the node access object
   */
  public void installRoles(final NodeAccess nodeAccess) {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    if (roles.isEmpty()) {
      // this node has not yet been persisted
      final NodeType nodeType = nodeAccess.getNodeType(this);
      final Collection<RoleType> roleTypes = nodeType.getAllRoleTypes();
      LOGGER.info(nodeNickname + ": installing " + roleTypes.size() + " roles from role types");
      synchronized (roles) {
        for (final RoleType roleType : roleTypes) {
          final Role role = new Role(
                  roleType,
                  nodeRuntime);
          role.setNode(this);
          nodeRuntime.setX509SecurityInfoAndIdForRole(role);
          nodeAccess.getRDFEntityManager().persist(role);
          LOGGER.info(nodeNickname + ":   persisting role " + role);
          roles.add(role);
          installRole(role, nodeAccess);
        }
      }
    } else {
      // this node has already been persisted
      LOGGER.info(nodeNickname + ": installing " + roles.size() + " roles");
      synchronized (roles) {
        for (final Role role : roles) {
          installRole(role, nodeAccess);
        }
      }
    }
  }

  /** Installs the given role.
   *
   * @param role the given role
   * @param nodeAccess the node access object
   */
  private void installRole(final Role role, final NodeAccess nodeAccess) {
    //Preconditions
    assert role != null : "role must not be null";
    assert nodeAccess != null : "nodeAccess must not be null";
    assert !roleIdDictionary.containsKey(role.getId()) : "role must not already be installed " + role;

    roleIdDictionary.put(role.getId(), role);
    final RoleType roleType = nodeAccess.getRoleType(role);
    assert roleType != null : "role type not found for " + role;
    assert !roleTypeNameDictionary.containsKey(roleType.getTypeName()) : "role type must not already be installed " + role;
    roleTypeNameDictionary.put(roleType.getTypeName(), role);
    nodeRuntime.registerRole(role);
    role.installSkills(nodeAccess);
  }

  /** Returns an unmodifiable view of the set of roles.
   *
   * @return an unmodifiable view of the set of roles
   */
  public Collection<Role> getRoles() {
    synchronized (roleIdDictionary) {
      return Collections.unmodifiableCollection(roleIdDictionary.values());
    }
  }

  /** Gets the role for the given role type name or null if not found.
   *
   * @param roleTypeName the given role type name
   * @return the role or null if not found
   */
  public Role getRoleForTypeName(final String roleTypeName) {
    //Preconditions
    assert roleTypeName != null : "roleTypeName must not be null";
    assert !roleTypeName.isEmpty() : "roleTypeName must not be empty";

    return roleTypeNameDictionary.get(roleTypeName);
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    if (nodeTypeName == null) {
      return "[Node]";
    } else {
      return "[" + nodeNickname + ": " + nodeTypeName + "]";
    }
  }

  /** Gets the node runtime.
   *
   * @return the node runtime
   */
  public NodeRuntime getNodeRuntime() {
    return nodeRuntime;
  }

  /** Sets the node runtime.
   *
   * @param nodeRuntime the node runtime
   */
  public void setNodeRuntime(NodeRuntime nodeRuntime) {
    this.nodeRuntime = nodeRuntime;
  }

  /** Gets the node type name.
   *
   * @return the node type name
   */
  public String getNodeTypeName() {
    return nodeTypeName;
  }

  /** Gets the node nickname.
   *
   * @return the node nickname
   */
  public String getNodeNickname() {
    return nodeNickname;
  }

  /** Sets the node nickname.
   *
   * @param nodeNickname the node nickname
   */
  public void setNodeNickname(final String nodeNickname) {
    this.nodeNickname = nodeNickname;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether the other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Node other = (Node) obj;
    if (Objects.equals(this.id, other.id)) {
      assert (this.nodeNickname == null && other.nodeNickname == null) || (this.nodeNickname.equals(other.nodeNickname)) :
              "nodes having equal ids must have equal nicknames: " + this + " " + other;
      return true;
    } else {
      return false;
    }
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + Objects.hashCode(this.id);
    hash = 29 * hash + Objects.hashCode(this.nodeTypeURI);
    return hash;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.instantiate();
    }
    for (final Role role : roles) {
      role.instantiate();
    }
  }

  /** Recursively persists this RDF entity and all its components.
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

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager, URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    }
    for (final Role role : roles) {
      role.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    }
    rdfEntityManager.persist(this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    }
    for (final Role role : roles) {
      role.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    }
    rdfEntityManager.remove(this);
  }

  /** Compares another node to this one.
   *
   * @param that the other node
   * @return -1 if less than, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final Node that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.nodeNickname.compareTo(that.nodeNickname);
  }

}
