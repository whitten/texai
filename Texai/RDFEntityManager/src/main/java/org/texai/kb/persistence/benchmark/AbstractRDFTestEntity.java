/*
 * AbstractRDFTestEntity.java
 *
 * Created on August 16, 2007, 10:59 AM
 *
 * Description: Provides an abstract RDF test entity to test the inheritance of semantic annotations.
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
package org.texai.kb.persistence.benchmark;

import java.util.Set;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFProperty;

/** Provides an abstract RDF test entity to test the inheritance of semantic annotations.
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
  @RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)
},
subject = "texai:org.texai.kb.persistence.benchmark.AbstractRDFTestEntity")
public abstract class AbstractRDFTestEntity {    // NOPMD

  /** a set field */
  @RDFProperty(predicate = "cyc:cyclistNotes")
  private Set<String> cyclistNotes;

  /** Creates a new instance of AbstractRDFTestEntity. */
  public AbstractRDFTestEntity() {
  }

  /** Gets the cyclist notes test field.
   *
   * @return the cyclist notes test field
   */
  public final Set<String> getCyclistNotes() {
    return cyclistNotes;
  }

  /** Sets the cyclist notes test field.
   *
   * @param cyclistNotes the cyclist notes test field
   */
  public final void setCyclistNotes(final Set<String> cyclistNotes) {
    this.cyclistNotes = cyclistNotes;
  }
}
