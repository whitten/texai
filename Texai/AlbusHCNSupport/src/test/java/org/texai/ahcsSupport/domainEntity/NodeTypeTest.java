/*
 * NodeTypeTest.java
 *
 * Created on Jun 30, 2008, 6:44:56 PM
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
import static org.junit.Assert.*;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class NodeTypeTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeTypeTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public NodeTypeTest() {
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
   * Test of getId method, of class NodeType.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    NodeType instance = new NodeType();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals(instance.getId(), loadedInstance.getId());
  }

  /**
   * Test of getTypeName & setTypeName method, of class NodeType.
   */
  @Test
  public void testGetTypeName() {
    LOGGER.info("getTypeName");
    NodeType instance = new NodeType();
    assertNull(instance.getTypeName());
    instance.setTypeName("MyTypeName");
    assertEquals("MyTypeName", instance.getTypeName());
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertEquals("MyTypeName", loadedInstance.getTypeName());
  }

  /**
   * Test of getInheritedNodeTypes & addInheritedNodeType & removeInheritedNodeType method, of class NodeType.
   */
  @Test
  public void testGetInheritedNodeTypes() {
    LOGGER.info("getInheritedNodeTypes");
    NodeType instance = new NodeType();
    assertEquals("[]", instance.getInheritedNodeTypes().toString());
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    instance.addInheritedNodeType(inheritedNodeType);
    assertEquals("[[NodeType MyInheritedNodeType]]", instance.getInheritedNodeTypes().toString());
    instance.removeInheritedNodeType(inheritedNodeType);
    assertEquals("[]", instance.getInheritedNodeTypes().toString());
    instance.addInheritedNodeType(inheritedNodeType);
    assertEquals("[[NodeType MyInheritedNodeType]]", instance.getInheritedNodeTypes().toString());
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    loadedInstance.getInheritedNodeTypes().size(); // instantiate lazy set
    assertEquals("[[NodeType MyInheritedNodeType]]", loadedInstance.getInheritedNodeTypes().toString());
  }

  /**
   * Test of getRoleTypes & addRoleType & removeRoleType method, of class NodeType.
   */
  @Test
  public void testGetRoleTypes() {
    LOGGER.info("getRoleTypes");
    NodeType instance = new NodeType();
    assertEquals("[]", instance.getRoleTypes().toString());
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    instance.addRoleType(roleType);
    assertEquals("[[RoleType MyRoleType]]", instance.getRoleTypes().toString());
    instance.removeRoleType(roleType);
    assertEquals("[]", instance.getRoleTypes().toString());
    instance.addRoleType(roleType);
    assertEquals("[[RoleType MyRoleType]]", instance.getRoleTypes().toString());
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    loadedInstance.getRoleTypes().size(); // instantiate lazy set
    assertEquals("[[RoleType MyRoleType]]", loadedInstance.getRoleTypes().toString());
  }

  /**
   * Test of getAllRoleTypesof class NodeType.
   */
  @Test
  public void testGetAllRoleTypes() {
    NodeType instance = new NodeType();
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    instance.addRoleType(roleType);
    assertEquals("[[RoleType MyRoleType]]", instance.getAllRoleTypes().toString());
    rdfEntityManager.persist(instance);
    NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    loadedInstance.getInheritedNodeTypes().size(); // instantiate lazy set
    assertEquals("[[RoleType MyRoleType]]", loadedInstance.getAllRoleTypes().toString());
    loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertEquals("[NodeType]", loadedInstance.toString());
    rdfEntityManager.persist(instance);
    loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertEquals("[NodeType]", loadedInstance.toString());

    assertEquals("[]", instance.getInheritedNodeTypes().toString());
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    RoleType inheritedRoleType = new RoleType();
    inheritedRoleType.setTypeName("MyInheritedRoleType");
    inheritedNodeType.addRoleType(inheritedRoleType);
    rdfEntityManager.persist(inheritedNodeType);
    NodeType loadedInheritedNodeType = rdfEntityManager.find(NodeType.class, inheritedNodeType.getId());
    assertEquals("[NodeType MyInheritedNodeType]", loadedInheritedNodeType.toString());

    instance.addInheritedNodeType(inheritedNodeType);
    assertEquals("[[NodeType MyInheritedNodeType]]", instance.getInheritedNodeTypes().toString());
    instance.removeInheritedNodeType(inheritedNodeType);
    assertEquals("[]", instance.getInheritedNodeTypes().toString());
    instance.addInheritedNodeType(inheritedNodeType);
    assertEquals("[[NodeType MyInheritedNodeType]]", instance.getInheritedNodeTypes().toString());
    LOGGER.info("persisting mutated instance ...");
    rdfEntityManager.persist(instance);
    loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    loadedInstance.getInheritedNodeTypes().size(); // instantiate lazy set
    assertEquals("[NodeType]", loadedInstance.toString());
    assertEquals("[[NodeType MyInheritedNodeType]]", loadedInstance.getInheritedNodeTypes().toString());
    assertEquals("[[RoleType MyInheritedRoleType], [RoleType MyRoleType]]", StringUtils.toSortedStrings(loadedInstance.getAllRoleTypes()).toString());
  }

  /**
   * Test of getMissionDescription & setMissionDescription method, of class NodeType.
   */
  @Test
  public void testGetMissionDescription() {
    LOGGER.info("getMissionDescription");
    NodeType instance = new NodeType();
    assertNull(instance.getMissionDescription());
    instance.setMissionDescription("my mission description");
    assertEquals("my mission description", instance.getMissionDescription());
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("my mission description", loadedInstance.getMissionDescription());
  }

  /**
   * Test of equals method, of class NodeType.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    Object obj = new Object();
    NodeType instance = new NodeType();
    instance.setTypeName("MyTypeName");
    assertFalse(instance.equals(obj));
    NodeType nodeType = new NodeType();
    nodeType.setTypeName("MyTypeName");
    assertEquals(instance, nodeType);
    nodeType.setTypeName("AnotherTypeName");
    assertFalse(instance.equals(nodeType));
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertEquals(instance, loadedInstance);
  }

  /**
   * Test of hashCode method, of class NodeType.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    NodeType instance = new NodeType();
    instance.setTypeName("MyTypeName");
    assertEquals(1777808084, instance.hashCode());
    rdfEntityManager.persist(instance);
    final NodeType loadedInstance = rdfEntityManager.find(NodeType.class, instance.getId());
    assertEquals(1777808084, loadedInstance.hashCode());
  }

  /**
   * Test of toXML method, of class NodeType.
   */
  @Test
  public void testToXML_0args() {
    LOGGER.info("toXML");
    NodeType instance = new NodeType();
    instance.setTypeName("MyTypeName");
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    instance.addInheritedNodeType(inheritedNodeType);
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    instance.addRoleType(roleType);
    instance.setMissionDescription("my mission description");
    assertEquals(
            "<node-type>\n" +
            "  <name>MyTypeName</name>\n" +
            "  <mission>my mission description</mission>\n" +
            "  <inherited-node-type-names>\n" +
            "    <inherited-node-type-name>MyInheritedNodeType</inherited-node-type-name>\n" +
            "  </inherited-node-type-names>\n" +
            "  <role-type-names>\n" +
            "    <role-type-name>MyRoleType</role-type-name>\n" +
            "  </role-type-names>\n" +
            "</node-type>\n", instance.toXML());
  }

  /**
   * Test of toXML method, of class NodeType.
   */
  @Test
  public void testToXML_int() {
    LOGGER.info("toXML");
    NodeType instance = new NodeType();
    instance.setTypeName("MyTypeName");
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    instance.addInheritedNodeType(inheritedNodeType);
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    instance.addRoleType(roleType);
    instance.setMissionDescription("my mission description");
    assertEquals(
            "<node-type>\n" +
            "  <name>MyTypeName</name>\n" +
            "  <mission>my mission description</mission>\n" +
            "  <inherited-node-type-names>\n" +
            "    <inherited-node-type-name>MyInheritedNodeType</inherited-node-type-name>\n" +
            "  </inherited-node-type-names>\n" +
            "  <role-type-names>\n" +
            "    <role-type-name>MyRoleType</role-type-name>\n" +
            "  </role-type-names>\n" +
            "</node-type>\n", instance.toXML(0));
    assertEquals(
            "    <node-type>\n" +
            "      <name>MyTypeName</name>\n" +
            "      <mission>my mission description</mission>\n" +
            "      <inherited-node-type-names>\n" +
            "        <inherited-node-type-name>MyInheritedNodeType</inherited-node-type-name>\n" +
            "      </inherited-node-type-names>\n" +
            "      <role-type-names>\n" +
            "        <role-type-name>MyRoleType</role-type-name>\n" +
            "      </role-type-names>\n" +
            "    </node-type>\n", instance.toXML(4));
  }
}
