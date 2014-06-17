/*
 * SomeValuesFromRestriction.java
 *
 * Created on Nov 10, 2010, 8:34:31 PM
 *
 * Description: Provides an OWL some-values restriction on a property's values.
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

/** Provides an OWL some-values restriction on a property's values.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:KBObjectContext", subClassOf = {"owl:Restriction"})
public class SomeValuesFromRestriction extends AbstractRestriction {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the class that constrains some of the values of the associated property */
  @RDFProperty
  private final URI someValuesClass;

  /** Constructs a new SomeValuesFromRestriction instance. */
  public SomeValuesFromRestriction() {
    someValuesClass = null;
  }

  /** Constructs a new SomeValuesFromRestriction instance.
   *
   * @param onProperty the property for which this restriction applies
   * @param someValuesClass the class that constrains some of the values of the associated property
   */
  public SomeValuesFromRestriction(
          final URI onProperty,
          final URI someValuesClass) {
    super(onProperty);
    //Preconditions
    assert someValuesClass != null : "allValuesClass must not be null";

    this.someValuesClass = someValuesClass;
  }

  /** Gets the class that constrains some of the values of the associated property.
   *
   * @return the class that constrains some of the values of the associated property
   */
  public URI getSomeValuesClass() {
    return someValuesClass;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return getOnProperty().hashCode() + someValuesClass.hashCode();
  }

  /** Returns whether this object equals the given object.
   *
   * @param obj the given object
   * @return whether this object equals the given object
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof SomeValuesFromRestriction) {
      final SomeValuesFromRestriction that = (SomeValuesFromRestriction) obj;
      return this.getOnProperty().equals(that.getOnProperty()) && this.someValuesClass.equals(that.someValuesClass);
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
    stringBuilder.append(", someVauesFrom ");
    stringBuilder.append(RDFUtility.formatResource(someValuesClass));
    stringBuilder.append(']');
    return stringBuilder.toString();
  }
}
