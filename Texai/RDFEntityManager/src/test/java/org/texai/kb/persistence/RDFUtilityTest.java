/*
 * RDFUtilityTest.java
 * JUnit based test
 *
 * Created on August 30, 2007, 10:38 PM
 */
package org.texai.kb.persistence;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import static junit.framework.Assert.assertEquals;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFUtility.ResourceComparator;

/**
 *
 * @author reed
 */
public class RDFUtilityTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFUtilityTest.class);
  /** the test repository name */
  private static final String TEST_REPOSITORY_NAME = "Test";
  /** the directory containing the test repository */
  private static File testRepositoryDirectory;
  /** the Sesame repository connection */
  static RepositoryConnection repositoryConnection = null;

  public RDFUtilityTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    LOGGER.info("oneTimeSetup");
    CacheInitializer.initializeCaches();

    DistributedRepositoryManager.addTestRepositoryPath(
            TEST_REPOSITORY_NAME,
            true); // isRepositoryDirectoryCleaned

    try {
      RDFUtilityTest.class.getClassLoader().setDefaultAssertionStatus(true);
      repositoryConnection = DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(TEST_REPOSITORY_NAME);
      repositoryConnection.clear();
    } catch (RepositoryException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    try {
      repositoryConnection.close();
    } catch (final RepositoryException ex) {
      ex.printStackTrace();
    }
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
   * Test of getLiteralForDate method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testGetLiteralForDate() {
    LOGGER.info("getLiteralForDate");

    // January 1, 1970, 00:00:00 GMT
    final Date date = new Date(0L);
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    RDFUtility instance = new RDFUtility(rdfEntityManager);
    final Object literal = instance.getLiteralForDate(date);
    assertTrue(literal instanceof Literal);
    assertTrue(((Literal) literal).getLabel().startsWith("1969-12-31T"));
    rdfEntityManager.close();
  }

  /**
   * Test of getLiteralForCalendar method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testGetLiteralForCalendar() {
    LOGGER.info("getLiteralForCalendar");
    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
    Date date = null;
    try {
      date = sdf.parse("23-Oct-2007 11:22:01");
    } catch (ParseException ex) {
      fail(ex.getMessage());
    }
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(date);
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    RDFUtility instance = new RDFUtility(rdfEntityManager);
    final Object literal = instance.getLiteralForCalendar(calendar);
    assertTrue(literal instanceof Literal);
    assertTrue(((Literal) literal).getLabel().startsWith("2007-10-23T"));
    rdfEntityManager.close();
  }

  /**
   * Test of renameURI method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testRenameURI() {
    LOGGER.info("renameURI");

    // create some statements
    URI a = new URIImpl("http://texai.org/texai/a");
    URI b = new URIImpl("http://texai.org/texai/b");
    URI c = new URIImpl("http://texai.org/texai/c");
    URI d = new URIImpl("http://texai.org/texai/d");
    URI e = new URIImpl("http://texai.org/texai/e");
    URI f = new URIImpl("http://texai.org/texai/f");
    URI g = new URIImpl("http://texai.org/texai/g");
    URI h = new URIImpl("http://texai.org/texai/h");
    URI i = new URIImpl("http://texai.org/texai/i");
    URI j = new URIImpl("http://texai.org/texai/j");
    try {
      // verify adding and removing of statements
      repositoryConnection.add(g, h, i);
      assertTrue(repositoryConnection.hasStatement(g, h, i, false));
      repositoryConnection.remove(g, h, i);
      assertTrue(!repositoryConnection.hasStatement(g, h, i, false));

      repositoryConnection.add(new StatementImpl(h, i, j));
      assertTrue(repositoryConnection.hasStatement(new StatementImpl(h, i, j), false));
      repositoryConnection.remove(new StatementImpl(h, i, j));
      assertTrue(!repositoryConnection.hasStatement(new StatementImpl(h, i, j), false));

      // add test statements containing URI a
      repositoryConnection.add(a, b, c);
      assertTrue(repositoryConnection.hasStatement(a, b, c, true));

      repositoryConnection.add(new StatementImpl(h, a, c));
      assertTrue(repositoryConnection.hasStatement(h, a, c, true));

      repositoryConnection.add(new StatementImpl(a, a, a));
      assertTrue(repositoryConnection.hasStatement(a, a, a, true));

      repositoryConnection.add(new ContextStatementImpl(d, b, c, a));
      assertTrue(repositoryConnection.hasStatement(d, b, c, true, a));

      repositoryConnection.add(new ContextStatementImpl(a, a, a, a));
      assertTrue(repositoryConnection.hasStatement(a, a, a, true, a));

      // add some other statements
      repositoryConnection.add(new StatementImpl(c, b, c));
      repositoryConnection.add(new StatementImpl(d, b, h));
      repositoryConnection.add(new StatementImpl(d, f, a));
      repositoryConnection.add(new ContextStatementImpl(e, b, c, j));
    } catch (RepositoryException ex) {
      fail(ex.getMessage());
    }
    URI oldURI = a;
    URI newURI = b;
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    RDFUtility instance = new RDFUtility(rdfEntityManager);
    // rename all URI a to URI b
    instance.renameURI("Test", oldURI, newURI);
    try {
      assertTrue(repositoryConnection.hasStatement(b, b, c, true));
      assertTrue(!repositoryConnection.hasStatement(a, b, c, false));
      assertTrue(!repositoryConnection.hasStatement(h, a, c, true));
      assertTrue(repositoryConnection.hasStatement(h, b, c, true));
      assertTrue(!repositoryConnection.hasStatement(a, a, a, true));
      assertTrue(repositoryConnection.hasStatement(b, b, b, true));
      assertTrue(!repositoryConnection.hasStatement(d, b, c, true, a));
      assertTrue(repositoryConnection.hasStatement(d, b, c, true, b));
      assertTrue(!repositoryConnection.hasStatement(a, a, a, true, a));
      assertTrue(repositoryConnection.hasStatement(b, b, b, true, b));
    } catch (RepositoryException ex) {
      fail(ex.getMessage());
    }
    rdfEntityManager.close();
  }

  /**
   * Test of formatStatementAsXMLTurtle method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testFormatStatementAsXMLTurtle() {
    LOGGER.info("formatStatementAsXMLTurtle");
    URI a = new URIImpl("http://texai.org/texai/a");
    URI b = new URIImpl("http://texai.org/texai/b");
    URI c = new URIImpl("http://texai.org/texai/c");
    final Statement statement = new StatementImpl(a, b, c);
    assertEquals("<turtle><![CDATA[texai:a texai:b texai:c .]]></turtle>", RDFUtility.formatStatementAsXMLTurtle(statement));
  }

  /**
   * Test of formatStatements method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testFormatStatements() {
    LOGGER.info("formatStatements");
    URI a = new URIImpl("http://texai.org/texai/a");
    URI c = new URIImpl(Constants.CYC_NAMESPACE + "c");
    final List<Statement> statements = new ArrayList<>();
    statements.add(new StatementImpl(a, RDF.TYPE, c));
    statements.add(new StatementImpl(c, RDF.TYPE, a));
    assertEquals("{(texai:a rdf:type cyc:c), (cyc:c rdf:type texai:a)}", RDFUtility.formatStatements(statements));
  }

  /**
   * Test of isVariableURI method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testIsVariableURI() {
    LOGGER.info("isVariableURI");
    assertFalse(RDFUtility.isVariableURI(null));
    assertFalse(RDFUtility.isVariableURI(new URIImpl("http://texai.org/texai/a")));
    assertTrue(RDFUtility.isVariableURI(new URIImpl("http://texai.org/texai/?a")));
  }

  /**
   * Test of isInstanceURI method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testIsInstanceURI() {
    LOGGER.info("isInstanceURI");
    assertFalse(RDFUtility.isInstanceURI(null));
    assertFalse(RDFUtility.isInstanceURI(new URIImpl("http://texai.org/texai/a")));
    assertFalse(RDFUtility.isInstanceURI(new URIImpl("http://texai.org/texai/?a")));
    assertFalse(RDFUtility.isInstanceURI(new URIImpl(Constants.CYC_NAMESPACE + "Cat")));
    assertTrue(RDFUtility.isInstanceURI(new URIImpl("http://texai.org/texai/Cat1")));
  }

  /**
   * Test of decodeNamespace method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testDecodeNamespace() {
    LOGGER.info("decodeNamespace");
    assertEquals("http://www.w3.org/1999/02/22-rdf-syntax-ns#", RDFUtility.decodeNamespace("rdf"));
    assertEquals("http://www.w3.org/2000/01/rdf-schema#", RDFUtility.decodeNamespace("rdfs"));
    assertEquals("http://www.w3.org/2002/07/owl#", RDFUtility.decodeNamespace("owl"));
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/", RDFUtility.decodeNamespace("cyc"));
    assertEquals("http://texai.org/texai/", RDFUtility.decodeNamespace("texai"));
    assertNull(RDFUtility.decodeNamespace("abc"));
  }

  /**
   * Test of formatResources method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testFormatResources() {
    LOGGER.info("formatResources");
    final Set<Resource> resources = new HashSet<>();
    resources.add(RDF.TYPE);
    resources.add(new BNodeImpl("abc"));
    assertEquals("{_:abc, rdf:type}", RDFUtility.formatResources(resources));
  }

  /**
   * Test of ResourceComparator class, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testURIComparator() {
    LOGGER.info("URIComparator");
    final List<Resource> resources = new ArrayList<>();
    resources.add(RDF.TYPE);
    resources.add(new BNodeImpl("xyz"));
    resources.add(new BNodeImpl("def"));
    resources.add(new BNodeImpl("abc"));
    Collections.sort(resources, new ResourceComparator());
    assertEquals("{_:abc, _:def, _:xyz, rdf:type}", RDFUtility.formatResources(resources));
  }

  /**
   * Test of makeURIFromAlias method, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testMakeURIFromAlias() {
    LOGGER.info("makeURIFromAlias");
    assertEquals("http://sw.cyc.com/2006/07/27/cyc/TransportationDevice", RDFUtility.makeURIFromAlias("cyc:TransportationDevice").toString());
    assertEquals("http://texai.org/texai/onPhysical_Subject", RDFUtility.makeURIFromAlias("texai:onPhysical_Subject").toString());
    assertEquals("urn:isbn:0-395-36341-1", RDFUtility.makeURIFromAlias("urn:isbn:0-395-36341-1").toString());
  }

  /**
   * Test of getDefaultClassFromId and getDefaultClassFromIdString methods, of class org.texai.kb.persistence.RDFUtility.
   */
  @Test
  public void testGetDefaultClassFromId() {
    LOGGER.info("getDefaultClassFromId");
    final URI id = new URIImpl("http://texai.org/texai/org.texai.ahcsSupport.domainEntity.RoleType_e75bd4bd-5f29-4b3e-ba2a-298d42acc730");
    assertEquals("org.texai.ahcsSupport.domainEntity.RoleType", RDFUtility.getDefaultClassFromId(id));
    assertEquals("org.texai.ahcsSupport.domainEntity.RoleType", RDFUtility.getDefaultClassFromIdString(id.toString()));
  }
}
