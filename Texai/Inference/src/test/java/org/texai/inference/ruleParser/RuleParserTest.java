/*
 * RuleParserTest.java
 * JUnit based test
 *
 * Created on October 29, 2007, 10:38 PM
 */
package org.texai.inference.ruleParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import org.junit.Test;
import junit.framework.TestCase;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.inference.domainEntity.Rule;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;


/**
 *
 * @author reed
 */
public class RuleParserTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RuleParserTest.class.getName());
  /** the RDF entity manager */
  private static RDFEntityManager rdfEntityManager;
  /** the buffered input stream */
  private static BufferedInputStream bufferedInputStream;
  /** the rule parser */
  private static RuleParser ruleParser;

  public RuleParserTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  /**
   * Test of parseInput() method, of class org.texai.fcg.ruleParser.RuleParser.
   */
  @Test
  public void testParseInput() {
    LOGGER.info("parseInput");
    rdfEntityManager = new RDFEntityManager();
    LOGGER.info("oneTimeSetup");
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addRepositoryPath(
            "InferenceRules",
            System.getenv("REPOSITORIES_TMPFS") + "/InferenceRules");
    DistributedRepositoryManager.clearNamedRepository("InferenceRules");
    try {
      final File unitTestRulesPath = new File("data/test-rules-1.rule");
      bufferedInputStream = new BufferedInputStream(new FileInputStream(unitTestRulesPath));
      LOGGER.info("processing input: " + unitTestRulesPath);
    } catch (final FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
    ruleParser = new RuleParser(bufferedInputStream);
    ruleParser.initialize(rdfEntityManager);
    final URI variableURI = new URIImpl(Constants.TEXAI_NAMESPACE + "?test");
    assertEquals("http://texai.org/texai/?test", variableURI.toString());
    assertEquals("?test", RDFUtility.formatURIAsTurtle(variableURI));
    List<Rule> rules;
    try {
      rules = ruleParser.Rules();
      for (final Rule rule : rules) {
        LOGGER.info("rule: " + rule.toString());
        rule.cascadePersist(rdfEntityManager, null);
      }
    } catch (ParseException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
    Iterator<Rule> rules_iter = rdfEntityManager.rdfEntityIterator(
            Rule.class,
            null); // overrideContext
    while (rules_iter.hasNext()) {
      final Rule loadedRule = rules_iter.next();
      assertNotNull(loadedRule);

      //  (
      //  description "if there is a room then it is likely that a table is in the room"
      //  context: texai:InferenceRuleTestContext
      //  if:
      //    ?situation-localized rdf:type cyc:Situation-Localized .
      //    ?room rdf:type cyc:RoomInAConstruction .
      //    ?situation-localized cyc:situationConstituents ?room .
      //  then:
      //    _:in-completely-situation-localized rdf:type texai:InCompletelySituationLocalized .
      //    ?situation-localized texai:likelySubSituations _:in-completely-situation-localized .
      //    _:table rdf:type cyc:Table_PieceOfFurniture .
      //    _:table texai:in-ContCompletely ?room .
      //  )

      assertEquals("(\ndescription \"if there is a room then it is likely that a table is in the room\"\ncontext: texai:InferenceRuleTestContext\nif:\n  ?situation-localized rdf:type cyc:Situation-Localized .\n  ?room rdf:type cyc:RoomInAConstruction .\n  ?situation-localized cyc:situationConstituents ?room .\nthen:\n  _:in-completely-situation-localized rdf:type texai:InCompletelySituationLocalized .\n  ?situation-localized texai:likelySubSituations _:in-completely-situation-localized .\n  _:table rdf:type cyc:Table_PieceOfFurniture .\n  _:table texai:in-ContCompletely ?room .\n)", loadedRule.toString());
      LOGGER.info("loadedRule:\n" + loadedRule);
    }
    CacheManager.getInstance().shutdown();
    try {
      if (bufferedInputStream != null) {
        bufferedInputStream.close();
      }
    } catch (final Exception ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
    }
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
  }
}
