/*
 * AbstractOperator.java
 *
 * Created on Feb 8, 2009, 10:52:24 PM
 *
 * Description: Provides an abstract SPARQL constraint operator.
 *
 * Copyright (C) Feb 8, 2009 Stephen L. Reed.
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

package org.texai.inference.sparql.domainEntity;


import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;

/** Provides an abstract SPARQL constraint operator.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public abstract class AbstractOperator implements CascadePersistence {

  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD

  /** Constructs a new AbstractOperator instance. */
  protected AbstractOperator() {
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public abstract void instantiate();

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public abstract void cascadePersist(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext);

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public abstract void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager);
}
