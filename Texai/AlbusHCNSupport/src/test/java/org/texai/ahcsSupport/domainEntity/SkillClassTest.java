/*
 * SkillClassTest.java
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
public class SkillClassTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(SkillClassTest.class);
  /**
   * the RDF entity manager
   */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public SkillClassTest() {
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
   * Test of getId method, of class SkillClass.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    SkillClass instance = makeTestSkillClass();
    assertNull(instance.getId());
    rdfEntityManager.persist(instance);
    assertNotNull(instance.getId());
    assertTrue(instance.getId().toString().startsWith("http://texai.org/texai/org.texai.ahcsSupport.domainEntity.SkillClass_"));
    SkillClass loadedSkillClass = rdfEntityManager.find(SkillClass.class, instance.getId());
    assertNotNull(loadedSkillClass);
    assertEquals(instance, loadedSkillClass);
  }

  /**
   * Test of getSkillClassName method, of class SkillClass.
   */
  @Test
  public void testGetSkillClassName() {
    LOGGER.info("getSkillClassName");
    SkillClass instance = makeTestSkillClass();
    assertEquals("org.texai.skill.governance.Governance", instance.getSkillClassName());
  }

  /**
   * Test of getPackageName method, of class SkillClass.
   */
  @Test
  public void testPackageName() {
    LOGGER.info("getPackageName");
    SkillClass instance = makeTestSkillClass();
    assertEquals("org.texai.skill.governance", instance.getPackageName());
  }

  /**
   * Test of getName method, of class SkillClass.
   */
  @Test
  public void testName() {
    LOGGER.info("getName");
    SkillClass instance = makeTestSkillClass();
    assertEquals("Governance", instance.getName());
  }

  /**
   * Test of toString method, of class SkillClass.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    SkillClass instance = makeTestSkillClass();
    assertEquals("[org.texai.skill.governance.Governance]", instance.toString());
  }

  /**
   * Test of equals method, of class SkillClass.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    SkillClass instance1 = makeTestSkillClass();
    assertFalse(instance1.equals(new Object()));
    SkillClass instance2 = makeTestSkillClass();
    assertTrue(instance1.equals(instance2));
  }

  /**
   * Test of hashCode method, of class SkillClass.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    SkillClass instance = makeTestSkillClass();
    assertEquals(-1694190849, instance.hashCode());
  }

  /**
   * Test of toXML method, of class SkillClass.
   */
  @Test
  public void testToXML() {
    LOGGER.info("toXML");
    int indent = 0;
    SkillClass instance = makeTestSkillClass();
    assertEquals(
            "<skill-class>\n" +
            "  <skill-class-name>org.texai.skill.governance.Governance</skill-class-name>\n" +
            "</skill-class>\n", instance.toXML(indent));

  }

  /**
   * Makes a test skill class.
   *
   * @return a test skill class
   */
  public static SkillClass makeTestSkillClass() {
    final String skillClassName = "org.texai.skill.governance.Governance";
    final boolean isClassExistsTested = false;
    return new SkillClass(
            skillClassName,
            isClassExistsTested);
  }

}
