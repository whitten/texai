/*
 * RoleTypeTest.java
 *
 * Created on Jun 30, 2008, 6:43:37 PM
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

import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class RoleTypeTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RoleTypeTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public RoleTypeTest() {
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
   * Test of getId method, of class RoleType.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    RoleType instance = new RoleType();
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    assertTrue(instance.getId().toString().startsWith("http://texai.org/texai/org.texai.ahcsSupport.domainEntity.RoleType_"));
    LOGGER.info("id: " + instance.getId());
  }

  /**
   * Test of getSkillUses, addSkillUse, removeSkillUse methods of class RoleType.
   */
  @Test
  public void testGetSkillUses() {
    LOGGER.info("getSkillUses");
    RoleType instance = new RoleType();
    assertEquals("[]", instance.getSkillUses().toString());
    instance.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    assertEquals("[[org.texai.skill.impl.HeartbeatImpl]]", instance.getSkillUses().toString());
    instance.removeSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    assertEquals("[]", instance.getSkillUses().toString());
    instance.addSkillUse(new SkillClass("org.texai.skill.impl.HeartbeatImpl"));
    assertEquals("[[org.texai.skill.impl.HeartbeatImpl]]", instance.getSkillUses().toString());
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertNotNull(loadedInstance.getSkillUses());
    loadedInstance.getSkillUses().size(); // instantiate the lazy set
    assertEquals("[[org.texai.skill.impl.HeartbeatImpl]]", loadedInstance.getSkillUses().toString());
  }

  /**
   * Test of getDescription & setDescription method, of class RoleType.
   */
  @Test
  public void testGetDescription() {
    LOGGER.info("getDescription");
    RoleType instance = new RoleType();
    assertNull(instance.getDescription());
    instance.setDescription("my description");
    assertNotNull(instance.getDescription());
    assertEquals("my description", instance.getDescription());
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("my description", loadedInstance.getDescription());
  }

  /**
   * Test of equals method, of class RoleType.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    Object obj = new Object();
    RoleType instance = new RoleType();
    assertFalse(instance.equals(obj));
    instance.setTypeName("MyRoleType");
    RoleType roleType = new RoleType();
    assertFalse(instance.equals(roleType));
    roleType.setTypeName("MyRoleType");
    assertTrue(instance.equals(roleType));
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertTrue(instance.equals(loadedInstance));
  }

  /**
   * Test of hashCode method, of class RoleType.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    RoleType instance = new RoleType();
    instance.setTypeName("MyRoleType");
    assertEquals(-2107229501, instance.hashCode());
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals(-2107229501, loadedInstance.hashCode());
  }

  /**
   * Test of getTypeName & setTypeName method, of class RoleType.
   */
  @Test
  public void testGetTypeName() {
    LOGGER.info("getTypeName");
    RoleType instance = new RoleType();
    assertNull(instance.getTypeName());
    instance.setTypeName("MyRoleType");
    assertNotNull(instance.getTypeName());
    assertEquals("MyRoleType", instance.getTypeName());
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("MyRoleType", loadedInstance.getTypeName());
  }

  /**
   * Test of getAlbusHCSGranularityLevel & setAlbusHCSGranularityLevel method, of class RoleType.
   */
  @Test
  public void testGetAlbusHCSGranularityLevel() {
    LOGGER.info("getAlbusHCSGranularityLevel");
    RoleType instance = new RoleType();
    assertNull(instance.getAlbusHCSGranularityLevel());
    instance.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    assertNotNull(instance.getAlbusHCSGranularityLevel());
    assertEquals("AlbusHCS1DayGranularityLevel", instance.getAlbusHCSGranularityLevel().getLocalName());
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
    assertEquals("AlbusHCS1DayGranularityLevel", loadedInstance.getAlbusHCSGranularityLevel().getLocalName());
  }

  /**
   * Test of toXML method, of class RoleType.
   */
  @Test
  public void testToXML_0args() {
    LOGGER.info("toXML");
    RoleType instance = new RoleType();
    instance.setTypeName("MyRoleType");
    instance.addSkillUse(new SkillClass("org.texai.skill.impl.Heartbeat"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    instance.setDescription("my description");
    instance.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    String result = instance.toXML();
    assertEquals(
            "<role-type>\n" +
            "  <name>MyRoleType</name>\n" +
            "  <description>my description</description>\n" +
            "  <skill-classes>\n" +
            "    <skill-class>\n" +
            "      <skill-class-name>org.texai.skill.impl.Heartbeat</skill-class-name>\n" +
            "    </skill-class>\n" +
            "  </skill-classes>\n" +
            "  <granularity-level>AlbusHCS1DayGranularityLevel</granularity-level>\n" +
            "</role-type>\n",
            result);
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
  }

  /**
   * Test of toXML method, of class RoleType.
   */
  @Test
  public void testToXML_int() {
    LOGGER.info("toXML");
    RoleType instance = new RoleType();
    instance.setTypeName("MyRoleType");
    instance.addSkillUse(new SkillClass("org.texai.skill.impl.Heartbeat"));
    final RoleType parentRoleType = new RoleType();
    parentRoleType.setTypeName("MyParentRoleType");
    final RoleType childRoleType = new RoleType();
    childRoleType.setTypeName("MyChildRoleType");
    instance.setDescription("my description");
    instance.setAlbusHCSGranularityLevel(AHCSConstants.ALBUS_HCS_1_DAY_GRANULARITY_LEVEL);
    String result = instance.toXML(4);
    assertEquals(
            "    <role-type>\n" +
            "      <name>MyRoleType</name>\n" +
            "      <description>my description</description>\n" +
            "      <skill-classes>\n" +
            "        <skill-class>\n" +
            "          <skill-class-name>org.texai.skill.impl.Heartbeat</skill-class-name>\n" +
            "        </skill-class>\n" +
            "      </skill-classes>\n" +
            "      <granularity-level>AlbusHCS1DayGranularityLevel</granularity-level>\n" +
            "    </role-type>\n", result);
    rdfEntityManager.persist(instance);
    final RoleType loadedInstance = rdfEntityManager.find(RoleType.class, instance.getId());
    assertNotNull(loadedInstance);
  }
}
