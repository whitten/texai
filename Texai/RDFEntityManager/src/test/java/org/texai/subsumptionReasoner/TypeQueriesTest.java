/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.subsumptionReasoner;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class TypeQueriesTest {

  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;
  /** the repository connection */
  private static RepositoryConnection repositoryConnection;
  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(TypeQueriesTest.class);
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  public TypeQueriesTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("testOneTimeSetup");

    JournalWriter.deleteJournalFiles();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

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

  /** one time setup */
  public void testOneTimeSetup() {
  }

  /**
   * Test of isDirectType method, of class TypeQueries.
   */
  @Test
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
  @Test
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
    @Test
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
  @Test
  public void testGetDirectTypes2() {
    LOGGER.info("getDirectTypes");
    TypeQueries instance = new TypeQueries(rdfEntityManager);
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    assertEquals("{cyc:DomesticatedAnimalType, cyc:OrganismClassificationType}", RDFUtility.formatSortedResources(instance.getDirectTypes("OpenCyc", typeTerm)));
  }

  /**
   * Test of getDirectInstances method, of class TypeQueries.
   */
  @Test
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
  @Test
  public void testSubClassOfHierarchy() {
    LOGGER.info("subClassOfHierarchy");
    URI typeTerm1 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    final Set<URI> visitedTypeTerms = new HashSet<>();
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
  @Test
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

}
