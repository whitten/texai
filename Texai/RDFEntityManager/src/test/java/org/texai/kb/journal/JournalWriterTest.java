package org.texai.kb.journal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.texai.kb.Constants;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class JournalWriterTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalWriterTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";

  public JournalWriterTest() {
  }

  /**
   * Test of write method, of class JournalWriter.
   */
  @Test
  public void testWrite() {
    LOGGER.info("write");
    final File directory = new File("./journals/");
    if (!directory.exists()) {
      LOGGER.info("creating journals directory");
      directory.mkdir();
    }
    assert directory.exists() : "./journals/ does not exist";
    final File[] files = directory.listFiles();
    for (final File file : files) {
      if (!file.isHidden()) {
        LOGGER.info("deleting file: " + file);
        file.delete();
      }
    }
    List<JournalRequest> journalRequests = new ArrayList<>();
    journalRequests.add(new JournalRequest(
            TEST_REPOSITORY_NAME,
            Constants.ADD_OPERATION,
            new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE))));
    JournalWriter instance = new JournalWriter();
    instance.setIsUnitTest(true);
    instance.write(journalRequests);
    JournalWriter.close();
  }
}
