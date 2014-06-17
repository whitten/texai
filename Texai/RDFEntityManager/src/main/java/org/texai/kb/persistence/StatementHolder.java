/*
 * StatementHolder.java
 *
 * Created on Jan 15, 2009, 11:48:10 AM
 *
 * Description: Provides an RDF statement holder.
 *
 * Copyright (C) Jan 15, 2009 Stephen L. Reed.
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
package org.texai.kb.persistence;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;

/** Provides an RDF statement holder.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:ModelSituationContext")
@NotThreadSafe
public class StatementHolder implements Comparable<StatementHolder>, CascadePersistence, Cloneable {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the subject of the RDF statement */
  @RDFProperty(predicate = "texai:fcgStatementHolderSubject")
  private URI subject;
  /** the predicate of the RDF statement */
  @RDFProperty(predicate = "texai:fcgStatementHolderPredicate")
  private final URI predicate;
  /** the object of the RDF statement */
  @RDFProperty(predicate = "texai:fcgStatementHolderObject")
  private Value object;
  /** the cached statement */
  private Statement statement;

  /** Constructs a new StatementHolder instance. */
  public StatementHolder() {
    subject = null;
    predicate = null;
    object = null;
  }

  /** Constructs a new StatementHolder instance.
   *
   * @param statement the statement
   */
  public StatementHolder(final Statement statement) {
    //Preconditions
    assert statement != null : "statement must not be null";
    assert statement.getSubject() instanceof URI : "statement subject must be a URI";

    subject = (URI) statement.getSubject();
    predicate = statement.getPredicate();
    object = statement.getObject();
  }

  /** Constructs a new StatementHolder instance.
   *
   * @param subject the subject of the RDF statement
   * @param predicate the predicate of the RDF statement
   * @param object the object of the RDF statement
   */
  public StatementHolder(
          final URI subject,
          final URI predicate,
          final Value object) {
    //Preconditions
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";
    assert object != null : "object must not be null";

    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
  }

  /** Creates a clone of this object.
   *
   * @return a clone of this object
   */
  @Override
  public StatementHolder clone() {
    //Preconditions
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";
    assert object != null : "object must not be null";

    try {
      return (StatementHolder) super.clone();
    } catch (CloneNotSupportedException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Creates a clone of the given statement holders.
   *
   * @param statementHolders  the given statement holders
   * @return a clone of the given statement holders
   */
  public static Set<StatementHolder> cloneSet(final Set<StatementHolder> statementHolders) {
    //Preconditions
    assert statementHolders != null : "statementHolders must not be null";

    final Set<StatementHolder> clonedStatementHolders = new ArraySet<StatementHolder>();
    for (final StatementHolder statementHolder : statementHolders) {
      clonedStatementHolders.add(statementHolder.clone());
    }

    return clonedStatementHolders;
  }

  /** Creates a set of statement holders for the given statements.
   *
   * @param statements the given statements
   * @return a set of statement holders for the given statements
   */
  public static Set<StatementHolder> toStatementHolders(final Set<Statement> statements) {
    //Preconditions
    assert statements != null : "statements must not be null";

    final Set<StatementHolder> statementHolders = new ArraySet<StatementHolder>();
    for (final Statement statement : statements) {
      statementHolders.add(new StatementHolder(statement));
    }

    return statementHolders;
  }

  /** Creates a set of statements for the given statement holders.
   *
   * @param statement holders the given statement holders
   * @return a set of statements for the given statement holders
   */
  public static Set<Statement> toStatements(final Set<StatementHolder> statementHolders) {
    //Preconditions
    assert statementHolders != null : "statementHolders must not be null";

    final Set<Statement> statements = new HashSet<Statement>();
    for (final StatementHolder statementHolder : statementHolders) {
      statements.add(statementHolder.getStatement());
    }
    return statements;
  }

  /** Gets the statement.
   *
   * @return the statement
   */
  public Statement getStatement() {
    if (statement == null) {
      statement = new StatementImpl(subject, predicate, object);
    }
    return statement;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return subject.hashCode() + predicate.hashCode();
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return  whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof StatementHolder) {
      final StatementHolder that = (StatementHolder) obj;
      return this.subject.equals(that.subject) &&
              this.predicate.equals(that.predicate) &&
              this.object.equals(that.object);
    } else {
      return false;
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return getStatement().toString();
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
    rdfEntityManager.persist(rootRDFEntity1, this);
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

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
  }

  /** Compares another object with this one.
   *
   * @param that another statement holder
   * @return -1 if this is less than the other statement holder, 0 if equal, otherwise +1
   */
  @Override
  public int compareTo(final StatementHolder that) {
    //Preconditions
    assert that != null : "that must not be null";

      return RDFUtility.formatStatementAsTurtle(this.getStatement()).compareTo(RDFUtility.formatStatementAsTurtle(that.getStatement()));
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the subject of the RDF statement.
   *
   * @return the subject of the RDF statement
   */
  public URI getSubject() {
    return subject;
  }

  /** Gets the predicate of the RDF statement.
   *
   * @return the predicate of the RDF statement
   */
  public URI getPredicate() {
    return predicate;
  }

  /** Gets the object of the RDF statement.
   *
   * @return the object of the RDF statement
   */
  public Value getObject() {
    return object;
  }

  /** Sets the subject.
   *
   * @param subject the subject
   */
  public void setSubject(final URI subject) {
    //Preconditions
    assert subject != null : "subject must not be null";

    this.subject = subject;
    statement = null;
  }

  /** Sets the object.
   *
   * @param object the object
   */
  public void setObject(final Value object) {
    //Preconditions
    assert object != null : "object must not be null";

    this.object = object;
    statement = null;
  }

}
