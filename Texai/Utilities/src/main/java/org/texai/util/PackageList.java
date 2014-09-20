/*
 * PackageList.java
 *
 * Created on May 4, 2010, 1:11:21 PM
 *
 * Description: Merges two input comma-delimited lists into an ordered output list.
 *
 * Copyright (C) May 4, 2010, Stephen L. Reed.
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
package org.texai.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;

/** Merges two input comma-delimited lists into an ordered output list.
 *
 * @author reed
 */
@NotThreadSafe
public class PackageList {

  // the first comma-delimited list
  private String list1;
  // the second comma-delimited list
  private String list2;
  // the merged entries string builder
  final StringBuilder stringBuilder = new StringBuilder();
  // the ordered entries
  private final List<String> entries = new ArrayList<>();

  /** Constructs a new PackageList instance. */
  public PackageList() {
  }

  /** Merges two input comma-delimited lists into an ordered output list. */
  private void process() {
    final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    System.out.println("Enter first comma-delimited list");
    try {
      list1 = bufferedReader.readLine().trim();
      System.out.println("Enter second comma-delimited list.");
      list2 = bufferedReader.readLine().trim();
      merge(list1);
      merge(list2);
      Collections.sort(entries);
      boolean isFirst = true;
      for (final String entry : entries) {
        if (isFirst) {
          isFirst = false;
        } else {
          stringBuilder.append(", ");
        }
        stringBuilder.append(entry);
      }
      System.out.println("\nMerged comma-delimited list ...\n" + stringBuilder.toString());
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
      System.err.println(StringUtils.getStackTraceAsString(ex));
      System.exit(1);
    }
  }

  private void merge(final String list) {
    //Preconditions
    assert list != null : "list must not be null";

    final String delimitedEntries[] = list.split(",");
    for (final String delimitedEntry : delimitedEntries) {
      final String trimmedDelimitedEntry = delimitedEntry.trim();
      if (!entries.contains(trimmedDelimitedEntry)) {
        entries.add(trimmedDelimitedEntry);
      }
    }
  }



  /** Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final PackageList packageList = new PackageList();
    packageList.process();
  }
}
