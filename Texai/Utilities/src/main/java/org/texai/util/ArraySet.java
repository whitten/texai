/*
 * ArraySet.java
 *
 * Created on August 24, 2007, 11:27 PM
 *
 * Description: Provides an efficent implementation for small sets.
 *
 * Copyright (C) August 24, 2007 Stephen L. Reed.
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
package org.texai.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Provides an efficient implementation for small sets.
 *
 * @param <E> the element type
 *
 * @author reed
 */
public class ArraySet<E> extends AbstractSet<E> implements Cloneable, Serializable {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the list that efficiently contains a small number of set elements
  private final List<E> list;

  /**
   * Creates a new instance of ArraySet.
   */
  public ArraySet() {
    list = new ArrayList<>();
  }

  //
  // @param collection the given collection
  @SuppressWarnings("OverridableMethodCallInConstructor")
  public ArraySet(final Collection<? extends E> collection) {
    list = new ArrayList<>();

    // no need to check for duplicate elements if collection is a set
    if (collection instanceof Set<?>) {
      list.addAll(collection);
    } else {
      final Iterator<? extends E> iter = collection.iterator();
      while (iter.hasNext()) {
        add(iter.next());    // NOPMD
      }
    }
  }

  //
  // @param size the given initial capacity
  public ArraySet(final int size) {
    list = new ArrayList<>(size);
  }

  //
  // @return an iterator over the elements in this set
  @Override
  public Iterator<E> iterator() {
    return list.iterator();
  }

  // set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
  // <tt>Integer.MAX_VALUE</tt>.
  //
  // @return the number of elements in this set (its cardinality)
  @Override
  public int size() {
    return list.size();
  }

  //
  // @param element element to be added to this set
  // @return <tt>true</tt> if this set did not already contain the specified
  // element
  @Override
  public boolean add(final E element) {
    final boolean isModified = !list.contains(element);
    if (isModified) {
      list.add(element);
    }
    return isModified;
  }

  //
  // @param element the element to be removed from this set, if present
  // @return <tt>true</tt> if this set contained the specified element
  @Override
  @SuppressWarnings("unchecked")
  public boolean remove(final Object element) {
    return list.remove((E) element);
  }

  //
  // @return <tt>true</tt> if this set contains no elements
  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  //
  // @param element the element whose presence in this set is to be tested
  // @return <tt>true</tt> if this set contains the specified element
  @Override
  @SuppressWarnings("unchecked")
  public boolean contains(final Object element) {
    return list.contains((E) element);
  }

  // The set will be empty after this call returns.
  @Override
  public void clear() {
    list.clear();
  }

  //
  // @return a shallow copy of this set
  // @throws CloneNotSupportedException if the clone operation is not supported by the element types of this set
  @Override
  @SuppressWarnings("unchecked")
  public Object clone() throws CloneNotSupportedException {
    final ArraySet<E> newSet = (ArraySet<E>) super.clone();
    for (final E element : list) {
      newSet.add(element);
    }
    return newSet;
  }
}
