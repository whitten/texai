/*
 * DisjointWithQueries.java
 *
 * Created on Oct 28, 2010, 11:35:42 PM
 *
 * Description: Provides disjointWith inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * Copyright (C) Oct 28, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.subsumptionReasoner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import org.texai.kb.persistence.RDFUtility;
import org.texai.subsumptionGraph.CachedSubsumptionGraph;
import org.texai.util.LRUMap;
import org.texai.util.TexaiException;

/** Provides disjointWith inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * @author reed
 */
@NotThreadSafe
public class DisjointWithQueries {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(DisjointWithQueries.class);
  /** the disjointWith memoized cache */
  private final DisjointWithMemoizationDictionary disjointWithMemoizationDictionary = new DisjointWithMemoizationDictionary();
  /** the disjointWith query string */
  private static final String DISJOINT_WITH_QUERY_STRING = "SELECT s, o FROM {s} <" + Constants.OWL_NAMESPACE + "disjointWith> {o}";
  /** the SubClassOfQueries object */
  private final SubClassOfQueries subClassOfQueries;
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the tuple query */
  private final TupleQuery disjointWithQuery;
  /** the cached subsumption graph */
  private final CachedSubsumptionGraph cachedSubsumptionGraph;

  /** Constructs a new DisjointWithQueries instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public DisjointWithQueries(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "kbEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
    subClassOfQueries = new SubClassOfQueries(rdfEntityManager);
    cachedSubsumptionGraph = CachedSubsumptionGraph.getInstance();
    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository("OpenCyc");
    assert repositoryConnection != null;
    try {
      disjointWithQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              DISJOINT_WITH_QUERY_STRING);
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns whether the two given terms are disjoint classes.
   *
   * @param repositoryName the repository name
   * @param term1 the first given term
   * @param term2 the second given term
   * @return whether the two given terms are disjoint classes.
   */
  public boolean areDisjoint(
          final URI term1,
          final URI term2) {
    //preconditions
    assert term1 != null : "term1 must not be null";
    assert term2 != null : "term2 must not be null";

    final List<URI> termPair = orderTermPair(term1, term2);
    Boolean areDisjoint;
    areDisjoint = disjointWithMemoizationDictionary.get().get(termPair);
    if (areDisjoint == null) {
      areDisjoint = areDisjointInternal(term1, term2);
      disjointWithMemoizationDictionary.get().put(termPair, areDisjoint);
    }
    return areDisjoint;
  }

  /** Adds the given term pair to the memoized disjoint-with dictionary.
   *
   * @param term1 the first given term
   * @param term2 the second given term
   */
  public void addMemoizedDisjointWith(final URI term1, final URI term2) {
    //preconditions
    assert term1 != null : "term1 must not be null";
    assert term2 != null : "term2 must not be null";

    final List<URI> termPair = orderTermPair(term1, term2);
    disjointWithMemoizationDictionary.get().put(termPair, true);
  }

  /** Returns whether the two given terms are disjoint classes, without memoizing the results.  This algorithm works by
   * spreading activation from the two given terms, through successive superclass expansions, until either Thing is reached
   * for all branches, or a disjoint link has been detected.
   *
   * @param term1 the first given term
   * @param term2 the second given term
   * @return whether the two given terms are disjoint classes.
   */
  private boolean areDisjointInternal(
          final URI term1,
          final URI term2) {
    //preconditions
    assert term1 != null : "term1 must not be null";
    assert term2 != null : "term2 must not be null";

    // the dictionaries of self classes and classes discovered to be superclasses of the respective terms so far,
    // class --> superclass path from respective term
    final Map<URI, List<URI>> classDictionary1 = new HashMap<>();
    final Map<URI, List<URI>> classDictionary2 = new HashMap<>();
    // the dictionaries of the the classes discovered to be disjoint with the respective terms so far,
    // disjoint class --> other disjoint class
    final Map<URI, URI> disjointClassDictionary1 = new HashMap<>();
    final Map<URI, URI> disjointClassDictionary2 = new HashMap<>();
    // the superclasses of the respective terms which were discovered in the most recent spreading activation iteration
    final Set<URI> queue1 = new HashSet<>();
    final Set<URI> queue2 = new HashSet<>();
    // the superclasses of the respective terms which will be processed in the next spreading activation iteration
    final Set<URI> nextQueue1 = new HashSet<>();
    final Set<URI> nextQueue2 = new HashSet<>();

    // add self classes to the class dictionaries
    final List<URI> superClassPath1 = new ArrayList<>();
    superClassPath1.add(term1);
    classDictionary1.put(term1, superClassPath1);
    final List<URI> superClassPath2 = new ArrayList<>();
    superClassPath2.add(term2);
    classDictionary2.put(term2, superClassPath2);

    // add disjoint terms
    for (final URI disjointWithTerm : getDirectDisjointWiths(term1)) {
      disjointClassDictionary1.put(disjointWithTerm, term1);
    }
    for (final URI disjointWithTerm : getDirectDisjointWiths(term2)) {
      disjointClassDictionary2.put(disjointWithTerm, term2);
    }

    // detect disjoint relationship
    for (final URI disjointWithTerm : disjointClassDictionary1.keySet()) {
      if (classDictionary2.containsKey(disjointWithTerm)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("explanation why " + RDFUtility.formatResource(term1) + " is disjoint with " + RDFUtility.formatResource(term2) + " ...");
          LOGGER.debug("  " + RDFUtility.formatResources(classDictionary2.get(disjointWithTerm)));
          LOGGER.debug("  " + RDFUtility.formatResources(classDictionary1.get(disjointClassDictionary1.get(disjointWithTerm))));
          return true;
        }
      }
    }
    for (final URI disjointWithTerm : disjointClassDictionary2.keySet()) {
      if (classDictionary1.containsKey(disjointWithTerm)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("explanation why " + RDFUtility.formatResource(term1) + " is disjoint with " + RDFUtility.formatResource(term2) + " ...");
          LOGGER.debug("  " + RDFUtility.formatResources(classDictionary2.get(disjointWithTerm)));
          LOGGER.debug("  " + RDFUtility.formatResources(classDictionary1.get(disjointClassDictionary2.get(disjointWithTerm))));
          return true;
        }
      }
    }

    // add self classes to the spreading activation queues
    queue1.add(term1);
    queue2.add(term2);

    while (true) {
//      if (LOGGER.isDebugEnabled()) {
//        LOGGER.info("spreading activation ...");
//        LOGGER.info("  queue1: " + RDFUtility.formatResources(queue1));
//        LOGGER.info("  queue2: " + RDFUtility.formatResources(queue2));
//      }
      // spread activitation by adding direct superclasses
      for (final URI term : queue1) {
        final Collection<URI> superClasses = subClassOfQueries.getDirectSuperClasses("OpenCyc", term);
        for (final URI superClass : superClasses) {
          if (!classDictionary1.containsKey(superClass)) {
            // add superclass to the class dictionary
            final List<URI> superClassPath = classDictionary1.get(term);
            assert superClassPath != null;
            final List<URI> superClassPathClone = new ArrayList<>(superClassPath);
            superClassPathClone.add(superClass);
            classDictionary1.put(superClass, superClassPathClone);
            nextQueue1.add(superClass);
          }
        }
      }
      for (final URI term : queue2) {
        final Collection<URI> superClasses = subClassOfQueries.getDirectSuperClasses("OpenCyc", term);
        for (final URI superClass : superClasses) {
          if (!classDictionary2.containsKey(superClass)) {
            // add superclass to the class dictionary
            final List<URI> superClassPath = classDictionary2.get(term);
            assert superClassPath != null;
            final List<URI> superClassPathClone = new ArrayList<>(superClassPath);
            superClassPathClone.add(superClass);
            classDictionary2.put(superClass, superClassPathClone);
            nextQueue2.add(superClass);
          }
        }
      }

      if (nextQueue1.isEmpty() && nextQueue2.isEmpty()) {
        return false;
      }

      // add disjoint terms with respect to the added direct superclasses
      for (final URI term : nextQueue1) {
        for (final URI disjointWithTerm : getDirectDisjointWiths(term)) {
          disjointClassDictionary1.put(disjointWithTerm, term);
        }
      }
      for (final URI term : nextQueue2) {
        for (final URI disjointWithTerm : getDirectDisjointWiths(term)) {
          disjointClassDictionary2.put(disjointWithTerm, term);
        }
      }

      // detect disjoint relationship
      for (final URI disjointWithTerm : disjointClassDictionary1.keySet()) {
        if (classDictionary2.containsKey(disjointWithTerm)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("explanation why " + RDFUtility.formatResource(term1) + " is disjoint with " + RDFUtility.formatResource(term2) + " ...");
            LOGGER.debug("  " + RDFUtility.formatResources(classDictionary2.get(disjointWithTerm)));
            LOGGER.debug("  " + RDFUtility.formatResources(classDictionary1.get(disjointClassDictionary1.get(disjointWithTerm))));
            return true;
          }
        }
      }
      for (final URI disjointWithTerm : disjointClassDictionary2.keySet()) {
        if (classDictionary1.containsKey(disjointWithTerm)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("explanation why " + RDFUtility.formatResource(term1) + " is disjoint with " + RDFUtility.formatResource(term2) + " ...");
            LOGGER.debug("  " + RDFUtility.formatResources(classDictionary2.get(disjointWithTerm)));
            LOGGER.debug("  " + RDFUtility.formatResources(classDictionary1.get(disjointClassDictionary2.get(disjointWithTerm))));
            return true;
          }
        }
      }

      // migrate the queue items for the next spreading activation iteration
      queue1.clear();
      queue1.addAll(nextQueue1);
      nextQueue1.clear();
      queue2.clear();
      queue2.addAll(nextQueue2);
      nextQueue2.clear();
    }
  }

  /** Returns whether the given terms are disjoint.
   *
   * @param termPair the two ordered terms
   * @return whether the given terms are disjoint
   */
  private boolean areDisjoint(final URI[] termPair) {
    //preconditions
    assert termPair != null : "termPair must not be null";

    disjointWithQuery.setBinding("s", termPair[0]);
    disjointWithQuery.setBinding("o", termPair[1]);
    boolean areDisjoint = false;
    try {
      TupleQueryResult tupleQueryResult = disjointWithQuery.evaluate();
      areDisjoint = tupleQueryResult.hasNext();
      tupleQueryResult.close();
      if (!areDisjoint) {
        disjointWithQuery.setBinding("s", termPair[1]);
        disjointWithQuery.setBinding("o", termPair[0]);
        tupleQueryResult = disjointWithQuery.evaluate();
        areDisjoint = tupleQueryResult.hasNext();
        tupleQueryResult.close();
      }
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return areDisjoint;
  }

  /** Returns the class terms that are directly asserted to disjoint with the given class term.
   *
   * @param term the given class term
   * @return the class terms that are directly asserted to disjoint with the given class term
   */
  public Collection<URI> getDirectDisjointWiths(final URI term) {
    //preconditions
    assert term != null : "term must not be null";

    if (cachedSubsumptionGraph == null) {
      final Set<URI> disjointWithTerms = new HashSet<>();
      disjointWithQuery.setBinding("s", term);
      disjointWithQuery.removeBinding("o");
      try {
        // term owl:disjointWith ?
        TupleQueryResult tupleQueryResult = disjointWithQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          disjointWithTerms.add((URI) bindingSet.getBinding("o").getValue());
        }
        tupleQueryResult.close();

        // ? owl:disjointWith term
        disjointWithQuery.removeBinding("s");
        disjointWithQuery.setBinding("o", term);
        tupleQueryResult = disjointWithQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          disjointWithTerms.add((URI) bindingSet.getBinding("s").getValue());
        }
        tupleQueryResult.close();
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
      return disjointWithTerms;
    } else {
      return cachedSubsumptionGraph.getDirectDisjointWiths(term);
    }
  }

  /** Returns an ordered pair consisting of the given terms.
   *
   * @param term1 the first given term
   * @param term2 the second given term
   * @return an ordered pair consisting of the given terms
   */
  private static List<URI> orderTermPair(final URI term1, final URI term2) {
    //preconditions
    assert term1 != null : "term1 must not be null";
    assert term2 != null : "term2 must not be null";

    final List<URI> termPair = new ArrayList<>();
    if (term1.toString().compareTo(term2.toString()) < 1) {
      termPair.add(term1);
      termPair.add(term2);
    } else {
      termPair.add(term2);
      termPair.add(term1);
    }
    return termPair;
  }

  /** Provides the disjointWith memoized cache. */
  class DisjointWithMemoizationDictionary extends ThreadLocal<Map<List<URI>, Boolean>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<List<URI>, Boolean> initialValue() {
      return new LRUMap<>(
              100, // initialCapacity
              10000);  // maximumCapacity
    }
  }
}
