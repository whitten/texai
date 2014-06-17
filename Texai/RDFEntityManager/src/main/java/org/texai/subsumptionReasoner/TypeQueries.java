/*
 * TypeQueries.java
 *
 * Created on Apr 25, 2008, 5:14:29 PM
 *
 * Description: Provides type inference queries into the the knowledge base specified by a given RDF entity manager.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.LRUMap;
import org.texai.util.TexaiException;

/** Provides type inference queries into the the knowledge base specified by a given RDF entity manager.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class TypeQueries {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(TypeQueries.class);
  //TODO the OpenCyc KB has many relationships without context
  //  /** the subclassof query string */
  //  private static final String TYPE_QUERY_STRING = "SELECT s, o FROM context <" + Constants.TERM_UNIVERSAL_VOCABULARY_MT +
  //          "> {s} <" + Constants.RDF_NAMESPACE + "type> {o}";
  //  /** the subclassof query string */
  //  private static final String SUBCLASSOF_QUERY_STRING = "SELECT s, o FROM context <" + Constants.TERM_UNIVERSAL_VOCABULARY_MT +
  //          "> {s} <" + Constants.RDFS_NAMESPACE + "subClassOf> {o}";
  /** the rdf:type query string */
  private static final String TYPE_QUERY_STRING = "SELECT s, o FROM {s} <" + Constants.RDF_NAMESPACE + "type> {o}";
  /** the rdfs:subclassof query string */
  private static final String SUBCLASSOF_QUERY_STRING = "SELECT s, o FROM  {s} <" + Constants.RDFS_NAMESPACE + "subClassOf> {o}";
  /** the cyc:Individual URI */
  private static final URI INDIVIDUAL_TERM = new URIImpl(Constants.CYC_NAMESPACE + "Individual");
  /** the cyc:Microtheory URI */
  private static final URI MICROTHEORY_TERM = new URIImpl(Constants.CYC_NAMESPACE + "Microtheory");
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;

  /** the isType cache */
  private IsTypeCache isTypeCache = new IsTypeCache();
  /** the type hierarchy cache */
  private TypeHierarchyCache typeHierarchyCache = new TypeHierarchyCache();
  /** the types cache */
  private TypesCache typesCache = new TypesCache();


  /** Constructs a new TypeQueries instance.
   *
   * @param rdfEntityManager  the RDF entity manager
   */
  public TypeQueries(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Returns whether the given term is directly an instance of the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @return whether the given term is directly an instance of the given type term
   */
  public boolean isDirectType(
          final String repositoryName,
          final URI term,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    boolean isDirectType;
    try {
      final TupleQuery typeTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              TYPE_QUERY_STRING);
      typeTupleQuery.setBinding("s", term);
      typeTupleQuery.setBinding("o", typeTerm);
      final TupleQueryResult tupleQueryResult = typeTupleQuery.evaluate();
      isDirectType = tupleQueryResult.hasNext();
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return isDirectType;
  }

  /** Returns the type hierarchy between the given term's direct type and the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @return the type hierarchy between the given term's direct type and the given type term
   */
  public List<URI> typeHierarchy(
          final String repositoryName,
          final URI term,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    List<URI> typeHierarchy = null;
    final List<URI> key = new ArrayList<>(2);
    if (repositoryName.equals("OpenCyc")) {
      key.add(term);
      key.add(typeTerm);
        typeHierarchy = typeHierarchyCache.get().get(key);
        if (typeHierarchy != null) {
          return typeHierarchy;
        }
      }
    typeHierarchy = new ArrayList<>();
    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      final List<URI> directTypeTerms = new ArrayList<>();
      final TupleQuery typeTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              TYPE_QUERY_STRING);
      typeTupleQuery.setBinding("s", term);
      final TupleQueryResult tupleQueryResult = typeTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI directTypeTerm = (URI) bindingSet.getBinding("o").getValue();
        if (directTypeTerm.equals(typeTerm)) {
          // is a direct type
          typeHierarchy.add(directTypeTerm);
        } else {
          directTypeTerms.add(directTypeTerm);
        }
      }
      tupleQueryResult.close();
      if (typeHierarchy.isEmpty()) {
        final Set<URI> visitedTypeTerms = new HashSet<>(directTypeTerms);
        for (final URI directTypeTerm : directTypeTerms) {
          final List<URI> typeHierarchy1 = subClassOfHierarchy(
                  repositoryName,
                  directTypeTerm,
                  typeTerm,
                  visitedTypeTerms,
                  repositoryConnection);
          if (!typeHierarchy1.isEmpty()) {
            typeHierarchy.addAll(typeHierarchy1);
            break;
          }
        }
      }
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(RDFUtility.formatURIAsTurtle(term) + " type hierarchy to " + RDFUtility.formatURIAsTurtle(typeTerm)
              + " --> " + RDFUtility.formatResources(typeHierarchy));
    }

    if (!typeHierarchy.isEmpty() && repositoryName.equals("OpenCyc")) {
        typeHierarchyCache.get().put(key, typeHierarchy);
    }
    return typeHierarchy;
  }

  /** Returns whether the given term is directly or indirectly an instance of the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @return whether the given term is directly or indirectly an instance of the given type term
   */
  public boolean isType(
          final String repositoryName,
          final URI term,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    String key = null;
    if (repositoryName.equals(Constants.OPEN_CYC)) {
      key = RDFUtility.formatResource(term) + "/" + RDFUtility.formatResource(typeTerm);
        final Boolean result = isTypeCache.get().get(key);
        if (result != null) {
          return result;
        }
    }

    // cache miss, so get the type from the repository
    final boolean isType = isType_NoCache(
            repositoryName,
            term,
            typeTerm);

    if (repositoryName.equals(Constants.OPEN_CYC)) {
        isTypeCache.get().put(key, isType);
    }
    return isType;
  }

  /** Returns whether the given term is directly or indirectly an instance of the given type term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @param typeTerm the given type term
   * @return whether the given term is directly or indirectly an instance of the given type term
   */
  private boolean isType_NoCache(
          final String repositoryName,
          final URI term,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";
    assert typeTerm != null : "typeTerm must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    boolean isType = false;
    try {
      final List<URI> directTypeTerms = new ArrayList<>();
      final TupleQuery typeTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              TYPE_QUERY_STRING);
      typeTupleQuery.setBinding("s", term);
      final TupleQueryResult tupleQueryResult = typeTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI directTypeTerm = (URI) bindingSet.getBinding("o").getValue();
        if (directTypeTerm.equals(typeTerm)) {
          // is a direct type
          isType = true;
        } else {
          directTypeTerms.add(directTypeTerm);
        }
      }
      tupleQueryResult.close();
      if (!isType) {
        final Set<URI> visitedTypeTerms = new HashSet<>(directTypeTerms);
        for (final URI directTypeTerm : directTypeTerms) {
          if (isSubClassOf(
                  repositoryName,
                  directTypeTerm,
                  typeTerm,
                  visitedTypeTerms)) {
            isType = true;
            break;
          }
        }
      }
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    if (LOGGER.isDebugEnabled()) {
      if (isType) {
        LOGGER.debug(RDFUtility.formatURIAsTurtle(term) + " is type of " + RDFUtility.formatURIAsTurtle(typeTerm));
      } else {
        LOGGER.debug(RDFUtility.formatURIAsTurtle(term) + " is not a type of " + RDFUtility.formatURIAsTurtle(typeTerm));
      }
    }
    return isType;
  }

  /** Returns whether the given term is directly or indirectly a subclass of of the given type term.
   *
   * @param repositoryName the repository name
   * @param typeTerm1 the candidate subclass type term
   * @param typeTerm2 the given type term
   * @param visitedTypeTerms the visited type terms
   * @return whether the given term is directly or indirectly a subclass of of the given type term
   */
  private boolean isSubClassOf(
          final String repositoryName,
          final URI typeTerm1,
          final URI typeTerm2,
          final Set<URI> visitedTypeTerms) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerm1 != null : "typeTerm1 must not be null";
    assert typeTerm2 != null : "typeTerm2 must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    try {
      final TupleQuery subClassOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subClassOfTupleQuery.setBinding("s", typeTerm1);
      final TupleQueryResult tupleQueryResult = subClassOfTupleQuery.evaluate();
      final List<URI> superClassTerms = new ArrayList<>();
      boolean isSubClassOf = false;
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI superClassTerm = (URI) bindingSet.getBinding("o").getValue();
        if (typeTerm2.equals(superClassTerm)) {
          isSubClassOf = true;
          break;
        } else if (!visitedTypeTerms.contains(superClassTerm)) {
          visitedTypeTerms.add(superClassTerm);
          superClassTerms.add(superClassTerm);
        }
      }
      tupleQueryResult.close();
      if (isSubClassOf) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
        }
        return true;
      }
      for (final URI superClassTerm : superClassTerms) {
        if (isSubClassOf(
                repositoryName,
                superClassTerm,
                typeTerm2,
                visitedTypeTerms)) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
          }
          return true;
        }
      }
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is not a subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
    }
    return false;
  }

  /** Returns the first discovered subclass hierarchy terms between the two given type terms.
   *
   * @param repositoryName the repository name
   * @param typeTerm1 the subclass term
   * @param typeTerm2 the superclass term
   * @param visitedTypeTerms the visited type terms
   * @return the subclass hierarchy terms between the two given type terms
   */
  protected List<URI> subClassOfHierarchy(
          final String repositoryName,
          final URI typeTerm1,
          final URI typeTerm2,
          final Set<URI> visitedTypeTerms,
          final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerm1 != null : "term must not be null";
    assert typeTerm2 != null : "typeTerm must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("considering class " + RDFUtility.formatURIAsTurtle(typeTerm1) + " as a member of the super class hierarchy to " + RDFUtility.formatURIAsTurtle(typeTerm2));
    }
    final List<URI> subClassOfHierarchy = new ArrayList<>();
    subClassOfHierarchy.add(typeTerm1);
    if (typeTerm1.equals(typeTerm2)) {
      return subClassOfHierarchy;
    }

    try {
      final TupleQuery subClassOfTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              SUBCLASSOF_QUERY_STRING);
      subClassOfTupleQuery.setBinding("s", typeTerm1);
      final TupleQueryResult tupleQueryResult = subClassOfTupleQuery.evaluate();
      final List<URI> superClassTerms = new ArrayList<>();
      boolean isDone = false;
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI superClassTerm = (URI) bindingSet.getBinding("o").getValue();
        if (visitedTypeTerms.contains(superClassTerm)) {
          continue;
        }
        visitedTypeTerms.add(superClassTerm);
        superClassTerms.add(superClassTerm);
        if (typeTerm2.equals(superClassTerm)) {
          isDone = true;
          subClassOfHierarchy.add(superClassTerm);
          break;
        }
      }
      tupleQueryResult.close();
      if (isDone) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
        }
        return subClassOfHierarchy;
      }
      for (final URI superClassTerm : superClassTerms) {
        final List<URI> subClassOfHierarchy1 = subClassOfHierarchy(
                repositoryName,
                superClassTerm,
                typeTerm2,
                visitedTypeTerms,
                repositoryConnection);
        if (!subClassOfHierarchy1.isEmpty()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
          }
          subClassOfHierarchy.addAll(subClassOfHierarchy1);
          return subClassOfHierarchy;
        }
      }
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(RDFUtility.formatURIAsTurtle(typeTerm1) + " is not a subclass of " + RDFUtility.formatURIAsTurtle(typeTerm2));
    }
    subClassOfHierarchy.clear();
    return subClassOfHierarchy;
  }

  /** Returns the direct types of the given term.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return the direct types of the given term
   */
  public Set<URI> getDirectTypes(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    Set<URI> directTypeTerms;
    if (repositoryName.equals("OpenCyc")) {
        directTypeTerms = typesCache.get().get(term);
        if (directTypeTerms != null && !directTypeTerms.isEmpty()) {
          return directTypeTerms;
        }
    }
    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    directTypeTerms = new HashSet<>();
    try {
      final TupleQuery typeTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              TYPE_QUERY_STRING);
      typeTupleQuery.setBinding("s", term);
      final TupleQueryResult tupleQueryResult = typeTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        directTypeTerms.add((URI) bindingSet.getBinding("o").getValue());
      }
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    if (repositoryName.equals("OpenCyc")) {
        typesCache.get().put(term, directTypeTerms);
    }
    return directTypeTerms;
  }

  /** Returns the direct instances of the given type term.
   *
   * @param repositoryName the repository name
   * @param typeTerm the given term
   * @return the direct instances of the given type term
   */
  public Set<URI> getDirectInstances(
          final String repositoryName,
          final URI typeTerm) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert typeTerm != null : "typeTerm must not be null";

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository(repositoryName);
    final Set<URI> directInstances = new HashSet<>();
    try {
      final TupleQuery typeTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              TYPE_QUERY_STRING);
      typeTupleQuery.setBinding("o", typeTerm);
      final TupleQueryResult tupleQueryResult = typeTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        directInstances.add((URI) bindingSet.getBinding("s").getValue());
      }
      tupleQueryResult.close();
    } catch (final MalformedQueryException ex) {
      throw new TexaiException(ex);
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return directInstances;
  }

  /** Returns whether the given term is an individual.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is an individual
   */
  public boolean isIndividualTerm(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return isType(
            repositoryName,
            term,
            INDIVIDUAL_TERM);
  }

  /** Returns whether the given term is a class.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is a class
   */
  public boolean isClassTerm(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return term.equals(OWL.CLASS)
            || isType(repositoryName, term, OWL.CLASS);
  }

  /** Returns whether the given term is a context.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is a context
   */
  public boolean isContextTerm(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return isType(
            repositoryName,
            term,
            MICROTHEORY_TERM);
  }

  /** Returns whether the given term is a property.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is a property
   */
  public boolean isPropertyTerm(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return isType(
            repositoryName,
            term,
            RDF.PROPERTY);
  }

  /** Clears the caches. */
  public void clearCaches() {
      isTypeCache.get().clear();
      typesCache.get().clear();
  }

  /** Provides the isType cache. */
  class IsTypeCache extends ThreadLocal<Map<String, Boolean>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<String, Boolean> initialValue() {
      return new LRUMap<>(
          10, // initialCapacity
          10000); // maxCapacity
    }
  }

  /** Provides the type hierarchy cache. */
  class TypeHierarchyCache extends ThreadLocal<Map<List<URI>, List<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<List<URI>, List<URI>> initialValue() {
      return new LRUMap<>(
          10, // initialCapacity
          50000); // maxCapacity
    }
  }

  /** Provides the types cache. */
  class TypesCache extends ThreadLocal<Map<URI, Set<URI>>> {

    /** Returns the current thread's "initial value" for this thread-local variable.
     *
     * @return the current thread's "initial value
     */
    @Override
    protected Map<URI, Set<URI>> initialValue() {
      return new LRUMap<>(
          10, // initialCapacity
          50000); // maxCapacity
    }
  }
}
