/*
 * NodeTest.java
 *
 * Created on Jun 30, 2008, 10:17:30 PM
 *
 * Description: .
 *
 * Copyright (C) May 6, 2010 reed.
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
package org.texai.ahcsSupport.domainEntity;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.ahcsSupport.NodeAccess;
import static org.junit.Assert.*;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class NodeTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public NodeTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "Nodes",
            true); // isRepositoryDirectoryCleaned
    DistributedRepositoryManager.addTestRepositoryPath(
            "NodeRoleTypes",
            true); // isRepositoryDirectoryCleaned
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
   * Test of getId method, of class Node.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    Node instance = new Node();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
  }

  /**
   * Test of getNodeType method, of class Node.
   */
  @Test
  public void testGetNodeType() {
    LOGGER.info("getNodeType");
    NodeType nodeType = new NodeType();
    nodeType.setTypeName("MyTypeName");
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    nodeType.addInheritedNodeType(inheritedNodeType);
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    nodeType.addRoleType(roleType);
    nodeType.setMissionDescription("my mission description");
    rdfEntityManager.persist(nodeType);
    Node instance = new Node(
            nodeType, null);
    final NodeAccess nodeAccess = new NodeAccess(rdfEntityManager);
    assertEquals("[NodeType MyTypeName]", nodeAccess.getNodeType(instance).toString());
    rdfEntityManager.persist(instance);
    Node loadedInstance = rdfEntityManager.find(Node.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("[NodeType MyTypeName]", nodeAccess.getNodeType(loadedInstance).toString());
  }

  /**
   * Test of toString method, of class Node.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    Node instance = new Node();
    String result = instance.toString();
    assertEquals("[Node]", result);

    NodeType nodeType = new NodeType();
    nodeType.setTypeName("MyTypeName");
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    nodeType.addInheritedNodeType(inheritedNodeType);
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    nodeType.addRoleType(roleType);
    nodeType.setMissionDescription("my mission description");
    rdfEntityManager.persist(nodeType);
    instance = new Node(
            nodeType,
            null);
    instance.setNodeNickname("MyNodeNickname");
    assertEquals("[MyNodeNickname: MyTypeName]", instance.toString());
    rdfEntityManager.persist(instance);
    Node loadedInstance = rdfEntityManager.find(Node.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("[MyNodeNickname: MyTypeName]", loadedInstance.toString());
  }
}
