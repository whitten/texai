/*
 * Directory.java
 *
 * Created on Apr 14, 2009, 3:28:52 PM
 *
 * Description: Provides an HTML view of a directory.
 *
 * Copyright (C) Apr 14, 2009 Stephen L. Reed.  Adapted from http://www.simpleframework.org/
 * Directory class.
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
package org.texai.webserver;

import java.io.File;
import java.io.UnsupportedEncodingException;
import net.jcip.annotations.NotThreadSafe;
import org.texai.util.TexaiException;

/** Provides an HTML view of a directory.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class Directory {

  /** Constructs a new Directory instance. */
  public Directory() {
  }

  /** Gets the contents of a directory.
   *
   * @param root the directory root
   * @param target the target
   * @return the directory listing formatted in HTML
   */
  public byte[] getContents(
          final File root,
          final String target) {
    //Preconditions
    assert target != null : "target must not be null";

    final File directory = new File(root, target.replace('/', File.separatorChar));
    final String[] names = directory.list();

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n" + "<HTML><HEAD>" + "<TITLE>Index of ").
            append(target).
            append("</TITLE>\n" + "</HEAD><BODY>" + "<H1>Index of ").
            append(target).
            append(
            "</H1>\n" +
            "<HR><TABLE>" +
            "<TR><TD><B>Name</B></TD>" +
            "<TD><B>Size</B></TD>");


    for (String name1 : names) {
      final File file = new File(directory, name1);
      final boolean isDirectory = file.isDirectory();
      String name = name1;
      if (isDirectory) {
        name = name + "/";
      }
      final String size = isDirectory ? "-" : String.valueOf(file.length());
      stringBuilder.append("<TR><TD><TT><A HREF=\"");
      stringBuilder.append(target);
      stringBuilder.append(name);
      stringBuilder.append("\">");
      stringBuilder.append(name);
      stringBuilder.append("</A></TT></TD><TD><TT>");
      stringBuilder.append(size);
      stringBuilder.append("</TT></TD></TR>\n");
    }
    stringBuilder.append("</TABLE><HR>" + "</BODY></HTML>");
    return getBytes(stringBuilder.toString());
  }

  /** Returns the bytes that constitute the given text.
   *
   * @param text the given text
   * @return the bytes that constitute the given text.
   */
  private byte[] getBytes(final String text) {
    try {
      return text.getBytes("utf-8");
    } catch (final UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the content type of the formatted directory listing.
   *
   * @return the content type.
   */
  public String getContentType() {
    return "text/html; charset=utf-8";
  }
}
