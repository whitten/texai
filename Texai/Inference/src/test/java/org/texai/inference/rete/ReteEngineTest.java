/*
 * ReteEngineTest.java
 *
 * Created on Jun 30, 2008, 3:47:41 PM
 *
 * Description: .
 *
 * Copyright (C) Aug 12, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.inference.rete;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.texai.inference.QueryContainerInitializer;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.inference.sparqlParser.SPARQLParser;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;

/**
 *
 * @author reed
 */
public class ReteEngineTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ReteEngineTest.class);
  /** the test query container 1 */
  static QueryContainer queryContainer1;
  /** the test query container 2 */
  static QueryContainer queryContainer2;
  /** the test query container 3 */
  static QueryContainer queryContainer3;
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;

  public ReteEngineTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "InferenceRules",
            System.getenv("REPOSITORIES_TMPFS") + "/InferenceRules");
    String queryString =
            "PREFIX rdf: <" + Constants.RDF_NAMESPACE + ">\n"
            + "PREFIX owl: <" + Constants.OWL_NAMESPACE + ">\n"
            + "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n"
            + "PREFIX texai: <" + Constants.TEXAI_NAMESPACE + ">\n"
            + "\n"
            + "SELECT ?Individual ?Thing\n"
            + "WHERE {\n"
            + "  ?Individual rdf:type ?Thing .\n"
            + "  ?Individual rdf:type texai:FCGClauseSubject .\n"
            + "  ?Individual rdf:type texai:IndefiniteThingInThisDiscourse .\n"
            + "  _:Situation_Localized rdf:type cyc:Situation-Localized .\n"
            + "  _:Situation_Localized cyc:situationConstituents ?Individual .\n"
            + "  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n"
            + "  FILTER (\n"
            + "    !sameTerm(?Thing, texai:FCGClauseSubject) &&\n"
            + "    !sameTerm(?Thing, texai:IndefiniteThingInThisDiscourse))\n"
            + "}";
    queryContainer1 = (new SPARQLParser()).parseQuery(queryString, "test1");
    queryString =
            "  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "  PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\n"
            + "  PREFIX texai: <http://texai.org/texai/>\n"
            + "\n"
            + "  SELECT ?prompter\n"
            + "  WHERE {\n"
            + "    _:Agent3 rdf:type cyc:Agent .\n"
            + "    _:Agent3 texai:fcgDiscourseRole ?Speaker .\n"
            + "    _:Agent3 rdf:type texai:FCGClauseObject .\n"
            + "    _:Assignment-Obligation1 rdf:type cyc:Assignment-Obligation .\n"
            + "    _:Assignment-Obligation1 cyc:allottedAgents ?prompter .\n"
            + "    _:Assignment-Obligation1 cyc:assigner ?Speaker .\n"
            + "    _:Assignment-Obligation1 texai:assignmentPostCondition _:PromptingSomeone2 .\n"
            + "    _:PromptingSomeone2 rdf:type cyc:PromptingSomeone .\n"
            + "    _:PromptingSomeone2 cyc:actionFulfillsAssignment _:Assignment-Obligation1 .\n"
            + "    _:PromptingSomeone2 cyc:communicatorOfInfo ?prompter .\n"
            + "    _:PromptingSomeone2 cyc:recipientOfInfo texai:Agent3 .\n"
            + "    _:PromptingSomeone2 cyc:situationConstituents ?prompter .\n"
            + "    _:PromptingSomeone2 texai:fcgDiscourseRole ?prompter .\n"
            + "    _:PromptingSomeone2 texai:situationHappeningOnDate cyc:Now .\n"
            + "  }";
    queryContainer2 = (new SPARQLParser()).parseQuery(queryString, "test2");
    queryString =
            "  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
            + "  PREFIX cyc: <http://sw.cyc.com/2006/07/27/cyc/>\n"
            + "  PREFIX texai: <http://texai.org/texai/>\n"
            + "\n"
            + "  SELECT ?Agent1 ?Agent4\n"
            + "  WHERE {\n"
            + "    ?Agent1 texai:parentOf ?Agent2 .\n"
            + "    ?Agent2 texai:parentOf ?Agent3 .\n"
            + "    ?Agent3 texai:parentOf ?Agent4 .\n"
            + "  }";
    queryContainer3 = (new SPARQLParser()).parseQuery(queryString, "test3");
    Logger.getLogger(QueryContainerInitializer.class).setLevel(Level.INFO);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    DistributedRepositoryManager.shutDown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of addQueryContainer and addStatement methods, of class ReteEngine.
   */
  @Test
  public void testAddQueryContainer() {
    LOGGER.info("addQueryContainer - in order of conditions");
    ReteEngine instance = new ReteEngine();
    instance.addQueryContainer(queryContainer1);
    assertEquals("[[AlphaMemory rdf:type texai:IndefiniteThingInThisDiscourse], [AlphaMemory cyc:situationConstituents], [AlphaMemory rdf:type], [AlphaMemory rdf:type cyc:Situation-Localized], [AlphaMemory rdf:type texai:FCGClauseSubject], [AlphaMemory texai:situationHappeningOnDate cyc:Now]]", instance.getAlphaMemoryDictionary().values().toString());
    instance.toGraphViz("rete-graph1", null);
    instance.addQueryContainer(queryContainer2);
    assertEquals("[[AlphaMemory texai:fcgDiscourseRole], [AlphaMemory rdf:type cyc:Assignment-Obligation], [AlphaMemory cyc:situationConstituents], [AlphaMemory cyc:actionFulfillsAssignment], [AlphaMemory rdf:type cyc:Agent], [AlphaMemory rdf:type], [AlphaMemory rdf:type cyc:Situation-Localized], [AlphaMemory rdf:type texai:FCGClauseObject], [AlphaMemory cyc:assigner], [AlphaMemory rdf:type texai:IndefiniteThingInThisDiscourse], [AlphaMemory rdf:type cyc:PromptingSomeone], [AlphaMemory cyc:recipientOfInfo texai:Agent3], [AlphaMemory cyc:communicatorOfInfo], [AlphaMemory cyc:allottedAgents], [AlphaMemory texai:assignmentPostCondition], [AlphaMemory rdf:type texai:FCGClauseSubject], [AlphaMemory texai:situationHappeningOnDate cyc:Now]]", instance.getAlphaMemoryDictionary().values().toString());
    instance.toGraphViz("rete-graph2", null);

    LOGGER.info("========================================================================");
    // _:Situation_Localized texai:situationHappeningOnDate cyc:Now
    final URI situationLocalized1 = new URIImpl(Constants.TEXAI_NAMESPACE + "SituationLocalized1");
    final URI situationHappeningOnDate = new URIImpl(Constants.TEXAI_NAMESPACE + "situationHappeningOnDate");
    final URI now = new URIImpl(Constants.CYC_NAMESPACE + "Now");
    final Statement statement1 = new StatementImpl(
            situationLocalized1,
            situationHappeningOnDate,
            now);
    instance.addStatement(statement1);

    // ?Individual rdf:type ?Thing
    final URI buster = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    final URI domesticCat = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    final Statement statement2 = new StatementImpl(
            buster,
            RDF.TYPE,
            domesticCat);
    instance.addStatement(statement2);

    // _:Agent3 rdf:type cyc:Agent
    final URI sam = new URIImpl(Constants.TEXAI_NAMESPACE + "Sam");
    final URI agent = new URIImpl(Constants.CYC_NAMESPACE + "Agent");
    final Statement statement3 = new StatementImpl(
            sam,
            RDF.TYPE,
            agent);
    instance.addStatement(statement3);

    // ?Individual rdf:type texai:FCGClauseSubject
    final URI fcgClauseSubject = new URIImpl(Constants.TEXAI_NAMESPACE + "FCGClauseSubject");
    final Statement statement5 = new StatementImpl(
            buster,
            RDF.TYPE,
            fcgClauseSubject);
    instance.addStatement(statement5);

    // ?Individual rdf:type texai:IndefiniteThingInThisDiscourse
    final URI indefiniteThingInThisDiscourse = new URIImpl(Constants.TEXAI_NAMESPACE + "IndefiniteThingInThisDiscourse");
    final Statement statement6 = new StatementImpl(
            buster,
            RDF.TYPE,
            indefiniteThingInThisDiscourse);
    instance.addStatement(statement6);

    // _:Situation_Localized rdf:type cyc:Situation-Localized
    final URI situationLocalized = new URIImpl(Constants.CYC_NAMESPACE + "Situation-Localized");
    final Statement statement7 = new StatementImpl(
            situationLocalized1,
            RDF.TYPE,
            situationLocalized);
    instance.addStatement(statement7);

    // _:Situation_Localized cyc:situationConstituents ?Individual
    final URI situationConstitutents = new URIImpl(Constants.CYC_NAMESPACE + "situationConstituents");
    final Statement statement8 = new StatementImpl(
            situationLocalized1,
            situationConstitutents,
            buster);
    instance.addStatement(statement8);
    instance.toGraphViz("rete-graph3", null);
    assertEquals("[test1]", instance.getSatisfiedQueryContainerNames().toString());
  }


  /**
   * Test of addQueryContainer and addStatement methods, of class ReteEngine.
   */
  @Test
  public void testAddQueryContainer2() {
    LOGGER.info("addQueryContainer - in reverse order of conditions");
    ReteEngine instance = new ReteEngine();
    instance.addQueryContainer(queryContainer1);
    assertEquals("[[AlphaMemory rdf:type texai:IndefiniteThingInThisDiscourse], [AlphaMemory cyc:situationConstituents], [AlphaMemory rdf:type], [AlphaMemory rdf:type cyc:Situation-Localized], [AlphaMemory rdf:type texai:FCGClauseSubject], [AlphaMemory texai:situationHappeningOnDate cyc:Now]]", instance.getAlphaMemoryDictionary().values().toString());
    instance.toGraphViz("rete-graph1", null);
    instance.addQueryContainer(queryContainer2);
    assertEquals("[[AlphaMemory texai:fcgDiscourseRole], [AlphaMemory rdf:type cyc:Assignment-Obligation], [AlphaMemory cyc:situationConstituents], [AlphaMemory cyc:actionFulfillsAssignment], [AlphaMemory rdf:type cyc:Agent], [AlphaMemory rdf:type], [AlphaMemory rdf:type cyc:Situation-Localized], [AlphaMemory rdf:type texai:FCGClauseObject], [AlphaMemory cyc:assigner], [AlphaMemory rdf:type texai:IndefiniteThingInThisDiscourse], [AlphaMemory rdf:type cyc:PromptingSomeone], [AlphaMemory cyc:recipientOfInfo texai:Agent3], [AlphaMemory cyc:communicatorOfInfo], [AlphaMemory cyc:allottedAgents], [AlphaMemory texai:assignmentPostCondition], [AlphaMemory rdf:type texai:FCGClauseSubject], [AlphaMemory texai:situationHappeningOnDate cyc:Now]]", instance.getAlphaMemoryDictionary().values().toString());
    instance.toGraphViz("rete-graph2", null);

    //Logger.getLogger(ReteEngine.class).setLevel(Level.DEBUG);
    LOGGER.info("========================================================================");
    final URI situationLocalized1 = new URIImpl(Constants.TEXAI_NAMESPACE + "SituationLocalized1");
    final URI situationHappeningOnDate = new URIImpl(Constants.TEXAI_NAMESPACE + "situationHappeningOnDate");
    final URI now = new URIImpl(Constants.CYC_NAMESPACE + "Now");
    final URI situationConstitutents = new URIImpl(Constants.CYC_NAMESPACE + "situationConstituents");
    final URI situationLocalized = new URIImpl(Constants.CYC_NAMESPACE + "Situation-Localized");
    final URI indefiniteThingInThisDiscourse = new URIImpl(Constants.TEXAI_NAMESPACE + "IndefiniteThingInThisDiscourse");
    final URI buster = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    final URI domesticCat = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    final URI sam = new URIImpl(Constants.TEXAI_NAMESPACE + "Sam");
    final URI agent = new URIImpl(Constants.CYC_NAMESPACE + "Agent");

    // _:Agent3 rdf:type cyc:Agent
    final Statement statement3 = new StatementImpl(
            sam,
            RDF.TYPE,
            agent);
    instance.addStatement(statement3);

    // _:Situation_Localized texai:situationHappeningOnDate cyc:Now
    final Statement statement1 = new StatementImpl(
            situationLocalized1,
            situationHappeningOnDate,
            now);
    instance.addStatement(statement1);

    // _:Situation_Localized cyc:situationConstituents ?Individual
    final Statement statement8 = new StatementImpl(
            situationLocalized1,
            situationConstitutents,
            buster);
    instance.addStatement(statement8);

    // _:Situation_Localized rdf:type cyc:Situation-Localized
    final Statement statement7 = new StatementImpl(
            situationLocalized1,
            RDF.TYPE,
            situationLocalized);
    instance.addStatement(statement7);

    // ?Individual rdf:type texai:IndefiniteThingInThisDiscourse
    final Statement statement6 = new StatementImpl(
            buster,
            RDF.TYPE,
            indefiniteThingInThisDiscourse);
    instance.addStatement(statement6);

    // ?Individual rdf:type texai:FCGClauseSubject
    final URI fcgClauseSubject = new URIImpl(Constants.TEXAI_NAMESPACE + "FCGClauseSubject");
    final Statement statement5 = new StatementImpl(
            buster,
            RDF.TYPE,
            fcgClauseSubject);
    instance.addStatement(statement5);

    // beta memory propagation all happens on the last added statement which is the first condition
    instance.toGraphViz("rete-graph4", null);

    // ?Individual rdf:type ?Thing
    final Statement statement2 = new StatementImpl(
            buster,
            RDF.TYPE,
            domesticCat);
    instance.addStatement(statement2);


    instance.toGraphViz("rete-graph5", null);
    assertEquals("[test1]", instance.getSatisfiedQueryContainerNames().toString());

    //instance.getBindings("test1");
  }

  /**
   * Test of addQueryContainer and addStatement methods, of class ReteEngine.
   */
  @Test
  public void testAddQueryContainer3() {
    LOGGER.info("addQueryContainer - in order of conditions to every defined query");
    DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    rdfEntityManager = new RDFEntityManager();
    final QueryContainerInitializer queryContainerInitializer = new QueryContainerInitializer();
    final ReteEngine instance = new ReteEngine();
    queryContainerInitializer.setReteEngine(instance);
    queryContainerInitializer.initialize(
            rdfEntityManager,
            false);  // overrideContext
    queryContainerInitializer.process("../Main/data/bootstrap-queries.xml");
    queryContainerInitializer.finalization();
    instance.addQueryContainer(queryContainer1); // test1 is expected to be satisfied

    LOGGER.info("========================================================================");
    // _:Situation_Localized texai:situationHappeningOnDate cyc:Now
    final URI situationLocalized1 = new URIImpl(Constants.TEXAI_NAMESPACE + "SituationLocalized1");
    final URI situationHappeningOnDate = new URIImpl(Constants.TEXAI_NAMESPACE + "situationHappeningOnDate");
    final URI now = new URIImpl(Constants.CYC_NAMESPACE + "Now");
    final Statement statement1 = new StatementImpl(
            situationLocalized1,
            situationHappeningOnDate,
            now);
    instance.addStatement(statement1);

    // ?Individual rdf:type ?Thing
    final URI buster = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    final URI domesticCat = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    final Statement statement2 = new StatementImpl(
            buster,
            RDF.TYPE,
            domesticCat);
    instance.addStatement(statement2);

    // _:Agent3 rdf:type cyc:Agent
    final URI sam = new URIImpl(Constants.TEXAI_NAMESPACE + "Sam");
    final URI agent = new URIImpl(Constants.CYC_NAMESPACE + "Agent");
    final Statement statement3 = new StatementImpl(
            sam,
            RDF.TYPE,
            agent);
    instance.addStatement(statement3);

    // ?Individual rdf:type texai:FCGClauseSubject
    final URI fcgClauseSubject = new URIImpl(Constants.TEXAI_NAMESPACE + "FCGClauseSubject");
    final Statement statement5 = new StatementImpl(
            buster,
            RDF.TYPE,
            fcgClauseSubject);
    instance.addStatement(statement5);

    // ?Individual rdf:type texai:IndefiniteThingInThisDiscourse
    final URI indefiniteThingInThisDiscourse = new URIImpl(Constants.TEXAI_NAMESPACE + "IndefiniteThingInThisDiscourse");
    final Statement statement6 = new StatementImpl(
            buster,
            RDF.TYPE,
            indefiniteThingInThisDiscourse);
    instance.addStatement(statement6);

    // _:Situation_Localized rdf:type cyc:Situation-Localized
    final URI situationLocalized = new URIImpl(Constants.CYC_NAMESPACE + "Situation-Localized");
    final Statement statement7 = new StatementImpl(
            situationLocalized1,
            RDF.TYPE,
            situationLocalized);
    instance.addStatement(statement7);

    // _:Situation_Localized cyc:situationConstituents ?Individual
    final URI situationConstitutents = new URIImpl(Constants.CYC_NAMESPACE + "situationConstituents");
    final Statement statement8 = new StatementImpl(
            situationLocalized1,
            situationConstitutents,
            buster);
    instance.addStatement(statement8);
    instance.toGraphViz("rete-graph7", null);
    assertEquals("[test1]", instance.getSatisfiedQueryContainerNames().toString());
    instance.toGraphViz("rete-graph7a", "test1");

    instance.reset();
    assertEquals("[]", instance.getSatisfiedQueryContainerNames().toString());
    instance.toGraphViz("rete-graph8", null);

    rdfEntityManager.close();
  }

  /**
   * Test of addQueryContainer and addStatement methods, of class ReteEngine.
   */
  @Test
  public void testAddQueryContainer4() {
    LOGGER.info("addQueryContainer - with a number of similar conditions");
    ReteEngine instance = new ReteEngine();
    instance.addQueryContainer(queryContainer3);
    instance.toGraphViz("rete-graph9", null);

    Logger.getLogger(ReteEngine.class).setLevel(Level.DEBUG);
    final URI alfred = new URIImpl(Constants.TEXAI_NAMESPACE + "Alfred");
    final URI betty = new URIImpl(Constants.TEXAI_NAMESPACE + "Betty");
    final URI cathy = new URIImpl(Constants.TEXAI_NAMESPACE + "Cathy");
    final URI david = new URIImpl(Constants.TEXAI_NAMESPACE + "David");
    final URI parentOf = new URIImpl(Constants.TEXAI_NAMESPACE + "parentOf");
    instance.addStatement(new StatementImpl(
            alfred,
            parentOf,
            betty));
    instance.addStatement(new StatementImpl(
            betty,
            parentOf,
            cathy));
    instance.addStatement(new StatementImpl(
            cathy,
            parentOf,
            david));
    instance.toGraphViz("rete-graph9", null);
    final Map<String, Value> bindingsDictionary = instance.getBindings("test3");
    assertNotNull(bindingsDictionary);
    assertEquals("{?Agent4=http://texai.org/texai/David, ?Agent1=http://texai.org/texai/Alfred}", instance.getBindings("test3").toString());
  }

  /**
   * Test of addQueryContainer and addStatement methods, of class ReteEngine.
   */
  @Test
  public void testBenchmark() {
    LOGGER.info("benchmarking the Rete engine");
    Logger.getLogger(ReteEngine.class).setLevel(Level.WARN);
    DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    rdfEntityManager = new RDFEntityManager();
    final QueryContainerInitializer queryContainerInitializer = new QueryContainerInitializer();
    final ReteEngine instance = new ReteEngine();
    queryContainerInitializer.setReteEngine(instance);
    queryContainerInitializer.initialize(
            rdfEntityManager,
            false);  // overrideContext
    queryContainerInitializer.process("../Main/data/bootstrap-queries.xml");
    queryContainerInitializer.finalization();
    instance.addQueryContainer(queryContainer1); // test1 is expected to be satisfied

    // _:Situation_Localized texai:situationHappeningOnDate cyc:Now
    final URI situationLocalized1 = new URIImpl(Constants.TEXAI_NAMESPACE + "SituationLocalized1");
    final URI situationHappeningOnDate = new URIImpl(Constants.TEXAI_NAMESPACE + "situationHappeningOnDate");
    final URI now = new URIImpl(Constants.CYC_NAMESPACE + "Now");
    final Statement statement1 = new StatementImpl(
            situationLocalized1,
            situationHappeningOnDate,
            now);

    // ?Individual rdf:type ?Thing
    final URI buster = new URIImpl(Constants.TEXAI_NAMESPACE + "Buster");
    final URI domesticCat = new URIImpl(Constants.CYC_NAMESPACE + "DomesticCat");
    final Statement statement2 = new StatementImpl(
            buster,
            RDF.TYPE,
            domesticCat);

    // _:Agent3 rdf:type cyc:Agent
    final URI sam = new URIImpl(Constants.TEXAI_NAMESPACE + "Sam");
    final URI agent = new URIImpl(Constants.CYC_NAMESPACE + "Agent");
    final Statement statement3 = new StatementImpl(
            sam,
            RDF.TYPE,
            agent);

    // ?Individual rdf:type texai:FCGClauseSubject
    final URI fcgClauseSubject = new URIImpl(Constants.TEXAI_NAMESPACE + "FCGClauseSubject");
    final Statement statement5 = new StatementImpl(
            buster,
            RDF.TYPE,
            fcgClauseSubject);

    // ?Individual rdf:type texai:IndefiniteThingInThisDiscourse
    final URI indefiniteThingInThisDiscourse = new URIImpl(Constants.TEXAI_NAMESPACE + "IndefiniteThingInThisDiscourse");
    final Statement statement6 = new StatementImpl(
            buster,
            RDF.TYPE,
            indefiniteThingInThisDiscourse);

    // _:Situation_Localized rdf:type cyc:Situation-Localized
    final URI situationLocalized = new URIImpl(Constants.CYC_NAMESPACE + "Situation-Localized");
    final Statement statement7 = new StatementImpl(
            situationLocalized1,
            RDF.TYPE,
            situationLocalized);

    // _:Situation_Localized cyc:situationConstituents ?Individual
    final URI situationConstitutents = new URIImpl(Constants.CYC_NAMESPACE + "situationConstituents");
    final Statement statement8 = new StatementImpl(
            situationLocalized1,
            situationConstitutents,
            buster);
    final List<Statement> statements = new ArrayList<Statement>();
    statements.add(statement1);
    statements.add(statement2);
    statements.add(statement3);
    statements.add(statement5);
    statements.add(statement6);
    statements.add(statement7);
    statements.add(statement8);

    assertEquals("[]", instance.getSatisfiedQueryContainerNames().toString());
    ReteResults reteResults = instance.executeRete(statements);
    instance.toGraphViz("rete-graph10", null);
    assertEquals("[test1]", instance.getSatisfiedQueryContainerNames().toString());
//    final long iterations = 6000000;
    final long iterations = 600000;
    LOGGER.info("benchmarking " + iterations + " iterations");
    final long startTimeMillis = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      reteResults = instance.executeRete(statements);
      assertEquals("[test1]", instance.getSatisfiedQueryContainerNames().toString());
      assertEquals(1, reteResults.size());
      assertEquals("{?Thing=http://sw.cyc.com/2006/07/27/cyc/DomesticCat, ?Individual=http://texai.org/texai/Buster}", reteResults.getBindingDictionary("test1").toString());
    }
    final long durationMillis = System.currentTimeMillis() - startTimeMillis;
    LOGGER.info("duration " + (durationMillis / 1000L) + " seconds");
    final long iterationsPerSecond = (iterations * 1000L) / durationMillis;
    LOGGER.info("iterations per second " + iterationsPerSecond);
    final long microsecondsPerIteration = (durationMillis * 1000L) / iterations;
    LOGGER.info("iteration duration in microseconds " + microsecondsPerIteration);
    rdfEntityManager.close();
  }

}
