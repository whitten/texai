/*
 * ClassKBObject.java
 *
 * Created on Apr 8, 2009, 11:47:11 AM
 *
 * Description: Provides a class KB object.
 *
 * Copyright (C) Apr 8, 2009 Stephen L. Reed.
 */
package org.texai.kb.object;

import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDFS;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;

/** Provides a class KB object.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class ClassKBObject extends AbstractKBObject {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;

  /** Constructs a new ClassKBObject instance.
   *
   * @param statements the statements
   * @param repositoryName the repository name
   */
  public ClassKBObject(
          final Set<Statement> statements,
          final String repositoryName) {
    super(statements, repositoryName);
  }

  /** Makes a new class KB object from the given turtle-formatted statements.
   *
   * @param string the given turtle-formatted statements
   * @param repositoryName the repository name
   * @return a new class KB object
   */
  public static ClassKBObject makeClassKBObject(
          final String string,
          final String repositoryName) {
    //Preconditions
    assert string != null : "string must not be null";
    assert !string.isEmpty() : "string must not be empty";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    final Set<Statement> statements = new HashSet<>(TurtleStatementParser.makeTurtleStatementParser(string).getStatements());
    boolean isSubClassOfFound = false;
    for (final Statement statement : statements) {
      if (statement.getPredicate().equals(RDFS.SUBCLASSOF)) {
        isSubClassOfFound = true;
        break;
      }
    }
    if (!isSubClassOfFound) {
      throw new TexaiException("class KB object must contain an rdfs:subClassOf statement");
    }
    return new ClassKBObject(statements, repositoryName);
  }

  /** Returns the direct super classes.
   *
   * @return the direct super classes.
   */
  public Set<URI> getSuperClasses() {
    final Set<URI> superClasses = new ArraySet<>();
    for (final Statement statement : getStatements()) {
      if (statement.getPredicate().equals(RDFS.SUBCLASSOF)) {
        superClasses.add((URI) statement.getObject());
      }
    }
    return superClasses;
  }

  /** Returns the direct disjoint-withs.
   *
   * @return the direct disjoint-withs
   */
  public Set<URI> getDisjointWiths() {
    final Set<URI> disjointWiths = new ArraySet<>();
    for (final Statement statement : getStatements()) {
      if (statement.getPredicate().equals(OWL.DISJOINTWITH)) {
        disjointWiths.add((URI) statement.getObject());
      }
    }
    return disjointWiths;
  }

}
