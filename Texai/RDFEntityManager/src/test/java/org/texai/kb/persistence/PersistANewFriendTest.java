/*
 * PersistANewFriendTest.java
 *
 * Created on August 17, 2007, 6:38 PM
 *
 * Description: Tests the sample class PersistANewFriend.
 *
 * Copyright (C) August 17, 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.File;
import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.After;
import org.junit.AfterClass;
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
  private static final String TEST_REPOSITORY_NAME = "Test";
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
