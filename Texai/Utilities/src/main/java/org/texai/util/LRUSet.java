/*
 * LRUSet.java
 *
 * Created on Nov 18, 2010, 1:32:31 PM
 *
 * Description: A set having a maximum capacity whose excess elements are evicted according to least recently used.
 *
 * Copyright (C) Nov 18, 2010, Stephen L. Reed.
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
package org.texai.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/** A set having a maximum capacity whose excess elements are evicted according to least recently used.
 *
 * @param <E> the element class
 * @author reed
 */
@NotThreadSafe
public class LRUSet<E> implements Set<E> {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the dummy map value
  private static final Object PRESENT = new Object();
  // the map
  private final LRUMap<E, Object> map;

  // 
  // @param initialCapacity the initial capacity of the cache
  // @param maxCapacity the maximum capacity of the cache
  public LRUSet(final int initialCapacity, final int maxCapacity) {
    //Preconditions
    assert initialCapacity >= 0 : "initialCapacity must not be negative";
    assert initialCapacity > 0 : "maxCapacity must be positive";
    assert initialCapacity <= maxCapacity : "initialCapacity not be greater than maxCapacity";

    map = new LRUMap<>(initialCapacity, maxCapacity);
  }

  // 
  // @param obj element whose presence in this set is to be tested
  // @return <tt>true</tt> if this set contains the specified element
  @Override
  @SuppressWarnings("element-type-mismatch")
  public boolean contains(final Object obj) {
    return map.containsKey(obj);
  }

  // 
  // @return an iterator over the elements in this set
  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  // 
  // @return an array containing all the elements in this set
  @Override
  public Object[] toArray() {
    return map.keySet().toArray();
  }

  // 
  // 
  // @param <T> the array type
  // @param a the array into which the elements of this set are to be
  // stored, if it is big enough; otherwise, a new array of the same
  // runtime type is allocated for this purpose.
  // @return an array containing all the elements in this set
  // @throws ArrayStoreException if the runtime type of the specified array
  // is not a supertype of the runtime type of every element in this
  // set
  // @throws NullPointerException if the specified array is null
  @Override
  @SuppressWarnings("SuspiciousToArrayCall")
  public <T> T[] toArray(final T[] a) {
    //Preconditions
    assert a != null;

    return map.keySet().toArray(a);
  }

  // (optional operation).
  // 
  // @param e element to be added to this set
  // @return <tt>true</tt> if this set did not already contain the specified
  // element
  // @throws UnsupportedOperationException if the <tt>add</tt> operation
  // is not supported by this set
  // @throws ClassCastException if the class of the specified element
  // prevents it from being added to this set
  // @throws NullPointerException if the specified element is null and this
  // set does not permit null elements
  // @throws IllegalArgumentException if some property of the specified element
  // prevents it from being added to this set
  @Override
  public boolean add(final E e) {
    //Preconditions
    assert e != null;

    if (map.containsKey(e)) {
      return true;
    }
    map.put(e, PRESENT);
    return false;
  }

  // 
  // @param o object to be removed from this set, if present
  // @return <tt>true</tt> if this set contained the specified element
  // @throws ClassCastException if the type of the specified element
  // is incompatible with this set (optional)
  // @throws NullPointerException if the specified element is null and this
  // set does not permit null elements (optional)
  // @throws UnsupportedOperationException if the <tt>remove</tt> operation
  // is not supported by this set
  @Override
  public boolean remove(final Object o) {
    //Preconditions
    assert o != null;

    @SuppressWarnings("element-type-mismatch")
    final Object obj = map.remove(o);
    return obj != null;
  }

  // Returns <tt>true</tt> if this set contains all of the elements of the
  // specified collection.  If the specified collection is also a set, this
  // method returns <tt>true</tt> if it is a <i>subset</i> of this set.
  // 
  // @param  c collection to be checked for containment in this set
  // @return <tt>true</tt> if this set contains all of the elements of the
  // specified collection
  // @throws ClassCastException if the types of one or more elements
  // in the specified collection are incompatible with this
  // set (optional)
  // @throws NullPointerException if the specified collection contains one
  // or more null elements and this set does not permit null
  // elements (optional), or if the specified collection is null
  // @see    #contains(Object)
  @Override
  public boolean containsAll(final Collection<?> c) {
    //Preconditions
    assert c != null;

    return map.keySet().containsAll(c);
  }

  // they're not already present (optional operation).  If the specified
  // collection is also a set, the <tt>addAll</tt> operation effectively
  // modifies this set so that its value is the <i>union</i> of the two
  // sets.  The behavior of this operation is undefined if the specified
  // collection is modified while the operation is in progress.
  // 
  // @param  c collection containing elements to be added to this set
  // @return <tt>true</tt> if this set changed as a result of the call
  // 
  // @throws UnsupportedOperationException if the <tt>addAll</tt> operation
  // is not supported by this set
  // @throws ClassCastException if the class of an element of the
  // specified collection prevents it from being added to this set
  // @throws NullPointerException if the specified collection contains one
  // or more null elements and this set does not permit null
  // elements, or if the specified collection is null
  // @throws IllegalArgumentException if some property of an element of the
  // specified collection prevents it from being added to this set
  // @see #add(Object)
  @Override
  public boolean addAll(final Collection<? extends E> c) {
    //Preconditions
    assert c != null;

    boolean isAdded = map.keySet().containsAll(c);
    for (final E item : c) {
      map.put(item, PRESENT);
    }
    return isAdded;
  }

  // specified collection (optional operation).  In other words, removes
  // from this set all of its elements that are not contained in the
  // specified collection.  If the specified collection is also a set, this
  // operation effectively modifies this set so that its value is the
  // <i>intersection</i> of the two sets.
  // 
  // @param  c collection containing elements to be retained in this set
  // @return <tt>true</tt> if this set changed as a result of the call
  // @throws UnsupportedOperationException if the <tt>retainAll</tt> operation
  // is not supported by this set
  // @throws ClassCastException if the class of an element of this set
  // is incompatible with the specified collection (optional)
  // @throws NullPointerException if this set contains a null element and the
  // specified collection does not permit null elements (optional),
  // or if the specified collection is null
  // @see #remove(Object)
  @Override
  public boolean retainAll(final Collection<?> c) {
    //Preconditions
    assert c != null;

    boolean isChanged = false;
    final Iterator<Entry<E, Object>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      final Entry<E, Object> entry = iter.next();
      if (!c.contains(entry.getKey())) {
        isChanged = true;
        iter.remove();
      }
    }
    return isChanged;
  }

  // specified collection (optional operation).  If the specified
  // collection is also a set, this operation effectively modifies this
  // set so that its value is the <i>asymmetric set difference</i> of
  // the two sets.
  // 
  // @param  c collection containing elements to be removed from this set
  // @return <tt>true</tt> if this set changed as a result of the call
  // @throws UnsupportedOperationException if the <tt>removeAll</tt> operation
  // is not supported by this set
  // @throws ClassCastException if the class of an element of this set
  // is incompatible with the specified collection (optional)
  // @throws NullPointerException if this set contains a null element and the
  // specified collection does not permit null elements (optional),
  // or if the specified collection is null
  // @see #remove(Object)
  // @see #contains(Object)
  @Override
  public boolean removeAll(final Collection<?> c) {
    //Preconditions
    assert c != null;

    boolean isChanged = false;
    final Iterator<Entry<E, Object>> iter = map.entrySet().iterator();
    while (iter.hasNext()) {
      final Entry<E, Object> entry = iter.next();
      if (c.contains(entry.getKey())) {
        isChanged = true;
        iter.remove();
      }
    }
    return isChanged;
  }

  // set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
  // <tt>Integer.MAX_VALUE</tt>.
  // 
  // @return the number of elements in this set (its cardinality)
  @Override
  public int size() {
    return map.size();
  }

  // 
  // @return <tt>true</tt> if this set contains no elements
  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  // The set will be empty after this call returns.
  // 
  // @throws UnsupportedOperationException if the <tt>clear</tt> method
  // is not supported by this set
  @Override
  public void clear() {
    map.clear();
  }

  // 
  // @return a string representation of this object
  @Override
  public String toString() {
    return map.keySet().toString();
  }

  // 
  // @param obj the other object
  // @return whether some other object equals this one
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    @SuppressWarnings("unchecked")
    final LRUSet<E> other = (LRUSet<E>) obj;
    return this.map == other.map || (this.map != null && this.map.equals(other.map));
  }

  // 
  // @return a hash code for this object
  @Override
  public int hashCode() {
    return map.keySet().hashCode();
  }
}
