/*
 * Restriction.java
 *
 * Created on Nov 10, 2010, 8:22:11 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 10, 2010, Stephen L. Reed.
 */
package org.texai.kb.restriction.domainEntity;

import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/**
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractRestriction implements RDFPersistent {
  /** the default serial version UID */
  private static final long serialVersionUID = 1L;

  /** the id assigned by the persistence framework */
  @Id
  private URI id;
  /** the property for which this restriction applies */
  @RDFProperty(predicate = "owl:onProperty")
  private final URI onProperty;

  /** Constructs a new Restriction instance. */
  public AbstractRestriction() {
    onProperty = null;
  }

  /** Constructs a new Restriction instance.
   *
   * @param onProperty the property for which this restriction applies
   */
  public AbstractRestriction(final URI onProperty) {
    //Preconditions
    assert onProperty != null : "onProperty must not be null";

    this.onProperty = onProperty;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the property for which this restriction applies.
   *
   * @return the property for which this restriction applies
   */
  public URI getOnProperty() {
    return onProperty;
  }
}
