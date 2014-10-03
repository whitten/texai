/*
 * KBInitializerTest.java
 *
 * Created on Jun 30, 2008, 12:25:39 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 22, 2010 reed.
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
package org.texai.kb;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class KBInitializerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(KBInitializerTest.class);
  /** the name of the Open Cyc repository */
  private static final String OPEN_CYC = "OpenCyc";
  /** the name of the Test repository */
  private static final String TEST = "Test";

  public KBInitializerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
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
   * Test of process method, of class KBInitializer.
   */
  @Test
  public void process1Test() {
    LOGGER.info("process1Test");
    DistributedRepositoryManager.copyProductionRepositoryToTest(OPEN_CYC);
    DistributedRepositoryManager.addTestRepositoryPath(
            OPEN_CYC,
            true); // isRepositoryDirectoryCleaned
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final KBInitializer kbInitializer = new KBInitializer(rdfEntityManager);
    kbInitializer.process();
    rdfEntityManager.close();
  }

  /**
   * Test of process method, of class KBInitializer.
   */
  @Test
  public void process2Test() {
    LOGGER.info("process2Test");
    DistributedRepositoryManager.addTestRepositoryPath(
            TEST,
            true); // isRepositoryDirectoryCleaned
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    final RepositoryConnection repositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(TEST);
    assertNotNull(repositoryConnection);
    final KBInitializer kbInitializer = new KBInitializer(repositoryConnection);
    kbInitializer.process();
    rdfEntityManager.close();
  }

}
