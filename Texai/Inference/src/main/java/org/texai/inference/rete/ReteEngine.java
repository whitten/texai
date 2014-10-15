/*
 * ReteEngine.java
 *
 * Created on Aug 12, 2010, 11:20:32 AM
 *
 * Description: Provides a RETE rule matching engine.
 *
 * Copyright (C) Aug 12, 2010, Stephen L. Reed.
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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.texai.inference.rete.TestAtJoinNode.FieldType;
import org.texai.inference.sparql.domainEntity.AbstractOperator;
import org.texai.inference.sparql.domainEntity.AndOperator;
import org.texai.inference.sparql.domainEntity.Constraint;
import org.texai.inference.sparql.domainEntity.NotOperator;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.inference.sparql.domainEntity.SameTermOperator;
import org.texai.inference.sparql.domainEntity.SelectQuery;
import org.texai.inference.sparql.domainEntity.Variable;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides a Rete rule matching engine.  See http://www.cis.temple.edu/~ingargio/cis587/readings/rete.html for
 * a description of the Rete Algorithm.
 *
 * @author reed
 */
@NotThreadSafe
public class ReteEngine {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(ReteEngine.class);
  /** the alpha memory dictionary, pattern --> alpha memories */
  private final Map<String, AlphaMemory> alphaMemoryDictionary = new HashMap<>();
  /** the nodes dictionary, query container name --> nodes */
  private final Map<String, List<AbstractReteNode>> nodeDictionary = new HashMap<>();
  /** the current query container name */
  private String queryContainerName;
  /** the query container names */
  private final Set<String> queryContainerNames = new HashSet<>();
  /** the satisfied production node dictionary, query container name --> satisfied production node */
  private final Map<String, ProductionNode> satisfiedProductionNodeDictionary = new HashMap<>();
  /** the set of mutated alpha memories */
  private final Set<AlphaMemory> mutatedAlphaMemories = new HashSet<>();
  /** the set of mutated beta memories and mutated production nodes */
  private final Set<TokenMemory> mutatedTokenMemories = new HashSet<>();
  /** the alpha memory label serial number for graphing */
  private int alphaMemoryLabelSerialNbr = 0;
  /** the join label serial number for graphing */
  private int joinLabelSerialNumber = 0;
  /** the beta memory label serial number for graphing */
  private int betaMemoryLabelSerialNumber = 0;
  /** the dummy top label serial number for graphing */
  private int dummyTopLabelSerialNumber = 0;
  /** the node id for graphing */
  private int nodeId = 0;
  /** the statements */
  private Set<org.openrdf.model.Statement> addedStatements = new HashSet<>();
  /** the scored query container informations */
  private final List<ScoredQueryContainerInfo> scoredQueryContainerInfos = new ArrayList<>();

  /** Constructs a new ReteEngine instance. */
  public ReteEngine() {
  }

  /** Executes the Rete engine in a thread safe manner, using the given statements to return any satisfied queries and their
   * respective bindings.
   *
   * @param statements the given statements
   * @return the Rete results
   */
  public synchronized ReteResults executeRete(final Set<org.openrdf.model.Statement> statements) {
    //Preconditions
    assert statements != null : "statements must not be null";

    return executeRete(new ArrayList<>(statements));
  }

  /** Executes the Rete engine in a thread safe manner, using the given statements to return any satisfied queries and their
   * respective bindings.
   *
   * @param statements the given statements
   * @return the Rete results
   */
  public synchronized ReteResults executeRete(final List<org.openrdf.model.Statement> statements) {
    //Preconditions
    assert statements != null : "statements must not be null";

    final Map<String, Map<String, Value>> resultsDictionary = new HashMap<>();
    reset();
    for (final org.openrdf.model.Statement statement : statements) {
      addStatement(statement);
    }
    for (final ProductionNode productionNode : satisfiedProductionNodeDictionary.values()) {
      resultsDictionary.put(productionNode.getQueryContainer().getName(), getBindings(productionNode));
    }
    return new ReteResults(resultsDictionary);
  }

  /** Resets the Rete network by clearing the mutated alpha and beta memories from the last execution. */
  public void reset() {
    satisfiedProductionNodeDictionary.clear();
    for (final AlphaMemory alphaMemory : mutatedAlphaMemories) {
      alphaMemory.getStatements().clear();
    }
    mutatedAlphaMemories.clear();
    for (final TokenMemory mutatedTokenMemory : mutatedTokenMemories) {
      mutatedTokenMemory.getTokens().clear();
    }
    mutatedTokenMemories.clear();
    addedStatements.clear();
  }

  /** Adds the given query to the rete network.
   *
   * @param queryContainer the query container
   */
  public void addQueryContainer(final QueryContainer queryContainer) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";
    assert queryContainer.getQuery() instanceof SelectQuery : "query must be a select query";

    queryContainerName = queryContainer.getName();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("adding query container: " + queryContainerName);
    }
    queryContainerNames.add(queryContainerName);
    final Map<URI, Set<Value>> notSameTermAsDictionary = notSameTermAsDictionary(queryContainer);

    final List<org.texai.inference.domainEntity.Statement> conditions = queryContainer.getQuery().getWhereClause().getStatements();
    assert !conditions.isEmpty();
    final DummyTopNode dummyTopNode = new DummyTopNode();
    addNodeToDictionary(dummyTopNode);
    AbstractReteNode currentNode = dummyTopNode;
    final org.texai.inference.domainEntity.Statement firstCondition = conditions.get(0);
    final List<org.texai.inference.domainEntity.Statement> earlierConditions = new ArrayList<>();
    List<TestAtJoinNode> tests = getJoinTestsFromCondition(firstCondition, earlierConditions);
    earlierConditions.add(firstCondition);
    AlphaMemory alphaMemory = buildOrShareAlphaMemory(firstCondition, notSameTermAsDictionary);
    currentNode = buildOrShareJoinNode(currentNode, alphaMemory, tests, firstCondition);

    final int conditions_size = conditions.size();
    for (int i = 1; i < conditions_size; i++) {
      final org.texai.inference.domainEntity.Statement condition = conditions.get(i);
      // get the beta memory node for the condition
      currentNode = buildOrShareBetaMemoryNode(currentNode);
      // get the join node for the condition
      tests = getJoinTestsFromCondition(condition, earlierConditions);
      earlierConditions.add(condition);
      alphaMemory = buildOrShareAlphaMemory(condition, notSameTermAsDictionary);
      currentNode = buildOrShareJoinNode(currentNode, alphaMemory, tests, condition);
    }
    final ProductionNode productionNode = new ProductionNode(currentNode, queryContainer);
    currentNode.getChildren().add(productionNode);
    addNodeToDictionary(productionNode);
    updateNewNodeWithMatchesFromAbove(productionNode);
  }

  /** Returns the non-sameAsTerm dictionary for the given query container, variable URI --> not same-as term URIs.
   *
   * @param queryContainer the given query container
   * @return the non-sameAsTerm dictionary, variable URI --> not same-as term URIs
   */
  private Map<URI, Set<Value>> notSameTermAsDictionary(final QueryContainer queryContainer) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";
    assert queryContainer.getQuery() instanceof SelectQuery : "query must be a select query";
    assert queryContainer.getQuery().getWhereClause() != null : "query must have a where clause";

    final Map<URI, Set<Value>> notSameTermAsDictionary = new HashMap<>();
    final Constraint constraint = queryContainer.getQuery().getWhereClause().getConstraint();
    if (constraint != null) {
      populateNotSameTermAsDictionary(notSameTermAsDictionary, constraint.getOperator());
    }
    return notSameTermAsDictionary;
  }

  /** Recursively populates the not same-term as dictionary.
   *
   * @param notSameTermAsDictionary the non-sameAsTerm dictionary, variable URI --> not same-as term URIs
   * @param operator the where clause constraint operator
   */
  private void populateNotSameTermAsDictionary(
          final Map<URI, Set<Value>> notSameTermAsDictionary,
          final AbstractOperator operator) {
    //Preconditions
    assert notSameTermAsDictionary != null : "notSameTermAsDictionary must not be null";
    assert operator != null : "operator must not be null";

    if (operator instanceof NotOperator) {
      final NotOperator notOperator = (NotOperator) operator;
      if (notOperator.getArg() instanceof SameTermOperator) {
        final SameTermOperator sameTermOperator = (SameTermOperator) notOperator.getArg();
        final Value term1 = sameTermOperator.getTerm1();
        final Value term2 = sameTermOperator.getTerm2();
        if (RDFUtility.isVariableURI(term1) && !RDFUtility.isVariableURI(term2)) {
          Set<Value> notSameTerms = notSameTermAsDictionary.get((URI) term1);
          if (notSameTerms == null) {
            notSameTerms = new HashSet<>();
            notSameTermAsDictionary.put((URI) term1, notSameTerms);
          }
          notSameTerms.add(term2);
        } else if (!RDFUtility.isVariableURI(term1) && RDFUtility.isVariableURI(term2)) {
          Set<Value> notSameTerms = notSameTermAsDictionary.get((URI) term2);
          if (notSameTerms == null) {
            notSameTerms = new HashSet<>();
            notSameTermAsDictionary.put((URI) term2, notSameTerms);
          }
          notSameTerms.add(term1);
        }
      }
    }
    if (operator instanceof AndOperator) {
      final AndOperator andOperator = (AndOperator) operator;
      populateNotSameTermAsDictionary(notSameTermAsDictionary, andOperator.getArg1());
      populateNotSameTermAsDictionary(notSameTermAsDictionary, andOperator.getArg2());
    }
  }

  /** Adds an RDF statement to the Rete network.
   *
   * @param statement the given RDF statement
   */
  public void addStatement(final org.openrdf.model.Statement statement) {
    //Preconditions
    assert statement != null : "statement must not be null";

    if (addedStatements.contains(statement)) {
      LOGGER.info("ignoring duplicate statement: " + RDFUtility.formatStatement(statement));
      return;
    }
    addedStatements.add(statement);
    boolean isAlphaMemoryActivated = false;
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("adding statement: " + RDFUtility.formatStatement(statement));
    }

    // try predicate/object pattern
    final URI predicate = statement.getPredicate();
    final String pattern = derivePattern(predicate, statement.getObject());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  trial predicate/object pattern: '" + pattern + "'");
    }
    AlphaMemory alphaMemory = alphaMemoryDictionary.get(pattern);
    if (alphaMemory != null) {
      isAlphaMemoryActivated = true;
      alphaMemoryActivation(alphaMemory, statement);
    }

    // try predicate pattern
    final String predicatePattern = RDFUtility.formatResource(predicate);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  predicate pattern: '" + predicatePattern + "'");
    }
    alphaMemory = alphaMemoryDictionary.get(predicatePattern);
    if (alphaMemory != null) {
      if (alphaMemory.getNotSameTerms().contains(statement.getObject())) {
        LOGGER.debug("    bypassing due to not same-as terms: " + alphaMemory.getNotSameTerms());
      } else {
        isAlphaMemoryActivated = true;
        alphaMemoryActivation(alphaMemory, statement);
      }
    }

    if (!isAlphaMemoryActivated) {
      if (LOGGER.isDebugEnabled()) {
        for (final String pattern1 : alphaMemoryDictionary.keySet()) {
          LOGGER.debug("'" + pattern1 + "'");
        }
      }
      LOGGER.info("no pattern matches statement: " + statement);
    }
  }

  /** Activates the given alpha memory with the given statement.
   *
   * @param alphaMemory the given alpha memory
   * @param statement the given statement
   */
  private void alphaMemoryActivation(
          final AlphaMemory alphaMemory,
          final org.openrdf.model.Statement statement) {
    //Preconditions
    assert alphaMemory != null : "alphaMemory must not be null";
    assert statement != null : "statement must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("alphaMemoryActivation " + alphaMemory);
    }
    if (alphaMemory.getStatements().isEmpty()) {
      mutatedAlphaMemories.add(alphaMemory);
    }
    alphaMemory.getStatements().addFirst(statement);
    LOGGER.debug("  statements: " + RDFUtility.formatStatements(alphaMemory.getStatements()));
    for (final JoinNode joinNode : alphaMemory.getSuccessors()) {
      joinNodeRightActivation(joinNode, statement);
    }
  }

  /** Performs activation of the given join node when a new statement is added to the corresponding alpha memory.
   *
   * @param joinNode the given join node
   * @param statement the new statement
   */
  private void joinNodeRightActivation(
          final JoinNode joinNode,
          final org.openrdf.model.Statement statement) {
    //Preconditions
    assert joinNode != null : "joinNode must not be null";
    assert statement != null : "statement must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("joinNodeRightActivation " + joinNode);
    }
    final List<Token> tokens;
    if (joinNode.getParent() instanceof DummyTopNode) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  DummyTopNode");
      }
      tokens = new ArrayList<>();
      tokens.add(new Token());
    } else {
      final BetaMemoryNode betaMemoryNode = (BetaMemoryNode) joinNode.getParent();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  parent betaMemoryNode " + betaMemoryNode);
      }
      tokens = betaMemoryNode.getTokens();
    }
    for (final Token token : tokens) {
      if (performJoinTests(joinNode.getTests(), token, statement)) {
        for (final AbstractReteNode childNode : joinNode.getChildren()) {
          if (childNode instanceof TokenMemory) {
            betaMemoryLeftActivation((TokenMemory) childNode, token, statement, joinNode.getCondition());
            if (childNode instanceof ProductionNode) {
              // done!
              satisfiedProductionNode((ProductionNode) childNode);
            }
          }
        }
      }
    }
  }

  /** Performs left activation for the given join node.
   *
   * @param joinNode the given join node
   * @param token the token
   */
  private void joinNodeLeftActivation(
          final JoinNode joinNode,
          final Token token) {
    //Preconditions
    assert joinNode != null : "joinNode must not be null";
    assert token != null : "token must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("joinNodeLeftActivation " + joinNode);
    }
    for (final org.openrdf.model.Statement statement : joinNode.getAlphaMemory().getStatements()) {
      if (performJoinTests(joinNode.getTests(), token, statement)) {
        for (final AbstractReteNode childNode : joinNode.getChildren()) {
          if (childNode instanceof TokenMemory) {
            betaMemoryLeftActivation((TokenMemory) childNode, token, statement, joinNode.getCondition());
            if (childNode instanceof ProductionNode) {
              // done!
              satisfiedProductionNode((ProductionNode) childNode);
            }
          }
        }
      }
    }
  }

  /** Records that the given production node has been satisfied.
   *
   * @param productionNode the given production node
   */
  private void satisfiedProductionNode(final ProductionNode productionNode) {
    //Preconditions
    assert productionNode != null : "productionNode must not be null";
    assert !productionNode.getTokens().isEmpty() : "productionNode must have at least one satisfying token";

    final String satisfiedQueryContainerName = productionNode.getQueryContainer().getName();
    satisfiedProductionNodeDictionary.put(satisfiedQueryContainerName, productionNode);
    LOGGER.debug(satisfiedQueryContainerName + " satisfied the query");

    // mark the satisfying tokens for illustration in a graph
    Token token = productionNode.getTokens().get(0);
    while (true) {
      token.setIsMemberOfSatisfactionSet(true);
      if (token.getParent() == null) {
        break;
      }
      token = token.getParent();
    }
  }

  /** Performs join tests.
   *
   * @param tests the join tests
   * @param token the token
   * @param statement the statement
   * @return whether tests were satisfied
   */
  private boolean performJoinTests(
          final List<TestAtJoinNode> tests,
          final Token token,
          final org.openrdf.model.Statement statement) {
    //Preconditions
    assert tests != null : "tests must not be null";
    assert token != null : "token must not be null";
    assert statement != null : "statement must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("performJoinTests " + token);
    }
    if (token.isDummy()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  token is dummy");
      }
      return true;
    }
    for (final TestAtJoinNode test : tests) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  " + test);
      }
      final Value arg1;
      if (test.getArg1FieldType().equals(FieldType.SUBJECT)) {
        arg1 = statement.getSubject();
      } else {
        arg1 = statement.getObject();
      }
      if (LOGGER.isDebugEnabled()) {
        if (arg1 instanceof Resource) {
          LOGGER.debug("    arg1: " + RDFUtility.formatResource((Resource) arg1));
        } else {
          LOGGER.debug("    arg1: " + arg1);
        }
      }

      logTokenList(token);

      // search up the token list by the specified number of levels
      int level = 1;
      final int nbrOfLevelsUp = test.getNbrLevelsUp();
      Token currentToken = token;
      while (true) {
        if (level >= nbrOfLevelsUp) {
          break;
        }
        currentToken = currentToken.getParent();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("    current token: " + currentToken);
        }
        assert currentToken != null;
        level++;
      }
      if (currentToken.isDummy()) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("    current token is dummy");
        }
        return true;
      }
      final org.openrdf.model.Statement statement2 = currentToken.getStatement();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("    arg2 statement " + RDFUtility.formatStatement(statement2));
        LOGGER.debug("      test condition " + test.getCondition());
      }
      final Value arg2;
      if (test.getArg2FieldType().equals(FieldType.SUBJECT)) {
        arg2 = statement2.getSubject();
      } else {
        arg2 = statement2.getObject();
      }
      if (LOGGER.isDebugEnabled()) {
        if (arg2 instanceof Resource) {
          LOGGER.debug("    arg2: " + RDFUtility.formatResource((Resource) arg2));
        } else {
          LOGGER.debug("    arg2: " + arg1);
        }
      }
      if (!arg1.equals(arg2)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("    arg1 not equal arg2 - join tests failed");
        }
        return false;
      }
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  all tested true");
    }
    return true;
  }

  /** Performs beta memory left activation.
   *
   * @param tokenMemory the beta memory node or production node
   * @param token the token
   * @param statement the statement
   * @param condition the join condition
   */
  private void betaMemoryLeftActivation(
          final TokenMemory tokenMemory,
          final Token token,
          final org.openrdf.model.Statement statement,
          final org.texai.inference.domainEntity.Statement condition) {
    //Preconditions
    assert tokenMemory != null : "betaMemoryNode must not be null";
    assert token != null : "token must not be null";
    assert statement != null : "statement must not be null";
    assert condition != null : "condition must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("betaMemoryLeftActivation, token: " + token + ", statement: " + RDFUtility.formatStatement(statement));
    }
    final String objectVariableName;
    if (isVariableName(condition.getObject())) {
      objectVariableName = getVariableName((Resource) condition.getObject());
    } else {
      objectVariableName = null;
    }
    final Token newToken = new Token(
            token,
            statement,
            getVariableName(condition.getSubject()), // subjectVariableName
            objectVariableName);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  token:    " + token);
      LOGGER.debug("  newToken: " + newToken);
    }
    if (tokenMemory.getTokens().contains(token)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  redundant token");
      }
      return;
    }
    logTokenList(newToken);
    if (tokenMemory.getTokens().isEmpty()) {
      mutatedTokenMemories.add(tokenMemory);
    }
    tokenMemory.getTokens().addFirst(newToken);
    if (tokenMemory instanceof BetaMemoryNode) {
      for (final AbstractReteNode childNode : ((BetaMemoryNode) tokenMemory).getChildren()) {
        joinNodeLeftActivation((JoinNode) childNode, newToken);
      }
    }
  }

  /** Gets join tests from the given condition and the given earlier conditions.
   *
   * @param condition the given condition
   * @param earlierConditions the given earlier conditions
   * @return the join tests
   */
  private List<TestAtJoinNode> getJoinTestsFromCondition(
          final org.texai.inference.domainEntity.Statement condition,
          final List<org.texai.inference.domainEntity.Statement> earlierConditions) {
    //Preconditions
    assert condition != null : "condition must not be null";
    assert earlierConditions != null : "earlierConditions must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("getJoinTestsFromCondition, condition: " + condition + ", earlierConditions: " + earlierConditions);
    }
    final List<TestAtJoinNode> tests = new ArrayList<>();

    // the subject of the given condition is always a variable
    String variableName = getVariableName(condition.getSubject());
    assert variableName != null;
    TestAtJoinNode test = findTestForVariable(FieldType.SUBJECT, variableName, earlierConditions);
    if (test != null) {
      tests.add(test);
    }

    if (isVariableName(condition.getObject())) {
      // the object of the given condition is a variable
      variableName = getVariableName((Resource) condition.getObject());
      final TestAtJoinNode testAtJoinNode = findTestForVariable(FieldType.OBJECT, variableName, earlierConditions);
      if (testAtJoinNode != null) {
        tests.add(testAtJoinNode);
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  tests: " + tests);
    }
    return tests;
  }

  /** Returns a join test for the given variable name if it can be found in the earlier conditions.
   *
   * @param arg1FieldType the argument 1 field type, i.e. FieldType.SUBJECT or FieldType.OBJECT
   * @param joinVariableName the given variable name to join
   * @param earlierConditions the earlier conditions
   * @return a join test for the given variable name if it can be found in the earlier conditions, otherwise returns null
   */
  private TestAtJoinNode findTestForVariable(
          final FieldType arg1FieldType,
          final String joinVariableName,
          final List<org.texai.inference.domainEntity.Statement> earlierConditions) {
    //Preconditions
    assert arg1FieldType != null : "arg1FieldType must not be null";
    assert joinVariableName != null : "joinVariableName must not be null";
    assert !joinVariableName.isEmpty() : "joinVariableName must not be empty";
    assert earlierConditions != null : "earlierConditions must not be null";

    final int earlierConditions_size = earlierConditions.size();
    for (int i = earlierConditions_size - 1; i >= 0; i--) {
      final org.texai.inference.domainEntity.Statement earlierCondition = earlierConditions.get(i);
      final int nbrOfLevelsUp = earlierConditions_size - i;
      // the subject of the earlier condition is always a variable
      String variableName = getVariableName(earlierCondition.getSubject());
      if (variableName.equals(joinVariableName)) {
        final TestAtJoinNode test = new TestAtJoinNode(
                variableName,
                arg1FieldType,
                nbrOfLevelsUp,
                FieldType.SUBJECT);
        test.setCondition(earlierCondition);
        return test;
      }
      if (isVariableName(earlierCondition.getObject())) {
        // the object of the given condition is a variable
        variableName = getVariableName((Resource) earlierCondition.getObject());
        if (variableName.equals(joinVariableName)) {
          final TestAtJoinNode test = new TestAtJoinNode(
                  variableName,
                  arg1FieldType,
                  nbrOfLevelsUp,
                  FieldType.OBJECT);
          test.setCondition(earlierCondition);
          return test;
        }
      }
    }
    // the join variable was not found in any of the earlier conditions
    return null;
  }

  /** Logs the token list starting at the given token.
   *
   * @param token the given token
   */
  private void logTokenList(final Token token) {
    //Preconditions
    assert token != null : "token must not be null";

    if (LOGGER.isDebugEnabled()) {
      int level = 1;
      Token token1 = token;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("    token list");
      }
      while (true) {
        LOGGER.debug("      " + level++ + " " + token1.toString());
        token1 = token1.getParent();
        if (token1 == null) {
          break;
        }
      }
    }
  }

  /** Builds a new alpha memory or shares an existing one having the required pattern for the given condition.
   *
   * @param condition the given condition
   * @param notSameTermAsDictionary the non-sameAsTerm dictionary, variable URI --> not same-as term URIs
   * @return the alpha memory having the required pattern
   */
  private AlphaMemory buildOrShareAlphaMemory(
          final org.texai.inference.domainEntity.Statement condition,
          final Map<URI, Set<Value>> notSameTermAsDictionary) {
    //Preconditions
    assert condition != null : "condition must not be null";
    assert notSameTermAsDictionary != null : "notSameTermAsDictionary must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("buildOrShareAlphaMemory, condition: " + condition + ", not same-as terms: " + notSameTermAsDictionary);
    }
    final Value object = condition.getObject();
    final String pattern = derivePattern(condition.getPredicate(), object);
    AlphaMemory alphaMemory = alphaMemoryDictionary.get(pattern);
    if (alphaMemory == null) {
      final Set<Value> notSameTerms = new HashSet<>();
      if (RDFUtility.isVariableURI(object)) {
        if (notSameTermAsDictionary.containsKey((URI) object)) {
          notSameTerms.addAll(notSameTermAsDictionary.get((URI) object));
          LOGGER.debug("  applicable not same-as terms: " + notSameTerms);
        }
      }
      alphaMemory = new AlphaMemory(pattern, notSameTerms);
      alphaMemoryDictionary.put(pattern, alphaMemory);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  added  " + alphaMemory);
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  reused " + alphaMemory);
      }
    }
    return alphaMemory;
  }

  /** Derives a pattern for matching the given predicate and object.
   *
   * @param predicate the given predicate
   * @param predicate the given object
   * @returna pattern for matching the given condition
   */
  public static String derivePattern(final URI predicate, final Value object) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert object != null : "object must not be null";

    final String predicatePattern = RDFUtility.formatResource(predicate);
    if (isVariableName(object)) {
      return predicatePattern;
    } else if (object instanceof URI) {
      return predicatePattern + " " + RDFUtility.formatResource((URI) object);
    } else {
      return predicatePattern + " " + object;
    }
  }

  /** Builds or shares a beta memory node for the given parent node.
   *
   * @param parent the parent node
   * @return a beta memory node
   */
  private BetaMemoryNode buildOrShareBetaMemoryNode(final AbstractReteNode parent) {
    //Preconditions
    assert parent != null : "parent must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("buildOrShareBetaMemoryNode, parent: " + parent);
    }

    // look for an existing node to share
    for (final AbstractReteNode childNode : parent.getChildren()) {
      if (childNode instanceof BetaMemoryNode) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("  reused " + (BetaMemoryNode) childNode);
        }
        return (BetaMemoryNode) childNode;
      }
    }
    // otherwise, create a new beta memory node
    final BetaMemoryNode betaMemoryNode = new BetaMemoryNode(parent);
    parent.getChildren().addFirst(betaMemoryNode);
    addNodeToDictionary(betaMemoryNode);
    updateNewNodeWithMatchesFromAbove(betaMemoryNode);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  added " + betaMemoryNode);
    }
    return betaMemoryNode;
  }

  /** Builds or shares a join node.
   *
   * @param parent the parent node
   * @param alphaMemory the alpha memory
   * @param tests the tests
   * @param condition the condition
   * @return a join node
   */
  private JoinNode buildOrShareJoinNode(
          final AbstractReteNode parent,
          final AlphaMemory alphaMemory,
          final List<TestAtJoinNode> tests,
          final org.texai.inference.domainEntity.Statement condition) {
    //Preconditions
    assert parent != null : "parent must not be null";
    assert alphaMemory != null : "alphaMemory must not be null";
    assert tests != null : "tests must not be null";
    assert condition != null : "condition must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("buildOrShareJoinNode, parent: " + parent + ", alphaMemory: " + alphaMemory + ", tests: " + tests + " condition: " + condition);
    }

    // look for an existing join node to share
    for (final AbstractReteNode childNode : parent.getChildren()) {
      if (childNode instanceof JoinNode
              && ((JoinNode) childNode).getAlphaMemory().equals(alphaMemory)
              && ((JoinNode) childNode).getTests().equals(tests)
              && ((JoinNode) childNode).getCondition().equals(condition)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("  reusing " + childNode);
        }
        return (JoinNode) childNode;
      }
    }
    final JoinNode joinNode = new JoinNode(parent, alphaMemory, tests, condition);
    parent.getChildren().addFirst(joinNode);
    alphaMemory.getSuccessors().addFirst(joinNode);
    addNodeToDictionary(joinNode);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  added " + joinNode);
    }
    return joinNode;
  }

  /** Updates the given production or beta node with matches from above.
   *
   * @param node the given production or beta node
   */
  private void updateNewNodeWithMatchesFromAbove(final AbstractReteNode node) {
    //Preconditions
    assert node != null : "node must not be null";

    //TODO if queries are ever added dynamically
  }

  /** Returns whether the given value is a variable name for a URI or blank node.
   *
   * @param value the given value
   * @return whether the given value is a variable name for a URI or blank node
   */
  public static boolean isVariableName(final Value value) {
    //Preconditions
    assert value != null : "resource must not be null";

    if (value instanceof BNode) {
      return true;
    } else {
      return RDFUtility.isVariableURI(value);
    }
  }

  /** Returns the variable name for a URI or blank node, or null if the resource is not a variable.
   *
   * @param resource the given resource
   * @return the variable name for a URI or blank node, or null if the resource is not a variable
   */
  public static String getVariableName(final Resource resource) {
    //Preconditions
    assert resource != null : "resource must not be null";

    if (resource instanceof BNode) {
      return ((BNode) resource).getID();
    } else if (resource instanceof URI && ((URI) resource).getLocalName().startsWith("?")) {
      return ((URI) resource).getLocalName();
    } else {
      return null;
    }
  }

  /** Gets the alpha memory dictionary, pattern --> alpha memories.
   *
   * @return the alpha memory dictionary
   */
  public Map<String, AlphaMemory> getAlphaMemoryDictionary() {
    return alphaMemoryDictionary;
  }

  /** Gets the number of added query containers.
   *
   * @return the number of added query containers
   */
  public int getNbrOfAddedQueryContainers() {
    return queryContainerNames.size();
  }

  /** Gets whether the query container has been added to the Rete network.
   *
   * @param queryContainerName the name of the query container
   * @return whether the query container has been added to the Rete network
   */
  public boolean containsQueryContainer(final String queryContainerName) {
    //Preconditions
    assert queryContainerName != null : "queryContainerName must not be null";
    assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";

    return queryContainerNames.contains(queryContainerName);
  }

  /** Adds the given node to the node dictionary.
   *
   * @param node the given node
   */
  private void addNodeToDictionary(final AbstractReteNode node) {
    //Preconditions
    assert node != null : "node must not be null";

    List<AbstractReteNode> nodes = nodeDictionary.get(queryContainerName);
    if (nodes == null) {
      nodes = new ArrayList<>();
      nodeDictionary.put(queryContainerName, nodes);
    }
    nodes.add(node);
  }

  /** Gets the names of the query containers that are satisfied by the currently added statements.
   *
   * @return the names of the query containers that are satisfied by the currently added statements
   */
  public Set<String> getSatisfiedQueryContainerNames() {
    return satisfiedProductionNodeDictionary.keySet();
  }

  /** Gets the name of the most specific query container that is satisfied by the currently added statements.
   *
   * @return the name of the most specific query container that is satisfied by the currently added statements
   */
  public String getMostSpecificQueryContainerName() {
    if (satisfiedProductionNodeDictionary.isEmpty()) {
      return null;
    } else if (satisfiedProductionNodeDictionary.size() == 1) {
      return ((ProductionNode) satisfiedProductionNodeDictionary.values().toArray()[0]).getQueryContainer().getName();
    }
    scoredQueryContainerInfos.clear();
    for (final ProductionNode productionNode : satisfiedProductionNodeDictionary.values()) {
      scoredQueryContainerInfos.add(new ScoredQueryContainerInfo(
              productionNode.getQueryContainer().getName(),
              countMatchedStatements(productionNode)));
    }
    Collections.sort(scoredQueryContainerInfos);
    LOGGER.info("scored production nodes: " + scoredQueryContainerInfos);
    final ScoredQueryContainerInfo firstScoredQueryContainerInfo = scoredQueryContainerInfos.get(0);
    final ScoredQueryContainerInfo secondScoredQueryContainerInfo = scoredQueryContainerInfos.get(1);
    if (firstScoredQueryContainerInfo.nbrMatchedStatements > secondScoredQueryContainerInfo.nbrMatchedStatements) {
      return firstScoredQueryContainerInfo.queryContainerName;
    } else {
      throw new TexaiException("cannot determine highest-scoring production " + scoredQueryContainerInfos);
    }

  }

  /** Gets the scored query container informations.
   *
   * @return the scored query container informations
   */
  public List<ScoredQueryContainerInfo> getScoredQueryContainerInfos() {
    return scoredQueryContainerInfos;
  }

  /** Provides scored query container information. */
  public static class ScoredQueryContainerInfo implements Comparable<ScoredQueryContainerInfo> {

    /** the query container name */
    private final String queryContainerName;
    /** the number of matched statements */
    private final int nbrMatchedStatements;

    /** Constructs a new ScoredQueryContainerInfo instance.
     *
     * @param queryContainerName the query container name
     * @param nbrMatchedStatements the number of matched statements
     */
    ScoredQueryContainerInfo(
            final String queryContainerName,
            final int nbrMatchedStatements) {
      //Preconditions
      assert queryContainerName != null : "queryContainerName must not be null";
      assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";
      assert nbrMatchedStatements > 0 : "nbrMatchedStatements must be positive";

      this.queryContainerName = queryContainerName;
      this.nbrMatchedStatements = nbrMatchedStatements;
    }

    /** Compares another object to this one.
     *
     * @param that the other object
     * @return -1 if this is less than the other object, 0 if equal, otherwise return +1
     */
    @Override
    public int compareTo(ScoredQueryContainerInfo that) {
      //Preconditions
      assert that != null : "that must not be null";

      if (this.nbrMatchedStatements > that.nbrMatchedStatements) {
        return -1;
      } else if (this.nbrMatchedStatements == that.nbrMatchedStatements) {
        return 0;
      } else {
        return 1;
      }
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return "[" + queryContainerName + " " + nbrMatchedStatements + "]";
    }
  }

  /** Gets the bindings from the satisfied query having the given name, filtered by the query's select clause.
   *
   * @param queryContainerName the query container name
   * @return the bindings from the satisfied query filtered by the query's select clause, or null if the query was not satisfied
   */
  public Map<String, Value> getBindings(final String queryContainerName) {
    //Preconditions
    assert queryContainerName != null : "queryContainerName must not be null";
    assert !queryContainerName.isEmpty() : "queryContainerName must not be empty";

    final ProductionNode productionNode = satisfiedProductionNodeDictionary.get(queryContainerName);
    if (productionNode == null) {
      return null;
    } else {
      return getBindings(productionNode);
    }
  }

  /** Gets the bindings from the given satisfied production node, filtered by the query's select clause.
   *
   * @param productionNode the satisfied production node
   * @return the bindings from the satisfied query filtered by the query's select clause, or null if the query was not satisfied
   */
  private Map<String, Value> getBindings(final ProductionNode productionNode) {
    //Preconditions
    assert productionNode != null : "productionNode must not be null";
    final Set<String> selectedVariableNames = new ArraySet<>();
    final SelectQuery selectQuery = (SelectQuery) productionNode.getQueryContainer().getQuery();
    for (final Variable variable : selectQuery.getSelect().getVariables()) {
      selectedVariableNames.add(variable.getName());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("selected variable names: " + selectedVariableNames);
      LOGGER.debug("bindings for the satisfied production: " + queryContainerName);
    }
    final Map<String, Value> bindingDictionary = new HashMap<>();
    assert !productionNode.getTokens().isEmpty();
    Token token = productionNode.getTokens().get(0);
    assert token != null;
    while (true) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  " + token.toString());
      }
      if (!token.isDummy()) {
        String variableName = token.getSubjectVariableName();
        Value value = token.getStatement().getSubject();
        if (!bindingDictionary.containsKey(variableName)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("    " + variableName + "=" + RDFUtility.formatValue(value));
          }
          if (selectedVariableNames.contains(variableName)) {
            bindingDictionary.put(variableName, value);
          }
        }
        variableName = token.getObjectVariableName();
        if (variableName != null) {
          value = token.getStatement().getObject();
          if (!bindingDictionary.containsKey(variableName)) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("    " + variableName + "=" + RDFUtility.formatValue(value));
            }
            if (selectedVariableNames.contains(variableName)) {
              bindingDictionary.put(variableName, value);
            }
          }
        }
      }
      token = token.getParent();
      if (token == null) {
        break;
      }
    }
    return bindingDictionary;
  }

  /** Counts the matched statements for the given production node.
   *
   * @param productionNode the given production node
   * @return the number of matched statements
   */
  private static int countMatchedStatements(final ProductionNode productionNode) {
    //Preconditions
    assert productionNode != null : "productionNode must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("countMatchedStatements for " + productionNode.getQueryContainer().getName());
    }
    int nbrMatchedStatements = 0;
    AbstractReteNode node = productionNode;
    while (node != null) {
      if (node instanceof BetaMemoryNode) {
        for (final Token token : ((BetaMemoryNode) node).getTokens()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  token: " + token.graphLabel());
          }
          if (token.isMemberOfSatisfactionSet()) {
            nbrMatchedStatements++;
          }
        }
      }
      node = node.getParent();
    }

    return nbrMatchedStatements;
  }

  /** Emits a Rete visualization having the given graph name.
   *
   * @param graphName the labeled tree
   * @param focalQueryContainerName the focal query container name
   */
  public void toGraphViz(
          final String graphName,
          final String focalQueryContainerName) {
    //Preconditions
    assert graphName != null : "graphName must not be null";
    assert !graphName.isEmpty() : "graphName must not be empty";

    if (System.getProperty("file.separator").equals("\\")) {
      // do not try to execute GraphViz on Windows
      return;
    }
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    toGraphViz(System.getProperty("user.dir") + "/doc/graph-visualization", graphName, focalQueryContainerName);
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("cd doc/graph-visualization ; dot -Tpng ");
    stringBuilder.append(graphName);
    stringBuilder.append(".dot -o ");
    stringBuilder.append(graphName);
    stringBuilder.append(".png");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    try {
      Runtime.getRuntime().exec(cmdArray);
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Generates a GraphViz input file that depicts this graph.
   *
   * @param graphPath the graph directory
   * @param graphName the output graph name
   * @param focalQueryContainerName the focal query container name
   */
  public void toGraphViz(
          final String graphPath,
          final String graphName,
          final String focalQueryContainerName) {
    //Preconditions
    assert graphPath != null : "graphPath must not be null";
    assert !graphPath.isEmpty() : "graphPath must not be empty";
    assert graphName != null : "graphName must not be null";
    assert !graphName.isEmpty() : "graphName must not be empty";
    assert !graphName.contains(" ") : "graphName must not contain whitespace";

    boolean isFocalQueryGraph = focalQueryContainerName != null;
    final StringBuilder stringBuilder = new StringBuilder();
    final String graphDataPath = graphPath + "/" + graphName + ".dot";
    final String keyDataPath = graphPath + "/" + graphName + "-key.txt";
    BufferedWriter graphBufferedWriter = null;
    BufferedWriter keyBufferedWriter = null;
    try {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("graphDataPath: " + graphDataPath);
      }
      graphBufferedWriter = new BufferedWriter(new FileWriter(graphDataPath));
      keyBufferedWriter = new BufferedWriter(new FileWriter(keyDataPath));
      graphBufferedWriter.append("digraph \"");
      graphBufferedWriter.append(graphName);
      graphBufferedWriter.append("\" {\n");
      graphBufferedWriter.append("  ratio = \"auto\" ;\n");
      graphBufferedWriter.append("  mincross = 2.0 ;\n");

      // see http://www.graphviz.org/doc/info/colors.html

      alphaMemoryLabelSerialNbr = 0;
      joinLabelSerialNumber = 0;
      betaMemoryLabelSerialNumber = 0;
      dummyTopLabelSerialNumber = 0;
      nodeId = 0;

      final List<AlphaMemory> focalAlphaMemories = new ArrayList<>();
      if (isFocalQueryGraph) {
        graphQueryContainer(
                graphBufferedWriter,
                keyBufferedWriter,
                focalQueryContainerName,
                stringBuilder,
                focalAlphaMemories);
      } else {
        for (final String queryContainerName1 : nodeDictionary.keySet()) {
          graphQueryContainer(
                  graphBufferedWriter,
                  keyBufferedWriter,
                  queryContainerName1,
                  stringBuilder,
                  null);
        }
      }

      final int entranceNodeId = ++nodeId;
      if (!isFocalQueryGraph) {
        // graph the entrance node
        graphBufferedWriter.append("  ");
        graphBufferedWriter.append("N");
        graphBufferedWriter.append(String.valueOf(entranceNodeId));
        graphBufferedWriter.append(" [\n    style = filled\n    fillcolor = cornflowerblue\n    label = \"Entrance\" ];\n");
      }

      final List<AlphaMemory> orderedAlphaMemories = new ArrayList<>(alphaMemoryDictionary.values());
      Collections.sort(orderedAlphaMemories);
      for (final AlphaMemory alphaMemory : orderedAlphaMemories) {
        if (isFocalQueryGraph) {
          // restrict alpha memories to those that connect to the focal query container
          final List<AbstractReteNode> focalNodes = nodeDictionary.get(focalQueryContainerName);
          assert focalNodes != null && !focalNodes.isEmpty();
          boolean isConnectedToFocalQueryContainer = false;
          for (final JoinNode joinNode : alphaMemory.getSuccessors()) {
            if (focalNodes.contains(joinNode)) {
              isConnectedToFocalQueryContainer = true;
              break;
            }
          }
          if (!isConnectedToFocalQueryContainer) {
            continue;
          }
        }
        // describe the alpha memory node
        keyBufferedWriter.append("Alpha");
        keyBufferedWriter.append(String.valueOf(++alphaMemoryLabelSerialNbr));
        keyBufferedWriter.append("  ");
        keyBufferedWriter.append(alphaMemory.toString());
        keyBufferedWriter.append("\n");

        // graph the alpha memory node
        graphBufferedWriter.append("  ");
        graphBufferedWriter.append("N");
        final int alphaMemoryNodeId = ++nodeId;
        alphaMemory.setId(alphaMemoryNodeId);
        graphBufferedWriter.append(String.valueOf(alphaMemoryNodeId));
        graphBufferedWriter.append(" [\n    style = filled\n    fillcolor = ");
        if (alphaMemory.getStatements().isEmpty()) {
          graphBufferedWriter.append("cyan");
        } else {
          graphBufferedWriter.append("cyan3");
        }
        graphBufferedWriter.append("\n    label = \"");
        stringBuilder.setLength(0);
        stringBuilder.append("pattern '");
        stringBuilder.append(alphaMemory.getPattern());
        stringBuilder.append('\'');
        for (final org.openrdf.model.Statement statement : alphaMemory.getStatements()) {
          stringBuilder.append("\\n");
          stringBuilder.append(RDFUtility.formatStatement(statement));
        }
        graphBufferedWriter.append(stringBuilder.toString());
        graphBufferedWriter.append("\" ];\n");

        if (!isFocalQueryGraph) {
          // link the entrance node to the alpha memory node
          graphBufferedWriter.append("  ");
          graphBufferedWriter.append("N");
          graphBufferedWriter.append(String.valueOf(entranceNodeId));
          graphBufferedWriter.append(" -> ");
          graphBufferedWriter.append("N");
          graphBufferedWriter.append(String.valueOf(alphaMemoryNodeId));
          graphBufferedWriter.append(";\n");
        }

        for (final JoinNode successor : alphaMemory.getSuccessors()) {
          if (isFocalQueryGraph) {
            final List<AbstractReteNode> focalNodes = nodeDictionary.get(focalQueryContainerName);
            if (!focalNodes.contains(successor)) {
              // this is a focal query container graph, so bypass connecting non-focal join nodes
              continue;
            } else if (successor.getId() == 0) {
              continue;
            }
          }
          // link the alpha memory node to the join node
          graphBufferedWriter.append("  ");
          graphBufferedWriter.append("N");
          graphBufferedWriter.append(String.valueOf(alphaMemoryNodeId));
          graphBufferedWriter.append(" -> ");
          graphBufferedWriter.append("N");
          graphBufferedWriter.append(String.valueOf(successor.getId()));
          graphBufferedWriter.append(";\n");
        }
      }
      graphBufferedWriter.append("}");
      graphBufferedWriter.close();
      keyBufferedWriter.close();
    } catch (final IOException ex) {
      try {
        if (graphBufferedWriter != null) {
          graphBufferedWriter.close();
        }

        if (keyBufferedWriter != null) {
          keyBufferedWriter.close();
        }
        throw new TexaiException(ex);
      } catch (final IOException ex1) {
        throw new TexaiException(ex1);    // NOPMD
      }
    }
  }

  /** Graphs the given named query container.
   *
   * @param graphBufferedWriter the graph buffered writer
   * @param keyBufferedWriter the graph key buffered writer
   * @param queryContainerName1 the query container name
   * @param stringBuilder the string builder
   * @throws IOException if an input/output exception occurs
   */
  private void graphQueryContainer(
          final BufferedWriter graphBufferedWriter,
          final BufferedWriter keyBufferedWriter,
          final String queryContainerName1,
          final StringBuilder stringBuilder,
          final List<AlphaMemory> focalAlphaMemories) throws IOException {
    //Preconditions
    assert graphBufferedWriter != null : "graphBufferedWriter must not be null";
    assert keyBufferedWriter != null : "keyBufferedWriter must not be null";
    assert queryContainerName1 != null : "queryContainerName1 must not be null";
    assert !queryContainerName1.isEmpty() : "queryContainerName1 must not be empty";
    assert stringBuilder != null : "stringBuilder must not be null";

    graphBufferedWriter.append("subgraph cluster_");
    graphBufferedWriter.append(queryContainerName1);
    graphBufferedWriter.append(" {\n");
    graphBufferedWriter.append("  label = \"");
    graphBufferedWriter.append(queryContainerName1);
    graphBufferedWriter.append("\"\n");
    for (final AbstractReteNode node : nodeDictionary.get(queryContainerName1)) {
      final String fillColor;
      final String nodeLabel;
      final String shape;

      if (node instanceof JoinNode) {
        // graph join node
        final JoinNode joinNode = (JoinNode) node;
        if (focalAlphaMemories != null) {
          focalAlphaMemories.add(joinNode.getAlphaMemory());
        }
        fillColor = "magenta";
        if (queryContainerNames.size() <= 2) {
          // show more detail if two or less queries
          stringBuilder.setLength(0);
          stringBuilder.append("Join ");
          stringBuilder.append(++joinLabelSerialNumber);
          stringBuilder.append("\\n");
          stringBuilder.append(joinNode.getCondition().toParenthesizedString());
          nodeLabel = stringBuilder.toString();
          shape = "";
        } else {
          // show minimal join node in order to have a good graph with many queries
          nodeLabel = StringUtils.padWithTrailingSpaces("Join" + ++joinLabelSerialNumber, 3);
          shape = "\n    shape = circle fixedsize = true height = 0.9";
        }

      } else if (node instanceof BetaMemoryNode) {
        // graph beta memory
        if (((BetaMemoryNode) node).getTokens().isEmpty()) {
          fillColor = "greenyellow";
        } else {
          fillColor = "green";
        }
        stringBuilder.setLength(0);
        stringBuilder.append("BetaMemory");
        stringBuilder.append(++betaMemoryLabelSerialNumber);
        for (final Token token : ((BetaMemoryNode) node).getTokens()) {
          stringBuilder.append("\\n");
          stringBuilder.append(token.graphLabel());
        }
        nodeLabel = stringBuilder.toString();
        shape = "";

      } else if (node instanceof DummyTopNode) {
        // graph dummy top join node
        fillColor = "green";
        nodeLabel = "DummyTop" + ++dummyTopLabelSerialNumber;
        shape = "";

      } else if (node instanceof ProductionNode) {
        // graph production node
        if (satisfiedProductionNodeDictionary.values().contains((ProductionNode) node)) {
          fillColor = "gold";
        } else {
          fillColor = "cornflowerblue";
        }
        stringBuilder.setLength(0);
        stringBuilder.append("Production ");
        stringBuilder.append(((ProductionNode) node).getQueryContainer().getName());
        for (final Token token : ((ProductionNode) node).getTokens()) {
          stringBuilder.append("\\n");
          stringBuilder.append(token.graphLabel());
        }
        nodeLabel = stringBuilder.toString();
        shape = "\n    shape = box";

      } else {
        assert false;
        fillColor = null;
        nodeLabel = null;
        shape = null;
      }

      // describe the node
      keyBufferedWriter.append(nodeLabel);
      keyBufferedWriter.append("  ");
      keyBufferedWriter.append(node.toString());
      keyBufferedWriter.append("\n");

      // graph the node
      graphBufferedWriter.append("  ");
      graphBufferedWriter.append("N");
      node.setId(++nodeId);
      graphBufferedWriter.append(String.valueOf(nodeId));
      graphBufferedWriter.append(" [\n");
      graphBufferedWriter.append(shape);
      graphBufferedWriter.append("\n    style = filled\n    fillcolor = ");
      graphBufferedWriter.append(fillColor);
      graphBufferedWriter.append("\n    label = \"");
      graphBufferedWriter.append(nodeLabel);
      graphBufferedWriter.append("\" ];\n");
    }

    for (final AbstractReteNode node : nodeDictionary.get(queryContainerName1)) {
      for (final AbstractReteNode childNode : node.getChildren()) {
        // link the node to its children
        graphBufferedWriter.append("  ");
        graphBufferedWriter.append("N");
        graphBufferedWriter.append(String.valueOf(node.getId()));
        graphBufferedWriter.append(" -> ");
        graphBufferedWriter.append("N");
        graphBufferedWriter.append(String.valueOf(childNode.getId()));
        graphBufferedWriter.append(";\n");
      }
    }
    graphBufferedWriter.append("}\n");
  }
}
