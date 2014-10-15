/*
 * AlphaMemory.java
 *
 * Created on Aug 12, 2010, 8:48:29 PM
 *
 * Description: Provides an alpha memory for the Rete matching algorithm.
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

import java.util.LinkedList;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;

/** Provides an alpha memory for the Rete matching algorithm.
 *
 * @author reed
 */
@NotThreadSafe
public class AlphaMemory implements Comparable<AlphaMemory> {

  /** the pattern for matching */
  private final String pattern;
  /** the statements that match the pattern */
  private final LinkedList<Statement> statements = new LinkedList<>();
  /** the successor nodes, e.g. join nodes */
  private final LinkedList<JoinNode> successors = new LinkedList<>();
  /** the graph id */
  private int id;
  /** the not sameTerm constraints */
  private final Set<Value> notSameTerms;

  /** Constructs a new AlphaMemory instance.
   *
   * @param pattern the pattern for matching
   * @param notSameTermAsDictionary the not sameTerm constraints
   */
  public AlphaMemory(
          final String pattern,
          final Set<Value> notSameTerms) {
    //Preconditions
    assert pattern != null : "pattern must not be null";
    assert !pattern.isEmpty() : "pattern must not be empty";
    assert notSameTerms != null : "notSameTerms must not be null";

    this.pattern = pattern;
    this.notSameTerms = notSameTerms;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[AlphaMemory " + pattern + ']';
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AlphaMemory other = (AlphaMemory) obj;
    if ((this.pattern == null) ? (other.pattern != null) : !this.pattern.equals(other.pattern)) {
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
    int hash = 5;
    hash = 53 * hash + (this.pattern != null ? this.pattern.hashCode() : 0);
    return hash;
  }

  /** Gets the pattern which the stored statements match.
   *
   * @return the pattern
   */
  public String getPattern() {
    return pattern;
  }

  /** Gets the statements stored in this alpha memory.
   *
   * @return the statements
   */
  public LinkedList<Statement> getStatements() {
    return statements;
  }

  /** Gets the successor nodes, e.g. join nodes.
   *
   * @return the successors
   */
  public LinkedList<JoinNode> getSuccessors() {
    return successors;
  }

  /** Compares another alpha memory with this one.
   *
   * @param that the other alpha memory
   * @return -1 if less than, 0 if equal, otherwise return -1
   */
  @Override
  public int compareTo(final AlphaMemory that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.pattern.compareTo(that.pattern);
  }

  /** Gets the graph id.
   *
   * @return the graph id
   */
  public int getId() {
    return id;
  }

  /** Sets the graph id.
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /** Gets the not sameTerm constraints.
   *
   * @return the not sameTerm constraints
   */
  public Set<Value> getNotSameTerms() {
    return notSameTerms;
  }
}
