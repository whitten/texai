/*
 * AbstractKBObject.java
 *
 * Created on Apr 8, 2009, 11:46:51 AM
 *
 * Description: Provides an abstract KB object.
 *
 * Copyright (C) Apr 8, 2009 Stephen L. Reed.
 */
package org.texai.kb.object;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.ArraySet;

/** Provides an abstract KB object, which is a set of statements having a common subject term.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public abstract class AbstractKBObject implements Serializable {
  /** the default serial version UID */
  private static final long serialVersionUID = 1L;
// NOPMD

  private URI id;    // NOPMD
  /** the statements */
  private final Set<Statement> statements;
  /** the subject URI */
  private URI subject;
  /** the repository name */
  private final String repositoryName;

  /** Constructs a new AbstractKBObject instance.
   *
   * @param statements the statements
   * @param repositoryName the repository name
   */
  public AbstractKBObject(
          final Set<Statement> statements,
          final String repositoryName) {
    //Preconditions
    assert statements != null : "statements must not be null";
    assert !statements.isEmpty() : "statements must not be empty";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    this.statements = statements;
    this.repositoryName = repositoryName;
  }

  /** Gets the statements.
   *
   * @return the statements
   */
  public final Set<Statement> getStatements() {
    return statements;
  }

  /** Gets the subject.
   *
   * @return the subject
   */
  public final URI getSubject() {
    if (subject == null) {
      subject = (URI) ((Statement) statements.toArray()[0]).getSubject();
    }
    return subject;
  }

  /** Gets the repository name.
   *
   * @return the repository name
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** Returns the classes for which this object is an instance.
   *
   * @return the classes for which this object is an instance
   */
  public final Set<URI> getTypes() {
    final Set<URI> types = new ArraySet<>();
    for (final Statement statement : statements) {
      if (statement.getPredicate().equals(RDF.TYPE)) {
        types.add((URI) statement.getObject());
      }
    }
    return types;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object.
   */
  @Override
  public final int hashCode() {
    int hash = 7;
    hash = 89 * hash + (this.subject != null ? this.subject.hashCode() : 0);    // NOPMD
    return hash;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public final boolean equals(final Object obj) {
    if (obj instanceof AbstractKBObject) {
      final AbstractKBObject that = (AbstractKBObject) obj;
      return this.getStatements().equals(that.getStatements());
    } else {
      return false;
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public final String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    List<Statement> sortedStatements = new ArrayList<>(statements);
    sortedStatements = RDFUtility.sortStatements(sortedStatements);
    for (final Statement sortedStatement : sortedStatements) {
      stringBuilder.append(RDFUtility.formatStatementAsTurtle(sortedStatement));
      stringBuilder.append("\n");
    }
    return stringBuilder.toString();
  }
}
