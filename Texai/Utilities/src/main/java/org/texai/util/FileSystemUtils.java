/*
 * FileSystemUtils.java
 *
 * Created on October 4, 2006, 8:08 AM
 *
 * Description: Spring Framework file system utilities.
 *
 * Copyright (C) 2009 Stephen L. Reed.
 */
package org.texai.util;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

  /** Returns a dated file name with the given prefix, e.g. manifest-2014-01-12.
   *
   * @param prefix the given prefix, e.g. "manifest"
   * @return a dated file name with the given prefix, e.g. manifest-2014-01-12
   */
  public static String formDatedFileName(final String prefix) {
    //Preconditions
    assert StringUtils.isNonEmptyString(prefix) : "prefix must be a non-null string";

    DateTimeFormatter dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-DD");
    final StringBuilder stringBuilder = new StringBuilder();
    return stringBuilder
            .append(prefix)
            .append('-')
            .append(dateTimeFormat.print(new DateTime()))
            .toString();
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
