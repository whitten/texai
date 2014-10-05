/*
 * ConciseLoggingFormatter.java
 *
 * Created on October 2, 2006, 7:51 PM
 *
 * Description:  Provides a concise format for Java log records, consisting of only the log message.  Not used
 * with Log4J.
 *
 * Copyright (C) 2006 Stephen L. Reed.
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
package org.texai.util;

import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * Provides a concise format for Java log records, consisting of only the log message. Not used with Log4J.
 *
 * @author reed
 */
public final class ConciseLoggingFormatter extends SimpleFormatter {

  /**
   * Creates a new instance of ConciseLoggingFormatter.
   */
  public ConciseLoggingFormatter() {
    super();
  }

  /**
   * Formats the given log record.
   *
   * @param logRecord the log record
   *
   * @return the formatted log record
   */
  @Override
  public String format(final LogRecord logRecord) {
    final StringBuilder stringBuilder = new StringBuilder(200);
    if (logRecord == null) {
      stringBuilder.append("null logRecord received by ConciseLoggingFormatter");
    } else if (logRecord.getMessage() == null) {
      stringBuilder.append("null logRecord message received by ConciseLoggingFormatter");
    } else {
      stringBuilder.append(logRecord.getMessage());
    }
    stringBuilder.append('\n');
    return stringBuilder.toString();
  }
}
