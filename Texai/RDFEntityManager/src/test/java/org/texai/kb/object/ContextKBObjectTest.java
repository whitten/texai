/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.kb.persistence.RDFUtility.ResourceComparator;

/**
 *
 * @author reed
 */
public class ContextKBObjectTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(ContextKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public ContextKBObjectTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
public static Test suite() {
  final TestSuite suite = new TestSuite();
   suite.addTest(new ContextKBObjectTest("testGetSuperContexts"));
   suite.addTest(new ContextKBObjectTest("testFinalization"));
   return suite;
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
   * Test of getSuperContexts method, of class ContextKBObject.
   */
  public void testGetSuperContexts() {
    LOGGER.info("getSuperContexts");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            OPEN_CYC,
            "data/repositories/" + OPEN_CYC);
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    URI term = new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT);
    final AbstractKBObject kbObject = kbAccess.findKBObject(OPEN_CYC, term);
    assertNotNull(kbObject);
    assertTrue(kbObject instanceof ContextKBObject);
    final ContextKBObject contextKBObject = (ContextKBObject) kbObject;
    LOGGER.info("\n" + contextKBObject.toString());
    ArrayList<URI> result = new ArrayList<>(contextKBObject.getSuperContexts());
    Collections.sort(result, new ResourceComparator());
    assertEquals("{cyc:BaseKB, cyc:CoreCycLMt, cyc:CycAgencyTheoryMt, cyc:CycHistoricalPossibilityTheoryMt, cyc:UniversalVocabularyImplementationMt}", RDFUtility.formatResources(result));
    rdfEntityManager.close();
  }

  public void testFinalization() {
    System.out.println("finalization");
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }
}
