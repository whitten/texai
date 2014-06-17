/*
 * PersistANewFriendTest.java
 *
 * Created on August 17, 2007, 6:38 PM
 *
 * Description: Tests the sample class PersistANewFriend.
 *
 * Copyright (C) August 17, 2007 Stephen L. Reed.
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
package org.texai.kb.persistence;

import java.io.File;
import java.io.IOException;
import javax.xml.bind.DatatypeConverter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.openrdf.repository.Repository;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.sample.PersistANewFriend;

/**
 *
 * @author reed
 */
public class PersistANewFriendTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(PersistANewFriendTest.class);
  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;
  /** the Sesame repository */
  static Repository repository = null;

  /**
   * Creates a new instance of PersistANewFriendTest.
   * @param testName 
   */
  public PersistANewFriendTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new PersistANewFriendTest("test"));
    suite.addTest(new PersistANewFriendTest("testOneTimeTearDown"));
    return suite;
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public void test() {
    System.out.println("test");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();

    String testRepositoryPath = System.getenv("REPOSITORIES_TMPFS");
    if (testRepositoryPath == null || testRepositoryPath.isEmpty()) {
      testRepositoryPath = System.getProperty("user.dir") + "/repositories";
    } else if (testRepositoryPath.endsWith("/")) {
      testRepositoryPath = testRepositoryPath.substring(0, testRepositoryPath.length() - 1);
    }
    assertFalse(testRepositoryPath.isEmpty());

    testRepositoryDirectory = new File(testRepositoryPath);
    try {
      if (testRepositoryDirectory.exists()) {
        FileUtils.cleanDirectory(testRepositoryDirectory);
      } else {
        FileUtils.deleteDirectory(testRepositoryDirectory);
      }
    } catch (final IOException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(testRepositoryDirectory);
    DistributedRepositoryManager.addRepositoryPath(
            TEST_REPOSITORY_NAME,
            testRepositoryPath + "/" + TEST_REPOSITORY_NAME);

    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
    final PersistANewFriend persistANewFriend = new PersistANewFriend();
    persistANewFriend.initialize();
    persistANewFriend.createAndPersistAFriend();
    persistANewFriend.finalization();
    System.out.println("  test OK");
  }

  /** Performs one time tear down of test harness. This must be the last test method. */
  public void testOneTimeTearDown() {
    System.out.println("oneTimeTearDown");
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
  }
}
