package org.texai.kb.journal;

import java.io.File;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.nativerdf.NativeStore;

/**
 *
 * @author reed
 */
public class RepositoryArchiverTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(JournalWriterTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";

  public RepositoryArchiverTest() {
  }

  /**
   * Test of archive method, of class RepositoryArchiver.
   */
  @Test
  public void testArchive() {
    LOGGER.info("archive");

    String testRepositoryPath = System.getenv("REPOSITORIES_TMPFS");
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
    } catch (RepositoryException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(repository);
    try {
      repository.shutDown();
    } catch (RepositoryException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    RepositoryArchiver instance = new RepositoryArchiver(dataDirectoryPath);
    instance.archive();
  }
}
