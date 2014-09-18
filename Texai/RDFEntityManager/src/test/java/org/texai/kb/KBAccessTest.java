/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class KBAccessTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(KBAccessTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public KBAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.copyProductionRepositoryToTest(OPEN_CYC);
    DistributedRepositoryManager.addTestRepositoryPath(
            OPEN_CYC,
            false); // isRepositoryDirectoryCleaned
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of doesTermExist method, of class KBAccess. This does not modify the production OpenCyc repository.
   */
  @Test
  public void testDoesTermExist() {
    LOGGER.info("doesTermExist");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "CityOfAustinTX");
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    KBAccess instance = new KBAccess(rdfEntityManager);
    boolean result = instance.doesTermExist(OPEN_CYC, term);
    assertTrue(result);
    result = instance.doesTermExist(OPEN_CYC, term);
    assertTrue(result);
    term = new URIImpl(Constants.CYC_NAMESPACE + "TUVWXYZ");
    result = instance.doesTermExist(OPEN_CYC, term);
    assertTrue(!result);
    JournalWriter.close();
    rdfEntityManager.close();
  }
}
