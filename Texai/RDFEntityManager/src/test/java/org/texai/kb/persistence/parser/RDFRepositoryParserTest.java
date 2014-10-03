/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.persistence.parser;

import java.util.Set;
import net.sf.ehcache.CacheManager;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.domainEntity.RepositoryContentDescription;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class RDFRepositoryParserTest {

  public RDFRepositoryParserTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    CacheInitializer.initializeCaches();
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

  /**
   * Test of makeRuleParser method, of class RDFRepositoryParser.
   */
  @Test
  public void testMakeRDFRepositoryParser() {
    System.out.println("makeRDFRepositoryParser");
    String string =
            "((AmericanEnglishConstructionAndLexicalCategoryRules\n" +
            "  (indices: \"spoc,posc,opsc\")\n" +
            "  (className: org.texai.fcg.domainEntity.ConstructionRule)\n" +
            "  (className: org.texai.fcg.domainEntity.ConstructionRuleGenerationIndexEntry)\n" +
            "  (className: org.texai.fcg.domainEntity.ConstructionRuleIndexEntry)\n" +
            "  (className: org.texai.fcg.domainEntity.LexicalStemRule)\n" +
            "  (className: org.texai.fcg.domainEntity.LexicalStemRuleGenerationIndexEntry)\n" +
            "  (className: org.texai.fcg.domainEntity.LexicalStemRuleParsingIndexEntry))\n" +
            " (AmericanEnglishGrammarUnitTestSpecifications\n" +
            "  (indices: \"spoc,posc\")\n" +
            "  (className: org.texai.fcg.domainEntity.GrammarRuleUnitTestSpecification)))";
    RepositoryContentDescriptionParser result = RepositoryContentDescriptionParser.makeRepositoryContentDescriptionParser(string);
    try {
      final Set<RepositoryContentDescription> rdfRepositories = result.parseInput();
      assertEquals("[[AmericanEnglishConstructionAndLexicalCategoryRules], [AmericanEnglishGrammarUnitTestSpecifications]]", StringUtils.toSortedStrings(rdfRepositories).toString());
      assertEquals(
              "((AmericanEnglishConstructionAndLexicalCategoryRules\n" +
              "  (indices: \"spoc,posc,opsc\")\n" +
              "  (className: org.texai.fcg.domainEntity.ConstructionRule)\n" +
              "  (className: org.texai.fcg.domainEntity.ConstructionRuleGenerationIndexEntry)\n" +
              "  (className: org.texai.fcg.domainEntity.ConstructionRuleIndexEntry)\n" +
              "  (className: org.texai.fcg.domainEntity.LexicalStemRule)\n" +
              "  (className: org.texai.fcg.domainEntity.LexicalStemRuleGenerationIndexEntry)\n" +
              "  (className: org.texai.fcg.domainEntity.LexicalStemRuleParsingIndexEntry)\n" +
              " (AmericanEnglishGrammarUnitTestSpecifications\n" +
              "  (indices: \"spoc,posc\")\n" +
              "  (className: org.texai.fcg.domainEntity.GrammarRuleUnitTestSpecification))",
              RepositoryContentDescription.toString(rdfRepositories));
    } catch (final ParseException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }
}
