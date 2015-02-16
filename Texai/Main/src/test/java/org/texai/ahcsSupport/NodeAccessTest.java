/*
 * NodeAccessTest.java
 *
 * Created on Jun 30, 2008, 8:28:10 AM
 *
 * Description: .
 *
 * Copyright (C) May 10, 2010 reed.
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
import static org.junit.Assert.assertNull;
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
  private final static String CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH = "data/test-ContainerSingletonConfiguration.crt";

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
                    CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH); // configurationCertificateFilePath
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
    assertTrue(result.toString().startsWith(""));
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
   * Test of persistSkillClass method, of class NodeAccess.
   */
  @Test
  public void testPersistSkillClass() {
    LOGGER.info("persistSkillClass");
    SkillClass skillClass = makeTestSkillClass();
    NodeAccess instance = new NodeAccess(rdfEntityManager);
    SkillClass result = instance.findSkillClass(skillClass.getSkillClassName());
    assertNull(result);
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.INFO);
    instance.persistSkillClass(skillClass);
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    LOGGER.info("finding " + skillClass.getSkillClassName());
    result = instance.findSkillClass(skillClass.getSkillClassName());
    assertNotNull(result);
    LOGGER.info("removeSkillClass");
    instance.removeSkillClass(skillClass);
    result = instance.findSkillClass(skillClass.getSkillClassName());
    assertNull(result);
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
    final String skillClassName = "org.texai.skill.governance.MyClass";
    final boolean isClassExistsTested = false;
    return new SkillClass(
            skillClassName,
            isClassExistsTested);
  }

}
