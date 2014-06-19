/*
 * JWhichTest.java
 * JUnit based test
 *
 * Created on October 4, 2006, 8:21 AM
 *
 * Copyright (C) 2006 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.texai.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;


/**
 *
 * @author reed
 */
public class JWhichTest {

  public JWhichTest() {
  }

  /**
   * Test of formatClasspath method, of class org.texai.util.JWhich.
   */
  @Test
  public void testFormatClasspath() {
    System.out.println("formatClasspath");

    String classpath = "../../Utilities/dist/Utilities.jar";
    JWhich instance = new JWhich();

    String expResult = "\n    ../../Utilities/dist/Utilities.jar";
    String result = instance.formatClasspath(classpath);
    assertEquals(expResult, result);

    classpath = "../../Utilities/dist/Utilities.jar:../../KB/lib/OpenCyc.jar";
    expResult = "\n    ../../Utilities/dist/Utilities.jar\n    ../../KB/lib/OpenCyc.jar";
    result = instance.formatClasspath(classpath);
    assertEquals(expResult, result);

  }

  /**
   * Test of which method, of class org.texai.util.JWhich.
   */
  @Test
  public void testWhich() {
    System.out.println("which");

    String className = "org.texai.util.JWhich";
    JWhich instance = new JWhich();

    String result = instance.which(className);
    assertTrue(result.contains(" found in "));

  }
}
