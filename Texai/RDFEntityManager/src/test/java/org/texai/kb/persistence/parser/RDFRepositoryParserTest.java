/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.persistence.parser;

import java.util.Set;
import junit.framework.TestCase;
import org.texai.kb.persistence.domainEntity.RepositoryContentDescription;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class RDFRepositoryParserTest extends TestCase {

  public RDFRepositoryParserTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test of makeRuleParser method, of class RDFRepositoryParser.
   */
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
    } catch (final Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }
}
