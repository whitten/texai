package org.texai.kb.object;

import java.util.Set;
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
import org.texai.subsumptionReasoner.SubClassOfQueries;
import org.texai.subsumptionReasoner.TypeQueries;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class PropertyKBObjectTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(PropertyKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public PropertyKBObjectTest() {
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
   * Test of getSuperProperties method, of class PropertyKBObject.
   */
  @Test
  public void testGetSuperProperties() {
    LOGGER.info("getSuperProperties");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    final SubClassOfQueries subClassOfQueries = new SubClassOfQueries(rdfEntityManager);
    final TypeQueries typeQueries = new TypeQueries(rdfEntityManager);
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    assertTrue(typeQueries.isPropertyTerm("OpenCyc", term));


    URI binarySituationPredicate = new URIImpl(Constants.CYC_NAMESPACE + "BinarySituationPredicate");
    LOGGER.info("BinarySituationPredicate super classes:\n" +
            subClassOfQueries.getDirectSuperClasses("OpenCyc", binarySituationPredicate));

    final AbstractKBObject kbObject = kbAccess.findKBObject("OpenCyc", term);
    assertNotNull(kbObject);
    assertEquals(term, kbObject.getSubject());
    LOGGER.info("\n" + kbObject.toString());
    assertTrue(kbObject instanceof PropertyKBObject);
    final PropertyKBObject propertyKBObject = (PropertyKBObject) kbObject;
    final Set<URI> expectedTypes = new ArraySet<>();
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/RelationalNounSlot"));
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/NonAbduciblePredicate"));
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/EventOrRoleConcept"));
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/AgentiveRole"));
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/ActorSlotsConcernedWithAgencyOrInitiatingAnEvent-Actor-Topic"));
    expectedTypes.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/Actions-Topic"));
    final Set<URI> resultTypes = propertyKBObject.getTypes();
    assertEquals(expectedTypes.size(), resultTypes.size());
    for (final URI expectedType : expectedTypes) {
      assertTrue(resultTypes.contains(expectedType));
    }
    final Set<URI> expectedSuperProperties = new ArraySet<>();
    expectedSuperProperties.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/doneBy"));
    expectedSuperProperties.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/stagesEvent"));
    expectedSuperProperties.add(new URIImpl("http://sw.cyc.com/2006/07/27/cyc/deliberateActors"));
    final Set<URI> resultSuperProperties = propertyKBObject.getSuperProperties();
    assertEquals(expectedSuperProperties.size(), resultSuperProperties.size());
    for (final URI expectedSuperProperty : expectedSuperProperties) {
      assertTrue(resultSuperProperties.contains(expectedSuperProperty));
    }
    JournalWriter.close();
    rdfEntityManager.close();
  }

  /**
   * Test of makePropertyKBObject method, of class PropertyKBObject.
   */
  @Test
  public void testMakePropertyKBObject() {
    LOGGER.info("makePropertyKBObject");
    final String string =
            "texai:myProperty rdf:type rdf:Property .\n"
            + "texai:myProperty rdfs:subPropertyOf cyc:ConceptuallyRelated .\n";
    final PropertyKBObject propertyKBObject = PropertyKBObject.makePropertyKBObject(string, OPEN_CYC);

    //  texai:myProperty rdf:type rdf:Property .
    //  texai:myProperty rdfs:subPropertyOf cyc:ConceptuallyRelated .

    assertEquals("texai:myProperty rdf:type rdf:Property .\ntexai:myProperty rdfs:subPropertyOf cyc:ConceptuallyRelated .\n", propertyKBObject.toString());
    assertEquals("[http://www.w3.org/1999/02/22-rdf-syntax-ns#Property]", propertyKBObject.getTypes().toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/ConceptuallyRelated]", propertyKBObject.getSuperProperties().toString());
  }
}
