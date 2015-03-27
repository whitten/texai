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
package org.texai.skill.support;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class NodeInfoTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeInfo.class);

  public NodeInfoTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getContainerName method, of class NodeInfo.
   */
  @Test
  public void testGetContainerName() {
    LOGGER.info("getContainerName");
    NodeInfo instance = new NodeInfo("test-container");
    assertEquals("test-container", instance.getContainerName());
  }

  /**
   * Test of getIpAddress method, of class NodeInfo.
   */
  @Test
  public void testGetIpAddress() {
    LOGGER.info("getIpAddress");
    NodeInfo instance = new NodeInfo("test-container");
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
    NodeInfo instance = new NodeInfo("test-container");
    assertTrue(!instance.isSuperPeer());
    instance.setIsSuperPeer(true);
    assertTrue(instance.isSuperPeer());
  }

  /**
   * Test of hashCode method, of class NodeInfo.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    NodeInfo instance = new NodeInfo("test-container");
    assertEquals(923362937, instance.hashCode());
  }

  /**
   * Test of equals method, of class NodeInfo.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    NodeInfo instance = new NodeInfo("test-container");
    assertFalse(instance.equals(new NodeInfo("test-container2")));
    assertTrue(instance.equals(new NodeInfo("test-container")));
  }

  /**
   * Test of toString method, of class NodeInfo.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    NodeInfo instance = new NodeInfo("test-container");
    assertEquals("[container test-container,  null]", instance.toString());
    instance.setIpAddress("127.0.0.1");
    assertEquals("[container test-container,  127.0.0.1]", instance.toString());
  }

}
