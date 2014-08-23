/*
 * InferenceAccess.java
 *
 * Created on Feb 4, 2009, 8:44:18 AM
 *
 * Description: Provides access to inference objects.
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

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.TexaiException;

/** Provides access to inference objects.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class InferenceAccess {

  /** the log4j logger */
  private static final Logger LOGGER = Logger.getLogger(InferenceAccess.class);
  /** the KB entity manager */
  private final RDFEntityManager kbEntityManager;
  /** the name of the cache for the query container lookup, name --> query container */
  public static final String CACHE_QUERY_CONTAINER_LOOKUP = "query container lookup";

  /** Constructs a new InferenceAccess instance.
   *
   * @param kbEntityManager the KB entity manager
   */
  public InferenceAccess(final RDFEntityManager kbEntityManager) {
    //Preconditions
    assert kbEntityManager != null : "kbEntityManager must not be null";

    this.kbEntityManager = kbEntityManager;
    CacheInitializer.addNamedCache(CACHE_QUERY_CONTAINER_LOOKUP);
  }

  public void persistQueryContainer(final QueryContainer queryContainer, final boolean areQueriesReplaced) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";

    final QueryContainer existingQueryContainer = findQueryContainerByName(queryContainer.getName());
    if (existingQueryContainer == null) {
      LOGGER.info("persisting: " + queryContainer.getName());
      queryContainer.cascadePersist(kbEntityManager);
    } else if (areQueriesReplaced) {
      LOGGER.info("removing previous: " + existingQueryContainer.getName());
      existingQueryContainer.cascadeRemove(
              kbEntityManager);
      LOGGER.info("persisting: " + queryContainer.getName());
      queryContainer.cascadePersist(kbEntityManager);
    }
  }

  public void cacheQueryContainer(final QueryContainer queryContainer) {
    //Preconditions
    assert queryContainer != null : "queryContainer must not be null";
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("caching " + queryContainer.getName());
    }

    final Cache cache = CacheManager.getInstance().getCache(CACHE_QUERY_CONTAINER_LOOKUP);
    assert cache != null : "cache not found for: " + CACHE_QUERY_CONTAINER_LOOKUP;
    final  Element element = new Element(queryContainer.getName(), queryContainer);
    cache.put(element);
  }

  /** Finds the SPARQL query container having the given name.
   *
   * @param name the query name
   * @return the SPARQL query container having the given name, or null if not found
   */
  @SuppressWarnings("unchecked")
  public QueryContainer findQueryContainerByName(final String name) {
    //Preconditions
    assert name != null : "name must not be null";
    assert !name.isEmpty() : "name must not be empty";

    final Cache cache = CacheManager.getInstance().getCache(CACHE_QUERY_CONTAINER_LOOKUP);
    assert cache != null : "cache not found for: " + CACHE_QUERY_CONTAINER_LOOKUP;
    Element element = cache.get(name);
    if (element == null) {
      final QueryContainer queryContainer;
      final List<QueryContainer> queries = kbEntityManager.find(
              InferenceConstants.SPARQL_QUERY_CONTAINER_NAME,
              name,
              QueryContainer.class);
      if (queries.isEmpty()) {
        return null;
      } else if (queries.size() == 1) {
        queryContainer = queries.get(0);
      } else {
        throw new TexaiException("query must be unique:\n" + queries);
      }
      element = new Element(name, queryContainer);
      cache.put(element);
      return queryContainer;
    } else {
      return (QueryContainer) element.getValue();
    }
  }

  /** Gets the KB entity manager.
   *
   * @return the KB entity manager
   */
  public RDFEntityManager getKbEntityManager() {
    return kbEntityManager;
  }
}
