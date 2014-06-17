/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.journal;

import java.io.File;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author reed
 */
public class RepositoryArchiverTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalWriterTest.class);
  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  public RepositoryArchiverTest(String testName) {
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
   * Test of archive method, of class RepositoryArchiver.
   */
  public void testArchive() {
    LOGGER.info("archive");

    String testRepositoryPath = System.getenv("REPOSITORIES_TMPFS");
    if (testRepositoryPath == null || testRepositoryPath.isEmpty()) {
      testRepositoryPath = System.getProperty("user.dir") + "/repositories";
    } else if (testRepositoryPath.endsWith("/")) {
      testRepositoryPath = testRepositoryPath.substring(0, testRepositoryPath.length() - 1);
    }
    assertFalse(testRepositoryPath.isEmpty());

    Repository repository = null;
    final String dataDirectoryPath = testRepositoryPath + "/" + TEST_REPOSITORY_NAME;
    try {
      final File dataDirectory = new File(dataDirectoryPath);
      LOGGER.info("accessing Sesame2 repository in " + dataDirectory.toString());
      final String indices = "spoc,posc";
      repository = new SailRepository(new NativeStore(dataDirectory, indices));
      repository.initialize();
      final RepositoryConnection repositoryConnection = repository.getConnection();
      repositoryConnection.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(repository);
    try {
      repository.shutDown();
    } catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    RepositoryArchiver instance = new RepositoryArchiver(dataDirectoryPath);
    instance.archive();
  }
}
