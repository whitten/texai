/*
 * AllValuesFromRestrictionTest.java
 *
 * Created on Jun 30, 2008, 9:18:31 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 10, 2010 reed.
 */
package org.texai.kb.restriction.domainEntity;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class AllValuesFromRestrictionTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(AllValuesFromRestrictionTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the name of the test repository */
  private static final String TEST = "Test";

  public AllValuesFromRestrictionTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    JournalWriter.deleteJournalFiles();
    DistributedRepositoryManager.addTestRepositoryPath(
            TEST,
            true); // isRepositoryDirectoryCleaned
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    JournalWriter.close();
    rdfEntityManager.close();
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
   * Test of getOnProperty method, of class AllValuesFromRestriction.
   */
  @Test
  public void testGetOnProperty() {
    LOGGER.info("getOnProperty");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    URI result = instance.getOnProperty();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/performedBy", result.toString());
  }

  /**
   * Test of getAllValuesClass method, of class AllValuesFromRestriction.
   */
  @Test
  public void testGetAllValuesClass() {
    LOGGER.info("getAllValuesClass");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    URI result = instance.getAllValuesClass();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/Person", result.toString());
  }

  /**
   * Test of hashCode method, of class AllValuesFromRestriction.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    int result = instance.hashCode();
    assertEquals(-1507920724, result);
  }

  /**
   * Test of equals method, of class AllValuesFromRestriction.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    final URI allValuesClass2 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    AllValuesFromRestriction instance2 = new AllValuesFromRestriction(onProperty, allValuesClass2);
    assertFalse(instance.equals(instance2));
    AllValuesFromRestriction instance3 = new AllValuesFromRestriction(onProperty, allValuesClass);
    assertTrue(instance.equals(instance3));
  }

  /**
   * Test of toString method, of class AllValuesFromRestriction.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    String result = instance.toString();
    assertEquals("[Restriction on cyc:performedBy, allVauesFrom cyc:Person]", result);
  }

  /**
   * Test of persistence, of class AllValuesFromRestriction.
   */
  @Test
  public void testPersistence() {
    LOGGER.info("persistence");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    AllValuesFromRestriction instance = new AllValuesFromRestriction(onProperty, allValuesClass);
    assertTrue(RDFEntityManager.isSerializable(instance));
    assertNull(instance.getId());
    rdfEntityManager.persist(instance, TEST);
    final URI id = instance.getId();
    assertNotNull(id);
    final AllValuesFromRestriction loadedInstance = rdfEntityManager.find(AllValuesFromRestriction.class, id, TEST);
    assertNotNull(loadedInstance);
    assertEquals(instance, loadedInstance);
    rdfEntityManager.remove(instance, TEST);
    assertNull(rdfEntityManager.find(AllValuesFromRestriction.class, id, TEST));
  }


}
