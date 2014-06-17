/*
 * SubClassOfQueries.java
 *
 * Created on Apr 25, 2008, 5:25:19 PM
 *
 * Description: Provides subClassOf inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * Copyright (C) Apr 25, 2008 Stephen L. Reed.
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
package org.texai.subsumptionReasoner;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.subsumptionGraph.CachedSubsumptionGraph;
import org.texai.util.ArraySet;
import org.texai.util.LRUMap;
import org.texai.util.TexaiException;

/** Provides subClassOf inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public class SubClassOfQueries {

//TODO the OpenCyc KB has many relationships without context
  /** the subClassOf query string */
//  private static final String SUBCLASSOF_QUERY_STRING = "SELECT s, o FROM context <" + Constants.TERM_UNIVERSAL_VOCABULARY_MT +
//          "> {s} <" + Constants.RDFS_NAMESPACE + "subClassOf> {o}";
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SubClassOfQueries.class);
  /** the query */
  private static final String SUBCLASSOF_QUERY_STRING = "SELECT s, o FROM {s} <" + Constants.RDFS_NAMESPACE + "subClassOf> {o}";
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the subclass-of cache name */
  private final String SUBCLASS_OF_CACHE = "subclass of";
  /** the super class cache */
  private final DirectSuperClassCache directSuperClassCache = new DirectSuperClassCache();
  /** the super class cache */
  private final SuperClassCache superClassCache = new SuperClassCache();
  /** the Situation URI */
  private static final URI SITUATION_TERM = new URIImpl(Constants.CYC_NAMESPACE + "Situation");
  /** the subclass-of cache */
  private final Cache cache = CacheManager.getInstance().getCache(SUBCLASS_OF_CACHE);
  /** the cached subsumption graph */
  private final CachedSubsumptionGraph cachedSubsumptionGraph;

  /** Constructs a new SubClassOfQueries instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public SubClassOfQueries(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
    cachedSubsumptionGraph = CachedSubsumptionGraph.getInstance();

    //Postconditions
    assert cache != null : "cache not found for: " + SUBCLASS_OF_CACHE;
  }

  /** Returns whether the given term is directly a subclass of of the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @return whether the given term is directly a subclass of of the given type term
   */
  public boolean isDirectSubClassOf(
          final String repositoryName,
          final URI term,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    boolean isDirectSubClassOf;
    try {
      final TupleQuery subClassOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subClassOfTupleQuery.setBinding("s", term);
      subClassOfTupleQuery.setBinding("o", typeTerm);
      final TupleQueryResult tupleQueryResult = subClassOfTupleQuery.evaluate();
      isDirectSubClassOf = tupleQueryResult.hasNext();
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return isDirectSubClassOf;
  }

  /** Returns whether the first type term is directly or indirectly a subclass of of the second type term.
   *
   * @param repositoryName the repository name
   * @param typeTerm1 the first type term
   * @param typeTerm2 the second type term
   * @return whether the first type term is directly or indirectly a subclass of of the second type term
   */
  public boolean isSubClassOf(
          final String repositoryName,
          final URI typeTerm1,
          final URI typeTerm2) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerm1 != null : "typeTerm1 must not be null";
    assert typeTerm2 != null : "typeTerm2 must not be null";

    final String key = RDFUtility.formatResource(typeTerm1) + "/" + RDFUtility.formatResource(typeTerm2);
    Element element = cache.get(key);
    if (element == null) {
      final boolean isSubClassOf = isSubClassOf_NoCache(
              repositoryName,
              typeTerm1,
              typeTerm2,
              new HashSet<URI>()); // visitedTypeTerms
      cache.put(new Element(key, isSubClassOf));
      return isSubClassOf;
    } else {
      return (Boolean) element.getValue();
    }
  }

  /** Removes redundant types from the given types. A given type is considered redundant if another type is a subclass of
   * the given type.
   *
   * @param repositoryName the repository name
   * @param typeTerms the type terms
   */
  public void removeRedundantTypes(
          final String repositoryName,
          final Collection<URI> typeTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerms != null : "typeTerms must not be null";

    final Set<URI> typesToRemove = new HashSet<>();
    for (final URI typeTerm : typeTerms) {
      for (final URI otherTypeTerm : typeTerms) {
        if (typeTerm.equals(otherTypeTerm)) {
          continue;
        } else if (isSubClassOf(repositoryName, otherTypeTerm, typeTerm)) {
          typesToRemove.add(typeTerm);
        }
      }
    }
    typeTerms.removeAll(typesToRemove);
  }

  /** Returns whether the first type terms are each subclasses of at least one of the second type terms.
   *
   * @param repositoryName the repository name
   * @param typeTerms1 the first type terms
   * @param typeTerms2 the second type terms
   * @return whether the first type terms are each subclasses of at least one of the second type terms
   */
  public boolean areSubClassesOf(
          final String repositoryName,
          final Collection<URI> typeTerms1,
          final Collection<URI> typeTerms2) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerms1 != null : "typeTerms1 must not be null";
    assert typeTerms2 != null : "typeTerms2 must not be null";

    for (final URI typeTerm1 : typeTerms1) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("considering typeTerm1 " + RDFUtility.formatResource(typeTerm1));
      }
      boolean isSubClassOf = false;
      for (final URI typeTerm2 : typeTerms2) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("considering typeTerm2 " + RDFUtility.formatResource(typeTerm2));
        }
        if (typeTerm1.equals(typeTerm2) || isSubClassOf(repositoryName, typeTerm1, typeTerm2)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  --> is sub class of");
          }
          isSubClassOf = true;
          break;
        } else if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("  --> is not sub class of");
        }
      }
      if (!isSubClassOf) {
        return false;
      }
    }
    return true;
  }

  /** Returns whether the given term is directly or indirectly a subclass of of the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @param visitedTypeTerms the visited type terms
   * @return whether the given term is directly or indirectly a subclass of of the given type term
   */
  private boolean isSubClassOf_NoCache(
          final String repositoryName,
          final URI term,
          final URI typeTerm,
          final Set<URI> visitedTypeTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    if (term.toString().equals(Constants.TERM_OWL_THING)) {
      // owl:Thing is the ontology root concept
      return false;
    }
    for (final URI superClassTerm : getDirectSuperClasses(repositoryName, term)) {
      if (typeTerm.equals(superClassTerm)) {
        return true;
      } else if (!visitedTypeTerms.contains(superClassTerm)) {
        visitedTypeTerms.add(superClassTerm);
        if (isSubClassOf_NoCache(
                repositoryName,
                superClassTerm,
                typeTerm,
                visitedTypeTerms)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Returns the direct superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct superclasses of the given term
   */
  public Collection<URI> getDirectSuperClasses(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    Collection<URI> directSuperClassTerms;
    if (repositoryName.equals(Constants.OPEN_CYC)) {
      directSuperClassTerms = directSuperClassCache.get().get(term);
      if (directSuperClassTerms != null && !directSuperClassTerms.isEmpty()) {
        return directSuperClassTerms;
      }
    }

    directSuperClassTerms = getDirectSuperClasses_NoCache(repositoryName, term);

    if (repositoryName.equals(Constants.OPEN_CYC)) {
      directSuperClassCache.get().put(term, directSuperClassTerms);
    }
    return directSuperClassTerms;
  }

  /** Returns the direct superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct superclasses of the given term
   */
  private Collection<URI> getDirectSuperClasses_NoCache(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

     if (cachedSubsumptionGraph == null) {
      final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
      final Set<URI> directSuperClassTerms = new ArraySet<>();
      try {
        final TupleQuery subClassOfTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                SUBCLASSOF_QUERY_STRING);
        subClassOfTupleQuery.setBinding("s", term);
        final TupleQueryResult tupleQueryResult = subClassOfTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          directSuperClassTerms.add((URI) bindingSet.getBinding("o").getValue());
        }
        tupleQueryResult.close();
      } catch (final MalformedQueryException ex) {
        throw new TexaiException(ex);
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
//    LOGGER.info("getDirectSuperClasses for " + RDFUtility.formatResource(term)
//            + ", directSuperClassTerms: " + RDFUtility.formatResources(directSuperClassTerms));
    return directSuperClassTerms;
    } else {
      return cachedSubsumptionGraph.getDirectSuperClasses(repositoryName, term);
    }
  }

  /** Returns the direct and indirect superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct and indirect superclasses of the given term
   */
  public Set<URI> getSuperClasses(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    Set<URI> superClassTerms;
    if (repositoryName.equals(Constants.OPEN_CYC)) {
      superClassTerms = superClassCache.get().get(term);
      if (superClassTerms != null && !superClassTerms.isEmpty()) {
        return superClassTerms;
      }
    }
    superClassTerms = new HashSet<>();
    getSuperClasses(repositoryName, term, superClassTerms);
    if (repositoryName.equals(Constants.OPEN_CYC)) {
      superClassCache.get().put(term, superClassTerms);
    }
    return superClassTerms;
  }

  /** Recursively determines the direct and indirect superclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param superClassTerms the current set of superclass terms
   */
  public void getSuperClasses(
          final String repositoryName,
          final URI term,
          final Set<URI> superClassTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert superClassTerms != null : "superClassTerms must not be null";

//    LOGGER.info("getSuperClasses for " + RDFUtility.formatResource(term) + ", superClassTerms: " + RDFUtility.formatResources(superClassTerms));
    for (final URI directSuperClassTerm : getDirectSuperClasses(repositoryName, term)) {
      if (!superClassTerms.contains(directSuperClassTerm)) {
        superClassTerms.add(directSuperClassTerm);
        getSuperClasses(repositoryName, directSuperClassTerm, superClassTerms);
      }
    }
  }

  /** Returns the direct subclasses of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct subclasses of the given term
   */
  public Set<URI> getDirectSubClasses(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    final Set<URI> directSubClassTerms = new ArraySet<>();
    try {
      final TupleQuery subClassOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subClassOfTupleQuery.setBinding("o", term);
      final TupleQueryResult tupleQueryResult = subClassOfTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        directSubClassTerms.add((URI) bindingSet.getBinding("s").getValue());
      }
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return directSubClassTerms;
  }

  /** Returns whether the given class is a subclass of cyc:Situation.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given class is a subclass of cyc:Situation
   */
  public boolean isSituationClass(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return isSubClassOf(repositoryName, term, SITUATION_TERM);
  }

  /** Clears the caches. */
  public void clearCaches() {
    cache.removeAll();
    superClassCache.get().clear();
  }

  /** Provides the direct super class cache. */
  class DirectSuperClassCache extends ThreadLocal<Map<URI, Collection<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<URI, Collection<URI>> initialValue() {
      return new LRUMap<>(
              10, // initialCapacity
              20000); // maxCapacity
    }
  }

  /** Provides the super class cache. */
  class SuperClassCache extends ThreadLocal<Map<URI, Set<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<URI, Set<URI>> initialValue() {
      return new LRUMap<>(
              10, // initialCapacity
              5000); // maxCapacity
    }
  }
}
