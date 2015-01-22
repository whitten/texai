/*
 * UpdateAFriendTest.java
 *
 * Created on August 17, 2007, 6:39 PM
 *
 * Description: Tests the sample class UpdateAFriend.
 *
 * Copyright (C) August 17, 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.File;
import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.sample.UpdateAFriend;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author reed
 */
public class UpdateAFriendTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(UpdateAFriendTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  /**
   * Creates a new instance of UpdateAFriendTest.
   */
  public UpdateAFriendTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
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

    final UpdateAFriend updateAFriend = new UpdateAFriend();
    updateAFriend.initialize();
    updateAFriend.createAndPersistAFriend();
    updateAFriend.updateFriend();
    updateAFriend.finalization();
    LOGGER.info("  test OK");
  }

}
