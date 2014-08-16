/*
 * RoleTypeInitializerTest.java
 *
 * Created on Jun 30, 2008, 1:14:49 PM
 *
 * Description: .
 *
 * Copyright (C) May 7, 2010 reed.
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
package org.texai.ahcsSupport;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class RoleTypeInitializerTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RoleTypeInitializerTest.class);

  public RoleTypeInitializerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "NodeRoleTypes",
            true); // isRepositoryDirectoryCleaned
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
   * Test of class RoleTypeInitializer.
   */
  @Test
  public void testProcess() {
    LOGGER.info("beginning test");
    final RoleTypeInitializer roleTypeInitializer =
            new RoleTypeInitializer();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    roleTypeInitializer.initialize(rdfEntityManager); // clear repository first
    roleTypeInitializer.process("data/role-types-test.xml");
    roleTypeInitializer.finalization();
    rdfEntityManager.close();
  }

}
