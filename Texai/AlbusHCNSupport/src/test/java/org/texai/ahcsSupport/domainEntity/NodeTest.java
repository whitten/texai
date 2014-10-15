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

import java.util.HashSet;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.BasicNodeRuntime;
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
    Node instance = makeTestNode();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    final Node loadedInstance = rdfEntityManager.find(Node.class, instance.getId());
    assertEquals(loadedInstance, instance);
  }

  /**
   * Test of toString method, of class Node.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    Node instance = makeTestNode();
    String result = instance.toString();
    assertEquals("TestNode", result);
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
    final BasicNodeRuntime nodeRuntime = new BasicNodeRuntime("TestContainer");
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
    assertEquals("TestNode", instance.getName());
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
    assertEquals(-1, instance.compareTo(instance2));
    assertEquals(0, instance.compareTo(instance));
    assertEquals(1, instance2.compareTo(instance));
  }

  /**
   * Test of hashCode method, of class Node.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    Node instance = makeTestNode();
    assertEquals(-1082308397, instance.hashCode());
    Node instance2 = makeTestNode2();
    assertEquals(808001501, instance2.hashCode());
  }

  /**
   * Makes a test node.
   *
   * @return a test node
   */
  public static Node makeTestNode() {
    final String name = "TestNode";
    final String missionDescription = "a test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles);
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
    final String name = "TestNode2";
    final String missionDescription = "a test node";
    final Set<Role> roles = new HashSet<>();
    Role testRole = makeTestRole();
    roles.add(testRole);
    Node testNode = new Node(
            name,
            missionDescription,
            roles);
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

}
