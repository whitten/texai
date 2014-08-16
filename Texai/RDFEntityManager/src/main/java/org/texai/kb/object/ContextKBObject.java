/*
 * ContextKBObject.java
 *
 * Created on Apr 8, 2009, 11:47:55 AM
 *
 * Description: Provides a context KB object.
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

import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.util.ArraySet;

/** Provides a context KB object.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class ContextKBObject extends AbstractKBObject {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;

  /** Constructs a new ContextKBObject instance.
   *
   * @param statements the statements
   * @param repositoryName the repository name
   */
  public ContextKBObject(
          final Set<Statement> statements,
          final String repositoryName) {
    super(statements, repositoryName);
  }

  /** Returns the contexts for which this context is a subcontext.
   *
   * @return the properties for which this property is a subproperty
   */
  public Set<URI> getSuperContexts() {
    final Set<URI> superContexts = new ArraySet<>();
    for (final Statement statement : getStatements()) {
      if (statement.getPredicate().toString().equals(Constants.TERM_GENL_MT)) {
        superContexts.add((URI) statement.getObject());
      }
    }
    return superContexts;
  }

}
