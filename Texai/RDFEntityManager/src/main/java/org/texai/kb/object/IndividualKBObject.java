/*
 * IndividualKBObject.java
 *
 * Created on Apr 8, 2009, 11:47:26 AM
 *
 * Description: Provides an individual KB object.
 *
 * Copyright (C) Apr 8, 2009 Stephen L. Reed.
 */
package org.texai.kb.object;

import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.util.TexaiException;

/** Provides an individual KB object.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class IndividualKBObject extends AbstractKBObject {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;

  /** Constructs a new IndividualKBObject instance.
   *
   * @param statements the statements
   * @param repositoryName the repository name
   */
  public IndividualKBObject(
          final Set<Statement> statements,
          final String repositoryName) {
    super(statements, repositoryName);
  }

  /** Makes a new individual KB object from the given turtle-formatted statements.
   *
   * @param string the given turtle-formatted statements
   * @param repositoryName the repository name
   * @return a new individual KB object
   */
  public static IndividualKBObject makeIndividualKBObject(
          final String string,
          final String repositoryName) {
    //Preconditions
    assert string != null : "string must not be null";
    assert !string.isEmpty() : "string must not be empty";

    final Set<Statement> statements = new HashSet<>(TurtleStatementParser.makeTurtleStatementParser(string).getStatements());
    boolean isSubClassOfFound = false;
    for (final Statement statement : statements) {
      if (statement.getPredicate().equals(RDF.TYPE)) {
        isSubClassOfFound = true;
        break;
      }
    }
    if (!isSubClassOfFound) {
      throw new TexaiException("individual KB object must contain a rdf:type statement");
    }
    return new IndividualKBObject(statements, repositoryName);
  }

}
