/*
 * Entailment.java
 *
 * Created on Mar 14, 2008, 11:31:51 AM
 *
 * Description: Provides deductive entailment inference.
 *
 * Copyright (C) Mar 14, 2008 Stephen L. Reed.
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


import org.texai.kb.CacheInitializer;

/** Provides deductive entailment inference.
 *
 * @author Stephen L. Reed
 */
public final class Entailment {

  // Named caches
  /** the name of the cache for the term entailment rule index, term pair --> connecting rule list */
  public static final String CACHE_TERM_ENTAILMENT_RULE = "term entailment rules";
  /** the named caches used by this library */
  static final String[] NAMED_CACHES = {CACHE_TERM_ENTAILMENT_RULE};

  /** Constructs a new Entailment instance. */
  public Entailment() {
    initialize();
  }

  /** Initializes this class. */
  private void initialize() {
    CacheInitializer.addNamedCaches(NAMED_CACHES);
  }

  //TODO add entailment inference

  /** Closes this instance and releases its resources. */
  public void close() {
    for (final String namedCache : NAMED_CACHES) {
      CacheInitializer.resetCache(namedCache);
    }
  }


  //TODO
}
