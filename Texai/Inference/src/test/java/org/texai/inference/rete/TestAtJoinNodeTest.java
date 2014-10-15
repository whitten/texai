/*
 * TestAtJoinNodeTest.java
 *
 * Created on Jun 30, 2008, 10:31:43 PM
 *
 * Description: .
 *
 * Copyright (C) Aug 12, 2010 reed.
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
package org.texai.inference.rete;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.inference.rete.TestAtJoinNode.FieldType;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class TestAtJoinNodeTest {

  /** the logger */
  private static Logger LOGGER = Logger.getLogger(TestAtJoinNodeTest.class);

  public TestAtJoinNodeTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
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
   * Test of getArg1FieldType method, of class TestAtJoinNode.
   */
  @Test
  public void testGetArg1FieldType() {
    LOGGER.info("getArg1FieldType");
    TestAtJoinNode instance = new TestAtJoinNode("?Var", FieldType.SUBJECT, 3, FieldType.OBJECT);
    assertEquals(FieldType.SUBJECT, instance.getArg1FieldType());
  }

  /**
   * Test of getNbrLevelsUp method, of class TestAtJoinNode.
   */
  @Test
  public void testGetConditionNumberOfArg2() {
    LOGGER.info("getConditionNumberOfArg2");
    TestAtJoinNode instance = new TestAtJoinNode("?Var", FieldType.SUBJECT, 3, FieldType.OBJECT);
    assertEquals(3, instance.getNbrLevelsUp());
  }

  /**
   * Test of getArg2FieldType method, of class TestAtJoinNode.
   */
  @Test
  public void testGetArg2FieldType() {
    LOGGER.info("getArg2FieldType");
    TestAtJoinNode instance = new TestAtJoinNode("?Var", FieldType.SUBJECT, 3, FieldType.OBJECT);
    assertEquals(FieldType.OBJECT, instance.getArg2FieldType());
  }

  /**
   * Test of toString method, of class TestAtJoinNode.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    TestAtJoinNode instance = new TestAtJoinNode("?Var", FieldType.SUBJECT, 3, FieldType.OBJECT);
    assertEquals("[Test ?Var arg1: subject, arg2 levels up: 3, arg2: object]", instance.toString());
  }
}
