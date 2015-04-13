/*
 * Copyright (C) 2015 Texai
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.texai.skill.support.domainEntity;

import org.texai.ahcsSupport.domainEntity.ContainerInfo;
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

/**
 *
 * @author reed
 */
public class ContainerInfoTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerInfo.class);
  // the RDF entity manager
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public ContainerInfoTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "Nodes",
            true); // isRepositoryDirectoryCleaned
  }

  @AfterClass
  public static void tearDownClass() {
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
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    final ContainerInfo loadedInstance = rdfEntityManager.find(ContainerInfo.class, instance.getId());
    assertEquals(loadedInstance.toString(), instance.toString());
  }
  /**
   * Test of getContainerName method, of class NodeInfo.
   */
  @Test
  public void testGetContainerName() {
    LOGGER.info("getContainerName");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertEquals("test-container", instance.getContainerName());
  }

  /**
   * Test of getIpAddress method, of class NodeInfo.
   */
  @Test
  public void testGetIpAddress() {
    LOGGER.info("getIpAddress");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertNull(instance.getIpAddress());
    instance.setIpAddress("127.0.0.1");
    assertEquals("127.0.0.1", instance.getIpAddress());
  }

  /**
   * Test of isSuperPeer method, of class NodeInfo.
   */
  @Test
  public void testIsSuperPeer() {
    LOGGER.info("isSuperPeer");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertTrue(instance.isSuperPeer());
    final ContainerInfo instance2 = new ContainerInfo(
              "test-container2",
              false, // isSuperPeer
              false, // isFirstContainer
              true, // isClientGateway
              false); // isBlockExplorer
    assertTrue(instance.isSuperPeer());
  }

  /**
   * Test of hashCode method, of class NodeInfo.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertEquals(923362937, instance.hashCode());
  }

  /**
   * Test of equals method, of class NodeInfo.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    final ContainerInfo instance2 = new ContainerInfo(
              "test-container2",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertFalse(instance.equals(instance2));
    final ContainerInfo instance3 = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertTrue(instance.equals(instance3));
  }

  /**
   * Test of toString method, of class NodeInfo.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final ContainerInfo instance = new ContainerInfo(
              "test-container",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    assertEquals("[container test-container, super peer]", instance.toString());
    instance.setIpAddress("127.0.0.1");
    assertEquals("[container test-container,  127.0.0.1, super peer]", instance.toString());
    instance.setIsAlive(true);
    assertEquals("[container test-container,  127.0.0.1, alive, super peer]", instance.toString());
    final ContainerInfo instance2 = new ContainerInfo(
              "test-container2",
              false, // isSuperPeer
              false, // isFirstContainer
              true, // isClientGateway
              true); // isBlockExplorer
    assertEquals("[container test-container2, gateway, block explorer]", instance2.toString());
  }

}
