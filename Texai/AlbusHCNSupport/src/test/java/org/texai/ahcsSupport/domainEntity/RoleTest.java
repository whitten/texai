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

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;
import net.sf.ehcache.CacheManager;
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
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.kb.Constants;
import org.texai.kb.util.UUIDUtils;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class RoleTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RoleTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public RoleTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "NodeRoleTypes",
            true); // isRepositoryDirectoryCleaned
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
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(

            roleType,
            nodeRuntime);
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
  }

  /**
   * Test of getRoleType method, of class Role.
   */
  @Test
  public void testGetRoleType() {
    LOGGER.info("getRoleType");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(
            roleType,
            nodeRuntime);
    final NodeAccess nodeAccess = new NodeAccess(rdfEntityManager);
    assertEquals("[RoleType MyRoleType]", nodeAccess.getRoleType(instance).toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[RoleType MyRoleType]", nodeAccess.getRoleType(loadedInstance).toString());
  }

  /**
   * Test of getNode & setNode method, of class Role.
   */
  @Test
  public void testGetNode() {
    LOGGER.info("getNode");
    NodeType nodeType = new NodeType();
    nodeType.setTypeName("MyTypeName");
    NodeType inheritedNodeType = new NodeType();
    inheritedNodeType.setTypeName("MyInheritedNodeType");
    nodeType.addInheritedNodeType(inheritedNodeType);
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    nodeType.addRoleType(roleType);
    nodeType.setMissionDescription("my mission description");
    rdfEntityManager.persist(nodeType);
    Node node = new Node(
            nodeType,
            null);
    node.setNodeNickname("MyNodeNickname");
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(
            roleType,
            nodeRuntime);
    assertNull(instance.getNode());
    instance.setNode(node);
    assertEquals("[MyNodeNickname: MyTypeName]", instance.getNode().toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[MyNodeNickname: MyTypeName]", loadedInstance.getNode().toString());
  }

  /**
   * Test of getRoleState & setRoleState methods, of class Role.
   */
  @Test
  public void testGetRoleState() {
    LOGGER.info("getRoleState");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(

            roleType,
            nodeRuntime);
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
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    final URI nodeRuntimeRoleId = new URIImpl(Constants.TEXAI_NAMESPACE + "NodeRuntime_001");
    rdfEntityManager.persist(roleType);
    Role instance = new Role(
            roleType,
            nodeRuntime);
    assertEquals("[]", instance.getSkills().toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[]", loadedInstance.getSkills().toString());
  }

  /**
   * Test of getParentRoleIdString & setParentRoleIdString method, of class Role.
   */
  @Test
  public void testGetParentRoleIdString() {
    LOGGER.info("getParentRoleIdString");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(

            roleType,
            nodeRuntime);
    assertNull(instance.getParentRoleIdString());
    instance.setParentRoleIdString((new URIImpl(Constants.TEXAI_NAMESPACE + "MyParentRole")).toString());
    assertEquals("http://texai.org/texai/MyParentRole", instance.getParentRoleIdString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("http://texai.org/texai/MyParentRole", loadedInstance.getParentRoleIdString());
  }

  /**
   * Test of getChildRoleIdStrings method, of class Role.
   */
  @Test
  public void testGetChildRoleIdStrings() {
    LOGGER.info("getChildRoleIdStrings");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(
            roleType,
            nodeRuntime);
    assertEquals("[]", instance.getChildRoleIdStrings().toString());
  }

  /**
   * Test of setX509SecurityInfo & getX509Certificate method, of class Role.
   */
  @Test
  public void testSetX509SecurityInfo() {
    LOGGER.info("setX509SecurityInfo");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(

            roleType,
            nodeRuntime);
    assertNull(instance.getX509Certificate());
    rdfEntityManager.persist(instance);
    if (!X509Utils.isTrustedDevelopmentSystem()) {
      return;
    }
    X509SecurityInfo x509SecurityInfo = null;
    try {
      X509Certificate rootX509Certificate = X509Utils.getRootX509Certificate();
      KeyPair keyPair = X509Utils.generateRSAKeyPair2048();
      PrivateKey certificateAuthorityPrivateKey = X509Utils.getRootPrivateKey();
      final UUID uid = UUIDUtils.uriToUUID(instance.getId());
      final char[] keystorePassword = "my-password".toCharArray();
      x509SecurityInfo = X509Utils.generateX509SecurityInfo(
          keyPair,
          certificateAuthorityPrivateKey,
          rootX509Certificate,
          uid,
          keystorePassword,
          false, null); // isJCEUnlimitedStrengthPolicy

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(x509SecurityInfo);
    instance.setX509SecurityInfo(x509SecurityInfo);
    assertNotNull(instance.getX509Certificate());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertNull(loadedInstance.getX509Certificate());
  }

  /**
   * Test of toString method, of class Role.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    RoleType roleType = new RoleType();
    roleType.setTypeName("MyRoleType");
    roleType.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    roleType.setDescription("my description");
    roleType.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    final NodeRuntime nodeRuntime = null;
    rdfEntityManager.persist(roleType);
    Role instance = new Role(

            roleType,
            nodeRuntime);
    assertEquals("[MyRoleType]", instance.toString());
    rdfEntityManager.persist(instance);
    Role loadedInstance = rdfEntityManager.find(Role.class, instance.getId());
    assertEquals("[MyRoleType]", loadedInstance.toString());
  }
}
