/*
 * DisjointWithQueriesTest.java
 *
 * Created on Jun 30, 2008, 12:37:00 AM
 *
 * Description: .
 *
 * Copyright (C) Oct 29, 2010 reed.
 */
package org.texai.subsumptionReasoner;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class DisjointWithQueriesTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(DisjointWithQueriesTest.class);
  /** the rdf entity manager */
  private static RDFEntityManager rdfEntityManager;

  public DisjointWithQueriesTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("setUpClass");
    CacheInitializer.initializeCaches();
    rdfEntityManager = new RDFEntityManager();
    Logger.getLogger(DisjointWithQueries.class).setLevel(Level.DEBUG);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    LOGGER.info("tearDownClass");
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
   * Test of areDisjoint method, of class DisjointWithQueries.
   */
  @Test
  public void testAreDisjoint() {
    LOGGER.info("areDisjoint");
    final DisjointWithQueries instance = new DisjointWithQueries(rdfEntityManager);

    LOGGER.info("------------------------------------------------------------------------------");
    assertFalse(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "Thing"),
            new URIImpl(Constants.CYC_NAMESPACE + "Thing")));

    LOGGER.info("------------------------------------------------------------------------------");
    assertTrue(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "Hat"),
            new URIImpl(Constants.CYC_NAMESPACE + "Mask")));

    LOGGER.info("------------------------------------------------------------------------------");
    assertTrue(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "Fedora"),
            new URIImpl(Constants.CYC_NAMESPACE + "Mask")));

    LOGGER.info("------------------------------------------------------------------------------");
    assertTrue(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "SunHat"),
            new URIImpl(Constants.CYC_NAMESPACE + "Mask")));

    LOGGER.info("------------------------------------------------------------------------------");
    assertFalse(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"),
            new URIImpl(Constants.CYC_NAMESPACE + "Lion")));

    LOGGER.info("------------------------------------------------------------------------------");
    instance.addMemoizedDisjointWith(
            new URIImpl(Constants.CYC_NAMESPACE + "LexicalWord"),
            new URIImpl(Constants.TEXAI_NAMESPACE + "LexiconEntry"));
    assertTrue(instance.areDisjoint(
            new URIImpl(Constants.CYC_NAMESPACE + "LexicalWord"),
            new URIImpl(Constants.TEXAI_NAMESPACE + "LexiconEntry")));
  }
}
