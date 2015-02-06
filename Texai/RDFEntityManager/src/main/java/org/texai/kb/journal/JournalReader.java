/*
 * JournalReader.java
 *
 * Created on Mar 17, 2009, 9:56:32 AM
 *
 * Description: Provides a journal reader.
 *
 * Copyright (C) Mar 17, 2009 Stephen L. Reed.
 */
package org.texai.kb.journal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.turtleStatementParser.ParseException;
import org.texai.turtleStatementParser.TurtleStatementParser;
import org.texai.util.TexaiException;

/** Provides a journal reader. Assumes that repository names do not include a dash character.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class JournalReader {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalReader.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();

  /** Constructs a new JournalReader instance. */
  public JournalReader() {
  }

  /** Reads the journal entries from the given journal file path and applies them to the appropriate repository.
   *
   * @param journalFilePath the given journal file path, e.g. /home/reed/svn/RDFEntityManager/journals/test/test-2009-03-18T22:44:10.017-05:00.jrnl
   */
  public void read(final String journalFilePath) {
    //Preconditions
    assert journalFilePath != null : "journalFilePath must not be null";
    assert !journalFilePath.isEmpty() : "journalFilePath must not be empty";

    read(journalFilePath, null);
  }

  /** Reads the journal entries from the given journal file path and applies them to the given repository.
   *
   * @param journalFilePath the given journal file path, e.g. /home/reed/svn/RDFEntityManager/journals/test/test-2009-03-18T22:44:10.017-05:00.jrnl
   * @param dataDirectoryPath the repository data directory path, or null if the repository is in the default location and its name
   * should be extracted from the given journal file path
   */
  @SuppressWarnings("null")
  public void read(final String journalFilePath, final String dataDirectoryPath) {
    //Preconditions
    assert journalFilePath != null : "journalFilePath must not be null";
    assert !journalFilePath.isEmpty() : "journalFilePath must not be empty";

    LOGGER.info("reading: " + journalFilePath);

    final File journalFile = new File(journalFilePath);
    if (!journalFile.exists()) {
      throw new TexaiException("journal file not found: " + journalFilePath);
    }
    String canonicalJournalFilePath = null;
    try {
      canonicalJournalFilePath = journalFile.getCanonicalPath();
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
    int index = canonicalJournalFilePath.lastIndexOf(File.separator);
    final String journalFileName = canonicalJournalFilePath.substring(index + 1);

    Repository repository;
    RepositoryConnection repositoryConnection = null;
    if (dataDirectoryPath == null) {
      // extract repository name from the given journal file path
      index = journalFileName.indexOf('-');
      final String repositoryName = journalFileName.substring(0, index);
      repositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(repositoryName);
    } else {
      try {
        final File dataDirectory = new File(dataDirectoryPath);
        LOGGER.info("accessing Sesame2 repository in " + dataDirectory.toString());
        final String indices = "spoc,posc";
        repository = new SailRepository(new NativeStore(dataDirectory, indices));
        repository.initialize();
        repositoryConnection = repository.getConnection();
      } catch (RepositoryException ex) {
        throw new TexaiException(ex);
      }
    }
    assert repositoryConnection != null;
    try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(canonicalJournalFilePath), "UTF-8"))) {
      while (true) {
        final String line = bufferedReader.readLine();
        if (line == null) {
          bufferedReader.close();
          break;
        }
        LOGGER.info(line);
        index = line.indexOf(' ');
        final int index2 = line.indexOf(' ', index + 1);
        index = line.indexOf(' ', index2 + 1);
        final String operation = line.substring(index2 + 1, index);
        if (IS_DEBUG_LOGGING_ENABLED) {
          LOGGER.debug("  operation: " + operation);
        }
        final String turtleStatementString = line.substring(index + 1);
        final TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(turtleStatementString);
        Statement statement = null;
        try {
          statement = turtleStatementParser.Statement();
          if (IS_DEBUG_LOGGING_ENABLED) {
            LOGGER.debug("  parsed statement: " + RDFUtility.formatStatementAsTurtle(statement));
          }
        } catch (final ParseException ex) {
          throw new TexaiException(ex);
        }
        try {
          switch (operation) {
            case Constants.ADD_OPERATION:
              if (IS_DEBUG_LOGGING_ENABLED) {
                LOGGER.debug("add: " + RDFUtility.formatStatementAsTurtle(statement));
              }
              repositoryConnection.add(statement);
              break;
            case Constants.REMOVE_OPERATION:
              if (IS_DEBUG_LOGGING_ENABLED) {
                LOGGER.debug("remove: " + RDFUtility.formatStatementAsTurtle(statement));
              }
              repositoryConnection.remove(statement);
              break;
            default:
              assert false;
              break;
          }
        } catch (final RepositoryException ex) {
          throw new TexaiException(ex);
        }
      }
      try {
        repositoryConnection.close();
      } catch (RepositoryException ex) {
        throw new TexaiException(ex);
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }
}
