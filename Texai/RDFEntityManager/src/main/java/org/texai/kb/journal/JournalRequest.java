/*
 * JournalRequest.java
 *
 * Created on Mar 17, 2009, 10:06:21 AM
 *
 * Description: Provides a journal request.
 *
 * Copyright (C) Mar 17, 2009 Stephen L. Reed.
 */
package org.texai.kb.journal;

import net.jcip.annotations.Immutable;
import org.openrdf.model.Statement;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFUtility;

/** Provides a journal request.
 *
 * @author Stephen L. Reed
 */
@Immutable
public final class JournalRequest {

  /** the repository name */
  private final String repositoryName;
  /** the repository journaling operation, i.e. add, remove */
  private final String operation;
  /** the statement to be journaled */
  private final Statement statement;

  /** Constructs a new JournalRequest instance.
   *
   * @param repositoryName the repository name
   * @param operation the repository journaling operation, i.e. add, remove
   * @param statement the statement to be journaled
   */
  public JournalRequest(
          final String repositoryName,
          final String operation,
          final Statement statement) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert operation != null : "operation must not be null";
    assert operation.equals(Constants.ADD_OPERATION) || operation.equals(Constants.REMOVE_OPERATION) : "operation must be add or remove";
    assert statement != null : "statement must not be null";

    this.repositoryName = repositoryName;
    this.operation = operation;
    this.statement = statement;
  }

  /** Gets the repository name.
   *
   * @return the repository name
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** Gets the repository journaling operation, i.e. add, remove.
   *
   * @return the repository journaling operation, i.e. add, remove
   */
  public String getOperation() {
    return operation;
  }

  /** Gets the statement to be journaled.
   *
   * @return the statement to be journaled
   */
  public Statement getStatement() {
    return statement;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(repositoryName);
    stringBuilder.append(" ");
    stringBuilder.append(operation);
    stringBuilder.append(" ");
    stringBuilder.append(RDFUtility.formatStatementAsTurtle(statement));
    return stringBuilder.toString();
  }
}
