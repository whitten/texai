/*
 * RepositoryArchiver.java
 *
 * Created on Mar 18, 2009, 9:19:14 AM
 *
 * Description: Provides a repository archiver.
 *
 * Copyright (C) Mar 18, 2009 Stephen L. Reed.
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
package org.texai.kb.journal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.texai.util.TexaiException;

/** Provides a repository archiver.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class RepositoryArchiver {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RepositoryArchiver.class);
  /** the repository data directory path */
  private final String dataDirectoryPath;

  /** Constructs a new RepositoryArchiver instance.
   *
   * @param dataDirectoryPath the repository data directory path
   */
  public RepositoryArchiver(final String dataDirectoryPath) {
    //Preconditions
    assert dataDirectoryPath != null : "dataDirectoryPath must not be null";
    assert !dataDirectoryPath.isEmpty() : "dataDirectoryPath must not be empty";

    this.dataDirectoryPath = dataDirectoryPath;
  }

  /** Constructs a new RepositoryArchiver instance.
   *
   * @param repository the repository
   */
  public RepositoryArchiver(final Repository repository) {
    //Preconditions
    assert repository != null : "repository must not be null";

    dataDirectoryPath = repository.getDataDir().getAbsolutePath();
  }

  /** Archives the repository. */
  public void archive() {
    // open data directory
    final File dataDirectory = new File(dataDirectoryPath);
    assert dataDirectory.isDirectory() : "dataDirectoryPath must be a directory " + dataDirectoryPath;

    // open archive directory
    final String absolutePath = dataDirectory.getAbsolutePath();
    final File archiveDirectory;
    if (absolutePath.contains("repositories")) {
      archiveDirectory = new File(absolutePath.replace("repositories", "archiveRepositories"));
    } else if (absolutePath.contains("Repositories")) {
      archiveDirectory = new File(absolutePath.replace("Repositories", "archiveRepositories"));
    } else {
      throw new TexaiException("invalid repositories path for archiving");
    }

    if (archiveDirectory.exists()) {
      final File[] files = archiveDirectory.listFiles();
      for (final File file : files) {
        LOGGER.info("deleting previous archive file: " + file);
        final boolean wasFileDeleted = file.delete();
        if (!wasFileDeleted) {
          throw new TexaiException("previous archive file was not deleted: " + file);
        }
      }
    } else {
      LOGGER.info("  creating " + archiveDirectory);
      final boolean wasDirectoryCreated = archiveDirectory.mkdirs();
      if (!wasDirectoryCreated) {
        throw new TexaiException("archive directory was not created: " + archiveDirectory);
      }
    }
    assert archiveDirectory.isDirectory() : "archiveDirectory must exist " + archiveDirectory;

    // copy files
    for (final File dataFile : dataDirectory.listFiles()) {
      final String dataFileName = dataFile.getName();
      if (dataFileName.equals("lock") || dataFileName.equals(".svn")) {
        continue;
      }
      final File archiveFile;
      if (absolutePath.contains("repositories")) {
        archiveFile = new File(dataFile.getAbsolutePath().replace("repositories", "archiveRepositories"));
      } else {
        archiveFile = new File(dataFile.getAbsolutePath().replace("Repositories", "archiveRepositories"));
      }
      LOGGER.info("copying " + dataFile + " to " + archiveFile);
      try {
        if (!archiveFile.exists()) {
          LOGGER.info("  creating " + archiveFile);
          final boolean wasFileCreated = archiveFile.createNewFile();
          if (!wasFileCreated) {
            throw new TexaiException("archive file was not created: " + archiveFile);
          }
        }
        FileChannel dataFileChannel = null;
        FileChannel archiveFileChannel = null;
        try {
          dataFileChannel = new FileInputStream(dataFile).getChannel();
          archiveFileChannel = new FileOutputStream(archiveFile).getChannel();
          archiveFileChannel.transferFrom(dataFileChannel, 0, dataFileChannel.size());
        } finally {
          if (dataFileChannel != null) {
            dataFileChannel.close();
          }
          if (archiveFileChannel != null) {
            archiveFileChannel.close();
          }
        }
      } catch (final IOException ex) {
        throw new TexaiException(ex);
      }
    }
  }
}
