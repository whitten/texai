/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.object;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class IndividualKBObjectTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(IndividualKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public IndividualKBObjectTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
public static Test suite() {
  final TestSuite suite = new TestSuite();
   suite.addTest(new IndividualKBObjectTest("test"));
   suite.addTest(new IndividualKBObjectTest("testMakeIndividualKBObject"));
   suite.addTest(new IndividualKBObjectTest("testFinalization"));
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

  public void test() {
    System.out.println("test");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            OPEN_CYC,
            "data/repositories/" + OPEN_CYC);
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "CityOfAustinTX");

    final AbstractKBObject kbObject = kbAccess.findKBObject(OPEN_CYC, term);
    assertNotNull(kbObject);
    assertEquals(term, kbObject.getSubject());
    assertTrue(kbObject instanceof IndividualKBObject);
    final IndividualKBObject individualKBObject = (IndividualKBObject) kbObject;
    LOGGER.info("\n" + individualKBObject.toString());
    assertEquals("cyc:CityOfAustinTX cyc:prettyString-Canonical \"Austin\" .\ncyc:CityOfAustinTX rdf:type cyc:City .\ncyc:CityOfAustinTX rdf:type cyc:Individual .\ncyc:CityOfAustinTX rdf:type cyc:StateCapital .\ncyc:CityOfAustinTX rdf:type cyc:USCity .\n", individualKBObject.toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/USCity, http://sw.cyc.com/2006/07/27/cyc/StateCapital, http://sw.cyc.com/2006/07/27/cyc/Individual, http://sw.cyc.com/2006/07/27/cyc/City]", individualKBObject.getTypes().toString());
    JournalWriter.close();
    rdfEntityManager.close();
  }

  /**
   * Test of makeIndividualKBObject method, of class IndividualKBObject.
   */
  public void testMakeIndividualKBObject() {
    LOGGER.info("makeIndividualKBObject");
    final String string =
            "texai:MyGroup001 rdf:type texai:MyGroup .";
    final IndividualKBObject individualKBObject = IndividualKBObject.makeIndividualKBObject(string, OPEN_CYC);

    //  texai:MyGroup001 rdf:type texai:MyGroup .

    assertEquals("texai:MyGroup001 rdf:type texai:MyGroup .\n", individualKBObject.toString());
    assertEquals("[http://texai.org/texai/MyGroup]", individualKBObject.getTypes().toString());
  }

  public void testFinalization() {
    System.out.println("finalization");
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }
}
