/*
 * SkillClass.java
 *
 * Created on Jun 29, 2010, 12:05:44 PM
 *
 * Description: Contains information about a skill class use in the Albus hierarchical control system.
 *
 * Copyright (C) Jun 29, 2010, Stephen L. Reed.
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

import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;

/**
 * Contains information about a skill class use in the Albus hierarchical control system.
 *
 * @author reed
 */
@Immutable
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class SkillClass implements CascadePersistence, Comparable<SkillClass> {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  private URI id;    // NOPMD
  // the skill class name
  @RDFProperty
  private final String skillClassName;

  /**
   * Constructs a new SkillClass instance.
   */
  public SkillClass() {
    skillClassName = null;
  }

  /**
   * Constructs a new SkillClass instance.
   *
   * @param skillClassName the skill class name
   */
  public SkillClass(final String skillClassName) {
    //Preconditions
    assert isValidSkillClassName(skillClassName) : "skillClassName must be a valid class name";

    this.skillClassName = skillClassName;
  }

  /**
   * Returns whether the given skill class name actually names a valid class.
   *
   * @param skillClassName the given skill class name
   *
   * @return whether the given skill class name actually names a valid class
   */
  public static boolean isValidSkillClassName(final String skillClassName) {
    if (!StringUtils.isNonEmptyString(skillClassName)
            || !StringUtils.isJavaClassName(skillClassName)) {
      return false;
    }
    try {
      Class.forName(skillClassName);
    } catch (ClassNotFoundException ex) {
      return false;
    }
    return true;
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
   * Gets the skill class name.
   *
   * @return the skill class name
   */
  public String getSkillClassName() {
    return skillClassName;
  }

  /**
   * Gets the package name, e.g. org.texai.skill.dialog.bl .
   *
   * @return the package name
   */
  public String getPackageName() {
    final int index = skillClassName.lastIndexOf(".");
    assert index > 1;
    return skillClassName.substring(0, index);
  }

  /**
   * Gets the unqualified class name.
   *
   * @return the unqualified class name
   */
  public String getName() {
    final int index = skillClassName.lastIndexOf(".");
    assert index > 1;
    return skillClassName.substring(index + 1);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[" + skillClassName + "]";
  }

  /**
   * Returns whether some other object equals this one.
   *
   * @param obj the other object
   *
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
    final SkillClass other = (SkillClass) obj;
    return !((this.skillClassName == null) ? (other.skillClassName != null) : !this.skillClassName.equals(other.skillClassName));
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 61 * hash + (this.skillClassName != null ? this.skillClassName.hashCode() : 0);
    return hash;
  }

  /**
   * Returns an XML representation of this object.
   *
   * @param indent the indentation amount
   *
   * @return an XML representation of this object
   */
  public String toXML(final int indent) {
    //Preconditions
    assert indent >= 0 : "indent must not be negative";

    final StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
    stringBuilder.append("<skill-class>\n");

    if (skillClassName != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <skill-class-name>");
      stringBuilder.append(skillClassName);
      stringBuilder.append("</skill-class-name>\n");
    }

    if (id != null) {
      for (int i = 0; i < indent; i++) {
        stringBuilder.append(' ');
      }
      stringBuilder.append("  <id>");
      stringBuilder.append(id);
      stringBuilder.append("</id>\n");
    }

    for (int i = 0; i < indent; i++) {
      stringBuilder.append(' ');
    }
    stringBuilder.append("</skill-class>\n");
    return stringBuilder.toString();
  }

  /**
   * Compares this object with another.
   *
   * @param that the other object
   *
   * @return -1 if less than, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final SkillClass that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.skillClassName.compareTo(that.skillClassName);
  }

  /**
   * Ensures that this persistent object is fully instantiated.
   */
  @Override
  public void instantiate() {
  }

  /**
   * Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.persist(this, overrideContext);
  }

  /**
   * Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.remove(this);
  }
}
