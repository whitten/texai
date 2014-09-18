/*
 * AbstractCascadePersistence.java
 *
 * Created on Jul 27, 2010, 8:16:43 AM
 *
 * Description: Provides a convenient superclass for complex persistent objects.
 *
 * Copyright (C) Jul 27, 2010, Stephen L. Reed.
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
package org.texai.kb.persistence;

import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;

/** Provides a convenient superclass for complex persistent objects.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractCascadePersistence implements CascadePersistence {
  /** the default serial version UID */
  private static final long serialVersionUID = 1L;

  /** the id assigned by the persistence framework */
  @Id
  private URI id;                                          // NOPMD

  /** Constructs a new AbstractCascadePersistence instance. */
  public AbstractCascadePersistence() {
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Ensures that this persistent object is fully instantiated. Subclasses should invoke super.instantiate() after
   * optionally performing a recursive instaniation of their own locally declared entities.
   *
   * <pre>
   *    entity.instantiate();
   *    . . .
   *    super.instantiate();
   * </pre>
   */
  @Override
  public void instantiate() {
  }

  /** Recursively persists this RDF entity and all its components.  Subclasses should invoke super.cascadePersist() after
   * optionally performing a recursive persistence of their own locally declared entities.
   *
   * <pre>
   *    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
   *    entity.cascadePersist(
   *            rootRDFEntity1,
   *            rdfEntityManager,
   *            overrideContext);
   *    . . .
   *    super.cascadePersist(rootRDFEntity, rdfEntityManager, overrideContext);
   * </pre>
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null if the object's default is to be used
   */
  @Override
  public void cascadePersist(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    rdfEntityManager.persist(rootRDFEntity1, this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its components. Subclasses should invoke super.cascadePersist() after
   * optionally performing a recursive persistence of their own locally declared entities.
   *
   * <pre>
   *    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
   *    entity.cascadeRemove(
   *            rootRDFEntity1,
   *            rdfEntityManager);
   *    . . .
   *    super.cascadeRemove(rootRDFEntity, rdfEntityManager);
   * </pre>
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
