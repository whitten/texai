/*
 * IdentitySet.java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.texai.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

/**
 * Set that compares object by identity rather than equality. Wraps around a <code>IdentityHashMap</code>
 *
 * @param <E> the element type
 *
 * @author Emmanuel Bernard
 */
@NotThreadSafe
public class IdentitySet<E> extends AbstractSet<E> implements Serializable {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the underlying map
  private Map<E, Object> map;
  // the contains placeholder object
  private final Object CONTAINS = new Object();

  /**
   * Creates a new IdentitySet instance with the default initial size of 10 elements.
   */
  public IdentitySet() {
    this(10);
  }

  /**
   * Creates a new IdentitySet instance with the given initial size.
   *
   * @param size the given initial size
   */
  public IdentitySet(int size) {
    this.map = new IdentityHashMap<>(size);
  }

  /**
   * Returns the number of elements in this set (its cardinality). If this set contains more than <tt>Integer.MAX_VALUE</tt> elements,
   * returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this set (its cardinality)
   */
  @Override
  public int size() {
    return map.size();
  }

  /**
   * Returns <tt>true</tt> if this set contains no elements.
   *
   * @return <tt>true</tt> if this set contains no elements
   */
  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  /**
   * Returns <tt>true</tt> if this set contains the specified element.
   *
   * @param element the specified element
   *
   * @return <tt>true</tt> if this set contains the specified element
   */
  @Override
  @SuppressWarnings("element-type-mismatch")
  public boolean contains(final Object element) {
    return map.containsKey(element);
  }

  /**
   * Returns an array containing all of the elements in this set.
   *
   * @return an array containing all the elements in this set
   */
  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  /**
   * Adds the specified element to this set if it is not already present (optional operation).
   *
   * @param element the specified element
   *
   * @return <tt>true</tt> if this set did not already contain the specified element
   */
  @Override
  public boolean add(final E element) {
    return map.put(element, CONTAINS) == null;
  }

  /**
   * Removes the specified element from this set if it is present (optional operation).
   *
   * @param element the specified element
   *
   * @return <tt>true</tt> if this set contained the specified element
   */
  @Override
  @SuppressWarnings({"unchecked", "element-type-mismatch"})
  public boolean remove(final Object element) {
    return map.remove(element) == CONTAINS;
  }

  /**
   * Removes all of the elements from this set (optional operation). The set will be empty after this call returns.
   */
  @Override
  public void clear() {
    map.clear();
  }

}
