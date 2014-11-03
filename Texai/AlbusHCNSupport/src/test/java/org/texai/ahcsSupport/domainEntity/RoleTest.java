/*
 * RoleTest.java
 *
 * Created on Jun 30, 2008, 10:17:20 PM
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

import java.util.Optional;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class RoleTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(RoleTest.class);
  /**
   * the RDF entity manager
   */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public RoleTest() {
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
   * Test of getId method, of class Role.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    Role instance = makeTestRole();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    Role loadedRole = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals(instance, loadedRole);
  }

  /**
   * Test of getNode & setNode method, of class Role.
   */
  @Test
  public void testGetNode() {
    LOGGER.info("getNode");
    Role instance = makeTestRole();
    assertNotNull(instance.getNode());
    assertEquals("TestContainer.TestAgent", instance.getNode().toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("TestContainer.TestAgent", loadedInstance.getNode().toString());
  }

  /**
   * Test of getRoleState & setRoleState methods, of class Role.
   */
  @Test
  public void testGetRoleState() {
    LOGGER.info("getRoleState");
    Role instance = makeTestRole();
    assertEquals(State.UNINITIALIZED, instance.getRoleState());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals(State.UNINITIALIZED, loadedInstance.getRoleState());
  }

  /**
   * Test of getSkills method, of class Role.
   */
  @Test
  public void testGetSkills() {
    LOGGER.info("getSkills");
    Role instance = makeTestRole();
    assertEquals("[]", instance.getSkills().toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[]", loadedInstance.getSkills().toString());
  }

  /**
   * Test of getParentQualifiedName method, of class Role.
   */
  @Test
  public void testGetParentQualifiedName() {
    LOGGER.info("getParentQualifiedName");
    Role instance = makeTestRole();
    assertNotNull(instance.getParentQualifiedName());
    assertEquals("TestContainer.TestParentAgent.TestParentRole", instance.getParentQualifiedName());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("TestContainer.TestParentAgent.TestParentRole", loadedInstance.getParentQualifiedName());
  }

  /**
   * Test of getChildQualifiedNames method, of class Role.
   */
  @Test
  public void testGetChildQualifiedNames() {
    LOGGER.info("getChildQualifiedNames");
    Role instance = makeTestRole();
    assertEquals("[]", instance.getChildQualifiedNames().toString());
  }

  /**
   * Test of getChildQualifiedNames method, of class Role.
   */
  @Test
  public void testGetChildQualifiedNames2() {
    LOGGER.info("getChildQualifiedNames");
    Node node = NodeTest.makeTestNodeWithChild();
    Optional<Role> optional = node.getRoles().stream().findFirst();
    if (optional.isPresent()) {
      assertEquals("[TestContainer.TestChildAgent.TestChildRole]", optional.get().getChildQualifiedNames().toString());
    } else {
      fail();
    }
  }

  /**
   * Test of getChildQualifiedNameForAgent method, of class Role.
   */
  @Test
  public void testGetChildQualifiedNameForAgent() {
    LOGGER.info("getChildQualifiedNameForAgent");
    Node node = NodeTest.makeTestNodeWithChild();
    Optional<Role> optional = node.getRoles().stream().findFirst();
    if (optional.isPresent()) {
      assertEquals("TestContainer.TestChildAgent.TestChildRole", optional.get().getChildQualifiedNameForAgent("TestChildAgent"));
      assertNull(optional.get().getChildQualifiedNameForAgent("ABCAgent"));
    } else {
      fail();
    }
  }

  /**
   * Test of getChildQualifiedNamesForAgent method, of class Role.
   */
  @Test
  public void testGetChildQualifiedNamesForAgent() {
    LOGGER.info("getChildQualifiedNamesForAgents");
    Node node = NodeTest.makeTestNodeWithChild();
    Optional<Role> optional = node.getRoles().stream().findFirst();
    if (optional.isPresent()) {
      assertEquals("[TestContainer.TestChildAgent.TestChildRole]", optional.get().getChildQualifiedNamesForAgent("TestChildAgent").toString());
      assertEquals("[]", optional.get().getChildQualifiedNamesForAgent("ABCAgent").toString());
    } else {
      fail();
    }
  }

 /**
   * Test of toString method, of class Role.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    Role instance = makeTestRole();
    assertEquals("[TestContainer.TestAgent.TestRole]", instance.toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[TestContainer.TestAgent.TestRole]", loadedInstance.toString());
  }

  /**
   * Makes a test role.
   *
   * @return a test role
   */
  public static Role makeTestRole() {
    final Node testNode = NodeTest.makeTestNode();
    final Optional<Role> optional = testNode.getRoles().stream().findFirst();
    assertNotNull(optional);
    return optional.get();
  }

}
