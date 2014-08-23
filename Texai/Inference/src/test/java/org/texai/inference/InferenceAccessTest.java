/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.inference;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.inference.sparqlParser.SPARQLParser;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import static org.junit.Assert.*;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;

/**
 *
 * @author reed
 */
public class InferenceAccessTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(InferenceAccessTest.class.getName());
  /** the knowledge base entity manager */
  private static RDFEntityManager rdfEntityManager;

  public InferenceAccessTest() {
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
      final QueryContainer queryContainer = (new SPARQLParser()).parseQuery(queryString, "test1");
      queryContainer.cascadePersist(rdfEntityManager);
      CacheInitializer.resetCaches();
      CacheInitializer.initializeCaches();
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }

  /**
   * Test of findQueryByName method, of class InferenceAccess.
   */
  @Test
  public void findQueryByName() {
    LOGGER.info("findQueryByName");
    InferenceAccess instance = new InferenceAccess(
            rdfEntityManager);
    QueryContainer result = instance.findQueryContainerByName("test1");
    assertNotNull(result);
    assertEquals("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX owl: <http://www.w3.org/2002/07/owl#>\nPREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\nPREFIX texai: <http://texai.org/texai/>\n\nSELECT ?LexicalWord1 ?string\nWHERE {\n  ?Individual owl:sameAs ?Individual .\n  ?Individual rdf:type ?Thing .\n  ?Individual rdf:type texai:FCGClauseSubject .\n  ?Individual rdf:type texai:IndefiniteThingInThisDiscourse .\n  _:Situation_Localized rdf:type cyc:Situation-Localized .\n  _:Situation_Localized cyc:situationConstituents ?Individual .\n  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n}", result.toString());
    result = instance.findQueryContainerByName("xyz");
    assertNull(result);
  }
}
