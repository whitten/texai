/*
 * TestAtJoinNode.java
 *
 * Created on Aug 12, 2010, 9:50:37 PM
 *
 * Description: Provides a test-at-join-node container for the Rete algorithm.
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

import net.jcip.annotations.Immutable;
import org.texai.inference.domainEntity.Statement;

/** Provides a test-at-join-node container for the Rete algorithm, which specifies the locations of the two fields
 * whose values must be equal for some variable to be bound consistently.
 *
 * @author reed
 */
@Immutable
public class TestAtJoinNode {

  /** the variable name */
  private final String variableName;

  /** the indicator of where the variable is within the RDF triple that is the condition */
  public enum FieldType {SUBJECT, OBJECT}
  /** the argument 1 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT */
  private final FieldType arg1FieldType;
  /** the number of levels up above the current condition in the earlier conditions, of argument 2 */
  private final int nbrOfLevelsUp;
  /** the argument 2 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT */
  private final FieldType arg2FieldType;
  /** the expected condition indicated by the nbrOfLevelsUp field, which is optionally used for diagnostics */
  private Statement condition;

  /** Constructs a new TestAtJoinNode instance.
   *
   * @param variableName the variable name
   * @param arg1FieldType the argument 1 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT
   * @param nbrOfLevelsUp the number of levels up above the current condition in the earlier conditions, of argument 2
   * @param arg2FieldType the argument 2 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT
   */
  public TestAtJoinNode(
          final String variableName,
          final FieldType arg1FieldType,
          final int nbrOfLevelsUp,
          final FieldType arg2FieldType) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";
    assert arg1FieldType != null : "arg1FieldType must not be null";
    assert nbrOfLevelsUp >= 0 : "nbrOfLevelsUp must not be negative";
    assert arg2FieldType != null : "arg2FieldType must not be null";

    this.variableName = variableName;
    this.arg1FieldType = arg1FieldType;
    this.nbrOfLevelsUp = nbrOfLevelsUp;
    this.arg2FieldType = arg2FieldType;
  }
  
  /** Gets the argument 1 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT.
   *
   * @return the argument 1 field type
   */
  public FieldType getArg1FieldType() {
    return arg1FieldType;
  }

  /** Gets the number of levels up above the current condition in the earlier conditions, of argument 2.
   *
   * @return the number of levels up above the current condition in the earlier conditions, of argument 2
   */
  public int getNbrLevelsUp() {
    return nbrOfLevelsUp;
  }

  /** Gets the argument 2 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT.
   *
   * @return the argument 2 field type
   */
  public FieldType getArg2FieldType() {
    return arg2FieldType;
  }

  /** Gets the expected condition indicated by the nbrOfLevelsUp field, which is optionally used for diagnostics.
   *
   * @return the expected condition indicated by the nbrOfLevelsUp field
   */
  public Statement getCondition() {
    return condition;
  }

  /** Sets the expected condition indicated by the nbrOfLevelsUp field, which is optionally used for diagnostics.
   *
   * @param condition the expected condition indicated by the nbrOfLevelsUp field
   */
  public void setCondition(final Statement condition) {
    //Preconditions
    assert condition != null : "condition must not be null";

    this.condition = condition;
  }

  /** Gets the variable name.
   *
   * @return the variable name
   */
  public String getVariableName() {
    return variableName;
  }

  /** Returns a string representation of this object.
   * 
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[Test ");
    stringBuilder.append(variableName);
    stringBuilder.append(" arg1: ");
    if (arg1FieldType.equals(FieldType.SUBJECT)) {
      stringBuilder.append("subject");
    } else {
      assert arg1FieldType.equals(FieldType.OBJECT);
      stringBuilder.append("object");
    }
    stringBuilder.append(", arg2 levels up: ");
    stringBuilder.append(nbrOfLevelsUp);
    stringBuilder.append(", arg2: ");
    if (arg2FieldType.equals(FieldType.SUBJECT)) {
      stringBuilder.append("subject");
    } else {
      assert arg2FieldType.equals(FieldType.OBJECT);
      stringBuilder.append("object");
    }
    stringBuilder.append("]");
    return stringBuilder.toString();
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
    final TestAtJoinNode other = (TestAtJoinNode) obj;
    if (this.arg1FieldType != other.arg1FieldType) {
      return false;
    }
    if (this.nbrOfLevelsUp != other.nbrOfLevelsUp) {
      return false;
    }
    if (this.arg2FieldType != other.arg2FieldType) {
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
    hash = 23 * hash + (this.arg1FieldType != null ? this.arg1FieldType.hashCode() : 0);
    hash = 23 * hash + this.nbrOfLevelsUp;
    hash = 23 * hash + (this.arg2FieldType != null ? this.arg2FieldType.hashCode() : 0);
    return hash;
  }

}
