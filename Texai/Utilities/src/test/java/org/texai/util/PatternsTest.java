/*
 * PatternsTest.java
 *
 * Created on Jun 30, 2008, 1:40:46 PM
 *
 * Description: .
 *
 * Copyright (C) Nov 2, 2011 reed.
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
import java.util.List;
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
public class PatternsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PatternsTest.class);

  public PatternsTest() {
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
   * Test of addPattern method, of class Patterns.
   */
  @Test
  public void testAddPattern() {
    LOGGER.info("addPattern");
    String operation = "log-operation";
    String patternString = "log operation \"([\\w]*)\"";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
  }

  /**
   * Test of matches method, of class Patterns.
   */
  @Test
  public void testMatches() {
    LOGGER.info("matches");
    String operation = "log-operation";
    String patternString = "log operation \"([\\w]*)\"";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
    assertTrue(instance.matches("log operation \"test\""));
  }

  /**
   * Test of getMatchedOperation method, of class Patterns.
   */
  @Test
  public void testGetMatchedOperation() {
    String operation = "log-operation";
    String patternString = "log operation \"([\\w]*)\"";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
    assertTrue(instance.matches("log operation \"test\""));
    String result = instance.getMatchedOperation();
    assertEquals("log-operation", result);
  }

  /**
   * Test of getMatchedGroups method, of class Patterns.
   */
  @Test
  public void testGetMatchedGroups() {
    String operation = "log-operation";
    String patternString = "log operation \"([\\w]*)\"";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
    assertTrue(instance.matches("log operation \"test\""));
    List<String> result = instance.getMatchedGroups();
    assertEquals("[test]", result.toString());
  }

  /**
   * Test of getMatchedGroups method, of class Patterns.
   */
  @Test
  public void testGetMatchedGroups2() {
    String operation = "log-operation";
    String patternString = "log operation \"([\\w|\\s]*)\"";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
    assertTrue(instance.matches("log operation \"create node_Task\""));
    List<String> result = instance.getMatchedGroups();
    assertEquals("[create node_Task]", result.toString());
  }

  /**
   * Test of getMatchedGroups method, of class Patterns.
   */
  @Test
  public void testGetMatchedGroups3() {
    // remove the role type VNCOperationManagement
    String operation = "remove role type";
    String patternString = "remove role type ([\\w|\\s]*)";
    Patterns instance = new Patterns();
    instance.addPattern(operation, patternString);
    assertTrue(instance.matches("remove role type VNCOperationManagement"));
    List<String> result = instance.getMatchedGroups();
    assertEquals("[VNCOperationManagement]", result.toString());
  }
}
