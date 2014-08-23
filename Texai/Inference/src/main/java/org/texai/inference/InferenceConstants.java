/*
 * InferenceConstants.java
 *
 * Created on Feb 4, 2009, 8:52:23 AM
 *
 * Description: Provides immutable constants for inference.
 *
 * Copyright (C) Feb 4, 2009 Stephen L. Reed.
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

package org.texai.inference;


import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;

/** Provides immutable constants for inference.
 *
 * @author Stephen L. Reed
 */
@Immutable
public final class InferenceConstants {

  /** the texai:sparqlQueryContainerName term */
  public static final URI SPARQL_QUERY_CONTAINER_NAME = new URIImpl(Constants.TEXAI_NAMESPACE + "sparqlQueryContainerName");

  /** Constructs a new InferenceConstants instance. */
  private InferenceConstants() {
  }
}
