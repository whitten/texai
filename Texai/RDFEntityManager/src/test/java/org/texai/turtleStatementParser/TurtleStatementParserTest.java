/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.turtleStatementParser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.texai.kb.persistence.RDFUtility;
import org.texai.turtleStatementParser.misc.ParsedTurtleStatementHandler;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class TurtleStatementParserTest extends TestCase {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(TurtleStatementParserTest.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the indicator whether info logging is enabled */
  private static final boolean IS_INFO_LOGGING_ENABLED = LOGGER.isEnabledFor(Level.INFO);
  /** the number of RDF statements in the test file */
  private static int statementsCnt = 0;

  public TurtleStatementParserTest(String testName) {
    super(testName);
  }

  /**
   * Test of makeTurtleStatementParser method, of class TurtleStatementParser.
   */
  public void testMakeTurtleStatementParser1() {
    LOGGER.info("makeTurtleStatementParser1");
    String string =
            "texai:ListAccessor4 texai:listAccessor_Index \"1\"^^<http://www.w3.org/2001/XMLSchema#int> .";
    TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(string);
    List<Statement> statements = null;
    try {
      statements = turtleStatementParser.Statements();
    } catch (final ParseException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(statements);
    assertFalse(statements.isEmpty());
    assertEquals("[(http://texai.org/texai/ListAccessor4, http://texai.org/texai/listAccessor_Index, \"1\"^^<http://www.w3.org/2001/XMLSchema#int>)]", StringUtils.toSortedStrings(statements).toString());

    string =
            "_:Learning rdf:type cyc:Learning .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(string);
    statements = null;
    try {
      statements = turtleStatementParser.Statements();
    } catch (final ParseException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(statements);
    assertEquals("[(_:Learning, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://sw.cyc.com/2006/07/27/cyc/Learning)]", statements.toString());

    string =
            "texai:org.texai.inference.domainEntity.Statement_8ded04c4-b019-438f-96b0-e77f197434c0 texai:infStatementObject ?prompter in texai:EnglishConstructionGrammarDomainContext .\n"
            + "texai:org.texai.fcg.domainEntity.MorphologicalRule_b2ab20c1-e016-4f9b-a51e-3a9c13bb10c5 texai:fcgMorphologicalRuleUsageCount \"0\"^^<http://www.w3.org/2001/XMLSchema#long> in texai:EnglishConstructionGrammarDomainContext .\n"
            + "texai:org.texai.fcg.domainEntity.LexicalStemRule_92d8420a-782b-44a2-97cc-01905992037f texai:fcgRuleApplicationDirection \"3\"^^<http://www.w3.org/2001/XMLSchema#int> in texai:EnglishConstructionGrammarDomainContext .\n"
            + "texai:org.texai.fcg.domainEntity.UnitNameVariable_3bb01d05-1a05-42c5-b81d-49424c4ae29e texai:domainEntityClassName \"org.texai.fcg.domainEntity.UnitNameVariable\" in cyc:UniversalVocabularyMt .\n"
            + "texai:org.texai.fcg.domainEntity.LexicalStemRule_e7ac8af6-d3d9-4ea0-871f-3a19c49b105a texai:fcgRuleWeight \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double> in texai:EnglishConstructionGrammarDomainContext .\n"
            + "texai:org.texai.actr.domainEntity.ChunkType rdf:type cyc:ObjectType in cyc:UniversalVocabularyMt .\n"
            + "texai:Assignment-Obligation2 cyc:allottedAgents texai:Texai .\n"
            + "texai:Assignment-Obligation2 cyc:assigner texai:ConsoleGuestUser .\n"
            + "texai:Assignment-Obligation2 rdf:type cyc:Assignment-Obligation .\n"
            + "_:Learning cyc:actionFulfillsAssignment texai:Assignment-Obligation2 .\n"
            + "_:Learning cyc:situationConstituents texai:Texai .\n"
            + "_:Learning cyc:thingComprehended texai:ProperCountNoun1_Group .\n"
            + "_:Learning rdf:type cyc:Learning .\n"
            + "texai:ProperCountNoun1_Group cyc:groupMemberType cyc:ProperCountNoun .\n"
            + "texai:ProperCountNoun1_Group rdf:type cyc:Group .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(string);
    statements = turtleStatementParser.getStatements();
    assertNotNull(statements);
    for (final Statement statement : RDFUtility.sortStatements(statements)) {
      LOGGER.info("  " + RDFUtility.formatStatementAsTurtle(statement));
    }
    assertEquals(15, statements.size());
    assertEquals("[(http://texai.org/texai/org.texai.inference.domainEntity.Statement_8ded04c4-b019-438f-96b0-e77f197434c0, http://texai.org/texai/infStatementObject, http://texai.org/texai/?prompter) [http://texai.org/texai/EnglishConstructionGrammarDomainContext], (http://texai.org/texai/org.texai.fcg.domainEntity.MorphologicalRule_b2ab20c1-e016-4f9b-a51e-3a9c13bb10c5, http://texai.org/texai/fcgMorphologicalRuleUsageCount, \"0\"^^<http://www.w3.org/2001/XMLSchema#long>) [http://texai.org/texai/EnglishConstructionGrammarDomainContext], (http://texai.org/texai/org.texai.fcg.domainEntity.LexicalStemRule_92d8420a-782b-44a2-97cc-01905992037f, http://texai.org/texai/fcgRuleApplicationDirection, \"3\"^^<http://www.w3.org/2001/XMLSchema#int>) [http://texai.org/texai/EnglishConstructionGrammarDomainContext], (http://texai.org/texai/org.texai.fcg.domainEntity.UnitNameVariable_3bb01d05-1a05-42c5-b81d-49424c4ae29e, http://texai.org/texai/domainEntityClassName, \"org.texai.fcg.domainEntity.UnitNameVariable\") [http://sw.cyc.com/2006/07/27/cyc/UniversalVocabularyMt], (http://texai.org/texai/org.texai.fcg.domainEntity.LexicalStemRule_e7ac8af6-d3d9-4ea0-871f-3a19c49b105a, http://texai.org/texai/fcgRuleWeight, \"1.0\"^^<http://www.w3.org/2001/XMLSchema#double>) [http://texai.org/texai/EnglishConstructionGrammarDomainContext], (http://texai.org/texai/org.texai.actr.domainEntity.ChunkType, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://sw.cyc.com/2006/07/27/cyc/ObjectType) [http://sw.cyc.com/2006/07/27/cyc/UniversalVocabularyMt], (http://texai.org/texai/Assignment-Obligation2, http://sw.cyc.com/2006/07/27/cyc/allottedAgents, http://texai.org/texai/Texai), (http://texai.org/texai/Assignment-Obligation2, http://sw.cyc.com/2006/07/27/cyc/assigner, http://texai.org/texai/ConsoleGuestUser), (http://texai.org/texai/Assignment-Obligation2, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://sw.cyc.com/2006/07/27/cyc/Assignment-Obligation), (_:Learning, http://sw.cyc.com/2006/07/27/cyc/actionFulfillsAssignment, http://texai.org/texai/Assignment-Obligation2), (_:Learning, http://sw.cyc.com/2006/07/27/cyc/situationConstituents, http://texai.org/texai/Texai), (_:Learning, http://sw.cyc.com/2006/07/27/cyc/thingComprehended, http://texai.org/texai/ProperCountNoun1_Group), (_:Learning, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://sw.cyc.com/2006/07/27/cyc/Learning), (http://texai.org/texai/ProperCountNoun1_Group, http://sw.cyc.com/2006/07/27/cyc/groupMemberType, http://sw.cyc.com/2006/07/27/cyc/ProperCountNoun), (http://texai.org/texai/ProperCountNoun1_Group, http://www.w3.org/1999/02/22-rdf-syntax-ns#type, http://sw.cyc.com/2006/07/27/cyc/Group)]", statements.toString());
  }

  /**
   * Test of makeTurtleStatementParser method, of class TurtleStatementParser.
   */
  public void testMakeTurtleStatementParser2() {
    LOGGER.info("makeTurtleStatementParser2");

    final BufferedInputStream inputStream;
    try {
      inputStream = new BufferedInputStream(new FileInputStream("data/parsing-test.turtle"));
      final ParsedTurtleStatementHandler parsedTurtleStatementHandler = new MyParsedTurtleStatementHandler();
      TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(inputStream, parsedTurtleStatementHandler);
      turtleStatementParser.getStatements();
      assertEquals(5, statementsCnt);
    } catch (IOException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }

  class MyParsedTurtleStatementHandler implements ParsedTurtleStatementHandler {

  /** Handles a parsed turtle statement.
   *
   * @param statement the statement
   */
    @Override
    public void handleStatement(final Statement statement) {
      LOGGER.info("statement: " + RDFUtility.formatStatementAsTurtle(statement));
      statementsCnt++;
    }

  }
}
