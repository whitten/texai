/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb;

import org.texai.kb.persistence.KBAccess;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class KBAccessTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(KBAccessTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public KBAccessTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
public static Test suite() {
  final TestSuite suite = new TestSuite();
   suite.addTest(new KBAccessTest("testDoesTermExist"));
   suite.addTest(new KBAccessTest("testFinalization"));
   return suite;
}

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Test of doesTermExist method, of class KBAccess.
   */
  public void testDoesTermExist() {
    System.out.println("doesTermExist");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            OPEN_CYC,
            "data/repositories/" + OPEN_CYC);
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

  public void testFinalization() {
    System.out.println("finalization");
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }
}
