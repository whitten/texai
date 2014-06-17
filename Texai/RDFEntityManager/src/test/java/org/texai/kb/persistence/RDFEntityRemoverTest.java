/*
 * RDFEntityRemoverTest.java
 *
 * Created on August 16, 2007, 10:14 AM
 *
 * Description: .
 *
 * Copyright (C) August 16, 2007 Stephen L. Reed.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.CacheInitializer;

/**
 *
 * @author reed
 */
public class RDFEntityRemoverTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityRemoverTest.class);
  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;
  /** the Sesame repository connection */
  static RepositoryConnection repositoryConnection = null;
  /** the RDF entity manager */
  static RDFEntityManager rdfEntityManager;

  public RDFEntityRemoverTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new RDFEntityRemoverTest("testRemove"));
    suite.addTest(new RDFEntityRemoverTest("testOneTimeTearDown"));
    return suite;
  }

  /** Sets up the unit test.
   * 
   * @throws java.lang.Exception
   */
  @Override
  protected void setUp() throws Exception {
    if (rdfEntityManager == null) {
      LOGGER.info("oneTimeSetup");
      DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());

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

      try {
        getClass().getClassLoader().setDefaultAssertionStatus(true);
        CacheInitializer.resetCaches();
        CacheInitializer.initializeCaches();
        rdfEntityManager = new RDFEntityManager();
        repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(TEST_REPOSITORY_NAME);
        repositoryConnection.clear();
      } catch (Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  }

  /** Tears down the unit test.
   * 
   * @throws java.lang.Exception
   */
  @Override
  protected void tearDown() throws Exception {
  }

  /**
   * Test of persist method, of class org.texai.kb.persistence.RDFEntityPersister.
   */
  public void testRemove() {
    LOGGER.info("remove");

    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity1.setDontCareField("do not care");
    rdfTestEntity1.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity1.setMaxNbrOfScooterRiders(2);
    List<RDFTestEntity> myPeers = new ArrayList<RDFTestEntity>(1);
    myPeers.add(rdfTestEntity2);
    rdfTestEntity1.setMyPeers(myPeers);
    rdfTestEntity1.setName("TestDomainEntity 1");
    rdfTestEntity1.setNumberOfCrew(1);
    final String[] comments1 = {"comment 1", "comment 2"};
    rdfTestEntity1.setComment(comments1);
    Set<String> cyclistNotes = new HashSet<String>();
    cyclistNotes.add("note 1");
    cyclistNotes.add("note 2");
    rdfTestEntity2.setDontCareField("do not care");
    rdfTestEntity2.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity2.setMaxNbrOfScooterRiders(2);
    myPeers = new ArrayList<RDFTestEntity>(1);
    myPeers.add(rdfTestEntity1);
    rdfTestEntity2.setMyPeers(myPeers);
    List<Double> myPeersStrengths = new ArrayList<Double>();
    myPeersStrengths.add(Double.valueOf(0.5d));
    rdfTestEntity2.setName("TestDomainEntity 2");
    rdfTestEntity2.setNumberOfCrew(1);
    final String[] comments2 = {"comment 1", "comment 2"};
    rdfTestEntity2.setComment(comments2);

    RDFEntityPersister rdfEntityPersister = new RDFEntityPersister(rdfEntityManager);
    rdfEntityPersister.persist(repositoryConnection, rdfTestEntity1);
    assertNotNull(rdfTestEntity1.getId());

    RDFEntityLoader rdfEntityLoader = new RDFEntityLoader();
    URI uri1 = rdfTestEntity1.getId();
    Object result = rdfEntityLoader.find(
            repositoryConnection,
            RDFTestEntity.class, uri1);
    assertNotNull(result);

    RDFEntityRemover instance = new RDFEntityRemover(rdfEntityManager);
    instance.remove(repositoryConnection, rdfTestEntity1);
    assertNotNull(rdfTestEntity1.getId());

    result = rdfEntityLoader.find(
            repositoryConnection,
            RDFTestEntity.class, uri1);
    assertNull(result);

    LOGGER.info("  remove OK");
  }

  /** Performs one time tear down of test harness. This must be the last test method. */
  public void testOneTimeTearDown() {
    LOGGER.info("oneTimeTearDown");
    CacheManager.getInstance().shutdown();
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }
}
