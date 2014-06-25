/*
 * Rationale.java
 *
 * Created on Feb 2, 2009, 8:06:04 AM
 *
 * Description: Provides a rationale, for example to explain an observed behavior, or to justify a
 * possible behavior.
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

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;

/**
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class Rationale {

  /** the statements representing the rationale */
  private final List<Statement> statements;

  /** Constructs a new Rationale instance.
   *
   * @param statements the statements representing the rationale
   */
  public Rationale(final List<Statement> statements) {
    //Preconditions
    assert statements != null : "statements must not be null";
    assert !statements.isEmpty() : "statements must not be empty";

    this.statements = statements;
  }

  /** Gets the statements representing the rationale.
   *
   * @return the statements representing the rationale
   */
  public List<Statement> getStatements() {
    return statements;
  }
}
