/*
 * NodeTypeInitializerTest.java
 *
 * Created on Jun 30, 2008, 8:16:37 AM
 *
 * Description: .
 *
 * Copyright (C) May 10, 2010 reed.
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
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.RoleType;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class ReportNodeAndRoleTypeIDsTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(ReportNodeAndRoleTypeIDsTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public ReportNodeAndRoleTypeIDsTest() {
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
    final RoleTypeInitializer roleTypeInitializer =
            new RoleTypeInitializer();
    rdfEntityManager = new RDFEntityManager();
    roleTypeInitializer.initialize(rdfEntityManager); // clear repository first
    // load production role types
    roleTypeInitializer.process("../Main/data/role-types.xml");
    roleTypeInitializer.finalization();
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
   * Test of class NodeTypeInitializer.
   */
  @Test
  public void testProcess() {
    LOGGER.info("beginning test");
    final NodeTypeInitializer nodeTypeInitializer =
            new NodeTypeInitializer();
    nodeTypeInitializer.initialize(rdfEntityManager);
    // load production node types
    nodeTypeInitializer.process("../Main/data/node-types.xml");
    nodeTypeInitializer.finalization();

    final NodeAccess nodeAccess = new NodeAccess(rdfEntityManager);
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("\nnode types ...\n");
    stringBuilder.append("<node-types>\n");
    for (final NodeType nodeType : nodeAccess.getNodeTypes()) {
      stringBuilder.append(nodeType.toXML(2));
      stringBuilder.append('\n');
    }
    stringBuilder.append("</node-types>\n");

    stringBuilder.append("\nrole types ...\n");
    stringBuilder.append("<role-types>\n");
    for (final RoleType roleType : nodeAccess.getRoleTypes()) {
      stringBuilder.append(roleType.toXML(2));
      stringBuilder.append('\n');
    }
    stringBuilder.append("</role-types>\n");
    LOGGER.info(stringBuilder.toString());
  }

}
