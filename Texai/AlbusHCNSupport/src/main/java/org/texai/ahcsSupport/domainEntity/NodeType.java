/*
 * NodeType.java
 *
 * Created on May 6, 2010, 11:25:36 AM
 *
 * Description: Contains information about a node type in the Albus hierarcical control system.
 *
 * Copyright (C) May 6, 2010, Stephen L. Reed.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Contains information about a node type in the Albus hierarchical control system.
 *
 * @author reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class NodeType implements RDFPersistent, Comparable<NodeType> {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the node type name */
  @RDFProperty(predicate = "texai:ahcsNodeType_typeName")
  private String typeName;
  /** the inherited node types */
  @RDFProperty(predicate = "texai:ahcsNodeType_inheritedNodeType")
  private final Set<NodeType> inheritedNodeTypes = new HashSet<>();
  /** the node role types */
  @RDFProperty(predicate = "texai:ahcsNodeType_roleType")
  private final Set<RoleType> roleTypes = new HashSet<>();
  /** the node's mission described in English */
  @RDFProperty(predicate = "texai:ahcsNodeType_missionDescription")
  private String missionDescription;

  /** Constructs a new NodeType instance. */
  public NodeType() {
  }

  /** Constructs a new NodeType instance.
   *
   * @param typeName the node type name
   * @param inheritedNodeTypes the inherited node types
   * @param roleTypes the node role types
   * @param missionDescription the node's mission described in English
   */
  public NodeType(
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

    this.typeName = typeName;
    this.inheritedNodeTypes.addAll(inheritedNodeTypes);
    this.roleTypes.addAll(roleTypes);
    this.missionDescription = missionDescription;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the node type name.
   *
   * @return the node type name
   */
  public String getTypeName() {
    return typeName;
  }

  /** Sets the node type name.
   *
   * @param typeName the node type name
   */
  public void setTypeName(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    this.typeName = typeName;
  }

  /** Gets an unmodifiable copy of the inherited node types.
   *
   * @return the inherited node types
   */
  public Set<NodeType> getInheritedNodeTypes() {
    return Collections.unmodifiableSet(inheritedNodeTypes);
  }

  /** Adds the given inherited node type.
   *
   * @param inheritedNodeType the inherited node type to add
   */
  public void addInheritedNodeType(final NodeType inheritedNodeType) {
    //Preconditions
    assert inheritedNodeType != null : "inheritedNodeType must not be null";

    synchronized (inheritedNodeTypes) {
      inheritedNodeTypes.add(inheritedNodeType);
    }
  }

  /** Removes the given inherited node type.
   *
   * @param inheritedNodeType the inherited node type to remove
   */
  public void removeInheritedNodeType(final NodeType inheritedNodeType) {
    //Preconditions
    assert inheritedNodeType != null : "inheritedNodeType must not be null";

    synchronized (inheritedNodeTypes) {
      inheritedNodeTypes.remove(inheritedNodeType);
    }
  }

  /** Clears the inherited node types. */
  public void clearInheritedNodeTypes() {
    synchronized (inheritedNodeTypes) {
      inheritedNodeTypes.clear();
    }
  }

  /** Gets an unmodifiable copy of the node role types.
   *
   * @return the node role types
   */
  public Set<RoleType> getRoleTypes() {
    return Collections.unmodifiableSet(roleTypes);
  }

  /** Adds the given node role type.
   *
   * @param roleType the node role type to add
   */
  public void addRoleType(final RoleType roleType) {
    //Preconditions
    assert roleType != null : "roleType must not be null";

    synchronized (roleTypes) {
      roleTypes.add(roleType);
    }
  }

  /** Removes the given node role type.
   *
   * @param roleType the node role type to remove
   */
  public void removeRoleType(final RoleType roleType) {
    //Preconditions
    assert roleType != null : "roleType must not be null";

    synchronized (roleTypes) {
      roleTypes.remove(roleType);
    }
  }

  /** Clears the node role types. */
  public void clearRoleTypes() {
    synchronized (roleTypes) {
      roleTypes.clear();
    }
  }

  /** Gets an unmodifiable copy of the node role types and inherited role types.
   *
   * @return the node role types and inherited role types
   */
  public Set<RoleType> getAllRoleTypes() {
    final Set<RoleType> allRoleTypes = new HashSet<>(getRoleTypes());
    final LinkedList<NodeType> allInheritedNodeTypes = new LinkedList<>(inheritedNodeTypes);
    while (!allInheritedNodeTypes.isEmpty()) {
      final NodeType inheritedNodeType = allInheritedNodeTypes.removeFirst();
      allInheritedNodeTypes.addAll(inheritedNodeType.getInheritedNodeTypes());
      allRoleTypes.addAll(inheritedNodeType.getRoleTypes());
    }
    return Collections.unmodifiableSet(allRoleTypes);
  }

  /** Gets the node's mission described in English.
   *
   * @return the node's mission described in English
   */
  public synchronized String getMissionDescription() {
    return missionDescription;
  }

  /** Sets the node's mission described in English.
   *
   * @param missionDescription the node's mission described in English
   */
  public synchronized void setMissionDescription(final String missionDescription) {
    //Preconditions
    assert missionDescription != null : "missionDescription must not be null";
    assert !missionDescription.isEmpty() : "missionDescription must not be empty";

    this.missionDescription = missionDescription;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NodeType other = (NodeType) obj;
    if ((this.typeName == null) ? (other.typeName != null) : !this.typeName.equals(other.typeName)) {
      return false;
    }
    if (this.inheritedNodeTypes != other.inheritedNodeTypes && (!this.inheritedNodeTypes.equals(other.inheritedNodeTypes))) {
      return false;
    }
    if (this.roleTypes != other.roleTypes && (!this.roleTypes.equals(other.roleTypes))) {
      return false;
    }
    return !((this.missionDescription == null) ? (other.missionDescription != null) : !this.missionDescription.equals(other.missionDescription));
  }

  /** Returns whether another object equals this one.
   *
   * @param obj the other object
   * @return whether another object equals this one
   */
  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public synchronized int hashCode() {
    return 3 + (typeName != null ? typeName.hashCode() : 0);
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    if (typeName == null) {
      return "[NodeType]";
    } else {
      return "[NodeType " + typeName + "]";
    }
  }

  /** Returns an XML representation of this object.
   *
   * @return an XML representation of this object
   */
  public String toXML() {
    return toXML(0);
  }

  /** Returns an XML representation of this object.
   *
   * @param indent the indentation amount
   * @return an XML representation of this object
   */
  public String toXML(final int indent) {
    //Preconditions
    assert indent >= 0 : "indent must not be negative";

    final StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
    stringBuilder.append("<node-type>\n");
    if (typeName != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <name>");
      stringBuilder.append(typeName);
      stringBuilder.append("</name>\n");
    }
    if (missionDescription != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <mission>");
      stringBuilder.append(missionDescription);
      stringBuilder.append("</mission>\n");
    }
    if (id != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <id>");
      stringBuilder.append(id);
      stringBuilder.append("</id>\n");
    }
    if (!inheritedNodeTypes.isEmpty()) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <inherited-node-type-names>\n");
      for (final NodeType inheritedNodeType : inheritedNodeTypes) {
        if (inheritedNodeType.getTypeName() != null) {
          for (int i = 0; i < indent; i++) {
            stringBuilder.append(' ');
          }
          stringBuilder.append("    <inherited-node-type-name>");
          stringBuilder.append(inheritedNodeType.getTypeName());
          stringBuilder.append("</inherited-node-type-name>\n");
        }
      }
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  </inherited-node-type-names>\n");
    }
    if (!roleTypes.isEmpty()) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <role-type-names>\n");
      for (final RoleType roleType : roleTypes) {
        if (roleType.getTypeName() != null) {
          for (int i = 0; i < indent; i++) {
            stringBuilder.append(' ');
          }
          stringBuilder.append("    <role-type-name>");
          stringBuilder.append(roleType.getTypeName());
          stringBuilder.append("</role-type-name>\n");
        }
      }
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  </role-type-names>\n");
    }

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
    stringBuilder.append("</node-type>\n");
    return stringBuilder.toString();
  }

  /** Compares some other node type with this one.
   *
   * @param that the other node type
   * @return -1 if the other node type is less than this one, return 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final NodeType that) {
    //Preconditions
    assert that != null : "that must not be null";

    if (this.typeName == null || that.typeName == null) {
      return this.id.toString().compareTo(that.id.toString());
    } else {
      return this.typeName.compareTo(that.typeName);
    }
  }
}
