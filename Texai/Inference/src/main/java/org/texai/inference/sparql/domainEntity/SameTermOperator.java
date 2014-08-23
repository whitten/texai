/*
 * SameTermOperator.java
 *
 * Created on Feb 8, 2009, 10:52:03 PM
 *
 * Description: Provides a SPARQL sameTerm operator.
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

package org.texai.inference.sparql.domainEntity;


import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/** Provides a SPARQL sameTerm operator.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class SameTermOperator extends AbstractOperator {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the first term */
  @RDFProperty(predicate = "texai:sparqlSameTermOperatorTerm1")
  private final Value term1;
  /** the second term */
  @RDFProperty(predicate = "texai:sparqlSameTermOperatorTerm2")
  private final Value term2;

  /** Constructs a new SameTermOperator instance. */
  public SameTermOperator() {
    term1 = null;
    term2 = null;
  }

  /** Constructs a new SameTermOperator instance.
   * @param term1 the first term
   * @param term2 the second term
   */
  public SameTermOperator(final Value term1, final Value term2) {
    //Preconditions
    assert term1 != null : "term1 must not be null";
    assert term2 != null : "term2 must not be null";

    this.term1 = term1;
    this.term2 = term2;
  }

  /** Gets the first term.
   *
   * @return the first term
   */
  public Value getTerm1() {
    return term1;
  }

  /** Gets the second term.
   *
   * @return the second term
   */
  public Value getTerm2() {
    return term2;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SameTermOperator other = (SameTermOperator) obj;
    if (this.term1 != other.term1 && (this.term1 == null || !this.term1.equals(other.term1))) {
      return false;
    }
    if (this.term2 != other.term2 && (this.term2 == null || !this.term2.equals(other.term2))) {
      return false;
    }
    return true;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 61 * hash + (this.term1 != null ? this.term1.hashCode() : 0);
    hash = 61 * hash + (this.term2 != null ? this.term2.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("sameTerm(");
    if (term1 instanceof URI) {
      if (((URI) term1).getLocalName().startsWith("?")) {
        stringBuilder.append(((URI) term1).getLocalName());
      } else {
        stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) term1));
      }
    } else {
      stringBuilder.append(term1.toString());
    }
    stringBuilder.append(", ");
    if (term2 instanceof URI) {
      if (((URI) term2).getLocalName().startsWith("?")) {
        stringBuilder.append(((URI) term2).getLocalName());
      } else {
        stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) term2));
      }
    } else {
      stringBuilder.append(term2.toString());
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
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
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    rdfEntityManager.persist(rootRDFEntity1, this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(
          final RDFPersistent rootRDFEntity,
          final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
