/*
 * RoleType.java
 *
 * Created on May 6, 2010, 11:25:44 AM
 *
 * Description:  Contains information about a role type in the Albus hierarchical control system.
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
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Contains information about a role type in the Albus hierarchical control system.
 *
 * @author reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class RoleType implements RDFPersistent, Comparable<RoleType> {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the role type name */
  @RDFProperty(predicate = "texai:ahcsRoleType_typeName")
  private String typeName;
  /** the inherited role types */
  @RDFProperty(predicate = "texai:ahcsRoleType_inheritedRoleType")
  private final Set<RoleType> inheritedRoleTypes = new HashSet<>();
  /** the skill uses */
  @RDFProperty(predicate = "texai:ahcsRoleType_skillClassName")
  private final Set<SkillClass> skilClasses = new HashSet<>();
  /** the role's description in English */
  @RDFProperty(predicate = "texai:ahcsRoleType_description")
  private String description;
  /** the Albus hierarchical control system granularity level */
  @RDFProperty(predicate = "texai:ahcsRoleType_albusHCSGranularityLevel")
  private URI albusHCSGranularityLevel;

  /** Constructs a new RoleType instance. */
  public RoleType() {
  }

  /** Constructs a new RoleType instance.
   *
   * @param typeName the role type name
   * @param inheritedRoleTypes the inherited role types
   * @param skillUses the skill uses
   * @param description the role's description in English
   * @param albusHCSGranularityLevel the Albus hierarchical control system granularity level
   */
  public RoleType(
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

    this.typeName = typeName;
    this.inheritedRoleTypes.addAll(inheritedRoleTypes);
    this.skilClasses.addAll(skillUses);
    this.description = description;
    this.albusHCSGranularityLevel = albusHCSGranularityLevel;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets an unmodifiable copy of the inherited role types.
   *
   * @return the inherited role types
   */
  public Set<RoleType> getInheritedRoleTypes() {
    return Collections.unmodifiableSet(inheritedRoleTypes);
  }

  /** Adds the given inherited role type.
   *
   * @param inheritedRoleType the inherited role type to add
   */
  public void addInheritedRoleType(final RoleType inheritedRoleType) {
    //Preconditions
    assert inheritedRoleType != null : "inheritedRoleType must not be null";

    synchronized (inheritedRoleTypes) {
      inheritedRoleTypes.add(inheritedRoleType);
    }
  }

  /** Removes the given inherited role type.
   *
   * @param inheritedRoleType the inherited role type to remove
   */
  public void removeInheritedRoleType(final RoleType inheritedRoleType) {
    //Preconditions
    assert inheritedRoleType != null : "inheritedRoleType must not be null";

    synchronized (inheritedRoleTypes) {
      inheritedRoleTypes.remove(inheritedRoleType);
    }
  }

  /** Clears the inherited role types. */
  public void clearInheritedRoleTypes() {
    synchronized (inheritedRoleTypes) {
      inheritedRoleTypes.clear();
    }
  }

  /** Gets an unmodifiable copy of the skill uses.
   *
   * @return the skill uses
   */
  public Set<SkillClass> getSkillUses() {
    return Collections.unmodifiableSet(skilClasses);
    }

  /** Adds the given skill use.
   *
   * @param skillUse the skill use to add
   */
  public void addSkillUse(final SkillClass skillUse) {
    //Preconditions
    assert skillUse != null : "skillUse must not be null";

    synchronized (skilClasses) {
      skilClasses.add(skillUse);
    }
  }

  /** Removes the given skill use.
   *
   * @param skillUse the skill use to remove
   */
  public void removeSkillUse(final SkillClass skillUse) {
    //Preconditions
    assert skillUse != null : "skillClassName skillUse not be null";

    synchronized (skilClasses) {
      skilClasses.remove(skillUse);
    }
  }

  /** Clears the skill uses. */
  public void clearSkillUses() {
    synchronized (skilClasses) {
      skilClasses.clear();
    }
  }

  /** Gets an unmodifiable copy of the direct and inherited skill uses.
   *
   * @return an unmodifiable copy of the direct and inherited skill uses
   */
  public Set<SkillClass> getAllSkillClasses() {
    final Set<SkillClass> allSkillUses = new HashSet<>();
    allSkillUses.addAll(skilClasses);
    for (final RoleType inheritedRoleType : inheritedRoleTypes) {
      allSkillUses.addAll(inheritedRoleType.getAllSkillClasses());
    }
    return Collections.unmodifiableSet(allSkillUses);
  }

  /**Gets the role's description in English.
   *
   * @return the role's description in English
   */
  public synchronized String getDescription() {
    return description;
  }

  /**Sets the role's description in English.
   *
   * @param description the role's description in English
   */
  public synchronized void setDescription(final String description) {
    //Preconditions
    assert description != null : "description must not be null";
    assert !description.isEmpty() : "description must not be empty";

    this.description = description;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj whether some other object equals this one
   * @return
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RoleType other = (RoleType) obj;
    if ((this.typeName == null) ? (other.typeName != null) : !this.typeName.equals(other.typeName)) {
      return false;
    }
    if (this.skilClasses != other.skilClasses && (!this.skilClasses.equals(other.skilClasses))) {
      return false;
    }
    if ((this.description == null) ? (other.description != null) : !this.description.equals(other.description)) {
      return false;
    }
    return this.albusHCSGranularityLevel == other.albusHCSGranularityLevel || (this.albusHCSGranularityLevel != null && this.albusHCSGranularityLevel.equals(other.albusHCSGranularityLevel));
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return 7 + (typeName != null ? typeName.hashCode() : 0);
  }

  /** Gets the role type name.
   *
   * @return the role type name
   */
  public String getTypeName() {
    return typeName;
  }

  /** Sets the role type name.
   *
   * @param typeName the role type name
   */
  public void setTypeName(final String typeName) {
    //Preconditions
    assert typeName != null : "typeName must not be null";
    assert !typeName.isEmpty() : "typeName must not be empty";

    this.typeName = typeName;
  }

  /** Gets the Albus hierarchical control system granularity level.
   *
   * @return the Albus hierarchical control system granularity level
   */
  public URI getAlbusHCSGranularityLevel() {
    return albusHCSGranularityLevel;
  }

  /** Sets the Albus hierarchical control system granularity level.
   *
   * @param albusHCSGranularityLevel the Albus hierarchical control system granularity level
   */
  public void setAlbusHCSGranularityLevel(final URI albusHCSGranularityLevel) {
    //Preconditions
    assert albusHCSGranularityLevel != null : "albusHCSGranularityLevel must not be null";

    this.albusHCSGranularityLevel = albusHCSGranularityLevel;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    if (typeName == null) {
      return "[RoleType]";
    } else {
      return "[RoleType " + typeName + "]";
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
    stringBuilder.append("<role-type>\n");

    if (typeName != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <name>");
      stringBuilder.append(typeName);
      stringBuilder.append("</name>\n");
    }

    if (description != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <description>");
      stringBuilder.append(description);
      stringBuilder.append("</description>\n");
    }

    if (id != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <id>");
      stringBuilder.append(id);
      stringBuilder.append("</id>\n");
    }

    if (!skilClasses.isEmpty()) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <skill-classes>\n");
      for (final SkillClass skillClass : skilClasses) {
        stringBuilder.append(skillClass.toXML(indent + 4));
      }
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  </skill-classes>\n");
    }

    if (albusHCSGranularityLevel != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <granularity-level>");
      stringBuilder.append(albusHCSGranularityLevel.getLocalName());
      stringBuilder.append("</granularity-level>\n");
    }

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
    stringBuilder.append("</role-type>\n");
    return stringBuilder.toString();
  }

  /** Compares some other role type with this one.
   *
   * @param that the other role type
   * @return -1 if the other role type is less than this one, return 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final RoleType that) {
    //Preconditions
    assert that != null : "that must not be null";

    if (this.typeName == null || that.typeName == null) {
      return this.id.toString().compareTo(that.id.toString());
    } else {
      return this.typeName.compareTo(that.typeName);
    }
  }
}
