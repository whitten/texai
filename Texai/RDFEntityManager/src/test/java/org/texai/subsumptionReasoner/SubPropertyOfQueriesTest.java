/*
 * SubPropertyOfQueriesTest.java
 *
 * Created on Jun 30, 2008, 2:02:50 PM
 *
 * Description: .
 *
 * Copyright (C) Jan 7, 2011 reed.
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
package org.texai.subsumptionReasoner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.kb.persistence.RDFUtility.ResourceComparator;

/**
 *
 * @author reed
 */
public class SubPropertyOfQueriesTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(SubPropertyOfQueriesTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public SubPropertyOfQueriesTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    rdfEntityManager = new RDFEntityManager();
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
   * Test of isDirectSubPropertyOf method, of class SubPropertyOfQueries.
   */
  @Test
  public void testIsDirectSubPropertyOf() {
    LOGGER.info("isDirectSubPropertyOf");
    String repositoryName = "OpenCyc";
    URI property = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    URI superPropertyTerm = new URIImpl(Constants.CYC_NAMESPACE + "doneBy");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    boolean result = instance.isDirectSubPropertyOf(repositoryName, property, superPropertyTerm);
    assertEquals(true, result);
  }

  /**
   * Test of isSubPropertyOf method, of class SubPropertyOfQueries.
   */
  @Test
  public void testIsSubPropertyOf() {
    LOGGER.info("isSubPropertyOf");
    String repositoryName = "OpenCyc";
    URI property1 = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    URI property2 = new URIImpl(Constants.CYC_NAMESPACE + "doneBy");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    boolean result = instance.isSubPropertyOf(repositoryName, property1, property2);
    assertEquals(true, result);
  }

  /**
   * Test of getDirectSuperProperties method, of class SubPropertyOfQueries.
   */
  @Test
  public void testGetDirectSuperProperties() {
    LOGGER.info("getDirectSuperProperties");
    String repositoryName = "OpenCyc";
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "doneBy");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    Set<URI> result = instance.getDirectSuperProperties(repositoryName, term);
    List<URI> orderedResult = new ArrayList<>();
    orderedResult.addAll(result);
    Collections.sort(orderedResult, new ResourceComparator());
    assertTrue(orderedResult.size() > 2);
    LOGGER.info(RDFUtility.formatResources(orderedResult));
  }

  /**
   * Test of getSuperProperties method, of class SubPropertyOfQueries.
   */
  @Test
  public void testGetSuperProperties_String_URI() {
    LOGGER.info("getSuperProperties");
    String repositoryName = "OpenCyc";
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "performedBy");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    Set<URI> result = instance.getSuperProperties(repositoryName, term);
    List<URI> orderedResult = new ArrayList<>();
    orderedResult.addAll(result);
    Collections.sort(orderedResult, new ResourceComparator());
    assertTrue(orderedResult.size() > 2);
    LOGGER.info(RDFUtility.formatResources(orderedResult));
  }

  /**
   * Test of getDirectSubProperties method, of class SubPropertyOfQueries.
   */
  @Test
  public void testGetDirectSubProperties() {
    LOGGER.info("getDirectSubProperties");
    String repositoryName = "OpenCyc";
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "doneBy");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    Set<URI> result = instance.getDirectSubProperties(repositoryName, term);
    List<URI> orderedResult = new ArrayList<>();
    orderedResult.addAll(result);
    Collections.sort(orderedResult, new ResourceComparator());
    assertTrue(orderedResult.size() > 2);
    LOGGER.info(RDFUtility.formatResources(orderedResult));
  }

  /**
   * Test of clearCaches method, of class SubPropertyOfQueries.
   */
  @Test
  public void testClearCaches() {
    LOGGER.info("clearCaches");
    SubPropertyOfQueries instance = new SubPropertyOfQueries(rdfEntityManager);
    instance.clearCaches();
  }

}
