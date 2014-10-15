/*
 * Rule.java
 *
 * Created on Mar 14, 2008, 11:42:10 AM
 *
 * Description: Provides an immutable deductive inference rule for the Texai KB.
 *
 * Copyright (C) Mar 14, 2008 Stephen L. Reed.
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
package org.texai.inference.domainEntity;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;


//TODO modify antecedant to be a SPARQL query

/** Provides an immutable deductive inference rule for the Texai KB.
 * <code>
 * (
 * description: "if there is a room then it is likely that a table is in the room"
 * context: InferenceRuleTestContext
 * if:
 *    ?situation-localized rdf:type cyc:Situation-Localized .
 *    ?room rdf:type cyc:RoomInAConstruction .
 *    ?situation-localized cyc:situationConstituents ?room .
 * then:
 *    _:in-completely-situation-localized rdf:type InCompletelySituationLocalized .
 *    ?situation-localized likelySubSituations _:in-completely-situation-localized .
 *    _:table rdf:type cyc:Table_PieceOfFurniture .
 *    :table in-ContCompletely ?room .
 * )
 * </code>
 *
 * @author Stephen L. Reed
 */
@RDFEntity(namespaces = {
@RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
@RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)
}, subject = "texai:org.texai.inference.domainEntity.Rule", type = "cyc:ObjectType", subClassOf = "cyc:AbstractInformationStructure", context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class Rule implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the rule definition in English */
  @RDFProperty(predicate = "texai:infRuleDefinitionInEnglish")
  private final String ruleDefinitionInEnglish;
  /** the ordered list of RDF statements that constitute the antecedant clauses of the horn rule */
  @RDFProperty(predicate = "texai:infRuleAntecedantStatementList")
  private final List<Statement> antecedantStatements;
  /** the ordered list of RDF statements that constitute the consequent clauses of the horn rule */
  @RDFProperty(predicate = "texai:infRuleConsequentStatementList")
  private final List<Statement> consequentStatements;
  /** the context in which this rule applies */
  @RDFProperty(predicate = "texai:infRuleContext")
  private final URI context;

  /** Constructs a new Rule instance. */
  public Rule() {
    ruleDefinitionInEnglish = null;
    antecedantStatements = null;
    consequentStatements = null;
    context = null;
  }

  /** Constructs a new Rule instance.
   *
   * @param ruleDefinitionInEnglish the rule definition in English
   * @param antecedantStatements the list of RDF statements that constitute the antecedant clauses of the rule
   * @param consequentStatements the set of RDF statements that constitute the consequent clause of the rule
   * @param context the context in which this rule applies
   */
  public Rule(
          final String ruleDefinitionInEnglish,
          final List<org.openrdf.model.Statement> antecedantStatements,
          final List<org.openrdf.model.Statement> consequentStatements,
          final URI context) {
    //Preconditions
    assert ruleDefinitionInEnglish != null : "ruleDefinitionInEnglish must not be null";
    assert !ruleDefinitionInEnglish.isEmpty() : "ruleDefinitionInEnglish must not be empty";
    assert antecedantStatements != null : "antecedantStatements must not be null";
    assert !antecedantStatements.isEmpty() : "antecedantStatements must not be null";
    assert consequentStatements != null : "consequentStatements must not be null";
    assert !consequentStatements.isEmpty() : "consequentStatements must not be empty";

    this.ruleDefinitionInEnglish = ruleDefinitionInEnglish;
    this.antecedantStatements = new ArrayList<>(antecedantStatements.size());
    // transform Sesame RDF statements to their reified form for persistence
    for (final org.openrdf.model.Statement rdfStatement : antecedantStatements) {
      this.antecedantStatements.add(Statement.getStatement(rdfStatement));
    }
    this.consequentStatements = new ArrayList<>(consequentStatements.size());
    for (final org.openrdf.model.Statement rdfStatement : consequentStatements) {
      this.consequentStatements.add(Statement.getStatement(rdfStatement));
    }
    this.context = context;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the rule definition in English.
   *
   * @return the rule definition in English
   */
  public String getRuleDefinitionInEnglish() {
    return ruleDefinitionInEnglish;
  }

  /** Gets the list of RDF statements that constitute the antecedant clauses of the rule.
   *
   * @return the list of RDF statements that constitute the antecedant clauses of the rule
   */
  public List<org.openrdf.model.Statement> getAntecedantStatements() {
    final List<org.openrdf.model.Statement> antecedantRDFStatements = new ArrayList<>(antecedantStatements.size());
    for (final Statement statement : antecedantStatements) {
      antecedantRDFStatements.add(statement.getRDFStatement());
    }
    return antecedantRDFStatements;
  }

  /** Gets the set of RDF statements that constitute the consequent clause of the rule.
   *
   * @return the set of RDF statements that constitute the consequent clause of the rule
   */
  public List<org.openrdf.model.Statement> getConsequentStatements() {
    final List<org.openrdf.model.Statement> consequentRDFStatements = new ArrayList<>();
    for (final Statement statement : consequentStatements) {
      consequentRDFStatements.add(statement.getRDFStatement());
    }
    return consequentRDFStatements;
  }

  /** Gets the context in which this rule applies.
   *
   * @return the context in which this rule applies
   */
  public URI getContext() {
    return context;
  }

  /** Returns whether some other object is equal to this one.
   *
   * @param obj the other object
   * @return whether some other object is equal to this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Rule) {
      final Rule that = (Rule) obj;
      return this.antecedantStatements.equals(that.antecedantStatements) &&
              this.consequentStatements.equals(that.consequentStatements) &&
              this.context.equals(that.context);
    } else {
      return false;
    }
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return antecedantStatements.hashCode() + consequentStatements.hashCode() + context.hashCode();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("(\ndescription \"");
    stringBuilder.append(ruleDefinitionInEnglish);
    stringBuilder.append("\"\ncontext: ");
    stringBuilder.append(RDFUtility.formatURIAsTurtle(context));
    stringBuilder.append("\nif:\n");
    for (final Statement antecedantStatement : antecedantStatements) {
      stringBuilder.append("  ");
      stringBuilder.append(antecedantStatement.toString());
      stringBuilder.append("\n");
    }
    stringBuilder.append("then:\n");
    for (final Statement consequentStatement : consequentStatements) {
      stringBuilder.append("  ");
      stringBuilder.append(consequentStatement.toString());
      stringBuilder.append("\n");
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final Statement statement : antecedantStatements) {
      statement.instantiate();
    }
    for (final Statement statement : consequentStatements) {
      statement.instantiate();
    }
  }


  /** Recursively persists this RDF entity and all its components.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  public void cascadePersist(
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadePersist(this, rdfEntityManager, overrideContext);
  }


  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    for (final Statement statement : antecedantStatements) {
      statement.cascadePersist(
          rootRDFEntity1,
          rdfEntityManager,
          overrideContext);
    }
    for (final Statement statement : consequentStatements) {
      statement.cascadePersist(
          rootRDFEntity1,
          rdfEntityManager,
          overrideContext);
    }
    rdfEntityManager.persist(rootRDFEntity1, this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public void cascadeRemove(
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadeRemove(this, rdfEntityManager);
  }

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    for (final Statement statement : antecedantStatements) {
      statement.cascadeRemove(
          rootRDFEntity1,
          rdfEntityManager);
    }
    for (final Statement statement : consequentStatements) {
      statement.cascadeRemove(
          rootRDFEntity1,
          rdfEntityManager);
    }
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
