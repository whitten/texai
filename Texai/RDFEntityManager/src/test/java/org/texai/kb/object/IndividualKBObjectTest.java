package org.texai.kb.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.kb.persistence.RDFUtility.ResourceComparator;

/** This test does not modify the production OpenCyc repository.
 *
 * @author reed
 */
public class IndividualKBObjectTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(IndividualKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public IndividualKBObjectTest() {
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

  @Test
  public void test() {
    LOGGER.info("test");
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
    List<URI> result = new ArrayList<>(individualKBObject.getTypes());
    Collections.sort(result, new ResourceComparator());
    assertEquals("{cyc:City, cyc:Individual, cyc:StateCapital, cyc:USCity}", RDFUtility.formatResources(result));
    JournalWriter.close();
    rdfEntityManager.close();
  }

  /**
   * Test of makeIndividualKBObject method, of class IndividualKBObject.
   */
  @Test
  public void testMakeIndividualKBObject() {
    LOGGER.info("makeIndividualKBObject");
    final String string =
            "texai:MyGroup001 rdf:type texai:MyGroup .";
    final IndividualKBObject individualKBObject = IndividualKBObject.makeIndividualKBObject(string, OPEN_CYC);

    //  texai:MyGroup001 rdf:type texai:MyGroup .

    assertEquals("texai:MyGroup001 rdf:type texai:MyGroup .\n", individualKBObject.toString());
    assertEquals("[http://texai.org/texai/MyGroup]", individualKBObject.getTypes().toString());
  }

}
