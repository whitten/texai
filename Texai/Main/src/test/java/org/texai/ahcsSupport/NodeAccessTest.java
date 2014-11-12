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

import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
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

  /**
   * the log4j logger
   */
  private static final Logger LOGGER = Logger.getLogger(NodeAccessTest.class);
  /**
   * the RDF entity manager
   */
  private static RDFEntityManager rdfEntityManager;
  // the test keystore path
  private final static String KEY_STORE_FILE_NAME = "data/test-keystore.uber";
  // the test configuration certificate path
  private final static String SINGLETON_CONFIGURATION_FILE_NAME = "data/test-SingletonConfiguration.crt";

  public NodeAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "Nodes",
            true); // isRepositoryDirectoryCleaned
    rdfEntityManager = new RDFEntityManager();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
//    LOGGER.info("deleting " + KEY_STORE_FILE_NAME);
//    (new File(KEY_STORE_FILE_NAME)).delete();
    final String containerName = "TestContainer";
    final BasicNodeRuntime nodeRuntime = new BasicNodeRuntime(containerName);
    final char[] keystorePassword = "test-password".toCharArray();
    final NodesInitializer nodesInitializer
            = new NodesInitializer(
                    false, // isClassExistsTested
                    keystorePassword,
                    nodeRuntime,
                    KEY_STORE_FILE_NAME, // keyStoreFilePath
                    SINGLETON_CONFIGURATION_FILE_NAME); // configurationCertificateFilePath
    nodesInitializer.process(
            "data/nodes-test.xml", // nodesPath
            NodesInitializerTest.NODES_TEST_HASH); // nodesFileHashString
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
   * Test of getNodes method, of class NodeAccess.
   */
  @Test
  public void testGetNodes() {
    LOGGER.info("getNodes");
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    List<Node> result = instance.getNodes();
    Collections.sort(result);
    LOGGER.info(result.toString());
    assertTrue(result.toString().startsWith("[TestContainer.ContainerGovernanceAgent, TestContainer.ContainerHeartbeatAgent, "));
  }

  /**
   * Test of persistNode method, of class NodeAccess.
   */
  @Test
  public void testPersistNode() {
    LOGGER.info("persistNode");
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    Optional<Node> optional = instance.getNodes().stream().findFirst();
    assertNotNull(optional);
    assertTrue(optional.isPresent());
    Node node = optional.get();
    instance.persistNode(node);
    Node loadedNode = rdfEntityManager.find(Node.class, node.getId());
    assertEquals(loadedNode, node);
  }

  /**
   * Test of persistRole method, of class NodeAccess.
   */
  @Test
  public void testPersistRole() {
    LOGGER.info("persistRole");
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    Optional<Node> optional = instance.getNodes().stream().findFirst();
    assertNotNull(optional);
    assertTrue(optional.isPresent());
    Node node = optional.get();
    Optional<Role> optional1 = node.getRoles().stream().findFirst();
    assertNotNull(optional1);
    assertTrue(optional1.isPresent());
    Role role = optional1.get();
    instance.persistRole(role);
  }

  /**
   * Test of findSkillClass method, of class NodeAccess.
   */
  @Test
  public void testFindSkillClass() {
    LOGGER.info("findSkillClass");
    SkillClass skillClass1 = makeTestSkillClass();
    String skillClassName = skillClass1.getName();
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    SkillClass expResult = null;
    SkillClass result = instance.findSkillClass(skillClassName);
    assertEquals(expResult, result);
  }

  /**
   * Test of persistSkillClass method, of class NodeAccess.
   */
  @Test
  public void testPersistSkillClass() {
    LOGGER.info("persistSkillClass");
    SkillClass skillClass = makeTestSkillClass();
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    instance.persistSkillClass(skillClass);
  }

  /**
   * Test of removeSkillClass method, of class NodeAccess.
   */
  @Test
  public void testRemoveSkillClass() {
    LOGGER.info("removeSkillClass");
    SkillClass skillClass = makeTestSkillClass();
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    instance.removeSkillClass(skillClass);
  }

  /**
   * Test of getRDFEntityManager method, of class NodeAccess.
   */
  @Test
  public void testGetRDFEntityManager() {
    LOGGER.info("getRDFEntityManager");
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    RDFEntityManager result = instance.getRDFEntityManager();
    assertEquals(rdfEntityManager, result);
  }

  /**
   * Makes a test skill class.
   *
   * @return a test skill class
   */
  public static SkillClass makeTestSkillClass() {
    final String skillClassName = "org.texai.skill.governance.Governance";
    final boolean isClassExistsTested = false;
    return new SkillClass(
            skillClassName,
            isClassExistsTested);
  }

}
