/*
 * SubPropertyOfQueries.java
 *
 * Created on Jan 7, 2011, 1:46:43 PM
 *
 * Description: Provides subPropertyOf inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * Copyright (C) Jan 7, 2011, Stephen L. Reed.
 *
 */
package org.texai.subsumptionReasoner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.ArraySet;
import org.texai.util.LRUMap;
import org.texai.util.LRUSet;
import org.texai.util.TexaiException;

/** Provides subPropertyOf inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * @author reed
 */
@NotThreadSafe
public class SubPropertyOfQueries {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SubPropertyOfQueries.class);
  /** the query */
  private static final String SUBCLASSOF_QUERY_STRING = "SELECT s, o FROM {s} <" + Constants.RDFS_NAMESPACE + "subPropertyOf> {o}";
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the isSubPropertyOf cache */
  private final IsSubPropertyOfCache isSubPropertyOfCache = new IsSubPropertyOfCache();
  /** the super property cache */
  private final SuperPropertyCache superPropertyCache = new SuperPropertyCache();

  /** Constructs a new SubPropertyOfQueries instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public SubPropertyOfQueries(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }
  /** Returns whether the given term is directly a subproperty of of the given property (predicate) term.
   *
   * @param repositoryName the repository name
   * @param property the given property
   * @param superPropertyTerm the given super property
   * @return whether the given term is directly a subproperty of of the given property (predicate) term
   */
  public boolean isDirectSubPropertyOf(
          final String repositoryName,
          final URI property,
          final URI superPropertyTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert property != null : "property must not be null";
    assert superPropertyTerm != null : "superPropertyTerm must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    boolean isDirectSubPropertyOf;
    try {
      final TupleQuery subPropertyOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subPropertyOfTupleQuery.setBinding("s", property);
      subPropertyOfTupleQuery.setBinding("o", superPropertyTerm);
      final TupleQueryResult tupleQueryResult = subPropertyOfTupleQuery.evaluate();
      isDirectSubPropertyOf = tupleQueryResult.hasNext();
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return isDirectSubPropertyOf;
  }

  /** Returns whether the first property is directly or indirectly a subproperty of of the second property.
   *
   * @param repositoryName the repository name
   * @param property1 the first property term
   * @param property2 the second property term
   * @return  whether the first property term is directly or indirectly a subproperty of of the second property term
   */
  public boolean isSubPropertyOf(
          final String repositoryName,
          final URI property1,
          final URI property2) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert property1 != null : "term must not be null";
    assert property2 != null : "propertyTerm must not be null";

    final List<URI> key = new ArrayList<>(2);
    if (repositoryName.equals("OpenCyc")) {
      key.add(property1);
      key.add(property2);
        if (isSubPropertyOfCache.get().contains(key)) {
          return true;
        }
      }
    final boolean isSubPropertyOf = isSubPropertyOf(
            repositoryName,
            property1,
            property2,
            new HashSet<>());
    if (isSubPropertyOf && repositoryName.equals("OpenCyc")) {
        isSubPropertyOfCache.get().add(key);
    }
    return isSubPropertyOf;
  }

  /** Returns whether the first given property is directly or indirectly a subproperty of the second given property.
   *
   * @param repositoryName the repository name
   * @param property1 the given term
   * @param property2 the given property term
   * @param visitedPropertyTerms the visited property terms
   * @return whether the first given property is directly or indirectly a subproperty of the second given property
   */
  private boolean isSubPropertyOf(
          final String repositoryName,
          final URI property1,
          final URI property2,
          final Set<URI> visitedPropertyTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert property1 != null : "property1 must not be null";
    assert property2 != null : "property2 must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      final TupleQuery subPropertyOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subPropertyOfTupleQuery.setBinding("s", property1);
      final TupleQueryResult tupleQueryResult = subPropertyOfTupleQuery.evaluate();
      final List<URI> superPropertyTerms = new ArrayList<>();
      boolean isSubPropertyOf = false;
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI superPropertyTerm = (URI) bindingSet.getBinding("o").getValue();
        if (property2.equals(superPropertyTerm)) {
          isSubPropertyOf = true;
          break;
        } else if (!visitedPropertyTerms.contains(superPropertyTerm)) {
          visitedPropertyTerms.add(superPropertyTerm);
          superPropertyTerms.add(superPropertyTerm);
        }
      }
      tupleQueryResult.close();
      if (isSubPropertyOf) {
        return true;
      }
      for (final URI superPropertyTerm : superPropertyTerms) {
        if (isSubPropertyOf(
                repositoryName,
                superPropertyTerm,
                property2,
                visitedPropertyTerms)) {
          return true;
        }
      }
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return false;
  }

  /** Returns the direct superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct superclasses of the given term
   */
  public Set<URI> getDirectSuperProperties(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    final Set<URI> directSuperPropertyTerms = new ArraySet<>();
    try {
      final TupleQuery subPropertyOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subPropertyOfTupleQuery.setBinding("s", term);
      final TupleQueryResult tupleQueryResult = subPropertyOfTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        directSuperPropertyTerms.add((URI) bindingSet.getBinding("o").getValue());
      }
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
//    LOGGER.info("getDirectSuperProperties for " + RDFUtility.formatResource(term)
//            + ", directSuperPropertyTerms: " + RDFUtility.formatResources(directSuperPropertyTerms));
    return directSuperPropertyTerms;
  }

  /** Returns the direct and indirect superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct and indirect superclasses of the given term
   */
  public Set<URI> getSuperProperties(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    Set<URI> superPropertyTerms;
    if (repositoryName.equals("OpenCyc")) {
        superPropertyTerms = superPropertyCache.get().get(term);
        if (superPropertyTerms != null && !superPropertyTerms.isEmpty()) {
          return superPropertyTerms;
        }
    }
    superPropertyTerms = new HashSet<>();
    getSuperProperties(repositoryName, term, superPropertyTerms);
    if (repositoryName.equals("OpenCyc")) {
        superPropertyCache.get().put(term, superPropertyTerms);
    }
    return superPropertyTerms;
  }

  /** Recursively determines the direct and indirect superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param superPropertyTerms the current set of superclass terms
   */
  public void getSuperProperties(
          final String repositoryName,
          final URI term,
          final Set<URI> superPropertyTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert superPropertyTerms != null : "superPropertyTerms must not be null";

//    LOGGER.info("getSuperProperties for " + RDFUtility.formatResource(term) + ", superPropertyTerms: " + RDFUtility.formatResources(superPropertyTerms));
    for (final URI directSuperPropertyTerm : getDirectSuperProperties(repositoryName, term)) {
      if (!superPropertyTerms.contains(directSuperPropertyTerm)) {
        superPropertyTerms.add(directSuperPropertyTerm);
        getSuperProperties(repositoryName, directSuperPropertyTerm, superPropertyTerms);
      }
    }
  }

  /** Returns the direct subproperties of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct subproperties of the given term
   */
  public Set<URI> getDirectSubProperties(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    final Set<URI> directSubPropertyTerms = new ArraySet<>();
    try {
      final TupleQuery subPropertyOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subPropertyOfTupleQuery.setBinding("o", term);
      final TupleQueryResult tupleQueryResult = subPropertyOfTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        directSubPropertyTerms.add((URI) bindingSet.getBinding("s").getValue());
      }
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return directSubPropertyTerms;
  }

  /** Clears the caches. */
  public void clearCaches() {
      isSubPropertyOfCache.get().clear();
      superPropertyCache.get().clear();
  }

  /** Provides the isSubPropertyOf cache. */
  class IsSubPropertyOfCache extends ThreadLocal<Set<List<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Set<List<URI>> initialValue() {
      return new LRUSet<>(
          10, // initialCapacity
          10000); // maxCapacity
    }
  }

  /** Provides the super property cache. */
  class SuperPropertyCache extends ThreadLocal<Map<URI, Set<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<URI, Set<URI>> initialValue() {
      return new LRUMap<>(
          10, // initialCapacity
          10000); // maxCapacity
    }
  }

}
