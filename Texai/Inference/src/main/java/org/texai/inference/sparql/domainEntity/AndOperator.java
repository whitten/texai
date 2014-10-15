/*
 * AndOperator.java
 *
 * Created on Feb 8, 2009, 10:47:19 PM
 *
 * Description: Provides a SPARQL && operator.
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
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL && operator.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class AndOperator extends AbstractOperator {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the first argument */
  @RDFProperty(predicate = "texai:sparqlAndOperatorArg1")
  private final AbstractOperator arg1;
  /** the second argument */
  @RDFProperty(predicate = "texai:sparqlAndOperatorArg2")
  private final AbstractOperator arg2;

  /** Constructs a new AndOperator instance. */
  public AndOperator() {
    arg1 = null;
    arg2 = null;
  }

  /** Constructs a new AndOperator instance.
   * @param arg1 the first argument
   * @param arg2 the second argument
   */
  public AndOperator(final AbstractOperator arg1, final AbstractOperator arg2) {
    //Preconditions
    assert arg1 != null : "arg1 must not be null";
    assert arg2 != null : "arg2 must not be null";

    this.arg1 = arg1;
    this.arg2 = arg2;
  }

  /** Gets the first argument.
   *
   * @return the first argument
   */
  public AbstractOperator getArg1() {
    return arg1;
  }

  /** Gets the second argument.
   *
   * @return the second argument
   */
  public AbstractOperator getArg2() {
    return arg2;
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
    final AndOperator other = (AndOperator) obj;
    if (this.arg1 != other.arg1 && (this.arg1 == null || !this.arg1.equals(other.arg1))) {
      return false;
    }
    if (this.arg2 != other.arg2 && (this.arg2 == null || !this.arg2.equals(other.arg2))) {
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
    int hash = 3;
    hash = 29 * hash + (this.arg1 != null ? this.arg1.hashCode() : 0);
    hash = 29 * hash + (this.arg2 != null ? this.arg2.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return arg1.toString() + " && " + arg2.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    arg1.instantiate();
    arg2.instantiate();
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
    arg1.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    arg2.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
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
    arg1.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    arg2.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
