/*
 * RDFEntityPersister.java
 *
 * Created on October 31, 2006, 11:23 AM
 *
 * Description: This class persists domain entities into the RDF store,
 * mapping entity associations onto RDF triples.
 *
 * Copyright (C) 2006 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;
import net.jcip.annotations.NotThreadSafe;
import net.sf.cglib.proxy.Factory;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.Constants;
import org.texai.kb.persistence.lazy.LazyList;
import org.texai.kb.persistence.lazy.LazyMap;
import org.texai.kb.persistence.lazy.LazySet;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;

/** This helper class persists new or updated entities to the Sesame RDF store.  New RDF classes and predicates are automatically
 * defined in the RDF store. The persist() method cascades to new RDF entities that are field values of the entity being persisted.
 *
 * @author reed
 */
@NotThreadSafe
public final class RDFEntityPersister extends AbstractRDFEntityAccessor {  // NOPMD

  // session variables
  /** the logger */
  private final Logger logger = Logger.getLogger(RDFEntityPersister.class);                         // NOPMD
  /** the indicator whether the debug logging level is enabled */
  private final boolean isDebugEnabled;
  /** the entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the RDF utility bean */
  private final RDFUtility rdfUtility;
  /** the set of defined class and predicate URIs */
  private final Set<URI> definedClassAndPredicateURIs;
  /** the URI http://sw.cyc.com/2006/07/27/cyc/Collection */
  private static final URI URI_COLLECTION = new URIImpl(Constants.TERM_COLLECTION);
  /** the URI http://sw.cyc.com/2006/07/27/cyc/conceptuallyRelated */
  private static final URI URI_CONCEPTUALLY_RELATED = new URIImpl(Constants.TERM_CONCEPTUALLY_RELATED);
  //private static final URI URI_DOMAIN_ENTITY_CLASS_NAME = new URIImpl(Constants.TERM_DOMAIN_ENTITY_CLASS_NAME);
  /** the URI http://texai.org/texai/overrideContext */
  private static final URI URI_OVERRIDE_CONTEXT = new URIImpl(Constants.TERM_OVERRIDE_CONTEXT);
  /** the URI http://sw.cyc.com/2006/07/27/cyc/FirstOrderCollection */
  private static final URI URI_FIRST_ORDER_COLLECTION = new URIImpl(Constants.TERM_FIRST_ORDER_COLLECTION);
  /** the URI http://sw.cyc.com/2006/07/27/cyc/UniversalVocabularyMt */
  private static final URI URI_UNIVERSAL_VOCABULARY_MT = new URIImpl(Constants.TERM_UNIVERSAL_VOCABULARY_MT);
  // variables used to persist a particular entity
  /** the indicator that the entity instance is new */
  private boolean isNewDomainInstance;
  /** the stack of RDF entity information that allows the session to perform recursive method calls */
  private final Stack<RDFEntityInfo> rdfEntityInfoStack;
  /** the indicator to validate persisted statements */
  private boolean areStatementsValidated = false;
  /** the context for recursive calls to the persist method. */
  private URI recursiveContextURI;
  /** the connected RDF entities cache, entity hash --> id */
  final Cache identityCache = CacheManager.getInstance().getCache(Constants.CACHE_CONNECTED_RDF_ENTITY_URIS);

  /** Creates a new instance of RDFEntityPersister.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public RDFEntityPersister(final RDFEntityManager rdfEntityManager) {
    super();
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
    rdfUtility = new RDFUtility(rdfEntityManager);
    definedClassAndPredicateURIs = new HashSet<>();
    rdfEntityInfoStack = new Stack<>();
    isDebugEnabled = logger.isDebugEnabled();

    //Postconditions
    assert identityCache != null : "cache not found for: " + Constants.CACHE_CONNECTED_RDF_ENTITY_URIS;
  }

  /** Persists the given RDF entity as propositions in the RDF store.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity
   * @return the instance URI that represents the RDF entity
   */
  public URI persist(final RepositoryConnection repositoryConnection, final RDFPersistent rdfEntity) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName()
            + ") superclass: (" + rdfEntity.getClass().getSuperclass().getName()
            + ") must be have a @RDFEntity class level annotation in " + Arrays.toString(rdfEntity.getClass().getAnnotations());

    return persist(
            repositoryConnection,
            rdfEntity,
            null, // overrideContext
            null); // outputStream
  }

  /** Exports the given RDF entity as RDF statements on the given output stream.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   * @return the instance URI that represents the RDF entity
   */
  public URI export(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    assert writer != null : "writer must not be null";
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName()
            + ") superclass: (" + rdfEntity.getClass().getSuperclass().getName()
            + ") must be have a @RDFEntity class level annotation in " + Arrays.toString(rdfEntity.getClass().getAnnotations());

    return persist(
            repositoryConnection,
            rdfEntity,
            null, // overrideContext
            writer);
  }

  /** Persists the given RDF entity as propositions in the RDF store.  The default preference is to persist the RDF entity in the same
   * repository in which the root RDF entity is persisted.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity
   * @param overrideContext the override context, or null if the default context is to be used
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   * @return the instance URI that represents the RDF entity
   */
  @SuppressWarnings("deprecation")
  public URI persist(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final URI overrideContext,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName() + ") must be have a @RDFEntity class level annotation in " + Arrays.toString(rdfEntity.getClass().getAnnotations());
    assert !rdfEntity.getClass().getName().contains("CGLIB") : "proxy entity " + rdfEntity.getClass().getName()
            + " cannot be persisted. Fix by first accessing a proxy method to replace it by the instantiated object in the containing field";

    final boolean isAutoCommit;
    try {
      isAutoCommit = repositoryConnection.isAutoCommit();
      if (isAutoCommit) {
        // perform persistence operations within a transaction to avoid the otherwise unsatisfactory performance resulting from auto-commiting each
        // operation
        repositoryConnection.setAutoCommit(false);
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    final boolean wasStackEmpty = rdfEntityInfoStack.empty();
    initializeAbstractSessionState();
    initializeSessionState();
    if (isDebugEnabled) {
      logger.debug(stackLevel() + "persisting " + rdfEntity);
    }
    setRDFEntity(rdfEntity);
    setRDFEntityClass(rdfEntity.getClass());
    gatherAnnotationsForRDFEntityClass();
    setOverrideContextURI(overrideContext);
    configureRDFEntitySettings();
    if (!definedClassAndPredicateURIs.contains(getClassURI())) {
      persistTypes(
              repositoryConnection,
              writer);
      persistSubClassOfs(
              repositoryConnection,
              writer);
      definedClassAndPredicateURIs.add(getClassURI());
    }
    final boolean isExport = writer != null;
    findOrCreateDomainInstanceURI(
            repositoryConnection,
            writer);
    persistFields(
            repositoryConnection,
            writer);
    if (!isExport) {
      if (isAutoCommit) {
        try {
          repositoryConnection.commit();
          repositoryConnection.setAutoCommit(true);
        } catch (final RepositoryException ex) {
          throw new TexaiException(ex);
        }
      }
    }

    //Postconditions
    assert getInstanceURI() != null : "instanceURI must not be null";
    assert wasStackEmpty == rdfEntityInfoStack.empty() : "beginning stack empty status " + wasStackEmpty + " must equal ending stack status " + rdfEntityInfoStack.empty();

    return getInstanceURI();
  }

  /** Sets the identification URI of the given RDF entity.
   *
   * @param rdfEntity the RDF entity
   */
  public void setIdFor(final RDFPersistent rdfEntity) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert rdfEntity.getId() == null : "rdfEntity ID must be null in " + rdfEntity;
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName() + ") must be have a @RDFEntity class level annotation in " + Arrays.toString(rdfEntity.getClass().getAnnotations());

    setIdFor(rdfEntity, null);
  }

  /** Sets the identification URI of the given RDF entity to the given id.
   *
   * @param rdfEntity the RDF entity
   * @param id the given id
   */
  @SuppressWarnings("unchecked")
  public void setIdFor(final RDFPersistent rdfEntity, final URI id) {
    //Preconditions
    assert rdfEntity != null : "rdfEntity must not be null";
    assert rdfEntity.getId() == null : "rdfEntity ID must be null in " + rdfEntity;
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName() + ") must be have a @RDFEntity class level annotation in " + Arrays.toString(rdfEntity.getClass().getAnnotations());

    final boolean wasStackEmpty = rdfEntityInfoStack.empty();
    initializeAbstractSessionState();
    initializeSessionState();
    if (isDebugEnabled) {
      logger.debug(stackLevel() + "setting ID for " + rdfEntity);
    }
    setRDFEntity(rdfEntity);
    setRDFEntityClass(rdfEntity.getClass());
    gatherAnnotationsForRDFEntityClass();
    configureRDFEntitySettings();
    final Field idField = getIdField();
    if (idField == null) {
      throw new TexaiException("Id field not found for RDF entity " + getRDFEntity());
    }
    if (!idField.isAccessible()) {
      AccessController.doPrivileged(new SetAccessibleAction(idField));

    }
    final URI id1;
    if (id == null) {
      id1 = makeURI(getClassURI() + "_" + UUID.randomUUID().toString());
    } else {
      id1 = id;
    }
    try {
      idField.set(getRDFEntity(), id1);
    } catch (final IllegalArgumentException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert rdfEntity.getId() != null : "rdfEntity ID must not be null, perhaps there is a redundant @Id field in a superclass";
    assert wasStackEmpty == rdfEntityInfoStack.empty() : "beginning stack empty status " + wasStackEmpty + " must equal ending stack status " + rdfEntityInfoStack.empty();
  }

  /** Provides a container for a privileged action. */
  static class SetAccessibleAction implements PrivilegedAction<Object> {

    /** the field to set accessible */
    private final Field field;

    /** Constructs a new SetAccessibleAction instance.
     *
     * @param field the field to set accessible
     */
    public SetAccessibleAction(final Field field) {
      //Preconditions
      assert field != null : "field must not be null";

      this.field = field;
    }

    /** Performs the privileged action.
     *
     * @return null - not used
     */
    @Override
    public Object run() {
      field.setAccessible(true);
      return null; // nothing to return
    }
  }

  /** Gets the indicator to validate persisted statements.
   *
   * @return the indicator whether to validate persisted statements
   */
  public boolean areStatementsValidated() {
    return areStatementsValidated;
  }

  /** Sets the indicator to validate persisted statements.
   *
   * @param areStatementsValidated the indicator whether to validate persisted statements
   */
  public void setAreStatementsValidated(final boolean areStatementsValidated) {
    this.areStatementsValidated = areStatementsValidated;
  }

  /** Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }

  /** Gathers the existing type classes for which the entity class is a direct instance.
   *
   * @param repositoryConnection the repository connection
   * @return the existing type classes names for which the entity class is a direct instance
   */
  private Set<URI> gatherExistingTypes(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getClassURI() != null : "classURI must not be null";

    final Set<URI> existingTypeURIs = new HashSet<>();
    try {
      final TupleQuery existingTypesTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT typeTerm FROM {s} rdf:type {typeTerm}");
      existingTypesTupleQuery.setBinding("s", getClassURI());
      final TupleQueryResult tupleQueryResult = existingTypesTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        existingTypeURIs.add((URI) tupleQueryResult.next().getBinding("typeTerm").getValue());     // NOPMD
      }
      tupleQueryResult.close();
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return existingTypeURIs;
  }

  /** Gathers the existing classes for which the entity class is a direct subclass.
   *
   * @param repositoryConnection the repository connection
   * @return the existing classes for which the entity class is a direct subclass
   */
  private Set<URI> gatherExistingSubClassOfs(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getClassURI() != null : "classURI must not be null";

    final Set<URI> existingSubClassOfURIs = new HashSet<>();
    try {
      final TupleQuery existingSubClassOfsTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT superClassTerm FROM {classTerm} rdfs:subClassOf {superClassTerm}");
      existingSubClassOfsTupleQuery.setBinding("classTerm", getClassURI());
      final TupleQueryResult tupleQueryResult = existingSubClassOfsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        existingSubClassOfURIs.add((URI) tupleQueryResult.next().getBinding("superClassTerm").getValue());
      }
      tupleQueryResult.close();
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
    return existingSubClassOfURIs;
  }

  /** Persists the RDF entity class typeURI statements.
   *
   * @param repositoryConnection the repository connection
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void persistTypes(
          final RepositoryConnection repositoryConnection,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getClassURI() != null : "classURI must not be null";

    final Set<URI> existingTypeURIs;
    if (writer == null) {
      existingTypeURIs = gatherExistingTypes(repositoryConnection);
    } else {
      // when exporting, write out the type RDF statements
      existingTypeURIs = new HashSet<>();
    }
    if (existingTypeURIs.isEmpty() && getTypeURIs().length == 0) {
      // default to FirstOrderCollection for a new domain class
      final URI[] typeURIs = {URI_FIRST_ORDER_COLLECTION};
      setTypeURIs(typeURIs);
    }
    for (final URI typeURI : getTypeURIs()) {
      if (!existingTypeURIs.contains(typeURI)) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "adding type: " + typeURI);
        }
        final Statement statement = getValueFactory().createStatement(
                getClassURI(),
                RDF.TYPE,
                typeURI,
                URI_UNIVERSAL_VOCABULARY_MT);
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }
      }
    }
    if (getTypeURIs().length > 0 && writer == null) {
      // if the RDFEntity annotation specified typeURI, then delete any existing typeURI relationships not contained in the specification
      final List<URI> typeURIsList = Arrays.asList(getTypeURIs());
      for (final URI existingTypeURI : existingTypeURIs) {
        if (!typeURIsList.contains(existingTypeURI)) {
          if (isDebugEnabled) {
            logger.debug(stackLevel() + "deleting existingType: " + existingTypeURI);
          }
          final Statement statement = getValueFactory().createStatement(
                  getClassURI(),
                  RDF.TYPE,
                  existingTypeURI,
                  URI_UNIVERSAL_VOCABULARY_MT);
          rdfEntityManager.removeStatement(repositoryConnection, statement);
          if (logger.isInfoEnabled()) {
            logger.info("removed: " + RDFUtility.formatStatement(statement));    // NOPMD
          }
        }
      }
    }
  }

  /** Persists the RDF entity class genls propositions.
   *
   * @param repositoryConnection the repository connection
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void persistSubClassOfs(
          final RepositoryConnection repositoryConnection,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getClassURI() != null : "classURI must not be null";

    final Set<URI> existingSubClassOfURIs;
    if (writer == null) {
      existingSubClassOfURIs = gatherExistingSubClassOfs(repositoryConnection);
    } else {
      // when exporting, write out the subClassOf RDF statements
      existingSubClassOfURIs = new HashSet<>();
    }
    if (existingSubClassOfURIs.isEmpty() && getSubClassOfURIs().length == 0) {
      // default to Collection for a new domain class
      final URI[] subClassOfURIs = {URI_COLLECTION};
      setSubClassOfURIs(subClassOfURIs);
    }
    for (final URI subClassOfURI : getSubClassOfURIs()) {
      if (!existingSubClassOfURIs.contains(subClassOfURI)) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "adding subClassOf: " + subClassOfURI);
        }
        final Statement statement = getValueFactory().createStatement(
                getClassURI(),
                RDFS.SUBCLASSOF,
                subClassOfURI,
                URI_UNIVERSAL_VOCABULARY_MT);
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }
      }
    }
    if (getSubClassOfURIs().length > 0 && writer == null) {
      // if the RDFEntity annotation specified subClassOf, then delete any existing subClassOf relationships not contained in the specification
      final List<URI> subClassOfURIsList = Arrays.asList(getSubClassOfURIs());
      for (final URI existingSubClassOfURI : existingSubClassOfURIs) {
        if (!subClassOfURIsList.contains(existingSubClassOfURI)) {
          if (isDebugEnabled) {
            logger.debug(stackLevel() + "deleting : existingSubClassOf" + existingSubClassOfURI);
          }
          final Statement statement = getValueFactory().createStatement(
                  getClassURI(),
                  RDFS.SUBCLASSOF,
                  existingSubClassOfURI,
                  URI_UNIVERSAL_VOCABULARY_MT);
          rdfEntityManager.removeStatement(repositoryConnection, statement);
          if (logger.isInfoEnabled()) {
            logger.info("removed: " + RDFUtility.formatStatement(statement));    // NOPMD
          }
        }
      }
    }
  }

  /** Finds or creates the domain instance URI.
   *
   * @param repositoryConnection the repository connection
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void findOrCreateDomainInstanceURI(
          final RepositoryConnection repositoryConnection,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getFieldAnnotationDictionary() != null : "fieldAnnotationDictionary must not be null";
    assert getClassURI() != null : "classURI must not be null";
    assert getRDFEntity() != null : "rdfEntity() must not be null";

    final Field idField = getIdField();
    if (idField == null) {
      throw new TexaiException("Id field not found for RDF entity " + getRDFEntity());
    }
    if (!idField.isAccessible()) {
      idField.setAccessible(true);
    }
    Object value;
    try {
      value = idField.get(getRDFEntity());
    } catch (final IllegalArgumentException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }
    if (value == null) {
      setInstanceURI(makeURI(getClassURI() + "_" + UUID.randomUUID().toString()));
      if (logger.isDebugEnabled()) {
        logger.debug("persisting new id for " + getRDFEntity().toString() + "  " + getInstanceURI());
      }
      try {
        if (URI.class.isAssignableFrom(idField.getType())) {
          idField.set(getRDFEntity(), getInstanceURI());
        } else {
          throw new TexaiException("Id field is not a supported type");
        }
      } catch (final IllegalArgumentException | IllegalAccessException ex) {
        throw new TexaiException(ex);
      }
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  created new instance " + getInstanceURI());
      }
    } else {
      if (URI.class.isAssignableFrom(value.getClass())) {
        setInstanceURI((URI) value);
      } else {
        setInstanceURI(makeURI(value.toString()));
      }
      if (logger.isInfoEnabled()) {
        logger.info("persisting existing id for " + getRDFEntity().toString() + "  " + getInstanceURI());
      }
      isNewDomainInstance = false;
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  Id specifies existing instance " + getInstanceURI());
      }
    }
    final Statement typeStatement = getValueFactory().createStatement(
            getInstanceURI(),
            RDF.TYPE,
            getClassURI(),
            URI_UNIVERSAL_VOCABULARY_MT);
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, typeStatement);
      if (logger.isInfoEnabled()) {
        logger.info("added: " + RDFUtility.formatStatement(typeStatement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
      }
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(typeStatement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }

    if (!getEffectiveContextURI().equals(getContextURI())) {
      // create a statement to link this entity to its non-default persistence context
      final Statement contextStatement = getValueFactory().createStatement(
              getInstanceURI(),
              URI_OVERRIDE_CONTEXT,
              getEffectiveContextURI(),
              URI_UNIVERSAL_VOCABULARY_MT);
      if (writer == null) {
        rdfEntityManager.addStatement(repositoryConnection, contextStatement);
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(contextStatement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }
      } else {
        try {
          writer.write(RDFUtility.formatStatementAsTurtle(typeStatement));
          writer.newLine();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
    }
    final Element element = new Element(System.identityHashCode(getRDFEntity()), getInstanceURI());
    identityCache.put(element);

    //Postconditions
    assert getInstanceURI() != null : "instanceURI must not be null";
  }

  /** Iterates over the fields in the RDF entity instance and persists them
   * to the RDF store as propositions.
   *
   * @param repositoryConnection the repository connection
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void persistFields(
          final RepositoryConnection repositoryConnection,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";                                // NOPMD

    for (final Field field : getFieldAnnotationDictionary().keySet()) {
      final Annotation annotation = getFieldAnnotationDictionary().get(field);
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "field: " + field + ", annotation: " + annotation);
      }
      if ("@javax.persistence.Id()".equals(annotation.toString())) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "  skipping Id field");
        }
      } else {
        if (annotation instanceof RDFProperty) {
          persistField(
                  repositoryConnection,
                  field,
                  (RDFProperty) annotation,
                  writer);
        }
      }
    }
  }

  /** Persists the given field according to the given rdf property.
   *
   * @param repositoryConnection the repository connection
   * @param field the given RDF entity instance field
   * @param rdfProperty the property annotation associated with the field
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void persistField(
          final RepositoryConnection repositoryConnection,
          final Field field,
          final RDFProperty rdfProperty,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";                                // NOPMD
    assert field != null : "field must not be null";                                // NOPMD
    assert rdfProperty != null : "rdfProperty must not be null";

    if (isDebugEnabled) {
      logger.debug(stackLevel() + "  processing rdf property: " + rdfProperty);
    }
    // obtain the value object
    if (!field.isAccessible()) {
      field.setAccessible(true);
    }
    Object value;
    try {
      value = field.get(getRDFEntity());
    } catch (final IllegalArgumentException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }
    final Class<?> fieldType = field.getType();
    if (isDebugEnabled) {
      logger.debug(stackLevel() + "  field type: " + fieldType.getName());
    }
    if (value != null) {
      if (value instanceof LazySet || value instanceof LazyList || value instanceof LazyMap) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "    skipping lazy loaded field which has not yet been loaded");
        }
        return;                                                      // NOPMD
      }
      if (Factory.class.isAssignableFrom(value.getClass())) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "    skipping lazy loaded proxy field which has not yet been loaded");
        }
        return;                                                      // NOPMD
      }
    }
    final boolean isBooleanField = "boolean".equals(fieldType.getName());
    // determine the predicateURI
    final URI predicateURI = findPredicate(
            repositoryConnection,
            field,
            rdfProperty,
            isBooleanField,
            writer);
    // obtain the existing values
    Set<Value> existingRDFValues;
    if (isNewDomainInstance || writer != null) { // writer is for exporting RDF rather than peristing it
      existingRDFValues = new ArraySet<>();
    } else if (rdfProperty.inverse()) {
      try {
        final TupleQuery subjectsTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                "SELECT s, c FROM CONTEXT c {s} p {o}");
        subjectsTupleQuery.setBinding("p", predicateURI);
        subjectsTupleQuery.setBinding("o", getInstanceURI());
        subjectsTupleQuery.setBinding("c", getEffectiveContextURI());
        final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
        existingRDFValues = new ArraySet<>();
        while (tupleQueryResult.hasNext()) {
          existingRDFValues.add(tupleQueryResult.next().getBinding("s").getValue());
        }
        tupleQueryResult.close();
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
    } else {
      try {
        final TupleQuery objectsTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                "SELECT o, c FROM CONTEXT c {s} p {o}");
        objectsTupleQuery.setBinding("s", getInstanceURI());
        objectsTupleQuery.setBinding("p", predicateURI);
        objectsTupleQuery.setBinding("c", getEffectiveContextURI());
        final TupleQueryResult tupleQueryResult = objectsTupleQuery.evaluate();
        existingRDFValues = new ArraySet<>();
        while (tupleQueryResult.hasNext()) {
          existingRDFValues.add(tupleQueryResult.next().getBinding("o").getValue());
        }
        tupleQueryResult.close();
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
    }
    Set<Value> newRDFValues = new HashSet<>();
    if (isDebugEnabled) {
      logger.debug(stackLevel() + "  existing RDF values for predicate " + predicateURI + ": " + existingRDFValues);
    }
    // persist by the field type
    if (isBooleanField) {
      String trueClassName = rdfProperty.trueClass();
      if (trueClassName.isEmpty()) {
        // default true class
        trueClassName = getRDFEntity().getClass().getName() + "_" + field.getName() + "_True";
      }
      String falseClassName = rdfProperty.falseClass();
      if (falseClassName.isEmpty()) {
        // default false class
        falseClassName = getRDFEntity().getClass().getName() + "_" + field.getName() + "_False";
      }
      persistBooleanField(
              repositoryConnection,
              (Boolean) value,
              existingRDFValues,
              predicateURI,
              makeURI(trueClassName),
              makeURI(falseClassName),
              field,
              writer);
      return;
    }
    // persist the current value(s)
    if (value == null) {
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  field value is null: " + field);
      }
    } else if (value instanceof Map<?, ?>) {
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  field value is Map: " + field + " value: " + value);
      }
      persistMapFieldValue(
              repositoryConnection,
              (Map<?, ?>) value,
              existingRDFValues,
              rdfProperty,
              predicateURI,
              writer);
      return;
    } else if (value instanceof Collection<?>) {
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  field value is Collection: " + field + " value: " + value);
      }

      if (value instanceof List<?>) {
        BNode existingRDFListHead = null;
        assert existingRDFValues.size() <= 1 : "there must be only one list head " + existingRDFValues;
        if (!existingRDFValues.isEmpty()) {
          assert existingRDFValues.toArray()[0] instanceof BNode : "list head must be a blank node " + existingRDFValues.toArray()[0];
          existingRDFListHead = (BNode) existingRDFValues.toArray()[0];
        }
        if (logger.isDebugEnabled()) {
          logger.debug(stackLevel() + "  persisting field value as RDF collection: " + field + " value: " + value);
        }
        persistFieldValueListAsRDFCollection(
                repositoryConnection,
                (List<?>) value,
                existingRDFListHead,
                rdfProperty,
                predicateURI,
                writer);
        return;
      }
      // persist the set
      newRDFValues = persistFieldValues(
              repositoryConnection,
              (Collection<?>) value,
              existingRDFValues,
              rdfProperty,
              predicateURI,
              writer);
    } else if (field.getType().isArray()) {
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  field value is Array: " + field);
      }
      final int arrayLength = Array.getLength(value);
      final List<Object> valueObjects = new ArrayList<>(arrayLength);
      for (int i = 0; i < arrayLength; i++) {
        valueObjects.add(Array.get(value, i));
      }
      BNode existingRDFListHead = null;
      assert existingRDFValues.size() <= 1 : "there must be only one list head " + existingRDFValues;
      if (!existingRDFValues.isEmpty()) {
        assert existingRDFValues.toArray()[0] instanceof BNode : "list head must be a blank node " + existingRDFValues.toArray()[0];
        existingRDFListHead = (BNode) existingRDFValues.toArray()[0];
      }
      persistFieldValueListAsRDFCollection(
              repositoryConnection,
              valueObjects,
              existingRDFListHead,
              rdfProperty,
              predicateURI,
              writer);
      return;
    } else {
      if (isDebugEnabled) {
        logger.debug(stackLevel() + "  field value is Object: " + field);
      }
      final List<Object> valueList = new ArrayList<>(1);
      valueList.add(value);
      newRDFValues = persistFieldValues(
              repositoryConnection,
              valueList,
              existingRDFValues,
              rdfProperty,
              predicateURI,
              writer);
    }

    removePreviousValues(repositoryConnection, existingRDFValues, newRDFValues, predicateURI);
  }

  /** Removes the previous values for the given predicate.
   *
   * @param repositoryConnection the repository connection
   * @param existingRDFValues the existing RDF values
   * @param newRDFValues the new RDF values
   * @param predicate the predicate that relates the entity instance with each of the RDF values
   */
  private void removePreviousValues(
          final RepositoryConnection repositoryConnection,
          final Set<Value> existingRDFValues,
          final Set<Value> newRDFValues,
          final URI predicate) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert existingRDFValues != null : "existingRDFValues must not be null";
    assert predicate != null : "predicate must not be null";

    // delete previous associations if no longer applicable
    for (final Value existingRDFValue : existingRDFValues) {
      // weaker form of the membership test to determine equal RDF literals
      boolean isObsoleteValue = true;
      final String existingRDFValueString = existingRDFValue.toString();
      for (final Value newRDFValue : newRDFValues) {
        if (newRDFValue.toString().equals(existingRDFValueString)) {
          isObsoleteValue = false;
          break;
        }
      }
      if (isObsoleteValue) {
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "deleting obsolete value: " + existingRDFValue + " for predicate: " + predicate);
        }
        final Statement statement = getValueFactory().createStatement(
                getInstanceURI(),
                predicate,
                existingRDFValue,
                getEffectiveContextURI());
        rdfEntityManager.removeStatement(repositoryConnection, statement);
        if (logger.isInfoEnabled()) {
          logger.info("removed: " + RDFUtility.formatStatement(statement));
        }
      }
    }
  }

  /** Finds the predicate that will be used to persist the current field.
   *
   * @param repositoryConnection the repository connection
   * @param field the given RDF entity instance field
   * @param rdfProperty the property annotation associated with the field
   * @param isBooleanField the indicator that the current field is a boolean type
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   * @return the predicate that will be used to persist the current field
   */
  private URI findPredicate(
          final RepositoryConnection repositoryConnection,
          final Field field,
          final RDFProperty rdfProperty,
          final boolean isBooleanField,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";

    URI predicate;
    if (isBooleanField) {
      predicate = RDF.TYPE;
    } else {
      predicate = getEffectivePropertyURI(field, rdfProperty);
    }
    // check whether to define the predicate
    if (!definedClassAndPredicateURIs.contains(predicate)) {
      final Set<URI> existingTypes = new HashSet<>();
      if (writer == null) {
        // not exporting
        try {
          final TupleQuery existingTypesTupleQuery = repositoryConnection.prepareTupleQuery(
                  QueryLanguage.SERQL,
                  "SELECT typeTerm FROM {s} rdf:type {typeTerm}");
          existingTypesTupleQuery.setBinding("s", predicate);
          TupleQueryResult tupleQueryResult;
          tupleQueryResult = existingTypesTupleQuery.evaluate();
          while (tupleQueryResult.hasNext()) {
            existingTypes.add((URI) tupleQueryResult.next().getBinding("typeTerm").getValue());
          }
          tupleQueryResult.close();
        } catch (final RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
          throw new TexaiException(ex);
        }
      }

      if (existingTypes.isEmpty() && (rdfProperty.subPropertyOf().length > 0 || !rdfProperty.domain().isEmpty() || !rdfProperty.range().isEmpty())) {
        defineNewPredicate(
                repositoryConnection,
                rdfProperty,
                predicate,
                field,
                writer);
      }
      definedClassAndPredicateURIs.add(predicate);
    }

    //Postconditions
    assert predicate != null : "predicate must not be null";

    return predicate;
  }

  /** Persists the given boolean field value.  When the boolean value is true, a statement of the form
   * <instance-term> <rdf:type> <true-class> is asserted in the RDF store, otherwise a statement of the form
   * <instance-term> <rdf:type> <false-class> is asserted.
   *
   * @param repositoryConnection the repository connection
   * @param value the boolean value
   * @param existingValues the existing values
   * @param predicate the predicate that represents the association
   * @param trueClass the class of RDF instances for which the boolean association holds true
   * @param falseClass the class of RDF instances for which the boolean association holds false
   * @param field the given RDF entity instance field
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  @SuppressWarnings("unchecked")
  private void persistBooleanField(
          final RepositoryConnection repositoryConnection,
          final Boolean value,
          final Set<Value> existingValues,
          final URI predicate,
          final URI trueClass,
          final URI falseClass,
          final Field field,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert value != null : "value must not be null";
    assert existingValues != null : "existingValues must not be null";
    assert predicate != null : "predicate must not be null";
    assert trueClass != null : "trueClassName must not be null";
    assert falseClass != null : "falseClassName must not be null";

    try {
      boolean haveCreatedTrueOrFalseClassTerm = false;
      boolean isExport = writer != null;
      // check whether to define the true and false classes
      if (!definedClassAndPredicateURIs.contains(getClassURI())) {
        // query for existing definition of the true class
        final Set<URI> existingTypes = new HashSet<>();
        TupleQuery existingTypesTupleQuery;
        TupleQueryResult tupleQueryResult;
        existingTypesTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                "SELECT typeTerm FROM {s} rdf:type {typeTerm}");
        existingTypesTupleQuery.setBinding("s", trueClass);
        tupleQueryResult = existingTypesTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          existingTypes.add((URI) tupleQueryResult.next().getBinding("typeTerm").getValue());
        }
        tupleQueryResult.close();
        if (existingTypes.isEmpty()) {
          // define true class
          final List<URI> types = new ArrayList<>();
          types.add(URI_FIRST_ORDER_COLLECTION);
          final List<URI> subClassOfURIs = new ArrayList<>();
          subClassOfURIs.add(getClassURI());
          rdfUtility.defineRDFClass(
                  repositoryConnection.getRepository().getDataDir().getName(),
                  trueClass,
                  "This is the collection of " + getClassURI() + " instances for which boolean property " + field.getName() + " holds true.",
                  types,
                  subClassOfURIs,
                  writer);
          haveCreatedTrueOrFalseClassTerm = true;
        }
        // query for existing definition of the false class
        existingTypes.clear();
        existingTypesTupleQuery.setBinding("s", falseClass);
        tupleQueryResult = existingTypesTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          existingTypes.add((URI) tupleQueryResult.next().getBinding("typeTerm").getValue());
        }
        tupleQueryResult.close();
        if (existingTypes.isEmpty()) {
          // define false class
          final List<URI> types = new ArrayList<>();
          types.add(URI_FIRST_ORDER_COLLECTION);
          final List<URI> subClassOfURIs = new ArrayList<>();
          subClassOfURIs.add(getClassURI());
          rdfUtility.defineRDFClass(
                  repositoryConnection.getRepository().getDataDir().getName(),
                  falseClass,
                  "This is the collection of " + getClassURI() + " instances for which boolean property " + field.getName() + " holds false.",
                  types,
                  subClassOfURIs,
                  writer);
          haveCreatedTrueOrFalseClassTerm = true;
        }
      }
      if (haveCreatedTrueOrFalseClassTerm) {
        rdfUtility.assertDisjoint(
                repositoryConnection.getRepository().getDataDir().getName(),
                trueClass,
                falseClass,
                writer);
      }

      // assert the truth value relationship
      if (value) {
        if (!existingValues.contains(trueClass)) {
          final Statement statement = getValueFactory().createStatement(
                  getInstanceURI(),
                  RDF.TYPE,
                  trueClass,
                  getEffectiveContextURI());
          if (writer == null) {
            rdfEntityManager.addStatement(repositoryConnection, statement);
          } else {
            try {
              writer.write(RDFUtility.formatStatementAsTurtle(statement));
              writer.newLine();
            } catch (IOException ex) {
              throw new TexaiException(ex);
            }
          }
          if (logger.isInfoEnabled()) {
            logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
          }
        }
        if (existingValues.contains(falseClass)) {
          if (isDebugEnabled) {
            logger.debug(stackLevel() + "deleting existing value URI: " + falseClass + " for predicate: " + predicate);
          }
          final Statement statement = getValueFactory().createStatement(
                  getInstanceURI(),
                  RDF.TYPE,
                  falseClass,
                  getEffectiveContextURI());
          rdfEntityManager.removeStatement(repositoryConnection, statement);
          if (logger.isInfoEnabled()) {
            logger.info("removed: " + RDFUtility.formatStatement(statement));
          }
        }
      } else {
        if (!existingValues.contains(falseClass)) {
          final Statement statement = getValueFactory().createStatement(
                  getInstanceURI(),
                  RDF.TYPE,
                  falseClass,
                  getEffectiveContextURI());
          if (writer == null) {
            rdfEntityManager.addStatement(repositoryConnection, statement);
          } else {
            try {
              writer.write(RDFUtility.formatStatementAsTurtle(statement));
              writer.newLine();
            } catch (IOException ex) {
              throw new TexaiException(ex);
            }
          }
          if (logger.isInfoEnabled()) {
            logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
          }
        }
        if (existingValues.contains(trueClass)) {
          if (isDebugEnabled) {
            logger.debug(stackLevel() + "deleting existing value URI: " + trueClass + " for predicate: " + predicate);
          }
          final Statement statement = getValueFactory().createStatement(
                  getInstanceURI(),
                  RDF.TYPE,
                  trueClass,
                  getEffectiveContextURI());
          assert writer == null;
          rdfEntityManager.removeStatement(repositoryConnection, statement);
          if (logger.isInfoEnabled()) {
            logger.info("removed: " + RDFUtility.formatStatement(statement));
          }
        }
      }
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Persists the map field values as a set of blank nodes, each of which has two statements -
   * one for the key and the other for the value
   *
   * @param repositoryConnection the repository connection
   * @param value the java Map object
   * @param existingRDFValues the  existing RDF values
   * @param rdfProperty the RDF property annotation
   * @param predicateURI the predicate that represents the association
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  @SuppressWarnings("unchecked")
  private void persistMapFieldValue(
          final RepositoryConnection repositoryConnection,
          final Map<?, ?> value,
          final Set<Value> existingRDFValues,
          final RDFProperty rdfProperty,
          final URI predicateURI,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert value != null : "value must not be null";
    assert existingRDFValues != null : "existingValues must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateURI != null : "predicateURI must not be null";
    assert getEffectiveContextURI() != null : "effectiveContextURI must not be null";

    if (existingRDFValues.isEmpty() && value.isEmpty()) {
      // no new map entries to persist and no existing map entries to remove
      return;
    }

    // gather the existing map entries
    final Set<MapEntry> existingMapEntries = new HashSet<>();
    for (final Value existingRDFValue : existingRDFValues) {
      assert existingRDFValue instanceof BNode;
      existingMapEntries.add(
              MapEntry.makeMapEntry((BNode) existingRDFValue,
              repositoryConnection,
              getEffectiveContextURI()));
    }

    // add the new entries
    final Set<MapEntry> mapEntriesToRemain = new HashSet<>();
    for (final Entry<?, ?> entry : value.entrySet()) {
      final MapEntry mapEntry = new MapEntry(entry.getKey(), entry.getValue());
      mapEntriesToRemain.add(mapEntry);
      if (!existingMapEntries.contains(mapEntry)) {

        // persist the blank node
        final BNode bNode = getValueFactory().createBNode();
        Statement statement = getValueFactory().createStatement(
                getInstanceURI(),
                predicateURI,
                bNode,
                getEffectiveContextURI());
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }

        // persist the map entry key
        final Value keyRDFValue = getRDFValueFromJavaValue(repositoryConnection, entry.getKey(), rdfProperty);
        statement = getValueFactory().createStatement(
                bNode,
                PERSISTENT_MAP_ENTRY_KEY_URI, // predicateURI
                keyRDFValue,
                getEffectiveContextURI());
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }

        // persist the map entry value
        final Value valueRDFValue = getRDFValueFromJavaValue(repositoryConnection, entry.getValue(), rdfProperty);
        statement = getValueFactory().createStatement(
                bNode,
                PERSISTENT_MAP_ENTRY_VALUE_URI, // predicateURI
                valueRDFValue,
                getEffectiveContextURI());
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }
      }
    }

    assert writer == null || existingMapEntries.isEmpty() : "should be no existing map entries when exporting";
    // remove existing map enties that are not included in entries to remain.
    for (final MapEntry existingMapEntry : existingMapEntries) {
      if (!mapEntriesToRemain.contains(existingMapEntry)) {
        // remove the link from the persistent object to the blank node that relates the map entry
        final BNode bNode = existingMapEntry.bNode;
        Statement statement = getValueFactory().createStatement(
                getInstanceURI(),
                predicateURI,
                bNode,
                getEffectiveContextURI());
        rdfEntityManager.removeStatement(repositoryConnection, statement);
        getLogger().info("removed: " + RDFUtility.formatStatement(statement));

        // remove the link from the the blank node to the map entry key
        statement = getValueFactory().createStatement(
                bNode,
                PERSISTENT_MAP_ENTRY_KEY_URI, // predicateURI
                existingMapEntry.keyRDFValue,
                getEffectiveContextURI());
        rdfEntityManager.removeStatement(repositoryConnection, statement);
        getLogger().info("removed: " + RDFUtility.formatStatement(statement));

        // remove the link from the the blank node to the map entry value
        statement = getValueFactory().createStatement(
                bNode,
                PERSISTENT_MAP_ENTRY_VALUE_URI, // predicateURI
                existingMapEntry.valueRDFValue,
                getEffectiveContextURI());
        rdfEntityManager.removeStatement(repositoryConnection, statement);
        getLogger().info("removed: " + RDFUtility.formatStatement(statement));
      }
    }
  }

  /** Persists the field value(s).
   *
   * @param repositoryConnection the repository connection
   * @param valueList the list of java values
   * @param existingRDFValues the  existing RDF values
   * @param rdfProperty the RDF property annotation
   * @param predicateURI the predicate that represents the association
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   * @return the set of asserted RDF values
   */
  @SuppressWarnings("unchecked")
  private Set<Value> persistFieldValues(
          final RepositoryConnection repositoryConnection,
          final Collection<?> valueList,
          final Set<Value> existingRDFValues,
          final RDFProperty rdfProperty,
          final URI predicateURI,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert valueList != null : "valueList must not be null";
    assert existingRDFValues != null : "existingValues must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateURI != null : "predicateURI must not be null";
    assert getEffectiveContextURI() != null : "effectiveContextURI must not be null";

    final Set<Value> assertedRDFValues = new HashSet<>();
    final List<Value> rdfValues = createRDFValueTerms(repositoryConnection, valueList, rdfProperty);
    final Iterator<Value> rdfValues_iter = rdfValues.iterator();
    while (rdfValues_iter.hasNext()) {
      final Value rdfValue = rdfValues_iter.next();
      // weaker form of the membership test to determine equal RDF literals
      boolean isNewValue = true;
      final String rdfValueString = rdfValue.toString();
      for (final Value existingRDFValue : existingRDFValues) {
        if (existingRDFValue.toString().equals(rdfValueString)) {
          isNewValue = false;
          break;
        }
      }
      if (isNewValue) {
        Statement statement;
        if (rdfProperty.inverse()) {
          if (!(rdfValue instanceof URI)) {
            if (rdfValue == null) {
              throw new TexaiException(rdfValue + " must be a URI for inverse property, predicate: " + predicateURI
                      + ", rdfValue: " + rdfValue);
            } else {
              throw new TexaiException(rdfValue + " must be a URI for inverse property, predicate: " + predicateURI
                      + ", rdfValue: " + rdfValue + "(" + rdfValue.getClass().getName() + ")");
            }
          }
          statement = getValueFactory().createStatement(
                  (URI) rdfValue,
                  predicateURI,
                  getInstanceURI(),
                  getEffectiveContextURI());
        } else {
          statement = getValueFactory().createStatement(
                  getInstanceURI(),
                  predicateURI,
                  rdfValue,
                  getEffectiveContextURI());
        }
        if (writer == null) {
          rdfEntityManager.addStatement(repositoryConnection, statement);
        } else {
          try {
            writer.write(RDFUtility.formatStatementAsTurtle(statement));
            writer.newLine();
          } catch (IOException ex) {
            throw new TexaiException(ex);
          }
        }
        if (logger.isInfoEnabled()) {
          logger.info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
        }
      }
      assertedRDFValues.add(rdfValue);
    }
    return assertedRDFValues;
  }

  /** Persists the field values as an RDF collection.
   *
   * @param repositoryConnection the repository connection
   * @param valueList the list of java values
   * @param existingRDFListHead the existing RDF value which if present is a blank node that is the head of the RDF value list
   * @param rdfProperty the RDF property annotation
   * @param predicateURI the predicate that represents the association
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void persistFieldValueListAsRDFCollection(
          final RepositoryConnection repositoryConnection,
          final List<?> valueList,
          final BNode existingRDFListHead,
          final RDFProperty rdfProperty,
          final URI predicateURI,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert valueList != null : "valueList must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateURI != null : "predicateURI must not be null";
    assert getEffectiveContextURI() != null : "getEffectiveContextURI must not be null";

    if (existingRDFListHead == null && valueList.isEmpty()) {
      // no new values to persist and no existing values to remove
      return;
    }
    final List<Value> rdfValues = createRDFValueTerms(repositoryConnection, valueList, rdfProperty);
    if (existingRDFListHead != null) {
      final List<Value> existingRDFValues = getRDFListValues(repositoryConnection, existingRDFListHead);
      if (existingRDFValues.equals(rdfValues)) {
        // no change in the list
        return;
      }
    }
    // add new list
    final BNode newListRDFHead = addRDFList(
            repositoryConnection,
            rdfEntityManager,
            rdfValues,
            writer);
    Statement statement = getValueFactory().createStatement(
            getInstanceURI(),
            predicateURI,
            newListRDFHead,
            getEffectiveContextURI());
    if (writer == null) {
      rdfEntityManager.addStatement(repositoryConnection, statement);
    } else {
      try {
        writer.write(RDFUtility.formatStatementAsTurtle(statement));
        writer.newLine();
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    if (existingRDFListHead != null) {
      // remove existing RDF list
      removeRDFList(repositoryConnection, rdfEntityManager, existingRDFListHead);
      statement = getValueFactory().createStatement(
              getInstanceURI(),
              predicateURI,
              existingRDFListHead,
              getEffectiveContextURI());
      assert writer == null;
      rdfEntityManager.removeStatement(repositoryConnection, statement);
      getLogger().info("removed: " + RDFUtility.formatStatement(statement));
    }
  }

  /** Creates a new predicate for the given rdf property.
   *
   * @param repositoryConnection the repository connection
   * @param rdfProperty the given rdf property
   * @param predicate the predicate
   * @param field the given field
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   */
  private void defineNewPredicate(
          final RepositoryConnection repositoryConnection,
          final RDFProperty rdfProperty,
          final URI predicate,
          final Field field,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicate != null : "predicate must not be null";
    assert field != null : "field must not be null";

    URI domainURI;
    final String domainName = rdfProperty.domain();
    if (domainName.length() == 0) {
      domainURI = getClassURI();
    } else {
      domainURI = makeURI(domainName);
    }
    URI rangeURI;
    final String rangeName = rdfProperty.range();
    if (rangeName.length() == 0) {
      rangeURI = getDefaultTypeURIForFieldType(field);
    } else {
      rangeURI = makeURI(rangeName);
    }
    final List<URI> typeURIs = new ArrayList<>(1);
    if (XMLDatatypeUtil.isBuiltInDatatype(rangeURI)) {
      typeURIs.add(OWL.DATATYPEPROPERTY);
    } else {
      typeURIs.add(OWL.OBJECTPROPERTY);
    }
    final List<URI> subPropertyOfURIs = new ArrayList<>(1);
    subPropertyOfURIs.add(URI_CONCEPTUALLY_RELATED);
    rdfUtility.defineRDFPredicate(
            repositoryConnection.getRepository().getDataDir().getName(),
            predicate,
            (String) null,
            typeURIs,
            subPropertyOfURIs,
            domainURI,
            rangeURI,
            writer);
  }

  /** For the purpose of defining a new predicate, returns the RDF type that corresponds to the type of the given field.
   *
   * @param field the given field
   * @return the URI type that corresponds to the type of the given field
   */
  private URI getDefaultTypeURIForFieldType(final Field field) {
    //Preconditions
    assert field != null : "field must not be null";

    return getTypeURIForClass(field.getType());
  }

  /** For the purpose of defining a new predicate, returns the RDF type that corresponds to the given type of a Java field.
   *
   * @param fieldClass the given type of a Java field
   * @return the URI type that corresponds to the type of the given field
   */
  private URI getTypeURIForClass(final Class<?> fieldClass) {
    //Preconditions
    assert fieldClass != null : "clazz must not be null";

    URI fieldClassURI;
    if (fieldClass.equals(Byte.class) || fieldClass.equals(byte.class)) {
      fieldClassURI = XMLSchema.BYTE;
    } else if (fieldClass.equals(Short.class) || fieldClass.equals(short.class)) {
      fieldClassURI = XMLSchema.SHORT;
    } else if (fieldClass.equals(Integer.class) || fieldClass.equals(int.class)) {
      fieldClassURI = XMLSchema.INT;
    } else if (fieldClass.equals(Long.class) || fieldClass.equals(long.class)) {
      fieldClassURI = XMLSchema.LONG;
    } else if (fieldClass.equals(Float.class) || fieldClass.equals(float.class)) {
      fieldClassURI = XMLSchema.FLOAT;
    } else if (fieldClass.equals(Double.class) || fieldClass.equals(double.class)) {
      fieldClassURI = XMLSchema.DOUBLE;
    } else if (fieldClass.equals(String.class)) {
      fieldClassURI = XMLSchema.STRING;
    } else if (fieldClass.equals(BigInteger.class)) {
      fieldClassURI = XMLSchema.INTEGER;
    } else if (fieldClass.equals(BigDecimal.class)) {
      fieldClassURI = XMLSchema.DECIMAL;
    } else if (fieldClass.equals(Calendar.class)) {
      fieldClassURI = XMLSchema.DATETIME;
    } else if (fieldClass.equals(Date.class)) {
      fieldClassURI = XMLSchema.DATETIME;
    } else if (fieldClass.equals(QName.class)) {
      fieldClassURI = XMLSchema.QNAME;
    } else if (fieldClass.equals(java.net.URI.class)) {
      fieldClassURI = XMLSchema.ANYURI;
    } else if (fieldClass.equals(byte[].class)) {
      fieldClassURI = XMLSchema.BASE64BINARY;
    } else if (fieldClass.isArray()) {
      fieldClassURI = getTypeURIForClass(fieldClass.getComponentType());
    } else {
      fieldClassURI = getClassURI(fieldClass);
    }

    //Postconditions
    assert fieldClassURI != null : "classURI must not be null";

    return fieldClassURI;
  }

  /** Creates the RDF values for the given value list.
   *
   * @param repositoryConnection the repository connection
   * @param valueList the given java value list
   * @param rdfProperty the RDF property annotation
   * @return the RDF values for the given value list
   */
  private List<Value> createRDFValueTerms(
          final RepositoryConnection repositoryConnection,
          final Collection<?> valueList,
          final RDFProperty rdfProperty) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert valueList != null : "valueList must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";

    if (isDebugEnabled) {
      logger.debug(stackLevel() + "  valueList: " + valueList);
    }
    final List<Value> rdfValues = new ArrayList<>(valueList.size());
    for (final Object value : valueList) {
      rdfValues.add(getRDFValueFromJavaValue(repositoryConnection, value, rdfProperty));
    }
    return rdfValues;
  }

  /** Gets the RDF value that will be persisted for the given java value.
   *
   * @param repositoryConnection the repository connection
   * @param value the given java value
   * @param rdfProperty the RDF property annotation
   * @return the RDF value term for the given value
   */
  private Value getRDFValueFromJavaValue(
          final RepositoryConnection repositoryConnection,
          final Object value,
          final RDFProperty rdfProperty) {    // NOPMD
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert value != null : "value must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";

    Value rdfValue = null;
    if (value instanceof Byte) {
      if (rdfProperty.range().endsWith("unsignedByte")) {
        if (((Byte) value) < 0) {
          throw new TexaiException("attempt to persist a negative byte value [" + value + "] for an unsignedByte field");
        }
        rdfValue = getValueFactory().createLiteral(value.toString(), XMLSchema.UNSIGNED_BYTE);
      } else {
        rdfValue = getValueFactory().createLiteral((Byte) value);
      }
    } else if (value instanceof Short) {
      if (rdfProperty.range().endsWith("unsignedShort")) {
        if (((Short) value) < 0) {
          throw new TexaiException("attempt to persist a negative short value [" + value + "] for an unsignedShort field");
        }
        rdfValue = getValueFactory().createLiteral(value.toString(), XMLSchema.UNSIGNED_SHORT);
      } else {
        rdfValue = getValueFactory().createLiteral((Short) value);
      }
    } else if (value instanceof Integer) {
      if (rdfProperty.range().isEmpty()) {
        rdfValue = getValueFactory().createLiteral((Integer) value);
      } else if (rdfProperty.range().endsWith("unsignedInt")) {
        if (((Integer) value) < 0) {
          throw new TexaiException("attempt to persist a negative integer value [" + value + "] for an unsignedInt field");
        }
        rdfValue = getValueFactory().createLiteral(value.toString(), XMLSchema.UNSIGNED_INT);
      } else {
        rdfValue = getValueFactory().createLiteral((Integer) value);
      }
    } else if (value instanceof Long) {
      if (rdfProperty.range().endsWith("unsignedLong")) {
        if (((Long) value) < 0) {
          throw new TexaiException("attempt to persist a negative long value [" + value + "] for an unsignedLong field");
        }
        rdfValue = getValueFactory().createLiteral(value.toString(), XMLSchema.UNSIGNED_LONG);
      } else {
        rdfValue = getValueFactory().createLiteral((Long) value);
      }
    } else if (value instanceof Float) {
      rdfValue = getValueFactory().createLiteral((Float) value);
    } else if (value instanceof Double) {
      rdfValue = getValueFactory().createLiteral((Double) value);
    } else if (value instanceof String) {
      rdfValue = getValueFactory().createLiteral((String) value);
    } else if (value instanceof BigInteger) {
      if (rdfProperty.range().endsWith("positiveInteger")) {
        if (((BigInteger) value).longValue() <= 0) {
          throw new TexaiException("attempt to persist a non-positive integer value [" + value + "] for an positiveInteger field");
        }
        rdfValue = getValueFactory().createLiteral(DatatypeConverter.printInteger((BigInteger) value), XMLSchema.POSITIVE_INTEGER);
      } else if (rdfProperty.range().endsWith("nonNegativeInteger")) {
        if (((BigInteger) value).longValue() < 0) {
          throw new TexaiException("attempt to persist a negative integer value [" + value + "] for an nonNegativeInteger field");
        }
        rdfValue = getValueFactory().createLiteral(DatatypeConverter.printInteger((BigInteger) value), XMLSchema.NON_NEGATIVE_INTEGER);
      } else if (rdfProperty.range().endsWith("nonPositiveInteger")) {
        if (((BigInteger) value).longValue() > 0) {
          throw new TexaiException("attempt to persist a positive integer value [" + value + "] for an nonPositiveInteger field");
        }
        rdfValue = getValueFactory().createLiteral(DatatypeConverter.printInteger((BigInteger) value), XMLSchema.NON_POSITIVE_INTEGER);
      } else if (rdfProperty.range().endsWith("negativeInteger")) {
        if (((BigInteger) value).longValue() >= 0) {
          throw new TexaiException("attempt to persist a non-negative integer value [" + value + "] for an negativeInteger field");
        }
        rdfValue = getValueFactory().createLiteral(DatatypeConverter.printInteger((BigInteger) value), XMLSchema.NEGATIVE_INTEGER);
      } else {
        rdfValue = getValueFactory().createLiteral(DatatypeConverter.printInteger((BigInteger) value), XMLSchema.INTEGER);
      }
    } else if (value instanceof BigDecimal) {
      rdfValue = getValueFactory().createLiteral(DatatypeConverter.printDecimal((BigDecimal) value), XMLSchema.DECIMAL);
    } else if (value instanceof DateTime) {
      final GregorianCalendar calendar = ((DateTime) value).toGregorianCalendar();
      rdfValue = getValueFactory().createLiteral(DatatypeConverter.printDateTime(calendar), XMLSchema.DATETIME);
    } else if (value instanceof Calendar) {
      rdfValue = getValueFactory().createLiteral(DatatypeConverter.printDateTime((Calendar) value), XMLSchema.DATETIME);
    } else if (value instanceof Date) {
      final Calendar calendar = Calendar.getInstance();
      calendar.setTime((Date) value);
      rdfValue = getValueFactory().createLiteral(DatatypeConverter.printDateTime(calendar), XMLSchema.DATETIME);
    } else if (value instanceof UUID) {
      rdfValue = getValueFactory().createLiteral(((UUID) value).toString());
    } else if (value instanceof QName) {
      throw new TexaiException("QName type is not implemented for persistence");
    } else if (value instanceof java.net.URI) {
      rdfValue = getValueFactory().createLiteral(((java.net.URI) value).toString(), XMLSchema.ANYURI);
    } else if (value instanceof Value) {
      rdfValue = (Value) value;
    } else if (value instanceof Byte[]) {
      throw new TexaiException("Byte[] type is not implemented for persistence");
    } else if (value instanceof Factory) {
      throw new TexaiException("Attempted to persist " + value + " which is a lazy-loaded object in its unloaded state. "
              + "Call some method on this object to force loading.");
    } else if (isRDFEntity(value)) {
      final Element element = identityCache.get(System.identityHashCode(value));
      if (element != null) {
        rdfValue = (Value) element.getObjectValue();
      }
      if (rdfValue == null && value instanceof RDFPersistent) {
        rdfValue = ((RDFPersistent) value).getId();
      }
      if (rdfValue == null) {
        // save this session state before the recursive method call
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "  saving session state for rdfEntity: " + getRDFEntity());
        }
        saveAbstractSessionState();
        setOverrideContextURI(null);  // override context affects only the outmost RDF entity
        saveSessionState();
        if (isDebugEnabled) {
          getLogger().debug(stackLevel() + "recursing to persist " + value);
        }
        rdfValue = persist(repositoryConnection, (RDFPersistent) value);
        restoreAbstractSessionState();
        restoreSessionState();
        if (isDebugEnabled) {
          logger.debug(stackLevel() + "  restored session state for rdfEntity: " + getRDFEntity());
          logger.debug(stackLevel() + "  previously persisted rdfValue: " + rdfValue);
        }
      }
    } else {
      throw new TexaiException("unhandled value type (" + value.getClass().getName() + ") " + value
              + "\nPerhaps the value is a type missing an @RDFEntity annnotation.");
    }

    //Postconditions
    assert rdfValue != null : "rdfValue must not be null";

    return rdfValue;
  }

  /** Pushes the current session state onto a stack and then intitializes the session state. */
  public void saveSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    if (isDebugEnabled) {
      logger.debug(stackLevel() + "saving session state");
    }
    rdfEntityInfoStack.push(new RDFEntityInfo(recursiveContextURI, isNewDomainInstance));
  }

  /** Initializes the session state during a recursive method call. */
  private void initializeSessionState() {
    recursiveContextURI = null;                            // NOPMD
    isNewDomainInstance = true;                            // NOPMD
  }

  /** Restores the session state following a recursive method call. */
  public void restoreSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    final RDFEntityInfo rdfEntityInfo = rdfEntityInfoStack.pop();
    recursiveContextURI = rdfEntityInfo.contextURI;
    isNewDomainInstance = rdfEntityInfo.isNewDomainInstance;
    if (isDebugEnabled) {
      logger.debug(stackLevel() + "restored session state");
    }
  }

  /** Contains the session state for recursive method calls. */
  private static class RDFEntityInfo {

    /** the context into which the class-scoped associations are persisted */
    private final URI contextURI;                          // NOPMD
    /** the indicator that the domain instance is new */
    private boolean isNewDomainInstance;                   // NOPMD

    /**
     * Creates a new RDFEntityInfo instance.
     *
     * @param contextURI the context into which the class-scoped associations are persisted
     * @param isNewDomainInstance  the indicator that the domain instance is new
     */
    protected RDFEntityInfo(
            final URI contextURI,
            final boolean isNewDomainInstance) {
      this.contextURI = contextURI;
      this.isNewDomainInstance = isNewDomainInstance;
    }
  }
}
