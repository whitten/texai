/*
 * CachedSubsumptionGraphTest.java
 *
 * Created on Jun 30, 2008, 2:44:36 PM
 *
 * Description: .
 *
 * Copyright (C) May 2, 2011 reed.
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
package org.texai.subsumptionGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
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

/**
 *
 * @author reed
 */
public class CachedSubsumptionGraphTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(CachedSubsumptionGraphTest.class);

  public CachedSubsumptionGraphTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getDirectSuperClasses method, of class CachedSubsumptionGraph.
   */
  @Test
  public void testGetDirectSuperClasses() {
    CacheInitializer.initializeCaches();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    CachedSubsumptionGraph.initializeSingletonInstance(rdfEntityManager);
    rdfEntityManager.close();
    final CachedSubsumptionGraph cachedSubsumptionGraph1 = CachedSubsumptionGraph.getInstance();
    cachedSubsumptionGraph1.logDictionaryStatistics();
    final List<URI> superClasses = new ArrayList<>(cachedSubsumptionGraph1.getDirectSuperClasses("OpenCyc", new URIImpl(Constants.CYC_NAMESPACE + "Person")));
    Collections.sort(superClasses, new RDFUtility.ResourceComparator());
    assertNotNull(superClasses);
    //LOGGER.info(RDFUtility.formatResources(superClasses));
    assertTrue(RDFUtility.formatResources(superClasses).startsWith("{cyc:HomoSapiens, cyc:LegalAgent, cyc:NarrativeRole, cyc:Sentient, cyc:SocialBeing"));

    final List<URI> disjointWiths = new ArrayList<>(cachedSubsumptionGraph1.getDirectDisjointWiths(new URIImpl(Constants.CYC_NAMESPACE + "Person")));
    Collections.sort(disjointWiths, new RDFUtility.ResourceComparator());
    assertNotNull(disjointWiths);
    assertEquals("{cyc:ConsumableProduct, cyc:Crystalline, cyc:DisposableProduct, cyc:FoodIngredientOnly, cyc:FoodOrDrink, cyc:IBTGeneration, cyc:MailableObject, cyc:MilitaryEquipment, cyc:NonPersonAnimal, cyc:PackagedObject, cyc:PhysicalDevice, cyc:TouristAttraction}", RDFUtility.formatResources(disjointWiths));

    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
    LOGGER.info("CachedSubsumptionGraph completed");
  }
}
