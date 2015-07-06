/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.ahcsSupport;

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
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
public class ContainerInfoAccessTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerInfoAccessTest.class);
  // the RDF entity manager
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public ContainerInfoAccessTest() {
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
   * Test of class ContainerInfoAccess.
   */
  @Test
  public void initializeContainerInfos() {
    LOGGER.info("initializeContainerInfos");
    ContainerInfoAccess instance = new ContainerInfoAccess(rdfEntityManager, NetworkUtils.TEXAI_MAINNET);
    assertEquals("[]", instance.getContainerInfos().toString());
    instance.initializeContainerInfos();
    assertTrue(instance.areContainerInfosConsistent());
    LOGGER.info(instance.getContainerInfos());
    assertEquals(4, instance.getContainerInfos().size());
  }

  /**
   * Test of class ContainerInfoAccess.
   */
  @Test
  public void testLoadContainerInfos() {
    LOGGER.info("containerInfos");
    ContainerInfoAccess instance = new ContainerInfoAccess(rdfEntityManager, NetworkUtils.TEXAI_MAINNET);
    assertEquals("[]", instance.getContainerInfos().toString());
    ContainerInfo containerInfo = new ContainerInfo(
            "Mint", // containerName
            true, // isSuperPeer
            true, // isFirstContainer
            false, // isClientGateway
            false); // isBlockExplorer
    instance.addContainerInfo(containerInfo);
    assertEquals("[[container Mint, super peer, first container]]", instance.getContainerInfos().toString());
    instance.persistContainerInfos();
    instance.loadContainerInfos();
    assertEquals("[[container Mint, super peer, first container]]", instance.getContainerInfos().toString());

    containerInfo = new ContainerInfo(
            "Alice", // containerName
            true, // isSuperPeer
            false, // isFirstContainer
            true, // isClientGateway
            false); // isBlockExplorer
    instance.addContainerInfo(containerInfo);
    assertEquals("[[container Alice, super peer, gateway], [container Mint, super peer, first container]]", instance.getContainerInfos().toString());
    instance.persistContainerInfos();
    instance.loadContainerInfos();
    assertEquals("[[container Alice, super peer, gateway], [container Mint, super peer, first container]]", instance.getContainerInfos().toString());
    assertTrue(instance.areContainerInfosConsistent());
    containerInfo = new ContainerInfo(
            "Alice", // containerName
            true, // isSuperPeer
            false, // isFirstContainer
            true, // isClientGateway
            false); // isBlockExplorer
    containerInfo.addSuperPeerContainerName("xxx");
    instance.addContainerInfo(containerInfo);
    assertFalse(instance.areContainerInfosConsistent());
  }

}
