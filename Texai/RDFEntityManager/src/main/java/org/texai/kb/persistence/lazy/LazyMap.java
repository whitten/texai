/*
 * LazyMap.java
 *
 * Created on Jul 30, 2010, 1:08:25 PM
 *
 * Description: Provides a means to lazily load a Set field.
 *
 * Copyright (C) Jul 30, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.kb.persistence.lazy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityLoader;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.TexaiException;

/** Provides a means to lazily load a Set field.
 *
 * @author reed
 */
@NotThreadSafe
public class LazyMap implements Map, Serializable {

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
  /** the loaded set */
  private Map loadedMap;
  /** the indicator that the lazy set is currently being loaded */
  private boolean isLoading = false;

  /** Creates a new instance of LazySet.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF instance
   * @param field the RDF instance field
   * @param rdfProperty the RDF property
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   */
  public LazyMap(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    super();
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfInstance must not be null";
    assert field != null : "field must not be null";
    assert rdfProperty != null : "rdfProperty must not be null";
    assert predicateValuesDictionary != null : "predicateValuesDictionary must not be null";

    repositoryName = repositoryConnection.getRepository().getDataDir().getName();
    this.rdfEntity = rdfEntity;
    this.field = field;
    this.fieldName = field.getName();
    this.rdfProperty = rdfProperty;
    this.predicateValuesDictionary = predicateValuesDictionary;
  }

  /** Gets the loaded map.
   *
   * @return the loaded map
   */
  public synchronized Map getLoadedMap() {
    if (isLoading) {
      return null;                                                   // NOPMD
    } else {
      loadTheMap();
      return loadedMap;
    }
  }

  /** Lazily loads the set. Also replaces the lazy set with the loaded set on the RDF entity so that subsequent
   * field value will directly access the set.
   */
  @SuppressWarnings("unchecked")                                     // NOPMD
  private synchronized void loadTheMap() {
    if (!isLoading && loadedMap == null) {
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
      loadedMap = (Map) rdfEntityLoader.loadLazyRDFEntityField(
              repositoryConnection,
              rdfEntity,
              field,
              rdfProperty,
              predicateValuesDictionary);
      assert loadedMap != null : "loadedSet must not be null";
      try {
        repositoryConnection.close();
      } catch (final RepositoryException ex) {
        throw new TexaiException(ex);
      } finally {
        isLoading = false;
      }
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    String string;
    if (loadedMap == null) {
      string = "[LazyMap for " + rdfProperty + "]";
    } else {
      string = loadedMap.toString();
    }
    return string;
  }

  // Query Operations
  /** Returns the number of key-value mappings in this map.  If the
   * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of key-value mappings in this map
   */
  @Override
  public int size() {
    int size;
    loadTheMap();
    if (isLoading) {
      size = 0;
    } else {
      size = loadedMap.size();
    }
    return size;
  }

  /** Returns <tt>true</tt> if this map contains no key-value mappings.
   *
   * @return <tt>true</tt> if this map contains no key-value mappings
   */
  @Override
  public boolean isEmpty() {
    boolean isEmpty;
    loadTheMap();
    if (isLoading) {
      isEmpty = true;
    } else {
      isEmpty = loadedMap.isEmpty();
    }
    return isEmpty;
  }

  /** Returns <tt>true</tt> if this map contains a mapping for the specified
   * key.  More formally, returns <tt>true</tt> if and only if
   * this map contains a mapping for a key <tt>k</tt> such that
   * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
   * at most one such mapping.)
   *
   * @param key key whose presence in this map is to be tested
   * @return <tt>true</tt> if this map contains a mapping for the specified
   *         key
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map (optional)
   * @throws NullPointerException if the specified key is null and this map
   *         does not permit null keys (optional)
   */
  @Override
  public boolean containsKey(final Object key) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.containsKey(key);
    }
  }

  /** Returns <tt>true</tt> if this map maps one or more keys to the
   * specified value.  More formally, returns <tt>true</tt> if and only if
   * this map contains at least one mapping to a value <tt>v</tt> such that
   * <tt>(value==null ? v==null : value.equals(v))</tt>.  This operation
   * will probably require time linear in the map size for most
   * implementations of the <tt>Map</tt> interface.
   *
   * @param value value whose presence in this map is to be tested
   * @return <tt>true</tt> if this map maps one or more keys to the
   *         specified value
   * @throws ClassCastException if the value is of an inappropriate type for
   *         this map (optional)
   * @throws NullPointerException if the specified value is null and this
   *         map does not permit null values (optional)
   */
  @Override
  public boolean containsValue(final Object value) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.containsValue(value);
    }
  }

  /** Returns the value to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * <p>More formally, if this map contains a mapping from a key
   * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
   * key.equals(k))}, then this method returns {@code v}; otherwise
   * it returns {@code null}.  (There can be at most one such mapping.)
   *
   * <p>If this map permits null values, then a return value of
   * {@code null} does not <i>necessarily</i> indicate that the map
   * contains no mapping for the key; it's also possible that the map
   * explicitly maps the key to {@code null}.  The {@link #containsKey
   * containsKey} operation may be used to distinguish these two cases.
   *
   * @param key the key whose associated value is to be returned
   * @return the value to which the specified key is mapped, or
   *         {@code null} if this map contains no mapping for the key
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map (optional)
   * @throws NullPointerException if the specified key is null and this map
   *         does not permit null keys (optional)
   */
  @Override
  public Object get(final Object key) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.get(key);
    }
  }

  // Modification Operations
  /** Associates the specified value with the specified key in this map
   * (optional operation).  If the map previously contained a mapping for
   * the key, the old value is replaced by the specified value.  (A map
   * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
   * if {@link #containsKey(Object) m.containsKey(k)} would return
   * <tt>true</tt>.)
   *
   * @param key key with which the specified value is to be associated
   * @param value value to be associated with the specified key
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   *         (A <tt>null</tt> return can also indicate that the map
   *         previously associated <tt>null</tt> with <tt>key</tt>,
   *         if the implementation supports <tt>null</tt> values.)
   * @throws UnsupportedOperationException if the <tt>put</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of the specified key or value
   *         prevents it from being stored in this map
   * @throws NullPointerException if the specified key or value is null
   *         and this map does not permit null keys or values
   * @throws IllegalArgumentException if some property of the specified key
   *         or value prevents it from being stored in this map
   */
  @Override
  @SuppressWarnings("unchecked")
  public Object put(final Object key, final Object value) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.put(key, value);
    }
  }

  /** Removes the mapping for a key from this map if it is present
   * (optional operation).   More formally, if this map contains a mapping
   * from key <tt>k</tt> to value <tt>v</tt> such that
   * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
   * is removed.  (The map can contain at most one such mapping.)
   *
   * <p>Returns the value to which this map previously associated the key,
   * or <tt>null</tt> if the map contained no mapping for the key.
   *
   * <p>If this map permits null values, then a return value of
   * <tt>null</tt> does not <i>necessarily</i> indicate that the map
   * contained no mapping for the key; it's also possible that the map
   * explicitly mapped the key to <tt>null</tt>.
   *
   * <p>The map will not contain a mapping for the specified key once the
   * call returns.
   *
   * @param key key whose mapping is to be removed from the map
   * @return the previous value associated with <tt>key</tt>, or
   *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
   * @throws UnsupportedOperationException if the <tt>remove</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the key is of an inappropriate type for
   *         this map (optional)
   * @throws NullPointerException if the specified key is null and this
   *         map does not permit null keys (optional)
   */
  @Override
  public Object remove(final Object key) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.remove(key);
    }
  }

  // Bulk Operations
  /** Copies all of the mappings from the specified map to this map
   * (optional operation).  The effect of this call is equivalent to that
   * of calling {@link #put(Object,Object) put(k, v)} on this map once
   * for each mapping from key <tt>k</tt> to value <tt>v</tt> in the
   * specified map.  The behavior of this operation is undefined if the
   * specified map is modified while the operation is in progress.
   *
   * @param m mappings to be stored in this map
   * @throws UnsupportedOperationException if the <tt>putAll</tt> operation
   *         is not supported by this map
   * @throws ClassCastException if the class of a key or value in the
   *         specified map prevents it from being stored in this map
   * @throws NullPointerException if the specified map is null, or if
   *         this map does not permit null keys or values, and the
   *         specified map contains null keys or values
   * @throws IllegalArgumentException if some property of a key or value in
   *         the specified map prevents it from being stored in this map
   */
  @Override
  @SuppressWarnings("unchecked")
  public void putAll(final Map m) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      loadedMap.putAll(m);
    }
  }

  /** Removes all of the mappings from this map (optional operation).
   * The map will be empty after this call returns.
   *
   * @throws UnsupportedOperationException if the <tt>clear</tt> operation
   *         is not supported by this map
   */
  @Override
  public void clear() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      loadedMap.clear();
    }
  }

  // Views
  /** Returns a {@link Set} view of the keys contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation), the results of
   * the iteration are undefined.  The set supports element removal,
   * which removes the corresponding mapping from the map, via the
   * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
   * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
   * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
   * operations.
   *
   * @return a set view of the keys contained in this map
   */
  @Override
  public Set keySet() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.keySet();
    }
  }

  /** Returns a {@link Collection} view of the values contained in this map.
   * The collection is backed by the map, so changes to the map are
   * reflected in the collection, and vice-versa.  If the map is
   * modified while an iteration over the collection is in progress
   * (except through the iterator's own <tt>remove</tt> operation),
   * the results of the iteration are undefined.  The collection
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
   * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
   * support the <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a collection view of the values contained in this map
   */
  @Override
  public Collection values() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.values();
    }
  }

  /** Returns a {@link Set} view of the mappings contained in this map.
   * The set is backed by the map, so changes to the map are
   * reflected in the set, and vice-versa.  If the map is modified
   * while an iteration over the set is in progress (except through
   * the iterator's own <tt>remove</tt> operation, or through the
   * <tt>setValue</tt> operation on a map entry returned by the
   * iterator) the results of the iteration are undefined.  The set
   * supports element removal, which removes the corresponding
   * mapping from the map, via the <tt>Iterator.remove</tt>,
   * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
   * <tt>clear</tt> operations.  It does not support the
   * <tt>add</tt> or <tt>addAll</tt> operations.
   *
   * @return a set view of the mappings contained in this map
   */
  @Override
  public Set entrySet() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy map");
    } else {
      loadTheMap();
      return loadedMap.entrySet();
    }
  }
}
