/*
 * Token.java
 *
 * Created on Aug 13, 2010, 6:43:00 AM
 *
 * Description: Provides a token for the Rete algorithm.
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

import net.jcip.annotations.Immutable;
import org.openrdf.model.Statement;
import org.texai.kb.persistence.RDFUtility;

/** Provides a token for the Rete algorithm.
 *
 * @author reed
 */
@Immutable
public class Token {

  /** the parent token, or null if topmost */
  private final Token parent;
  /** the statement */
  private final Statement statement;
  /** the bound variable of the statement subject */
  private final String subjectVariableName;
  /** the bound variable of the statement object,
   * or null if the corresponding join condition has a constant term in the object position */
  private final String objectVariableName;
  /** the indicator whether this token is a member of the query satisfaction set */
  private boolean isMemberOfSatisfactionSet;

  /** Constructs a new Token instance, as a dummy. */
  public Token() {
    parent = null;
    statement = null;
    subjectVariableName = null;
    objectVariableName = null;
  }

  /** Constructs a new Token instance.
   *
   * @param parent the parent token, or null if topmost
   * @param statement the statement
   */
  public Token(
          final Token parent,
          final Statement statement,
          final String subjectVariableName,
          final String objectVariableName) {
    //Preconditions
    assert statement != null : "statement must not be null";
    assert subjectVariableName != null : "subjectVariableName must not be null";
    assert !subjectVariableName.isEmpty() : "statement must not be empty";

    this.parent = parent;
    this.statement = statement;
    this.subjectVariableName = subjectVariableName;
    this.objectVariableName = objectVariableName;
  }

  /** Gets the parent token, or null if topmost.
   *
   * @return the parent token, or null if topmost
   */
  public Token getParent() {
    return parent;
  }

  /** Gets the statement.
   * 
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }

  /** Gets the bound variable of the statement subject.
   *
   * @return the bound variable of the statement subject
   */
  public String getSubjectVariableName() {
    return subjectVariableName;
  }

  /** Gets the bound variable of the statement object.
   *
   * @return the bound variable of the statement object,
   * or null if the corresponding join condition has a constant term in the object position
   */
  public String getObjectVariableName() {
    return objectVariableName;
  }

  /** Returns whether this is a dummy token.
   * 
   * @return whether this is a dummy token
   */
  public boolean isDummy() {
    return statement == null;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    if (isDummy()) {
      return "[dummy token]";
    }
    stringBuilder.append("Token ");
    stringBuilder.append(RDFUtility.formatStatement(statement));
    stringBuilder.append(' ');
    stringBuilder.append(subjectVariableName);
    stringBuilder.append('=');
    stringBuilder.append(RDFUtility.formatResource(statement.getSubject()));
    if (objectVariableName != null) {
      stringBuilder.append(' ');
      stringBuilder.append(objectVariableName);
      stringBuilder.append('=');
      stringBuilder.append(RDFUtility.formatValue(statement.getObject()));
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Returns a string representation of this object suitable for a GraphViz label.
   *
   * @return a string representation of this object suitable for a GraphViz label
   */
  public String graphLabel() {
    final StringBuilder stringBuilder = new StringBuilder();
    if (isDummy()) {
      return "[dummy token]";
    }
    if (isMemberOfSatisfactionSet) {
      stringBuilder.append("* ");
    }
    stringBuilder.append(subjectVariableName);
    stringBuilder.append('=');
    stringBuilder.append(RDFUtility.formatResource(statement.getSubject()));
    if (objectVariableName != null) {
      stringBuilder.append(' ');
      stringBuilder.append(objectVariableName);
      stringBuilder.append('=');
      stringBuilder.append(RDFUtility.formatValue(statement.getObject()));
    }
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
    final Token other = (Token) obj;
    if (this.statement != other.statement && (this.statement == null || !this.statement.equals(other.statement))) {
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
    int hash = 3;
    hash = 97 * hash + (this.statement != null ? this.statement.hashCode() : 0);
    return hash;
  }

  /** Gets the indicator whether this token is a member of the query satisfaction set.
   *
   * @return the indicator whether this token is a member of the query satisfaction set
   */
  public boolean isMemberOfSatisfactionSet() {
    return isMemberOfSatisfactionSet;
  }

  /** Sets the indicator whether this token is a member of the query satisfaction set.
   *
   * @param isMemberOfSatisfactionSet the indicator whether this token is a member of the query satisfaction set
   */
  public void setIsMemberOfSatisfactionSet(boolean isMemberOfSatisfactionSet) {
    this.isMemberOfSatisfactionSet = isMemberOfSatisfactionSet;
  }
}
