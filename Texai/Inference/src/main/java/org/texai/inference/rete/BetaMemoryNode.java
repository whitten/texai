/*
 * BetaMemoryNode.java
 *
 * Created on Aug 13, 2010, 6:18:57 AM
 *
 * Description: Provides a beta memory node for the Rete algorithm.
 *
 * Copyright (C) Aug 13, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.inference.rete;

import java.util.LinkedList;
import net.jcip.annotations.NotThreadSafe;

/** Provides a beta memory node for the Rete algorithm.
 *
 * @author reed
 */
@NotThreadSafe
public class BetaMemoryNode extends AbstractReteNode implements TokenMemory {

  /** the tokens */
  private final LinkedList<Token> tokens = new LinkedList<>();

  /** Constructs a new BetaMemoryNode instance.
   *
   * @param parent the parent node
   */
  public BetaMemoryNode(final AbstractReteNode parent) {
    super(parent);
  }

  /** Gets the tokens.
   *
   * @return the tokens
   */
  @Override
  public LinkedList<Token> getTokens() {
    return tokens;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[BetaMemoryNode ");
    stringBuilder.append(tokens);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }
}
