/*
 * RDFEntityLazyLoader.java
 *
 * Created on March 2, 2007, 1:30 PM
 *
 * Description: Provides a lazy loader for RDF entities.
 *
 * Copyright (C) 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence.lazy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;
import net.sf.cglib.proxy.LazyLoader;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.RDFEntityLoader;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.util.TexaiException;

/**  Provides a facility that lazily loads an RDF entity field. The RDF entity value is loaded automatically from the RDF store when any of its
 * defined methods are invoked. The method call is delegated to the loaded RDF entity.  Subsequent references to the RDF entity field obtain the
 * loaded RDF entity directly.  Note that because not-yet-loaded lazy RDF entities are not persisted to the RDF store, before they are copied into
 * another persistent field they should first be initialized (loaded) by invoking any of their defined methods.
 *
 * Instances of this class are associated with the RDF entity field by means of a cglib proxy Enhancer.
 *
 * @author reed
 */
@ThreadSafe
public final class RDFEntityLazyLoader implements LazyLoader, Serializable {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RDFEntityLazyLoader.class);
  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the repository name */
  private final String repositoryName;
  /** the RDF instance */
  private final RDFPersistent rdfEntity;
  /** the RDF instance field */
  private transient Field field;
  /** the RDF instance field name */
  private final String fieldName;
  /** the RDF property */
  private final RDFProperty rdfProperty;
  /** the predicate values dictionary, predicate --> RDF values */
  private final Map<URI, List<Value>> predicateValuesDictionary;
  /** the loaded object */
  private Object loadedObject;
  /** the indicator that the lazy object is currently being loaded */
  private boolean isLoading = false;

  /** Creates a new instance of RDFEntityLazyLoader.
   *
   * @param repositoryConnection the repository connection
   * @param rdfInstance RDF instance
   * @param field the RDF instance field
   * @param rdfProperty RDF property
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   */
  public RDFEntityLazyLoader(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfInstance,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    super();
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfInstance != null : "rdfInstance must not be null";
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateValuesDictionary != null : "predicateValuesDictionary must not be null";
    this.predicateValuesDictionary = predicateValuesDictionary;

    repositoryName = repositoryConnection.getRepository().getDataDir().getName();
    this.rdfEntity = rdfInstance;
    this.field = field;
    this.fieldName = field.getName();
    this.rdfProperty = rdfProperty;
  }

  /** Returns the object to which the original method invocation should be dispatched.
   * Also replaces the lazy object with the loaded object on the RDF entity so that subsequent
   * invocations will directly access the object.
   *
   * @return the object to which the original method invocation should be dispatched
   * @throws RepositoryException when a repository error occurs
   */
  @Override
  public synchronized Object loadObject() throws RepositoryException {
    if (!isLoading && loadedObject == null) {
      isLoading = true;
      final RDFEntityLoader rdfEntityLoader = new RDFEntityLoader();
      // obtain a new repository connection from the named repository
      final RepositoryConnection repositoryConnection =
              DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(repositoryName);
      if (field == null) {
        try {
          field = rdfEntity.getClass().getField(fieldName);
        } catch (NoSuchFieldException | SecurityException ex) {
        throw new TexaiException(ex);
        }
      }
      loadedObject = rdfEntityLoader.loadLazyRDFEntityField(
              repositoryConnection,
              rdfEntity,
              field,
              rdfProperty,
              predicateValuesDictionary);
      repositoryConnection.close();
      isLoading = false;
      LOGGER.debug("dynamically loaded " + loadedObject + " for field " + field);
    }
    return loadedObject;
  }
}
