/*
 * NodeRuntimeConfiguration.java
 *
 * Created on Oct 21, 2011, 12:34:48 PM
 *
 * Description: Provides node runtime configuration.
 *
 * Copyright (C) Oct 21, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ahcsSupport.domainEntity;

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
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides node runtime configuration.
 *
 * @author reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class NodeRuntimeConfiguration implements CascadePersistence {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeRuntimeConfiguration.class);
  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;
  /** the node runtime id, which matches NodeRuntimeInfo.id */
  @RDFProperty
  private final URI nodeRuntimeId;
  /** the node runtime key store entry alias */
  @RDFProperty
  private String nodeRuntimeKeyStoreEntryAlias;
  /** the certificate signing key store entry alias */
  @RDFProperty
  private String certificateSigningKeyStoreEntryAlias;
  /** the nodes */
  @RDFProperty
  private final Set<Node> nodes = new HashSet<>();
  // non-persistent fields ...
  /** the local node dictionary, nickname --> local node */
  private Map<String, Node> localNodeDictionary;
  /** the local role dictionary, role id --> role */
  private Map<URI, Role> localRoleDictionary;
  /** the local role dictionary lock */
  private final Object localRoleDictionary_lock = new Object();
  /** the top friendship role */
  private Role topFriendshipRole;

  /** Constructs a new NodeRuntimeConfiguration instance. */
  public NodeRuntimeConfiguration() {
    nodeRuntimeId = null;
  }

  /** Constructs a new NodeRuntimeConfiguration instance.
   *
   * @param nodeRuntimeId the node runtime id
   */
  public NodeRuntimeConfiguration(final URI nodeRuntimeId) {
    //Preconditions
    assert nodeRuntimeId != null : "nodeRuntimeId must not be null";

    this.nodeRuntimeId = nodeRuntimeId;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the node runtime id.
   *
   * @return the nodeRuntimeId
   */
  public URI getNodeRuntimeId() {
    return nodeRuntimeId;
  }

  /** Gets the node runtime key store entry alias.
   *
   * @return the node runtime key store entry alias
   */
  public String getNodeRuntimeKeyStoreEntryAlias() {
    return nodeRuntimeKeyStoreEntryAlias;
  }

  /** Sets the node runtime key store entry alias.
   *
   * @param nodeRuntimeKeyStoreEntryAlias the node runtime key store entry alias
   */
  public void setNodeRuntimeKeyStoreEntryAlias(final String nodeRuntimeKeyStoreEntryAlias) {
    //Preconditions
    assert nodeRuntimeKeyStoreEntryAlias != null : "nodeRuntimeKeyStoreEntryAlias must not be null";
    assert !nodeRuntimeKeyStoreEntryAlias.isEmpty() : "nodeRuntimeKeyStoreEntryAlias must not be empty";

    this.nodeRuntimeKeyStoreEntryAlias = nodeRuntimeKeyStoreEntryAlias;
  }

  /** Gets the certificate signing key store entry alias.
   *
   * @return the certificate signing key store entry alias
   */
  public String getCertificateSigningKeyStoreEntryAlias() {
    return certificateSigningKeyStoreEntryAlias;
  }

  /** Sets the certificate signing key store entry alias.
   *
   * @param certificateSigningKeyStoreEntryAlias the certificate signing key store entry alias
   */
  public void setCertificateSigningKeyStoreEntryAlias(final String certificateSigningKeyStoreEntryAlias) {
    //Preconditions
    assert certificateSigningKeyStoreEntryAlias != null : "nodeRuntimeKeyStoreEntryAlias must not be null";
    assert !certificateSigningKeyStoreEntryAlias.isEmpty() : "nodeRuntimeKeyStoreEntryAlias must not be empty";

    this.certificateSigningKeyStoreEntryAlias = certificateSigningKeyStoreEntryAlias;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder()).append("[NodeRuntimeConfiguration ").append(nodeRuntimeId).append(']').toString();
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NodeRuntimeConfiguration other = (NodeRuntimeConfiguration) obj;
    if (!Objects.equals(this.id, other.id)) {
      return false;
    }
    return Objects.equals(this.nodeRuntimeId, other.nodeRuntimeId);
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 37 * hash + Objects.hashCode(this.id);
    hash = 37 * hash + Objects.hashCode(this.nodeRuntimeId);
    return hash;
  }

  /** Gets an unmodifiable copy of the local nodes.
   *
   * @return the local nodes
   */
  public Set<Node> getNodes() {
    synchronized (nodes) {
      return Collections.unmodifiableSet(nodes);
    }
  }

  /** Gets the local node dictionary, nickname --> local node.
   *
   * @return the local node dictionary
   */
  private Map<String, Node> getLocalNodeDictionary() {
    if (localNodeDictionary == null) {
      // lazy initialization
      localNodeDictionary = new HashMap<>();
      for (final Node node : nodes) {
        localNodeDictionary.put(node.getNodeNickname(), node);
      }
    }
    return localNodeDictionary;
  }

  /** Returns the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @return the node having the given nickname, or null if not found
   */
  public Node getNode(final String nodeNickname) {
    //Preconditions
    assert nodeNickname != null : "nodeNickname must not be null";
    assert !nodeNickname.isEmpty() : "nodeNickname must not be empty";

    synchronized (getLocalNodeDictionary()) { // force lazy initialization
      return localNodeDictionary.get(nodeNickname);
    }
  }

  /** Adds the given local node.
   *
   * @param node the local node to add
   */
  public void addNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";
    assert node.getNodeNickname() != null : "node nickname must not be null " + node;
    assert !node.getNodeNickname().isEmpty() : "node nickname must not be empty";

    synchronized (getLocalNodeDictionary()) { // force lazy initialization
      localNodeDictionary.put(node.getNodeNickname(), node);
      nodes.add(node);
    }
    LOGGER.info("addNode, nodeRuntimeId: " + nodeRuntimeId + ", nodes size: " + nodes.size());
  }

  /** Removes the given local node.
   *
   * @param node the given local node to remove
   */
  public void removeNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";
    assert node.getNodeNickname() != null : "node nickname must not be null";
    assert !node.getNodeNickname().isEmpty() : "node nickname must not be empty";

    synchronized (getLocalNodeDictionary()) { // force lazy initialization
      localNodeDictionary.remove(node.getNodeNickname());
      nodes.remove(node);
    }
  }

  /** Gets the local role dictionary, role id --> role.
   *
   * @return the local role dictionary
   */
  private Map<URI, Role> getLocalRoleDictionary() {
    synchronized (localRoleDictionary_lock) {
      if (localRoleDictionary == null) {
        // lazy initialization
        localRoleDictionary = new HashMap<>();
        for (final Node node : nodes) {
          for (final Role role : node.getRoles()) {
            localRoleDictionary.put(role.getId(), role);
          }
        }
      }
      return localRoleDictionary;
    }
  }

  /** Returns whether the given role id belongs to a local role.
   *
   * @param roleId the given role id
   * @return whether the given role id belongs to a local role
   */
  public boolean isLocalRole(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    synchronized (getLocalRoleDictionary()) { // force lazy initialization
      return localRoleDictionary.containsKey(roleId);
    }
  }

  /** Adds the given role to the local role dictionary.
   *
   * @param role the given role
   */
  public void addRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";
    assert role.getId() != null : "role must have been persisted, and thus have an id";

    synchronized (getLocalRoleDictionary()) { // force lazy initialization
      localRoleDictionary.put(role.getId(), role);
    }
  }

  /** Removes the given role from the local role dictionary.
   *
   * @param role the given role
   */
  public void removeRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";
    assert role.getId() != null : "role must have been persisted, and thus have an id";

    synchronized (getLocalRoleDictionary()) { // force lazy initialization
      assert localRoleDictionary.containsKey(role.getId()) : "role must be registered";
      localRoleDictionary.remove(role.getId());
    }
  }

  /** Gets the local role having the given id.
   *
   * @param roleId the role id
   * @return the local role having the given id, or null if not found
   */
  public Role getLocalRole(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    synchronized (getLocalRoleDictionary()) { // force lazy initialization
      return localRoleDictionary.get(roleId);
    }
  }

  /** Gets the top friendship role.
   *
   * @return the top friendship role
   */
  public Role getTopFriendshipRole() {
    if (topFriendshipRole == null) {
      synchronized (getLocalNodeDictionary()) { // force lazy initialization
        final Node topFriendshipNode = localNodeDictionary.get(AHCSConstants.NODE_NICKNAME_TOP_LEVEL_FRIENDSHIP_AGENT);
        assert topFriendshipNode != null;
        topFriendshipRole = topFriendshipNode.getRoleForTypeName(AHCSConstants.TOP_FRIENDSHIP_ROLE_TYPE);
      }
    }

    //Postconditions
    assert topFriendshipRole != null : "topFriendshipRole must not be null";

    return topFriendshipRole;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final Node node : nodes) {
      node.instantiate();
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

    for (final Node node : nodes) {
      node.cascadePersist(
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

    for (final Node node : nodes) {
      node.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    }
    rdfEntityManager.remove(this);
  }
}
