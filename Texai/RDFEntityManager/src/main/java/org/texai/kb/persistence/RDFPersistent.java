/*
 * RDFPersistent.java
 *
 * Created on May 26, 2009, 6:51:16 PM
 *
 * Description: Defines the RDF persistence interface.
 *
 * Copyright (C) May 26, 2009 Stephen L. Reed.
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
