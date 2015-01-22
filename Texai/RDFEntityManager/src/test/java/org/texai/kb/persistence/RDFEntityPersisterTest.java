/*
 * RDFEntityPersisterTest.java
 * JUnit based test
 *
 * Created on October 31, 2006, 5:04 PM
 *
 * Copyright (C) 2006 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;

/**
 *
 * @author reed
 */
public class RDFEntityPersisterTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityPersisterTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;
  /** the Sesame repository connection */
  static RepositoryConnection repositoryConnection = null;
  /** the RDF entity manager */
  static RDFEntityManager rdfEntityManager = null;

  public RDFEntityPersisterTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("oneTimeSetup");
    CacheInitializer.initializeCaches();
    RDFEntityPersisterTest.class.getClassLoader().setDefaultAssertionStatus(true);
    rdfEntityManager = new RDFEntityManager();
    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    try {
      repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(TEST_REPOSITORY_NAME);
      repositoryConnection.clear();
    } catch (RepositoryException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.INFO);
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
     * Test of getRDFListValues method, of class org.texai.kb.persistence.AbstractRDFEntityAccessor.
     */
  @Test
  public void testGetRDFListValues() {
    LOGGER.info("getRDFListValues");

    URI mySubject = new URIImpl("http://texai.org/texai/mySubject");
    URI myPredicate = new URIImpl("http://texai.org/texai/myPredicate");
    final ValueFactory valueFactory = new ValueFactoryImpl();
    Literal integerElement = valueFactory.createLiteral(Integer.valueOf(1));
    Literal stringElement = valueFactory.createLiteral("a");
    Literal floatElement = valueFactory.createLiteral(Float.valueOf(3.14f));
    BNode bnode1 = valueFactory.createBNode();
    BNode bnode2 = valueFactory.createBNode();
    BNode bnode3 = valueFactory.createBNode();
    URI myContext = valueFactory.createURI("http://texai.org/texai/myContext");
    try {
      // test Sesame adding and removing statements with blank nodes
      repositoryConnection.add(bnode1, RDF.FIRST, integerElement, myContext);
      assertTrue(repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));
      repositoryConnection.remove(bnode1, RDF.FIRST, integerElement, myContext);
      assertTrue(!repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));
      repositoryConnection.add(bnode1, RDF.FIRST, integerElement, myContext);
      assertTrue(repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));
      final Statement statement = valueFactory.createStatement(
              bnode1,
              RDF.FIRST,
              integerElement,
              myContext);
      rdfEntityManager.removeStatement(repositoryConnection, statement);
      assertTrue(!repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));

      // persist the list [1 "a" 3.14]
      repositoryConnection.add(mySubject, myPredicate, bnode1, myContext);
      repositoryConnection.add(bnode1, RDF.FIRST, integerElement, myContext);
      repositoryConnection.add(bnode1, RDF.REST, bnode2, myContext);
      repositoryConnection.add(bnode2, RDF.FIRST, stringElement, myContext);
      repositoryConnection.add(bnode2, RDF.REST, bnode3, myContext);
      repositoryConnection.add(bnode3, RDF.FIRST, floatElement, myContext);
      repositoryConnection.add(bnode3, RDF.REST, RDF.NIL, myContext);
    } catch (RepositoryException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    RDFEntityPersister instance = new RDFEntityPersister(rdfEntityManager);
    instance.setContextURI(myContext);
    instance.setEffectiveContextURI(myContext);
    List<Value> results = instance.getRDFListValues(repositoryConnection, bnode1);
    assertEquals("[\"1\"^^<http://www.w3.org/2001/XMLSchema#int>, \"a\", \"3.14\"^^<http://www.w3.org/2001/XMLSchema#float>]", results.toString());
    try {
      assertTrue(repositoryConnection.hasStatement(mySubject, myPredicate, bnode1, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode1, RDF.REST, bnode2, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode2, RDF.FIRST, stringElement, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode2, RDF.REST, bnode3, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode3, RDF.FIRST, floatElement, false, myContext));
      assertTrue(repositoryConnection.hasStatement(bnode3, RDF.REST, RDF.NIL, false, myContext));
      instance.removeRDFList(
              repositoryConnection,
              rdfEntityManager,
              bnode1);
      assertTrue(repositoryConnection.hasStatement(mySubject, myPredicate, bnode1, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode1, RDF.FIRST, integerElement, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode1, RDF.REST, bnode2, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode2, RDF.FIRST, stringElement, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode2, RDF.REST, bnode3, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode3, RDF.FIRST, floatElement, false, myContext));
      assertTrue(!repositoryConnection.hasStatement(bnode3, RDF.REST, RDF.NIL, false, myContext));
    } catch (RepositoryException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }

  /**
   * Test of addRDFList method, of class org.texai.kb.persistence.AbstractRDFEntityAccessor.
   */
  @Test
  public void testAddRDFList() {
    LOGGER.info("addRDFList");

    List<Value> valueList = new ArrayList<>();
    final ValueFactory valueFactory = new ValueFactoryImpl();
    valueList.add(valueFactory.createLiteral("a"));
    valueList.add(valueFactory.createLiteral(-1));
    valueList.add(valueFactory.createLiteral(1234.0f));
    RDFEntityPersister instance = new RDFEntityPersister(rdfEntityManager);
    URI myContext = valueFactory.createURI("http://texai.org/texai/myContext");
    instance.setContextURI(myContext);
    instance.setEffectiveContextURI(myContext);
    BNode rdfListHead = instance.addRDFList(
            repositoryConnection,
            rdfEntityManager,
            valueList,
            null); // writer
    assertNotNull(rdfListHead);
    List<Value> results = instance.getRDFListValues(repositoryConnection, rdfListHead);
    assertEquals("[\"a\", \"-1\"^^<http://www.w3.org/2001/XMLSchema#int>, \"1234.0\"^^<http://www.w3.org/2001/XMLSchema#float>]", results.toString());
    assertEquals(valueList, results);
  }

  /**
   * Test of persist method, of class org.texai.kb.persistence.RDFEntityPersister.
   */
  @Test
  public void testPersist() {
    LOGGER.info("persist");

    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity1.setDontCareField("do not care");
    rdfTestEntity1.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity1.setMaxNbrOfScooterRiders(2);
    rdfTestEntity1.setName("TestDomainEntity 1");
    rdfTestEntity1.setNumberOfCrew(1);

    final String[] comments1 = {"comment 1", "comment 2"};
    rdfTestEntity1.setComment(comments1);
    List<Integer> integerList = new ArrayList<>();
    integerList.add(1);
    rdfTestEntity1.setIntegerList(integerList);
    rdfTestEntity1.setByteField((byte) 1);

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    Set<String> cyclistNotes = new HashSet<>();
    cyclistNotes.add("note 1");
    cyclistNotes.add("note 2");
    Set<URI> someURIs = new HashSet<>();
    someURIs.add(new URIImpl(Constants.TEXAI_NAMESPACE + "a"));
    rdfTestEntity1.setSomeURIs(someURIs);
    rdfTestEntity2.setDontCareField("do not care");
    rdfTestEntity2.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity2.setMaxNbrOfScooterRiders(2);
    rdfTestEntity2.setName("TestDomainEntity 2");
    rdfTestEntity2.setNumberOfCrew(1);
    final String[] comments2 = {"comment 1", "comment 2"};
    rdfTestEntity2.setComment(comments2);
    final UUID testUUID = UUID.randomUUID();
    rdfTestEntity1.setUuidField(testUUID);
    rdfTestEntity1.mapField = new HashMap<>();
    rdfTestEntity1.mapField.put(1, "a");
    rdfTestEntity1.mapField.put(2, "b");
    rdfTestEntity1.mapField.put(3, "c");

    RDFEntityPersister instance = new RDFEntityPersister(rdfEntityManager);
    LOGGER.info("******************************************************************");
    instance.persist(repositoryConnection, rdfTestEntity1);
    LOGGER.info("******************************************************************");
    assertNotNull(rdfTestEntity1.getId());
    List<RDFTestEntity> myPeers = new ArrayList<>(1);
    rdfTestEntity1.setMyPeers(myPeers);
    myPeers.add(rdfTestEntity2);
    myPeers = new ArrayList<>(1);
    myPeers.add(rdfTestEntity1);
    rdfTestEntity2.setMyPeers(myPeers);
    instance.persist(repositoryConnection, rdfTestEntity1);
    assertNotNull(rdfTestEntity2.getId());
    LOGGER.info("******************************************************************");
    try {
      final String queryString
              = "SELECT s FROM {s} rdf:type {<" + Constants.TEXAI_NAMESPACE + "org.texai.kb.persistence.RDFTestEntity>}";
      TupleQuery subjectsTupleQuery = repositoryConnection.prepareTupleQuery(QueryLanguage.SERQL, queryString);
      final List<URI> instanceURIs = new ArrayList<>();
      final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        instanceURIs.add((URI) tupleQueryResult.next().getBinding("s").getValue());
      }
      if (instanceURIs.isEmpty()) {
        fail("no test entities selected");
      }
    } catch (final RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      fail(ex.getMessage());
    }
    final RDFTestEntity rdfTestEntity3 = new RDFTestEntity();
    rdfTestEntity3.setDontCareField("do not care");
    rdfTestEntity3.setFavoriteTestRDFEntityPeer(rdfTestEntity2);
    rdfTestEntity3.setMaxNbrOfScooterRiders(2);
    myPeers = new ArrayList<>(2);
    myPeers.add(rdfTestEntity1);
    myPeers.add(rdfTestEntity2);
    rdfTestEntity3.setMyPeers(myPeers);
    rdfTestEntity3.setName("TestDomainEntity 2");
    rdfTestEntity3.setNumberOfCrew(1);
    final String[] comments3 = {"comment 1", "comment 2"};
    rdfTestEntity3.setComment(comments3);
    rdfTestEntity1.getMyPeers().add(rdfTestEntity3);
    // should persist new RDF test entity 3, but not RDF test entity 2 which is already has a URI
    System.out.println("******************");
    System.out.println("*  rdfTestEntity1: " + rdfTestEntity1.getId());
    System.out.println("*  rdfTestEntity2: " + rdfTestEntity2.getId());
    System.out.println("******************");
    instance.persist(repositoryConnection, rdfTestEntity1);

    //TODO test moving and then trying to save a lazy-loaded field
    final RDFTestEntity rdfTestEntity4 = new RDFTestEntity();
    rdfTestEntity4.setName("RDF test entity 4");
    rdfEntityManager.persist(repositoryConnection, rdfTestEntity4);
    final RDFTestEntity rdfTestEntity5 = new RDFTestEntity();
    rdfTestEntity5.setFavoriteTestRDFEntityPeer(rdfTestEntity4);
    rdfTestEntity5.setName("RDF test entity 5");
    rdfEntityManager.persist(repositoryConnection, rdfTestEntity5);
    final RDFTestEntity rdfTestEntity5_loaded = rdfEntityManager.find(
            RDFTestEntity.class,
            rdfTestEntity5.getId());
    final RDFTestEntity rdfTestEntity6 = new RDFTestEntity();
    rdfTestEntity6.setFavoriteTestRDFEntityPeer(rdfTestEntity5_loaded.getFavoriteTestRDFEntityPeer());

    LOGGER.info("  persist OK");
  }
}
