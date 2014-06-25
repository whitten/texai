/*
 * ProposedAction.java
 *
 * Created on Feb 2, 2009, 8:17:05 AM
 *
 * Description: Provides a representation of a proposed behavior.
 *
 * Copyright (C) Feb 2, 2009 Stephen L. Reed.
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
package org.texai.ahcs.character;

import java.beans.Statement;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;

/** Provides a representation of a proposed behavior.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class ProposedBehavior {

  /** the statements that represent this observed behavior */
  private final List<Statement> statements;

  /** Constructs a new ProposedBehavior instance.
   *
   * @param statements the statements representing the proposed behavior
   */
  public ProposedBehavior(final List<Statement> statements) {
    //Preconditions
    assert statements != null : "statements must not be null";
    assert !statements.isEmpty() : "statements must not be empty";

    this.statements = statements;
  }

  /** Gets the statements that represent this proposed behavior.
   *
   * @return the statements that represent this proposed behavior
   */
  public List<Statement> getStatements() {
    return statements;
  }
}
