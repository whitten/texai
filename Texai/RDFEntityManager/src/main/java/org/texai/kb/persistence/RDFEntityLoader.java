/*
 * RDFEntityLoader.java
 *
 * Created on October 31, 2006, 11:22 AM
 *
 * Description: This class loads RDF entities from the RDF store,
 * mapping RDF triples onto RDF entity associations.
 *
 * Copyright (C) 2006 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.FetchType;
import javax.xml.bind.DatatypeConverter;
import net.jcip.annotations.NotThreadSafe;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import org.apache.log4j.Logger;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.openrdf.OpenRDFException;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.Constants;
import org.texai.kb.persistence.lazy.LazyList;
import org.texai.kb.persistence.lazy.LazyMap;
import org.texai.kb.persistence.lazy.LazySet;
import org.texai.kb.persistence.lazy.RDFEntityLazyLoader;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** This helper class loads previously persisted entities from the Sesame RDF store.  Collection and entity fields are
 * by default lazily loaded.
 *
 * @author reed
 */
@NotThreadSafe
public final class RDFEntityLoader extends AbstractRDFEntityAccessor {   // NOPMD

  /** the cached dictionary of default contexts, class name --> context URI */
  private static final Map<String, URI> DEFAULT_CONTEXT_DICTIONARY = new ConcurrentHashMap<>();
  /** the URI http://texai.org/texai/overrideContext */
  private static final URI URI_OVERRIDE_CONTEXT = new URIImpl(Constants.TERM_OVERRIDE_CONTEXT);
  /** the logger */
  private final Logger logger = Logger.getLogger(RDFEntityLoader.class);               // NOPMD
  /** the indicator whether the debug logging level is enabled */
  private final boolean isDebugEnabled;
  /** the dictionary of connected RDF entities, URI --> RDF instance */
  private Map<URI, Object> connectedRDFEntityDictionary = new HashMap<>();
  /** the dictionary of class names, class name --> class */
  private final Map<String, Class<?>> classNameDictionary = new HashMap<>();
  /** the stack of RDF entity information that enables recursive method calls */
  private final Stack<RDFEntityInfo> rdfEntityInfoStack = new Stack<>();
  /** the predicate values dictionary, predicate --> RDF values */
  private Map<URI, List<Value>> predicateValuesDictionary;
  /** the proxy factory dictionary, field type --> proxy factory */
  private final Map<Class<?>, Factory> proxyFactoryDictionary = new HashMap<>();

  /** Creates a new instance of RDFEntityLoader. */
  public RDFEntityLoader() {
    super();
    isDebugEnabled = logger.isDebugEnabled();
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI.
   *
   * @param repositoryConnection the repository connection
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity or null if not found
   */
  public Object find(
          final RepositoryConnection repositoryConnection,
          final URI instanceURI) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";     // NOPMD
    assert instanceURI != null : "instanceURI must not be null";     // NOPMD

    return findRDFEntity(
            repositoryConnection,
            null,
            instanceURI);
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI and class.  If the class is an interface
   * then the class name is looked up from the RDF store before instantiating the entity.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param clazz the RDF entity class
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity
   */
  @SuppressWarnings("unchecked")
  public <T> T find(
          final RepositoryConnection repositoryConnection,
          final Class<T> clazz,
          final URI instanceURI) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";     // NOPMD
    assert clazz != null : "clazz must not be null";
    assert instanceURI != null : "instanceURI must not be null";     // NOPMD

    connectedRDFEntityDictionary.clear();
    if (clazz.isInterface()) {
      return (T) find(repositoryConnection, instanceURI);
    } else {
      return findRDFEntity(
              repositoryConnection,
              clazz,
              instanceURI);
    }
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its URI, without clearing
   * the dictionary of connected RDF entities.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param clazz the RDF entity class
   * @param instanceURI the java.net.URI that represents the RDF entity
   * @return the RDF entity or null if not found
   */
  public <T> T find(
          final RepositoryConnection repositoryConnection,
          final Class<T> clazz,
          final java.net.URI instanceURI) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";     // NOPMD
    assert clazz != null : "clazz must not be null";
    assert instanceURI != null : "instanceURI must not be null";

    return find(
            repositoryConnection,
            clazz,
            new URIImpl(instanceURI.toString()));
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its id string, without clearing
   * the dictionary of connected RDF entities.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param clazz the RDF entity class
   * @param idString the id string that represents the RDF entity
   * @return the RDF entity or null if not found
   */
  public <T> T find(
          final RepositoryConnection repositoryConnection,
          final Class<T> clazz,
          final String idString) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";     // NOPMD
    assert clazz != null : "clazz must not be null";
    assert idString != null : "idString must not be null";
    assert !idString.isEmpty() : "idString must not be empty";

    return find(
            repositoryConnection,
            clazz,
            new URIImpl(idString));
  }

  /** Finds and loads RDF entities having the given RDF predicate, RDF value and class.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param predicate the given RDF predicate
   * @param rdfValue the RDF value of the predicate
   * @param rdfEntityClass the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final RepositoryConnection repositoryConnection,
          final URI predicate,
          final Value rdfValue,
          final Class<T> rdfEntityClass) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "rdfValue must not be null";
    assert rdfEntityClass != null : "rdfEntityClass must not be null";    // NOPMD

    return find(
            repositoryConnection,
            predicate,
            rdfValue,
            null, // override context
            rdfEntityClass);
  }

  /** Finds and loads RDF entities having the given RDF predicate, RDF value and class.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param predicate the given RDF predicate
   * @param rdfValue the RDF value of the predicate
   * @param overrideContextURI the override context, or null if the default context is to be used
   * @param rdfEntityClass the class of the desired RDF entities
   * @return the RDF entities having the given RDF predicate and RDF value
   */
  public <T> List<T> find(
          final RepositoryConnection repositoryConnection,
          final URI predicate,
          final Value rdfValue,
          final URI overrideContextURI,
          final Class<T> rdfEntityClass) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "rdfValue must not be null";
    assert rdfEntityClass != null : "rdfEntityClass must not be null";    // NOPMD

    final List<T> rdfEntities = new ArrayList<>();
    connectedRDFEntityDictionary.clear();
    initializeAbstractSessionState();
    setRDFEntityClass(rdfEntityClass);
    gatherAnnotationsForRDFEntityClass();
    setOverrideContextURI(overrideContextURI);
    configureRDFEntitySettings();
    final List<URI> instanceURIs = findInstanceURIsByPredicateAndValue(
            repositoryConnection,
            predicate,
            rdfValue,
            rdfEntityClass);
    for (final URI instanceURI : instanceURIs) {
      rdfEntities.add(findRDFEntity(
              repositoryConnection,
              rdfEntityClass,
              instanceURI));
    }
    return rdfEntities;
  }

  /** Gets the default context of the given persistent class.
   *
   * @param rdfEntityClass the persistent class
   * @return the default context
   */
  public synchronized URI getDefaultContext(final Class<?> rdfEntityClass) {
    //Preconditions
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    // static dictionary is guarded by the synchronization of this method
    URI contextURI1 = DEFAULT_CONTEXT_DICTIONARY.get(rdfEntityClass.getName());
    if (contextURI1 == null) {
      connectedRDFEntityDictionary.clear();
      initializeAbstractSessionState();
      setRDFEntityClass(rdfEntityClass);
      gatherAnnotationsForRDFEntityClass();
      setOverrideContextURI(null);
      configureRDFEntitySettings();
      contextURI1 = getContextURI();
      DEFAULT_CONTEXT_DICTIONARY.put(rdfEntityClass.getName(), contextURI1);
    }
    return contextURI1;
  }

  /** Loads the given lazy RDF entity field.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity
   * @param field the field to be loaded
   * @param rdfProperty the RDF property the associates field value(s) in the knowledge base
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   * @return the field value
   */
  public Object loadLazyRDFEntityField(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "domainInstance must not be null";
    assert field != null : "field must not be null";                 // NOPMD
    assert rdfProperty != null : "rdfProperty must not be null";     // NOPMD

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "lazy loading RDF entity entity " + rdfEntity.getClass().getName() + "\n  field: " + field + "\n  rdfProperty: " + rdfProperty);
    }
    initializeAbstractSessionState();
    setRDFEntityClass(rdfEntity.getClass());
    setRDFEntity(rdfEntity);
    gatherAnnotationsForRDFEntityClass();
    configureRDFEntitySettings();
    setInstanceURIFromIdField();
    final Object value;
    try {
      value = loadField(
              repositoryConnection,
              field,
              rdfProperty,
              predicateValuesDictionary);
    } catch (final TexaiException ex) {
      getLogger().warn(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex.getMessage()
              + "\npredicateValuesDictionary: " + predicateValuesDictionary, ex);
    }
    return value;
  }

  /** Returns an iterator over the set of instances of the given RDF entity class.
   *
   * @param <T> the entity type
   * @param repositoryConnection the repository connection
   * @param rdfEntityClass the given RDF entity class
   * @param overrideContextURI the override context
   * @return an iterator over the set of instances of the given RDF entity class
   */
  public <T> Iterator<T> rdfEntityIterator(
          final RepositoryConnection repositoryConnection,
          final Class<T> rdfEntityClass,
          final URI overrideContextURI) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    return new RDFEntityIterator<>(repositoryConnection, rdfEntityClass, overrideContextURI);
  }

  /** Provides an iterator over the instances of the specified RDF entity class.
   *
   * @param <E> the specified RDF entity class
   */
  public final class RDFEntityIterator<E> implements Iterator<E> {

    /** the repository connection */
    private final RepositoryConnection repositoryConnection;
    /** the RDF entity class */
    private final Class<?> rdfEntityClass;
    /** the iterator over the set of instances of the given RDF entity class */
    private final Iterator<URI> rdfEntityURISet_iter;
    /** the effective context */
    private final URI effectiveContextURI;

    /** Constructs a new RDFEntityIterator instance.
     *
     * @param repositoryConnection the repository connection
     * @param rdfEntityClass the given RDF entity class
     * @param overrideContextURI the override context
     */
    public RDFEntityIterator(
            final RepositoryConnection repositoryConnection,
            final Class<?> rdfEntityClass,
            final URI overrideContextURI) {
      //Preconditions
      assert repositoryConnection != null : "repositoryConnection must not be null";
      assert rdfEntityClass != null : "rdfEntityClass must not be null";

      this.repositoryConnection = repositoryConnection;
      this.rdfEntityClass = rdfEntityClass;
      saveAbstractSessionState();
      saveSessionState();
      connectedRDFEntityDictionary.clear();
      initializeAbstractSessionState();
      setRDFEntityClass(rdfEntityClass);
      gatherAnnotationsForRDFEntityClass();
      setOverrideContextURI(overrideContextURI);
      configureRDFEntitySettings();
      effectiveContextURI = getEffectiveContextURI();
      assert effectiveContextURI != null;
      final Set<URI> rdfEntityURISet = new HashSet<>();
      try {
        if (overrideContextURI == null || overrideContextURI.equals(getContextURI())) {
          // query the type statement
          final TupleQuery subjectsOfTypeTupleQuery = repositoryConnection.prepareTupleQuery(
                  QueryLanguage.SERQL,
                  "SELECT s FROM {s} rdf:type {o}");
          subjectsOfTypeTupleQuery.setBinding("o", getClassURI());
          final TupleQueryResult tupleQueryResult = subjectsOfTypeTupleQuery.evaluate();
          while (tupleQueryResult.hasNext()) {
            rdfEntityURISet.add((URI) tupleQueryResult.next().getBinding("s").getValue());
          }
          tupleQueryResult.close();
        } else {
          // query the overrideContext statement
          final TupleQuery subjectsOfTypeTupleQuery = repositoryConnection.prepareTupleQuery(
                  QueryLanguage.SERQL,
                  "SELECT s FROM {s} p {o}");
          subjectsOfTypeTupleQuery.setBinding("p", URI_OVERRIDE_CONTEXT);
          subjectsOfTypeTupleQuery.setBinding("o", overrideContextURI);
          final TupleQueryResult tupleQueryResult = subjectsOfTypeTupleQuery.evaluate();
          while (tupleQueryResult.hasNext()) {
            rdfEntityURISet.add((URI) tupleQueryResult.next().getBinding("s").getValue());
          }
          tupleQueryResult.close();
        }
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
      restoreSessionState();
      restoreAbstractSessionState();
      rdfEntityURISet_iter = rdfEntityURISet.iterator();
    }

    /** Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    @Override
    public boolean hasNext() {
      return rdfEntityURISet_iter.hasNext();
    }

    /** Returns the next element in the iteration.  Calling this method
     * repeatedly until the {@link #hasNext()} method returns false will
     * return each element in the underlying collection exactly once.
     *
     * @return the next element in the iteration.
     */
    @SuppressWarnings("unchecked")
    @Override
    public E next() {
      if (!rdfEntityURISet_iter.hasNext()) {
        throw new NoSuchElementException("iterator is empty and cannot return the next element");
      }
      final URI rdfEntityURI = rdfEntityURISet_iter.next();
      saveAbstractSessionState();
      saveSessionState();

      connectedRDFEntityDictionary.clear();
      setEffectiveContextURI(effectiveContextURI);
      assert getEffectiveContextURI() != null;
      final Object rdfEntity = findRDFEntity(
              repositoryConnection,
              rdfEntityClass,
              rdfEntityURI);
      assert rdfEntity != null : "rdfEntity not found: " + rdfEntityURI + ", class: " + rdfEntityClass;
      restoreAbstractSessionState();
      restoreSessionState();
      return (E) rdfEntity;
    }

    /** Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     */
    @Override
    public void remove() {
      rdfEntityURISet_iter.remove();
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return "[iterator over instances of " + rdfEntityClass.getName() + "]";
    }
  }

  /** Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }

  /** Finds and loads the RDF entity from propositions in the knowledge base given its instance URI, without clearing
   * the dictionary of connected RDF entities.  If the given class is null then it is looked up using the URI.
   *
   * @param <T> the class type
   * @param repositoryConnection the repository connection
   * @param clazz the RDF entity class
   * @param instanceURI the URI that represents the RDF entity
   * @return the RDF entity or null if not found
   */
  @SuppressWarnings("unchecked")
  private <T> T findRDFEntity(
          final RepositoryConnection repositoryConnection,
          final Class<T> clazz,
          final URI instanceURI) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert instanceURI != null : "instanceURI must not be null";

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "loading RDF entity instance URI: " + instanceURI);
    }

    initializeAbstractSessionState();
    Class<?> rdfEntityClass = clazz;
    if (clazz == null) {
      rdfEntityClass = getJavaClass(instanceURI);
      if (rdfEntityClass == null) {
        throw new TexaiException("cannot determine the RDF entity class from the URI " + instanceURI);
      }

    }
    setRDFEntityClass(rdfEntityClass);
    setInstanceURI(instanceURI);
    predicateValuesDictionary = queryForPredicateAndValues(repositoryConnection);
    if (predicateValuesDictionary.isEmpty()) {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "no predicateValuesDictionary for instance URI: " + instanceURI);
      }

      setRDFEntity(null);
    } else {
      //validatePredicateValuesDictionary(instanceURI.toString());
      gatherAnnotationsForRDFEntityClass();
      configureRDFEntitySettings();
      instantiateRDFEntity();
      loadIdField();
      loadFields(repositoryConnection);
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "caching connected RDF entity: " + instanceURI + "-->" + getRDFEntity().getClass().getName());
      }

    }

    return (T) getRDFEntity();
  }

  /** Returns the set of instance URIs having the given RDF predicate and RDF value.
   *
   * @param repositoryConnection the repository connection
   * @param predicate the identifying RDF predicate
   * @param rdfValue the RDF value of the predicate which identifies the desired RDF entity
   * @param rdfEntityClass the class of the desired RDF entity
   * @return the set of instance URIs having the given RDF predicate, RDF value and given class
   */
  private List<URI> findInstanceURIsByPredicateAndValue(
          final RepositoryConnection repositoryConnection,
          final URI predicate,
          final Value rdfValue,
          final Class<?> rdfEntityClass) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert predicate != null : "predicate must not be null";
    assert rdfValue != null : "value must not be null";
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    final String rdfEntityClassName = rdfEntityClass.getName();
    final List<URI> instanceURIs = new ArrayList<>();
    try {
      final TupleQuery subjectsTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT s, c FROM CONTEXT c {s} p {o}");
      subjectsTupleQuery.setBinding("p", predicate);
      subjectsTupleQuery.setBinding("o", rdfValue);
      final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI instanceURI = (URI) bindingSet.getBinding("s").getValue();
        final URI contextURI1 = (URI) bindingSet.getBinding("c").getValue();
        final String className = getJavaClass(instanceURI).getName();
        if (className == null || className.isEmpty()) {
          throw new TexaiException("missing class name for URI " + instanceURI);
        }

        if (className.equals(rdfEntityClassName) && getEffectiveContextURI().equals(contextURI1)) {
          instanceURIs.add(instanceURI);
        }

      }
      tupleQueryResult.close();
    } catch (final OpenRDFException ex) {
      throw new TexaiException(ex);
    }

    return instanceURIs;
  }

  /** Sets the instance URI from the id field contained in the RDF entity. */
  private void setInstanceURIFromIdField() {
    //Preconditions
    assert getRDFEntity() != null : "rdfEntity() must not be null";

    if (getRDFEntity() instanceof RDFPersistent) {
      setInstanceURI((getRDFEntity()).getId());
      return;
    }
    final Field idField = getIdField();
    if (idField == null) {
      throw new TexaiException("Id field not found for RDF entity " + getRDFEntity().getClass().getName());
    }

    Object value;
    if (!idField.isAccessible()) {
      idField.setAccessible(true);
    }

    try {
      value = idField.get(getRDFEntity());
    } catch (final IllegalArgumentException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }

    if (value instanceof String || value instanceof java.net.URI) {
      setInstanceURI(getValueFactory().createURI(value.toString()));
    } else if (URI.class.isAssignableFrom(value.getClass())) {
      setInstanceURI((URI) value);
    } else {
      throw new TexaiException("cannot load ID from " + value);
    }

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "  found instance URI " + getInstanceURI());
    }
  }

  /** Instantiates the RDF entity. */
  private void instantiateRDFEntity() {
    //Preconditions
    assert getRDFEntityClass() != null : "rdfEntityClass must not be null";
    assert getInstanceURI() != null : "instanceURI must not be null";

    RDFPersistent rdfEntity;

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "instantiating: " + getRDFEntityClass().getName() + " for " + getInstanceURI());
    }

    try {
      rdfEntity = (RDFPersistent) getRDFEntityClass().newInstance();
    } catch (final IllegalAccessException ex) {
      throw new TexaiException(ex);
    } catch (final InstantiationException ex) {
      throw new TexaiException("exception instantiating: " + getRDFEntityClass().getName() + " for "
              + getInstanceURI() + "\n RDF entity classes require a default constructor"
              + "\n This error can also be caused by an invalid @RDFEntity subject annotation.", ex);
    }

    setRDFEntity(rdfEntity);
    connectedRDFEntityDictionary.put(getInstanceURI(), getRDFEntity());
  }

  /** Loads the id field of the RDF entity. */
  private void loadIdField() {
    //Preconditions
    assert getRDFEntity() != null : "rdfEntity must not be null";

    final Field idField = getIdField();
    if (idField == null) {
      throw new TexaiException("ID field not found for RDF entity " + getRDFEntity());
    }

    if (!idField.isAccessible()) {
      idField.setAccessible(true);
    }

    try {
      final Class<?> idFieldType = idField.getType();
      if (idFieldType.equals(String.class)) {
        idField.set(getRDFEntity(), getInstanceURI().toString());
      } else if (idFieldType.equals(java.net.URI.class)) {
        idField.set(getRDFEntity(), new java.net.URI(getInstanceURI().toString()));
      } else if (URI.class.isAssignableFrom(idFieldType)) {
        idField.set(getRDFEntity(), getInstanceURI());
      } else {
        throw new TexaiException("cannot load id for " + getInstanceURI() + " into ID field type " + idFieldType.getName());
      }

    } catch (final IllegalArgumentException ex) {
      throw new TexaiException(ex.getMessage() + "\n  rdfEntity: " + getRDFEntity() + "\n  instanceURI: " + getInstanceURI(), ex);
    } catch (final URISyntaxException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Loads the RDF entity fields from propositions in the knowledge base.
   *
   * @param repositoryConnection the repository connection
   */
  private void loadFields(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getInstanceURI() != null : "instanceURI must not be null";

    for (final Field field : getFieldAnnotationDictionary().keySet()) {
      final Annotation annotation = getFieldAnnotationDictionary().get(field);
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "loading field: " + field + ", annotation: " + annotation);
      }

      if ("@javax.persistence.Id()".equals(annotation.toString())) {
        if (isDebugEnabled) {
          getLogger().debug(stackLevel() + "  skipping Id field");
        }
      } else {
        if (annotation instanceof RDFProperty) {
          final Class<?> fieldType = field.getType();
          final RDFProperty rdfProperty = (RDFProperty) annotation;
          if (rdfProperty.fetch().equals(FetchType.EAGER)) {
            loadField(
                    repositoryConnection,
                    field,
                    rdfProperty,
                    predicateValuesDictionary);
          } else {
            // default behavior is lazy loading
            if (Set.class.isAssignableFrom(fieldType)) {
              if (isDebugEnabled) {
                getLogger().debug(stackLevel() + "  lazySet for field: " + field);
              }
              final LazySet lazySet = new LazySet(
                      repositoryConnection,
                      getRDFEntity(),
                      field,
                      rdfProperty,
                      predicateValuesDictionary);

              setFieldValue(field, lazySet, fieldType, repositoryConnection);
            } else if (List.class.isAssignableFrom(fieldType)) {
              if (isDebugEnabled) {
                getLogger().debug(stackLevel() + "  lazyList for field: " + field);
              }
              final LazyList lazyList = new LazyList(
                      repositoryConnection,
                      getRDFEntity(),
                      field,
                      rdfProperty,
                      predicateValuesDictionary);

              setFieldValue(field, lazyList, fieldType, repositoryConnection);
            } else if (Map.class.isAssignableFrom(fieldType)) {
              if (isDebugEnabled) {
                getLogger().debug(stackLevel() + "  lazyMap for field: " + field);
              }
              final LazyMap lazyMap = new LazyMap(
                      repositoryConnection,
                      getRDFEntity(),
                      field,
                      rdfProperty,
                      predicateValuesDictionary);

              setFieldValue(field, lazyMap, fieldType, repositoryConnection);
            } else if (isRDFEntityClass(fieldType)) {
              setToDynmicallyCreatedProxy(
                      repositoryConnection,
                      field,
                      fieldType,
                      rdfProperty,
                      predicateValuesDictionary);
            } else {
              // otherwise load the value now
              loadField(
                      repositoryConnection,
                      field,
                      rdfProperty,
                      predicateValuesDictionary);
            }

          }
        }
      }
    }
  }

  /** Sets the field to a dynamically created proxy, using cglib that will lazily load the RDF entity field.  If the URI value
   * is null then the field is left null.
   *
   * @param repositoryConnection the repository connection
   * @param field the field
   * @param fieldType the field type, which is an RDF entity
   * @param rdfProperty the RDF property annotation that describes the field
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   */
  private void setToDynmicallyCreatedProxy(
          final RepositoryConnection repositoryConnection,
          final Field field,
          final Class<?> fieldType,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert field != null : "field must not be null";
    assert fieldType != null : "fieldType must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateValuesDictionary != null : "predicateValuesDictionary must not be null";
    assert getEffectiveContextURI() != null : "predicateValuesDictionary must not be null";

    // see if there is a URI value for this field
    final URI predicate;
    predicate = getEffectivePropertyURI(field, rdfProperty);
    List<Value> rdfValues;
    if (rdfProperty.inverse()) {
      try {
        rdfValues = new ArrayList<>();
        final TupleQuery subjectsTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                "SELECT s, c FROM CONTEXT c {s} p {o}");
        subjectsTupleQuery.setBinding("p", predicate);
        subjectsTupleQuery.setBinding("o", getInstanceURI());
        //subjectsTupleQuery.setBinding("c", getEffectiveContextURI());
        final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          final URI contextURI1 = (URI) bindingSet.getBinding("c").getValue();
          if (getEffectiveContextURI().equals(contextURI1)) {
            rdfValues.add(bindingSet.getBinding("s").getValue());
          }
        }

        tupleQueryResult.close();
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }

    } else {
      rdfValues = predicateValuesDictionary.get(predicate);
    }

    if (rdfValues == null || rdfValues.isEmpty()) {
      // no URI value for this field, so no need for a proxy to lazily load it
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  URI value not found, field is null: " + field);
      }

      return;
    }

    //TODO cannot use a dynamic proxy on the android platform

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "  lazyily loaded proxy for field: " + field);
    }
    //  load the value upon first access to the associated proxy
    final RDFEntityLazyLoader rdfEntityLazyLoader = new RDFEntityLazyLoader(
            repositoryConnection,
            getRDFEntity(),
            field,
            rdfProperty,
            predicateValuesDictionary);
    // dynamically create the proxy using cglib
    Object lazyObjectProxy;

    final Factory factory = proxyFactoryDictionary.get(fieldType);
    if (factory == null) {
      lazyObjectProxy = Enhancer.create(fieldType, rdfEntityLazyLoader);
      proxyFactoryDictionary.put(fieldType, (Factory) lazyObjectProxy);
    } else {
      lazyObjectProxy = factory.newInstance(rdfEntityLazyLoader);
    }

    setFieldValue(field, lazyObjectProxy, fieldType, repositoryConnection);
  }

  /** Queries for the predicate and RDF rdfValues of matching RDF triples having the subject filled by the instanceURI.
   *
   * @param repositoryConnection the repository connection
   * @return the list of predicate and RDF value
   */
  private Map<URI, List<Value>> queryForPredicateAndValues(final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert getInstanceURI() != null : "instance uri must not be null in " + getRDFEntity();

    final Map<URI, List<Value>> tempPredicateValuesDictionary = new HashMap<>();
    try {
      final TupleQuery predicatesAndObjectsTupleQuery = repositoryConnection.prepareTupleQuery(
              QueryLanguage.SERQL,
              "SELECT p, o FROM {s} p {o}");
      predicatesAndObjectsTupleQuery.setBinding("s", getInstanceURI());
      final TupleQueryResult tupleQueryResult = predicatesAndObjectsTupleQuery.evaluate();
      while (tupleQueryResult.hasNext()) {
        final BindingSet bindingSet = tupleQueryResult.next();
        final URI predicate = (URI) bindingSet.getBinding("p").getValue();
        List<Value> rdfValues = tempPredicateValuesDictionary.get(predicate);
        if (rdfValues == null) {
          rdfValues = new ArrayList<>();
          tempPredicateValuesDictionary.put(predicate, rdfValues);
        }

        rdfValues.add(bindingSet.getBinding("o").getValue());
      }

      tupleQueryResult.close();
    } catch (final RepositoryException | MalformedQueryException | QueryEvaluationException ex) {
      getLogger().error("repositoryConnection: " + repositoryConnection);
      throw new TexaiException(ex);
    }

    return tempPredicateValuesDictionary;
  }

  /** Loads the given field according to the given RDF property.
   *
   * @param repositoryConnection the repository connection
   * @param field the given RDF entity instance field
   * @param rdfProperty the property annotation associated with the field
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   * @return the loaded value
   */
  @SuppressWarnings("unchecked")
  private Object loadField(
          final RepositoryConnection repositoryConnection,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateValuesDictionary != null : "predicateValuesDictionary must not be null";

    this.predicateValuesDictionary = predicateValuesDictionary;

    // obtain field attributes
    final Class<?> fieldType = field.getType();
    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "  field: " + field);
      getLogger().debug(stackLevel() + "  field type: " + fieldType.getName());
    }

    final List<Object> values = getValues(
            repositoryConnection,
            field,
            rdfProperty,
            predicateValuesDictionary);
    final int values_size = values.size();
    // load the value according to its field type
    Object loadedValue = null;
    if (fieldType.isArray()) {
      // load an array field
      final Object array = Array.newInstance(fieldType.getComponentType(), values_size);
      for (int i = 0; i
              < values_size; i++) {
        Array.set(array, i, values.get(i));
      }

      loadedValue = array;
      setFieldValue(
              field,
              loadedValue,
              fieldType,
              repositoryConnection);

    } else if (fieldType.equals(List.class)) {
      // load a List field
      final List<Object> list = new ArrayList<>();
      for (final Object value : values) {
        list.add(value);
      }
      loadedValue = list;
      setFieldValue(field, loadedValue, fieldType, repositoryConnection);

    } else if (fieldType.equals(Map.class)) {
      // load a Map field
      final Map<Object, Object> map = new HashMap<>();
      for (final Object value : values) {
        final MapEntry mapEntry = (MapEntry) value;
        map.put(mapEntry.key, mapEntry.value);
      }
      loadedValue = map;
      setFieldValue(field, loadedValue, fieldType, repositoryConnection);

    } else if (Collection.class.isAssignableFrom(fieldType)) {
      // load a Collection field
      Collection<Object> collection;
      Class<?> concreteFieldType;
      if (fieldType.equals(Collection.class)) {
        concreteFieldType = ArrayList.class;    // NOPMD
      } else if (fieldType.equals(HashSet.class)) {    // NOPMD
        // if the field specifies HashSet then instantiate one, otherwise for Set fields instantiate an ArraySet
        // which is more efficient for construction and iteration
        concreteFieldType = HashSet.class;    // NOPMD
      } else if (fieldType.equals(Set.class)) {
        concreteFieldType = ArraySet.class;
      } else {
        concreteFieldType = fieldType;
      }
      try {
        collection = (Collection<Object>) concreteFieldType.newInstance();
      } catch (final InstantiationException | IllegalAccessException ex) {
        throw new TexaiException(ex);
      }
      for (final Object value : values) {
        collection.add(value);
      }
      loadedValue = collection;
      setFieldValue(field, loadedValue, fieldType, repositoryConnection);

    } else {
      // load a single value field
      if (values_size > 1) {
        getLogger().info(stackLevel() + "expected only one value for field " + field + " but found\n  " + values
                + " for instanceURI " + getInstanceURI());
      }

      if (values.isEmpty()) {
        if (isDebugEnabled) {
          getLogger().debug(stackLevel() + "  field has no values to load " + field);
        }
      } else {
        loadedValue = values.get(0);
        setFieldValue(field, loadedValue, fieldType, repositoryConnection);
      }
    }
    return loadedValue;
  }

  /** Gets the values for loading the given field.
   *
   * @param repositoryConnection the repository connection
   * @param field the given RDF entity instance field
   * @param rdfProperty the property annotation associated with the field
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   * @return the unordered list of values for the field
   */
  private List<Object> getValues(
          final RepositoryConnection repositoryConnection,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateValuesDictionary != null : "predicateValuesDictionary must not be null";

    List<Object> values;
    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "  processing RDF property: " + rdfProperty);
    }

    final boolean isBooleanField = "boolean".equals(field.getType().getName());
    URI predicate;

    if (isBooleanField) {
      predicate = RDF.TYPE;
    } else {
      predicate = getEffectivePropertyURI(field, rdfProperty);
    }

    if (!field.isAccessible()) {
      field.setAccessible(true);
    }

    final Class<?> fieldType = field.getType();
    if (rdfProperty.inverse()) {
      if (isBooleanField) {
        throw new TexaiException("the inverse annotation is not applicable to boolean fields");
      }

      values = queryForInverseValues(
              repositoryConnection,
              predicate,
              fieldType,
              rdfProperty.inverse());
    } else {
      List<Value> rdfValues = predicateValuesDictionary.get(predicate);
      if (rdfValues == null) {
        return new ArrayList<>(0);                                  // NOPMD
      }

      // if the field is a List or an array then the value is the blank node that heads the actual value list
      if (List.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
        assert rdfValues.size() == 1 : "only one blank node must be present " + rdfValues
                + " in " + repositoryConnection.getRepository().getDataDir();
        assert rdfValues.get(
                0) instanceof BNode : "RDF value must be a blank node " + rdfValues.get(0);
        final BNode rdfListHead = (BNode) rdfValues.get(0);
        rdfValues = getRDFListValues(repositoryConnection, rdfListHead);

      } else if (Map.class.isAssignableFrom(fieldType)) {
        // if the field is a Map then the RDF values are the blank nodes that each relates a map entry key and map entry value
        return getMapValues(repositoryConnection, rdfValues, rdfProperty);
      }

      // transform the RDF values to java objects
      values = new ArrayList<>(rdfValues.size());
      for (final Value rdfValue : rdfValues) {
        if (isBooleanField) {
          values.add(rdfValue);
        } else {
          values.add(getJavaValueFromRDFValue(
                  repositoryConnection,
                  rdfValue,
                  field.getType()));
        }
      }
    }
    if (isBooleanField) {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  boolean field: " + field.getName());
      }
      // for boolean values, return a true value if the trueClass is among the queried values, otherwise return a false value
      String trueClassName = rdfProperty.trueClass();
      if (trueClassName.isEmpty()) {
        // default true class
        trueClassName = getRDFEntity().getClass().getName() + "_" + field.getName() + "_True";
      }

      final URI trueClass = makeURI(trueClassName);
      String falseClassName = rdfProperty.falseClass();
      if (falseClassName.isEmpty()) {
        // default false class
        falseClassName = getRDFEntity().getClass().getName() + "_" + field.getName() + "_False";
      }

      final URI falseClass = makeURI(falseClassName);
      // note that there will only be one boolean value returned
      final List<Object> booleanValues = new ArrayList<>(1);
      for (final Object value : values) {
        if (isDebugEnabled) {
          getLogger().debug(stackLevel() + "    " + value + " equals " + trueClass + " ?");
        }

        if (value.equals(trueClass)) {
          booleanValues.add(Boolean.TRUE);
          return booleanValues;                                           // NOPMD
        }

      }
      for (final Object value : values) {
        if (isDebugEnabled) {
          getLogger().debug(stackLevel() + "    " + value + " equals " + trueClass + " ?");
        }

        if (value.equals(falseClass)) {
          booleanValues.add(Boolean.FALSE);
          return booleanValues;                                           // NOPMD
        }

      }
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  boolean field true/false class not found for : " + field.getName() + ", defaulting to false");
      }

      booleanValues.add(Boolean.FALSE);
      values = booleanValues;
    }

    return values;
  }

  /** Gathers the MapEntry objects corresponding to the given blank node RDF values that each relates a persisted
   * map entry key and map entry value.
   *
   * @param repositoryConnection the repository connection
   * @param rdfValues the given RDF values
   * @param rdfProperty the RDFProperty annotation of the field to be persisted
   * @return the MapEntry objects corresponding to the given blank node RDF values
   */
  public List<Object> getMapValues(
          final RepositoryConnection repositoryConnection,
          final List<Value> rdfValues,
          final RDFProperty rdfProperty) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfValues != null : "rdfValues must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";

    final List<Object> values = new ArrayList<>();
    for (final Value rdfValue : rdfValues) {
      assert rdfValue instanceof BNode;
      final MapEntry mapEntry = MapEntry.makeMapEntry(
              (BNode) rdfValue, // bNode
              repositoryConnection,
              getEffectiveContextURI());

      // get the type of the map key
      final String mapKeyTypeName = rdfProperty.mapKeyType();
      if (mapKeyTypeName == null || mapKeyTypeName.isEmpty()) {
        throw new TexaiException("Map field @RDFProperty annotation is missing its required mapKeyType property");
      }
      final Class<?> mapKeyType;
      try {
        mapKeyType = Class.forName(mapKeyTypeName);
      } catch (ClassNotFoundException ex) {
        throw new TexaiException("The @RDFProperty mapKeyType class was not found: " + mapKeyTypeName);
      }

      // convert the key RDF value to the corresponding Java object
      mapEntry.key = getJavaValueFromRDFValue(repositoryConnection, mapEntry.keyRDFValue, mapKeyType);

      // get the type of the map value
      final String mapValueTypeName = rdfProperty.mapValueType();
      if (mapValueTypeName == null || mapValueTypeName.isEmpty()) {
        throw new TexaiException("Map field @RDFProperty annotation is missing its required mapValueType property");
      }
      final Class<?> mapValueType;
      try {
        mapValueType = Class.forName(mapValueTypeName);
      } catch (ClassNotFoundException ex) {
        throw new TexaiException("The @RDFProperty mapValueType class was not found: " + mapValueTypeName);
      }

      // convert the value RDF value to the corresponding Java object
      mapEntry.value = getJavaValueFromRDFValue(repositoryConnection, mapEntry.valueRDFValue, mapValueType);
      values.add(mapEntry);
    }
    return values;
  }

  /** Sets the given field to the given value if the value is not null.
   *
   * @param field the given field to be set
   * @param value the field value
   * @param fieldType the field type
   * @param repositoryConnection the repository connection
   */
  private void setFieldValue(final Field field,
          final Object value,
          final Class<?> fieldType,
          final RepositoryConnection repositoryConnection) {
    //Preconditions
    assert field != null : "field must not be null";
    assert fieldType != null : "fieldType must not be null";

    if (value == null) {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  value is null");
      }
    } else {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  load value (" + value.getClass().getName() + ") into fieldType " + fieldType.getName());
      }

      if (!field.isAccessible()) {
        field.setAccessible(true);
      }

      try {
        field.set(getRDFEntity(), value);
      } catch (final IllegalArgumentException ex) {
        final StringBuilder stringBuilder = new StringBuilder();
        try {
          final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(
                  getInstanceURI(), // subj
                  null, // pred
                  null, // obj
                  false); // includeInferred
          while (repositoryResult.hasNext()) {
            stringBuilder.append('\n');
            stringBuilder.append(RDFUtility.formatStatement(repositoryResult.next()));
          }
          stringBuilder.append('\n');
        } catch (RepositoryException ex1) {
          throw new TexaiException(ex1);
        }
        throw new TexaiException(
                ex.getMessage()
                + "\ninstanceURI: " + getInstanceURI()
                + "\nfield: " + field
                + "\nvalue: " + value + " value class: " + value.getClass().getName()
                + "\nfieldType: " + fieldType
                + "\nperhaps the field's value is an unpersisted new entity,"
                + "\n  or if the value has an incompatible type, suspect a corrupt repository"
                + "\nstatments:" + stringBuilder.toString(), ex);
      } catch (final IllegalAccessException ex) {
        throw new TexaiException(
                ex.getMessage() + "\nfield: " + field + "\nvalue: " + value + "\nfieldType: " + fieldType, ex);
      }

    }
  }

  /** Queries for the value terms filling the object position of matching RDF triples having the
   * given predicate, and the subject filled by the instanceURI.  The RDF value terms are translated into java objects.
   *
   * @param repositoryConnection the repository connection
   * @param predicate the predicate that relates the value for the RDF entity field
   * @param fieldType the field type
   * @param isInverseProperty the indicator that the property is to be inverted with respect to treatment of subject and object
   * @return the list of java values
   */
  private List<Object> queryForInverseValues(
          final RepositoryConnection repositoryConnection,
          final URI predicate,
          final Class<?> fieldType,
          final boolean isInverseProperty) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert predicate != null : "predicate must not be null";
    assert fieldType != null : "fieldType must not be null";

    List<Value> rdfValues = new ArrayList<>();
    if (isInverseProperty) {
      try {
        final TupleQuery subjectsTupleQuery = repositoryConnection.prepareTupleQuery(
                QueryLanguage.SERQL,
                "SELECT s, c FROM CONTEXT c {s} p {o}");
        subjectsTupleQuery.setBinding("p", predicate);
        subjectsTupleQuery.setBinding("o", getInstanceURI());
        //subjectsTupleQuery.setBinding("c", getEffectiveContextURI());
        final TupleQueryResult tupleQueryResult = subjectsTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          final URI contextURI1 = (URI) bindingSet.getBinding("c").getValue();
          if (getEffectiveContextURI().equals(contextURI1)) {
            rdfValues.add(bindingSet.getBinding("s").getValue());
          }
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
        objectsTupleQuery.setBinding("p", predicate);
        //objectsTupleQuery.setBinding("c", getEffectiveContextURI());
        final TupleQueryResult tupleQueryResult = objectsTupleQuery.evaluate();
        while (tupleQueryResult.hasNext()) {
          final BindingSet bindingSet = tupleQueryResult.next();
          final URI contextURI1 = (URI) bindingSet.getBinding("c").getValue();
          if (getEffectiveContextURI().equals(contextURI1)) {
            rdfValues.add(bindingSet.getBinding("o").getValue());
          }
        }

        tupleQueryResult.close();
      } catch (final OpenRDFException ex) {
        throw new TexaiException(ex);
      }
    }

    // if the field is a List or an array then the value is the blank node that heads the actual value list
    if (List.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
      assert rdfValues.size() == 1 : "only one blank node must be present " + rdfValues;
      assert rdfValues.get(
              0) instanceof BNode : "RDF value must be a blank node " + rdfValues.get(0);
      final BNode rdfListHead = (BNode) rdfValues.get(0);
      rdfValues = getRDFListValues(repositoryConnection, rdfListHead);
    }

    // transform RDF values to java objects
    final List<Object> values = new ArrayList<>(rdfValues.size());
    for (final Value rdfValue : rdfValues) {
      values.add(getJavaValueFromRDFValue(
              repositoryConnection,
              rdfValue,
              fieldType));
    }

    return values;
  }

  /**
   * Gets the java value from the given RDF value.  In case of RDF entities,
   * state is saved and a recursive RDF entity load is performed.
   *
   * @param repositoryConnection the repository connection
   * @param rdfValue the RDF value
   * @param fieldType the field type
   * @return the java value from the given RDF value
   */
  private Object getJavaValueFromRDFValue(
          final RepositoryConnection repositoryConnection,
          final Value rdfValue,
          final Class<?> fieldType) {   // NOPMD
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfValue != null : "valueTerm must not be null";

    Object value = null;

    if (fieldType.equals(URI.class)) {
      return rdfValue;
    } else if (rdfValue instanceof Literal) {
      if (fieldType.equals(Literal.class) || fieldType.equals(Value.class)) {
        return rdfValue;
      }
      final Literal literal = (Literal) rdfValue;
      if (fieldType.equals(UUID.class)) {
        value = UUID.fromString(literal.getLabel());
      } else if (literal.getDatatype() == null || literal.getDatatype().equals(XMLSchema.STRING)) {
        value = literal.getLabel();
      } else if (literal.getDatatype().equals(XMLSchema.BYTE) || literal.getDatatype().equals(XMLSchema.UNSIGNED_BYTE)) {
        value = literal.byteValue();
      } else if (literal.getDatatype().equals(XMLSchema.SHORT) || literal.getDatatype().equals(XMLSchema.UNSIGNED_SHORT)) {
        value = literal.shortValue();
      } else if (literal.getDatatype().equals(XMLSchema.INT) || literal.getDatatype().equals(XMLSchema.UNSIGNED_INT)) {
        value = literal.intValue();
      } else if (literal.getDatatype().equals(XMLSchema.LONG) || literal.getDatatype().equals(XMLSchema.UNSIGNED_LONG)) {
        if (int.class.equals(fieldType) || Integer.class.equals(fieldType)) {
          // legacy
          value = literal.intValue();
        } else {
          value = literal.longValue();
        }
      } else if (literal.getDatatype().equals(XMLSchema.FLOAT)) {
        value = literal.floatValue();
      } else if (literal.getDatatype().equals(XMLSchema.DOUBLE)) {
        value = literal.doubleValue();
      } else if (literal.getDatatype().equals(XMLSchema.INTEGER) || literal.getDatatype().equals(XMLSchema.POSITIVE_INTEGER) || literal.getDatatype().equals(XMLSchema.NON_NEGATIVE_INTEGER) || literal.getDatatype().equals(XMLSchema.NON_POSITIVE_INTEGER) || literal.getDatatype().equals(XMLSchema.NEGATIVE_INTEGER)) {
        value = literal.integerValue();
      } else if (literal.getDatatype().equals(XMLSchema.DECIMAL)) {
        value = literal.decimalValue();
      } else if (literal.getDatatype().equals(XMLSchema.DATETIME)) {
        final Calendar calendarValue = DatatypeConverter.parseDateTime(literal.getLabel());
        if (fieldType.equals(Date.class)) {
          value = calendarValue.getTime();
        } else if (fieldType.equals(Calendar.class)) {
          value = calendarValue;
        } else if (fieldType.equals(DateTime.class)) {
          value = new DateTime(
                  calendarValue,
                  (Chronology) null); // force ISO chronolgy
        } else {
          throw new TexaiException("cannot load " + literal + " into field type " + fieldType.getName());
        }

      } else if (literal.getDatatype().equals(XMLSchema.ANYURI)) {
        try {
          value = new java.net.URI(literal.getLabel());
        } catch (final URISyntaxException ex) {
          throw new TexaiException("cannot form URI from " + literal, ex);
        }

      }
      return value;                                                       // NOPMD
    }

    if (fieldType.isAssignableFrom(rdfValue.getClass())) {
      return rdfValue;
    }

    if (Collection.class.isAssignableFrom(fieldType) || fieldType.isArray()) {
      if (rdfValue instanceof BNode) {
        // the value is the blank node that heads the list of actual values
        return rdfValue;
      }
      assert rdfValue instanceof URI : "rdfValue must be type URI";
      if (getJavaClass((URI) rdfValue) == null) {
        // URI is not an RDF entity id
        return rdfValue;
      }
    }

    value = connectedRDFEntityDictionary.get((URI) rdfValue);
    if (value == null) {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  RDF value: " + rdfValue);
      }
      // save this state before the recursive method call
      saveAbstractSessionState();
      saveSessionState();
      final DistributedRepositoryManager distributedRepositoryManager = DistributedRepositoryManager.getInstance();
      // obtain a new repository connection according to where the entity is persisted
      final String repositoryName = distributedRepositoryManager.getRepositoryNameForInstance((URI) rdfValue);
      if (repositoryName == null) {
        // the field type may be a superclass of the RDF entity, therefore do not pass it as a parameter
        value = find(repositoryConnection, (URI) rdfValue);
      } else {
        final RepositoryConnection repositoryConnection2 = distributedRepositoryManager.getRepositoryConnectionForRepositoryName(repositoryName);
        // the field type may be a superclass of the RDF entity, therefore do not pass it as a parameter
        value = find(repositoryConnection2, (URI) rdfValue);
        try {
          repositoryConnection2.close();
        } catch (final RepositoryException ex) {
          throw new TexaiException(ex);
        }
      }
      if (value == null) {
        // domain entity not found when treating the rdfValue as an ID
        value = rdfValue;
      }
      restoreSessionState();
      restoreAbstractSessionState();
    } else {
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  previously loaded value: " + value);
      }

    }
    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "  RDF value " + rdfValue + " ==>  java value " + value);
    }

    return value;
  }

  /** Determines the java class of the RDF entity indentified by the given URI.
   *
   * @param uri the URI that identifies an RDF entity
   * @return the java class of the RDF entity indentified by the given URI
   */
  private Class<?> getJavaClass(final URI uri) {
    //preconditions
    assert uri != null : "uri must not be null";

    Class<?> clazz;
    // attempt to parse the class name from the URI
    final String parsedClassName = parseClassNameFromURI(uri);
    if (parsedClassName == null) {
      // URI is not an RDF entity id
      return null;
    }
    // does the class name have a cached class?
    clazz = classNameDictionary.get(parsedClassName);
    if (clazz == null) {
      try {
        // try to lookup the class name parsed from the URI
        clazz = Class.forName(parsedClassName);
        classNameDictionary.put(parsedClassName, clazz);
      } catch (final ClassNotFoundException ex) {
        throw new TexaiException("cannot find entity class " + parsedClassName + " from malformed URI " + uri, ex);
      }
      if (isDebugEnabled) {
        getLogger().debug(stackLevel() + "  URI: " + uri + " parsed class: " + clazz);
      }
    }
    return clazz;
  }

  /** Returns the class name parsed from the given URI.
   *
   * @param uri the given URI
   * @return the class name parsed from the given URI if it has the format of an RDF entity id, otherwise returns null
   */
  private String parseClassNameFromURI(final URI uri) {
    //preconditions
    assert uri != null : "uri must not be null";

    final String localName = uri.getLocalName();
    int index = -1;
    for (int i = localName.length() - 1; i >= 0; i--) {
      if (localName.charAt(i) == '_') {
        index = i;
        break;
      }

    }
    if (index == -1) {
      return null;
    } else {
      final String className = localName.substring(0, index);
      if (className.startsWith("org.texai.")) {
        return className;
      } else {
        // handle legacy WordNet URIs
        return "org.texai.wordnet.domain.entity." + className;
      }
    }
  }

  /** Pushes the current session bean state onto a stack and then initializes the session state. */
  public void saveSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "saving session state");
    }

    rdfEntityInfoStack.push(new RDFEntityInfo(connectedRDFEntityDictionary, predicateValuesDictionary));
  }

  /** Restores the session state following a recursive method call. */
  public void restoreSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    final RDFEntityInfo rdfEntityInfo = rdfEntityInfoStack.pop();
    connectedRDFEntityDictionary =
            rdfEntityInfo.connectedRDFEntityDictionary;
    predicateValuesDictionary =
            rdfEntityInfo.predicateValuesDictionary;
    if (isDebugEnabled) {
      getLogger().debug(stackLevel() + "restored session state");
    }
  }

  /** Contains the state for recursive method calls. */
  private static class RDFEntityInfo {

    /** the dictionary of connected RDF entities, URI --> RDF instance */
    private final Map<URI, Object> connectedRDFEntityDictionary;                // NOPMD
    /** the predicate values dictionary, predicate --> RDF values */
    private final Map<URI, List<Value>> predicateValuesDictionary;

    /** Creates a new RDFEntityInfo instance.
     *
     * @param connectedRDFEntityDictionary the dictionary of connected RDF entities, URI --> RDF instance
     * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
     */
    protected RDFEntityInfo(
            final Map<URI, Object> connectedRDFEntityDictionary,
            final Map<URI, List<Value>> predicateValuesDictionary) {
      //Preconditions
      assert connectedRDFEntityDictionary != null : "connectedRDFEntityDictionary must not be null";

      this.connectedRDFEntityDictionary = connectedRDFEntityDictionary;
      this.predicateValuesDictionary = predicateValuesDictionary;
    }
  }
}
