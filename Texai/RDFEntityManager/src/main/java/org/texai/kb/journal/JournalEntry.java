/*
 * JournalEntry.java
 *
 * Created on Mar 17, 2009, 9:57:29 AM
 *
 * Description: Provides a journal entry.
 *
 * Copyright (C) Mar 17, 2009 Stephen L. Reed.
 */
package org.texai.kb.journal;

import net.jcip.annotations.Immutable;
import org.joda.time.DateTime;
import org.openrdf.model.Statement;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFUtility;
import org.texai.turtleStatementParser.ParseException;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.util.TexaiException;

/** Provides a journal entry.
 *
 * @author Stephen L. Reed
 */
@Immutable
public final class JournalEntry {

  /** the timestamp */
  private final DateTime dateTime;
  /** the suffix number */
  private final int suffixNbr;
  /** the knowledge base operation */
  private final String operation;
  /** the statement */
  private final Statement statement;

  /** Constructs a new JournalEntry instance.
   *
   * @param dateTime the timestamp
   * @param suffixNbr the suffix number
   * @param operation the knowledge base operation
   * @param statement the statement
   */
  public JournalEntry(
          final DateTime dateTime,
          final int suffixNbr,
          final String operation,
          final Statement statement) {
    //Preconditions
    assert dateTime != null : "dateTime must not be null";
    assert suffixNbr >= 0 : "suffixNbr must not be negative";
    assert operation != null : "operation must not be null";
    assert operation.equals(Constants.ADD_OPERATION) || operation.equals(Constants.REMOVE_OPERATION) : "operation must be add or remove";
    assert statement != null : "statement must not be null";

    this.dateTime = dateTime;
    this.suffixNbr = suffixNbr;
    this.operation = operation;
    this.statement = statement;
  }

  /** Returns the journal entry parsed from the given string representation.
   *
   * <code>
   * 2009-03-18T16:48:55.291-05:00 1 add cyc:arity rdf:type cyc:BinaryPredicate . in cyc:UniversalVocabularyMt
   * </code>
   *
   * @param string the given string representation
   * @return the journal entry
   */
  public static JournalEntry parse(final String string) {
    //Preconditions
    assert string != null : "string must not be null";
    assert !string.isEmpty() : "string must not be empty";

    int index = string.indexOf(' ');
    final String dateTimeString = string.substring(0, index);
    final DateTime dateTime = new DateTime(dateTimeString);

    index++;
    int index2 = string.indexOf(' ', index);
    final String suffixNbrString = string.substring(index, index2);
    final int suffixNbr = Integer.parseInt(suffixNbrString);

    index = index2 + 1;
    index2 = string.indexOf(' ', index);
    final String operation = string.substring(index, index2);

    index2++;
    final String statementString = string.substring(index2);
    final TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(statementString);
    Statement statement = null;
    try {
      statement = turtleStatementParser.Statements().get(0);
    } catch (final ParseException ex) {
      throw new TexaiException(ex);
    }

    return new JournalEntry(
            dateTime,
            suffixNbr,
            operation,
            statement);
  }

  /** Gets the timestamp.
   *
   * @return the timestamp
   */
  public DateTime getDateTime() {
    return dateTime;
  }

  /** Gets the suffix number.
   *
   * @return the suffix number
   */
  public int getSuffixNbr() {
    return suffixNbr;
  }

  /** Gets the knowledge base operation.
   *
   * @return the knowledge base operation
   */
  public String getOperation() {
    return operation;
  }

  /** Gets the statement.
   *
   * @return the statement
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
    stringBuilder.append(dateTime);
    stringBuilder.append(" ");
    stringBuilder.append(suffixNbr);
    stringBuilder.append(" ");
    stringBuilder.append(operation);
    stringBuilder.append(" ");
    stringBuilder.append(RDFUtility.formatStatementAsTurtle(statement));
    return stringBuilder.toString();
  }
}
