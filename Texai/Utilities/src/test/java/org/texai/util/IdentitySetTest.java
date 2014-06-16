/*
 * IdentitySetTest.java
 *
 * Created on Jun 30, 2008, 3:49:46 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 4, 2011 reed.
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
import java.util.Iterator;
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
public class IdentitySetTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(IdentitySetTest.class);

  public IdentitySetTest() {
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
   * Test of size method, of class IdentitySet.
   */
  @Test
  public void testSize() {
    LOGGER.info("size");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    instance.add(1);
    instance.add(2);
    assertEquals(2, instance.size());
    instance.add(3);
    assertEquals(3, instance.size());
    final Integer element = new Integer(1);
    instance.add(element);
    assertEquals(4, instance.size());
    instance.add(element);
    assertEquals(4, instance.size());
  }

  /**
   * Test of isEmpty method, of class IdentitySet.
   */
  @Test
  public void testIsEmpty() {
    LOGGER.info("isEmpty");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    assertTrue(instance.isEmpty());
    instance.add(1);
    assertFalse(instance.isEmpty());
  }

  /**
   * Test of contains method, of class IdentitySet.
   */
  @Test
  public void testContains() {
    LOGGER.info("contains");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    final Integer element = new Integer(1);
    instance.add(element);
    assertTrue(instance.contains(element));
    instance.add(1);
    assertTrue(instance.contains(element));
    assertFalse(instance.contains(new Integer(1)));
    assertFalse(instance.contains(new Object()));
  }

  /**
   * Test of iterator method, of class IdentitySet.
   */
  @Test
  public void testIterator() {
    LOGGER.info("iterator");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    Iterator<Integer> iterator = instance.iterator();
    assertTrue(!iterator.hasNext());
    instance.add(1);
    iterator = instance.iterator();
    assertTrue(iterator.hasNext());
    assertEquals("1", iterator.next().toString());
  }

  /**
   * Test of add method, of class IdentitySet.
   */
  @Test
  public void testAdd() {
    LOGGER.info("add");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    instance.add(1);
    instance.add(1);
    assertEquals("[1]", instance.toString());
  }

  /**
   * Test of remove method, of class IdentitySet.
   */
  @Test
  public void testRemove() {
    LOGGER.info("remove");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    assertEquals(0, instance.size());
    Integer element = new Integer(1);
    instance.add(element);
    assertEquals(1, instance.size());
    boolean isRemoved = instance.remove(element);
    assertTrue(isRemoved);
    assertEquals(0, instance.size());
    instance.add(element);
    instance.add(element);
    instance.add(element);
    assertEquals(1, instance.size());
    instance.add(2);
    instance.add(3);
    assertEquals(3, instance.size());
    isRemoved = instance.remove(element);
    assertTrue(isRemoved);
    assertEquals(2, instance.size());
  }

  /**
   * Test of clear method, of class IdentitySet.
   */
  @Test
  public void testClear() {
    LOGGER.info("clear");
    IdentitySet<Integer> instance = new IdentitySet<Integer>();
    assertEquals(0, instance.size());
    Integer element = new Integer(1);
    instance.add(element);
    assertEquals(1, instance.size());
    instance.clear();
    assertEquals(0, instance.size());
  }
}
