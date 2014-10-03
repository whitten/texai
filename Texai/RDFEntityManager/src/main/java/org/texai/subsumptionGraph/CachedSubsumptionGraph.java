/*
 * CachedSubsumptionGraph.java
 *
 * Created on May 2, 2011, 11:20:41 AM
 *
 * Description: Provides a cached subsumption graph of the OpenCyc repository.
 *
 * Copyright (C) May 2, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.subsumptionGraph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.TexaiException;

/** Provides a cached subsumption graph of the OpenCyc repository.
 *
 * @author reed
 */
@NotThreadSafe
public class CachedSubsumptionGraph implements Serializable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(CachedSubsumptionGraph.class);
  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the URI to id dictionary, URI --> id */
  private final Map<String, Integer> uriToIdDictionary = new HashMap<>();
  /** the URIs, indexed by id */
  private final List<String> uris = new ArrayList<>();
  /** the superclass dictionary, class id --> super class ids */
  private final Map<Integer, List<Integer>> superClassDictionary = new HashMap<>();
  /** the subClassOf dictionary, super class id --> sub class ids */
  private final Map<Integer, List<Integer>> subClassOfDictionary = new HashMap<>();
  /** the type dictionary, instance id --> class id */
  private final Map<Integer, List<Integer>> typeDictionary = new HashMap<>();
  /** the instance dictionary, class id --> instance ids */
  private final Map<Integer, List<Integer>> instanceDictionary = new HashMap<>();
  /** the disjoint with dictionary, class id --> disjoint class ids */
  private final Map<Integer, List<Integer>> disjointWithDictionary = new HashMap<>();
  /** the next available uri id */
  private int nextId = 0;
  /** the singleton instance */
  private static CachedSubsumptionGraph cachedSubsumptionGraph;

  /** Constructs a new CachedSubsumptionGraph instance. */
  public CachedSubsumptionGraph() {
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
    assert "OpenCyc".equals(repositoryName) : "repositoryName must be OpenCyc";
    assert term != null : "term must not be null";

    final Collection<URI> superClassTerms = new ArrayList<>();
    final Integer id = uriToIdDictionary.get(RDFUtility.formatResource(term));
    if (id != null) {
      final List<Integer> superClasses = superClassDictionary.get(id);
      if (superClasses != null) {
        for (final Integer superClass : superClasses) {
          superClassTerms.add(RDFUtility.makeURIFromAlias(uris.get(superClass)));
        }
      }
    }
    return superClassTerms;
  }

  /** Returns the class terms that are directly asserted to disjoint with the given class term.
   *
   * @param term the given class term
   * @return the class terms that are directly asserted to disjoint with the given class term
   */
  public Collection<URI> getDirectDisjointWiths(final URI term) {
    //preconditions
    assert term != null : "term must not be null";

    final Collection<URI> disjointWithTerms = new ArrayList<>();
    final Integer id = uriToIdDictionary.get(RDFUtility.formatResource(term));
    if (id != null) {
      final List<Integer> disjointWiths = disjointWithDictionary.get(id);
      if (disjointWiths != null) {
        for (final Integer disjointWith : disjointWiths) {
          disjointWithTerms.add(RDFUtility.makeURIFromAlias(uris.get(disjointWith)));
        }
      }
    }
    return disjointWithTerms;
  }

  /** Loads the dictionaries from the OpenCyc repository.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public void loadDictionariesFromOpenCycRepository(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    uriToIdDictionary.clear();
    uris.clear();
    superClassDictionary.clear();
    subClassOfDictionary.clear();
    typeDictionary.clear();
    instanceDictionary.clear();
    disjointWithDictionary.clear();
    nextId = 0;

    final RepositoryConnection repositoryConnection = rdfEntityManager.getConnectionToNamedRepository("OpenCyc");
    try {
      final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(null, null, null, false);
      int statementCnt = 0;
      LOGGER.info("gathering OpenCyc statements...");
      while (repositoryResult.hasNext()) {
        statementCnt++;
        final Statement statement = repositoryResult.next();
        if (statementCnt % 100000 == 0) {
          LOGGER.info(statementCnt + "  " + RDFUtility.formatStatementAsTurtle(statement));
        }
        final URI predicate = statement.getPredicate();
        if (predicate.equals(RDF.TYPE)) {
          // load typeDictionary entry
          final Integer subjectIndex = getId((URI) statement.getSubject());
          final Integer objectIndex = getId((URI) statement.getObject());
          List<Integer> types = typeDictionary.get(subjectIndex);
          if (types == null) {
            types = new ArrayList<>();
            typeDictionary.put(subjectIndex, types);
          }
          types.add(objectIndex);

          // load instanceDictionary entry
          List<Integer> instances = instanceDictionary.get(subjectIndex);
          if (instances == null) {
            instances = new ArrayList<>();
            instanceDictionary.put(subjectIndex, instances);
          }
          instances.add(objectIndex);

        } else if (predicate.equals(RDFS.SUBCLASSOF)) {
          // load superClassDictionary entry
          final Integer subjectIndex = getId((URI) statement.getSubject());
          final Integer objectIndex = getId((URI) statement.getObject());
          List<Integer> superClasses = superClassDictionary.get(subjectIndex);
          if (superClasses == null) {
            superClasses = new ArrayList<>();
            superClassDictionary.put(subjectIndex, superClasses);
          }
          superClasses.add(objectIndex);

          // load subClassOfDictionary entry
          List<Integer> subClasses = subClassOfDictionary.get(objectIndex);
          if (subClasses == null) {
            subClasses = new ArrayList<>();
            subClassOfDictionary.put(objectIndex, subClasses);
          }
          subClasses.add(subjectIndex);


        } else if (predicate.equals(OWL.DISJOINTWITH)) {
          // load disjointWithDictionary entry with two symmetric entries
          final Integer subjectIndex = getId((URI) statement.getSubject());
          final Integer objectIndex = getId((URI) statement.getObject());
          List<Integer> disjointClasses = disjointWithDictionary.get(subjectIndex);
          if (disjointClasses == null) {
            disjointClasses = new ArrayList<>();
            disjointWithDictionary.put(subjectIndex, disjointClasses);
          }
          disjointClasses.add(objectIndex);
          disjointClasses = disjointWithDictionary.get(objectIndex);
          if (disjointClasses == null) {
            disjointClasses = new ArrayList<>();
            disjointWithDictionary.put(objectIndex, disjointClasses);
          }
          disjointClasses.add(subjectIndex);
        }
      }
      LOGGER.info(statementCnt + " OpenCyc statements");
    } catch (RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the id for the given URI.
   *
   * @param uri the given URI
   * @return the id
   */
  private Integer getId(final URI uri) {
    //Preconditions
    assert uri != null : "uri must not be null";

    final String uriString = RDFUtility.formatResource(uri);
    Integer id = uriToIdDictionary.get(uriString);
    if (id == null) {
      id = nextId++;
      uriToIdDictionary.put(uriString, id);
      uris.add(uriString);
      assert uris.get(id).equals(uriString);
    }
    return id;
  }

  /** Initializes the singleton instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public static void initializeSingletonInstance(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cachedSubsumptionGraph = new CachedSubsumptionGraph();
    cachedSubsumptionGraph.loadDictionariesFromOpenCycRepository(rdfEntityManager);
  }

  /** Gets the singleton instance.
   *
   * @return  the singleton instance
   */
  public static CachedSubsumptionGraph getInstance() {
    return cachedSubsumptionGraph;
  }

  /** Logs the dictionary statistics. */
  public void logDictionaryStatistics() {
    LOGGER.info("uriToIdDictionary:      " + uriToIdDictionary.size());
    LOGGER.info("uris:                   " + uris.size());
    LOGGER.info("superClassDictionary:   " + superClassDictionary.size());
    LOGGER.info("subClassOfDictionary:   " + subClassOfDictionary.size());
    LOGGER.info("typeDictionary:         " + typeDictionary.size());
    LOGGER.info("instanceDictionary:     " + instanceDictionary.size());
    LOGGER.info("disjointWithDictionary: " + disjointWithDictionary.size());
  }

  /** Executes this application.
   *
   * @param args the command line args - unused
   */
  public static void main(final String[] args) {
    CacheInitializer.initializeCaches();
    final RDFEntityManager rdfEntityManager = new RDFEntityManager();
    CachedSubsumptionGraph.initializeSingletonInstance(rdfEntityManager);
    rdfEntityManager.close();
    final CachedSubsumptionGraph cachedSubsumptionGraph1 = CachedSubsumptionGraph.getInstance();
    cachedSubsumptionGraph1.logDictionaryStatistics();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
    LOGGER.info("CachedSubsumptionGraph completed");
  }
}
