/*
 * WhereClause.java
 *
 * Created on Feb 8, 2009, 9:10:57 AM
 *
 * Description: Provides a SPARQL where clause.
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

import java.util.List;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.inference.domainEntity.Statement;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL where clause.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class WhereClause implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the statements */
  @RDFProperty(predicate = "texai:sparqlWhereClauseStatementsList")
  private final List<Statement> statements;
  /** the constraint */
  @RDFProperty(predicate = "texai:sparqlWhereClauseConstraint")
  private final Constraint constraint;

  /** Constructs a new WhereClause instance. */
  public WhereClause() {
    statements = null;
    constraint = null;
  }

  /** Constructs a new WhereClause instance.
   *
   * @param statements the statements
   * @param constraint the constraint
   */
  public WhereClause(final List<Statement> statements, final Constraint constraint) {
    //Preconditions
    assert statements != null : "statements must not be null";
    assert !statements.isEmpty() : "statements must not be empty";

    this.statements = statements;
    this.constraint = constraint;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
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
    final WhereClause other = (WhereClause) obj;
    if (this.statements != other.statements && (this.statements == null || !this.statements.equals(other.statements))) {
      return false;
    }
    if (this.constraint != other.constraint && (this.constraint == null || !this.constraint.equals(other.constraint))) {
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
    int hash = 3;
    hash = 89 * hash + (this.statements != null ? this.statements.hashCode() : 0);
    hash = 89 * hash + (this.constraint != null ? this.constraint.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("WHERE {\n");
    for (final Statement statement : statements) {
      stringBuilder.append("  ");
      stringBuilder.append(statement.toString());
      stringBuilder.append("\n");
    }
    if (constraint != null) {
      stringBuilder.append("  ");
      stringBuilder.append(constraint.toString());
      stringBuilder.append("\n");
    }
    stringBuilder.append("}");
    return stringBuilder.toString();
  }

  /** Return the statements.
   *
   * @return the statements
   */
  public List<Statement> getStatements() {
    return statements;
  }

  /** Gets the constraint.
   *
   * @return the constraint
   */
  public Constraint getConstraint() {
    return constraint;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final Statement statement : statements) {
      statement.instantiate();
    }
    if (constraint != null) {
      constraint.instantiate();
    }
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
    for (final Statement statement : statements) {
      statement.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    }
    if (constraint != null) {
      constraint.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    }
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
    for (final Statement statement : statements) {
      statement.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    }
    if (constraint != null) {
      constraint.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    }
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
