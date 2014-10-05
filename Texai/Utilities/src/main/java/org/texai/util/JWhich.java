/*
 * JWhich.java
 *
 * Created on October 4, 2006, 8:08 AM
 *
 * Description: Classpath checking utility
 *
 * Adapted from Mike Clark's article: http://www.javaworld.com/javaworld/javatips/jw-javatip105.html
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

import java.net.URL;
import org.apache.log4j.Logger;

/**
 *
 * @author reed
 */
public final class JWhich {

  /**
   * Creates a new instance of JWhich.
   */
  public JWhich() {
  }

  /**
   * Finds the absolute pathname of the class file containing the specified class name, as determined by the current classpath.
   *
   * @param className the class name
   *
   * @return the absolute pathname description or an error message
   */
  public String which(final String className) {
    // Preconditions
    assert className != null : "className must not be null";

    String myClassName = className;
    if (myClassName.charAt(0) != '/') {
      myClassName = "/" + myClassName;
    }
    myClassName = myClassName.replace('.', '/');
    myClassName = myClassName + ".class";
    final URL classUrl = JWhich.class.getResource(myClassName);
    String pathname;
    if (classUrl == null) {
      pathname = "\nClass '" + className + "' not found in the following classpath entries:"
              + formatClasspath(System.getProperty("java.class.path"));
    } else {
      pathname = "\nClass '" + className + "' found in \n'" + classUrl.getFile() + "'";
    }
    return pathname;
  }

  /**
   * Returns a formatted classpath string.
   *
   * @param classpath the given classpath to format
   *
   * @return a formatted classpath string
   */
  protected String formatClasspath(final String classpath) {
    // Preconditions
    assert classpath != null : "classpath must not be null";

    int beginIndex = 0;
    @SuppressWarnings("UnusedAssignment")
    int endIndex = 0;
    final StringBuilder stringBuilder = new StringBuilder(1000);
    do {
      endIndex = classpath.indexOf(':', beginIndex);
      stringBuilder.append("\n    ");
      if (endIndex == -1) {
        stringBuilder.append(classpath.substring(beginIndex));
      } else {
        stringBuilder.append(classpath.substring(beginIndex, endIndex));
        beginIndex = endIndex + 1;
      }
    } while (endIndex > -1);

    // Postconditions
    assert stringBuilder.length() > 0 : "formatted class path must not be an empty string";

    return stringBuilder.toString();
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments, the first one is the class name to find
   */
  public static void main(final String[] args) {
    final Logger logger = Logger.getLogger(JWhich.class);
    if (args.length > 0) {
      logger.info((new JWhich()).which(args[0]));
    } else {
      logger.warn("Usage: java JWhich <classname>");
    }
  }
}
