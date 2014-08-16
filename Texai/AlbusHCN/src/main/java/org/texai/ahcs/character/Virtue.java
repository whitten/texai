/*
 * Virtue.java
 *
 * Created on Feb 2, 2009, 8:05:54 AM
 *
 * Description: Provides a virtue description for a skill.
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

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;

/** Provides a virtue description for a skill.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class Virtue {

  /** the statements that represent this virtue */
  private final List<Statement> statements = new ArrayList<Statement>();

  /** Constructs a new Virtue instance. */
  public Virtue() {
  }

  /** Gets the statements that represent this virtue.
   *
   * @return the statements that represent this virtue
   */
  public List<Statement> getStatements() {
    return statements;
  }
}
