package org.texai.query;

import java.util.List;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.texai.kb.Constants;
import org.texai.turtleStatementParser.ParseException;
import org.texai.turtleStatementParser.TurtleStatementParser;

/**
 *
 * @author reed
 */
public class RDFQueriesTest {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(RDFQueriesTest.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the indicator whether info logging is enabled */
  private static final boolean IS_INFO_LOGGING_ENABLED = LOGGER.isEnabledFor(Level.INFO);

  public RDFQueriesTest() {
  }

  /**
   * Test of isTrueAnyS1_P1_O1_P2_O2 method, of class RDFQueries.
   */
  @Test
  public void testIsTrueAnyS1_P1_O1_P2_O2() {
    System.out.println("isTrueAnyS1_P1_O1_P2_O2");
    String string =
      "<texai:Assignment-Obligation2> <cyc:allottedAgents> <texai:Texai> .\n" +
      "<texai:Assignment-Obligation2> <cyc:assigner> <texai:ConsoleGuestUser> .\n" +
      "<texai:Assignment-Obligation2> <rdf:type> <cyc:Assignment-Obligation> .\n" +
      "<texai:Learning3> <cyc:actionFulfillsAssignment> <texai:Assignment-Obligation2> .\n" +
      "<texai:Learning3> <cyc:situationConstituents> <texai:Texai> .\n" +
      "<texai:Learning3> <cyc:thingComprehended> <texai:ProperCountNoun1_Group> .\n" +
      "<texai:Learning3> <rdf:type> <cyc:Learning> .\n" +
      "<texai:ProperCountNoun1_Group> <cyc:groupMemberType> <cyc:ProperCountNoun> .\n" +
      "<texai:ProperCountNoun1_Group> <rdf:type> <cyc:Group> .";
    TurtleStatementParser turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(string);
    List<Statement> statements = null;
    try {
      statements = turtleStatementParser.Statements();
    } catch (final ParseException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(statements);
    URI predicate1 = RDF.TYPE;
    Value object1 = new URIImpl(Constants.CYC_NAMESPACE + "Assignment-Obligation");
    URI predicate2 = new URIImpl(Constants.CYC_NAMESPACE + "allottedAgents");
    URI object2 = new URIImpl(Constants.TEXAI_NAMESPACE + "Texai");
    boolean result = RDFQueries.isTrueAnyS1_P1_O1_P2_O2(statements, predicate1, object1, predicate2, object2);
    assertEquals(true, result);

    string =
      "<texai:Assignment-Obligation2> <cyc:allottedAgents> <texai:Texai> .\n" +
      "<texai:Assignment-Obligation2> <cyc:assigner> <texai:ConsoleGuestUser> .\n" +
      "<texai:Assignment-Obligation2> <rdf:type> <cyc:Assignment> .\n" +   // will not match
      "<texai:Learning3> <cyc:actionFulfillsAssignment> <texai:Assignment-Obligation2> .\n" +
      "<texai:Learning3> <cyc:situationConstituents> <texai:Texai> .\n" +
      "<texai:Learning3> <cyc:thingComprehended> <texai:ProperCountNoun1_Group> .\n" +
      "<texai:Learning3> <rdf:type> <cyc:Learning> .\n" +
      "<texai:ProperCountNoun1_Group> <cyc:groupMemberType> <cyc:ProperCountNoun> .\n" +
      "<texai:ProperCountNoun1_Group> <rdf:type> <cyc:Group> .";
    turtleStatementParser = TurtleStatementParser.makeTurtleStatementParser(string);
    statements = null;
    try {
      statements = turtleStatementParser.Statements();
    } catch (final ParseException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertNotNull(statements);
    result = RDFQueries.isTrueAnyS1_P1_O1_P2_O2(statements, predicate1, object1, predicate2, object2);
    assertEquals(false, result);
  }
}
