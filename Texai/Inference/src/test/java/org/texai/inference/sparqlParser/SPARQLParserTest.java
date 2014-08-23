/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.inference.sparqlParser;

import java.util.Iterator;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class SPARQLParserTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(SPARQLParserTest.class);
  /** the knowledge base entity manager */
  private static RDFEntityManager rdfEntityManager;

  public SPARQLParserTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    if (rdfEntityManager == null) {
      rdfEntityManager = new RDFEntityManager();
      LOGGER.info("oneTimeSetup");
      JournalWriter.deleteJournalFiles();
      CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "InferenceRules",
            System.getenv("REPOSITORIES_TMPFS") + "/InferenceRules");
      DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }

  /**
   * Test of parseQuery method, of class SPARQLParser.
   */
  @Test
  public void parseQuery() {
    LOGGER.info("parseQuery");
    final SPARQLParser sparqlParser = new SPARQLParser();
    final String queryString =
            "PREFIX rdf: <" + Constants.RDF_NAMESPACE + ">\n" +
            "PREFIX owl: <" + Constants.OWL_NAMESPACE + ">\n" +
            "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n" +
            "PREFIX texai: <" + Constants.TEXAI_NAMESPACE + ">\n" +
            "\n" +
            "SELECT ?LexicalWord1 ?string\n" +
            "WHERE {\n" +
            "  ?Individual owl:sameAs ?Individual .\n" +
            "  ?Individual rdf:type ?Thing .\n" +
            "  ?Individual rdf:type texai:FCGClauseSubject .\n" +
            "  ?Individual rdf:type texai:IndefiniteThingInThisDiscourse .\n" +
            "  _:Situation_Localized rdf:type cyc:Situation-Localized .\n" +
            "  _:Situation_Localized cyc:situationConstituents ?Individual .\n" +
            "  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n" +
            "}";
    LOGGER.info("queryString:\n" + queryString);
    LOGGER.info("");
    final QueryContainer queryContainer = sparqlParser.parseQuery(queryString, "test query");
    LOGGER.info("queryContainer:\n" + queryContainer.toString());
    assertEquals(queryString, queryContainer.toString());
    final String queryString2 = queryContainer.toString();
    final QueryContainer queryContainer2 = sparqlParser.parseQuery(queryString2, "test query");
    assertEquals(queryString, queryContainer2.toString());
    queryContainer.cascadePersist(
            queryContainer,
            rdfEntityManager,
            null);
    final Iterator<QueryContainer> queryContainer_iter = rdfEntityManager.rdfEntityIterator(
            QueryContainer.class,
            null); // overrideContext
    assertTrue(queryContainer_iter.hasNext());
    final QueryContainer queryContainer3 = queryContainer_iter.next();
    assertEquals(queryString, queryContainer3.toString());
    assertTrue(!queryContainer_iter.hasNext());
    LOGGER.info(queryContainer3.toDetailedString());
  }
}
