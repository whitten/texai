/*
 * QueryContainerInitializerTest.java
 *
 * Created on Jun 30, 2008, 10:35:20 AM
 *
 * Description: .
 *
 * Copyright (C) Aug 14, 2010 reed.
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
package org.texai.inference;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.inference.rete.ReteEngine;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class QueryContainerInitializerTest {

  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(QueryContainerInitializerTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public QueryContainerInitializerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "InferenceRules",
            System.getenv("REPOSITORIES_TMPFS") + "/InferenceRules");
    DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    rdfEntityManager = new RDFEntityManager();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
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
   * Test of the class QueryContainerInitializer.
   */
  @Test
  public void test() {
    LOGGER.info("test");
    final QueryContainerInitializer queryContainerInitializer = new QueryContainerInitializer();
    final ReteEngine reteEngine = new ReteEngine();
    queryContainerInitializer.setReteEngine(reteEngine);
    queryContainerInitializer.initialize(
            rdfEntityManager,
            false);  // overrideContext
    queryContainerInitializer.process("../Main/data/bootstrap-queries.xml");
    queryContainerInitializer.finalization();
    reteEngine.toGraphViz("rete-graph6", null);
  }

}
