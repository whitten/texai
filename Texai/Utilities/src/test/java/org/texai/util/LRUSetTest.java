/*
 * LRUSetTest.java
 *
 * Created on Jun 30, 2008, 10:33:57 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 20, 2010 reed.
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

import java.util.ArrayList;
import org.apache.log4j.Logger;
import java.util.Collection;
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
public class LRUSetTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(LRUSetTest.class);

  public LRUSetTest() {
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
   * Test of contains method, of class LRUSet.
   */
  @Test
  public void testContains() {
    LOGGER.info("contains");
    LRUSet<String> instance = new LRUSet<String>(2, 2);
    assertFalse(instance.contains("abc"));
    instance.add("abc");
    assertTrue(instance.contains("abc"));
    assertFalse(instance.contains("def"));
    instance.add("def");
    assertTrue(instance.contains("def"));
    assertFalse(instance.contains("hij"));
    instance.add("hij");
    assertTrue(instance.contains("hij"));
    assertFalse(instance.contains("abc"));
  }

  /**
   * Test of iterator method, of class LRUSet.
   */
  @Test
  public void testIterator() {
    LOGGER.info("iterator");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    instance.add("hij");
    Iterator<String> iter = instance.iterator();
    assertTrue(iter.hasNext());
    assertEquals("abc", iter.next());
    assertTrue(iter.hasNext());
    assertEquals("def", iter.next());
    assertTrue(iter.hasNext());
    assertEquals("hij", iter.next());
    assertFalse(iter.hasNext());
  }

  /**
   * Test of toArray method, of class LRUSet.
   */
  @Test
  public void testToArray_0args() {
    LOGGER.info("toArray");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    Object[] result = instance.toArray();
    assertEquals(2, result.length);
    assertEquals("abc", result[0]);
    assertEquals("def", result[1]);
  }

  /**
   * Test of toArray method, of class LRUSet.
   */
  @Test
  public void testToArray_GenericType() {
    LOGGER.info("toArray");
    String[] a = {null, null};
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    Object[] result = instance.toArray(a);
    assertEquals(2, result.length);
    assertEquals("abc", result[0]);
    assertEquals("def", result[1]);
  }

  /**
   * Test of add method, of class LRUSet.
   */
  @Test
  public void testAdd() {
    LOGGER.info("add");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    instance.add("abc");
    assertEquals("[abc, def]", instance.toString());
  }

  /**
   * Test of remove method, of class LRUSet.
   */
  @Test
  public void testRemove() {
    LOGGER.info("remove");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    assertEquals("[abc, def]", instance.toString());
    instance.remove("hij");
    assertEquals("[abc, def]", instance.toString());
    instance.remove("abc");
    assertEquals("[def]", instance.toString());
    instance.remove("def");
    assertEquals("[]", instance.toString());
  }

  /**
   * Test of containsAll method, of class LRUSet.
   */
  @Test
  public void testContainsAll() {
    LOGGER.info("containsAll");
    Collection<String> c = new ArrayList<String>();
    c.add("abc");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    assertTrue(instance.containsAll(c));
  }

  /**
   * Test of addAll method, of class LRUSet.
   */
  @Test
  public void testAddAll() {
    Collection<String> c = new ArrayList<String>();
    c.add("abc");
    c.add("def");
    c.add("hij");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    assertEquals("[]", instance.toString());
    instance.addAll(c);
    assertEquals("[abc, def, hij]", instance.toString());
  }

  /**
   * Test of retainAll method, of class LRUSet.
   */
  @Test
  public void testRetainAll() {
    LOGGER.info("retainAll");
    Collection<String> c = new ArrayList<String>();
    c.add("abc");
    c.add("def");
    c.add("hij");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("1");
    instance.add("2");
    instance.add("3");
    instance.add("4");
    instance.add("def");
    boolean result = instance.retainAll(c);
    assertEquals("[def]", instance.toString());
    assertTrue(result);
  }

  /**
   * Test of removeAll method, of class LRUSet.
   */
  @Test
  public void testRemoveAll() {
    LOGGER.info("removeAll");
    Collection<String> c = new ArrayList<String>();
    c.add("abc");
    c.add("def");
    c.add("hij");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("1");
    instance.add("2");
    instance.add("3");
    instance.add("4");
    instance.add("def");
    boolean result = instance.removeAll(c);
    assertEquals("[1, 2, 3, 4]", instance.toString());
    assertTrue(result);
  }

  /**
   * Test of size method, of class LRUSet.
   */
  @Test
  public void testSize() {
    LOGGER.info("size");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    instance.add("abc");
    instance.add("def");
    instance.add("abc");
    assertEquals(2, instance.size());
  }

  /**
   * Test of isEmpty method, of class LRUSet.
   */
  @Test
  public void testIsEmpty() {
    LOGGER.info("isEmpty");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    assertTrue(instance.isEmpty());
    instance.add("abc");
    instance.add("def");
    instance.add("abc");
    assertFalse(instance.isEmpty());
  }

  /**
   * Test of clear method, of class LRUSet.
   */
  @Test
  public void testClear() {
    LOGGER.info("clear");
    LRUSet<String> instance = new LRUSet<String>(2, 10);
    assertTrue(instance.isEmpty());
    instance.add("abc");
    instance.add("def");
    instance.add("abc");
    assertFalse(instance.isEmpty());
    instance.clear();
    assertTrue(instance.isEmpty());
  }
}
