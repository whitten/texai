/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.journal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;

/**
 *
 * @author reed
 */
public class JournalReaderTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalReaderTest.class);
  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  public JournalReaderTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test of read method, of class JournalReader.
   */
  public void testRead() {
    System.out.println("read");
    CacheInitializer.initializeCaches();

    String testRepositoryPath = System.getenv("REPOSITORIES_TMPFS");
    if (testRepositoryPath == null || testRepositoryPath.isEmpty()) {
      testRepositoryPath = System.getProperty("user.dir") + "repositories";
    } else if (testRepositoryPath.endsWith("/")) {
      testRepositoryPath = testRepositoryPath.substring(0, testRepositoryPath.length() - 1);
    }
    assertFalse(testRepositoryPath.isEmpty());

    testRepositoryDirectory = new File(testRepositoryPath);
    try {
      if (testRepositoryDirectory.exists()) {
        FileUtils.cleanDirectory(testRepositoryDirectory);
      } else {
        FileUtils.deleteDirectory(testRepositoryDirectory);
      }
    } catch (final IOException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(testRepositoryDirectory);
    DistributedRepositoryManager.addRepositoryPath(
            TEST_REPOSITORY_NAME,
            testRepositoryPath + "/" + TEST_REPOSITORY_NAME);

    DistributedRepositoryManager.deleteNamedRepository(TEST_REPOSITORY_NAME);
    DistributedRepositoryManager.clearNamedRepository(TEST_REPOSITORY_NAME);
    final File directory = new File("./journals/" + TEST_REPOSITORY_NAME);
    if (!directory.exists()) {
      try {
        directory.createNewFile();
      } catch (final IOException ex) {
        fail(ex.getMessage());
      }
    }
    assert directory.exists() : "./journals/" + TEST_REPOSITORY_NAME + " does not exist";
    if (directory.listFiles() != null) {
      final File[] files = directory.listFiles();
      for (final File file : files) {
        if (!file.isHidden()) {
          LOGGER.info("deleting file: " + file);
          file.delete();
        }
      }
    }
    List<JournalRequest> journalRequests = new ArrayList<JournalRequest>();
    journalRequests.add(new JournalRequest(
            TEST_REPOSITORY_NAME,
            Constants.ADD_OPERATION,
            new ContextStatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE),
            new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT))));
    JournalWriter journalWriter = new JournalWriter();
    journalWriter.setIsUnitTest(true);
    journalWriter.write(journalRequests);
    journalWriter.commit();
    String journalFilePath = journalWriter.getJournalFilePath();
    JournalReader instance = new JournalReader();
    instance.read(journalFilePath);
    try {
      final RepositoryConnection repositoryConnection = 
              DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(TEST_REPOSITORY_NAME);
      assertEquals(1, repositoryConnection.size());
      repositoryConnection.close();
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    JournalWriter.close();
    DistributedRepositoryManager.shutDown();
  }
}
