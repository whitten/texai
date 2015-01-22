/*
 * MaxCardinalityRestriction.java
 *
 * Created on Nov 10, 2010, 8:35:11 PM
 *
 * Description: Provides an OWL maximum cardinality restriction on a property's values.
 *
 * Copyright (C) Nov 10, 2010, Stephen L. Reed.
 */
package org.texai.kb.restriction.domainEntity;

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/** Provides an OWL maximum cardinality restriction on a property's values.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:KBObjectContext", subClassOf = {"owl:Restriction"})
public class MaxCardinalityRestriction extends AbstractRestriction {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the maximum cardinality of the values of the associated property */
  @RDFProperty
  private final long maxCardinality;

  /** Constructs a new MaxCardinalityRestriction instance. */
  public MaxCardinalityRestriction() {
    maxCardinality = 0;
  }

  /** Constructs a new MaxCardinalityRestriction instance.
   *
   * @param onProperty the property for which this restriction applies
   * @param maxCardinality the maximum cardinality of the values of the associated property
   */
  public MaxCardinalityRestriction(
          final URI onProperty,
          final long maxCardinality) {
    super(onProperty);
    //Preconditions
    assert maxCardinality >= 0 : "maxCardinality must not be negative";

    this.maxCardinality = maxCardinality;
  }

  /** Gets the maximum cardinality of the values of the associated property.
   *
   * @return the maximum cardinality of the values of the associated property
   */
  public long getMaxCardinality() {
    return maxCardinality;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return getOnProperty().hashCode() + (int) maxCardinality;
  }

  /** Returns whether this object equals the given object.
   *
   * @param obj the given object
   * @return whether this object equals the given object
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof MaxCardinalityRestriction) {
      final MaxCardinalityRestriction that = (MaxCardinalityRestriction) obj;
      return this.getOnProperty().equals(that.getOnProperty()) && this.maxCardinality == that.maxCardinality;
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
    stringBuilder.append(", maxCardinality ");
    stringBuilder.append(maxCardinality);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }
}
