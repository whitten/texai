/*
 * RDFEntityRemover.java
 *
 * Created on August 13, 2007, 1:06 PM
 *
 * Description:  This class removes domain entities from the RDF store.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.texai.util.TexaiException;

/** This helper class removes domain entities from the RDF store.
 *
 * @author reed
 */
@NotThreadSafe
public final class RDFEntityRemover extends AbstractRDFEntityAccessor {

  /** the logger */
  private final Logger logger = Logger.getLogger(RDFEntityRemover.class);                         // NOPMD
  /** the entity manager */
  private final RDFEntityManager rdfEntityManager;

  /**
   * Creates a new instance of RDFEntityRemover.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public RDFEntityRemover(final RDFEntityManager rdfEntityManager) {
    super();
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Remove the statements in the RDF store that mention the URI that represents the given RDF entity.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF entity to be removed
   */
  public void remove(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity) {
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
    assert isRDFEntity(rdfEntity) : rdfEntity + "(" + rdfEntity.getClass().getName() + ")\n must be have a @RDFEntity class level annotation in\n" + Arrays.toString(rdfEntity.getClass().getAnnotations());

    initializeAbstractSessionState();
    if (getLogger().isDebugEnabled()) {
      logger.debug(stackLevel() + "removing " + rdfEntity);
    }
    setRDFEntity(rdfEntity);
    setRDFEntityClass(rdfEntity.getClass());
    gatherAnnotationsForRDFEntityClass();
    configureRDFEntitySettings();
    findInstanceURI();
    try {
      final boolean isAutoCommit = repositoryConnection.isAutoCommit();
      if (isAutoCommit) {
        // perform removal operations within a transaction to avoid the otherwise unsatisfactory performance resulting from auto-commiting each
        // removal operation
        repositoryConnection.setAutoCommit(false);
      }

      // remove the blank nodes that persist list fields and map fields
      for (final Field field : getFieldAnnotationDictionary().keySet()) {
        final Annotation annotation = getFieldAnnotationDictionary().get(field);
        if ("@javax.persistence.Id()".equals(annotation.toString())) {
          continue;
        } else {
          if (annotation instanceof RDFProperty) {
            final Class<?> fieldType = field.getType();
            if (List.class.isAssignableFrom(fieldType) || field.getType().isArray()) {
              // the field is a List or an Array whose values are persisted in an RDF collection, which is a chain of blank nodes
              final BNode listNode = (BNode) RDFUtility.getObjectGivenSubjectAndPredicate(
                      getInstanceURI(), // subject
                      getEffectivePropertyURI(field, (RDFProperty) annotation), // predicate
                      getEffectiveContextURI(), // context
                      repositoryConnection);
              if (listNode != null) {
                logger.info("removing statements in the RDF list for field " + field);
                removeRDFList(
                        repositoryConnection,
                        rdfEntityManager,
                        listNode);
              }
            } else if (Map.class.isAssignableFrom(fieldType)) {
              // the field is a Map whose entry values are persisted as blank nodes,
              // each related to the map entry key and map entry value
              final Set<Value> bNodes = RDFUtility.getObjectsGivenSubjectAndPredicate(
                      getInstanceURI(), // subject
                      getEffectivePropertyURI(field, (RDFProperty) annotation), // predicate
                      getEffectiveContextURI(), // context
                      repositoryConnection);
              for (final Value bNode : bNodes) {
                logger.info("removing statements having map entry subject " + bNode);
                RepositoryResult<Statement> repositoryResult =
                        repositoryConnection.getStatements((BNode) bNode, null, null, false);
                while (repositoryResult.hasNext()) {
                  rdfEntityManager.removeStatement(repositoryConnection, repositoryResult.next());
                }
              }
            }
          }
        }
      }

      logger.info("removing statements having subject " + getInstanceURI());
      RepositoryResult<Statement> repositoryResult =
              repositoryConnection.getStatements(getInstanceURI(), null, null, false);
      while (repositoryResult.hasNext()) {
        rdfEntityManager.removeStatement(repositoryConnection, repositoryResult.next());
      }
      logger.info("removing statements having object " + getInstanceURI());
      repositoryResult =
              repositoryConnection.getStatements(null, null, getInstanceURI(), false);
      while (repositoryResult.hasNext()) {
        rdfEntityManager.removeStatement(repositoryConnection, repositoryResult.next());
      }
      if (isAutoCommit) {
        repositoryConnection.commit();
        repositoryConnection.setAutoCommit(true);
      }
    } catch (final RepositoryException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Finds the domain instance URI. */
  private void findInstanceURI() {
    //Preconditions
    assert getClassURI() != null : "classURI must not be null";
    assert getRDFEntity() != null : "rdfEntity() must not be null";

    final Field idField = getIdField();
    if (idField == null) {
      throw new TexaiException("Id field not found for RDF entity " + getRDFEntity());
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
    if (value == null) {
      throw new TexaiException("Id field has no value " + idField);
    } else {
      if (URI.class.isAssignableFrom(value.getClass())) {
        setInstanceURI((URI) value);
      } else {
        setInstanceURI(makeURI(value.toString()));
      }
      if (getLogger().isDebugEnabled()) {
        logger.debug(stackLevel() + "  Id specifies existing instance " + getInstanceURI());
      }
    }

    //Postconditions
    assert getInstanceURI() != null : "instanceURI must not be null";
  }

  /** Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return logger;
  }
}
