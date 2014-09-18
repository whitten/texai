/*
 * Person.java
 *
 * Created on August 16, 2007, 3:50 PM
 *
 * Description: Provides a class to represent a person in the FOAF ontology.
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

import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFProperty;

/** Provides a class to represent a person in the FOAF ontology.
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "foaf", namespaceURI = Constants.FOAF_NAMESPACE)
}, subject = "foaf:Person")
public class Person extends Agent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the first name of this person  */
  @RDFProperty(predicate = "foaf:firstName")
  private String firstName;
  /** the family name of this person  */
  @RDFProperty(predicate = "foaf:familyName")
  private String familyName;

  /** Creates a new instance of Person. */
  public Person() {
  }

  /** Gets the first name.
   *
   * @return the first name
   */
  public String getFirstName() {
    return firstName;
  }

  /** Sets the first name.
   *
   * @param firstName the first name
   */
  public void setFirstName(final String firstName) {
    this.firstName = firstName;
  }

  /** Gets the family name.
   *
   * @return the family name
   */
  public String getFamilyName() {
    return familyName;
  }

  /** Sets the family name.
   *
   * @param familyName the family name
   */
  public void setFamilyName(final String familyName) {
    this.familyName = familyName;
  }
}
