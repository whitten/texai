/*
 * AbstractRDFEntityAccessor.java
 *
 * Created on November 1, 2006, 9:36 AM
 *
 * Description: Contains the common RDF entity annotation gathering methods for use in the
 * subclasses for loading and persistence.
 *
 * Copyright (C) 2006 Stephen L. Reed.
 */
package org.texai.kb.persistence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.kb.Constants;
import org.texai.util.TexaiException;

/**
 * This abstract class contains the state and behavior that is common to the concrete subclasses RDFEntityLoader, RDFEntityPersister and
 * RDFEntityRemover. It is responsible for parsing the semantic annotations from the entity in preparation for persist() or find() methods.
 *
 *
 * It features a stack to contain state information during recursive method calls, avoiding the need to construct new instances.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractRDFEntityAccessor {          // NOPMD

  /**
   * the predicate used to persist a map entry key
   */
  protected static final URI PERSISTENT_MAP_ENTRY_KEY_URI = new URIImpl(Constants.TERM_PERSISTENT_MAP_ENTRY_KEY);
  /**
   * the predicate used to persist a map entry value
   */
  protected static final URI PERSISTENT_MAP_ENTRY_VALUE_URI = new URIImpl(Constants.TERM_PERSISTENT_MAP_ENTRY_VALUE);
  /**
   * the class of the RDF entity
   */
  private Class<?> rdfEntityClass;
  /**
   * the instantiated RDF entity
   */
  private RDFPersistent rdfEntity;
  /**
   * the array of type level annotations
   */
  private Annotation[] typeLevelAnnotations;
  /**
   * the dictionary of field level annotations, field -> RDFProperty annotation
   */
  private Map<Field, Annotation> fieldAnnotationDictionary;
  /**
   * the id field
   */
  private Field idField;
  /**
   * the namespace dictionary, prefix --> namespace URI
   */
  private Map<String, String> namespaceDictionary;
  /**
   * the super classes of this RDF entity
   */
  private URI[] subClassOfURIs;
  /**
   * the typeURIs of this RDF entity
   */
  private URI[] typeURIs;
  /**
   * the class URI
   */
  private URI classURI;
  /**
   * the stack of abstract RDF entity information that allows recursive method calls
   */
  private final Stack<Object> rdfEntityInfoStack = new Stack<>();
  /**
   * the persistence context URI
   */
  private URI contextURI;
  /**
   * the override persistence context URI
   */
  private URI overrideContextURI = null;
  /**
   * the effective persistence context URI
   */
  private URI effectiveContextURI;
  /**
   * the instance URI
   */
  private URI instanceURI;
  /**
   * the value factory
   */
  private final ValueFactory valueFactory = new ValueFactoryImpl();
  /**
   * the dictionary of memoized class annotation information, class --> ClassAnnotationInfo
   */
  private final Map<Class<?>, ClassAnnotationInfo> memoizedClassAnnotationInfos = new HashMap<>();

  /**
   * Creates a new instance of AbstractRDFEntityAccessor.
   */
  protected AbstractRDFEntityAccessor() {
    super();
    fieldAnnotationDictionary = new HashMap<>();
    namespaceDictionary = new HashMap<>();
  }

  /**
   * Returns true if the given object is an annotated RDF entity.
   *
   * @param obj the given object
   *
   * @return true if the given object is an annotated RDF entity
   */
  public final boolean isRDFEntity(final Object obj) {
    //Preconditions
    assert obj != null : "obj must not be null";

    return isRDFEntityClass(obj.getClass());
  }

  /**
   * Returns true if the given object class is an annotated RDF entity class.
   *
   * @param clazz the given object class
   *
   * @return true if the given object is an annotated RDF entity class
   */
  @SuppressWarnings("unchecked")
  public final boolean isRDFEntityClass(final Class<?> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    for (final Annotation annotation : clazz.getAnnotations()) {
      // ignore whether classes have differnt classloaders
      if (annotation.annotationType().getName().equals(RDFEntity.class.getName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gathers annotations for the RDF entity class.
   */
  protected final void gatherAnnotationsForRDFEntityClass() {
    //Preconditions
    assert rdfEntityClass != null : "rdfEntityClass must not be null";    // NOPMD

    final ClassAnnotationInfo classAnnotationInfo = memoizedClassAnnotationInfos.get(rdfEntityClass);
    if (classAnnotationInfo == null) {
      typeLevelAnnotations = rdfEntityClass.getAnnotations();
      for (final Annotation annotation : typeLevelAnnotations) {
        if (getLogger().isDebugEnabled()) {
          getLogger().debug(stackLevel() + "type level annotation: " + annotation.toString());
        }
      }
      gatherFieldAnnotationDictionary(rdfEntityClass);
      if (idField == null) {
        throw new TexaiException("cannot find @Id field in class " + rdfEntityClass.getName() + " or in its superclasses");
      }

      memoizedClassAnnotationInfos.put(rdfEntityClass, new ClassAnnotationInfo(
              typeLevelAnnotations,
              fieldAnnotationDictionary,
              namespaceDictionary,
              idField));
    } else {
      typeLevelAnnotations = classAnnotationInfo.typeLevelAnnotations;
      fieldAnnotationDictionary = classAnnotationInfo.fieldAnnotationDictionary;
      idField = classAnnotationInfo.idField;
      namespaceDictionary = classAnnotationInfo.namespaceDictionary;
    }
  }

  /**
   * Processes the type level annotations to configure the RDF entity settings
   */
  protected final void configureRDFEntitySettings() {
    //Preconditions
    assert typeLevelAnnotations != null : "typeLevelAnnotations must not be null";
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    subClassOfURIs = new URI[0];
    for (final Annotation annotation : typeLevelAnnotations) {
      if (annotation instanceof RDFEntity) {
        final RDFEntity rdfEntity1 = (RDFEntity) annotation;
        for (final RDFNamespace rdfNamespace : rdfEntity1.namespaces()) {
          namespaceDictionary.put(rdfNamespace.prefix(), rdfNamespace.namespaceURI());
        }
        // ensure that the reserved namespace definitions are present
        namespaceDictionary.put("rdf", Constants.RDF_NAMESPACE);
        namespaceDictionary.put("rdfs", Constants.RDFS_NAMESPACE);
        namespaceDictionary.put("owl", Constants.OWL_NAMESPACE);
        namespaceDictionary.put("cyc", Constants.CYC_NAMESPACE);
        namespaceDictionary.put("texai", Constants.TEXAI_NAMESPACE);
        if (rdfEntity1.context().isEmpty()) {
          throw new TexaiException("context annotation property is missing");
        } else {
          contextURI = makeURI(rdfEntity1.context());
          if (overrideContextURI == null) {
            effectiveContextURI = contextURI;
          } else {
            effectiveContextURI = overrideContextURI;
          }
        }
        if (rdfEntity1.subject().isEmpty()) {
          classURI = makeURI(getRDFEntityClass().getName());
        } else {
          classURI = makeURI(rdfEntity1.subject());
          if (getLogger().isDebugEnabled()) {
            getLogger().debug(stackLevel() + "class URI: " + classURI.toString());
          }
        }
        final int subClassOfs_len = rdfEntity1.subClassOf().length;
        subClassOfURIs = new URI[subClassOfs_len];
        for (int i = 0; i < subClassOfs_len; i++) {
          subClassOfURIs[i] = makeURI(rdfEntity1.subClassOf()[i]);
        }
        final int types_len = rdfEntity1.type().length;
        typeURIs = new URI[types_len];
        for (int i = 0; i < types_len; i++) {
          typeURIs[i] = makeURI(rdfEntity1.type()[i]);
        }
        break;
      }
    }
    if (getLogger().isDebugEnabled()) {
      getLogger().debug(stackLevel() + "default context: " + contextURI.toString());
      if (overrideContextURI != null) {
        getLogger().debug(stackLevel() + "override context: " + overrideContextURI.toString());
      }
    }
    for (final URI subClassOf : subClassOfURIs) {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug(stackLevel() + "subClassOf: " + subClassOf.toString());
      }
    }

    //Postconditions
    assert effectiveContextURI != null : "effectiveContextURI must not be null for class " + rdfEntityClass + ", instanceURI " + getInstanceURI();
    if (classURI == null) {
      throw new TexaiException(rdfEntityClass + " effectiven context URI cannot be determined");
    }
  }

  /**
   * Gets the class URI from its RDF type level annotations.
   *
   * @param clazz the class
   *
   * @return the class URI from its RDF type level annotations
   */
  protected final URI getClassURI(final Class<?> clazz) {
    //Preconditions
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    final Annotation[] typeLevelAnnotations1 = rdfEntityClass.getAnnotations();
    for (final Annotation annotation : typeLevelAnnotations1) {
      if (getLogger().isDebugEnabled()) {
        getLogger().debug(stackLevel() + "type level annotation: " + annotation.toString());
      }
    }

    for (final Annotation annotation : typeLevelAnnotations1) {
      if (annotation instanceof RDFEntity) {
        final RDFEntity rdfEntity1 = (RDFEntity) annotation;
        URI classURI1;
        if (rdfEntity1.subject().isEmpty()) {
          classURI1 = makeURI(rdfEntityClass.getName());
        } else {
          classURI1 = makeURI(rdfEntity1.subject());
        }
        if (getLogger().isDebugEnabled()) {
          getLogger().debug(stackLevel() + "class URI: " + classURI1.toString());
        }
        break;
      }
    }

    //Postconditions
    if (classURI == null) {
      throw new TexaiException(clazz.getName() + " is not an RDF entity");
    }

    return classURI;
  }

  /**
   * Gathers the field level annotations for the given class and its superclasses, creating the dictionary, field -> RDFProperty annotation.
   * Also locates the id field.
   *
   * @param clazz the given class
   */
  private void gatherFieldAnnotationDictionary(final Class<?> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    if (!clazz.getName().equals(Object.class.getName()) && clazz.getSuperclass() != null) {
      // work down the class hierarchy
      gatherFieldAnnotationDictionary(clazz.getSuperclass());
    }
    if (getLogger().isDebugEnabled()) {
      getLogger().debug(stackLevel() + "gathering field annotations for class: " + clazz.getName());
    }
    final Field[] fields = clazz.getDeclaredFields();
    for (final Field field : fields) {
      if (getAnnotation(field, Id.class) == null) {
        final Annotation annotation = getAnnotation(field, RDFProperty.class);
        if (annotation != null) {
          if (getLogger().isDebugEnabled()) {
            getLogger().debug(stackLevel() + "    field: " + field.getName() + " annotation: " + annotation);
          }
          fieldAnnotationDictionary.put(field, annotation);
        }
      } else {
        if (getLogger().isDebugEnabled()) {
          getLogger().debug(stackLevel() + "    id field: " + field.getName());
        }
        idField = field;
      }
    }
  }

  /**
   * Returns the given annotation from the given field, or null if not found. This implementation ignores whether the annotation class and
   * the given class have different classloaders as might happen in the OSGi framework.
   *
   * @param field the given field
   * @param clazz the given annotation class
   *
   * @return the given annotation from the given field, or null if not found
   */
  private Annotation getAnnotation(final Field field, final Class<?> clazz) {
    //Preconditions
    assert field != null : "field must not be null";
    assert clazz != null : "clazz must not be null";

    final String className = clazz.getName();
    for (final Annotation annotation : field.getDeclaredAnnotations()) {
      if (annotation.annotationType().getName().equals(className)) {
        return annotation;
      }
    }
    return null;
  }

  /**
   * Returns a URI formed from the given name, prepending a namespace if required.
   *
   * @param name the given name, which may include a namespace prefix
   *
   * @return a URI formed from the given name, prepending a namespace if required
   */
  protected final URI makeURI(final String name) {
    //Preconditions
    assert name != null : "name must not be null";
    assert !name.isEmpty() : "name must not be empty";

    URI uri;
    if (name.indexOf('/') > -1 || name.indexOf('#') > -1) {
      uri = getValueFactory().createURI(name);
    } else {
      final int index = name.indexOf(':');
      final String prefix;
      if (index == -1) {
        prefix = "texai";
      } else {
        prefix = name.substring(0, index);
      }
      final String namespaceURI = namespaceDictionary.get(prefix);
      if (namespaceURI == null) {
        throw new TexaiException(name + " is a malformed URI, cannot find a namespace for the prefix " + prefix
                + "\nin: " + namespaceDictionary);
      }
      final String unprefixedName = name.substring(index + 1);
      if (unprefixedName.isEmpty()) {
        throw new TexaiException(name + " is a malformed URI, cannot parse prefixed name");
      }
      uri = getValueFactory().createURI(namespaceURI, unprefixedName);
    }

    //Postconditions
    assert uri != null : "uri must not be null";

    return uri;
  }

  /**
   * Gets a default property URI for the annotated field.
   *
   * @param field the annotated field
   * @param rdfProperty the property annotation
   *
   * @return a default property URI
   */
  protected URI getEffectivePropertyURI(final Field field, final RDFProperty rdfProperty) {
    //Preconditions
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";

    if (!rdfProperty.predicate().isEmpty()) {
      return makeURI(rdfProperty.predicate());
    }

    return RDFUtility.getDefaultPropertyURI(
            field.getDeclaringClass().getName(), // className
            field.getName(), // fieldName
            field.getType()); // fieldType
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  abstract Logger getLogger();

  /**
   * Gets the value factory.
   *
   * @return the value factory
   */
  public final ValueFactory getValueFactory() {
    return valueFactory;
  }

  /**
   * Gets the RDF entity class.
   *
   * @return the RDF entity class
   */
  public final Class<?> getRDFEntityClass() {
    return rdfEntityClass;
  }

  /**
   * Sets the RDF entity class.
   *
   * @param rdfEntityClass the RDF entity class
   */
  public final void setRDFEntityClass(final Class<?> rdfEntityClass) {
    //Preconditions
    assert rdfEntityClass != null : "rdfEntityClass must not be null";

    this.rdfEntityClass = rdfEntityClass;
  }

  /**
   * Gets the instantiated RDF entity.
   *
   * @return the instantiated RDF entity
   */
  public final RDFPersistent getRDFEntity() {
    return rdfEntity;
  }

  /**
   * Sets the instantiated RDF entity.
   *
   * @param rdfEntity the instantiated RDF entity
   */
  public final void setRDFEntity(final RDFPersistent rdfEntity) {
    this.rdfEntity = rdfEntity;
  }

  /**
   * Gets the array of type level annotations.
   *
   * @return the array of type level annotations
   */
  protected final Annotation[] getTypeLevelAnnotations() {
    return typeLevelAnnotations;                           // NOPMD
  }

  /**
   * Gets the dictionary of field level annotations.
   *
   * @return the dictionary of field level annotations, field -> Id or RDFProperty annotation
   */
  public final Map<Field, Annotation> getFieldAnnotationDictionary() {
    return fieldAnnotationDictionary;
  }

  /**
   * Gets the super classes of this RDF entity.
   *
   * @return the super classes of this RDF entity
   */
  protected final URI[] getSubClassOfURIs() {
    return subClassOfURIs;                                    // NOPMD
  }

  /**
   * Sets the super classes of this RDF entity.
   *
   * @param subClassOfURIs the super classes of this RDF entity
   */
  protected final void setSubClassOfURIs(final URI[] subClassOfURIs) {
    this.subClassOfURIs = subClassOfURIs;
  }

  /**
   * Gets the type URIs of this RDF entity.
   *
   * @return the type URIS of this RDF entity
   */
  protected final URI[] getTypeURIs() {
    return typeURIs;                                    // NOPMD
  }

  /**
   * Sets the typeURIs of this RDF entity.
   *
   *
   * @param typeURIs the typeURIs of this RDF entity
   */
  protected final void setTypeURIs(final URI[] typeURIs) {
    this.typeURIs = typeURIs;
  }

  /**
   * Gets the class URI.
   *
   * @return the class URI for this RDF entity
   */
  public final URI getClassURI() {
    return classURI;
  }

  /**
   * Sets the class URI.
   *
   * @param classURI the class URI for this RDF entity
   */
  public final void setClassURI(final URI classURI) {
    //Preconditions
    assert classURI != null : "classURI must not be null";

    this.classURI = classURI;
  }

  /**
   * Gets the context URI.
   *
   *
   * @return the context URI
   */
  public final URI getContextURI() {
    return contextURI;
  }

  /**
   * Sets the contex URI.
   *
   *
   * @param contextURI the context URI
   */
  public final void setContextURI(final URI contextURI) {
    this.contextURI = contextURI;
  }

  /**
   * Gets the instance URI.
   *
   * @return the instance URI
   */
  public final URI getInstanceURI() {
    return instanceURI;
  }

  /**
   * Sets the instance URI.
   *
   * @param instanceURI the instance URI
   */
  public final void setInstanceURI(final URI instanceURI) {
    this.instanceURI = instanceURI;
  }

  /**
   * Gets the id field.
   *
   * @return the id field
   */
  public final Field getIdField() {
    return idField;
  }

  /**
   * Returns the stack level for logging.
   *
   * @return the stack level for logging.
   */
  public final String stackLevel() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    return "[" + rdfEntityInfoStack.size() + "] ";
  }

  /**
   * Pushes the current session bean state onto a stack and then intitializes the session state.
   */
  protected final void saveAbstractSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    getLogger().debug(stackLevel() + "saving abstract session state");
    rdfEntityInfoStack.push(new AbstractRDFEntityInfo(
            rdfEntityClass,
            rdfEntity,
            typeLevelAnnotations,
            fieldAnnotationDictionary,
            namespaceDictionary,
            contextURI,
            overrideContextURI,
            effectiveContextURI,
            subClassOfURIs,
            typeURIs,
            classURI,
            instanceURI,
            idField));
  }

  /**
   * Initializes the session bean state.
   */
  protected final void initializeAbstractSessionState() {
    rdfEntityClass = null;
    rdfEntity = null;
    typeLevelAnnotations = null;
    fieldAnnotationDictionary = new HashMap<>();
    namespaceDictionary = new HashMap<>();
    subClassOfURIs = null;
    typeURIs = null;
    classURI = null;
    overrideContextURI = null;
    contextURI = null;
    effectiveContextURI = null;
    instanceURI = null;
    idField = null;
  }

  /**
   * Restores the session state following a recursive method call.
   */
  protected final void restoreAbstractSessionState() {
    //Preconditions
    assert rdfEntityInfoStack != null : "rdfEntityInfoStack must not be null";

    final AbstractRDFEntityInfo abstractRDFEntityInfo = (AbstractRDFEntityInfo) rdfEntityInfoStack.pop();
    rdfEntityClass = abstractRDFEntityInfo.rdfEntityClass;
    rdfEntity = abstractRDFEntityInfo.rdfEntity;
    typeLevelAnnotations = abstractRDFEntityInfo.typeLevelAnnotations;
    fieldAnnotationDictionary = abstractRDFEntityInfo.fieldAnnotationDictionary;
    namespaceDictionary = abstractRDFEntityInfo.namespaceDictionary;
    subClassOfURIs = abstractRDFEntityInfo.subClassOfURIs;
    typeURIs = abstractRDFEntityInfo.typeURIs;
    classURI = abstractRDFEntityInfo.classURI;
    contextURI = abstractRDFEntityInfo.contextURI;
    overrideContextURI = abstractRDFEntityInfo.overrideContextURI;
    effectiveContextURI = abstractRDFEntityInfo.effectiveContextURI;
    instanceURI = abstractRDFEntityInfo.instanceURI;
    idField = abstractRDFEntityInfo.idField;
    if (getLogger().isDebugEnabled()) {
      getLogger().debug(stackLevel() + "restored abstract session state");
    }
  }

  /**
   * Gets the override persistence context.
   *
   * @return the override persistence context
   */
  public final URI getOverrideContextURI() {
    return overrideContextURI;
  }

  /**
   * Sets the override persistence context.
   *
   * @param overrideContextURI the override persistence context
   */
  public final void setOverrideContextURI(final URI overrideContextURI) {
    this.overrideContextURI = overrideContextURI;
  }

  /**
   * Gets the effective persistence context URI.
   *
   * @return the effective persistence context URI
   */
  public final URI getEffectiveContextURI() {
    return effectiveContextURI;
  }

  /**
   * Sets the effective persistence context URI.
   *
   * @param effectiveContextURI the effective persistence context URI
   */
  public final void setEffectiveContextURI(final URI effectiveContextURI) {
    //Preconditions
    assert effectiveContextURI != null : "effectiveContextURI must not be null";

    this.effectiveContextURI = effectiveContextURI;
  }

  /**
   * Contains the session bean state for recursive method calls.
   */
  private static class AbstractRDFEntityInfo {             // NOPMD

    /**
     * the RDF entity class
     */
    private final Class<?> rdfEntityClass;                    // NOPMD
    /**
     * the instantiated RDF entity
     */
    private final RDFPersistent rdfEntity;                        // NOPMD
    /**
     * the array of type level annotations
     */
    private final Annotation[] typeLevelAnnotations;       // NOPMD
    /**
     * the dictionary of field level annotations, field -> Id or RDFProperty annotation
     */
    private final Map<Field, Annotation> fieldAnnotationDictionary;    // NOPMD
    /**
     * the namespace dictionary, prefix --> namespace URI
     */
    private final Map<String, String> namespaceDictionary; // NOPMD
    /**
     * the contextURI
     */
    private final URI contextURI;                          // NOPMD
    /**
     * the override persistence context URI
     */
    private final URI overrideContextURI;
    /**
     * the effective persistence context URI
     */
    private final URI effectiveContextURI;
    /**
     * the RDF super classes of this RDF entity
     */
    private final URI[] subClassOfURIs;                    // NOPMD
    /**
     * the typeURIs of this RDF entity
     */
    private final URI[] typeURIs;                          // NOPMD
    /**
     * the class URI
     */
    private final URI classURI;                            // NOPMD
    /**
     * the instance URI
     */
    private final URI instanceURI;                         // NOPMD
    /**
     * the id field
     */
    private final Field idField;                           // NOPMD

    /**
     * Creates a new AbstractRDFEntityInfo instance.
     *
     * @param rdfEntityClass the RDF entity class
     * @param rdfEntity the instantiated RDF entity
     * @param typeLevelAnnotations the array of type level annotations
     * @param fieldAnnotationDictionary the dictionary of field level annotations, field -> Id or RDFProperty annotation
     * @param namespaceDictionary the namespace dictionary, prefix --> namespace URI
     * @param contextURI the context
     * @param overrideContextURI the override context
     * @param effectiveContextURI the effective context
     * @param subClassOfURIs the super classes of this RDF entity
     * @param typeURIs the types of this RDF entity
     * @param classURI the class URI
     * @param instanceURI the instance URI
     * @param idField the id field
     */
    protected AbstractRDFEntityInfo(
            final Class<?> rdfEntityClass,
            final RDFPersistent rdfEntity,
            final Annotation[] typeLevelAnnotations,
            final Map<Field, Annotation> fieldAnnotationDictionary,
            final Map<String, String> namespaceDictionary,
            final URI contextURI,
            final URI overrideContextURI,
            final URI effectiveContextURI,
            final URI[] subClassOfURIs,
            final URI[] typeURIs,
            final URI classURI,
            final URI instanceURI,
            final Field idField) {
      super();

      this.rdfEntityClass = rdfEntityClass;
      this.rdfEntity = rdfEntity;
      this.typeLevelAnnotations = typeLevelAnnotations;
      this.fieldAnnotationDictionary = fieldAnnotationDictionary;
      this.namespaceDictionary = namespaceDictionary;
      this.contextURI = contextURI;
      this.overrideContextURI = overrideContextURI;
      this.effectiveContextURI = effectiveContextURI;
      this.typeURIs = typeURIs;
      this.subClassOfURIs = subClassOfURIs;
      this.classURI = classURI;
      this.instanceURI = instanceURI;
      this.idField = idField;
    }
  }

  /**
   * Contains the memoized class annotation information.
   */
  private static class ClassAnnotationInfo {

    /**
     * the array of type level annotations
     */
    private final Annotation[] typeLevelAnnotations;        // NOPMD
    /**
     * the dictionary of field level annotations, field -> Id or RDFProperty annotation
     */
    private final Map<Field, Annotation> fieldAnnotationDictionary;       // NOPMD
    /**
     * the namespace dictionary, prefix --> namespace URI
     */
    private final Map<String, String> namespaceDictionary; // NOPMD
    /**
     * the id field
     */
    private final Field idField;                           // NOPMD

    /**
     * Constructs a new ClassAnnotationInfo instance.
     *
     * @param typeLevelAnnotations the array of type level annotation
     * @param fieldAnnotationDictionary the dictionary of field level annotations, field -> Id or RDFProperty annotation
     * @param namespaceDictionary the namespace dictionary, prefix --> namespace URI
     * @param idField the id field
     */
    protected ClassAnnotationInfo(
            final Annotation[] typeLevelAnnotations,
            final Map<Field, Annotation> fieldAnnotationDictionary,
            final Map<String, String> namespaceDictionary,
            final Field idField) {
      //preconditions
      assert typeLevelAnnotations != null : "typeLevelAnnotations must not be null";
      assert fieldAnnotationDictionary != null : "fieldAnnotationDictionary must not be null";
      assert idField != null : "idField must not be null";

      this.typeLevelAnnotations = typeLevelAnnotations;
      this.fieldAnnotationDictionary = fieldAnnotationDictionary;
      this.namespaceDictionary = namespaceDictionary;
      this.idField = idField;
    }
  }

  /**
   * Adds the given list of RDF values to the repository as an RDF list structure.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntityManager the entity manager
   * @param valueList the given list of RDF values
   * @param writer the export output writer, or null when objects are ordinarily persisted to the given RDF quad store
   *
   * @return the blank node that heads the RDF list structure
   */
  public final BNode addRDFList(
          final RepositoryConnection repositoryConnection,
          final RDFEntityManager rdfEntityManager,
          final List<Value> valueList,
          final BufferedWriter writer) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert valueList != null : "valueList must not be null";
    assert !valueList.isEmpty() : "valueList must not be empty";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    BNode element = valueFactory.createBNode();
    final BNode rdfListHead = element;
    Statement statement;
    final int sizeLessOne = valueList.size() - 1;
    int index = 0;
    for (; index < sizeLessOne; index++) {
      // link all but the last value onto the RDF list
      final Value value = valueList.get(index);
      statement = valueFactory.createStatement(
              element,
              RDF.FIRST,
              value,
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
      getLogger().info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
      final BNode nextElement = valueFactory.createBNode();
      statement = valueFactory.createStatement(
              element,
              RDF.REST,
              nextElement,
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
      getLogger().info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
      element = nextElement;
    }
    // the final RDF list value is linked to nil
    final Value value = valueList.get(index);
    statement = valueFactory.createStatement(
            element,
            RDF.FIRST,
            value,
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
    getLogger().info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());
    statement = valueFactory.createStatement(
            element,
            RDF.REST,
            RDF.NIL,
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
    getLogger().info("added: " + RDFUtility.formatStatement(statement) + " to " + repositoryConnection.getRepository().getDataDir().getName());

    return rdfListHead;
  }

  /**
   * Returns the list of RDF values linked from the given RDF list head.
   *
   * @param repositoryConnection the repository connection
   * @param rdfListHead the blank node that heads the RDF list
   *
   * @return the list of RDF values linked from the given RDF list head
   */
  public final List<Value> getRDFListValues(
          final RepositoryConnection repositoryConnection,
          final BNode rdfListHead) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfListHead != null : "rdfListHead must not be null";
    assert effectiveContextURI != null : "effectiveContextURI must not be null";

    final List<Value> rdfValueList = new ArrayList<>();
    getLogger().debug(stackLevel() + "    getting existing RDF list values, head: " + rdfListHead);
    Value[] firstAndRest = getRDFListComponents(repositoryConnection, (Resource) rdfListHead);
    rdfValueList.add(firstAndRest[0]);
    Value rest = firstAndRest[1];
    while (!rest.equals(RDF.NIL)) {
      assert rest instanceof Resource;
      firstAndRest = getRDFListComponents(repositoryConnection, (Resource) rest);
      rdfValueList.add(firstAndRest[0]);
      rest = firstAndRest[1];
    }

    //Postconditions
    assert !rdfValueList.isEmpty() : "rdfValueList must not be empty at " + rdfListHead;

    return rdfValueList;
  }

// Disabled because the lazy initialized query fails to find some valid RDF statements at a certain point.
//  /** Returns the URI array [first, rest] whose elements are the first element of the given list node, and the rest of the list at the
//   * given node.
//   *
//   * @param repositoryConnection the repository connection
//   * @param listNode the blank node that heads the RDF list
//   * @return the URI array [first, rest] whose elements are the first element of the given list node, and the rest of the list at the
//   * given node
//   */
//  private Value[] getRDFListComponents(
//          final RepositoryConnection repositoryConnection,
//          final Resource listNode) {
//    //Preconditions
//    assert listNode != null : "listNode must not be null";
//    assert effectiveContextURI != null : "effectiveContextURI must not be null";
//
//    final Value[] firstAndRest = {null, null};
//    if (rdfListElementsQuery == null) {
//      // lazy initialization
//      try {
//        rdfListElementsQuery = repositoryConnection.prepareTupleQuery(
//                QueryLanguage.SERQL,
//                "SELECT f, r FROM {s} <http://www.w3.org/1999/02/22-rdf-syntax-ns#first> {f}; <http://www.w3.org/1999/02/22-rdf-syntax-ns#rest> {r}");
//      } catch (final MalformedQueryException ex) {
//        throw new TexaiException(ex);
//      } catch (final RepositoryException ex) {
//        throw new TexaiException(ex);
//      }
//    }
//    rdfListElementsQuery.setBinding("s", listNode);
//    final TupleQueryResult tupleQueryResult;
//    try {
//      tupleQueryResult = rdfListElementsQuery.evaluate();
//      if (tupleQueryResult.hasNext()) {
//        final BindingSet bindingSet = tupleQueryResult.next();
//        final Binding firstBinding = bindingSet.getBinding("f");
//        if (firstBinding == null) {
//          throw new TexaiException("missing RDf first list binding at listNode: " + listNode);
//        } else {
//          firstAndRest[0] = firstBinding.getValue();
//        }
//        final Binding restBinding = bindingSet.getBinding("r");
//        if (restBinding == null) {
//          throw new TexaiException("missing RDf rest list binding at listNode: " + listNode);
//        } else {
//          firstAndRest[1] = restBinding.getValue();
//        }
//
//      } else {
//        throw new TexaiException("invalid RDF list at " + listNode);
//      }
//      tupleQueryResult.close();
//      if (firstAndRest[0] == null || firstAndRest[1] == null) {
//        throw new TexaiException("missing RDF list components at listNode: " + listNode);
//      }
//    } catch (final Throwable ex) {
//      getLogger().error("repositoryConnection: " + repositoryConnection);
//      getLogger().error("repository: " + repositoryConnection.getRepository().getDataDir());
//      getLogger().error("statements having " + listNode + " as subject...");
//      try {
//        final RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(listNode, null, null, false);
//        while (repositoryResult.hasNext()) {
//          getLogger().error("statement: " + RDFUtility.formatStatement(repositoryResult.next()));
//        }
//      } catch (final RepositoryException ex1) {
//      // ignore
//      }
//      throw new TexaiException(ex);
//    }
//
//    //Postconditions
//    assert firstAndRest.length == 2 : "firstAndRest must have length 2";
//    assert firstAndRest[0] != null : "first must not be null";
//    assert firstAndRest[1] != null : "rest must not be null";
//
//    return firstAndRest;
//  }
  /**
   * Returns the URI array [first, rest] whose elements are the first element of the given list node, and the rest of the list at the given
   * node.
   *
   * @param repositoryConnection the repository connection
   * @param listNode the blank node that heads the RDF list
   *
   * @return the URI array [first, rest] whose elements are the first element of the given list node, and the rest of the list at the given
   * node
   */
  private Value[] getRDFListComponents(
          final RepositoryConnection repositoryConnection,
          final Resource listNode) {
    //Preconditions
    assert listNode != null : "listNode must not be null";
    assert effectiveContextURI != null : "effectiveContextURI must not be null";

    final Value[] firstAndRest = {null, null};

    try {
      RepositoryResult<Statement> repositoryResult = repositoryConnection.getStatements(
              listNode,
              RDF.FIRST,
              null,
              false);
      if (repositoryResult.hasNext()) {
        firstAndRest[0] = repositoryResult.next().getObject();
      }
      repositoryResult.close();
      repositoryResult = repositoryConnection.getStatements(
              listNode,
              RDF.REST,
              null,
              false);
      if (repositoryResult.hasNext()) {
        firstAndRest[1] = repositoryResult.next().getObject();
      }
      repositoryResult.close();
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
    if (firstAndRest[0] == null || firstAndRest[1] == null) {
      throw new TexaiException("missing RDF list components at listNode: " + listNode);
    }

    //Postconditions
    assert firstAndRest.length == 2 : "firstAndRest must have length 2";
    assert firstAndRest[0] != null : "first must not be null";
    assert firstAndRest[1] != null : "rest must not be null";

    return firstAndRest;
  }

  /**
   * Removes the given RDF list.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntityManager the entity manager
   * @param listNode the blank node that heads the RDF list
   */
  protected final void removeRDFList(
          final RepositoryConnection repositoryConnection,
          final RDFEntityManager rdfEntityManager,
          final BNode listNode) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntityManager != null : "rdfEntityManager must not be null";
    assert listNode != null : "listNode must not be null";

    final Value first = RDFUtility.getObjectGivenSubjectAndPredicate(
            listNode, RDF.FIRST, effectiveContextURI, repositoryConnection);
    if (first != null) { // expected statement may be missing if the value was previously removed, e.g. via cascadeRemove
      final Statement statement = valueFactory.createStatement(
              listNode,
              RDF.FIRST,
              first,
              effectiveContextURI);
      rdfEntityManager.removeStatement(repositoryConnection, statement);
      getLogger().info("removed: " + RDFUtility.formatStatement(statement));
    }

    final Value rest = RDFUtility.getObjectGivenSubjectAndPredicate(
            listNode, RDF.REST, effectiveContextURI, repositoryConnection);
    final Statement statement = valueFactory.createStatement(
            listNode,
            RDF.REST,
            rest,
            effectiveContextURI);
    rdfEntityManager.removeStatement(repositoryConnection, statement);
    getLogger().info("removed: " + RDFUtility.formatStatement(statement));
    if (rest.equals(RDF.NIL)) {
      return;
    }
    removeRDFList(repositoryConnection, rdfEntityManager, (BNode) rest);
  }

  /**
   * Provides a container for a map entry.
   */
  protected static class MapEntry {

    /**
     * the map entry key Java object
     */
    protected Object key;
    /**
     * the map entry key RDF value
     */
    protected Value keyRDFValue;
    /**
     * the map entry value Java object
     */
    protected Object value;
    /**
     * the map entry value RDF value
     */
    protected Value valueRDFValue;
    /**
     * the blank node
     */
    protected BNode bNode;

    /**
     * Constructs a new MapEntry object.
     *
     * @param bNode the blank node
     * @param keyRDFValue the map entry key RDF value
     * @param valueRDFValue the map entry value RDF value
     */
    MapEntry(
            final BNode bNode,
            final Value keyRDFValue,
            final Value valueRDFValue) {
      //Preconditions
      assert bNode != null : "bNode must not be null";
      assert keyRDFValue != null : "keyRDFValue must not be null";
      assert valueRDFValue != null : "valueRDFValue must not be null";

      this.bNode = bNode;
      this.keyRDFValue = keyRDFValue;
      this.valueRDFValue = valueRDFValue;
    }

    /**
     * Constructs a new MapEntry object.
     *
     * @param key the map entry key
     * @param value the map entry value
     */
    MapEntry(final Object key, final Object value) {
      //Preconditions
      assert key != null : "key must not be null";
      assert value != null : "value must not be null";

      this.key = key;
      this.value = value;
    }

    /**
     * Makes a map entry given the blank node that persisted it.
     *
     * @param bNode the blank node that persisted the map entry
     * @param repositoryConnection the repository connection
     * @param effectiveContextURI the effective context term
     *
     * @return the map entry
     */
    static MapEntry makeMapEntry(
            final BNode bNode,
            final RepositoryConnection repositoryConnection,
            final URI effectiveContextURI) {
      //Preconditions
      assert bNode != null : "bNode must not be null";
      assert repositoryConnection != null : "repositoryConnection must not be null";

      // get the persisted map entry key RDF value
      final Value keyRDFValue = RDFUtility.getObjectGivenSubjectAndPredicate(
              bNode, // subject
              PERSISTENT_MAP_ENTRY_KEY_URI, // predicate
              effectiveContextURI, // context
              repositoryConnection);
      assert keyRDFValue != null : "\nsubject: " + bNode + "\npredicate: " + PERSISTENT_MAP_ENTRY_KEY_URI + "\ncontext: " + effectiveContextURI;

      // get the persisted map-entry-value RDF-value
      final Value valueRDFValue = RDFUtility.getObjectGivenSubjectAndPredicate(
              bNode, // subject
              PERSISTENT_MAP_ENTRY_VALUE_URI, // predicate
              effectiveContextURI, // context
              repositoryConnection);
      assert keyRDFValue != null : "\nsubject: " + bNode + "\npredicate: " + PERSISTENT_MAP_ENTRY_VALUE_URI + "\ncontext: " + effectiveContextURI;

      return new MapEntry(bNode, keyRDFValue, valueRDFValue);
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return key + "-->" + value;
    }

    /**
     * Returns whether some other object equals this one.
     *
     * @param obj the other object
     *
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
      final MapEntry other = (MapEntry) obj;
      if (this.key != other.key && (this.key == null || !this.key.equals(other.key))) {
        return false;
      }
      if (this.value == other.value) {
        return true;
      } else {
        return this.value.equals(other.value);
      }
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 37 * hash + Objects.hashCode(this.key);
      hash = 37 * hash + Objects.hashCode(this.keyRDFValue);
      hash = 37 * hash + Objects.hashCode(this.value);
      hash = 37 * hash + Objects.hashCode(this.valueRDFValue);
      hash = 37 * hash + Objects.hashCode(this.bNode);
      return hash;
    }
  }
}
