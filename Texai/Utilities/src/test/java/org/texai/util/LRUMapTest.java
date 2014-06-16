/*
 * LRUMapTest.java
 *
 * Created on Jun 30, 2008, 10:33:12 PM
 *
 * Description: .
 *
 * Copyright (C) Oct 28, 2010 reed.
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
package org.texai.util;

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
public class LRUMapTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LRUMapTest.class);


  public LRUMapTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of removeEldestEntry method, of class LRUMap.
   */
  @Test
  public void testRemoveEldestEntry() {
    LOGGER.info("removeEldestEntry");
    final int initialCapacity = 3;
    final int maximumCapacity = 3;
    LRUMap<String, String> instance = new LRUMap<String, String>(initialCapacity, maximumCapacity);
    assertEquals(0, instance.size());

    instance.put("a", "1");
    assertEquals(1, instance.size());
    assertEquals("1", instance.get("a"));

    instance.put("b", "2");
    assertEquals(2, instance.size());
    assertEquals("2", instance.get("b"));

    instance.put("a", "1");
    assertEquals(2, instance.size());
    assertEquals("1", instance.get("a"));

    instance.put("b", "9");
    assertEquals(2, instance.size());
    assertEquals("9", instance.get("b"));

    instance.put("c", "3");
    assertEquals(3, instance.size());
    assertEquals("3", instance.get("c"));

    instance.put("d", "4");
    assertEquals(3, instance.size());
    assertEquals("4", instance.get("d"));
    assertNull(instance.get("a"));
  }
}
