/*
 * Persistent.java
 *
 * Created on May 29, 2008, 10:37:11 AM
 *
 * Description: Defines the interface for a complex persistent object, which cascades persistence and instantiation
 * operations.
 *
 * Copyright (C) May 29, 2008 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import org.openrdf.model.URI;

/** Defines the interface for a complex persistent object, which cascades persistence and instantiation
 * operations.
 *
 * @author Stephen L. Reed.
 */
public interface CascadePersistence extends RDFPersistent {

  /** Ensures that this persistent object is fully instantiated. */
  void instantiate();

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  void cascadePersist(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext);

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager);
}
