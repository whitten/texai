/*
 * PropertyKBObject.java
 *
 * Created on Apr 8, 2009, 11:47:40 AM
 *
 * Description: Provides a property KB object.
 *
 * Copyright (C) Apr 8, 2009 Stephen L. Reed.
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
package org.texai.kb.object;

import java.util.HashSet;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDFS;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;

/** Provides a property KB object, which is a set of statements having a common subject term.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class PropertyKBObject extends AbstractKBObject {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;

  /** Constructs a new PropertyKBObject instance.
   *
   * @param statements the statements
   * @param repositoryName the repository name
   */
  public PropertyKBObject(
          final Set<Statement> statements,
          final String repositoryName) {
    super(statements, repositoryName);
  }

  /** Makes a new property KB object from the given turtle-formatted statements.
   *
   * @param string the given turtle-formatted statements
   * @param repositoryName the repository name
   * @return a new property KB object
   */
  public static PropertyKBObject makePropertyKBObject(
          final String string,
          final String repositoryName) {
    //Preconditions
    assert string != null : "string must not be null";
    assert !string.isEmpty() : "string must not be empty";

    final Set<Statement> statements = new HashSet<>(TurtleStatementParser.makeTurtleStatementParser(string).getStatements());
    boolean isSubClassOfFound = false;
    for (final Statement statement : statements) {
      if (statement.getPredicate().equals(RDFS.SUBPROPERTYOF)) {
        isSubClassOfFound = true;
        break;
      }
    }
    if (!isSubClassOfFound) {
      throw new TexaiException("property KB object must contain a rdfs:subPropertyOf statement");
    }
    return new PropertyKBObject(statements, repositoryName);
  }

  /** Returns the properties for which this property is a subproperty.
   *
   * @return the properties for which this property is a subproperty
   */
  public Set<URI> getSuperProperties() {
    final Set<URI> superProperties = new ArraySet<>();
    for (final Statement statement : getStatements()) {
      if (statement.getPredicate().equals(RDFS.SUBPROPERTYOF)) {
        superProperties.add((URI) statement.getObject());
      }
    }
    return superProperties;
  }

}
