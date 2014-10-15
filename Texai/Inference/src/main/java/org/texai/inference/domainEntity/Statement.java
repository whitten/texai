/*
 * Statement.java
 *
 * Created on Mar 17, 2008, 11:13:53 AM
 *
 * Description: Provides a reified RDF statement.
 *
 * Copyright (C) Mar 17, 2008 Stephen L. Reed.
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

import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.texai.kb.Constants;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/** Provides a reified RDF statement.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(namespaces = {
@RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
@RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)
}, subject = "texai:org.texai.inference.domainEntity.Statement", type = "cyc:ObjectType", subClassOf = "cyc:AbstractInformationStructure", context = "texai:EnglishConstructionGrammarDomainContext")
@Immutable
public class Statement implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;                                          // NOPMD
  /** the subject */
  @RDFProperty(predicate = "texai:infStatementSubject")
  private final Resource subject;
  /** the predicate */
  @RDFProperty(predicate = "texai:infStatementPredicate")
  private final URI predicate;
  /** the object */
  @RDFProperty(predicate = "texai:infStatementObject")
  private final Object object;

  /** Constructs a new Statement instance. */
  public Statement() {
    subject = null;
    predicate = null;
    object = null;
  }

  /** Constructs a new RDF Statement instance.
   *
   * @param subject the subject
   * @param predicate the predicate
   * @param object the object
   */
  public Statement(
          final Resource subject,
          final URI predicate,
          final Value object) {
    //Preconditions
    assert subject != null : "subject must not be null";
    assert predicate != null : "predicate must not be null";
    assert object != null : "object must not be null";

    this.subject = subject;
    this.predicate = predicate;
    this.object = object;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the subject.
   *
   * @return the subject
   */
  public Resource getSubject() {
    return subject;
  }

  /** Returns whether the subject is a variable.
   *
   * @return whether the subject is a variable
   */
  public boolean isSubjectAVariable() {
    return subject instanceof URI && ((URI) subject).getLocalName().startsWith("?");
  }

  /** Gets the predicate.
   *
   * @return the predicate
   */
  public URI getPredicate() {
    return predicate;
  }

  /** Gets the object.
   *
   * @return the object
   */
  public Value getObject() {
    return (Value) object;
  }

  /** Returns whether the object is a variable.
   *
   * @return whether the object is a variable
   */
  public boolean isObjectAVariable() {
    return object instanceof URI && ((URI) object).getLocalName().startsWith("?");
  }

  /** Gets the RDF statement represented by this object.
   *
   * @return the RDF statement represented by this object
   */
  public org.openrdf.model.Statement getRDFStatement() {
    return new StatementImpl(subject, predicate, getObject());
  }

  /** Makes a new Sesame Statement from the given reified statement.
   *
   * @param rdfStatement the RDF statement
   * @return a new statement instance
   */
  public static Statement getStatement(final org.openrdf.model.Statement rdfStatement) {
    return new Statement(rdfStatement.getSubject(), rdfStatement.getPredicate(), rdfStatement.getObject());
  }

  /** Returns a SPARQL representation of this statement.
   *
   * @return a SPARQL representation of this statement
   */
  public String toSparql() {
    final StringBuilder stringBuilder = new StringBuilder();
    if (getSubject() instanceof URI) {
      if (((URI) getSubject()).getLocalName().startsWith("?")) {
        stringBuilder.append(((URI) getSubject()).getLocalName());
      } else {
        stringBuilder.append("<");
        stringBuilder.append(getSubject().toString());
        stringBuilder.append(">");
      }
    } else {
      stringBuilder.append(getSubject().toString());
    }
    stringBuilder.append(" ");
    if (getPredicate().getLocalName().startsWith("?")) {
      stringBuilder.append(getPredicate().getLocalName());
    } else {
      stringBuilder.append("<");
      stringBuilder.append(getPredicate().toString());
      stringBuilder.append(">");
    }
    stringBuilder.append(" ");
    if (getObject() instanceof URI) {
      if (((URI) getObject()).getLocalName().startsWith("?")) {
        stringBuilder.append(((URI) getObject()).getLocalName());
      } else {
        stringBuilder.append("<");
        stringBuilder.append(getObject().toString());
        stringBuilder.append(">");
      }
    } else {
      stringBuilder.append(getObject().toString());
    }
    stringBuilder.append(" .");
    return stringBuilder.toString();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    if (getSubject() instanceof URI) {
      stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) getSubject()));
    } else {
      stringBuilder.append(getSubject().toString());
    }
    stringBuilder.append(" ");
    stringBuilder.append(RDFUtility.formatURIAsTurtle(getPredicate()));

    stringBuilder.append(" ");
    if (getObject() instanceof URI) {
      stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) getObject()));
    } else {
      stringBuilder.append(getObject().toString());
    }
    stringBuilder.append(" .");
    return stringBuilder.toString();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  public String toParenthesizedString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('(');
    if (getSubject() instanceof URI) {
      stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) getSubject()));
    } else {
      stringBuilder.append(getSubject().toString());
    }
    stringBuilder.append(" ");
    stringBuilder.append(RDFUtility.formatURIAsTurtle(getPredicate()));

    stringBuilder.append(" ");
    if (getObject() instanceof URI) {
      stringBuilder.append(RDFUtility.formatURIAsTurtle((URI) getObject()));
    } else {
      stringBuilder.append(getObject().toString());
    }
    stringBuilder.append(')');
    return stringBuilder.toString();
  }

  /** Returns whether some other object is equal to this one.
   *
   * @param obj the other object
   * @return whether the other object is equal to this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Statement) {
      final Statement that = (Statement) obj;
      return this.subject.equals(that.subject) && this.predicate.equals(that.predicate) && this.object.equals(that.object);
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
    return subject.hashCode() + predicate.hashCode() + object.hashCode();
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
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    final RDFPersistent rootRDFEntity1 = rdfEntityManager.possibleNewRoot(rootRDFEntity, this);
    rdfEntityManager.persist(rootRDFEntity1, this, overrideContext);
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
    rdfEntityManager.remove(rootRDFEntity1, this);
  }

}
