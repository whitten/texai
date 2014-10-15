/*
 * SelectQuery.java
 *
 * Created on Feb 8, 2009, 9:10:09 AM
 *
 * Description: Provides a SPARQL SELECT query.
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


import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL SELECT query.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class SelectQuery extends AbstractQuery {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the SELECT clause */
  @RDFProperty(predicate = "texai:sparqlSelectClause")
  private final Select select;

  /** Constructs a new SelectQuery instance. */
  public SelectQuery() {
    select = null;
  }

  /** Constructs a new SelectQuery instance.
   * @param select the SELECT clause
   * @param whereClause the WHERE clause
   */
  public SelectQuery(final Select select, final WhereClause whereClause) {
    super(whereClause);
    //Preconditions
    assert select != null : "select must not be null";

    this.select = select;
  }

  /** Gets the SELECT clause.
   *
   * @return the SELECT clause
   */
  public Select getSelect() {
    return select;
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
    final SelectQuery other = (SelectQuery) obj;
    if (this.select != other.select && (this.select == null || !this.select.equals(other.select))) {
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
    hash = 23 * hash + (this.select != null ? this.select.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(select.toString());
    stringBuilder.append("\n");
    stringBuilder.append(getWhereClause().toString());
    return stringBuilder.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    select.instantiate();
    getWhereClause().instantiate();
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
    select.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    getWhereClause().cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
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
    select.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    getWhereClause().cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
