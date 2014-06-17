/*
 * HasValueRestrictionTest.java
 *
 * Created on Jun 30, 2008, 9:18:48 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 10, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
public class HasValueRestrictionTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HasValueRestrictionTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public HasValueRestrictionTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    JournalWriter.deleteJournalFiles();
    DistributedRepositoryManager.addRepositoryPath(
            "ConceptuallyRelatedTerms",
            System.getenv("REPOSITORIES_TMPFS") + "/Test");
    DistributedRepositoryManager.clearNamedRepository("Test");
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
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    URI result = instance.getOnProperty();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/performedBy", result.toString());
  }

  /**
   * Test of getHasValueClass method, of class HasValueRestriction.
   */
  @Test
  public void testGetHasValueClass() {
    LOGGER.info("getHasValueClass");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    URI result = instance.getHasValueClass();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/Person", result.toString());
  }

  /**
   * Test of hashCode method, of class HasValueRestriction.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    int result = instance.hashCode();
    assertEquals(-1507920724, result);
  }

  /**
   * Test of equals method, of class HasValueRestriction.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    assertEquals(false, instance.equals("abc"));
    final URI allValuesClass2 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    HasValueRestriction instance2 = new HasValueRestriction(onProperty, allValuesClass2);
    assertEquals(false, instance.equals(instance2));
    HasValueRestriction instance3 = new HasValueRestriction(onProperty, allValuesClass);
    assertEquals(true, instance.equals(instance3));
  }

  /**
   * Test of toString method, of class HasValueRestriction.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    String result = instance.toString();
    assertEquals("[Restriction on cyc:performedBy, hasValue cyc:Person]", result);
  }

  /**
   * Test of persistence, of class AllValuesFromRestriction.
   */
  @Test
  public void testPersistence() {
    LOGGER.info("persistence");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI allValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    HasValueRestriction instance = new HasValueRestriction(onProperty, allValuesClass);
    assertTrue(RDFEntityManager.isSerializable(instance));
    assertNull(instance.getId());
    rdfEntityManager.persist(instance, "Test");
    final URI id = instance.getId();
    assertNotNull(id);
    final HasValueRestriction loadedInstance = rdfEntityManager.find(HasValueRestriction.class, id, "Test");
    assertNotNull(loadedInstance);
    assertEquals(instance, loadedInstance);
    rdfEntityManager.remove(instance, "Test");
    assertNull(rdfEntityManager.find(HasValueRestriction.class, id, "Test"));
  }

}
