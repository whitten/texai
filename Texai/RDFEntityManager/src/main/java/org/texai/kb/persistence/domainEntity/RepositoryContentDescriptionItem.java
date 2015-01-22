/*
 * RepositoryContentDescriptionItem.java
 *
 * Created on May 1, 2009, 5:31:51 PM
 *
 * Description: Provides a container for describing the persistent class whose instances are persisted in a specified repository.
 *
 * Copyright (C) May 1, 2009 Stephen L. Reed.
 */
package org.texai.kb.persistence.domainEntity;

import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a container for describing the persistent class whose instances are persisted in a specified repository.
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:RepositoryContext")
@NotThreadSafe
public class RepositoryContentDescriptionItem implements Comparable<RepositoryContentDescriptionItem>, CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the knowledge base class, which could also be associated with a persistent Java class */
  @RDFProperty(predicate = "texai:repositoryClassContextInfoClass")
  private final URI classTerm;

  /** Constructs a new RepositoryContentDescriptionItem instance. */
  public RepositoryContentDescriptionItem() {
    classTerm = null;
  }

  /** Constructs a new RepositoryContentDescriptionItem instance.
   *
   * @param classTerm the knowledge base class, which could also be associated with a persistent Java class
   */
  @SuppressWarnings("unchecked")
  public RepositoryContentDescriptionItem(final URI classTerm) {
    //Preconditions
    assert classTerm != null : "classTerm must not be null";

    this.classTerm = classTerm;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the knowledge base class, which could also be associated with a persistent Java class.
   *
   * @return the knowledge base class, which could also be associated with a persistent Java class
   */
  public URI getClassTerm() {
    return classTerm;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final RepositoryContentDescriptionItem other = (RepositoryContentDescriptionItem) obj;
    return this.classTerm.equals(other.classTerm);
  }


  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  @SuppressWarnings(value = "PMD")
  public int hashCode() {
    int hash = 7;
    hash = 31 * hash + (this.classTerm != null ? this.classTerm.hashCode() : 0);
    return hash;
  }

  /** Compares another RepositoryContentDescriptionItem instance with this one, and orders by class name.
   *
   * @param that the other RDFRepository
   * @return -1 if less than, 0 if equal to, otherwise return +1
   */
  @Override
  public int compareTo(final RepositoryContentDescriptionItem that) {
    //Preconditions
    assert that != null : "that must not be null";

    if (this.getClassTerm() == null || that.getClassTerm() == null) {
      return 0;
    } else {
      return this.getClassTerm().toString().compareTo(that.getClassTerm().toString());
    }
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

    rdfEntityManager.persist(this, overrideContext);
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

    rdfEntityManager.remove(this);
  }
}
