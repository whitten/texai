/*
 * RemoveAFriendTest.java
 *
 * Created on August 17, 2007, 6:39 PM
 *
 * Description: Tests the sample class RemoveAFriend.
 *
 * Copyright (C) August 17, 2007 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.sample.RemoveAFriend;

/**
 *
 * @author reed
 */
public class RemoveAFriendTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RemoveAFriendTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";

  /**
   * Creates a new instance of RemoveAFriendTest.
   */
  public RemoveAFriendTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
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

    final RemoveAFriend deleteAFriend = new RemoveAFriend();
    deleteAFriend.initialize();
    deleteAFriend.createAndPersistAFriend();
    deleteAFriend.removeFriend();
    deleteAFriend.finalization();
    LOGGER.info("  test OK");
  }

}
