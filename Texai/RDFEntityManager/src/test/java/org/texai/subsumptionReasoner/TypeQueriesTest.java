/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.subsumptionReasoner;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sf.ehcache.CacheManager;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
public class TypeQueriesTest extends TestCase {

  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;
  /** the repository connection */
  private static RepositoryConnection repositoryConnection;
  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(TypeQueriesTest.class);
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  public TypeQueriesTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
  public static Test suite() {
    final TestSuite suite = new TestSuite();
    suite.addTest(new TypeQueriesTest("testOneTimeSetup"));
    suite.addTest(new TypeQueriesTest("testIsDirectType"));
    suite.addTest(new TypeQueriesTest("testIsType"));
    suite.addTest(new TypeQueriesTest("testGetDirectTypes"));
    suite.addTest(new TypeQueriesTest("testGetDirectTypes2"));
    suite.addTest(new TypeQueriesTest("testGetDirectInstances"));
    suite.addTest(new TypeQueriesTest("testSubClassOfHierarchy"));
    suite.addTest(new TypeQueriesTest("testTypeHierarchy"));
    suite.addTest(new TypeQueriesTest("testOneTimeTearDown"));
    return suite;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /** one time setup */
  public void testOneTimeSetup() {
    LOGGER.info("testOneTimeSetup");
    LOGGER.info("working directory: " + System.getProperty("user.dir"));
    JournalWriter.deleteJournalFiles();
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

    try {
      repositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(TEST_REPOSITORY_NAME);
      repositoryConnection.clear();
      rdfEntityManager = new RDFEntityManager();
      final URI universalVocabularyMt = new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT);
      // <texai:Buster> <rdf:type> <cyc:DomesticCat>
      final Statement statement1 = new StatementImpl(
              new URIImpl(Constants.TEXAI_NAMESPACE + "Buster"),
              RDF.TYPE,
              new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"));
      repositoryConnection.add(statement1, universalVocabularyMt);
      // <cyc:DomesticCat> <rdfs:subClassOf> <cyc:Cat>
      final Statement statement2 = new StatementImpl(
              new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"),
              RDFS.SUBCLASSOF,
              new URIImpl(Constants.CYC_NAMESPACE + "Cat"));
      repositoryConnection.add(statement2, universalVocabularyMt);
      // <cyc:Cat> <rdfs:subClassOf> <cyc:CarnivoreOrder>
      final Statement statement3 = new StatementImpl(
              new URIImpl(Constants.CYC_NAMESPACE + "Cat"),
              RDFS.SUBCLASSOF,
              new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder"));
      repositoryConnection.add(statement3, universalVocabularyMt);
    } catch (RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Test of isDirectType method, of class TypeQueries.
   */
  public void testIsDirectType() {
    LOGGER.info("isDirectType");
    URI term = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    assertTrue(instance.isDirectType(TEST_REPOSITORY_NAME, term, typeTerm));
    assertTrue(!instance.isDirectType(TEST_REPOSITORY_NAME, typeTerm, term));
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    assertTrue(!instance.isDirectType(TEST_REPOSITORY_NAME, term, typeTerm2));
  }

  /**
   * Test of isType method, of class TypeQueries.
   */
  public void testIsType() {
    LOGGER.info("isType");
    URI term = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    assertTrue(instance.isType(TEST_REPOSITORY_NAME, term, typeTerm));
    assertTrue(!instance.isType(TEST_REPOSITORY_NAME, typeTerm, term));
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    assertTrue(instance.isType(TEST_REPOSITORY_NAME, term, typeTerm2));
    assertTrue(!instance.isType(TEST_REPOSITORY_NAME, term, term));
    assertTrue(!instance.isType(TEST_REPOSITORY_NAME, typeTerm2, term));
    assertTrue(!instance.isType(TEST_REPOSITORY_NAME, typeTerm, typeTerm2));
  }

  /**
   * Test of getDirectTypes method, of class TypeQueries.
   */
  public void testGetDirectTypes() {
    LOGGER.info("getDirectTypes");
    URI term = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/DomesticCat]", instance.getDirectTypes(TEST_REPOSITORY_NAME, term).toString());
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    assertEquals("[]", instance.getDirectTypes(TEST_REPOSITORY_NAME, typeTerm).toString());
  }

  /**
   * Test of getDirectTypes method, of class TypeQueries.
   */
  public void testGetDirectTypes2() {
    LOGGER.info("getDirectTypes");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    final String result = RDFUtility.formatResources(instance.getDirectTypes("OpenCyc", typeTerm));
    assertTrue("{cyc:OrganismClassificationType, cyc:DomesticatedAnimalType}".equals(result) ||
            "{cyc:DomesticatedAnimalType, cyc:OrganismClassificationType}".equals(result));
  }

  /**
   * Test of getDirectInstances method, of class TypeQueries.
   */
  public void testGetDirectInstances() {
    LOGGER.info("getDirectInstances");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    assertEquals("[http://texai.org/texai/Buster]", instance.getDirectInstances(TEST_REPOSITORY_NAME, typeTerm).toString());
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    assertEquals("[]", instance.getDirectInstances(TEST_REPOSITORY_NAME, typeTerm2).toString());
  }

  /**
   * Test of subClassOfHierarchy method, of class TypeQueries.
   */
  public void testSubClassOfHierarchy() {
    LOGGER.info("subClassOfHierarchy");
    URI typeTerm1 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    final Set<URI> visitedTypeTerms = new HashSet<URI>();
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    final RepositoryConnection repositoryConnection1 = rdfEntityManager.getConnectionToNamedRepository("OpenCyc");
    List<URI> result = instance.subClassOfHierarchy(
            "OpenCyc",
            typeTerm1,
            typeTerm2,
            visitedTypeTerms,
            repositoryConnection1);
    assertEquals("{cyc:DomesticCat, cyc:Cat}", RDFUtility.formatResources(result));

    LOGGER.info("************************");
    visitedTypeTerms.clear();
    typeTerm2 = new URIImpl(Constants.OWL_NAMESPACE + "Thing");
    result = instance.subClassOfHierarchy(
            "OpenCyc",
            typeTerm1,
            typeTerm2,
            visitedTypeTerms,
            repositoryConnection1);
    assertEquals("{cyc:DomesticCat, cyc:Cat, cyc:Vertebrate, cyc:SentientAnimal, cyc:Animal, cyc:SolidTangibleThing, cyc:Container-Underspecified, cyc:Region-Underspecified, cyc:Location-Underspecified, owl:Thing}", RDFUtility.formatResources(result));

    visitedTypeTerms.clear();
    typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "Dog");
    result = instance.subClassOfHierarchy(
            "OpenCyc",
            typeTerm1,
            typeTerm2,
            visitedTypeTerms,
            repositoryConnection1);
    assertEquals("{}", RDFUtility.formatResources(result));
  }

  /**
   * Test of typeHierarchy method, of class TypeQueries.
   */
  public void testTypeHierarchy() {
    LOGGER.info("typeHierarchy");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "CityOfAustinTX");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "StateCapital");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    List<URI> result = instance.typeHierarchy(
            "OpenCyc",
            term,
            typeTerm);
    assertEquals("{cyc:StateCapital}", RDFUtility.formatResources(result));

    typeTerm = new URIImpl(Constants.OWL_NAMESPACE + "Thing");
    result = instance.typeHierarchy(
            "OpenCyc",
            term,
            typeTerm);
    assertEquals("{cyc:Individual, owl:Thing}", RDFUtility.formatResources(result));

  }

  /** one time tear-down */
  public void testOneTimeTearDown() {
    LOGGER.info("one time tear-down");
    CacheManager.getInstance().shutdown();
    try {
      repositoryConnection.close();
    } catch (RepositoryException ex) {
      throw new TexaiException(ex);
    }
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    try {
      if (testRepositoryDirectory.exists()) {
        FileUtils.deleteDirectory(testRepositoryDirectory);
      }
    } catch (final IOException ex) {
      // ignore failure to delete TMPFS directory
    }
  }
}
