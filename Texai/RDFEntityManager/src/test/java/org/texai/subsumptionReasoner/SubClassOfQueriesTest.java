/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.subsumptionReasoner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
public class SubClassOfQueriesTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(SubClassOfQueriesTest.class);
  /** the test repository name */
  private static String TEST_REPOSITORY_NAME = "Test";
  /** the repository connection */
  private static RepositoryConnection repositoryConnection;
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;

  public SubClassOfQueriesTest(String testName) {
    super(testName);
  }

  /** Returns a method-ordered test suite.
   *
   * @return a method-ordered test suite
   */
public static Test suite() {
  final TestSuite suite = new TestSuite();
   suite.addTest(new SubClassOfQueriesTest("testOneTimeSetup"));
   suite.addTest(new SubClassOfQueriesTest("testIsDirectSubClassOf"));
   suite.addTest(new SubClassOfQueriesTest("testIsSubClassOf"));
   suite.addTest(new SubClassOfQueriesTest("testAreSubClassesOf"));
   suite.addTest(new SubClassOfQueriesTest("testRemoveRedundantTypes"));
   suite.addTest(new SubClassOfQueriesTest("testGetDirectSuperClasses"));
   suite.addTest(new SubClassOfQueriesTest("testGetSuperClasses"));
   suite.addTest(new SubClassOfQueriesTest("testGetDirectSubClasses"));
   suite.addTest(new SubClassOfQueriesTest("testOneTimeTearDown"));
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
    JournalWriter.deleteJournalFiles();
    CacheInitializer.resetCaches();
    CacheInitializer.initializeCaches();

    String testRepositoryPath = System.getenv("REPOSITORIES_TMPFS");
    if (testRepositoryPath == null || testRepositoryPath.isEmpty()) {
      testRepositoryPath = System.getProperty("user.dir") + "/data/repositories";
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
   * Test of isDirectSubClassOf method, of class SubClassOfQueries.
   */
  public void testIsDirectSubClassOf() {
    LOGGER.info("isDirectSubClassOf");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    assertTrue(instance.isDirectSubClassOf(TEST_REPOSITORY_NAME, term, typeTerm));
    assertTrue(!instance.isDirectSubClassOf(TEST_REPOSITORY_NAME, typeTerm, term));
  }

  /**
   * Test of isSubClassOf method, of class SubClassOfQueries.
   */
  public void testIsSubClassOf() {
    LOGGER.info("isSubClassOf");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    URI typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    assertTrue(instance.isSubClassOf(TEST_REPOSITORY_NAME, term, typeTerm));
    assertTrue(!instance.isSubClassOf(TEST_REPOSITORY_NAME, typeTerm, term));
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    assertTrue(instance.isSubClassOf(TEST_REPOSITORY_NAME, term, typeTerm2));

    term = new URIImpl(Constants.CYC_NAMESPACE + "Situation-Localized");
    typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "Situation");
    assertTrue(instance.isSubClassOf("OpenCyc", term, typeTerm));

    term = new URIImpl(Constants.TEXAI_NAMESPACE + "OnPhysicalSituation");
    typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "Situation");
    assertTrue(instance.isSubClassOf("OpenCyc", term, typeTerm));

    term = new URIImpl(Constants.CYC_NAMESPACE + "Winning");
    typeTerm = new URIImpl(Constants.CYC_NAMESPACE + "FirstOrderCollection");
    assertTrue(instance.isSubClassOf("OpenCyc", term, typeTerm));

    term = new URIImpl(Constants.CYC_NAMESPACE + "Winning");
    typeTerm = new URIImpl(Constants.OWL_NAMESPACE + "Thing");
    assertTrue(instance.isSubClassOf("OpenCyc", term, typeTerm));
  }

  /**
   * Test of removeRedundantTypes method, of class SubClassOfQueries.
   */
  public void testRemoveRedundantTypes() {
    LOGGER.info("removeRedundantTypes");
    Collection<URI> typeTerms = new ArrayList<URI>();
    typeTerms.add(new URIImpl(Constants.CYC_NAMESPACE + "Cat"));
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    instance.removeRedundantTypes("OpenCyc", typeTerms);
    assertEquals("{cyc:Cat}", RDFUtility.formatResources(typeTerms));

    typeTerms.add(new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat"));
    instance.removeRedundantTypes("OpenCyc", typeTerms);
    assertEquals("{cyc:DomesticCat}", RDFUtility.formatResources(typeTerms));

    typeTerms.add(new URIImpl(Constants.OWL_NAMESPACE + "Thing"));
    instance.removeRedundantTypes("OpenCyc", typeTerms);
    assertEquals("{cyc:DomesticCat}", RDFUtility.formatResources(typeTerms));
  }

  /**
   * Test of areSubClassesOf method, of class SubClassOfQueries.
   */
  public void testAreSubClassesOf() {
    LOGGER.info("areSubClassesOf");

    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    final List<URI> typeTerms1 = new ArrayList<URI>();
    final List<URI> typeTerms2 = new ArrayList<URI>();
    assertTrue(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    typeTerms1.clear();
    typeTerms2.clear();
    URI typeTerm1 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    typeTerms1.add(typeTerm1);
    assertFalse(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    typeTerms1.clear();
    typeTerms2.clear();
    typeTerms2.add(typeTerm1);
    assertTrue(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    typeTerms1.clear();
    typeTerms2.clear();
    URI typeTerm2 = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    typeTerms1.add(typeTerm1);
    typeTerms2.add(typeTerm2);
    assertTrue(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    typeTerms1.clear();
    typeTerms2.clear();
    typeTerms1.add(typeTerm2);
    typeTerms2.add(typeTerm1);
    assertFalse(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    URI typeTerm3 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    typeTerms1.clear();
    typeTerms2.clear();
    typeTerms1.add(typeTerm1);
    typeTerms2.add(typeTerm2);
    typeTerms2.add(typeTerm3);
    assertTrue(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    typeTerms1.clear();
    typeTerms2.clear();
    typeTerms1.add(typeTerm1); // cyc:DomesticCat
    typeTerms1.add(typeTerm3); // cyc:CarnivoreOrder
    typeTerms2.add(typeTerm2); // cyc:Cat
    assertFalse(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));

    URI typeTerm4 = new URIImpl(Constants.CYC_NAMESPACE + "Computer");
    URI typeTerm5 = new URIImpl(Constants.CYC_NAMESPACE + "System");
    typeTerms1.clear();
    typeTerms2.clear();
    typeTerms1.add(typeTerm1);
    typeTerms1.add(typeTerm4);
    typeTerms2.add(typeTerm3);
    typeTerms2.add(typeTerm5);
    assertTrue(instance.areSubClassesOf("OpenCyc", typeTerms1, typeTerms2));
  }

  /**
   * Test of getDirectSuperClasses method, of class SubClassOfQueries.
   */
  public void testGetDirectSuperClasses() {
    LOGGER.info("getDirectSuperClasses");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/Cat]", instance.getDirectSuperClasses(TEST_REPOSITORY_NAME, term).toString());
    URI term2 = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/CarnivoreOrder]", instance.getDirectSuperClasses(TEST_REPOSITORY_NAME, term2).toString());
  }

  /**
   * Test of getSuperClasses method, of class SubClassOfQueries.
   */
  public void testGetSuperClasses() {
    LOGGER.info("getSuperClasses");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    assertEquals(
            "[http://sw.cyc.com/2006/07/27/cyc/CarnivoreOrder, http://sw.cyc.com/2006/07/27/cyc/Cat]",
            instance.getSuperClasses(TEST_REPOSITORY_NAME, term).toString());
    final List<URI> superClasses = new ArrayList<URI>(instance.getSuperClasses(
            "OpenCyc",
            new URIImpl(Constants.CYC_NAMESPACE + "Winning")));
    Collections.sort(superClasses, new RDFUtility.ResourceComparator());
    LOGGER.info("super classes of cyc:Winning");
    for (final URI superClass : superClasses) {
      LOGGER.info("  " + RDFUtility.formatResource(superClass));
    }
  }

  /**
   * Test of getDirectSubClasses method, of class SubClassOfQueries.
   */
  public void testGetDirectSubClasses() {
    LOGGER.info("getDirectSubClasses");
    URI term = new URIImpl(Constants.CYC_NAMESPACE + "Cat");
    SubClassOfQueries instance = new SubClassOfQueries(rdfEntityManager);
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/DomesticCat]", instance.getDirectSubClasses(TEST_REPOSITORY_NAME, term).toString());
    URI term2 = new URIImpl(Constants.CYC_NAMESPACE + "CarnivoreOrder");
    assertEquals("[http://sw.cyc.com/2006/07/27/cyc/Cat]", instance.getDirectSubClasses(TEST_REPOSITORY_NAME, term2).toString());
    URI term3 = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    assertEquals("[]", instance.getDirectSubClasses(TEST_REPOSITORY_NAME, term3).toString());
  }

  /** one time tear-down */
  public void testOneTimeTearDown() {
    LOGGER.info("testOneTimeTearDown");
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
