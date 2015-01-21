/*
 * ConversationStateInfoTest.java
 *
 * Created on Jun 30, 2008, 2:29:02 PM
 *
 * Description: .
 *
 * Copyright (C) Mar 15, 2010 reed.
 */
package org.texai.ahcsSupport.domainEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    assertEquals(testNode.getId(), loadedInstance.getNode().getId());
    assertEquals(testNode.toString(), loadedInstance.getNode().toString());
  }

  /**
   * Test of getRoleForTypeName method, of class ConversationStateInfo.
   */
  @Test
  public void testGetRole() {
    LOGGER.info("getRole");
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    assertEquals(testRole.getId(), loadedInstance.getRole().getId());
    assertEquals(testRole.toString(), loadedInstance.getRole().toString());
  }

  /**
   * Test of getSkillClassName method, of class ConversationStateInfo.
   */
  @Test
  public void testGetSkillClassName() {
    LOGGER.info("getSkillClassName");
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable1", "abc");
    final Map<String, Serializable> stateVariableDictionary1 = new HashMap<>();
    stateVariableDictionary1.put("aVariable2", "def");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
    Node testNode = NodeTest.makeTestNode();
    Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    Role testRole = optional.get();
    final UUID conversationId = UUID.randomUUID();
    final Map<String, Serializable> stateVariableDictionary = new HashMap<>();
    stateVariableDictionary.put("aVariable", "abc");
    ConversationStateInfo instance = new ConversationStateInfo(
            testNode,
            testRole,
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
