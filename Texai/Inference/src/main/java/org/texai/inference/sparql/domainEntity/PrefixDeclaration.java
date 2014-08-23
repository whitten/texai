/*
 * PrefixDeclaration.java
 *
 * Created on Feb 8, 2009, 8:05:00 AM
 *
 * Description: Provides a SPARQL prefix declaration.
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

/** Provides a SPARQL prefix declaration.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class PrefixDeclaration implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the prefix */
  @RDFProperty(predicate = "texai:sparqlPrefixDeclarationPrefix")
  private final String prefix;
  /** the prefix namespace */
  @RDFProperty(predicate = "texai:sparqlPrefixDeclarationNamespace")
  private final String namespace;

  /** Constructs a new PrefixDeclaration instance. */
  public PrefixDeclaration() {
    prefix = null;
    namespace = null;
  }

  /** Constructs a new PrefixDeclaration instance.
   *
   * @param prefix the prefix
   * @param namespace the namespace
   */
  public PrefixDeclaration(final String prefix, final String namespace) {
    //Preconditons
    assert prefix != null : "prefix must not be null";
    assert !prefix.isEmpty() : "prefix must not be empty";
    assert namespace != null : "namespace must not be null";
    assert !namespace.isEmpty() : "namespace must not be empty";

    this.prefix = prefix;
    this.namespace = namespace;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  public URI getId() {
    return id;
  }

  /** Gets the prefix.
   *
   * @return the prefix.
   */
  public String getPrefix() {
    return prefix;
  }

  /** Gets the namespace.
   *
   * @return the namespace.
   */
  public String getNamespace() {
    return namespace;
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
    final PrefixDeclaration other = (PrefixDeclaration) obj;
    if ((this.prefix == null) ? (other.prefix != null) : !this.prefix.equals(other.prefix)) {
      return false;
    }
    if ((this.namespace == null) ? (other.namespace != null) : !this.namespace.equals(other.namespace)) {
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
    hash = 97 * hash + (this.prefix != null ? this.prefix.hashCode() : 0);
    hash = 97 * hash + (this.namespace != null ? this.namespace.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return  a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("PREFIX ");
    stringBuilder.append(prefix);
    stringBuilder.append(": <");
    stringBuilder.append(namespace);
    stringBuilder.append(">");
    return stringBuilder.toString();
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
