/*
 * RDFEntityManagerTest.java
 *
 * Created on August 16, 2007, 11:12 AM
 *
 * Description: .
 *
 * Copyright (C) August 16, 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;

/**
 *
 * @author reed
 */
public class RDFEntityManagerTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityManagerTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";

  /**
   * Creates a new instance of RDFEntityManagerTest.
   */
  public RDFEntityManagerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("oneTimeSetup");
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    try {
      RDFEntityManagerTest.class.getClassLoader().setDefaultAssertionStatus(true);
      CacheInitializer.initializeCaches();
    } catch (Exception e) {
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

  /** Tests the get methods. */
  @Test
  public void testGetters() {
    LOGGER.info("testGetters");
    RDFEntityManager rdfEntityManager = new RDFEntityManager();
    assertNotNull(rdfEntityManager.getTypeQueries());
    LOGGER.info("  testGetters OK");
    rdfEntityManager.close();
  }

  /**
   * Test of getDefaultContext(final Class rdfEntityClass) method, of class org.texai.kb.persistence.RDFEntityManager.
   */
  @Test
  public void testGetDefaultContext() {
    LOGGER.info("getDefaultContext");
    RDFEntityManager rdfEntityManager = new RDFEntityManager();
    assertEquals("texai:TestContext", RDFUtility.formatURIAsTurtle(rdfEntityManager.getDefaultContext(RDFTestEntity.class)));
    rdfEntityManager.close();
  }

  /**
   * Test of find(final Class clazz, final URI instanceURI) method, of class org.texai.kb.persistence.RDFEntityManager.
   */
  @Test
  public void testFind() {
    LOGGER.info("find");

    // persist two RDF entities, the default context is texai:TestContext
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
    Set<String> cyclistNotes = new HashSet<>();
    cyclistNotes.add("note 1");
    cyclistNotes.add("note 2");
    rdfTestEntity1.setCyclistNotes(cyclistNotes);
    rdfTestEntity2.setDontCareField("do not care");
    rdfTestEntity2.setFavoriteTestRDFEntityPeer(rdfTestEntity1);
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

    // set XML datatype fields in the first test RDF entity
    rdfTestEntity1.setByteField((byte) 5);
    rdfTestEntity1.setIntField(6);
    rdfTestEntity1.setLongField(7L);
    rdfTestEntity1.setFloatField(1.1F);
    rdfTestEntity1.setDoubleField(1.2D);
    rdfTestEntity1.setBigIntegerField(new BigInteger("100"));
    rdfTestEntity1.setBigDecimalField(new BigDecimal("100.001"));
    rdfTestEntity1.setCalendarField(Calendar.getInstance());
    rdfTestEntity1.setDateField(Calendar.getInstance().getTime());

    RDFEntityManager rdfEntityManager = new RDFEntityManager();
    // Explicitly persist RDF test entity 1, which automatically cascades to persist RDF test entitity 2 because the latter lacks its id.
    // RDF test entity 1 is persisted with an override context into UniversalVocabularyMt.   RDF test entity 2 is persisted into its
    // default context texai:TestContext.
    rdfEntityManager.persist(
            rdfTestEntity1,
            new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT));
    assertNotNull(rdfTestEntity1.getId());
    assertNotNull(rdfTestEntity2.getId());
    System.out.println("RDFTest1Entity1: " + rdfTestEntity1.getId());
    System.out.println("RDFTest1Entity2: " + rdfTestEntity2.getId());

    // load a persisted entity
    URI uri1 = rdfTestEntity1.getId();
    Object result = rdfEntityManager.find(
            RDFTestEntity.class,
            uri1);
    assertTrue(result instanceof RDFTestEntity);
    RDFTestEntity rdfTestEntity1_Loaded = (RDFTestEntity) result;
    assertEquals(rdfTestEntity1.getId(), rdfTestEntity1_Loaded.getId());
    assertEquals(2, rdfTestEntity1_Loaded.getCyclistNotes().size());
    assertTrue(rdfTestEntity1_Loaded.getCyclistNotes().contains("note 1"));
    assertTrue(rdfTestEntity1_Loaded.getCyclistNotes().contains("note 2"));
    assertEquals(1, rdfTestEntity1_Loaded.getPeersHavingMeAsAFavorite().size());
    assertTrue(rdfTestEntity1_Loaded.getPeersHavingMeAsAFavorite().toArray()[0] instanceof RDFTestEntity);
    assertEquals("TestDomainEntity 2", ((RDFTestEntity) rdfTestEntity1_Loaded.getPeersHavingMeAsAFavorite().toArray()[0]).getName());
    assertEquals(rdfTestEntity1.getName(), rdfTestEntity1_Loaded.getName());
    assertEquals(rdfTestEntity1.getNumberOfCrew(), rdfTestEntity1_Loaded.getNumberOfCrew());
    assertEquals(rdfTestEntity1.getMaxNbrOfScooterRiders(), rdfTestEntity1_Loaded.getMaxNbrOfScooterRiders());
    assertEquals(rdfTestEntity1.getMyPeers().size(), rdfTestEntity1_Loaded.getMyPeers().size());
    assertEquals(rdfTestEntity1.getMyPeers(), rdfTestEntity1_Loaded.getMyPeers());
    assertNull(rdfTestEntity1_Loaded.getDontCareField());
    assertEquals(rdfTestEntity1.getFavoriteTestRDFEntityPeer(), rdfTestEntity1_Loaded.getFavoriteTestRDFEntityPeer());
    assertNotNull(rdfTestEntity1_Loaded.getComment());
    assertEquals(rdfTestEntity1.getComment().length, rdfTestEntity1_Loaded.getComment().length);
    for (int i = 0; i < rdfTestEntity1.getComment().length; i++) {
      assertEquals(rdfTestEntity1.getComment()[i], rdfTestEntity1_Loaded.getComment()[i]);
    }
    // test that  XML datatype fields loaded OK
    assertEquals(rdfTestEntity1.getByteField(), rdfTestEntity1_Loaded.getByteField());
    assertEquals(rdfTestEntity1.getIntField(), rdfTestEntity1_Loaded.getIntField());
    assertEquals(rdfTestEntity1.getLongField(), rdfTestEntity1_Loaded.getLongField());
    assertTrue(rdfTestEntity1.getFloatField() == rdfTestEntity1_Loaded.getFloatField());
    assertEquals(rdfTestEntity1.getBigIntegerField(), rdfTestEntity1_Loaded.getBigIntegerField());
    assertEquals(rdfTestEntity1.getBigDecimalField(), rdfTestEntity1_Loaded.getBigDecimalField());
    assertEquals(rdfTestEntity1.getDateField(), rdfTestEntity1_Loaded.getDateField());
    assertEquals(rdfTestEntity1.getCalendarField().getTime(), rdfTestEntity1_Loaded.getCalendarField().getTime());
    assertTrue(Math.abs(rdfTestEntity1.getDoubleField() - rdfTestEntity1_Loaded.getDoubleField()) < 0.0000001);
    rdfEntityManager.close();

    LOGGER.info("find by given property and value");
    RDFEntityManager instance = new RDFEntityManager();
    URI predicate = new URIImpl("http://sw.cyc.com/2006/07/27/cyc/prettyString-Canonical");
    Value value = new LiteralImpl("TestDomainEntity 1");
    List<RDFTestEntity> resultList = instance.find(
            predicate,
            value,
            RDFTestEntity.class);
    assertNotNull(resultList);
    assertEquals(0, resultList.size());
    resultList = instance.find(
            predicate,
            value,
            new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT),
            RDFTestEntity.class);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    result = resultList.get(0);
    assertEquals("TestDomainEntity 1", ((RDFTestEntity) result).getName());
    instance.close();
    LOGGER.info("  find by given property and value OK");

    LOGGER.info("rdfEntityIterator");

    // load a persisted entity
    instance = new RDFEntityManager();
    Iterator<RDFTestEntity> iterator = instance.rdfEntityIterator(
            RDFTestEntity.class, null);
    assertNotNull(iterator);
    int count = 0;
    while (iterator.hasNext()) {
      final Object obj = iterator.next();
      assertTrue(obj instanceof RDFTestEntity);
      RDFTestEntity rdfTestEntity = (RDFTestEntity) obj;
      System.out.println("  iteration: " + ++count + " RDF entity: " + rdfTestEntity.getName());
    }
    assertEquals(2, count);
    instance.close();
    LOGGER.info("  rdfEntityIterator OK");

    LOGGER.info("remove");

    instance = new RDFEntityManager();
    iterator = instance.rdfEntityIterator(
            RDFTestEntity.class, null);
    assertNotNull(iterator);
    assert iterator.hasNext();
    final Object obj = iterator.next();
    assertTrue(obj instanceof RDFTestEntity);
    RDFTestEntity rdfTestEntity = (RDFTestEntity) obj;
    instance.remove(rdfTestEntity);
    assertNotNull(rdfTestEntity.getId());
    uri1 = rdfTestEntity.getId();
    assertNull(instance.find(
            RDFTestEntity.class,
            uri1));
    instance.close();
    LOGGER.info("  remove OK");

    LOGGER.info("setIdFor");


    instance = new RDFEntityManager();
    rdfTestEntity = new RDFTestEntity();
    rdfTestEntity.setName("my name");
    instance.setIdFor(rdfTestEntity);
    URI id = rdfTestEntity.getId();
    LOGGER.info("setIdFor id:" + id);
    instance.persist(rdfTestEntity);
    assertEquals(id, rdfTestEntity.getId());
    RDFTestEntity loadedTestEntity = instance.find(RDFTestEntity.class, id);
    assertEquals(rdfTestEntity, loadedTestEntity);
    instance.close();
    LOGGER.info("  setIdFor OK");

  }

  /**
   * Test of createId method, of class org.texai.kb.persistence.RDFEntityPersister.
   */
  @Test
  public void testCreateId() {
    LOGGER.info("createId");

    final RDFTestEntity rdfTestEntity = new RDFTestEntity();
    final URI id = RDFEntityManager.createId(rdfTestEntity);
    LOGGER.info("  createId OK");
  }

  /**
   * Test of export method.
   */
  @Test
  public void testExport() {
    LOGGER.info("export");

    RDFEntityManager instance = new RDFEntityManager();
    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    rdfTestEntity1.setName("test entity 1");
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity2.setName("test entity 2");
    try {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter("data/text-export.turtle"))) {
        instance.export(rdfTestEntity1, writer);
        instance.export(rdfTestEntity2, writer);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    LOGGER.info("  export OK");
  }
}
