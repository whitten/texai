/*
 * FileSystemUtilsTest.java
 *
 * Created on Jun 30, 2008, 10:04:55 AM
 *
 * Description: .
 *
 * Copyright (C) Nov 3, 2011 reed.
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

import java.io.IOException;
import org.apache.log4j.Logger;
import java.io.File;
import org.apache.commons.io.FileUtils;
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
public class FileSystemUtilsTest {

  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(FileSystemUtilsTest.class);

  public FileSystemUtilsTest() {
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
   * Test of deleteRecursively method, of class FileSystemUtils.
   */
  @Test
  public void testDeleteRecursively() {
    LOGGER.info("deleteRecursively");
    final File root = new File("data/test-directory-archive");
    FileSystemUtils.deleteRecursively(root);
    assertFalse(root.exists());
  }

  /**
   * Test of archiveDirectory method, of class FileSystemUtils.
   */
  @Test
  public void testArchiveDirectory() {
    LOGGER.info("archiveDirectory");
    final File sourceDirectory = new File("data/test-directory");
    final File destinationDirectory = new File("data/test-directory-archive");
    FileSystemUtils.archiveDirectory(sourceDirectory, destinationDirectory);
    assertTrue(destinationDirectory.exists());
    assertTrue((new File("data/test-directory-archive/file1.txt")).exists());
    assertTrue((new File("data/test-directory-archive/file2.txt")).exists());
    assertTrue((new File("data/test-directory-archive/file3.txt")).exists());
    try {
      Thread.sleep(1_000);
    } catch (InterruptedException ex) {
    }
    File sourceFile1 = new File("data/test-directory/file1.txt");
    final long sourceFile1PreviousLastModified = sourceFile1.lastModified();
    File destinationFile1 = new File("data/test-directory-archive/file1.txt");
    final long destinationFile1PreviousLastModified = destinationFile1.lastModified();
    assertEquals(sourceFile1PreviousLastModified, destinationFile1PreviousLastModified);
    try {
      FileUtils.touch(sourceFile1);
    } catch (IOException ex) {
      assert false;
    }
    sourceFile1 = new File("data/test-directory/file1.txt");
    final long sourceFile1CurrentLastModified = sourceFile1.lastModified();
    assertTrue(sourceFile1CurrentLastModified > sourceFile1PreviousLastModified);

    FileSystemUtils.archiveDirectory(sourceDirectory, destinationDirectory);
    destinationFile1 = new File("data/test-directory-archive/file1.txt");
    final long destinationFile1CurrentLastModified = destinationFile1.lastModified();
    assertEquals(sourceFile1CurrentLastModified, destinationFile1CurrentLastModified);
  }
}
