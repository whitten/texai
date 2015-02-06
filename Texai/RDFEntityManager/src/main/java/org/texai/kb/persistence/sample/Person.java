/*
 * Person.java
 *
 * Created on August 16, 2007, 3:50 PM
 *
 * Description: Provides a class to represent a person in the FOAF ontology.
 *
 * Copyright (C) August 16, 2007 Stephen L. Reed.
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

  /** Returns whether the given object is equal to this object.
   *
   * @param obj the given object
   * @return whether the given object is equal to this object
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC"})
  public boolean equals(final Object obj) {
    if (obj instanceof Person) {
      final Person that = (Person) obj;
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
