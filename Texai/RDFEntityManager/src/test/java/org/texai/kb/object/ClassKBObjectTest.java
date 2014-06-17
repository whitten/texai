/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.object;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.texai.kb.persistence.RDFUtility;
import org.texai.kb.persistence.RDFUtility.ResourceComparator;

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
    List<URI> result = new ArrayList<>(classKBObject.getTypes());
    Collections.sort(result, new ResourceComparator());
    assertEquals("{cyc:ArtifactTypeByFunction, cyc:CDETagTranslationConstant, cyc:ExistingObjectType, cyc:FirstOrderCollection, cyc:SaffronSituationTypeConstant, cyc:SpatiallyDisjointObjectType, cyc:SpecializationsOfPhysicalDevice-Device-Topic, cyc:Transportation-Topic}", RDFUtility.formatResources(result));
    result = new ArrayList<>(classKBObject.getSuperClasses());
    Collections.sort(result, new ResourceComparator());
    assertEquals("{cyc:Artifact-NonAgentive, cyc:Conveyance, cyc:PhysicalDevice}", RDFUtility.formatResources(result));
    result = new ArrayList<>(classKBObject.getDisjointWiths());
    Collections.sort(result, new ResourceComparator());
    assertEquals("{cyc:AlarmDevice, cyc:Clothing-Generic, cyc:ComputationalSystem, cyc:ControlDevice, cyc:Decoration, cyc:GeographicalThing, cyc:InformationRecordingDevice, cyc:MeasuringDevice, cyc:NaturalTangibleStuff, cyc:NavigationDevice, cyc:NonPoweredDevice, cyc:PathArtifactSystem, cyc:PlumbingFixture, cyc:RecordingOfWaveIBT, cyc:SmallArm-Weapon, cyc:StationeryProduct, cyc:TextualMaterial, cyc:TravelAccessory, cyc:Wire}", RDFUtility.formatResources(result));
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
