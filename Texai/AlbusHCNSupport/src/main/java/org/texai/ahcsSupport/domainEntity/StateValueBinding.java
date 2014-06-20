/*
 * StateValueBinding.java
 *
 * Created on Oct 18, 2011, 10:29:21 AM
 *
 * Description: Contains a state variable name and value binding.
 *
 * Copyright (C) Oct 18, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ahcsSupport.domainEntity;

import java.util.Objects;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Contains a state variable name and value binding.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class StateValueBinding implements CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;
  /** the variable name */
  @RDFProperty()
  private final String variableName;
  /** the value */
  @RDFProperty()
  private Object value;

  /** Constructs a new StateValueBinding instance. */
  public StateValueBinding() {
    variableName = null;
    value = null;
  }

  /** Constructs a new StateValueBinding instance.
   *
   * @param variableName the variable name
   * @param value the value
   */
  public StateValueBinding(
          final String variableName,
          final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    this.variableName = variableName;
    this.value = value;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the variable name.
   *
   * @return the the variable name
   */
  public String getVariableName() {
    return variableName;
  }

  /** Gets the value.
   * @return the value
   */
  public Object getValue() {
    return value;
  }

  /** Sets the value.
   *
   * @param value the value
   */
  public void setValue(final Object value) {
    this.value = value;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder()).append('[').append(variableName).append(" --> ").append(value).append(']').toString();
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
    final StateValueBinding other = (StateValueBinding) obj;
    return Objects.equals(this.variableName, other.variableName);
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.variableName);
    return hash;
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
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
  public void cascadePersist(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager, URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.persist(this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.remove(this);
  }
}
