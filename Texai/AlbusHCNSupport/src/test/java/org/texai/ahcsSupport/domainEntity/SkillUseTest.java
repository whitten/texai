/*
 * SkillUseTest.java
 *
 * Created on Jun 30, 2008, 12:27:38 PM
 *
 * Description: .
 *
 * Copyright (C) Jun 29, 2010 reed.
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
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class SkillUseTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SkillUseTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public SkillUseTest() {
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
   * Test of getId method, of class SkillClass.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    assertTrue(instance.getId().toString().startsWith("http://texai.org/texai/org.texai.ahcsSupport.domainEntity.SkillClass_"));
  }

  /**
   * Test of getSkillClassName method, of class SkillClass.
   */
  @Test
  public void testGetSkillClassName() {
    LOGGER.info("getSkillClassName");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals("org.texai.skill.impl.HeartbeatImpl", instance.getSkillClassName());
  }

  /**
   * Test of getPackageName method, of class SkillClass.
   */
  @Test
  public void testPackageName() {
    LOGGER.info("getPackageName");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals("org.texai.skill.impl", instance.getPackageName());
  }

  /**
   * Test of getName method, of class SkillClass.
   */
  @Test
  public void testName() {
    LOGGER.info("getName");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals("HeartbeatImpl", instance.getName());
  }

  /**
   * Test of toString method, of class SkillClass.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals("[org.texai.skill.impl.HeartbeatImpl]", instance.toString());
  }

  /**
   * Test of equals method, of class SkillClass.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    SkillClass instance1 = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertFalse(instance1.equals(new Object()));
    SkillClass instance2 = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertTrue(instance1.equals(instance2));
  }

  /**
   * Test of hashCode method, of class SkillClass.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals(651653131, instance.hashCode());
  }

  /**
   * Test of toXML method, of class SkillClass.
   */
  @Test
  public void testToXML() {
    LOGGER.info("toXML");
    int indent = 0;
    SkillClass instance = new SkillClass("org.texai.skill.impl.HeartbeatImpl");
    assertEquals("<skill-class>\n" +
            "  <skill-class-name>org.texai.skill.impl.HeartbeatImpl</skill-class-name>\n" +
            "</skill-class>\n", instance.toXML(indent));

  }
}
