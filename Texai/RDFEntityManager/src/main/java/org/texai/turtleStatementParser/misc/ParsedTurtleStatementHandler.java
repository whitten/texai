/*
 * ParsedTurtleStatementHandler.java
 *
 * Created on Jan 14, 2013, 3:32:41 PM
 *
 * Description: Defines a parsed turtle statement handler.
 *
 * Copyright (C) Jan 14, 2013, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.turtleStatementParser.misc;

import org.openrdf.model.Statement;

/** Defines a parsed turtle statement handler.
 *
 * @author reed
 */
public interface ParsedTurtleStatementHandler {

  /** Handles a parsed turtle statement.
   *
   * @param statement the statement
   */
  void handleStatement(final Statement statement);
}
