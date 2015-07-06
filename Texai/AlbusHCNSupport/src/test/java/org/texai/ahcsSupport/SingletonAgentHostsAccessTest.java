/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.ahcsSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
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
public class SingletonAgentHostsAccessTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SingletonAgentHostsAccessTest.class);
  // the RDF entity manager
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public SingletonAgentHostsAccessTest() {
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
   * Test of initializeSingletonAgentsHosts method, of class SingletonAgentHostsAccess.
   */
  @Test
  public void testInitializeSingletonAgentsHosts() {
    LOGGER.info("initializeSingletonAgentsHosts");
    final BasicNodeRuntime basicNodeRuntime = new MockBasicNodeRuntime();
    SingletonAgentHostsAccess instance = new SingletonAgentHostsAccess(
            rdfEntityManager,
            basicNodeRuntime);
    instance.initializeSingletonAgentsHosts();
    assertEquals("[SingletonAgentHosts, size 11\n"
            + "  AICNetworkSeedAgent=turing\n"
            + "  AICRecoveryAgent=turing\n"
            + "  AICPrimaryAuditAgent=turing\n"
            + "  AICRewardAllocationAgent=turing\n"
            + "  AICNetworkEpisodicMemoryAgent=turing\n"
            + "  AICNetworkOperationAgent=turing\n"
            + "  NetworkOperationAgent=turing\n"
            + "  AICMintAgent=turing\n"
            + "  TopmostFriendshipAgent=turing\n"
            + "  AICFinancialAccountingAndControlAgent=turing\n"
            + "  NetworkSingletonConfigurationAgent=turing\n"
            + "]", instance.getEffectiveSingletonAgentHosts().toDetailedString());
  }

  /**
   * Test of loadSingletonAgentHosts method, of class SingletonAgentHostsAccess.
   */
  @Test
  public void testLoadSingletonAgentHosts() {
    LOGGER.info("loadSingletonAgentHosts");
    final BasicNodeRuntime basicNodeRuntime = new MockBasicNodeRuntime();
    SingletonAgentHostsAccess instance = new SingletonAgentHostsAccess(
            rdfEntityManager,
            basicNodeRuntime);
    instance.loadSingletonAgentHosts();
    SingletonAgentHosts result = instance.getEffectiveSingletonAgentHosts();
    assertEquals("[SingletonAgentHosts, size 11]", result.toString());
  }

  /**
   * Test of isSingletonAgent method, of class SingletonAgentHostsAccess.
   */
  @Test
  public void testIsSingletonAgent() {
    LOGGER.info("isSingletonAgent");

    final BasicNodeRuntime basicNodeRuntime = new MockBasicNodeRuntime();
    SingletonAgentHostsAccess instance = new SingletonAgentHostsAccess(
            rdfEntityManager,
            basicNodeRuntime);
    instance.initializeSingletonAgentsHosts();
    LOGGER.info(instance.getEffectiveSingletonAgentHosts());

    assertTrue(instance.isSingletonAgent("NetworkOperationAgent"));
  }

  /**
   * Test of getEffectiveSingletonAgentHosts method, of class SingletonAgentHostsAccess.
   */
  @Test
  public void testGetEffectiveSingletonAgentHosts() {
    LOGGER.info("getEffectiveSingletonAgentHosts");
    final BasicNodeRuntime basicNodeRuntime = new MockBasicNodeRuntime();
    SingletonAgentHostsAccess instance = new SingletonAgentHostsAccess(
            rdfEntityManager,
            basicNodeRuntime);
    instance.initializeSingletonAgentsHosts();
    SingletonAgentHosts result = instance.getEffectiveSingletonAgentHosts();
    assertEquals("[SingletonAgentHosts, size 11]", result.toString());
  }

  /**
   * Test serialization.
   */
  @Test
  public void testSerialization() {
    LOGGER.info("serialization");
    ObjectOutputStream objectOutputStream = null;
    try {
      final BasicNodeRuntime basicNodeRuntime = new MockBasicNodeRuntime();
      SingletonAgentHostsAccess instance = new SingletonAgentHostsAccess(
              rdfEntityManager,
              basicNodeRuntime);
      instance.initializeSingletonAgentsHosts();
      final SingletonAgentHosts singletonAgentHosts = instance.getEffectiveSingletonAgentHosts();
      assertNotNull(singletonAgentHosts);

      // serialize
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(singletonAgentHosts);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize
      final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
      final InputStream inputStream = new ByteArrayInputStream(serializedBytes);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object result = objectInputStream.readObject();
      assert result instanceof SingletonAgentHosts;
      assertEquals(result, singletonAgentHosts);

    } catch (IOException | ClassNotFoundException ex) {
      fail(ex.getMessage());
    } finally {
      try {
        if (objectOutputStream != null) {
          objectOutputStream.close();
        }
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }
  }

  /**
   * Provides a mock node runtime.
   */
  static class MockBasicNodeRuntime extends BasicNodeRuntime {

    MockBasicNodeRuntime() {
      super(
              "turing", // containerName
              NetworkUtils.TEXAI_MAINNET); // networkName
    }
  }

}
