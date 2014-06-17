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
public class ClassKBObjectTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(ClassKBObjectTest.class);
  /** the OpenCyc repository name */
  private static final String OPEN_CYC = "OpenCyc";

  public ClassKBObjectTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new ClassKBObjectTest("testGetSuperClasses"));
    suite.addTest(new ClassKBObjectTest("testMakeClassKBObject"));
    suite.addTest(new ClassKBObjectTest("testFinalization"));
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
   * Test of getSuperClasses method, of class ClassKBObject.
   */
  public void testGetSuperClasses() {
    LOGGER.info("getSuperClasses");
    CacheInitializer.initializeCaches();

    DistributedRepositoryManager.addRepositoryPath(
            OPEN_CYC,
            "data/repositories/" + OPEN_CYC);

    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "TransportationDevice");
    final KBAccess kbAccess = new KBAccess(rdfEntityManager);
    final AbstractKBObject kbObject = kbAccess.findKBObject(
            OPEN_CYC,
            term);
    assertNotNull(kbObject);
    assertEquals(term, kbObject.getSubject());
    LOGGER.info("\n" + kbObject.toString());
    assertTrue(kbObject instanceof ClassKBObject);
    final ClassKBObject classKBObject = (ClassKBObject) kbObject;
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/SpecializationsOfPhysicalDevice-Device-Topic, http://sw.cyc.com/2006/07/27/cyc/SaffronSituationTypeConstant, http://sw.cyc.com/2006/07/27/cyc/ArtifactTypeByFunction, http://sw.cyc.com/2006/07/27/cyc/ExistingObjectType, http://sw.cyc.com/2006/07/27/cyc/FirstOrderCollection, http://sw.cyc.com/2006/07/27/cyc/Transportation-Topic, http://sw.cyc.com/2006/07/27/cyc/SpatiallyDisjointObjectType, http://sw.cyc.com/2006/07/27/cyc/CDETagTranslationConstant]", classKBObject.getTypes().toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/Conveyance, http://sw.cyc.com/2006/07/27/cyc/Artifact-NonAgentive, http://sw.cyc.com/2006/07/27/cyc/PhysicalDevice]", classKBObject.getSuperClasses().toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/AlarmDevice, http://sw.cyc.com/2006/07/27/cyc/TravelAccessory, http://sw.cyc.com/2006/07/27/cyc/NavigationDevice, http://sw.cyc.com/2006/07/27/cyc/ComputationalSystem, http://sw.cyc.com/2006/07/27/cyc/TextualMaterial, http://sw.cyc.com/2006/07/27/cyc/RecordingOfWaveIBT, http://sw.cyc.com/2006/07/27/cyc/Wire, http://sw.cyc.com/2006/07/27/cyc/SmallArm-Weapon, http://sw.cyc.com/2006/07/27/cyc/MeasuringDevice, http://sw.cyc.com/2006/07/27/cyc/StationeryProduct, http://sw.cyc.com/2006/07/27/cyc/Decoration, http://sw.cyc.com/2006/07/27/cyc/PlumbingFixture, http://sw.cyc.com/2006/07/27/cyc/InformationRecordingDevice, http://sw.cyc.com/2006/07/27/cyc/Clothing-Generic, http://sw.cyc.com/2006/07/27/cyc/PathArtifactSystem, http://sw.cyc.com/2006/07/27/cyc/ControlDevice, http://sw.cyc.com/2006/07/27/cyc/GeographicalThing, http://sw.cyc.com/2006/07/27/cyc/NonPoweredDevice, http://sw.cyc.com/2006/07/27/cyc/NaturalTangibleStuff]", classKBObject.getDisjointWiths().toString());
    JournalWriter.close();
    rdfEntityManager.close();
  }

  /**
   * Test of makeClassKBObject method, of class ClassKBObject.
   */
  public void testMakeClassKBObject() {
    LOGGER.info("makeClassKBObject");
    final String string =
            "texai:MyGroup rdf:type cyc:ExistingObjectType .\n"
            + "texai:MyGroup rdfs:subClassOf cyc:Group .";
    final ClassKBObject classKBObject = ClassKBObject.makeClassKBObject(string, OPEN_CYC);

    //  texai:MyGroup rdf:type cyc:ExistingObjectType .
    //  texai:MyGroup rdfs:subClassOf cyc:Group .

    assertEquals("texai:MyGroup rdf:type cyc:ExistingObjectType .\ntexai:MyGroup rdfs:subClassOf cyc:Group .\n", classKBObject.toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/ExistingObjectType]", classKBObject.getTypes().toString());
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/Group]", classKBObject.getSuperClasses().toString());
  }

  public void testFinalization() {
    LOGGER.info("finalization");
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }
}
