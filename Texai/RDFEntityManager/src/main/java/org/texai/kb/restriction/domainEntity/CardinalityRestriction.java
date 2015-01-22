/*
 * CardinalityRestriction.java
 *
 * Created on Nov 10, 2010, 8:35:53 PM
 *
 * Description: Provides an OWL cardinality restriction on a property's values.
 *
 * Copyright (C) Nov 10, 2010, Stephen L. Reed.
 */
package org.texai.kb.restriction.domainEntity;

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/** Provides an OWL cardinality restriction on a property's values.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:KBObjectContext", subClassOf = {"owl:Restriction"})
public class CardinalityRestriction extends AbstractRestriction {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the cardinality of the values of the associated property */
  @RDFProperty
  private final long cardinality;

  /** Constructs a new CardinalityRestriction instance. */
  public CardinalityRestriction() {
    cardinality = 0;
  }

  /** Constructs a new CardinalityRestriction instance.
   *
   * @param onProperty the property for which this restriction applies
   * @param cardinality the cardinality of the values of the associated property
   */
  public CardinalityRestriction(
          final URI onProperty,
          final long cardinality) {
    super(onProperty);
    //Preconditions
    assert cardinality >= 0 : "cardinality must not be negative";

    this.cardinality = cardinality;
  }

  /** Gets the cardinality of the values of the associated property.
   *
   * @return the cardinality of the values of the associated property
   */
  public long getCardinality() {
    return cardinality;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return getOnProperty().hashCode() + (int) cardinality;
  }

  /** Returns whether this object equals the given object.
   *
   * @param obj the given object
   * @return whether this object equals the given object
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof CardinalityRestriction) {
      final CardinalityRestriction that = (CardinalityRestriction) obj;
      return this.getOnProperty().equals(that.getOnProperty()) && this.cardinality == that.cardinality;
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
    stringBuilder.append(", cardinality ");
    stringBuilder.append(cardinality);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

}
