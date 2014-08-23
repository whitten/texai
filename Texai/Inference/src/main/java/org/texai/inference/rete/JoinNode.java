/*
 * JoinNode.java
 *
 * Created on Aug 12, 2010, 10:00:10 PM
 *
 * Description: Provides a join node for the Rete algorithm.
 *
 * Copyright (C) Aug 12, 2010, Stephen L. Reed.
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

import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.texai.inference.domainEntity.Statement;

/** Provides a join node for the Rete algorithm.
 *
 * @author reed
 */
@NotThreadSafe
public class JoinNode extends AbstractReteNode {

  /** the alpha memory */
  private final AlphaMemory alphaMemory;
  /** the tests at this join node */
  private final List<TestAtJoinNode> tests;
  /** the condition */
  private final Statement condition;

  /** Constructs a new JoinNode instance. */
  public JoinNode(
          final AbstractReteNode parent,
          final AlphaMemory alphaMemory,
          final List<TestAtJoinNode> tests,
          final Statement condition) {
    super(parent);
    //Preconditions
    assert alphaMemory != null : "alphaMemory must not be null";
    assert tests != null : "tests must not be null";
    assert condition != null : "condition must not be null";

    this.alphaMemory = alphaMemory;
    this.tests = new ArrayList<>(tests);
    this.condition = condition;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[JoinNode ");
    stringBuilder.append(condition);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Gets the alpha memory.
   *
   * @return the alpha memory
   */
  public AlphaMemory getAlphaMemory() {
    return alphaMemory;
  }

  /** Gets the tests at this join node.
   *
   * @return the tests at this join node
   */
  public List<TestAtJoinNode> getTests() {
    return tests;
  }

  /** Gets the condition.
   *
   * @return the condition
   */
  public Statement getCondition() {
    return condition;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final JoinNode other = (JoinNode) obj;
    if (this.alphaMemory != other.alphaMemory && (this.alphaMemory == null || !this.alphaMemory.equals(other.alphaMemory))) {
      return false;
    }
    if (this.tests != other.tests && (this.tests == null || !this.tests.equals(other.tests))) {
      return false;
    }
    if (this.condition != other.condition && (this.condition == null || !this.condition.equals(other.condition))) {
      return false;
    }
    return true;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 23 * hash + (this.alphaMemory != null ? this.alphaMemory.hashCode() : 0);
    hash = 23 * hash + (this.tests != null ? this.tests.hashCode() : 0);
    hash = 23 * hash + (this.condition != null ? this.condition.hashCode() : 0);
    return hash;
  }
}
