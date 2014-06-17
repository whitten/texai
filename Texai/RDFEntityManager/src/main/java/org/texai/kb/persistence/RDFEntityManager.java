/*
 * RDFEntityManager.java
 *
 * Created on August 13, 2007, 12:01 PM
 *
 * Description: Provides a facade for the RDF entity manager components.
 *
 * Copyright (C) August 13, 2007 Stephen L. Reed.
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
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.BooleanLiteralImpl;
import org.openrdf.model.impl.ContextStatementImpl;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.NumericLiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.Constants;
import org.texai.kb.journal.JournalRequest;
import org.texai.kb.journal.JournalWriter;
import org.texai.subsumptionReasoner.SubClassOfQueries;
import org.texai.subsumptionReasoner.SubPropertyOfQueries;
import org.texai.subsumptionReasoner.TypeQueries;
import org.texai.util.TexaiException;

/** This class provides a facade for public methods in RDFEntityLoader, RDFEntityPersister, RDFEntityRemover, RDFUtility
 * and it provides a facade for commonly used methods (e.g. transactions) in the Sesame RepositoryConnection.
 *
 * @author reed
 */
@NotThreadSafe
public final class RDFEntityManager {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityManager.class);
  /** the singleton distributed repository manager, required here because of how the distributed repository manager is initialized */
  private static DistributedRepositoryManager distributedRepositoryManager = null;
  /** the RDF utility */
  private final RDFUtility rdfUtility;
  /** the RDF entity persister */
  private final RDFEntityPersister rdfEntityPersister;
  /** the RDF entity loader */
  private final RDFEntityLoader rdfEntityLoader;
  /** the RDF entity remover */
  private final RDFEntityRemover rdfEntityRemover;
  /** the journal writer */
  private final JournalWriter journalWriter;
  /** the cached indicator whether to automatically commit every repository operation */
  private boolean isAutoCommit = true;   // NOPMD
  /** the type queries object */
  private TypeQueries typeQueries;
  /** the subclass queries object */
  private SubClassOfQueries subClassOfQueries;
  /** the subproperty queries object */
  private SubPropertyOfQueries subPropertyOfQueries;
  /** the repository connection dictionary, repository name --> repository connection */
  private final Map<String, RepositoryConnection> repositoryConnectionDictionary = new HashMap<>();
  /** the indicator that this RDF entity manager is closed */
  private boolean isClosed = false;

  /** Creates a new instance of RDFEntityManager. */
  public RDFEntityManager() {
    rdfUtility = new RDFUtility(this);
    rdfEntityPersister = new RDFEntityPersister(this);
    rdfEntityLoader = new RDFEntityLoader();
    rdfEntityRemover = new RDFEntityRemover(this);
    journalWriter = new JournalWriter();
  }

  /** Closes this object and releases its resources. */
  public void close() {
    try {
      for (final RepositoryConnection repositoryConnection : repositoryConnectionDictionary.values()) {
        if (repositoryConnection.isOpen()) {
          if (!repositoryConnection.isAutoCommit()) {
            LOGGER.info("commiting any pending transaction for " + repositoryConnection
                    + " to " + repositoryConnection.getRepository().getDataDir().getName());
            repositoryConnection.commit();
          }
          repositoryConnection.close();
        }
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    isClosed = true;
  }

  /** Gets the indicator that this RDF entity manager is closed.
   *
   * @return the indicator that this RDF entity manager is closed
   */
  public boolean isClosed() {
    return isClosed;
  }

  /** Get the singleton distributed repository manager.
   *
   * @return the singleton distributed repository manager
   */
  public static DistributedRepositoryManager getDistributedRepositoryManager() {
    return distributedRepositoryManager;
  }

  /** the singleton distributed repository manager.
   *
   * @param aDistributedRepositoryManager the singleton distributed repository manager
   */
  public static void setDistributedRepositoryManager(final DistributedRepositoryManager aDistributedRepositoryManager) {
    distributedRepositoryManager = aDistributedRepositoryManager;
  }

  /** Returns a possible new root RDF entity given the existing root and a contained candidate root RDF entity.
   *
   * @param rootRDFEntity the existing root RDF entity
   * @param candidateRootRDFEntity a contained candidate root RDF entity
   * @return a possible new root RDF entity
   */
  public RDFPersistent possibleNewRoot(
          final RDFPersistent rootRDFEntity,
          final RDFPersistent candidateRootRDFEntity) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert DistributedRepositoryManager.getInstance().getRepositoryNameForClass(rootRDFEntity.getClass()) != null :
            "invalid root RDF entity does not have a matching repository content description, class: " + rootRDFEntity.getClass().getName()
            + ", item id: " + rootRDFEntity.getId();
    assert candidateRootRDFEntity != null : "candidateRootRDFEntity must not be null";

    final String repositoryName = DistributedRepositoryManager.getInstance().getRepositoryNameForClass(candidateRootRDFEntity.getClass());
    if (repositoryName == null) {
      // the candidate root RDF entity has a repository content description item that supercedes the repository content description
      // of the existing root RDF entity
      return rootRDFEntity;
    } else {
      return candidateRootRDFEntity;
    }
  }

  /** Gets a repository connection to the repository that contains the given class.
   * The RDF entity manager is responsible for closing the connection.
   *
   * @param clazz the given class
   * @return a repository connection to the repository that contains the given class
   */
  public RepositoryConnection getConnectionToRepositoryContainingClass(final Class<?> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    final String repositoryName = DistributedRepositoryManager.getInstance().getRepositoryNameForClass(clazz);
    if (repositoryName == null) {
      return null;
    } else {
      return getConnectionToNamedRepository(repositoryName);
    }
  }

  /** Gets a cached repository connection to the named repository.
   *
   * @param repositoryName the named repository
   * @return a repository connection to the named repository
   */
  public RepositoryConnection getConnectionToNamedRepository(final String repositoryName) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    if (distributedRepositoryManager == null) {
      // initialize the distributed repository manager
      DistributedRepositoryManager.getInstance();
    }
    assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null";

    RepositoryConnection repositoryConnection = repositoryConnectionDictionary.get(repositoryName);
    if (repositoryConnection == null) {
      repositoryConnection = distributedRepositoryManager.getRepositoryConnectionForRepositoryName(repositoryName);
      repositoryConnectionDictionary.put(repositoryName, repositoryConnection);
    }

    //Postconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    try {
      assert repositoryConnection.isOpen() : "repositoryConnection " + repositoryConnection + " must be open for " + repositoryName;
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }

    return repositoryConnection;
  }

  /** Removes all statements from specific contexts in the repository.
   *
   * @param repositoryName the repository name
   * @param contexts the context(s) to remove the data from. Note that this parameter is a vararg and as such is optional.
   * If no contexts are supplied the method operates on the entire repository.
   */
  public void clear(
          final String repositoryName,
          final Resource... contexts) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert contexts != null : "contexts must not be null";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    final List<JournalRequest> journalRequests = new ArrayList<>();
    try {
      if (contexts.length == 0) {
        // enumerate the statements in all contexts in order to journal their remove operations
        final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(null, null, null, false);
        while (repositoryResult.hasNext()) {
          final Statement statement = repositoryResult.next();
          journalRequests.add(new JournalRequest(
                  repositoryConnection.getRepository().getDataDir().getName(),
                  Constants.REMOVE_OPERATION,
                  statement));
        }
        repositoryConnection.clear();
      } else {
        for (final Resource context : contexts) {
          // enumerate the statements in this context in order to journal their remove operations
          final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(null, null, null, false, context);
          while (repositoryResult.hasNext()) {
            final Statement statement = repositoryResult.next();
            journalRequests.add(new JournalRequest(
                    repositoryConnection.getRepository().getDataDir().getName(),
                    Constants.REMOVE_OPERATION,
                    new ContextStatementImpl(statement.getSubject(), statement.getPredicate(), statement.getObject(), context)));
          }
          repositoryConnection.clear(context);
        }
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    journalWriter.write(journalRequests);
    if (isAutoCommit) {
      journalWriter.commit();
    }
  }

  /** Adds the supplied statement to the named repository, optionally to one or more named contexts.
   *
   * @param repositoryName the repository name
   * @param statement the statement to add
   * @param contexts the optional contexts
   */
  public void add(
          final String repositoryName,
          final Statement statement,
          final Resource... contexts) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert statement != null : "statement must not be null";
    assert contexts != null : "contexts must not be null";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    addStatement(repositoryConnection, statement, contexts);
  }

  /** Removes the given statement from the named repository, optionally from one or more named contexts.
   *
   * @param repositoryName the repository name
   * @param statement the statement to remove
   * @param contexts the optional contexts
   */
  public void remove(
          final String repositoryName,
          final Statement statement,
          final Resource... contexts) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert statement != null : "statement must not be null";
    assert contexts != null : "contexts must not be null";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    removeStatement(repositoryConnection, statement, contexts);
  }

  /** Gets the default context of the given persistent class.
   *
   * @param rdfEntityClass the persistent class
   * @return the default context
   */
  public URI getDefaultContext(final Class<?> rdfEntityClass) {
    return rdfEntityLoader.getDefaultContext(rdfEntityClass);
  }

  /** Gets the effective context of the given persistent class, given a potentially non-null override context.
   *
   * @param rdfEntityClass the persistent class
   * @param overrideContext the override context
   * @return the effective context
   */
  public URI getEffectiveContext(final Class<?> rdfEntityClass, final URI overrideContext) {
    //Preconditions
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    if (overrideContext == null) {
      return rdfEntityLoader.getDefaultContext(rdfEntityClass);
    } else {
      return overrideContext;
    }
  }

  /** Persists the given RDF entity as propositions in the RDF store.
   *
   * @param rdfEntity the RDF entity
   * @param repositoryName the repository name
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(final RDFPersistent rdfEntity, final String repositoryName) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    return rdfEntityPersister.persist(repositoryConnection, rdfEntity);
  }

  /** Persists the given RDF entity as propositions in the RDF store.
   *
   * @param rdfEntity the RDF entity
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(final RDFPersistent rdfEntity) {
    return persist(
            rdfEntity, // root RDF entity, same as the RDF entity in this case
            rdfEntity); // the RDF entity
  }

  /** Exports the given RDF entity as RDF statements into the given output stream. Use KBInitializer to import.
   *
   * @param rdfEntity the RDF entity
   * @param writer the export output writer
   */
  public void export(
          final RDFPersistent rdfEntity,
          final BufferedWriter writer) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert writer != null : "outputStream must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(rdfEntity, rdfEntity);
    rdfEntityPersister.export(repositoryConnection, rdfEntity, writer);
  }

  /** Persists the given RDF entity as propositions in the RDF store, with the given override context.
   *
   * @param rdfEntity the RDF entity
   * @param overrideContext the override context
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(final RDFPersistent rdfEntity, final URI overrideContext) {
    return persist(
            rdfEntity, // root RDF entity, same as the RDF entity in this case
            rdfEntity, // the RDF entity
            overrideContext);
  }

  /** Persists the given RDF entity.  The default preference is to persist the RDF entity in the same
   * repository in which the root RDF entity is persisted.
   *
   * @param rootRDFEntity the root RDF entity that contains the RDF entity
   * @param rdfEntity the RDF entity to persist
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(
          final RDFPersistent rootRDFEntity,
          final RDFPersistent rdfEntity) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    //assert isSerializable(rdfEntity) : "rdfEntity must be indeed serializable";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(rootRDFEntity, rdfEntity);
    return rdfEntityPersister.persist(repositoryConnection, rdfEntity);
  }

  /** Persists the given RDF entity.  The default preference is to persist the RDF entity in the same
   * repository in which the root RDF entity is persisted.
   *
   * @param rootRDFEntity the root RDF entity that contains the RDF entity
   * @param rdfEntity the RDF entity to persist
   * @param overrideContext the override context
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(
          final RDFPersistent rootRDFEntity,
          final RDFPersistent rdfEntity, // the RDF entity
          final URI overrideContext) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    //assert isSerializable(rdfEntity) : "rdfEntity must be indeed serializable";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(rootRDFEntity, rdfEntity);
    return rdfEntityPersister.persist(
            repositoryConnection,
            rdfEntity,
            overrideContext,
            null); // outputStream
  }

  /** Returns whether the given object is indeed serializable.
   *
   * @param obj the given object
   * @return whether the given object is indeed serializable
   */
  public static boolean isSerializable(final Serializable obj) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    try {
      oos = new ObjectOutputStream(out);
      oos.writeObject(obj);
      oos.close();
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    return out.toByteArray().length > 0;
  }

  /** Remove the statements in the RDF store that mention the URI that represents the given RDF entity.
   *
   * @param rdfEntity the given RDF entity
   * @param repositoryName the given repository name
   */
  public void remove(final RDFPersistent rdfEntity, final String repositoryName) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    rdfEntityRemover.remove(repositoryConnection, rdfEntity);
  }

  /** Remove the statements in the RDF store that mention the URI that represents the given RDF entity.
   *
   * @param rdfEntity the given RDF entity
   */
  public void remove(final RDFPersistent rdfEntity) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";

    remove(rdfEntity, rdfEntity);
  }

  /** Removes the given RDF entity.  The default preference is to remove the RDF entity from the same
   * repository in which the root RDF entity is persisted.
   *
   * @param rootRDFEntity the root RDF entity that contains the RDF entity
   * @param rdfEntity the RDF entity to remove
   */
  public void remove(
          final RDFPersistent rootRDFEntity,
          final RDFPersistent rdfEntity) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(rootRDFEntity, rdfEntity);
    rdfEntityRemover.remove(repositoryConnection, rdfEntity);
  }

  /** Sets the identification URI of the given RDF entity.
   *
   * @param rdfEntity the RDF entity
   */
  public void setIdFor(final RDFPersistent rdfEntity) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";

    rdfEntityPersister.setIdFor(rdfEntity);
  }

  /** Sets the identification URI of the given RDF entity to the given id.
   *
   * @param rdfEntity the RDF entity
   * @param id the given id
   */
  public void setIdFor(final RDFPersistent rdfEntity, final URI id) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert id != null : "id must not be null";

    rdfEntityPersister.setIdFor(rdfEntity, id);
  }

  /** Creates a default id for the given RDF entity, ignoring the annotated subject.
   *
   * @param rdfEntity the given RDF entity
   * @return a default id for the given RDF entity
   */
  public static URI createId(final Object rdfEntity) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";

    return new URIImpl(Constants.TEXAI_NAMESPACE + rdfEntity.getClass().getName() + "_" + UUID.randomUUID().toString());
  }

  /** Gets the indicator to validate persisted statements.
   *
   * @return the indicator whether to validate persisted statements
   */
  public boolean areStatementsValidated() {
    return rdfEntityPersister.areStatementsValidated();
  }

  /** Sets the indicator to validate persisted statements.
   *
   * @param areStatementsValidated the indicator whether to validate persisted statements
   */
  public void setAreStatementsValidated(final boolean areStatementsValidated) {
    rdfEntityPersister.setAreStatementsValidated(areStatementsValidated);
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI.
   *
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity
   */
  public Object find(final URI instanceURI) {
    //Preconditions
    assert instanceURI != null : "instanceURI must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(instanceURI);
    return rdfEntityLoader.find(repositoryConnection, instanceURI);
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI and class.  If the class is null,
   * then the class name is looked up from the RDF store before instantiating the entity.
   *
   * @param <T> the RDF entity class
   * @param clazz the RDF entity class
   * @param instanceURI the URI that represents the RDF entity
   * @param repositoryName the repository name
   * @return the RDF entity
   */
  public <T> T find(
          final Class<T> clazz,
          final URI instanceURI,
          final String repositoryName) {
    //Preconditions
    assert clazz != null : "clazz must not be null";
    assert instanceURI != null : "instanceURI must not be null";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);
    return rdfEntityLoader.find(repositoryConnection, clazz, instanceURI);
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI and class.  If the class is null,
   * then the class name is looked up from the RDF store before instantiating the entity.
   *
   * @param <T> the RDF entity class
   * @param clazz the RDF entity class
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity
   */
  public <T> T find(
          final Class<T> clazz,
          final URI instanceURI) {
    //Preconditions
    assert clazz != null : "clazz must not be null";
    assert instanceURI != null : "instanceURI must not be null";

    try {
      final RepositoryConnection repositoryConnection = getRepositoryConnection(instanceURI);
      return rdfEntityLoader.find(repositoryConnection, clazz, instanceURI);
    } catch (Throwable ex) {
      LOGGER.error("class: " + clazz.getName());
      LOGGER.error("instanceURI: " + instanceURI);
      if (distributedRepositoryManager == null) {
        // initialize the distributed repository manager
        distributedRepositoryManager = DistributedRepositoryManager.getInstance();
      }
      assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null";
      LOGGER.error("repository: " + distributedRepositoryManager.getRepositoryNameForInstance(instanceURI));
      throw new TexaiException(ex);
    }
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI and class.  If the class is null,
   * then the class name is looked up from the RDF store before instantiating the entity.
   *
   * @param <T> the rdf entity class
   * @param rootEntityClass the root RDF entity class that governs which repository contains the RDF entity to be found
   * @param clazz the RDF entity class
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity
   */
  public <T> T find(
          final Class<?> rootEntityClass,
          final Class<T> clazz,
          final URI instanceURI) {
    //Preconditions
    assert rootEntityClass != null : "rootEntityClass must not be null";
    assert clazz != null : "clazz must not be null";
    assert instanceURI != null : "instanceURI must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(rootEntityClass);
    return rdfEntityLoader.find(repositoryConnection, clazz, instanceURI);
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its id string, without clearing
   * the dictionary of connected RDF entities.
   *
   * @param <T> the RDF entity class
   * @param clazz the RDF entity class
   * @param idString the id string that represents the RDF entity
   * @return the RDF entity or null if not found
   */
  public <T> T find(
          final Class<T> clazz,
          final String idString) {
    //Preconditions
    assert clazz != null : "clazz must not be null";
    assert idString != null : "idString must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(repositoryConnection, clazz, idString);
  }

  /** Finds and loads RDF entities having the given RDF predicate and RDF value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param rdfValue the RDF value of the predicate
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final Value rdfValue,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "rdfValue must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(repositoryConnection, predicate, rdfValue, clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and RDF value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param rdfValue the RDF value of the predicate
   * @param clazz the class of the desired RDF entities
   * @param repositoryName the repository name
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final Value rdfValue,
          final Class<T> clazz,
          final String repositoryName) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "rdfValue must not be null";
    assert clazz != null : "clazz must not be null";
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";

    final RepositoryConnection repositoryConnection = this.getConnectionToNamedRepository(repositoryName);
    return rdfEntityLoader.find(repositoryConnection, predicate, rdfValue, clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and RDF value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param rdfValue the RDF value of the predicate
   * @param overrideContext the override context
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final Value rdfValue,
          final URI overrideContext,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "rdfValue must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(repositoryConnection, predicate, rdfValue, overrideContext, clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and boolean value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the boolean value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final boolean value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new BooleanLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and byte value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the byte value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final byte value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and double value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the double value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final double value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and float value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the float value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final float value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and int value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the int value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final int value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and long value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the long value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final long value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and short value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the short value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final short value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new NumericLiteralImpl(value),
            clazz);
  }

  /** Finds and loads RDF entities having the given RDF predicate and String value.
   *
   * @param <T> the RDF entity class
   * @param predicate the given RDF predicate
   * @param value the String value
   * @param clazz the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final URI predicate,
          final String value,
          final Class<T> clazz) {
    //Preconditions
    assert predicate != null : "predicate must not be null";
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.find(
            repositoryConnection,
            predicate,
            new LiteralImpl(value),
            clazz);
  }

  /** Returns an iterator over the set of RDF entity terms that represent instances of the given RDF entity class.
   *
   * @param <T> the RDF entity class
   * @param clazz the given RDF entity class
   * @param overrideContext the override context
   * @return an iterator over the set of RDF entity terms that represent instances of the given RDF entity class
   */
  public <T> Iterator<T> rdfEntityIterator(
          final Class<T> clazz,
          final URI overrideContext) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.rdfEntityIterator(
            repositoryConnection,
            clazz,
            overrideContext);
  }

  /** Returns an iterator over the set of RDF entity terms that represent instances of the given RDF entity class.
   *
   * @param <T> the RDF entity class
   * @param clazz the given RDF entity class
   * @return an iterator over the set of RDF entity terms that represent instances of the given RDF entity class
   */
  public <T> Iterator<T> rdfEntityIterator(
          final Class<T> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    final RepositoryConnection repositoryConnection = getRepositoryConnection(clazz);
    return rdfEntityLoader.rdfEntityIterator(
            repositoryConnection,
            clazz,
            null);
  }

  /** Gets the type queries object.
   *
   * @return the type queries object
   */
  public TypeQueries getTypeQueries() {
    if (typeQueries == null) {
      typeQueries = new TypeQueries(this);
    }
    return typeQueries;
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

    return getTypeQueries().isIndividualTerm(repositoryName, term);
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

    return getTypeQueries().isClassTerm(repositoryName, term);
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

    return getTypeQueries().isContextTerm(repositoryName, term);
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

    return getTypeQueries().isPropertyTerm(repositoryName, term);
  }

  /** Returns whether the given term is a situation.
   *
   * @param repositoryName the repository name
   * @param term the given term
   * @return whether the given term is a situation
   */
  public boolean isSituationTerm(
          final String repositoryName,
          final URI term) {
    //Preconditions
    assert repositoryName != null : "repositoryName must not be null";
    assert !repositoryName.isEmpty() : "repositoryName must not be empty";
    assert term != null : "term must not be null";

    return getSubClassOfQueries().isSituationClass(repositoryName, term);
  }

  /** Gets the subclass-of queries object.
   *
   * @return the subclass-of queries object
   */
  public SubClassOfQueries getSubClassOfQueries() {
    if (subClassOfQueries == null) {
      subClassOfQueries = new SubClassOfQueries(this);
    }
    return subClassOfQueries;
  }

  /** Gets the subproperty-of queries object.
   *
   * @return the subproperty-of queries object
   */
  public SubPropertyOfQueries getSubPropertyOfQueries() {
    if (subPropertyOfQueries == null) {
      subPropertyOfQueries = new SubPropertyOfQueries(this);
    }
    return subPropertyOfQueries;
  }

  /** Asserts the defining statements for a new RDF class.  This method is useful in the case where a new RDF class is required
   * but no direct instances of the RDF class will ever be instantiated as Java objects.
   *
   * @param repositoryName the repository name
   * @param classURI the RDF class
   * @param comment the RDF class comment
   * @param typeURIs the RDF class typeURIs
   * @param subClassOfURIs the RDF classes for which this RDF class is a subclass
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFClass(
          final String repositoryName,
          final URI classURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> subClassOfURIs,
          final BufferedWriter writer) {
    rdfUtility.defineRDFClass(
            repositoryName,
            classURI,
            comment,
            typeURIs,
            subClassOfURIs,
            writer);
  }

  /** Asserts the defining statements for a new RDF individual.  This method is useful in the case where a new RDF individual is required
   * but no corresponding Java class has been created.
   *
   * @param repositoryName the repository name
   * @param individualURI the individual URI
   * @param comment the RDF individual comment
   * @param typeURIs the RDF class typeURIs
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFIndividual(
          final String repositoryName,
          final URI individualURI,
          final String comment,
          final List<URI> typeURIs,
          final BufferedWriter writer) {
    rdfUtility.defineRDFIndividual(
            repositoryName,
            individualURI,
            comment,
            typeURIs,
            writer);
  }

  /** Asserts the defining statements for a new RDF context.
   *
   * @param repositoryName the repository name
   * @param contextURI the RDF context URI
   * @param comment the RDF class comment
   * @param typeURIs the RDF context typeURIs
   * @param genlMtURIs the RDF contexts for which this RDF context is a specialization
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFContext(
          final String repositoryName,
          final URI contextURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> genlMtURIs,
          final BufferedWriter writer) {
    rdfUtility.defineRDFContext(
            repositoryName,
            contextURI,
            comment,
            typeURIs,
            genlMtURIs,
            writer);
  }

  /** Asserts that the two given classes are disjoint.
   *
   * @param repositoryName the repository name
   * @param classURI1 the first given class
   * @param classURI2 the second given class
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void assertDisjoint(
          final String repositoryName,
          final URI classURI1,
          final URI classURI2,
          final BufferedWriter writer) {
    rdfUtility.assertDisjoint(
            repositoryName,
            classURI1,
            classURI2,
            writer);
  }

  /**
   * Asserts the defining statements for a new RDF predicate.
   *
   * @param repositoryName the repository name
   * @param predicateURI the RDF predicate
   * @param comment the RDF predicate comment
   * @param typeURIs the RDF predicate typeURIs
   * @param subPropertyOfs the RDF predicates for which this RDF predicate is a subproperty
   * @param domainURI the type of the RDF subject that can be used with this predicate
   * @param rangeURI the type of the RDF object that can be used with this predicate
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  public void defineRDFPredicate(
          final String repositoryName,
          final URI predicateURI,
          final String comment,
          final List<URI> typeURIs,
          final List<URI> subPropertyOfs,
          final URI domainURI,
          final URI rangeURI,
          final BufferedWriter writer) {
    rdfUtility.defineRDFPredicate(
            repositoryName,
            predicateURI,
            comment,
            typeURIs,
            subPropertyOfs,
            domainURI,
            rangeURI,
            writer);
  }

  /** Formats an RDF statement.
   *
   * @param statement the statement
   * @return the formatted statement in which namespaces are represented by prefixes
   */
  public String formatStatement(final Statement statement) {
    return RDFUtility.formatStatement(statement);
  }

  /** Persists the given RDF entity.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity to persist
   * @return the instance URI that represents the RDF entity
   */
  protected URI persist(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";

    return rdfEntityPersister.persist(repositoryConnection, rdfEntity);
  }

  /** Adds the supplied statement to the connected repository, optionally to one or more named contexts.
   *
   * @param repositoryConnection the repository connection
   * @param statement the statement to add
   * @param contexts the optional contexts
   */
  protected void addStatement(
          final RepositoryConnection repositoryConnection,
          final Statement statement,
          final Resource... contexts) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert statement != null : "statement must not be null";
    assert contexts != null : "contexts must not be null";

    final List<JournalRequest> journalRequests = new ArrayList<>();
    try {
      if (contexts.length == 0) {
        repositoryConnection.add(statement);
        journalRequests.add(new JournalRequest(
                repositoryConnection.getRepository().getDataDir().getName(),
                Constants.ADD_OPERATION,
                statement));
      } else {
        for (final Resource context : contexts) {
          repositoryConnection.add(statement, context);
          journalRequests.add(new JournalRequest(
                  repositoryConnection.getRepository().getDataDir().getName(),
                  Constants.ADD_OPERATION,
                  new ContextStatementImpl(statement.getSubject(), statement.getPredicate(), statement.getObject(), context)));
        }
      }
    } catch (final AssertionError | RepositoryException ex) {
      LOGGER.error("repository: " + repositoryConnection.getRepository().getDataDir());
      throw new TexaiException(ex);
    }
    journalWriter.write(journalRequests);
    if (isAutoCommit) {
      journalWriter.commit();
    }
  }

  /** Removes the given statement from specific contexts in the repository.
   *
   * @param repositoryConnection the repository connection
   * @param statement the given statement
   * @param contexts the context(s) to remove the data from. Note that this parameter is a vararg and as such is optional.
   * If no contexts are supplied the method operates on the entire repository.
   */
  protected void removeStatement(
          final RepositoryConnection repositoryConnection,
          final Statement statement,
          final Resource... contexts) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert statement != null : "statement must not be null";
    assert contexts != null : "contexts must not be null";

    final List<JournalRequest> journalRequests = new ArrayList<>();
    try {
      if (contexts.length == 0) {
        journalRequests.add(new JournalRequest(
                repositoryConnection.getRepository().getDataDir().getName(),
                Constants.REMOVE_OPERATION,
                statement));
        repositoryConnection.remove(statement);
      } else {
        for (final Resource context : contexts) {
          journalRequests.add(new JournalRequest(
                  repositoryConnection.getRepository().getDataDir().getName(),
                  Constants.REMOVE_OPERATION,
                  new ContextStatementImpl(statement.getSubject(), statement.getPredicate(), statement.getObject(), context)));
          repositoryConnection.remove(statement, context);
        }
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    journalWriter.write(journalRequests);
    if (isAutoCommit) {
      journalWriter.commit();
    }
  }

  /** Rolls back all updates that have been performed as part of this connection so far.
   *
   * @param repositoryConnection the repository connection
   */
  protected void rollback(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";

    try {
      repositoryConnection.rollback();
      journalWriter.rollback();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Loads the given RDF entity field.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity
   * @param field the field to be loaded
   * @param rdfProperty the RDF property the associates field value(s) in the knowledge base
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   * @return the value of the loaded field
   */
  protected Object loadRDFEntityField(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    return rdfEntityLoader.loadLazyRDFEntityField(
            repositoryConnection,
            rdfEntity,
            field,
            rdfProperty,
            predicateValuesDictionary);
  }

  /** Gets a repository connection for the given RDF entity URI.
   *
   * @param uri the given RDF entity URI
   * @return a repository connection
   */
  protected RepositoryConnection getRepositoryConnection(final URI uri) {
    //Preconditions
    assert uri != null : "uri must not be null";

    if (distributedRepositoryManager == null) {
      // initialize the distributed repository manager
      distributedRepositoryManager = DistributedRepositoryManager.getInstance();
    }
    assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null";

    final String repositoryName = distributedRepositoryManager.getRepositoryNameForInstance(uri);
    if (repositoryName == null) {
      throw new TexaiException("repository name not found for " + uri);
    }
    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);

    //Postconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";

    return repositoryConnection;
  }

  /** Gets a repository connection for the given RDF entity class.
   *
   * @param clazz the given RDF entity class
   * @return a repository connection
   */
  private RepositoryConnection getRepositoryConnection(final Class<?> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    if (distributedRepositoryManager == null) {
      // initialize the distributed repository manager
      DistributedRepositoryManager.getInstance();
    }
    assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null";

    final String repositoryName = distributedRepositoryManager.getRepositoryNameForClassName(clazz.getName());
    if (repositoryName == null) {
      DistributedRepositoryManager.logClassNameRepositoryDictionary();
      throw new TexaiException("repository name not found for " + clazz);
    }
    final RepositoryConnection repositoryConnection = getConnectionToNamedRepository(repositoryName);

    //Postconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";

    return repositoryConnection;
  }

  /** Returns a repository connection for the given RDF entity, returning a repository connection for the root RDF entity
   * if the repository connection cannot be determined from the given RDF entity alone.
   *
   * @param rootRDFEntity the root RDF entity that contains the RDF entity
   * @param rdfEntity the RDF entity to persist
   * @return a repository connection
   */
  private RepositoryConnection getRepositoryConnection(
          final RDFPersistent rootRDFEntity,
          final RDFPersistent rdfEntity) {
    //Preconditions
    assert rootRDFEntity != null : "rootRDFEntity must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";

    if (distributedRepositoryManager == null) {
      // initialize the distributed repository manager
      DistributedRepositoryManager.getInstance();
    }
    assert distributedRepositoryManager != null : "distributedRepositoryManager must not be null";

    if (rootRDFEntity.getId() == null) {
      setIdFor(rootRDFEntity);
    }
    final String rootRepositoryName = distributedRepositoryManager.getRepositoryNameForInstance(rootRDFEntity);
    if (rootRepositoryName == null) {
      DistributedRepositoryManager.logClassNameRepositoryDictionary();
      throw new TexaiException("repository name not found for " + rootRDFEntity.getId()
              + "\nclass: " + DistributedRepositoryManager.parseClassNameFromURI(rootRDFEntity.getId()));
    }
    final RepositoryConnection rootRepositoryConnection = getConnectionToNamedRepository(rootRepositoryName);
    assert rootRepositoryConnection != null;
    RepositoryConnection repositoryConnection;
    if (rdfEntity.getId() == null) {
      setIdFor(rdfEntity);
    }
    final String repositoryName = distributedRepositoryManager.getRepositoryNameForInstance(rdfEntity);
    if (repositoryName == null) {
      repositoryConnection = rootRepositoryConnection;
    } else {
      repositoryConnection = getConnectionToNamedRepository(repositoryName);
    }

    //Postconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";

    return repositoryConnection;
  }
}
