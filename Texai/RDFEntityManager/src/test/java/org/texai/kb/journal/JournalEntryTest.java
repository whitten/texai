/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.kb.journal;

import org.joda.time.DateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.texai.kb.Constants;

/**
 *
 * @author reed
 */
public class JournalEntryTest {

  public JournalEntryTest() {
  }

  /**
   * Test of parse method, of class JournalEntry.
   */
  @Test
  public void testParse() {
    System.out.println("parse");
    String string = "2009-03-18T16:48:55.291Z 1 add cyc:arity rdf:type cyc:BinaryPredicate .";
    JournalEntry result = JournalEntry.parse(string);
    assertTrue(result.toString().equals(string)
            || result.toString().equals("2009-03-18T11:48:55.291-05:00 1 add cyc:arity rdf:type cyc:BinaryPredicate ."));
  }

  /**
   * Test of getDateTime method, of class JournalEntry.
   */
  @Test
  public void testGetDateTime() {
    System.out.println("getDateTime");
    JournalEntry instance = new JournalEntry(new DateTime(), 1, Constants.ADD_OPERATION, new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE)));
    assertNotNull(instance.getDateTime());
  }

  /**
   * Test of getSuffixNbr method, of class JournalEntry.
   */
  @Test
  public void testGetSuffixNbr() {
    System.out.println("getSuffixNbr");
    JournalEntry instance = new JournalEntry(new DateTime(), 1, Constants.ADD_OPERATION, new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE)));
    int expResult = 1;
    int result = instance.getSuffixNbr();
    assertEquals(expResult, result);
  }

  /**
   * Test of getOperation method, of class JournalEntry.
   */
  @Test
  public void testGetOperation() {
    System.out.println("getOperation");
    JournalEntry instance = new JournalEntry(new DateTime(), 1, Constants.ADD_OPERATION, new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE)));
    String expResult = "add";
    String result = instance.getOperation();
    assertEquals(expResult, result);
  }

  /**
   * Test of getStatement method, of class JournalEntry.
   */
  @Test
  public void testGetStatement() {
    System.out.println("getStatement");
    JournalEntry instance = new JournalEntry(new DateTime(), 1, Constants.ADD_OPERATION, new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE)));
    assertNotNull(instance.getStatement().toString());
  }

  /**
   * Test of toString method, of class JournalEntry.
   */
  @Test
  public void testToString() {
    System.out.println("toString");
    JournalEntry instance = new JournalEntry(new DateTime(), 1, Constants.ADD_OPERATION, new StatementImpl(
            new URIImpl(Constants.TERM_ARITY),
            RDF.TYPE,
            new URIImpl(Constants.TERM_BINARY_PREDICATE)));
    assertNotNull(instance.toString());
  }
}
