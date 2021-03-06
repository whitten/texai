/*
 * CachedSubsumptionGraphTest.java
 *
 * Created on Jun 30, 2008, 2:44:36 PM
 *
 * Description: .
 *
 * Copyright (C) May 2, 2011 reed.
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

/** This test does not modify the production OpenCyc repository.
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
    assertNotNull(superClasses);
    LOGGER.info(RDFUtility.formatSortedResources(superClasses));
    assertEquals("{cyc:HomoSapiens, cyc:LegalAgent, cyc:NarrativeRole, cyc:Sentient, cyc:SocialBeing}", RDFUtility.formatSortedResources(superClasses));

    final List<URI> disjointWiths = new ArrayList<>(cachedSubsumptionGraph1.getDirectDisjointWiths(new URIImpl(Constants.CYC_NAMESPACE + "Person")));
    Collections.sort(disjointWiths, new RDFUtility.ResourceComparator());
    assertNotNull(disjointWiths);
    assertEquals("{cyc:ConsumableProduct, cyc:Crystalline, cyc:DisposableProduct, cyc:FoodIngredientOnly, cyc:FoodOrDrink, cyc:IBTGeneration, cyc:MailableObject, cyc:MilitaryEquipment, cyc:NonPersonAnimal, cyc:PackagedObject, cyc:PhysicalDevice, cyc:TouristAttraction}", RDFUtility.formatSortedResources(disjointWiths));

    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
    LOGGER.info("CachedSubsumptionGraph completed");
  }
}
