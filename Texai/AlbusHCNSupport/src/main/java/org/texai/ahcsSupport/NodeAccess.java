/*
 * NodeAccess.java
 *
 * Created on May 10, 2010, 7:44:57 AM
 *
 * Description: Provides access to persistent albus node objects.
 *
 * Copyright (C) May 10, 2010, Stephen L. Reed.
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
package org.texai.ahcsSupport;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration;
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.RoleType;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides access to persistent Albus node objects.
 *
 * @author reed
 */
@ThreadSafe
public final class NodeAccess {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeAccess.class);
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the role type cache, role id --> role type  */
  private final Map<URI, RoleType> roleTypeDictionary = new HashMap<>();
  /** the predicate that accesses NodeRuntimeConfiguration.nodeRuntimeId */
  private static final URI NODE_RUNTIME_CONFIGURATION_NODE_RUNTIME_ID = RDFUtility.getDefaultPropertyURI(
          NodeRuntimeConfiguration.class.getName(), // className
          "nodeRuntimeId", // fieldName
          URI.class); // fieldType

  /** Creates a new NodeAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public NodeAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "RDFEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Returns the node type objects having the given node type name, of which there should be at most one.
   *
   * @param typeName the given node type name
   * @return the node types having the given node type name
   */
  public synchronized List<NodeType> findNodeTypes(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    return rdfEntityManager.find(
            AHCSConstants.AHCS_NODE_TYPE_TYPE_NAME_TERM,
            typeName,
            NodeType.class);
  }

  /** Returns the node type having the given node type name.
   *
   * @param typeName the given node type name
   * @return the node type having the given node type name, or null if not found
   */
  public synchronized NodeType findNodeType(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    final List<NodeType> nodeTypes = findNodeTypes(typeName);

    if (nodeTypes.size() == 1) {
      return nodeTypes.get(0);
    } else if (nodeTypes.isEmpty()) {
      return null;
    } else {
      throw new TexaiException("unexpected duplicate node type: " + nodeTypes.toString());
    }
  }

  /** Returns the node type for the given node.
   *
   * @param node the given node
   * @return the node type for the given node
   */
  public synchronized NodeType getNodeType(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    return rdfEntityManager.find(NodeType.class, node.getNodeTypeURI());
  }

  /** Persists the given node type.
   *
   * @param nodeType  the given node type
   */
  public synchronized void persistNodeType(final NodeType nodeType) {
    //Preconditions
    assert nodeType != null : "nodeType must not be null";

    rdfEntityManager.persist(nodeType);
  }

  /** Returns a list of the instantiated nodes as persisted in the repository.
   *
   * @return a list of the instantiated nodes as persisted in the repository
   */
  public synchronized List<Node> getNodes() {
    final List<Node> nodes = new ArrayList<>();
    final Iterator<Node> nodes_iter = rdfEntityManager.rdfEntityIterator(Node.class);
    while (nodes_iter.hasNext()) {
      final Node node = nodes_iter.next();
      node.instantiate();
      nodes.add(node);
    }
    return nodes;
  }

  /** Returns a set of the instantiated nodes for the given node runtime id, as persisted in the repository.
   *
   * @param nodeRuntimeId the node runtime id
   * @return a set of the instantiated nodes for the given node runtime id
   */
  public synchronized Set<Node> getNodes(final URI nodeRuntimeId) {
    //Preconditions
    assert nodeRuntimeId != null : "nodeRuntimeId must not be null";

    final NodeRuntimeConfiguration nodeRuntimeConfiguration = getNodeRuntimeConfiguration(nodeRuntimeId);
    nodeRuntimeConfiguration.instantiate();
    return nodeRuntimeConfiguration.getNodes();
  }

  /** Persists the given nodes.
   *
   * @param nodes  the given nodes
   */
  public synchronized void persistNodes(final Collection<Node> nodes) {
    //Preconditions
    assert nodes != null : "nodes must not be null";
    assert !nodes.isEmpty() : "nodes must not be empty";

    for (final Node node : nodes) {
      persistNode(node);
    }
  }

  /** Persists the given node.
   *
   * @param node  the given node
   */
  public synchronized void persistNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    node.cascadePersist(
            rdfEntityManager,
            null); // overrideContext
  }

  /** Writes the node types XML file. */
  public synchronized void writeNodeTypesFile() {
    try {
      try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("data/node-types.xml"))) {
        bufferedWriter.write("<node-types>\n");
        for (final NodeType nodeType : getNodeTypes()) {
          bufferedWriter.write(nodeType.toXML(2));
        }
        bufferedWriter.write("</node-types>\n");
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns the role type objects having the given role type name, of which there should be at most one.
   *
   * @param typeName the given role type name
   * @return the role types having the given role type name
   */
  public synchronized List<RoleType> findRoleTypes(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    return rdfEntityManager.find(
            AHCSConstants.AHCS_ROLE_TYPE_TYPE_NAME_TERM,
            typeName,
            RoleType.class);
  }

  /** Returns the role type having the given role type name.
   *
   * @param typeName the given role type name
   * @return the role type having the given role type name, or null if not found
   */
  public synchronized RoleType findRoleType(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    final List<RoleType> roleTypes = findRoleTypes(typeName);

    if (roleTypes.size() == 1) {
      return roleTypes.get(0);
    } else if (roleTypes.isEmpty()) {
      return null;
    } else {
      throw new TexaiException("unexpected duplicate role type: " + roleTypes.toString());
    }
  }

  /** Returns the role type for the given role id.
   *
   * @param roleId the given role id
   * @return the role type for the given role
   */
  public synchronized RoleType getRoleType(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    synchronized (roleTypeDictionary) {
      RoleType roleType = roleTypeDictionary.get(roleId);
      if (roleType == null) {
        if (RDFUtility.getDefaultClassFromId(roleId).equals("org.texai.texailauncher.domainEntity.TexaiLauncherInfo")) {
          // the texai launcher is not a real role
          return null;
        }
        final Role role = rdfEntityManager.find(Role.class, roleId);
        if (role == null) {
          return null;
        }
        roleType = getRoleType(role);
        roleTypeDictionary.put(roleId, roleType);
      }
      return roleType;
    }
  }

  /** Returns the role type for the given role.
   *
   * @param role the given role
   * @return the role type for the given role
   */
  public synchronized RoleType getRoleType(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    return rdfEntityManager.find(RoleType.class, role.getRoleTypeURI());
  }

  /** Persists the given role type.
   *
   * @param roleType the given role type
   */
  public synchronized void persistRoleType(final RoleType roleType) {
    //Preconditions
    assert roleType != null : "roleType must not be null";

    rdfEntityManager.persist(roleType);
  }

  /** Removes the given role type.
   *
   * @param roleType the given role type
   */
  public synchronized void removeRoleType(final RoleType roleType) {
    //Preconditions
    assert roleType != null : "roleType must not be null";

    rdfEntityManager.remove(roleType);
  }

  /** Writes the role types XML file. */
  public synchronized void writeRoleTypesFile() {
    try {
      try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("data/role-types.xml"))) {
        bufferedWriter.write("<role-types>\n");
        for (final RoleType roleType : getRoleTypes()) {
          bufferedWriter.write(roleType.toXML(2));
        }
        bufferedWriter.write("</role-types>\n");
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns the direct and inherited skill uses for the given role.
   *
   * @param role the given role
   * @return the direct and inherited skill uses for the given role
   */
  public synchronized Set<SkillClass> getAllSkillClasses(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    final RoleType roleType = rdfEntityManager.find(RoleType.class, role.getRoleTypeURI());
    if (roleType == null) {
      throw new TexaiException("role type not found for role");
    }
    return roleType.getAllSkillClasses();
  }

  /** Persists the given skill class.
   *
   * @param skillClass  the given skill class
   */
  public synchronized void persistSkillClass(final SkillClass skillClass) {
    //Preconditions
    assert skillClass != null : "skillClass must not be null";

    rdfEntityManager.persist(skillClass);
  }

  /** Removes the given skill class.
   *
   * @param skillClass the given skill class
   */
  public synchronized void removeSkillClass(final SkillClass skillClass) {
    //Preconditions
    assert skillClass != null : "skillClass must not be null";

    rdfEntityManager.remove(skillClass);
  }

  /** Returns an ordered list of the node types.
   *
   * @return an ordered list of the node types
   */
  public synchronized List<NodeType> getNodeTypes() {
    final List<NodeType> nodeTypes = new ArrayList<>();
    final Iterator<NodeType> nodeTypes_iter = rdfEntityManager.rdfEntityIterator(NodeType.class);
    while (nodeTypes_iter.hasNext()) {
      nodeTypes.add(nodeTypes_iter.next());
    }
    Collections.sort(nodeTypes);
    return nodeTypes;
  }

  /** Returns an ordered list of the role types.
   *
   * @return an ordered list of the role types
   */
  public synchronized List<RoleType> getRoleTypes() {
    final List<RoleType> roleTypes = new ArrayList<>();
    final Iterator<RoleType> roleTypes_iter = rdfEntityManager.rdfEntityIterator(RoleType.class);
    while (roleTypes_iter.hasNext()) {
      roleTypes.add(roleTypes_iter.next());
    }
    Collections.sort(roleTypes);
    return roleTypes;
  }

  /** Persists the given role.
   *
   * @param role the given role
   */
  public synchronized void persistRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    rdfEntityManager.persist(role);
  }

  /** Retrieves the node runtime configuration for the given id.
   *
   * @param nodeRuntimeId the node runtime id
   * @return the node runtime configuration for the given id
   */
  public synchronized NodeRuntimeConfiguration getNodeRuntimeConfiguration(final URI nodeRuntimeId) {
    //Preconditions
    assert nodeRuntimeId != null : "nodeRuntimeId must not be null";

    final List<NodeRuntimeConfiguration> results = rdfEntityManager.find(
            NODE_RUNTIME_CONFIGURATION_NODE_RUNTIME_ID, //predicate
            nodeRuntimeId, // rdfValue
            NodeRuntimeConfiguration.class); // clazz
    if (results.isEmpty()) {
      LOGGER.info("node runtime configuration for " + nodeRuntimeId + " was not found, existing configurations are ...");
      final Iterator<NodeRuntimeConfiguration> nodeRuntimeConfiguration_iter = rdfEntityManager.rdfEntityIterator(NodeRuntimeConfiguration.class);
      while (nodeRuntimeConfiguration_iter.hasNext()) {
        LOGGER.info("  " + nodeRuntimeConfiguration_iter.next());
      }
      return null;
    } else if (results.size() == 1) {
      return results.get(0);
    } else {
      throw new TexaiException("non-unique nodeRuntimeId fields in " + results);
    }
  }

  /** Persists the node runtime configuration.
   *
   * @param nodeRuntimeConfiguration the node runtime configuration, including nodes, roles and state values
   */
  public synchronized void persistNodeRuntimeConfiguration(final NodeRuntimeConfiguration nodeRuntimeConfiguration) {
    //Preconditions
    assert nodeRuntimeConfiguration != null : "nodeRuntimeConfiguration must not be null";

    nodeRuntimeConfiguration.cascadePersist(
            rdfEntityManager,
            null); // overrideContext
  }

    /** Creates a node type.
   *
   * @param typeName the node type name
   * @param inheritedNodeTypes the inherited node types
   * @param roleTypes the node role types
   * @param missionDescription the node's mission described in English
   * @return the persisted node type
   */
  public NodeType createNodeType(
          final String typeName,
          final Collection<NodeType> inheritedNodeTypes,
          final Collection<RoleType> roleTypes,
          final String missionDescription) {
    //Preconditions
    assert typeName != null : "nodeTypeName must not be null";
    assert !typeName.isEmpty() : "nodeTypeName must not be empty";
    assert inheritedNodeTypes != null : "inheritedNodeTypes must not be null";
    assert roleTypes != null : "roleTypes must not be null";
    assert missionDescription != null : "missionDescription must not be null";
    assert !missionDescription.isEmpty() : "missionDescription must not be empty";

    final NodeType nodeType = new NodeType(
          typeName,
          inheritedNodeTypes,
          roleTypes,
          missionDescription);
    persistNodeType(nodeType);
    return nodeType;
  }

  /** Creates a node of the given type.
   *
   * @param nodeTypeName the node type name
   * @param nodeNickname the node nickname
   * @param nodeRuntime the node runtime
   * @return a node of the given type
   */
  public static Node createNode(
          final String nodeTypeName,
          final String nodeNickname,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeTypeName != null : "nodeTypeName must not be null";
    assert !nodeTypeName.isEmpty() : "nodeTypeName must not be empty";
    assert nodeNickname != null : "nodeNickname must not be null";
    assert !nodeNickname.isEmpty() : "nodeNickname must not be empty";
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert nodeRuntime.getNode(nodeNickname) == null : "node must not already exist: " + nodeNickname;

    // create the node of the given type and nickname
    final NodeAccess nodeAccess = nodeRuntime.getNodeAccess();
    final NodeType nodeType = nodeAccess.findNodeType(nodeTypeName);
    final Node node = new Node(nodeType, nodeRuntime);
    node.setNodeNickname(nodeNickname);
    nodeRuntime.addNode(node);

    // create its roles
    node.installRoles(nodeAccess);

    //Postconditions
    assert nodeRuntime.getNode(nodeNickname) != null : "node runtime node dictionary lookup failed for " + nodeNickname;

    return node;
  }

  /** Creates a role type.
   *
   * @param typeName the role type name
   * @param inheritedRoleTypes the inherited role types
   * @param skillUses the skill uses
   * @param description the role's description in English
   * @param albusHCSGranularityLevel the Albus hierarchical control system granularity level
   * @return the persisted node type
   */
  public RoleType createRoleType(
          final String typeName,
          final Collection<RoleType> inheritedRoleTypes,
          final Collection<SkillClass> skillUses,
          final String description,
          final URI albusHCSGranularityLevel) {
    //Preconditions
    assert typeName != null : "nodeTypeName must not be null";
    assert !typeName.isEmpty() : "nodeTypeName must not be empty";
    assert inheritedRoleTypes != null : "inheritedRoleTypes must not be null";
    assert skillUses != null : "skillUses must not be null";
    assert description != null : "description must not be null";
    assert !description.isEmpty() : "description must not be empty";

    final RoleType roleType = new RoleType(
          typeName,
          inheritedRoleTypes,
          skillUses,
          description,
          albusHCSGranularityLevel);
    persistRoleType(roleType);
    return roleType;
  }

  /** Creates a skill use.
   *
   * @param skillClassName the skill class name
   * @return the persisted skill use
   */
  public SkillClass createSkillUse(final String skillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(skillClassName) : "skillClassName must be a non-empty string";

    final SkillClass skillClass = new SkillClass(skillClassName);
    persistSkillClass(skillClass);
    return skillClass;
  }

  /** Returns the skill class having the given fully qualified name.
   *
   * @param skillClassName the given fully qualified name
   * @return the skill class having the given fully qualified name, or null if not found
   */
  public synchronized SkillClass findSkillClass(final String skillClassName) {
    //Preconditions
    assert skillClassName != null : "skillClassName must not be null";
    assert !skillClassName.isEmpty() : "skillClassName must not be empty";

    final List<SkillClass> skillClasses = rdfEntityManager.find(
            AHCSConstants.SKILL_CLASS_SKILL_CLASS_NAME_TERM,
            skillClassName,
            SkillClass.class);

    if (skillClasses.size() == 1) {
      return skillClasses.get(0);
    } else if (skillClasses.isEmpty()) {
      return null;
    } else {
      throw new TexaiException("unexpected duplicate skill class: " + skillClasses.toString());
    }
  }

  /** Connects the given child role to the given parent role.
   *
   * @param childRoleId the id of the child role
   * @param parentRoleId the id of the parent role
   * @param nodeRuntime the node runtime
   */
  public static void connectChildRoleToParent(
          final URI childRoleId,
          final URI parentRoleId,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert childRoleId != null : "nodeTypeName must not be null";
    assert parentRoleId != null : "nodeTypeName must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    // find the child and parent roles
    final Role childRole = nodeRuntime.getLocalRole(childRoleId);
    if (childRole == null) {
      throw new TexaiException("remote role connection not yet implemented - child role not local");
    }
    final Role parentRole = nodeRuntime.getLocalRole(parentRoleId);
    if (parentRole == null) {
      throw new TexaiException("remote role connection not yet implemented - parent role not local");
    }
    LOGGER.info("connecting child " + childRole + " to parent " + parentRole);

    // connect them
    assert childRole.getParentRoleId() == null : "child role must not already have a parent: " + childRole.toString();
    childRole.setParentRoleIdString(parentRole.getId().toString());
    assert !parentRole.getChildRoleIdStrings().contains(childRole.getId().toString()) :
            "parent role " + parentRole
            + " must not already be connected to child role: "
            + childRole.toString()
            + "\n" + displayChildRoles(parentRole, nodeRuntime);
    parentRole.addChildRole(childRole.getId().toString());

    // persist them
    final NodeAccess nodeAccess = nodeRuntime.getNodeAccess();
    nodeAccess.persistRole(childRole);
    nodeAccess.persistRole(parentRole);
  }

  /** Returns a string representation of the child roles of the given role.
   *
   * @param role the given role
   * @param nodeRuntime the node runtime
   * @return a string representation of the child roles
   */
  public static String displayChildRoles(
          final Role role,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert role != null : "role must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("child roles of ").append(role).append("...");
    for (final String childIdString : role.getChildRoleIdStrings()) {
      final Role childRole = nodeRuntime.getLocalRole(new URIImpl(childIdString));
      if (childRole == null) {
        stringBuilder.append("\n  not-found");
      } else {
        stringBuilder.append("\n  ").append(childRole);
      }
    }
    return stringBuilder.toString();
  }

  /** Connects the universal roles: HeartbeatRole, NodeLifeCycleRole and GovernanceRole to their respective parent roles.
   *
   * @param childNode the node containing the HeartbeatRole, NodeLifeCycleRole and GovernanceRole, to be connected to their
   * respective parent roles
   * @param parentHeartbeatNode the node containing the parent of the HeartbeatRole
   * @param liferNode the node containing the parent of the NodeLifeCycleRole
   * @param governorNode the node containing the parent of the GovernanceRole
   * @param jvmLoggerNode the node containing the parent of the NodeLoggerRole
   * @param nodeRuntime the node runtime
   */
  public static void connectUniversalRoles(
          final Node childNode,
          final Node parentHeartbeatNode,
          final Node liferNode,
          final Node governorNode,
          final Node jvmLoggerNode,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert childNode != null : "childNode must not be null";
    assert parentHeartbeatNode != null : "parentHeartbeatNode must not be null";
    assert liferNode != null : "liferNode must not be null";
    assert governorNode != null : "governorNode must not be null";
    assert jvmLoggerNode != null : "jvmLoggerNode must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";


    LOGGER.info("connecting universal roles for " + childNode);
    if (childNode.isTopFriendshipNode()) {
      final Role childHeartbeatRole = childNode.getRoleForTypeName(AHCSConstants.HEARTBEAT_ROLE_TYPE);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  childHeartbeatRole: " + childHeartbeatRole + ", id: " + childHeartbeatRole.getId());
      }
      final Role topFriendshipRole = childNode.getRoleForTypeName(AHCSConstants.TOP_FRIENDSHIP_ROLE_TYPE);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  topFriendshipRole: " + topFriendshipRole + ", id: " + topFriendshipRole.getId());
      }
      connectChildRoleToParent(
              childHeartbeatRole.getId(), // childRoleId
              topFriendshipRole.getId(), // parentRoleId
              nodeRuntime);
    } else {
      final Role childHeartbeatRole = childNode.getRoleForTypeName(AHCSConstants.HEARTBEAT_ROLE_TYPE);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  childHeartbeatRole: " + childHeartbeatRole + ", id: " + childHeartbeatRole.getId());
      }
      final Role parentHeartbeatRole = parentHeartbeatNode.getRoleForTypeName(AHCSConstants.HEARTBEAT_ROLE_TYPE);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  parentHeartbeatRole: " + parentHeartbeatRole + ", id: " + parentHeartbeatRole.getId());
      }
      connectChildRoleToParent(
              childHeartbeatRole.getId(), // childRoleId
              parentHeartbeatRole.getId(), // parentRoleId
              nodeRuntime);
    }

    final Role childNodeLifeCycleRole = childNode.getRoleForTypeName(AHCSConstants.NODE_LIFE_CYCLE_ROLE_TYPE);
    assert childNodeLifeCycleRole != null : childNode.getRoles();
    final Role lifeCycleManagmentRole = liferNode.getRoleForTypeName(AHCSConstants.LIFE_CYCLE_MANAGEMENT_ROLE_TYPE);
    assert lifeCycleManagmentRole != null : liferNode.getRoles();
    connectChildRoleToParent(
            childNodeLifeCycleRole.getId(), // childRoleId
            lifeCycleManagmentRole.getId(), // parentRoleId
            nodeRuntime);

    final Role childGovernanceRole = childNode.getRoleForTypeName(AHCSConstants.GOVERNANCE_ROLE_TYPE);
    assert childGovernanceRole != null : childNode.getRoles();
    final Role parentGovernanceManagmentRole = governorNode.getRoleForTypeName(AHCSConstants.GOVERNANCE_MANAGEMENT_ROLE_TYPE);
    assert parentGovernanceManagmentRole != null : governorNode.getRoles();
    connectChildRoleToParent(
            childGovernanceRole.getId(), // childRoleId
            parentGovernanceManagmentRole.getId(), // parentRoleId
            nodeRuntime);

    final Role childNodeLoggerRole = childNode.getRoleForTypeName(AHCSConstants.NODE_LOGGER_ROLE_TYPE);
    assert childNodeLoggerRole != null : childNode.getRoles();
    final Role parentJVMLoggerManagmentRole = jvmLoggerNode.getRoleForTypeName(AHCSConstants.JVM_LOGGER_MANAGEMENT_ROLE_TYPE);
    assert parentJVMLoggerManagmentRole != null : jvmLoggerNode.getRoles();
    connectChildRoleToParent(
            childNodeLoggerRole.getId(), // childRoleId
            parentJVMLoggerManagmentRole.getId(), // parentRoleId
            nodeRuntime);
  }

  /** Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  public RDFEntityManager getRDFEntityManager() {
    return rdfEntityManager;
  }
}
