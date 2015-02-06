/*
 * Repository.java
 *
 * Created on May 1, 2009, 1:45:05 PM
 *
 * Description: Provides a container for information about a Sesame RDF repository.
 *
 * Copyright (C) May 1, 2009 Stephen L. Reed.
 */
package org.texai.kb.persistence.domainEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.repository.Repository;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a container for information about a Sesame RDF repository.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:RepositoryContext")
@NotThreadSafe
public class RepositoryContentDescription implements Comparable<RepositoryContentDescription>, CascadePersistence {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the repository name */
  @RDFProperty(predicate = "texai:repositoryName")
  private final String repositoryName;
  /** the repository indices specification */
  @RDFProperty(predicate = "texai:repositoryIndices")
  private final String indices;
  /** the information objects about knowledge base classes whose contextualized instances are persisted in this repository */
  @RDFProperty(predicate = "texai:repositoryContextDescriptionItems", range = "texai:org.texai.kb.persistence.domainEntity.RepositoryContentDescriptionItem")
  private final Set<RepositoryContentDescriptionItem> repositoryContextDescriptionItems;
  /** the cached repository instance */
  private transient Repository repository;

  //TODO externalize

  /** Constructs a new Repository instance. */
  public RepositoryContentDescription() {
    repositoryName = null;
    repositoryContextDescriptionItems = null;
    indices = null;
  }

  /** Constructs a new Repository instance.
   *
   * @param repositoryName the repository name
   * @param indices the repository indices specification
   * @param repositoryContextDescriptionItems the information objects about knowledge base classes whose
   * contextualized instances are persisted in this repository
   */
  public RepositoryContentDescription(
          final String repositoryName,
          final String indices,
          final Set<RepositoryContentDescriptionItem> repositoryContextDescriptionItems) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert indices != null : "indices must not be null";
    assert !indices.isEmpty() : "indices must not be empty";
    assert repositoryContextDescriptionItems != null : "repositoryContextDescriptionItems must not be null";

    this.repositoryName = repositoryName;
    this.indices = indices;
    this.repositoryContextDescriptionItems = repositoryContextDescriptionItems;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the repository name.
   *
   * @return the repository name
   */
  public String getRepositoryName() {
    return repositoryName;
  }

  /** Gets the information objects about knowledge base classes whose contextuallized instances are persisted in this repository.
   *
   * @return the information objects about knowledge base classes whose contextuallized instances are persisted in this repository
   */
  public Set<RepositoryContentDescriptionItem> getRepositoryContentDescriptionItems() {
    return repositoryContextDescriptionItems;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object.
   */
  @Override
  public String toString() {
    return "[" + repositoryName + "]";
  }

  /** Returns the given RDF repository descriptions as a symbolic expression.
   *
   * @param rdfRepositories the given RDF repository descriptions
   * @return the given RDF repository descriptions as a symbolic expression, suitable for parsing
   */
  public static String toString(final Set<RepositoryContentDescription> rdfRepositories) {
    //Preconditions
    assert rdfRepositories != null : "rdfRepositories must not be null";

    //TODO handle other repository description features
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("(");
    boolean isFirst = true;
    final List<RepositoryContentDescription> orderedRepositoryContentDescriptions = new ArrayList<>(rdfRepositories);
    Collections.sort(orderedRepositoryContentDescriptions);
    for (final RepositoryContentDescription orderedRepositoryContentDescription : orderedRepositoryContentDescriptions) {
      if (isFirst) {
        isFirst = false;
      } else {
       stringBuilder.append("\n ");
      }
      stringBuilder.append("(");
      stringBuilder.append(orderedRepositoryContentDescription.getRepositoryName());
      stringBuilder.append("\n  (indices: \"");
      stringBuilder.append(orderedRepositoryContentDescription.indices);
      stringBuilder.append("\")");
      final List<RepositoryContentDescriptionItem> orderedRepositoryClassContextInfos =
              new ArrayList<>(orderedRepositoryContentDescription.getRepositoryContentDescriptionItems());
    Collections.sort(orderedRepositoryClassContextInfos);
      for (final RepositoryContentDescriptionItem repositoryClassContextInfo : orderedRepositoryClassContextInfos) {
        stringBuilder.append("\n  (className: ");
        stringBuilder.append(repositoryClassContextInfo.getClassTerm().getLocalName());
        stringBuilder.append(")");
      }
    }
    stringBuilder.append(")");
    return stringBuilder.toString();
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object.
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof RepositoryContentDescription) {
      final RepositoryContentDescription that = (RepositoryContentDescription) obj;
      return this.repositoryName.equals(that.repositoryName);
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
    int hash = 5;
    hash = 83 * hash + (this.repositoryName != null ? this.repositoryName.hashCode() : 0);    // NOPMD
    return hash;
  }

  /** Gets the cached repository instance.
   *
   * @return the cached repository instance
   */
  public Repository getRepository() {
    return repository;
  }

  /** Sets the cached repository instance.
   *
   * @param repository the cached repository instance
   */
  public void setRepository(final Repository repository) {
    //Preconditions
    assert repository != null : "repository must not be null";

    this.repository = repository;
  }

  /** Compares another RepositoryContentDescription instance with this one, and orders by name.
   *
   * @param that the other RepositoryContentDescription
   * @return -1 if less than, 0 if equal to, otherwise return +1
   */
  @Override
  public int compareTo(final RepositoryContentDescription that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.getRepositoryName().compareTo(that.getRepositoryName());
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final RepositoryContentDescriptionItem repositoryContentDescriptionItem : getRepositoryContentDescriptionItems()) {
      repositoryContentDescriptionItem.instantiate();
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

    for (final RepositoryContentDescriptionItem repositoryContentDescriptionItem : getRepositoryContentDescriptionItems()) {
      repositoryContentDescriptionItem.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    }
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

    for (final RepositoryContentDescriptionItem repositoryContentDescriptionItem : getRepositoryContentDescriptionItems()) {
      repositoryContentDescriptionItem.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    }
    rdfEntityManager.remove(this);
  }

  /** Gets the repository indices specification.
   *
   * @return the repository indices specification
   */
  public String getIndices() {
    return indices;
  }
}
