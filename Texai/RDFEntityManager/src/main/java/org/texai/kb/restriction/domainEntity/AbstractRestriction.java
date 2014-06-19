/*
 * Restriction.java
 *
 * Created on Nov 10, 2010, 8:22:11 PM
 *
 * Description: .
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
