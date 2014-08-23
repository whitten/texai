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

import java.util.Iterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration;
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class NodeAccessTest2 {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(NodeAccessTest2.class);
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public NodeAccessTest2() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "Nodes",
            System.getenv("REPOSITORIES_TMPFS") + "/Nodes");
    DistributedRepositoryManager.clearNamedRepository("Nodes");
    rdfEntityManager = new RDFEntityManager();
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
   * Test of getNodeRuntimeConfiguration method, of class NodeAccess.
   */
  @Test
  public void testGetNodeRuntimeConfiguration() {
    LOGGER.info("getNodeRuntimeConfiguration");
    final NodeAccess instance = new NodeAccess(rdfEntityManager);
    // create and persist the NodeRuntimeConfiguration used by the node runtime
    final URI nodeRuntimeId = new URIImpl("http://texai.org/texai/org.texai.texaiLauncher.domainEntity.NodeRuntimeInfo_0a3990e1-1720-4b59-baf8-60b1ea6bec05");
    LOGGER.info("creating node runtime configuration for " + nodeRuntimeId);
    final NodeRuntimeConfiguration nodeRuntimeConfiguration = new NodeRuntimeConfiguration(nodeRuntimeId);
    nodeRuntimeConfiguration.setCertificateSigningKeyStoreEntryAlias("certificateSigning");
    nodeRuntimeConfiguration.setNodeRuntimeKeyStoreEntryAlias("nodeRuntime");

    // directly assemble the top friendship node
    LOGGER.info("assembling top friendship node");
    final NodeType topFriendshipNodeType = instance.findNodeType(AHCSConstants.TOP_FRIENDSHIP_NODE_TYPE);
    assert topFriendshipNodeType != null;
    LOGGER.info("  top friendship node type: " + topFriendshipNodeType);
    final Node topFriendshipNode = new Node(
            topFriendshipNodeType,
            new MockNodeRuntime()); // nodeRuntime
    LOGGER.info("  top friendship node: " + topFriendshipNode);
    topFriendshipNode.setNodeNickname(AHCSConstants.NODE_NICKNAME_TOPPER);
    topFriendshipNode.installRoles(instance);
    nodeRuntimeConfiguration.addNode(topFriendshipNode);
    final Role bootstrapRole = topFriendshipNode.getRoleForTypeName(AHCSConstants.BOOTSTRAP_ROLE_TYPE);
    assertNotNull(bootstrapRole);

    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.INFO);
    instance.persistNodeRuntimeConfiguration(nodeRuntimeConfiguration);
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);

    LOGGER.info("bootstrap node runtime configurations are ...");
    final Iterator<NodeRuntimeConfiguration> nodeRuntimeConfiguration_iter = rdfEntityManager.rdfEntityIterator(NodeRuntimeConfiguration.class);
    while (nodeRuntimeConfiguration_iter.hasNext()) {
      LOGGER.info("  " + nodeRuntimeConfiguration_iter.next());
    }
    // http://texai.org/texai/http://texai.org/texai/org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration_NodeRuntimeId
    // predicate:                    texai:org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration_NodeRuntimeId
    final NodeRuntimeConfiguration loadedNodeRuntimeConfiguration = instance.getNodeRuntimeConfiguration(nodeRuntimeId);
    assertNotNull(loadedNodeRuntimeConfiguration);
    assertEquals(nodeRuntimeConfiguration, loadedNodeRuntimeConfiguration);
    final Node loadedFriendshipNode = loadedNodeRuntimeConfiguration.getNode(AHCSConstants.NODE_NICKNAME_TOPPER);
    assertNotNull(loadedFriendshipNode);
    final Role loadedBootstrapRole = topFriendshipNode.getRoleForTypeName(AHCSConstants.BOOTSTRAP_ROLE_TYPE);
    assertNotNull(loadedBootstrapRole);
    LOGGER.info("bootstrap role: " + loadedBootstrapRole);

  }
}
