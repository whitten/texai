/*
 * NodeTest.java
 *
 * Created on Jun 30, 2008, 10:17:30 PM
 *
 * Description: .
 *
 * Copyright (C) May 6, 2010 reed.
 */
package org.texai.ahcsSupport.domainEntity;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.NetworkUtils;

/**
 *
 * @author reed
 */
public class NodeTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(NodeTest.class);
  /**
   * the RDF entity manager
   */
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
    Node instance = makeTestNode();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    final Node loadedInstance = rdfEntityManager.find(Node.class, instance.getId());
    assertEquals(loadedInstance, instance);
    loadedInstance.instantiate();
    loadedInstance.getRoles().stream().forEach ((Role role) -> {
      assertTrue(role.getNode().equals(loadedInstance));
      assertEquals(role.getNode().getId(), loadedInstance.getId());
      loadedInstance.setStateValue("var1", "value");
      assertEquals("value", role.getNode().getStateValue("var1"));
    });
  }

  /**
   * Test of extractRoleName method, of class Node.
   */
  @Test
  public void testExtractRoleName() {
    LOGGER.info("extractRoleName");
    assertEquals("role", Node.extractRoleName("container.agent.role"));
  }

  /**
   * Test of extractAgentName method, of class Node.
   */
  @Test
  public void testExtractAgentName() {
    LOGGER.info("extractAgentName");
    assertEquals("agent", Node.extractAgentName("container.agent.role"));
  }

  /**
   * Test of extractAgentRoleName method, of class Node.
   */
  @Test
  public void testExtractAgentRoleName() {
    LOGGER.info("extractAgentRoleName");
    assertEquals("agent.role", Node.extractAgentRoleName("container.agent.role"));
  }

  /**
   * Test of extractContainerName method, of class Node.
   */
  @Test
  public void testExtractContainerName() {
    LOGGER.info("extractContainerName");
    assertEquals("TestContainer", Node.extractContainerName("TestContainer.TestAgent.TestRole"));
  }

  /**
   * Test of extractContainerAgentName method, of class Node.
   */
  @Test
  public void testExtractContainerAgentName() {
    LOGGER.info("extractContainerAgentName");
    assertEquals("TestContainer.TestAgent", Node.extractContainerAgentName("TestContainer.TestAgent.TestRole"));
  }

  /**
   * Test of toString method, of class Node.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    Node instance = makeTestNode();
    String result = instance.toString();
    assertEquals("TestContainer.TestAgent", result);
  }

  /**
   * Test of getStateValue, setStateValue and removeStateValueBinding methods, of class Node.
   */
  @Test
  public void testGetStateValue() {
    LOGGER.info("getStateValue");
    Node instance = makeTestNode();
    Object stateValue = instance.getStateValue("testVariable");
    assertEquals("1", stateValue.toString());
    stateValue = ((int) stateValue) + 1;
    assertEquals("2", stateValue.toString());
    instance.setStateValue("testVariable", stateValue);
    assertEquals("2", stateValue.toString());
    instance.removeStateValueBinding("testVariable");
    assertNull(instance.getStateValue("testVariable"));
  }

  /**
   * Test of getRoles method, of class Node.
   */
  @Test
  public void testGetRoles() {
    LOGGER.info("getRoles");
    Node instance = makeTestNode();
    assertEquals("[[TestContainer.TestAgent.TestRole]]", instance.getRoles().toString());
  }

  /**
   * Test of getNodeRuntime and setNodeRuntime methods, of class Node.
   */
  @Test
  public void testGetNodeRuntime() {
    LOGGER.info("getNodeRuntime");
    Node instance = makeTestNode();
    assertNull(instance.getNodeRuntime());
    final String networkName = NetworkUtils.TEXAI_TESTNET;
    final BasicNodeRuntime nodeRuntime = new BasicNodeRuntime("TestContainer", networkName);
    instance.setNodeRuntime(nodeRuntime);
    assertNotNull(instance.getNodeRuntime());
    assertEquals(instance.getNodeRuntime(), nodeRuntime);
  }

  /**
   * Test of getName method, of class Node.
   */
  @Test
  public void testGetName() {
    LOGGER.info("getName");
    Node instance = makeTestNode();
    assertEquals("TestContainer.TestAgent", instance.getName());
  }

  /**
   * Test of extractAgentName method, of class Node.
   */
  @Test
  public void testExtractAgentName2() {
    LOGGER.info("extractAgentName");
    Node instance = makeTestNode();
    assertEquals("TestAgent", instance.extractAgentName());
  }

  /**
   * Test of getMissionDescription method, of class Node.
   */
  @Test
  public void testMissionDescription() {
    LOGGER.info("getMissionDescription");
    Node instance = makeTestNode();
    assertEquals("a test node", instance.getMissionDescription());
  }

  /**
   * Test of compareTo method, of class Node.
   */
  @Test
  public void testCompareTo() {
    LOGGER.info("compareTo");
    Node instance = makeTestNode();
    Node instance2 = makeTestNode2();
    assertTrue(instance.compareTo(instance2) < 0);
    assertTrue(instance.compareTo(instance) == 0);
    assertTrue(instance2.compareTo(instance) > 0);
  }

  /**
   * Test of hashCode method, of class Node.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    Node instance = makeTestNode();
    assertEquals(-1896275661, instance.hashCode());
    Node instance2 = makeTestNode2();
    assertEquals(-536158921, instance2.hashCode());
  }

  /**
   * Makes a test node.
   *
   * @return a test node
   */
  public static Node makeTestNode() {
    final String name = "TestContainer.TestAgent";
    final String missionDescription = "a test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles,
            false); // isNetworkSingleton
    testRole.setNode(testNode);
    testNode.setStateValue("testVariable", 1);
    return testNode;
  }

  /**
   * Makes a second test node.
   *
   * @return a test node
   */
  public static Node makeTestNode2() {
    final String name = "TestContainer.XestAgent";
    final String missionDescription = "a 2nd test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles,
            false); // isNetworkSingleton
    testRole.setNode(testNode);
    testNode.setStateValue("testVariable", 1);
    return testNode;
  }

  /**
   * Makes a test node with two roles.
   *
   * @return a test node
   */
  public static Node makeTestNode3() {
    final String name = "TestContainer.XestAgent";
    final String missionDescription = "a 3nd test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Role testRole2 = makeTestRole2();
    roles.add(testRole2);
    Node testNode = new Node(
            name,
            missionDescription,
            roles,
            false); // isNetworkSingleton
    testRole.setNode(testNode);
    testNode.setStateValue("testVariable", 1);
    return testNode;
  }

  /**
   * Makes a test role.
   *
   * @return a test role
   */
  private static Role makeTestRole() {
    final String qualifiedName = "TestContainer.TestAgent.TestRole";
    final String description = "a test role";
    final String parentQualifiedName = "TestContainer.TestParentAgent.TestParentRole";
    final Set<String> childQualifiedNames = new HashSet<>();
    final Set<SkillClass> skillClasses = new HashSet<>();
    skillClasses.add(SkillClassTest.makeTestSkillClass());
    final Set<String> variableNames = new HashSet<>();
    variableNames.add("testVariable");
    final boolean areRemoteCommunicationsPermitted = false;
    return new Role(
            qualifiedName,
            description,
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            areRemoteCommunicationsPermitted);
  }

  /**
   * Makes a test role.
   *
   * @return a test role
   */
  private static Role makeTestRole2() {
    final String qualifiedName = "TestContainer.TestAgent.TestRole2";
    final String description = "a test role";
    final String parentQualifiedName = "TestContainer.TestParentAgent.TestParentRole";
    final Set<String> childQualifiedNames = new HashSet<>();
    final Set<SkillClass> skillClasses = new HashSet<>();
    skillClasses.add(SkillClassTest.makeTestSkillClass());
    final Set<String> variableNames = new HashSet<>();
    variableNames.add("testVariable");
    final boolean areRemoteCommunicationsPermitted = false;
    return new Role(
            qualifiedName,
            description,
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            areRemoteCommunicationsPermitted);
  }

  /**
   * Makes a test node with a child role.
   *
   * @return a test node
   */
  public static Node makeTestNodeWithChild() {
    final String name = "TestContainer.TestAgent";
    final String missionDescription = "a test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles,
            true); // isNetworkSingleton
    testRole.setNode(testNode);
    testNode.setStateValue("testVariable", 1);

    final Node testChildNode =  makeTestChildNode();
    final Optional<Role> optional = testChildNode.getRoles().stream().findFirst();
    if (optional.isPresent()) {
       testRole.getChildQualifiedNames().add(optional.get().getQualifiedName());
    } else {
      fail();
    }


    return testNode;
  }

  /**
   * Makes a test child node.
   *
   * @return a test child node
   */
  public static Node makeTestChildNode() {
    final String name = "TestContainer.TestChildAgent";
    final String missionDescription = "a child test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestChildRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles,
            false); // isNetworkSingleton
    testRole.setNode(testNode);
    testNode.setStateValue("testVariable", 1);
    return testNode;
  }

  /**
   * Makes a test child role.
   *
   * @return a test child role
   */
  private static Role makeTestChildRole() {
    final String qualifiedName = "TestContainer.TestChildAgent.TestChildRole";
    final String description = "a test role";
    final String parentQualifiedName = "TestContainer.TestParentAgent.TestParentRole";
    final Set<String> childQualifiedNames = new HashSet<>();
    final Set<SkillClass> skillClasses = new HashSet<>();
    skillClasses.add(SkillClassTest.makeTestSkillClass());
    final Set<String> variableNames = new HashSet<>();
    variableNames.add("testVariable");
    final boolean areRemoteCommunicationsPermitted = false;
    return new Role(
            qualifiedName,
            description,
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            areRemoteCommunicationsPermitted);
  }

}
