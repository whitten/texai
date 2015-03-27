/*
 * NodesInitializerTest.java
 *
 * Created on Jun 30, 2008, 8:16:37 AM
 *
 * Description: .
 *
 * Copyright (C) May 10, 2010 Stephen L. Reed
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
import java.io.File;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
public class NodesInitializerTest {

  /**
   * the log4j logger
   */
  private static final Logger LOGGER = Logger.getLogger(NodesInitializerTest.class);
  /**
   * the RDF entity manager
   */
  private static RDFEntityManager rdfEntityManager;
  // the test keystore path
  private final static String KEY_STORE_FILE_NAME = "data/test-keystore.uber";
  // the test configuration certificate path
  private final static String CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH = "data/test-ContainerSingletonConfiguration.crt";
  // the hash of nodes-test.xml
  public final static String NODES_TEST_HASH
          = "g6hHzzi4kzmQvmUI+eV0PqKCRAOjz+pBvVR90HrdUczvhulJmpVkrsgk2k6bBAS8i/jQ3pY7RL1+olin/QmWEg==";

  public NodesInitializerTest() {
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
    //TODO figure out maven assembly error when skipping tests then enable this longer test below
//    LOGGER.info("deleting " + KEY_STORE_FILE_NAME);
//    (new File(KEY_STORE_FILE_NAME)).delete();
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
   * Test of class NodeTypeInitializer.
   */
  @Test
  public void testVerifyNodesFileHash() {
    LOGGER.info("verifyNodesFileHash");
    final String containerName = "TestContainer";
    final String networkName = NetworkUtils.TEXAI_TESTNET;
    final BasicNodeRuntime nodeRuntime = new BasicNodeRuntime(containerName,  networkName);
    final char[] keystorePassword = "test-password".toCharArray();
    final NodesInitializer nodesInitializer
            = new NodesInitializer(
                    false, // isClassExistsTested
                    keystorePassword,
                    nodeRuntime,
                    KEY_STORE_FILE_NAME, // keyStoreFilePath
                    CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH); // containerConfigurationCertificateFilePath
    nodesInitializer.process(
            "data/nodes-test.xml", // nodesPath
            NODES_TEST_HASH); // nodesFileHashString
    try {
      nodesInitializer.process(
              "data/nodes-test.xml", // nodesPath
              "1234"); // nodesFileHashString
      fail();
    } catch (TexaiException ex) {
      // expected exception due to wrong checksum hash string
    }
    nodesInitializer.finalization();
  }

  /**
   * Test of class NodeTypeInitializer.
   */
  @Test
  public void testProcess() {
    LOGGER.info("beginning test");
    Logger.getLogger(NodesInitializer.class).setLevel(Level.DEBUG);
    final String containerName = "TestContainer";
    final String networkName = NetworkUtils.TEXAI_TESTNET;
    final BasicNodeRuntime nodeRuntime = new BasicNodeRuntime(containerName,  networkName);
   final char[] keystorePassword = "test-password".toCharArray();
    final File singletonConfigurationFile = new File(CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH);
    if (singletonConfigurationFile.exists()) {
      final boolean isOk = singletonConfigurationFile.delete();
      if (!isOk) {
        LOGGER.info("problem deleting the previous " + singletonConfigurationFile);
      }
    }
    final NodesInitializer nodesInitializer
            = new NodesInitializer(
                    false, // isClassExistsTested
                    keystorePassword,
                    nodeRuntime,
                    KEY_STORE_FILE_NAME, // keyStoreFilePath
                    CONTAINER_SINGLETON_CONFIGURATION_CERTIFICATE_PATH); // containerConfigurationCertificateFilePath
    nodesInitializer.process(
            "data/nodes-test.xml", // nodesPath
            NODES_TEST_HASH); // nodesFileHashString
    assertTrue(singletonConfigurationFile.exists());

    nodesInitializer.finalization();
  }

}
