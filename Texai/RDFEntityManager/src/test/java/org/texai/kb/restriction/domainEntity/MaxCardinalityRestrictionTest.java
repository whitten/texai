/*
 * MaxCardinalityRestrictionTest.java
 *
 * Created on Jun 30, 2008, 9:18:54 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 10, 2010 reed.
 */
package org.texai.kb.restriction.domainEntity;

import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;
import org.openrdf.model.URI;
import net.sf.ehcache.CacheManager;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.RDFEntityManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class MaxCardinalityRestrictionTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MaxCardinalityRestrictionTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the name of the test repository */
  private static final String TEST = "Test";

  public MaxCardinalityRestrictionTest() {
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
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    URI result = instance.getOnProperty();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/performedBy", result.toString());
  }

  /**
   * Test of getMaxCardinality method, of class MaxCardinalityRestriction.
   */
  @Test
  public void testGetMaxCardinality() {
    LOGGER.info("getMaxCardinality");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    long result = instance.getMaxCardinality();
    assertEquals(10, result);
  }

  /**
   * Test of hashCode method, of class MaxCardinalityRestriction.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    int result = instance.hashCode();
    assertEquals(646904761, result);
  }

  /**
   * Test of equals method, of class MaxCardinalityRestriction.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    MaxCardinalityRestriction instance2 = new MaxCardinalityRestriction(onProperty, 11);
    assertFalse(instance.equals(instance2));
    MaxCardinalityRestriction instance3 = new MaxCardinalityRestriction(onProperty, 10);
    assertTrue(instance.equals(instance3));
  }

  /**
   * Test of toString method, of class MaxCardinalityRestriction.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    String result = instance.toString();
    assertEquals("[Restriction on cyc:performedBy, maxCardinality 10]", result);
  }

  /**
   * Test of persistence, of class AllValuesFromRestriction.
   */
  @Test
  public void testPersistence() {
    LOGGER.info("persistence");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    MaxCardinalityRestriction instance = new MaxCardinalityRestriction(onProperty, 10);
    assertTrue(RDFEntityManager.isSerializable(instance));
    assertNull(instance.getId());
    rdfEntityManager.persist(instance, TEST);
    final URI id = instance.getId();
    assertNotNull(id);
    final MaxCardinalityRestriction loadedInstance = rdfEntityManager.find(MaxCardinalityRestriction.class, id, TEST);
    assertNotNull(loadedInstance);
    assertEquals(instance, loadedInstance);
    rdfEntityManager.remove(instance, TEST);
    assertNull(rdfEntityManager.find(MaxCardinalityRestriction.class, id, TEST));
  }

}
