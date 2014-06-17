/*
 * Agent.java
 *
 * Created on August 16, 2007, 3:50 PM
 *
 * Description: Provides a class to represent an agent in the FOAF ontology.
 *
 * Copyright (C) August 16, 2007 Stephen L. Reed.
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
package org.texai.kb.persistence.sample;

import java.util.Set;
import javax.persistence.Id;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a class to represent an agent in the FOAF ontology.
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
  @RDFNamespace(prefix = "foaf", namespaceURI = Constants.FOAF_NAMESPACE)
}, subject = "texai:org.texai.kb.persistence.sample.Agent")
public class Agent implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** This is the URI that identifies this person.  If null, then it is automatically generated when
   * this instance is persisted.  When a subclass is instantiated, this field is ignored. */
  @Id
  private URI id;    // NOPMD
  /** the name of this agent  */
  @RDFProperty(predicate = "foaf:name")
  private String name;
  /** the birthday of this Agent, represented in mm-dd string form, eg. '12-31'  */
  @RDFProperty(predicate = "foaf:birthday")
  private String birthday;
  /** the gender of this Agent (typically but not necessarily 'male' or 'female')  */
  @RDFProperty(predicate = "foaf:gender")
  private String gender;
  /** the set of some things that were made by this agent  */
  @RDFProperty(predicate = "foaf:made")
  private Set<Object> thingsMade;

  /** Creates a new instance of Agent. */
  public Agent() {
  }

  /** Gets the URI that identifies this person.
   *
   * @return the URI that identifies this person
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the name of this agent.
   *
   * @return the name of this agent
   */
  public String getName() {
    return name;
  }

  /** Sets the name of this agent.
   *
   * @param name the name of this agent
   */
  public void setName(final String name) {
    this.name = name;
  }

  /** Gets the birthday of this Agent.
   *
   * @return the birthday of this Agent
   */
  public String getBirthday() {
    return birthday;
  }

  /** Sets the birthday of this Agent.
   *
   * @param birthday the birthday of this Agent
   */
  public void setBirthday(final String birthday) {
    this.birthday = birthday;
  }

  /** Gets the gender of this Agent.
   *
   * @return the gender of this Agent
   */
  public String getGender() {
    return gender;
  }

  /** Sets the gender of this Agent.
   *
   * @param gender the gender of this Agent
   */
  public void setGender(final String gender) {
    this.gender = gender;
  }

  /** Gets the set of some things that were made by this agent.
   *
   * @return the set of some things that were made by this agent
   */
  public Set<Object> getThingsMade() {
    return thingsMade;
  }

  /** Sets the set of some things that were made by this agent.
   *
   * @param thingsMade the set of some things that were made by this agent
   */
  public void setThingsMade(final Set<Object> thingsMade) {
    this.thingsMade = thingsMade;
  }

  /** Returns whether the given object is equal to this object.
   *
   * @param obj the given object
   * @return whether the given object is equal to this object
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Agent) {
      final Agent that = (Agent) obj;
      return this.getId().equals(that.getId());
    } else {
      return false;
    }
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    if (getId() == null) {
      return super.hashCode();
    } else {
      return getId().hashCode();
    }
  }
}
