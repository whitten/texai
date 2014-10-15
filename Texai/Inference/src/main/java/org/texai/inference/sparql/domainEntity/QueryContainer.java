/*
 * QueryContainer.java
 *
 * Created on Feb 8, 2009, 8:03:16 AM
 *
 * Description: Provides a SPARQL query container.
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

import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL query container.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
@ThreadSafe
public class QueryContainer implements Comparable<QueryContainer>, CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the query name */
  @RDFProperty(predicate = "texai:sparqlQueryContainerName")
  private final String name;
  /** the base declaration */
  @RDFProperty(predicate = "texai:sparqlQueryContainerBaseDeclaration")
  private final BaseDeclaration baseDeclaration;
  /** the prefix declarations */
  @RDFProperty(predicate = "texai:sparqlQueryContainerPrefixDeclaration")
  private final Set<PrefixDeclaration> prefixDeclarations;
  /** the query */
  @RDFProperty(predicate = "texai:sparqlQueryContainerQuery")
  private final AbstractQuery query;

  /** Constructs a new QueryContainer instance. */
  public QueryContainer() {
    name = null;
    baseDeclaration = null;
    prefixDeclarations = null;
    query = null;
  }

  /** Constructs a new QueryContainer instance.
   *
   * @param name the query name
   * @param baseDeclaration the base declaration
   * @param prefixDeclarations the prefix declarations
   * @param query the query
   */
  public QueryContainer(
          final String name,
          final BaseDeclaration baseDeclaration,
          final Set<PrefixDeclaration> prefixDeclarations,
          final AbstractQuery query) {
    //Preconditons
    assert name != null : "name must not be null";
    assert !name.isEmpty() : "name must not be empty";
    assert prefixDeclarations != null : "prefixDeclarations must not be null";
    assert query != null : "query must not be null";

    this.name = name;
    this.baseDeclaration = baseDeclaration;
    this.prefixDeclarations = prefixDeclarations;
    this.query = query;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the query name.
   *
   * @return the query name
   */
  public String getName() {
    return name;
  }

  /** Gets the base declaration.
   *
   * @return the base declaration
   */
  public BaseDeclaration getBaseDeclaration() {
    return baseDeclaration;
  }

  /** Gets the prefix declarations.
   *
   * @return the prefix declarations
   */
  public Set<PrefixDeclaration> getPrefixDeclarations() {
    return prefixDeclarations;
  }

  /** Gets the query.
   *
   * @return the query
   */
  public AbstractQuery getQuery() {
    return query;
  }

  /** Returns a detailed string representation of this object.
   *
   * @return a detailed string representation of this object
   */
  public String toDetailedString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("name: ");
    stringBuilder.append(name);
    stringBuilder.append("\n");
    stringBuilder.append(toString());
    return stringBuilder.toString();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (final PrefixDeclaration prefixDeclaration : prefixDeclarations) {
      stringBuilder.append(prefixDeclaration.toString());
      stringBuilder.append("\n");
    }
    if (baseDeclaration != null) {
      stringBuilder.append(baseDeclaration.toString());
      stringBuilder.append("\n");
    }
    stringBuilder.append("\n");
    stringBuilder.append(query.toString());
    return stringBuilder.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    if (baseDeclaration != null) {
      baseDeclaration.instantiate();
    }
    for (final PrefixDeclaration prefixDeclaration : prefixDeclarations) {
      prefixDeclaration.instantiate();
    }
    query.instantiate();
  }

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  public void cascadePersist(
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadePersist(
            this,
            rdfEntityManager,
            null); // overrideContext
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
    if (rootRDFEntity1.getId() == null) {
      rdfEntityManager.setIdFor(rootRDFEntity1);
    }
    if (baseDeclaration != null) {
      baseDeclaration.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    }
    for (final PrefixDeclaration prefixDeclaration : prefixDeclarations) {
      prefixDeclaration.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    }
    query.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    rdfEntityManager.persist(rootRDFEntity1, this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its components.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public void cascadeRemove(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadeRemove(this, rdfEntityManager);
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
    if (baseDeclaration != null) {
      baseDeclaration.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    }
    for (final PrefixDeclaration prefixDeclaration : prefixDeclarations) {
      prefixDeclaration.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    }
    query.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

  /** Compares this query container to another one, by comparing their respective names.
   *
   * @param that the other query container
   * @return -1 if this one is less than that one, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final QueryContainer that) {
    //Preconditons
    assert that != null : "that must not be null";

    return this.getName().compareTo(that.getName());
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final QueryContainer other = (QueryContainer) obj;
    if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
      return false;
    }
    if (this.baseDeclaration != other.baseDeclaration && (this.baseDeclaration == null || !this.baseDeclaration.equals(other.baseDeclaration))) {
      return false;
    }
    if (this.prefixDeclarations != other.prefixDeclarations && (this.prefixDeclarations == null || !this.prefixDeclarations.equals(other.prefixDeclarations))) {
      System.out.println(this.prefixDeclarations);
      System.out.println(other.prefixDeclarations);
      return false;
    }
    if (this.query != other.query && (this.query == null || !this.query.equals(other.query))) {
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
    int hash = 5;
    hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);    // NOPMD
    return hash;
  }


}
