/*
 * SomeValuesFromRestrictionTest.java
 *
 * Created on Jun 30, 2008, 9:19:07 PM
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
public class SomeValuesFromRestrictionTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SomeValuesFromRestrictionTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the name of the test repository */
  private static final String TEST = "Test";

  public SomeValuesFromRestrictionTest() {
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
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    URI result = instance.getOnProperty();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/performedBy", result.toString());
  }

  /**
   * Test of getSomeValuesClass method, of class SomeValuesFromRestriction.
   */
  @Test
  public void testGetSomeValuesClass() {
    LOGGER.info("getSomeValuesClass");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    URI result = instance.getSomeValuesClass();
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/Person", result.toString());
  }

  /**
   * Test of hashCode method, of class SomeValuesFromRestriction.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    int result = instance.hashCode();
    assertEquals(-1507920724, result);
  }

  /**
   * Test of equals method, of class SomeValuesFromRestriction.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    final URI someValuesClass2 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    SomeValuesFromRestriction instance2 = new SomeValuesFromRestriction(onProperty, someValuesClass2);
    assertFalse(instance.equals(instance2));
    SomeValuesFromRestriction instance3 = new SomeValuesFromRestriction(onProperty, someValuesClass);
    assertTrue(instance.equals(instance3));
  }

  /**
   * Test of toString method, of class SomeValuesFromRestriction.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    String result = instance.toString();
    assertEquals("[Restriction on cyc:performedBy, someVauesFrom cyc:Person]", result);
  }

  /**
   * Test of persistence, of class AllValuesFromRestriction.
   */
  @Test
  public void testPersistence() {
    LOGGER.info("persistence");
    final URI onProperty = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    final URI someValuesClass = new URIImpl(Constants.CYC_NAMESPACE + "Person");
    SomeValuesFromRestriction instance = new SomeValuesFromRestriction(onProperty, someValuesClass);
    assertTrue(RDFEntityManager.isSerializable(instance));
    assertNull(instance.getId());
    rdfEntityManager.persist(instance, TEST);
    final URI id = instance.getId();
    assertNotNull(id);
    final SomeValuesFromRestriction loadedInstance = rdfEntityManager.find(SomeValuesFromRestriction.class, id, TEST);
    assertNotNull(loadedInstance);
    assertEquals(instance, loadedInstance);
    rdfEntityManager.remove(instance, TEST);
    assertNull(rdfEntityManager.find(SomeValuesFromRestriction.class, id, TEST));
  }

}
