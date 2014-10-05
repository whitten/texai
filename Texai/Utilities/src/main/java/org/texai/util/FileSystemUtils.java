/*
 * FileSystemUtils.java
 *
 * Created on October 4, 2006, 8:08 AM
 *
 * Description: Spring Framework file system utilities.
 *
 * Copyright (C) 2009 Stephen L. Reed.
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

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

/**
 * Utility methods for working with the file system.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.5.3
 */
public final class FileSystemUtils {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(FileSystemUtils.class);

  /**
   * This class is never instantiated.
   */
  private FileSystemUtils() {
  }

  /**
   * Deletes the supplied {@link File} - for directories, recursively delete any nested directories or files as well.
   *
   * @param root the root <code>File</code> to delete
   *
   * @return <code>true</code> if the <code>File</code> was deleted, otherwise <code>false</code>
   */
  public static boolean deleteRecursively(final File root) {
    //Preconditions
    if (root == null) {
      throw new InvalidParameterException("root must not be null");
    }

    if (root.exists()) {
      if (root.isDirectory()) {
        final File[] children = root.listFiles();
        if (children != null) {
          for (final File child : children) {
            deleteRecursively(child);
          }
        }
      }
      return root.delete();
    }
    return false;
  }

  /**
   * Archives the given source directory to the given destination directory, without overwriting unchanged files.
   *
   * @param sourceDirectory the source directory
   * @param destinationDirectory the destination directory
   */
  public static void archiveDirectory(
          final File sourceDirectory,
          final File destinationDirectory) {
    //Preconditions
    assert sourceDirectory != null : "sourceDirectory must not be null";
    assert sourceDirectory.exists() : "sourceDirectory must exist";
    assert destinationDirectory != null : "destinationDirectory must not be null";

    if (destinationDirectory.exists()) {
      if (destinationDirectory.isDirectory() == false) {
        throw new TexaiException("Destination '" + destinationDirectory + "' exists but is not a directory");
      }
    } else if (destinationDirectory.mkdirs() == false) {
      throw new TexaiException("Destination '" + destinationDirectory + "' directory cannot be created");
    }
    if (destinationDirectory.canWrite() == false) {
      throw new TexaiException("Destination '" + destinationDirectory + "' cannot be written to");
    }

    final File[] files = sourceDirectory.listFiles();
    if (files == null) {  // null if security restricted
      throw new TexaiException("Failed to list contents of " + sourceDirectory);
    }
    for (final File sourceFile : files) {
      final File destinationFile = new File(destinationDirectory, sourceFile.getName());
      if (sourceFile.isDirectory()) {
        // recurse
        archiveDirectory(sourceFile, destinationFile);
      } else {
        if (!destinationFile.exists() || destinationFile.lastModified() < sourceFile.lastModified()) {
          try {
            LOGGER.info("copy " + sourceFile + " to " + destinationFile);
            FileUtils.copyFile(sourceFile, destinationFile);
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
      }
    }

    //Postconditions
    assert destinationDirectory.exists() : "destinationDirectory must exist";
  }
}
