/*
 * RDFQueries.java
 *
 * Created on Jan 19, 2009, 4:37:31 PM
 *
 * Description: Provides common RDF queries given a query and a list of statements.
 *
 * Copyright (C) Jan 19, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.texai.kb.persistence.RDFUtility;

/** Provides common RDF queries given a query and a list of statements.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class RDFQueries {

  /** Constructs a new RDFQueries instance. */
  private RDFQueries() {
  }

  /** Returns whether statements satisfy the SPARQL query:
   * <code>
   * ?s predicate1 object1  ; predicate2 object2 .
   * </code>
   *
   * @param statements the statements
   * @param predicate1 the first predicate
   * @param object1 the first object
   * @param predicate2 the second predicate
   * @param object2 the second object
   * @return whether statements satisfy the SPARQL query
   */
  public static boolean isTrueAnyS1_P1_O1_P2_O2(
          final List<Statement> statements,
          final URI predicate1,
          final Value object1,
          final URI predicate2,
          final URI object2) {
    //Preconditions
    assert statements != null : "statements must not be null";
    assert predicate1 != null : "predicate1 must not be null";
    assert object1 != null : "object1 must not be null";
    assert predicate2 != null : "predicate2 must not be null";
    assert object2 != null : "object2 must not be null";
    assert !predicate1.equals(predicate2) : "predicate1 " + RDFUtility.formatResource(predicate1) +
            " must be different from predicate2 " + RDFUtility.formatResource(predicate2);

    final Map<Resource, List<Statement>> subjectsDictionary = new HashMap<>();
    final Set<Resource> passOneSubjects = new HashSet<>();
    // pass 1 - find ?s in:
    // ?s predicate1 object1 .
    for (final Statement statement : statements) {
      final Resource subject = statement.getSubject();
      if (statement.getPredicate().equals(predicate1) && statement.getObject().equals(object1)) {
        passOneSubjects.add(subject);
      }
      List<Statement> statementsHavingSubject = subjectsDictionary.get(subject);
      if (statementsHavingSubject == null) {
        statementsHavingSubject = new ArrayList<>();
        subjectsDictionary.put(subject, statementsHavingSubject);
      }
      statementsHavingSubject.add(statement);

    }
    if (subjectsDictionary.isEmpty()) {
      return false;
    }
    // pass 1 - find first:
    // ?s predicate2 object2 .
    for (final Entry<Resource, List<Statement>> entry : subjectsDictionary.entrySet()) {
      final Resource subject = entry.getKey();
      if (passOneSubjects.contains(subject)) {
        for (final Statement statementHavingSubject : entry.setValue(statements)) {
          if (statementHavingSubject.getPredicate().equals(predicate2) && statementHavingSubject.getObject().equals(object2)) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
