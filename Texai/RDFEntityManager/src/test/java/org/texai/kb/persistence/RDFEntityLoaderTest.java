/*
 * RDFEntityLoaderTest.java
 * JUnit based test
 *
 * Created on August 8, 2007, 9:23 PM
 *
 * Description: .
 *
 * Copyright (C) August 8, 2007 Stephen L. Reed.
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.apache.ws.jaxme.impl.DatatypeConverterImpl;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.CacheInitializer;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class RDFEntityLoaderTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityLoaderTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the Sesame repository connection */
  static RepositoryConnection repositoryConnection = null;
  /** the RDF entity manager */
  static RDFEntityManager rdfEntityManager = null;

  /**
   * Creates a new instance of RDFEntityLoaderTest.
   */
  public RDFEntityLoaderTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
    DatatypeConverter.setDatatypeConverter(new DatatypeConverterImpl());
    rdfEntityManager = new RDFEntityManager();
    repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(TEST_REPOSITORY_NAME);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    CacheManager.getInstance().shutdown();
    try {
      assertTrue(repositoryConnection.isAutoCommit());
      rdfEntityManager.close();
      DistributedRepositoryManager.shutDown();
    } catch (final RepositoryException ex) {
      ex.printStackTrace();
    }
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /** tests Sesame queries that use context. */
  @Test
  public void testSesameQueriesUsingContext() {
    LOGGER.info("test Sesame queries using context");

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    try {
      getClass().getClassLoader().setDefaultAssertionStatus(true);
      LOGGER.info("repositoryConnection " + repositoryConnection);
      repositoryConnection.clear();
    } catch (RepositoryException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }

    // create some statements
    URI subject1 = new URIImpl("http://texai.org/texai/subject1");
    URI predicate1 = new URIImpl("http://texai.org/texai/predicate1");
    URI object1 = new URIImpl("http://texai.org/texai/object1");
    URI context1 = new URIImpl("http://texai.org/texai/context1");
    URI context2 = new URIImpl("http://texai.org/texai/context2");
    try {
      // verify adding and quering of statements using context
      repositoryConnection.add(subject1, predicate1, object1);
      assertTrue(repositoryConnection.hasStatement(subject1, predicate1, object1, false));
      repositoryConnection.add(subject1, predicate1, object1, context1);
      assertTrue(repositoryConnection.hasStatement(subject1, predicate1, object1, false));
      assertTrue(repositoryConnection.hasStatement(subject1, predicate1, object1, false, context1));
      repositoryConnection.add(subject1, predicate1, object1, context2);
      assertTrue(repositoryConnection.hasStatement(subject1, predicate1, object1, false));
      assertTrue(repositoryConnection.hasStatement(subject1, predicate1, object1, false, context2));

      assertEquals(1, repositoryConnection.size(context1));
      assertEquals(1, repositoryConnection.size(context2));
      assertEquals(3, repositoryConnection.size());

      // get all statements in the repository
      final RepositoryResult<Statement> statements = repositoryConnection.getStatements(null, null, null, false);
      for (final Statement statement : statements.asList()) {
        LOGGER.info("statement: " + statement);
      }

      // query 1
      final TupleQuery tupleQuery1 = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o FROM {s} p {o}");
      tupleQuery1.setBinding("s", subject1);
      tupleQuery1.setBinding("p", predicate1);
      TupleQueryResult tupleQueryResult1 = tupleQuery1.evaluate();
      BindingSet bindingSet1 = tupleQueryResult1.next();
      Binding objectBinding1 = bindingSet1.getBinding("o");
      assertNotNull(objectBinding1);
      assertEquals(object1, objectBinding1.getValue());
      assertTrue(tupleQueryResult1.hasNext());
      bindingSet1 = tupleQueryResult1.next();
      objectBinding1 = bindingSet1.getBinding("o");
      assertNotNull(objectBinding1);
      assertEquals(object1, objectBinding1.getValue());
      assertTrue(tupleQueryResult1.hasNext());
      bindingSet1 = tupleQueryResult1.next();
      objectBinding1 = bindingSet1.getBinding("o");
      assertNotNull(objectBinding1);
      assertEquals(object1, objectBinding1.getValue());
      assertTrue(!tupleQueryResult1.hasNext());
      tupleQueryResult1.close();

      // query 2
      final TupleQuery tupleQuery2 = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o, c FROM CONTEXT c {s} p {o}");

      tupleQuery2.setBinding("s", subject1);
      tupleQuery2.setBinding("p", predicate1);
      tupleQuery2.setBinding("c", context1);
      final TupleQueryResult tupleQueryResult2 = tupleQuery2.evaluate();
      final BindingSet bindingSet2 = tupleQueryResult2.next();
      final Binding objectBinding2 = bindingSet2.getBinding("o");
      assertNotNull(objectBinding2);
      assertEquals(object1, objectBinding2.getValue());
      assertTrue(!tupleQueryResult2.hasNext());
      tupleQueryResult2.close();

      // query 3
      final TupleQuery tupleQuery3 = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o, c FROM CONTEXT c {s} p {o}");

      tupleQuery3.setBinding("s", subject1);
      tupleQuery3.setBinding("p", predicate1);
      tupleQuery3.setBinding("c", context2);
      final TupleQueryResult tupleQueryResult3 = tupleQuery3.evaluate();
      final BindingSet bindingSet3 = tupleQueryResult3.next();
      final Binding objectBinding3 = bindingSet3.getBinding("o");
      assertNotNull(objectBinding3);
      assertEquals(object1, objectBinding3.getValue());
      assertTrue(!tupleQueryResult3.hasNext());
      tupleQueryResult3.close();

      // note that this query ignores statements without a context, even when c is not bound.
      final TupleQuery objectsTupleQuery4 = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT o, c FROM CONTEXT c {s} p {o}");

      objectsTupleQuery4.setBinding("s", subject1);
      objectsTupleQuery4.setBinding("p", predicate1);
      TupleQueryResult tupleQueryResult4 = objectsTupleQuery4.evaluate();
      BindingSet bindingSet4 = tupleQueryResult4.next();
      Binding objectBinding4 = bindingSet4.getBinding("o");
      assertNotNull(objectBinding4);
      assertEquals(object1, objectBinding4.getValue());
      Binding contextBinding4 = bindingSet4.getBinding("c");
      assertNotNull(contextBinding4);
      assertEquals(context1, contextBinding4.getValue());
      assertTrue(tupleQueryResult4.hasNext());
      bindingSet4 = tupleQueryResult4.next();
      objectBinding4 = bindingSet4.getBinding("o");
      assertNotNull(objectBinding4);
      assertEquals(object1, objectBinding4.getValue());
      contextBinding4 = bindingSet4.getBinding("c");
      assertNotNull(contextBinding4);
      assertEquals(context2, contextBinding4.getValue());
      assertTrue(!tupleQueryResult4.hasNext());

      tupleQueryResult4.close();
    } catch (RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of find(final Class clazz, final URI instanceURI) method, of class org.texai.kb.persistence.RDFEntityLoader.
   */
  @Test
  public void testFind() {
    LOGGER.info("find");

    // persist two RDF entities
    final RDFTestEntity rdfTestEntity1 = new RDFTestEntity();
    final RDFTestEntity rdfTestEntity2 = new RDFTestEntity();
    rdfTestEntity1.setIsSomething(true);
    rdfTestEntity1.setIsSomethingElse(true);
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
    Set<Integer> someIntegers = new HashSet<>();
    someIntegers.add(1);
    someIntegers.add(2);
    rdfTestEntity1.setSomeIntegers(someIntegers);
    Set<URI> someURIs = new ArraySet<>();
    someURIs.add(new URIImpl("http://texai.org/texai/uri1"));
    someURIs.add(new URIImpl("http://texai.org/texai/uri2"));
    rdfTestEntity1.setSomeURIs(someURIs);
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
    final UUID testUUID = UUID.randomUUID();
    rdfTestEntity1.setUuidField(testUUID);

    // set XML datatype fields in the first test RDF entity
    rdfTestEntity1.setByteField((byte) 5);
    rdfTestEntity1.setUnsignedByteField((byte) 4);
    rdfTestEntity1.setIntField(-6);
    rdfTestEntity1.setUnsignedIntField(303);
    rdfTestEntity1.setLongField(-7L);
    rdfTestEntity1.setUnsignedLongField(404);
    rdfTestEntity1.setFloatField(1.1F);
    rdfTestEntity1.setDoubleField(1.2D);
    rdfTestEntity1.setBigIntegerField(new BigInteger("-100"));
    rdfTestEntity1.setPositiveBigIntegerField(new BigInteger("101"));
    rdfTestEntity1.setNonNegativeBigIntegerField(new BigInteger("0"));
    rdfTestEntity1.setNonPositiveBigIntegerField(new BigInteger("-102"));
    rdfTestEntity1.setNegativeBigIntegerField(new BigInteger("-103"));
    rdfTestEntity1.setBigDecimalField(new BigDecimal("-100.001"));
    rdfTestEntity1.setCalendarField(Calendar.getInstance());
    rdfTestEntity1.setDateTimeField(new DateTime());
    rdfTestEntity1.setDateField(Calendar.getInstance().getTime());
    rdfTestEntity1.mapField = new HashMap<>();
    rdfTestEntity1.mapField.put(1, "a");
    rdfTestEntity1.mapField.put(2, "b");
    rdfTestEntity1.mapField.put(3, "c");

    assertNotNull(rdfEntityManager);
    RDFEntityPersister rdfEntityPersister = new RDFEntityPersister(rdfEntityManager);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    rdfEntityPersister.persist(repositoryConnection, rdfTestEntity1);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    assertNotNull(rdfTestEntity1.getId());

    // load a persisted entity
    RDFEntityLoader instance = new RDFEntityLoader();
    URI uri1 = rdfTestEntity1.getId();
    Object result = instance.find(
            repositoryConnection,
            RDFTestEntity.class,
            uri1);
    assertNotNull(result);
    assertTrue(result instanceof RDFTestEntity);
    RDFTestEntity rdfTestEntity1_Loaded = (RDFTestEntity) result;
    assertEquals(rdfTestEntity1.getId(), rdfTestEntity1_Loaded.getId());
    assertEquals(rdfTestEntity1.getName(), rdfTestEntity1_Loaded.getName());
    assertEquals(rdfTestEntity1.isSomething(), rdfTestEntity1_Loaded.isSomething());
    assertEquals(rdfTestEntity1.isSomethingElse(), rdfTestEntity1_Loaded.isSomethingElse());
    assertEquals(rdfTestEntity1.getNumberOfCrew(), rdfTestEntity1_Loaded.getNumberOfCrew());
    assertEquals(rdfTestEntity1.getMaxNbrOfScooterRiders(), rdfTestEntity1_Loaded.getMaxNbrOfScooterRiders());
    assertEquals(rdfTestEntity1.getMyPeers().size(), rdfTestEntity1_Loaded.getMyPeers().size());
    assertEquals(1, rdfTestEntity1.getMyPeers().size());
    assertEquals(1, rdfTestEntity1_Loaded.getMyPeers().size());
    assertTrue(rdfTestEntity1.getMyPeers() instanceof List<?>);
    assertTrue(rdfTestEntity1_Loaded.getMyPeers() instanceof List<?>);
    assertTrue(rdfTestEntity1.getMyPeers().get(0) instanceof RDFTestEntity);
    LOGGER.info("rdfTestEntity1_Loaded.getMyPeers().get(0): " + rdfTestEntity1_Loaded.getMyPeers().get(0).getClass().getName());
    assertTrue(rdfTestEntity1_Loaded.getMyPeers().get(0) instanceof RDFTestEntity);
    LOGGER.info("rdfTestEntity1 peers: " + rdfTestEntity1.getMyPeers().toString());
    LOGGER.info("rdfTestEntity1_Loaded peers: " + rdfTestEntity1_Loaded.getMyPeers().toString());
    assertEquals(rdfTestEntity1.getMyPeers(), rdfTestEntity1_Loaded.getMyPeers());
    LOGGER.info("loaded dontCareField: " + rdfTestEntity1_Loaded.getDontCareField());
    assertEquals(rdfTestEntity1.getFavoriteTestRDFEntityPeer(), rdfTestEntity1_Loaded.getFavoriteTestRDFEntityPeer());
    assertNotNull(rdfTestEntity1_Loaded.getComment());
    assertEquals(rdfTestEntity1.getComment().length, rdfTestEntity1_Loaded.getComment().length);
    for (int i = 0; i < rdfTestEntity1.getComment().length; i++) {
      assertEquals(rdfTestEntity1.getComment()[i], rdfTestEntity1_Loaded.getComment()[i]);
    }
    assertEquals(testUUID, rdfTestEntity1.getUuidField());

    // test that  XML datatype fields loaded OK
    assertEquals(rdfTestEntity1.getByteField(), rdfTestEntity1_Loaded.getByteField());
    assertEquals(rdfTestEntity1.getUnsignedByteField(), rdfTestEntity1_Loaded.getUnsignedByteField());
    assertEquals(rdfTestEntity1.getIntField(), rdfTestEntity1_Loaded.getIntField());
    assertEquals(rdfTestEntity1.getUnsignedIntField(), rdfTestEntity1_Loaded.getUnsignedIntField());
    assertEquals(rdfTestEntity1.getLongField(), rdfTestEntity1_Loaded.getLongField());
    assertEquals(rdfTestEntity1.getUnsignedLongField(), rdfTestEntity1_Loaded.getUnsignedLongField());
    assertTrue(rdfTestEntity1.getFloatField() == rdfTestEntity1_Loaded.getFloatField());
    assertEquals(rdfTestEntity1.getBigIntegerField(), rdfTestEntity1_Loaded.getBigIntegerField());
    assertEquals(rdfTestEntity1.getPositiveBigIntegerField(), rdfTestEntity1_Loaded.getPositiveBigIntegerField());
    assertEquals(rdfTestEntity1.getNonNegativeBigIntegerField(), rdfTestEntity1_Loaded.getNonNegativeBigIntegerField());
    assertEquals(rdfTestEntity1.getNonPositiveBigIntegerField(), rdfTestEntity1_Loaded.getNonPositiveBigIntegerField());
    assertEquals(rdfTestEntity1.getNegativeBigIntegerField(), rdfTestEntity1_Loaded.getNegativeBigIntegerField());
    assertEquals(rdfTestEntity1.getBigDecimalField(), rdfTestEntity1_Loaded.getBigDecimalField());
    assertEquals(rdfTestEntity1.getDateField(), rdfTestEntity1_Loaded.getDateField());
    assertEquals(rdfTestEntity1.getDateTimeField().toString(), rdfTestEntity1_Loaded.getDateTimeField().toString());
    assertEquals(rdfTestEntity1.getCalendarField().getTime(), rdfTestEntity1_Loaded.getCalendarField().getTime());
    assertTrue(Math.abs(rdfTestEntity1.getDoubleField() - rdfTestEntity1_Loaded.getDoubleField()) < 0.0000001);
    assertEquals(2, rdfTestEntity1_Loaded.getSomeIntegers().size());
    assertTrue(rdfTestEntity1_Loaded.getSomeIntegers().contains(1));
    assertTrue(rdfTestEntity1_Loaded.getSomeIntegers().contains(2));
    assertEquals(2, rdfTestEntity1_Loaded.getSomeURIs().size());
    assertTrue(rdfTestEntity1_Loaded.getSomeURIs().toString().contains("http://texai.org/texai/uri1"));
    assertTrue(rdfTestEntity1_Loaded.getSomeURIs().toString().contains("http://texai.org/texai/uri2"));
    assertEquals(3, rdfTestEntity1_Loaded.mapField.size());  // load lazy field
    assertEquals("{1=a, 2=b, 3=c}", rdfTestEntity1_Loaded.mapField.toString());

    // test find without specifying the class
    Object result2 = instance.find(
            repositoryConnection,
            uri1);
    assertTrue(result2 instanceof RDFTestEntity);

    LOGGER.info("  find OK");
  }

  /**
   * Test of find(
   *      final URI property,
   *      final Value value,
   *      final Class rdfEntityClass) method, of class org.texai.kb.persistence.RDFEntityLoader.
   */
  @Test
  public void testFindByGivenPropertyAndValue() {
    LOGGER.info("find by given property and value");

    // load a persisted entity
    RDFEntityLoader instance = new RDFEntityLoader();

    URI predicate = new URIImpl("http://sw.cyc.com/2006/07/27/cyc/prettyString-Canonical");
    Value value = new LiteralImpl("TestDomainEntity 1");
    List<RDFTestEntity> resultList = instance.find(
            repositoryConnection,
            predicate,
            value,
            RDFTestEntity.class);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    assertTrue(resultList.get(0) instanceof RDFTestEntity);
    RDFTestEntity result = resultList.get(0);
    assertEquals("TestDomainEntity 1", result.getName());

    List<RDFTestEntity> resultList2 = instance.find(
            repositoryConnection,
            predicate,
            value,
            RDFTestEntity.class);
    assertNotNull(resultList2);
    assertEquals(1, resultList2.size());
    result = resultList2.get(0);
    assertEquals("TestDomainEntity 1", result.getName());

    LOGGER.info("  find by given property and value OK");
  }

  /**
   * Test of rdfEntityIterator method, of class org.texai.kb.ejb.session.RDFEntityLoader.
   */
  @Test
  public void testRDFEntityIterator() {
    LOGGER.info("rdfEntityIterator");

    // load a persisted entity
    RDFEntityLoader instance = new RDFEntityLoader();
    Iterator<RDFTestEntity> iterator = instance.rdfEntityIterator(
            repositoryConnection,
            RDFTestEntity.class, null);
    assertNotNull(iterator);
    int count = 0;
    while (iterator.hasNext()) {
      final Object obj = iterator.next();
      assertTrue(obj instanceof RDFTestEntity);
      RDFTestEntity rdfTestEntity = (RDFTestEntity) obj;
      LOGGER.info("  iteration: " + ++count + " RDF entity: " + rdfTestEntity.getName());
    }
    assertEquals(2, count);
    LOGGER.info("  rdfEntityIterator OK");
  }

  /** Tests list and array field edits. */
  @Test
  public void testListAndArrayFieldEdits() {
    LOGGER.info("list and array field edits");

    RDFEntityLoader instance = new RDFEntityLoader();

    // load an RDF test entity instance
    URI predicate = new URIImpl("http://sw.cyc.com/2006/07/27/cyc/prettyString-Canonical");
    Value value = new LiteralImpl("TestDomainEntity 1");
    List<RDFTestEntity> resultList = instance.find(
            repositoryConnection,
            predicate,
            value,
            RDFTestEntity.class);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    assertTrue(resultList.get(0) instanceof RDFTestEntity);
    RDFTestEntity rdfTestEntity1 = resultList.get(0);
    rdfTestEntity1.getCyclistNotes().size();  // load the lazy-loaded field
    assertEquals("[]", rdfTestEntity1.getCyclistNotes().toString());
    rdfTestEntity1.getIntegerList().size();  // load the lazy-loaded field
    assertEquals("[]", rdfTestEntity1.getIntegerList().toString());
    assertEquals("[comment 1, comment 2]", Arrays.asList(rdfTestEntity1.getComment()).toString());

    rdfTestEntity1.getCyclistNotes().add("a");
    rdfTestEntity1.getIntegerList().add(1);
    rdfTestEntity1.getComment()[1] = "modified comment 2";
    RDFEntityPersister rdfEntityPersister = new RDFEntityPersister(rdfEntityManager);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    rdfEntityPersister.persist(repositoryConnection, rdfTestEntity1);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    RDFTestEntity rdfTestEntity2 = instance.find(
            repositoryConnection,
            RDFTestEntity.class,
            rdfTestEntity1.getId());
    rdfTestEntity2.getCyclistNotes().size();  // load the lazy-loaded field
    assertEquals("[a]", rdfTestEntity2.getCyclistNotes().toString());
    rdfTestEntity2.getIntegerList().size();  // load the lazy-loaded field
    assertEquals("[1]", rdfTestEntity2.getIntegerList().toString());
    assertEquals("[comment 1, modified comment 2]", Arrays.asList(rdfTestEntity2.getComment()).toString());
    Set<String> cyclistNotes = new ArraySet<>();
    cyclistNotes.add("x");
    cyclistNotes.add("y");
    cyclistNotes.add("z");
    rdfTestEntity2.setCyclistNotes(cyclistNotes);
    String[] comment = {"h", "i", "j", "k"};
    rdfTestEntity2.setComment(comment);
    ArrayList<Integer> integerList = new ArrayList<>();
    integerList.add(101);
    integerList.add(102);
    integerList.add(104);
    integerList.add(105);
    integerList.add(106);
    rdfTestEntity2.setIntegerList(integerList);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    rdfEntityPersister.persist(repositoryConnection, rdfTestEntity2);
    try {
      assertTrue(repositoryConnection.isAutoCommit());
    } catch (final RepositoryException ex) {
      fail(ex.getMessage());
    }
    RDFTestEntity rdfTestEntity3 = instance.find(
            repositoryConnection,
            RDFTestEntity.class,
            rdfTestEntity1.getId());
    rdfTestEntity3.getCyclistNotes().size();  // load the lazy-loaded field
    assertEquals("[x, y, z]", rdfTestEntity3.getCyclistNotes().toString());
    rdfTestEntity3.getIntegerList().size();  // load the lazy-loaded field
    assertEquals("[101, 102, 104, 105, 106]", rdfTestEntity3.getIntegerList().toString());
    assertEquals("[h, i, j, k]", Arrays.asList(rdfTestEntity3.getComment()).toString());
    LOGGER.info("  list and array field edits OK");
  }
}
