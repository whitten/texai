/*
 * Friend.java
 *
 * Created on December 19, 2006, 4:19 PM
 *
 * Description: Extends the FOAF ontology to represent a friend.
 *
 * Copyright (C) 2006 Stephen L. Reed.
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

import java.util.Date;
import java.util.Set;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFProperty;

/** Extends the FOAF ontology to represent a friend.
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
  @RDFNamespace(prefix = "foaf", namespaceURI = Constants.FOAF_NAMESPACE)
}, subject = "texai:org.texai.kb.persistence.sample.Friend", subClassOf = "foaf:Person", context = "texai:FriendContext")
// RDF entity classes must not have the final modifier.
public class Friend extends Person {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** extends the FOAF ontology to provide date of birth */
  @RDFProperty(predicate = "texai:dateOfBirth", subPropertyOf = "texai:hasAttribute", range = "foaf:Person")
  private Date dateOfBirth;

  /**
   * Creates a new Friend instance. RDF entities are required to have
   * a no-parameter constructor among their constructors.
   */
  public Friend() {
  }

  /** Creates a new Friend instance. RDF entities are required to have
   * a no-parameter constructor among their constructors.
   * @param name the name
   * @param dateOfBirth the date of birth
   * @param birthday the birthday
   * @param familyName the family name
   * @param gender the gender
   * @param firstName the first name
   * @param thingsMade some things made by this agent
   */
  public Friend(
          final String name,
          final String birthday,
          final String gender,
          final Set<Object> thingsMade,
          final String firstName,
          final String familyName,
          final Date dateOfBirth) {
    setName(name);
    setBirthday(birthday);
    setGender(gender);
    setThingsMade(thingsMade);
    setFirstName(firstName);
    setFamilyName(familyName);
    this.dateOfBirth = dateOfBirth;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[friend: " + getName() + "]";
  }

  /** Gets the date of birth.
   *
   * @return  the date of birth
   */
  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  /** Sets the date of birth.
   *
   * @param dateOfBirth the date of birth
   */
  public void setDateOfBirth(final Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }
}
