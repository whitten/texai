package org.texai.kb.object;

import java.util.ArrayList;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;

/** This test does not modify the production OpenCyc repository.
 *
 * @author reed
 */
public class ContextKBObjectTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(ContextKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public ContextKBObjectTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
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
   * Test of getSuperContexts method, of class ContextKBObject.
   */
  @Test
  public void testGetSuperContexts() {
    LOGGER.info("getSuperContexts");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    URI term = new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT);
    final AbstractKBObject kbObject = kbAccess.findKBObject(OPEN_CYC, term);
    assertNotNull(kbObject);
    assertTrue(kbObject instanceof ContextKBObject);
    final ContextKBObject contextKBObject = (ContextKBObject) kbObject;
    LOGGER.info("\n" + contextKBObject.toString());
    ArrayList<URI> result = new ArrayList<>(contextKBObject.getSuperContexts());
    assertEquals("{cyc:BaseKB, cyc:CoreCycLMt, cyc:CycAgencyTheoryMt, cyc:CycHistoricalPossibilityTheoryMt, cyc:UniversalVocabularyImplementationMt}", RDFUtility.formatSortedResources(result));
    rdfEntityManager.close();
  }

}
