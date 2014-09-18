/*
 * RDFPartitionable.java
 *
 * Created on Dec 20, 2012, 2:40:38 PM
 *
 * Description: Defines the RDF repository partition interface.
 *
 * Copyright (C) Dec 20, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.kb.persistence;

/** Defines the RDF repository partition interface.
 *
 * @author reed
 */
public interface RDFPartitionable {

  /** Gets the number assigned to the repository partition into which this object is persisted.
   *
   * @return the number assigned to the repository partition into which this object is persisted
   */
  int getRepositoryPartitionNbr();

}
