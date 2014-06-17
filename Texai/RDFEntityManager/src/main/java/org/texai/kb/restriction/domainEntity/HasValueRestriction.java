/*
 * HasValueRestriction.java
 *
 * Created on Nov 10, 2010, 8:34:51 PM
 *
 * Description: Provides an OWL has-value restriction on a property's value.
 *
 * Copyright (C) Nov 10, 2010, Stephen L. Reed.
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
package org.texai.kb.restriction.domainEntity;

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/** Provides an OWL has-value restriction on a property's value.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:KBObjectContext", subClassOf = {"owl:Restriction"})
public class HasValueRestriction extends AbstractRestriction {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the class that constrains the value of the associated property */
  @RDFProperty
  private final URI hasValueClass;

  /** Constructs a new HasValueRestriction instance. */
  public HasValueRestriction() {
    hasValueClass = null;
  }

  /** Constructs a new HasValueRestriction instance.
   *
   * @param onProperty the property for which this restriction applies
   * @param hasValueClass the class that constrains the value of the associated property
   */
  public HasValueRestriction(
          final URI onProperty,
          final URI hasValueClass) {
    super(onProperty);
    //Preconditions
    assert hasValueClass != null : "hasValueClass must not be null";

    this.hasValueClass = hasValueClass;
  }

  /** Gets the class that constrains the value of the associated property.
   *
   * @return the class that constrains the value of the associated property
   */
  public URI getHasValueClass() {
    return hasValueClass;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return getOnProperty().hashCode() + hasValueClass.hashCode();
  }

  /** Returns whether this object equals the given object.
   *
   * @param obj the given object
   * @return whether this object equals the given object
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof HasValueRestriction) {
      final HasValueRestriction that = (HasValueRestriction) obj;
      return this.getOnProperty().equals(that.getOnProperty()) && this.hasValueClass.equals(that.hasValueClass);
    } else {
      return false;
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[Restriction on ");
    stringBuilder.append(RDFUtility.formatResource(getOnProperty()));
    stringBuilder.append(", hasValue ");
    stringBuilder.append(RDFUtility.formatResource(hasValueClass));
    stringBuilder.append(']');
    return stringBuilder.toString();
  }
}
