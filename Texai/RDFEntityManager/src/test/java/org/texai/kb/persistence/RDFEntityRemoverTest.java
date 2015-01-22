/*
 * RDFEntityRemoverTest.java
 *
 * Created on August 16, 2007, 10:14 AM
 *
 * Description: .
 *
 * Copyright (C) August 16, 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;

/**
 *
 * @author reed
 */
public class RDFEntityRemoverTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityRemoverTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;
  /** the Sesame repository connection */
  static RepositoryConnection repositoryConnection = null;
  /** the RDF entity manager */
  static RDFEntityManager rdfEntityManager;

  public RDFEntityRemoverTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("oneTimeSetup");
    CacheInitializer.initializeCaches();
    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    try {
      RDFEntityRemoverTest.class.getClassLoader().setDefaultAssertionStatus(true);
      CacheInitializer.resetCaches();
      CacheInitializer.initializeCaches();
      rdfEntityManager = new RDFEntityManager();
      repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(TEST_REPOSITORY_NAME);
      repositoryConnection.clear();
    } catch (RepositoryException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
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

  /**
   * Test of persist method, of class org.texai.kb.persistence.RDFEntityPersister.
   */
  @Test
  public void testRemove() {
    LOGGER.info("remove");

    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity1.setDontCareField("do not care");
    rdfTestEntity1.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity1.setMaxNbrOfScooterRiders(2);
    List<RDFTestEntity> myPeers = new ArrayList<>(1);
    myPeers.add(rdfTestEntity2);
    rdfTestEntity1.setMyPeers(myPeers);
    rdfTestEntity1.setName("TestDomainEntity 1");
    rdfTestEntity1.setNumberOfCrew(1);
    final String[] comments1 = {"comment 1", "comment 2"};
    rdfTestEntity1.setComment(comments1);
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    Set<String> cyclistNotes = new HashSet<>();
    cyclistNotes.add("note 1");
    cyclistNotes.add("note 2");
    rdfTestEntity2.setDontCareField("do not care");
    rdfTestEntity2.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity2.setMaxNbrOfScooterRiders(2);
    myPeers = new ArrayList<>(1);
    myPeers.add(rdfTestEntity1);
    rdfTestEntity2.setMyPeers(myPeers);
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    List<Double> myPeersStrengths = new ArrayList<>();
    myPeersStrengths.add(0.5d);
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
}
