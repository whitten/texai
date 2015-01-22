/*
 * RDFPersistent.java
 *
 * Created on May 26, 2009, 6:51:16 PM
 *
 * Description: Defines the RDF persistence interface.
 *
 * Copyright (C) May 26, 2009 Stephen L. Reed.
 */

package org.texai.kb.persistence;

import java.io.Serializable;
import org.openrdf.model.URI;

/** Defines the RDF persistence interface.
 *
 * @author Stephen L. Reed.
 */
public interface RDFPersistent extends Serializable {
  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  URI getId();
}
