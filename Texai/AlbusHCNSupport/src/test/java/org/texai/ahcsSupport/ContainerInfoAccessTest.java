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
  public void testLoadContainerInfos() {
    LOGGER.info("containerInfos");
    ContainerInfoAccess instance = new ContainerInfoAccess(rdfEntityManager, NetworkUtils.TEXAI_MAINNET);
    assertEquals("[]", instance.getContainerInfos().toString());
    instance.addContainerInfo(new ContainerInfo("Mint"));
    assertEquals("[[container Mint]]", instance.getContainerInfos().toString());
    instance.persistContainerInfos();
    instance.loadContainerInfos();
    assertEquals("[[container Mint]]", instance.getContainerInfos().toString());
    instance.addContainerInfo(new ContainerInfo("Alice"));
    assertEquals("[[container Alice], [container Mint]]", instance.getContainerInfos().toString());
    instance.persistContainerInfos();
    instance.loadContainerInfos();
    assertEquals("[[container Alice], [container Mint]]", instance.getContainerInfos().toString());
  }

}
