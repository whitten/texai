/*
 * TexaiLauncherAccessTest.java
 *
 * Created on Jun 30, 2008, 6:55:10 AM
 *
 * Description: .
 *
 * Copyright (C) Sep 16, 2011 reed.
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
package org.texai.texailauncher;

import org.texai.texaiLauncher.TexaiLauncherAccess;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.texai.kb.CacheInitializer;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.texaiLauncher.domainEntity.TexaiLauncherInfo;

/**
 *
 * @author reed
 */
public class TexaiLauncherAccessTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TexaiLauncherAccessTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public TexaiLauncherAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "Launcher",
            System.getenv("REPOSITORIES_TMPFS") + "/Launcher");
    rdfEntityManager = new RDFEntityManager();
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    if (rdfEntityManager != null) {
      rdfEntityManager.close();
    }
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }

  @Before
  public void setUp() {
    DistributedRepositoryManager.clearNamedRepository("Launcher");
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of findTexaiLauncherInfo method, of class TexaiLauncherAccess.
   */
  @Test
  public void testFindTexaiLauncherInfo() {
    LOGGER.info("findTexaiLauncherInfo");
    TexaiLauncherAccess instance = new TexaiLauncherAccess(rdfEntityManager);
    TexaiLauncherInfo result = instance.findTexaiLauncherInfo();
    assertNull(result);
    TexaiLauncherInfo texaiLauncherInfo = new TexaiLauncherInfo();
    instance.persistTexaiLauncherInfo(texaiLauncherInfo);
    result = instance.findTexaiLauncherInfo();
    assertNotNull(result);
  }

  /**
   * Test of findOrCreateTexaiLauncherInfo method, of class TexaiLauncherAccess.
   */
  @Test
  public void testFindOrCreateTexaiLauncherInfo() {
    LOGGER.info("findOrCreateTexaiLauncherInfo");
    TexaiLauncherAccess instance = new TexaiLauncherAccess(rdfEntityManager);
    TexaiLauncherInfo result = instance.findOrCreateTexaiLauncherInfo();
    assertNotNull(result);
  }
}
