/*
 * JournalWriter.java
 *
 * Created on Mar 17, 2009, 9:56:20 AM
 *
 * Description: Provides a thread-safe journal writer.
 *
 * Copyright (C) Mar 17, 2009 Stephen L. Reed.
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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.texai.util.TexaiException;

/** Provides a thread-safe journal writer.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public final class JournalWriter {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalWriter.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the journal file writer dictionary, repository name --> print writer */
  private static final Map<String, PrintWriter> JOURNAL_FILE_WRITER_DICTIONARY = new HashMap<>();
  /** the indicator that the print writers are closed */
  private static boolean arePrintWritersClosed = false;
  /** the indicator whether to inefficiently, but safely, flush the buffer when each operation is written */
  private boolean isWrittenImmediately = true;
  /** the indicator whether this object is being unit tested */
  private boolean isUnitTest = false;
  /** the journal file path */
  private String journalFilePath;
  /** the journal requests of the current transaction */
  private final List<JournalRequest> transactionJournalRequests = new ArrayList<>();

  /** Constructs a new JournalWriter instance. */
  public JournalWriter() {
  }

  /** Deletes journal files. */
  public static void deleteJournalFiles() {
    final File directory = new File("./journals");
    if (!directory.exists()) {
      LOGGER.info("creating journals directory");
      final boolean wasDirectoryCreated = directory.mkdir();
      if (!wasDirectoryCreated) {
        throw new TexaiException("journals directory file was not created: " + directory);
      }
    }
    assert directory.exists() : "./journals does not exist, current working directory is " + System.getProperty("user.dir");
    final File[] files = directory.listFiles();
    assert files != null;
    for (final File file : files) {
      if (!file.isHidden()) {
        LOGGER.info("deleting file: " + file);
        final boolean wasFileDeleted = file.delete();
        if (!wasFileDeleted) {
          LOGGER.warn("journal file not deleted: " + file);
        }
      }
    }
  }

  /** Writes the given list of journal requests to each request's corresponding journal file in a critical section.
   *
   * @param journalRequests the given list of journal requests
   */
  public synchronized void write(final List<JournalRequest> journalRequests) {
    //Preconditions
    assert journalRequests != null : "journalRequests must not be null";

    transactionJournalRequests.addAll(journalRequests);
  }

  /** Commits the given list of journal requests to each request's corresponding journal file in a critical section. */
  public synchronized void commit() {
    final File directory = new File("./journals");
    if (!directory.exists()) {
      LOGGER.info("creating journals directory");
      final boolean wasDirectoryCreated = directory.mkdir();
      if (!wasDirectoryCreated) {
        throw new TexaiException("journals directory was not created: " + directory);
      }
    }
    assert directory.exists() : "./journals does not exist, current working directory is " + System.getProperty("user.dir");
    final DateTime dateTime = new DateTime();
    int suffixNbr = 0;
    for (final JournalRequest transactionJournalRequest : transactionJournalRequests) {
      PrintWriter journalFileWriter = JOURNAL_FILE_WRITER_DICTIONARY.get(transactionJournalRequest.getRepositoryName());
      if (journalFileWriter == null) {
        journalFilePath = "./journals/" + transactionJournalRequest.getRepositoryName() + "-" + (new DateTime()).toString().replace(':', '_') + ".jrnl";
        try {
          final File journalFile = new File(journalFilePath);
          if (!journalFile.exists()) {
            final boolean wasFileCreated = journalFile.createNewFile();
            if (!wasFileCreated) {
              throw new TexaiException("file was not created: " + journalFilePath);
            }
          }
          journalFileWriter = new PrintWriter(journalFilePath);
        } catch (final IOException ex) {
          LOGGER.error("problem with file " + journalFilePath);
          throw new TexaiException(ex);
        }
        JOURNAL_FILE_WRITER_DICTIONARY.put(transactionJournalRequest.getRepositoryName(), journalFileWriter);
      }
      final JournalEntry journalEntry = new JournalEntry(
              dateTime,
              ++suffixNbr,
              transactionJournalRequest.getOperation(),
              transactionJournalRequest.getStatement());
      final String journalEntryString = journalEntry.toString();
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("writing: " + journalEntryString);
      }
      journalFileWriter.println(journalEntryString);
      if (isWrittenImmediately) {
        journalFileWriter.flush();
      }
    }
    transactionJournalRequests.clear();
  }

  /** Rolls back the journal requests that belong to the current transaction. */
  public synchronized void rollback() {
    transactionJournalRequests.clear();
  }

  /** Closes the journal file writers. */
  public static synchronized void close() {
    if (!arePrintWritersClosed) {
      for (final PrintWriter journalFileWriter : JOURNAL_FILE_WRITER_DICTIONARY.values()) {
        journalFileWriter.flush();
        journalFileWriter.close();
      }
      arePrintWritersClosed = true;
      JOURNAL_FILE_WRITER_DICTIONARY.clear();
    }
  }

  /** Gets the indicator whether to inefficiently, but safely, flush the buffer when each operation is written.
   *
   * @return the indicator whether to inefficiently, but safely, flush the buffer when each operation is written
   */
  public boolean isWrittenImmediately() {
    return isWrittenImmediately;
  }

  /** Sets the indicator whether to inefficiently, but safely, flush the buffer when each operation is written.
   *
   * @param isWrittenImmediately the indicator whether to inefficiently, but safely, flush the buffer when each operation is written
   */
  public void setIsWrittenImmediately(final boolean isWrittenImmediately) {
    this.isWrittenImmediately = isWrittenImmediately;
  }

  /** Gets the indicator whether this object is being unit tested.
   *
   * @return the indicator whether this object is being unit tested
   */
  public boolean isUnitTest() {
    return isUnitTest;
  }

  /** Sets the indicator whether this object is being unit tested.
   *
   * @param isUnitTest the indicator whether this object is being unit tested
   */
  public void setIsUnitTest(final boolean isUnitTest) {
    this.isUnitTest = isUnitTest;
  }

  /** Gets the journal file path.
   *
   * @return the journal file path
   */
  public synchronized String getJournalFilePath() {
    return journalFilePath;
  }
}
