/*
 * NodeAccessTest.java
 *
 * Created on Jun 30, 2008, 8:28:10 AM
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

import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.RoleType;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class NodeAccessTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(NodeAccessTest.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public NodeAccessTest() {
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
    DistributedRepositoryManager.addTestRepositoryPath(
            "Nodes",
            true); // isRepositoryDirectoryCleaned
    final RoleTypeInitializer roleTypeInitializer =
            new RoleTypeInitializer();
    rdfEntityManager = new RDFEntityManager();
    roleTypeInitializer.initialize(rdfEntityManager); // clear repository first
    roleTypeInitializer.process("data/role-types-test.xml");
    roleTypeInitializer.finalization();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    final NodeTypeInitializer nodeTypeInitializer =
            new NodeTypeInitializer();
    nodeTypeInitializer.initialize(rdfEntityManager);
    nodeTypeInitializer.process("data/node-types-test.xml");
    nodeTypeInitializer.finalization();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of findRoleTypes method, of class NodeAccess.
   */
  @Test
  public void testFindRoleTypes() {
    LOGGER.info("findRoleTypes");
    final NodeAccess instance = new NodeAccess(rdfEntityManager);
    List<RoleType> result = instance.findRoleTypes("abc");
    assertNotNull(result);
    assertEquals("[]", result.toString());
    result = instance.findRoleTypes("TestRole");
    assertNotNull(result);
    assertEquals("[[RoleType TestRole]]", result.toString());
  }

  /**
   * Test of findRoleType method, of class NodeAccess.
   */
  @Test
  public void testFindRoleType() {
    LOGGER.info("findRoleType");
    final NodeAccess instance = new NodeAccess(rdfEntityManager);
    RoleType result = instance.findRoleType("abc");
    assertNull(result);
    result = instance.findRoleType("TestRole");
    assertNotNull(result);
    assertEquals("[RoleType TestRole]", result.toString());
  }

  /**
   * Test of findNodeTypes method, of class NodeAccess.
   */
  @Test
  public void testFindNodeTypes() {
    LOGGER.info("findNodeTypes");
    final NodeAccess instance = new NodeAccess(rdfEntityManager);
    List<NodeType> result = instance.findNodeTypes("abc");
    assertNotNull(result);
    assertEquals("[]", result.toString());
    result = instance.findNodeTypes("Universal");
    assertNotNull(result);
    assertEquals("[[NodeType Universal]]", result.toString());
  }

  /**
   * Test of findNodeType method, of class NodeAccess.
   */
  @Test
  public void testFindNodeType() {
    LOGGER.info("findNodeType");
    final NodeAccess instance = new NodeAccess(rdfEntityManager);
    NodeType result = instance.findNodeType("abc");
    assertNull(result);
    result = instance.findNodeType("Universal");
    assertNotNull(result);
    assertEquals("[NodeType Universal]", result.toString());
  }

  /**
   * Test of getRoleType method, of class NodeAccess.
   */
  @Test
  public void testGetRoleType() {
    LOGGER.info("getRoleType");

    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(
            roleType,
            nodeRuntime);
    final NodeAccess nodeAccess = new NodeAccess(rdfEntityManager);
    assertEquals("[RoleType MyRoleType]", nodeAccess.getRoleType(instance).toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[RoleType MyRoleType]", nodeAccess.getRoleType(loadedInstance).toString());
    assertEquals("[RoleType MyRoleType]", nodeAccess.getRoleType(loadedInstance.getId()).toString());
  }

}
