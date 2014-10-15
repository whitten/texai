/*
 * SparqlParser.java
 *
 * Created on Feb 8, 2009, 7:32:46 AM
 *
 * Description: Provides a parser for a SPARQL query that creates a persistent syntax tree of the parsed elements.
 *
 * Copyright (C) Feb 8, 2009 Stephen L. Reed.
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
package org.texai.inference.sparqlParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BNodeImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.parser.sparql.ast.ASTAbs;
import org.openrdf.query.parser.sparql.ast.ASTAdd;
import org.openrdf.query.parser.sparql.ast.ASTAnd;
import org.openrdf.query.parser.sparql.ast.ASTAskQuery;
import org.openrdf.query.parser.sparql.ast.ASTAvg;
import org.openrdf.query.parser.sparql.ast.ASTBNodeFunc;
import org.openrdf.query.parser.sparql.ast.ASTBaseDecl;
import org.openrdf.query.parser.sparql.ast.ASTBasicGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTBind;
import org.openrdf.query.parser.sparql.ast.ASTBindingSet;
import org.openrdf.query.parser.sparql.ast.ASTBindingValue;
import org.openrdf.query.parser.sparql.ast.ASTBindingsClause;
import org.openrdf.query.parser.sparql.ast.ASTBlankNode;
import org.openrdf.query.parser.sparql.ast.ASTBlankNodePropertyList;
import org.openrdf.query.parser.sparql.ast.ASTBound;
import org.openrdf.query.parser.sparql.ast.ASTCeil;
import org.openrdf.query.parser.sparql.ast.ASTClear;
import org.openrdf.query.parser.sparql.ast.ASTCoalesce;
import org.openrdf.query.parser.sparql.ast.ASTCollection;
import org.openrdf.query.parser.sparql.ast.ASTCompare;
import org.openrdf.query.parser.sparql.ast.ASTConcat;
import org.openrdf.query.parser.sparql.ast.ASTConstraint;
import org.openrdf.query.parser.sparql.ast.ASTConstruct;
import org.openrdf.query.parser.sparql.ast.ASTConstructQuery;
import org.openrdf.query.parser.sparql.ast.ASTContains;
import org.openrdf.query.parser.sparql.ast.ASTCopy;
import org.openrdf.query.parser.sparql.ast.ASTCount;
import org.openrdf.query.parser.sparql.ast.ASTCreate;
import org.openrdf.query.parser.sparql.ast.ASTDatasetClause;
import org.openrdf.query.parser.sparql.ast.ASTDatatype;
import org.openrdf.query.parser.sparql.ast.ASTDay;
import org.openrdf.query.parser.sparql.ast.ASTDeleteClause;
import org.openrdf.query.parser.sparql.ast.ASTDeleteData;
import org.openrdf.query.parser.sparql.ast.ASTDeleteWhere;
import org.openrdf.query.parser.sparql.ast.ASTDescribe;
import org.openrdf.query.parser.sparql.ast.ASTDescribeQuery;
import org.openrdf.query.parser.sparql.ast.ASTDrop;
import org.openrdf.query.parser.sparql.ast.ASTEncodeForURI;
import org.openrdf.query.parser.sparql.ast.ASTExistsFunc;
import org.openrdf.query.parser.sparql.ast.ASTFalse;
import org.openrdf.query.parser.sparql.ast.ASTFloor;
import org.openrdf.query.parser.sparql.ast.ASTFunctionCall;
import org.openrdf.query.parser.sparql.ast.ASTGraphGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTGraphOrDefault;
import org.openrdf.query.parser.sparql.ast.ASTGraphPatternGroup;
import org.openrdf.query.parser.sparql.ast.ASTGraphRefAll;
import org.openrdf.query.parser.sparql.ast.ASTGroupClause;
import org.openrdf.query.parser.sparql.ast.ASTGroupConcat;
import org.openrdf.query.parser.sparql.ast.ASTGroupCondition;
import org.openrdf.query.parser.sparql.ast.ASTHavingClause;
import org.openrdf.query.parser.sparql.ast.ASTHours;
import org.openrdf.query.parser.sparql.ast.ASTIRI;
import org.openrdf.query.parser.sparql.ast.ASTIRIFunc;
import org.openrdf.query.parser.sparql.ast.ASTIf;
import org.openrdf.query.parser.sparql.ast.ASTIn;
import org.openrdf.query.parser.sparql.ast.ASTInfix;
import org.openrdf.query.parser.sparql.ast.ASTInlineData;
import org.openrdf.query.parser.sparql.ast.ASTInsertClause;
import org.openrdf.query.parser.sparql.ast.ASTInsertData;
import org.openrdf.query.parser.sparql.ast.ASTIsBlank;
import org.openrdf.query.parser.sparql.ast.ASTIsIRI;
import org.openrdf.query.parser.sparql.ast.ASTIsLiteral;
import org.openrdf.query.parser.sparql.ast.ASTIsNumeric;
import org.openrdf.query.parser.sparql.ast.ASTLang;
import org.openrdf.query.parser.sparql.ast.ASTLangMatches;
import org.openrdf.query.parser.sparql.ast.ASTLimit;
import org.openrdf.query.parser.sparql.ast.ASTLoad;
import org.openrdf.query.parser.sparql.ast.ASTLowerCase;
import org.openrdf.query.parser.sparql.ast.ASTMD5;
import org.openrdf.query.parser.sparql.ast.ASTMath;
import org.openrdf.query.parser.sparql.ast.ASTMax;
import org.openrdf.query.parser.sparql.ast.ASTMin;
import org.openrdf.query.parser.sparql.ast.ASTMinusGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTMinutes;
import org.openrdf.query.parser.sparql.ast.ASTModify;
import org.openrdf.query.parser.sparql.ast.ASTMonth;
import org.openrdf.query.parser.sparql.ast.ASTMove;
import org.openrdf.query.parser.sparql.ast.ASTNot;
import org.openrdf.query.parser.sparql.ast.ASTNotExistsFunc;
import org.openrdf.query.parser.sparql.ast.ASTNotIn;
import org.openrdf.query.parser.sparql.ast.ASTNow;
import org.openrdf.query.parser.sparql.ast.ASTNumericLiteral;
import org.openrdf.query.parser.sparql.ast.ASTObjectList;
import org.openrdf.query.parser.sparql.ast.ASTOffset;
import org.openrdf.query.parser.sparql.ast.ASTOptionalGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTOr;
import org.openrdf.query.parser.sparql.ast.ASTOrderClause;
import org.openrdf.query.parser.sparql.ast.ASTOrderCondition;
import org.openrdf.query.parser.sparql.ast.ASTPathAlternative;
import org.openrdf.query.parser.sparql.ast.ASTPathElt;
import org.openrdf.query.parser.sparql.ast.ASTPathMod;
import org.openrdf.query.parser.sparql.ast.ASTPathOneInPropertySet;
import org.openrdf.query.parser.sparql.ast.ASTPathSequence;
import org.openrdf.query.parser.sparql.ast.ASTPrefixDecl;
import org.openrdf.query.parser.sparql.ast.ASTProjectionElem;
import org.openrdf.query.parser.sparql.ast.ASTPropertyList;
import org.openrdf.query.parser.sparql.ast.ASTPropertyListPath;
import org.openrdf.query.parser.sparql.ast.ASTQName;
import org.openrdf.query.parser.sparql.ast.ASTQuadsNotTriples;
import org.openrdf.query.parser.sparql.ast.ASTQuery;
import org.openrdf.query.parser.sparql.ast.ASTQueryContainer;
import org.openrdf.query.parser.sparql.ast.ASTRDFLiteral;
import org.openrdf.query.parser.sparql.ast.ASTRand;
import org.openrdf.query.parser.sparql.ast.ASTRegexExpression;
import org.openrdf.query.parser.sparql.ast.ASTReplace;
import org.openrdf.query.parser.sparql.ast.ASTRound;
import org.openrdf.query.parser.sparql.ast.ASTSHA1;
import org.openrdf.query.parser.sparql.ast.ASTSHA224;
import org.openrdf.query.parser.sparql.ast.ASTSHA256;
import org.openrdf.query.parser.sparql.ast.ASTSHA384;
import org.openrdf.query.parser.sparql.ast.ASTSHA512;
import org.openrdf.query.parser.sparql.ast.ASTSTRUUID;
import org.openrdf.query.parser.sparql.ast.ASTSameTerm;
import org.openrdf.query.parser.sparql.ast.ASTSample;
import org.openrdf.query.parser.sparql.ast.ASTSeconds;
import org.openrdf.query.parser.sparql.ast.ASTSelect;
import org.openrdf.query.parser.sparql.ast.ASTSelectQuery;
import org.openrdf.query.parser.sparql.ast.ASTServiceGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTStr;
import org.openrdf.query.parser.sparql.ast.ASTStrAfter;
import org.openrdf.query.parser.sparql.ast.ASTStrBefore;
import org.openrdf.query.parser.sparql.ast.ASTStrDt;
import org.openrdf.query.parser.sparql.ast.ASTStrEnds;
import org.openrdf.query.parser.sparql.ast.ASTStrLang;
import org.openrdf.query.parser.sparql.ast.ASTStrLen;
import org.openrdf.query.parser.sparql.ast.ASTStrStarts;
import org.openrdf.query.parser.sparql.ast.ASTString;
import org.openrdf.query.parser.sparql.ast.ASTSubstr;
import org.openrdf.query.parser.sparql.ast.ASTSum;
import org.openrdf.query.parser.sparql.ast.ASTTimezone;
import org.openrdf.query.parser.sparql.ast.ASTTriplesSameSubject;
import org.openrdf.query.parser.sparql.ast.ASTTriplesSameSubjectPath;
import org.openrdf.query.parser.sparql.ast.ASTTrue;
import org.openrdf.query.parser.sparql.ast.ASTTz;
import org.openrdf.query.parser.sparql.ast.ASTUUID;
import org.openrdf.query.parser.sparql.ast.ASTUnionGraphPattern;
import org.openrdf.query.parser.sparql.ast.ASTUnparsedQuadDataBlock;
import org.openrdf.query.parser.sparql.ast.ASTUpdateContainer;
import org.openrdf.query.parser.sparql.ast.ASTUpdateSequence;
import org.openrdf.query.parser.sparql.ast.ASTUpperCase;
import org.openrdf.query.parser.sparql.ast.ASTVar;
import org.openrdf.query.parser.sparql.ast.ASTWhereClause;
import org.openrdf.query.parser.sparql.ast.ASTYear;
import org.openrdf.query.parser.sparql.ast.Node;
import org.openrdf.query.parser.sparql.ast.ParseException;
import org.openrdf.query.parser.sparql.ast.SimpleNode;
import org.openrdf.query.parser.sparql.ast.SyntaxTreeBuilder;
import org.openrdf.query.parser.sparql.ast.SyntaxTreeBuilderVisitor;
import org.openrdf.query.parser.sparql.ast.VisitorException;
import org.texai.inference.domainEntity.Statement;
import org.texai.inference.sparql.domainEntity.AbstractOperator;
import org.texai.inference.sparql.domainEntity.BaseDeclaration;
import org.texai.inference.sparql.domainEntity.PrefixDeclaration;
import org.texai.inference.sparql.domainEntity.AbstractQuery;
import org.texai.inference.sparql.domainEntity.AndOperator;
import org.texai.inference.sparql.domainEntity.Constraint;
import org.texai.inference.sparql.domainEntity.NotOperator;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.inference.sparql.domainEntity.SameTermOperator;
import org.texai.inference.sparql.domainEntity.Select;
import org.texai.inference.sparql.domainEntity.SelectQuery;
import org.texai.inference.sparql.domainEntity.Variable;
import org.texai.inference.sparql.domainEntity.WhereClause;
import org.texai.kb.Constants;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;

/**
 * Provides a parser for a SPARQL query that creates a persistent syntax tree of the parsed elements.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class SPARQLParser {

  /**
   * the log4j logger
   */
  private static final Logger LOGGER = Logger.getLogger(SPARQLParser.class);
  /**
   * the indicator whether debug logging is enabled
   */
  private static final boolean IS_DEBUG_LOGGING_ENABLED = LOGGER.isDebugEnabled();
  /**
   * the indicator whether info logging is enabled
   */
  private static final boolean IS_INFO_LOGGING_ENABLED = LOGGER.isEnabledFor(Level.INFO);
  /**
   * the prefix dictionary, prefix --> namespace
   */
  private final Map<String, String> prefixDictionary = new HashMap<>();
  /**
   * the base namespace
   */
  private String baseNamespace;

  /**
   * Constructs a new SparqlParser instance.
   */
  public SPARQLParser() {
  }

  /**
   * Parses the given SPARQL container and returns the syntax tree in Sesame form.
   *
   * @param queryString the SPARQL query
   * @param name the name to be assigned to the SPARQL query when it is persisted
   *
   * @return the syntax tree in Sesame form
   */
  public QueryContainer parseQuery(final String queryString, final String name) {
    //Preconditions
    assert queryString != null : "queryString must not be null";
    assert !queryString.isEmpty() : "queryString must not be empty";
    assert name != null : "name must not be null";
    assert !name.isEmpty() : "name must not be empty";

    // query container
    ASTQueryContainer astQueryContainer = null;
    try {
      astQueryContainer = SyntaxTreeBuilder.parseQuery(queryString);
    } catch (final ParseException ex) {
      throw new TexaiException(ex);
    }

    // BASE declaration
    final BaseDeclaration baseDeclaration;
    if (astQueryContainer.getBaseDecl() == null) {
      baseDeclaration = null;
    } else {
      baseNamespace = astQueryContainer.getBaseDecl().getIRI();
      baseDeclaration = new BaseDeclaration(baseNamespace);
    }

    // PREFIX declaration
    final Set<PrefixDeclaration> prefixDeclarations = new ArraySet<>();
    for (final ASTPrefixDecl astPrefixDecl : astQueryContainer.getPrefixDeclList()) {
      final String prefix = astPrefixDecl.getPrefix();
      assert prefix != null;
      assert !prefix.isEmpty();
      final String namespace = astPrefixDecl.getIRI().getValue();
      assert namespace != null;
      assert !namespace.isEmpty();
      prefixDeclarations.add(new PrefixDeclaration(prefix, namespace));
      prefixDictionary.put(prefix, namespace);
    }

    // query
    final ASTQuery astQuery = astQueryContainer.getQuery();
    final AbstractQuery query;
    if (astQuery instanceof ASTSelectQuery) {

      // SELECT query
      final ASTSelectQuery astSelectQuery = (ASTSelectQuery) astQuery;

      final ASTSelect astSelect = astSelectQuery.getSelect();
      final List<Variable> variables = new ArrayList<>();
      for (final ASTProjectionElem astProjectionElem : astSelect.jjtGetChildren(ASTProjectionElem.class)) {
        final ASTVar astVar = astProjectionElem.jjtGetChild(ASTVar.class);
        variables.add(new Variable("?" + astVar.getName()));
      }
      final Select select = new Select(
              astSelect.isDistinct(),
              astSelect.isReduced(),
              astSelect.isWildcard(),
              variables);

      // WHERE clause
      final ASTWhereClause astWhereClause = astSelectQuery.getWhereClause();
      final List<ASTGraphPatternGroup> astGraphPatternGroups = astWhereClause.jjtGetChildren(ASTGraphPatternGroup.class);
      assert astGraphPatternGroups.size() == 1 : "there must be one graph pattern group";
      final ASTGraphPatternGroup astGraphPatternGroup = astGraphPatternGroups.get(0);
      final List<ASTBasicGraphPattern> astBasicGraphPatterns = astGraphPatternGroup.jjtGetChildren(ASTBasicGraphPattern.class);
      assert astBasicGraphPatterns != null : "astBasicGraphPatterns must not be null";
      assert astBasicGraphPatterns.size() == 1 : "there must be one basic graph pattern";
      final ASTBasicGraphPattern astBasicGraphPattern = astBasicGraphPatterns.get(0);
      final List<ASTTriplesSameSubjectPath> astTriplesSameSubjectPathList = astBasicGraphPattern.jjtGetChildren(ASTTriplesSameSubjectPath.class);
      final List<Statement> statements = new ArrayList<>();
      for (final ASTTriplesSameSubjectPath astTriplesSameSubjectPath : astTriplesSameSubjectPathList) {

        // statement subject
        @SuppressWarnings("UnusedAssignment")
        Resource subject = null;
        final ASTVar astVar = astTriplesSameSubjectPath.jjtGetChild(ASTVar.class);
        if (astVar == null) {
          final ASTBlankNode astBlankNode = astTriplesSameSubjectPath.jjtGetChild(ASTBlankNode.class);
          if (astBlankNode == null) {
            final ASTIRI astIRI = astTriplesSameSubjectPath.jjtGetChild(ASTIRI.class);
            assert astIRI != null;
            subject = new URIImpl(astIRI.getValue());
          } else {
            subject = new BNodeImpl(astBlankNode.getID());
          }
        } else {
          subject = new URIImpl(Constants.TEXAI_NAMESPACE + "?" + astVar.getName());
        }

        // statement predicate
        final URI predicate;
        final ASTPropertyListPath astPropertyListPath = astTriplesSameSubjectPath.jjtGetChild(ASTPropertyListPath.class);
        final ASTPathAlternative astPathAlternative = astPropertyListPath.jjtGetChild(ASTPathAlternative.class);
        final ASTPathSequence astPathSequence = astPathAlternative.jjtGetChild(ASTPathSequence.class);
        final ASTPathElt astPathElt = astPathSequence.jjtGetChild(ASTPathElt.class);
        final ASTQName astQName = astPathElt.jjtGetChild(ASTQName.class);
        predicate = expandQNameToURI(astQName.getValue());

        // statement object
        Value object = null;
        final ASTObjectList astObjectList = astPropertyListPath.getObjectList();
        final Node objectNode = astObjectList.jjtGetChild(0);
        if (objectNode instanceof ASTVar) {
          object = new URIImpl(Constants.TEXAI_NAMESPACE + "?" + ((ASTVar) objectNode).getName());
        } else if (objectNode instanceof ASTQName) {
          object = expandQNameToURI(((ASTQName) objectNode).getValue());
        } else if (objectNode instanceof ASTBlankNode) {
          object = new BNodeImpl(((ASTBlankNode) objectNode).getID());
        } else {
          throw new UnsupportedOperationException("not yet supported as object: " + objectNode);
        }
        final Statement statement = new Statement(subject, predicate, object);
        LOGGER.debug("statement: " + statement);
        statements.add(statement);
      }
      final Constraint constraint;
      final ASTConstraint astConstraint = astBasicGraphPattern.jjtGetChild(ASTConstraint.class);
      if (astConstraint == null) {
        constraint = null;
      } else {
        constraint = visitASTConstraint(astConstraint);
      }
      final WhereClause whereClause = new WhereClause(statements, constraint);

      query = new SelectQuery(select, whereClause);

    } else {
      throw new UnsupportedOperationException("only SELECT queries are supported for now");
    }
    final QueryContainer queryContainer = new QueryContainer(
            name,
            baseDeclaration,
            prefixDeclarations,
            query);
    return queryContainer;
  }

  /**
   * Visits the children of the ASTConstraint to build an isomorphic Constraint tree.
   *
   * @param astConstraint the AST constraint
   *
   * @return an isomorphic Constraint tree which can be persisted in the knowlege base
   */
  private Constraint visitASTConstraint(final ASTConstraint astConstraint) {
    //Preconditions
    assert astConstraint != null : "astConstraint must not be null";

    final Constraint constraint;
    try {
      constraint = (Constraint) astConstraint.jjtAccept(new ASTConstraintVisitor(), null);
    } catch (final VisitorException ex) {
      throw new TexaiException(ex);
    }
    return constraint;
  }

  /**
   * Returns a URI formed from the given qualified name.
   *
   * @param qName the given qualified name
   *
   * @return a URI formed from the given qualified name
   */
  private URI expandQNameToURI(final String qName) {
    //Preconditions
    assert qName != null : "qName must not be null";
    assert !qName.isEmpty() : "qName must not be empty";

    int index = qName.indexOf(":");
    if (index == -1) {
      return new URIImpl(baseNamespace + qName);
    } else {
      final String prefix = qName.substring(0, index);
      assert !prefix.isEmpty();
      final String localName = qName.substring(index + 1);
      assert !localName.isEmpty();
      final String namespace = prefixDictionary.get(prefix);
      assert namespace != null : "namespace not found for prefix: " + prefix + "\nprefixDictionary: " + prefixDictionary;
      return new URIImpl(namespace + localName);
    }
  }

  /**
   * Provides a visitor for the syntax tree headed by an ASTConstraint node.
   */
  public class ASTConstraintVisitor implements SyntaxTreeBuilderVisitor {

    /**
     * Constructs a new ASTConstraintVisitor instance.
     */
    public ASTConstraintVisitor() {
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(SimpleNode node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTQueryContainer node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTBaseDecl node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTPrefixDecl node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTSelectQuery node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTSelect node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTConstructQuery node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTConstruct node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTDescribeQuery node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTDescribe node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTAskQuery node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTDatasetClause node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTWhereClause node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTOrderClause node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTOrderCondition node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTLimit node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTOffset node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTGraphPatternGroup node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTBasicGraphPattern node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTOptionalGraphPattern node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTGraphGraphPattern node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTUnionGraphPattern node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTConstraint node.
     *
     * @param astConstraint the given ASTConstraint node
     * @param data the data (not used)
     *
     * @return the constructed Constraint instance
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTConstraint astConstraint, Object data) throws VisitorException {
      //Preconditions
      assert astConstraint != null : "astConstraint must not be null";
      assert astConstraint.jjtGetNumChildren() == 1 : "must be one child";

      final AbstractOperator operator = (AbstractOperator) astConstraint.jjtGetChild(0).jjtAccept(this, null);
      return new Constraint(operator);
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTFunctionCall node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTTriplesSameSubject node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTPropertyList node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTObjectList node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTIRI node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTBlankNodePropertyList node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTCollection node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTVar node.
     *
     * @param astVar the given ASTVar node
     * @param data the data (not used)
     *
     * @return the variable as a URI
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTVar astVar, Object data) throws VisitorException {
      //Preconditions
      assert astVar != null : "astVar must not be null";

      return new URIImpl(Constants.TEXAI_NAMESPACE + "?" + astVar.getName());
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTOr node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTAnd node.
     *
     * @param astAnd the given ASTAnd node
     * @param data the data (not used)
     *
     * @return the constructed AndOperator instance
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTAnd astAnd, Object data) throws VisitorException {
      //Preconditions
      assert astAnd != null : "astAnd must not be null";
      assert astAnd.jjtGetNumChildren() == 2 : "must have two children";

      final AbstractOperator arg1 = (AbstractOperator) astAnd.jjtGetChild(0).jjtAccept(this, null);
      final AbstractOperator arg2 = (AbstractOperator) astAnd.jjtGetChild(1).jjtAccept(this, null);
      return new AndOperator(arg1, arg2);
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTCompare node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTMath node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTNot node.
     *
     * @param astNot the given ASTNot node
     * @param data the data (not used)
     *
     * @return the constructed NotOperator instance
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTNot astNot, Object data) throws VisitorException {
      //Preconditions
      assert astNot != null : "astNot must not be null";
      assert astNot.jjtGetNumChildren() == 1 : "astNot must have one child";

      final AbstractOperator operator = (AbstractOperator) astNot.jjtGetChild(0).jjtAccept(this, null);
      return new NotOperator(operator);
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTNumericLiteral node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTStr node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTLang node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTLangMatches node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTDatatype node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTBound node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTSameTerm node.
     *
     * @param astSameTerm the given ASTSameTerm node
     * @param data the data (not used)
     *
     * @return the constructed SameTerm instance
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTSameTerm astSameTerm, Object data) throws VisitorException {
      //Preconditions
      assert astSameTerm != null : "astSameTerm must not be null";
      assert astSameTerm.jjtGetNumChildren() == 2 : "must have two children";

      final Value value1 = (Value) astSameTerm.jjtGetChild(0).jjtAccept(this, null);
      final Value value2 = (Value) astSameTerm.jjtGetChild(1).jjtAccept(this, null);
      return new SameTermOperator(value1, value2);
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTIsIRI node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTIsBlank node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTIsLiteral node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTRegexExpression node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTRDFLiteral node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTTrue node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTFalse node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTString node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Visits the given ASTQName node.
     *
     * @param astQName the given ASTQName node
     * @param data the data (not used)
     *
     * @return the equivalent URI
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTQName astQName, Object data) throws VisitorException {
      //Preconditions
      assert astQName != null : "astQName must not be null";

      return expandQNameToURI(astQName.getValue());
    }

    /**
     * Visits the given node.
     *
     * @param node the given node
     * @param data the data
     *
     * @return the constructed tree
     * @throws org.openrdf.query.parser.sparql.ast.VisitorException when an error occurs
     */
    @Override
    public Object visit(ASTBlankNode node, Object data) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTUpdateSequence astus, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTUpdateContainer astuc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTProjectionElem astpe, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTBindingsClause astbc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTInlineData astid, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTBindingSet astbs, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTBindingValue astbv, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTGroupClause astgc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTGroupCondition astgc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTHavingClause asthc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMinusGraphPattern astmgp, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTServiceGraphPattern astsgp, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTTriplesSameSubjectPath astsp, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPropertyListPath astplp, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPathAlternative astpa, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPathSequence astps, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPathElt astpe, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPathOneInPropertySet astps, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTPathMod astpm, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTInfix asti, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTCount astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSum astsum, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMin astmin, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMax astmax, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTAvg astavg, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSample asts, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTGroupConcat astgc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMD5 astmd5, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSHA1 astsha, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSHA224 astsha, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSHA256 astsha, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSHA384 astsha, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSHA512 astsha, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTNow astnow, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTYear asty, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMonth astm, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTDay astday, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTHours asth, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMinutes astm, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSeconds asts, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTTimezone astt, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTTz asttz, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTRand astr, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTAbs astabs, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTCeil astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTFloor astf, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTRound astr, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSubstr asts, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrLen astsl, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTUpperCase astuc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTLowerCase astlc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrStarts astss, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrEnds astse, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrBefore astsb, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrAfter astsa, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTReplace astr, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTConcat astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTContains astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTEncodeForURI astfr, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTIf astif, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTIn astin, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTNotIn astni, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTCoalesce astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTIsNumeric astin, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTBNodeFunc astbnf, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTIRIFunc astrf, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrDt astsd, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTStrLang astsl, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTUUID astd, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTSTRUUID a, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTBind astb, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTExistsFunc astef, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTNotExistsFunc astnef, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTGraphRefAll astgra, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTGraphOrDefault astgod, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTUnparsedQuadDataBlock astqdb, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTQuadsNotTriples astqnt, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTLoad astl, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTClear astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTDrop astd, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTAdd astadd, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTMove astm, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTCopy astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTCreate astc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTInsertData astid, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTDeleteData astdd, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTDeleteWhere astdw, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTDeleteClause astdc, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTInsertClause astic, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object visit(ASTModify astm, Object o) throws VisitorException {
      throw new UnsupportedOperationException("Not supported yet.");
    }

  }

  /**
   * Performs an example execution of this class.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final SPARQLParser sparqlParser = new SPARQLParser();
    final String queryString
            = "PREFIX rdf: <" + Constants.RDF_NAMESPACE + ">\n"
            + "PREFIX owl: <" + Constants.OWL_NAMESPACE + ">\n"
            + "PREFIX cyc: <" + Constants.CYC_NAMESPACE + ">\n"
            + "PREFIX texai: <" + Constants.TEXAI_NAMESPACE + ">\n"
            + "\n"
            + "SELECT ?individual ?Thing\n"
            + "WHERE {\n"
            + "  ?individual owl:sameAs ?individual .\n"
            + "  ?individual rdf:type ?Thing .\n"
            + "  ?individual rdf:type texai:FCGClauseSubject .\n"
            + "  ?individual rdf:type texai:IndefiniteThingInThisDiscourse .\n"
            + "  _:Situation_Localized rdf:type cyc:Situation-Localized .\n"
            + "  _:Situation_Localized cyc:situationConstituents ?individual .\n"
            + "  _:Situation_Localized texai:situationHappeningOnDate cyc:Now .\n"
            + "  FILTER (!sameTerm(?Thing, texai:FCGClauseSubject) && !sameTerm(?Thing, texai:IndefiniteThingInThisDiscourse))\n"
            + "}";
    LOGGER.info("queryString:\n" + queryString);
    LOGGER.info("");
    final QueryContainer queryContainer = sparqlParser.parseQuery(queryString, "test query");
    LOGGER.info("queryContainer:\n" + queryContainer.toDetailedString());
  }

}
