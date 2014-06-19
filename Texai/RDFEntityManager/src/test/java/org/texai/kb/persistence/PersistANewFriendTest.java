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
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.repository.Repository;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.sample.PersistANewFriend;

/**
 *
 * @author reed
 */
public class PersistANewFriendTest {

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
   */
  public PersistANewFriendTest() {
  }
  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  @Test
  public void test() {
    LOGGER.info("test");
    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
    final PersistANewFriend persistANewFriend = new PersistANewFriend();
    persistANewFriend.initialize();
    persistANewFriend.createAndPersistAFriend();
    persistANewFriend.finalization();
    LOGGER.info("  test OK");
  }
}
