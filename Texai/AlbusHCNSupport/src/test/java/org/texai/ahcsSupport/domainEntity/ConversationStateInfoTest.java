/*
 * ConversationStateInfoTest.java
 *
 * Created on Jun 30, 2008, 2:29:02 PM
 *
 * Description: .
 *
 * Copyright (C) Mar 15, 2010 reed.
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;

/**
 *
 * @author reed
 */
public class ConversationStateInfoTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ConversationStateInfoTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public ConversationStateInfoTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "NodeRoleTypes",
            true); // isRepositoryDirectoryCleaned
    DistributedRepositoryManager.addTestRepositoryPath(
            "Nodes",
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
   * Test of getId method, of class ConversationStateInfo.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals(instance.getId(), loadedInstance.getId());
  }

  /**
   * Test of getNode method, of class ConversationStateInfo.
   */
  @Test
  public void testGetNode() {
    LOGGER.info("getNode");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals(node.getId(), loadedInstance.getNode().getId());
    assertEquals(node.toString(), loadedInstance.getNode().toString());
  }

  /**
   * Test of getRoleForTypeName method, of class ConversationStateInfo.
   */
  @Test
  public void testGetRole() {
    LOGGER.info("getRole");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals(role.getId(), loadedInstance.getRole().getId());
    assertEquals(role.toString(), loadedInstance.getRole().toString());
  }

  /**
   * Test of getSkillClassName method, of class ConversationStateInfo.
   */
  @Test
  public void testGetSkillClassName() {
    LOGGER.info("getSkillClassName");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals("MySkill", loadedInstance.getSkillClassName());
  }

  /**
   * Test of getConversationId method, of class ConversationStateInfo.
   */
  @Test
  public void testGetConversationId() {
    LOGGER.info("getConversationID");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals(conversationId, loadedInstance.getConversationId());
  }

  /**
   * Test of getValue method, of class ConversationStateInfo.
   */
  @Test
  public void testGetValue() {
    LOGGER.info("getValue");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertEquals("{aVariable=abc}", stateVariableDictionary.toString());
    Object result = instance.getValue("aVariable");
    assertEquals("abc", result);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    result = loadedInstance.getValue("aVariable");
    assertEquals("abc", result);
  }

  /**
   * Test of putAll method, of class ConversationStateInfo.
   */
  @Test
  public void testPutAll() {
    LOGGER.info("putAll");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable1", "abc");
    final Map<String, Serializable> stateVariableDictionary1 = new HashMap<>();
    stateVariableDictionary1.put("aVariable2", "def");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertEquals("[ConversationStateInfo {aVariable1=abc}]", instance.toString());
    instance.putAll(stateVariableDictionary1);
    assertEquals("[ConversationStateInfo {aVariable2=def, aVariable1=abc}]", instance.toString());
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals("[ConversationStateInfo {aVariable2=def, aVariable1=abc}]", loadedInstance.toString());
    Object result = loadedInstance.getValue("aVariable1");
    assertEquals("abc", result);
    result = loadedInstance.getValue("aVariable2");
    assertEquals("def", result);
  }

  /**
   * Test of toString method, of class ConversationStateInfo.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
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
    Node node = new Node(
            nodeType, null);
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
    Role role = new Role(
            roleType,
            nodeRuntime);
    rdfEntityManager.persist(role);
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            node,
            role,
            "MySkill",
            conversationId,
            stateVariableDictionary);
    assertNull(instance.getId());
    assertTrue(instance instanceof RDFPersistent);
    rdfEntityManager.persist(instance);
    final URI id = instance.getId();
    assertNotNull(id);
    final ConversationStateInfo loadedInstance = rdfEntityManager.find(ConversationStateInfo.class, id);
    assertNotNull(loadedInstance);
    assertEquals("[ConversationStateInfo {aVariable=abc}]", loadedInstance.toString());
  }
}
