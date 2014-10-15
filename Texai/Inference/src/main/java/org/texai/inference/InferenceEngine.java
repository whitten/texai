/*
 * InferenceEngine.java
 *
 * Created on Feb 3, 2009, 12:58:53 PM
 *
 * Description: Provides an inference engine.
 *
 * Copyright (C) Feb 3, 2009 Stephen L. Reed.
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
package org.texai.inference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.util.TexaiException;

/** Provides an inference engine.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class InferenceEngine {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(InferenceEngine.class);
  /** the indicator whether debug logging is enabled */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /** the in-memory repository used for lightweight lookups */
  private final Repository scratchPadRepository = new SailRepository(new MemoryStore());
  /** the repository connection */
  private final RepositoryConnection scratchPadRepositoryConnection;
  /** the dictionary of prepared queries, query id --> prepared query */
  private final Map<URI, TupleQuery> preparedQueriesDictionary = new HashMap<>();

  /** Constructs a new InferenceEngine instance. */
  public InferenceEngine() {
    try {
      scratchPadRepository.initialize();
      scratchPadRepositoryConnection = scratchPadRepository.getConnection();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Evaluates the given query against the given statements.
   *
   * @param queryContainer the given SPARQL query container
   * @param statements the statements that constitute the knowledge base
   * @return the tuple query result
   */
  public Map<String, Value> evaluateWithSingleResult(
          final QueryContainer queryContainer,
          final List<org.openrdf.model.Statement> statements) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";
    assert statements != null : "statements must not be null";

    final Map<String, Value> variableBindingDictionary = new HashMap<>();
    if (statements.isEmpty()) {
      return variableBindingDictionary;
    }
    final TupleQueryResult tupleQueryResult = evaluate(queryContainer, statements);
    try {
      if (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        for (String variable : bindingSet.getBindingNames()) {
          variableBindingDictionary.put("?" + variable, bindingSet.getBinding(variable).getValue());
        }
      }
      tupleQueryResult.close();
    } catch (final QueryEvaluationException ex) {
      throw new TexaiException(ex);
    }
    return variableBindingDictionary;
  }

  /** Returns whether the query is satisfiable the given statements.
   *
   * @param queryContainer the given SPARQL query container
   * @param statements the statements that constitute the knowledge base
   * @return the tuple query result
   */
  public boolean evaluateTrue(
          final QueryContainer queryContainer,
          final List<org.openrdf.model.Statement> statements) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";
    assert statements != null : "statements must not be null";

    if (statements.isEmpty()) {
      return false;
    }
    final TupleQueryResult tupleQueryResult = evaluate(queryContainer, statements);
    final boolean isSatisfied;
    try {
      isSatisfied = tupleQueryResult.hasNext();
      tupleQueryResult.close();
      return isSatisfied;
    } catch (QueryEvaluationException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Evaluates the given query against the given statements.
   *
   * @param queryContainer the given SPARQL query container
   * @param statements the statements that constitute the knowledge base
   * @return the tuple query result which the caller must close
   */
  public TupleQueryResult evaluate(
          final QueryContainer queryContainer,
          final List<org.openrdf.model.Statement> statements) {
    //Preconditions
    assert queryContainer != null : "query must not be null";
    assert statements != null : "statements must not be null";
    assert !statements.isEmpty() : "statements must not be empty";

    // either retrieve the prepared tuple query from the cache or make it
    TupleQuery tupleQuery = null;
    if (queryContainer.getId() != null) {
      tupleQuery = preparedQueriesDictionary.get(queryContainer.getId());
    }
    if (tupleQuery == null) {
      if (IS_DEBUG_LOGGING_ENABLED) {
        LOGGER.debug("sparql:\n" + queryContainer.toString());
      }
      try {
        tupleQuery = scratchPadRepositoryConnection.prepareTupleQuery(QueryLanguage.SPARQL, queryContainer.toString());
        if (queryContainer.getId() != null) {
          preparedQueriesDictionary.put(queryContainer.getId(), tupleQuery);
        }
      } catch (final RepositoryException | MalformedQueryException ex) {
        throw new TexaiException(ex);
      }
    }

    // perform the query in a critical section to employ a single repository per inference engine instance
    //TODO make a pool of scratchpad repositories
    synchronized (scratchPadRepository) {
      long startTimeMillis = 0;
      try {
        if (IS_DEBUG_LOGGING_ENABLED) {
          startTimeMillis = System.currentTimeMillis();
        }
        scratchPadRepositoryConnection.clear();
        scratchPadRepositoryConnection.add(statements);
        final TupleQueryResult tupleQueryResult = tupleQuery.evaluate();
        if (IS_DEBUG_LOGGING_ENABLED) {
          LOGGER.debug("query duration milliseconds: " + Long.toString(System.currentTimeMillis() - startTimeMillis));
        }
        return tupleQueryResult;
      } catch (final RepositoryException | QueryEvaluationException ex) {
        throw new TexaiException(ex);
      }
    }
  }

  /** Finalizes this object. */
  public void finalization() {
    try {
      scratchPadRepositoryConnection.close();
      scratchPadRepository.shutDown();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the repository connection.
   *
   * @return the repository connection
   */
  protected RepositoryConnection getScratchPadRepositoryConnection() {
    return scratchPadRepositoryConnection;
  }
}
