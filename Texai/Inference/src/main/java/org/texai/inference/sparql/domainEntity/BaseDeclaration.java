/*
 * BaseDeclaration.java
 *
 * Created on Feb 8, 2009, 8:04:45 AM
 *
 * Description: Provides a SPARQL base declaration.
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
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL base declaration.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class BaseDeclaration implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the base namespace */
  @RDFProperty(predicate = "texai:sparqlBaseNamespace")
  private final String baseNamespace;

  /** Constructs a new BaseDeclaration instance. */
  public BaseDeclaration() {
    baseNamespace = null;
  }

  /** Constructs a new BaseDeclaration instance.
   *
   * @param baseNamespace the base namespace
   */
  public BaseDeclaration(final String baseNamespace) {
    //Preconditons
    assert baseNamespace != null : "baseNamespace must not be null";

    this.baseNamespace = baseNamespace;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the base namespace.
   *
   * @return the base namespace.
   */
  public String getBaseNamespace() {
    return baseNamespace;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final BaseDeclaration other = (BaseDeclaration) obj;
    if ((this.baseNamespace == null) ? (other.baseNamespace != null) : !this.baseNamespace.equals(other.baseNamespace)) {
      return false;
    }
    return true;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 61 * hash + (this.baseNamespace != null ? this.baseNamespace.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return  a string representation of this object
   */
  @Override
  public String toString() {
    return "BASE <" + baseNamespace + ">";
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
  }

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
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

  /** Recursively removes this RDF entity and all its components.
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
