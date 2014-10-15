/*
 * Select.java
 *
 * Created on Feb 8, 2009, 9:10:46 AM
 *
 * Description: Provides a SELECT clause.
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

import java.util.List;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SELECT clause
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class Select implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the ID assigned by the RDF persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the indicator whether this is a select DISTINCT clause */
  @RDFProperty(trueClass = "texai:org.texai.inference.domainEntity.Select_IsDistinct", falseClass = "texai:org.texai.inference.domainEntity.Select_IsNotDistinct")
  private final boolean isDistinct;
  /** the indicator whether this is a select REDUCED clause */
  @RDFProperty(trueClass = "texai:org.texai.inference.domainEntity.Select_IsReduced", falseClass = "texai:org.texai.inference.domainEntity.Select_IsNotReduced")
  private final boolean isReduced;
  /** the indicator whether this is a select * clause */
  @RDFProperty(trueClass = "texai:org.texai.inference.domainEntity.Select_IsDistinct", falseClass = "texai:org.texai.inference.domainEntity.Select_IsNotDistinct")
  private final boolean isWildcard;
  /** the selected variables */
  @RDFProperty(predicate = "texai:sparqlSelectVariableList")
  private final List<Variable> variables;

  /** Constructs a new Select instance. */
  public Select() {
    isDistinct = false;
    isReduced = false;
    isWildcard = false;
    variables = null;
  }

  /** Constructs a new Select instance.
   *
   * @param isDistinct the indicator whether this is a select DISTINCT clause
   * @param isReduced the indicator whether this is a select REDUCED clause
   * @param isWildcard the indicator whether this is a select * clause
   * @param variables the selected variables
   */
  public Select(
          final boolean isDistinct,
          final boolean isReduced,
          final boolean isWildcard,
          final List<Variable> variables) {
    //Preconditions
    assert variables != null : "variables must not be null";

    this.isDistinct = isDistinct;
    this.isReduced = isReduced;
    this.isWildcard = isWildcard;
    this.variables = variables;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
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
    final Select other = (Select) obj;
    if (this.isDistinct != other.isDistinct) {
      return false;
    }
    if (this.isReduced != other.isReduced) {
      return false;
    }
    if (this.isWildcard != other.isWildcard) {
      return false;
    }
    if (this.variables != other.variables && (this.variables == null || !this.variables.equals(other.variables))) {
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
    hash = 17 * hash + (this.isDistinct ? 1 : 0);
    hash = 17 * hash + (this.isReduced ? 1 : 0);
    hash = 17 * hash + (this.isWildcard ? 1 : 0);
    hash = 17 * hash + (this.variables != null ? this.variables.hashCode() : 0);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("SELECT ");
    if (isDistinct) {
      stringBuilder.append("DISTINCT ");
    }
    if (isReduced) {
      stringBuilder.append("REDUCED ");
    }
    if (isWildcard) {
      stringBuilder.append("* ");
    }
    boolean isFirst = true;
    for (final Variable variable : variables) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(" ");
      }
      stringBuilder.append(variable.toString());
    }


    return stringBuilder.toString();
  }

  /** Returns whether this is a select DISTINCT clause.
   *
   * @return whether this is a select DISTINCT clause
   */
  public boolean isDistinct() {
    return isDistinct;
  }

  /** Returns whether this is a select REDUCED clause.
   *
   * @return whether this is a select REDUCED clause
   */
  public boolean isReduced() {
    return isReduced;
  }

  /** Returns whether this is a select * clause.
   *
   * @return whether this is a select * clause
   */
  public boolean isWildcard() {
    return isWildcard;
  }

  /** Returns the selected variables.
   *
   * @return the selected variables
   */
  public List<Variable> getVariables() {
    return variables;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final Variable variable : variables) {
      variable.instantiate();
    }
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
    for (final Variable variable : variables) {
      variable.cascadePersist(
            rootRDFEntity1,
            rdfEntityManager,
            overrideContext);
    }
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
    for (final Variable variable : variables) {
      variable.cascadeRemove(
            rootRDFEntity1,
            rdfEntityManager);
    }
    rdfEntityManager.remove(rootRDFEntity1, this);
  }
}
