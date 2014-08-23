/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.inference;

import java.util.List;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.Statement;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.texai.inference.sparqlParser.SPARQLParser;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.turtleStatementParser.TurtleStatementParser;

/**
 *
 * @author reed
 */
public class InferenceEngineTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(InferenceEngineTest.class.getName());
  /** the knowledge base entity manager */
  private static RDFEntityManager rdfEntityManager;

  public InferenceEngineTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    if (rdfEntityManager == null) {
      LOGGER.info("oneTimeSetup");
      JournalWriter.deleteJournalFiles();
      CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "Test",
            System.getenv("REPOSITORIES_TMPFS") + "/Test");
      DistributedRepositoryManager.clearNamedRepository("Test");
      rdfEntityManager = new RDFEntityManager();
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }

  /**
   * Test of evaluate method, of class InferenceEngine.
   * @throws java.lang.Exception when an error occurs
   */
  @Test
  public void evaluate1() throws Exception {
    System.out.println("evaluate 1");

    String queryString =
            "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n" +
            "\n" +
            "SELECT ?LexicalWord1 ?CharacterString\n" +
            "WHERE {\n" +
            "  ?LexicalWord1 cyc:wordStrings ?CharacterString .\n" +
            "}";
    SPARQLParser sparqlParser = new SPARQLParser();
    QueryContainer queryContainer = sparqlParser.parseQuery(queryString, "test1");
    queryContainer.cascadePersist(rdfEntityManager);

    //  PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>
    //
    //  SELECT ?LexicalWord1 ?CharacterString
    //  WHERE {
    //    ?LexicalWord1 cyc:wordStrings ?CharacterString .
    //  }

    assertEquals("PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\n\nSELECT ?LexicalWord1 ?CharacterString\nWHERE {\n  ?LexicalWord1 cyc:wordStrings ?CharacterString .\n}", queryContainer.toString());

    String statementsString =
            "texai:LexicalWord1 cyc:wordStrings \"Buster\" .\n";
    TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(statementsString);
    List<Statement> statements = turtleStatementParser.Statements();
    assertEquals(1, statements.size());

    InferenceEngine instance = new InferenceEngine();
    TupleQueryResult result = instance.evaluate(queryContainer, statements);
    assertNotNull(result);
    assertEquals(1L, instance.getScratchPadRepositoryConnection().size());
    assertTrue(instance.getScratchPadRepositoryConnection().hasStatement(statements.get(0), false));
    assertTrue(result.hasNext());
    BindingSet bindingSet = result.next();
    assertEquals("[CharacterString=\"Buster\";LexicalWord1=http://texai.org/texai/LexicalWord1]", bindingSet.toString());
    result.close();

    queryString =
            "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n" +
            "PREFIX texai: <" + Constants.TEXAI_NAMESPACE + ">\n" +
            "\n" +
            "SELECT ?LexicalWord1 ?string\n" +
            "WHERE {\n" +
            "  ?LexicalWord1 cyc:wordStrings ?string .\n" +
            "  ?LexicalWord1 texai:fcgStatus texai:SingleObject .\n" +
            "  ?LexicalWord1 texai:typeOrSubClassOf cyc:LexicalWord .\n" +
            "  ?LexicalWord1 texai:typeOrSubClassOf cyc:ProperCountNoun .\n" +
            "}";
    sparqlParser = new SPARQLParser();
    queryContainer = sparqlParser.parseQuery(queryString, "X_is_a_proper_noun");
    queryContainer.cascadePersist(rdfEntityManager);

    //  PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>
    //  PREFIX texai: <http://texai.org/texai/>
    //
    //  SELECT ?LexicalWord1 ?string
    //  WHERE {
    //    ?LexicalWord1 cyc:wordStrings ?string .
    //    ?LexicalWord1 texai:fcgStatus texai:SingleObject .
    //    ?LexicalWord1 texai:typeOrSubClassOf cyc:LexicalWord .
    //    ?LexicalWord1 texai:typeOrSubClassOf cyc:ProperCountNoun .
    //  }

    assertEquals("PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\nPREFIX texai: <http://texai.org/texai/>\n\nSELECT ?LexicalWord1 ?string\nWHERE {\n  ?LexicalWord1 cyc:wordStrings ?string .\n  ?LexicalWord1 texai:fcgStatus texai:SingleObject .\n  ?LexicalWord1 texai:typeOrSubClassOf cyc:LexicalWord .\n  ?LexicalWord1 texai:typeOrSubClassOf cyc:ProperCountNoun .\n}", queryContainer.toString());

    statementsString =
            "texai:LexicalWord1 cyc:wordStrings \"Buster\" .\n" +
            "texai:LexicalWord1 texai:fcgDiscourseRole texai:External .\n" +
            "texai:LexicalWord1 texai:fcgStatus texai:SingleObject .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf cyc:LexicalWord .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf cyc:ProperCountNoun .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf texai:IndefiniteThingInThisDiscourse .\n" +
            "texai:LexicalWord1 rdf:type texai:FCGClauseSubject .\n" +
            "texai:Situation-Localized3 cyc:situationConstituents texai:LexicalWord1 .\n" +
            "texai:Situation-Localized3 texai:situationHappeningOnDate cyc:Now .\n" +
            "texai:Situation-Localized3 texai:typeOrSubClassOf cyc:Situation-Localized .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(statementsString);
    statements = turtleStatementParser.Statements();

    result = instance.evaluate(queryContainer, statements);
    assertNotNull(result);
    assertTrue(result.hasNext());
    bindingSet = result.next();
    assertEquals("[string=\"Buster\";LexicalWord1=http://texai.org/texai/LexicalWord1]", bindingSet.toString());
    result.close();

    statementsString =
            "texai:LexicalWord1 cyc:wordStrings \"Taz\" .\n" +
            "texai:LexicalWord1 texai:fcgDiscourseRole texai:External .\n" +
            "texai:LexicalWord1 texai:fcgStatus texai:SingleObject .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf cyc:LexicalWord .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf cyc:ProperCountNoun .\n" +
            "texai:LexicalWord1 texai:typeOrSubClassOf texai:IndefiniteThingInThisDiscourse .\n" +
            "texai:LexicalWord1 rdf:type texai:FCGClauseSubject .\n" +
            "texai:Situation-Localized3 cyc:situationConstituents texai:LexicalWord1 .\n" +
            "texai:Situation-Localized3 texai:situationHappeningOnDate cyc:Now .\n" +
            "texai:Situation-Localized3 texai:typeOrSubClassOf cyc:Situation-Localized .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(statementsString);
    statements = turtleStatementParser.Statements();

    result = instance.evaluate(queryContainer, statements);
    assertNotNull(result);
    assertTrue(result.hasNext());
    bindingSet = result.next();
    assertEquals("[string=\"Taz\";LexicalWord1=http://texai.org/texai/LexicalWord1]", bindingSet.toString());
    result.close();

    assertEquals("{?string=\"Taz\", ?LexicalWord1=http://texai.org/texai/LexicalWord1}", instance.evaluateWithSingleResult(queryContainer, statements).toString());

    // test "X is a Y"

    queryString =
            "PREFIX rdf: <" + Constants.RDF_NAMESPACE + ">\n" +
            "PREFIX owl: <" + Constants.OWL_NAMESPACE + ">\n" +
            "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n" +
            "PREFIX texai: <" + Constants.TEXAI_NAMESPACE + ">\n" +
            "\n" +
            "SELECT ?individual ?Thing\n" +
            "WHERE {\n" +
            "  ?individual owl:sameAs ?individual .\n" +
            "  ?individual rdf:type ?Thing .\n" +
            "  ?individual rdf:type texai:FCGClauseSubject .\n" +
            "  ?individual rdf:type texai:IndefiniteThingInThisDiscourse .\n" +
            "  _:Situation_Localized rdf:type cyc:Situation-Localized .\n" +
            "  _:Situation_Localized cyc:situationConstituents ?individual .\n" +
            "  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n" +
            "  FILTER (!sameTerm(?Thing, texai:FCGClauseSubject) && !sameTerm(?Thing, texai:IndefiniteThingInThisDiscourse))\n" +
            "}";
    sparqlParser = new SPARQLParser();
    queryContainer = sparqlParser.parseQuery(queryString, "X_is_a_Y");
    queryContainer.cascadePersist(rdfEntityManager);

    //  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
    //  PREFIX owl: <http://www.w3.org/2002/07/owl#>
    //  PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>
    //  PREFIX texai: <http://texai.org/texai/>
    //
    //  SELECT ?individual ?Thing
    //  WHERE {
    //    ?individual owl:sameAs ?individual .
    //    ?individual rdf:type ?Thing .
    //    ?individual rdf:type texai:FCGClauseSubject .
    //    ?individual rdf:type texai:IndefiniteThingInThisDiscourse .
    //    _:Situation_Localized rdf:type cyc:Situation-Localized .
    //    _:Situation_Localized cyc:situationConstituents ?individual .
    //    _:Situation_Localized texai:situationHappeningOnDate cyc:Now .
    //    FILTER (!sameTerm(?Thing, texai:FCGClauseSubject) && !sameTerm(?Thing, texai:IndefiniteThingInThisDiscourse))
    //  }

    assertEquals("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\nPREFIX owl: <http://www.w3.org/2002/07/owl#>\nPREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\nPREFIX texai: <http://texai.org/texai/>\n\nSELECT ?individual ?Thing\nWHERE {\n  ?individual owl:sameAs ?individual .\n  ?individual rdf:type ?Thing .\n  ?individual rdf:type texai:FCGClauseSubject .\n  ?individual rdf:type texai:IndefiniteThingInThisDiscourse .\n  _:Situation_Localized rdf:type cyc:Situation-Localized .\n  _:Situation_Localized cyc:situationConstituents ?individual .\n  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n  FILTER (!sameTerm(?Thing, texai:FCGClauseSubject) && !sameTerm(?Thing, texai:IndefiniteThingInThisDiscourse))\n}", queryContainer.toString());

    statementsString =
            "texai:Buster rdf:type texai:FCGClauseSubject .\n" +
            "texai:Buster rdf:type texai:IndefiniteThingInThisDiscourse .\n" +
            "texai:Buster rdf:type cyc:DomesticCat .\n" +
            "texai:Buster owl:sameAs texai:Buster .\n" +
            "texai:Situation-Localized1 cyc:situationConstituents texai:Buster .\n" +
            "texai:Situation-Localized1 texai:situationHappeningOnDate cyc:Now .\n" +
            "texai:Situation-Localized1 rdf:type cyc:Situation-Localized .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(statementsString);
    statements = turtleStatementParser.Statements();

    result = instance.evaluate(queryContainer, statements);
    assertNotNull(result);
    assertTrue(result.hasNext());
    bindingSet = result.next();
    assertEquals("[individual=http://texai.org/texai/Buster;Thing=http://sw.cyc.com/2006/07/27/cyc/DomesticCat]", bindingSet.toString());
    result.close();

    instance.finalization();
  }
}
