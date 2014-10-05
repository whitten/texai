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

/**
 * A set having a maximum capacity whose excess elements are evicted according to least recently used.
 *
 * @param <E> the element class
 *
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

  /**
   * Creates a new LRUSet instance with the given initial capacity and maximum capacity.
   *
   * @param initialCapacity the initial capacity of the cache
   * @param maxCapacity the maximum capacity of the cache
   */
  public LRUSet(final int initialCapacity, final int maxCapacity) {
    //Preconditions
    assert initialCapacity >= 0 : "initialCapacity must not be negative";
    assert initialCapacity > 0 : "maxCapacity must be positive";
    assert initialCapacity <= maxCapacity : "initialCapacity not be greater than maxCapacity";

    map = new LRUMap<>(initialCapacity, maxCapacity);
  }

  /**
   * Returns <tt>true</tt> if this set contains the specified element.
   *
   * @param obj element whose presence in this set is to be tested
   *
   * @return <tt>true</tt> if this set contains the specified element
   */
  @Override
  @SuppressWarnings("element-type-mismatch")
  public boolean contains(final Object obj) {
    return map.containsKey(obj);
  }

  /**
   * Returns an iterator over the elements in this set. The elements are returned in no particular order (unless this set is an instance of
   * some class that provides a guarantee).
   *
   * @return an iterator over the elements in this set
   */
  @Override
  public Iterator<E> iterator() {
    return map.keySet().iterator();
  }

  /**
   * Returns an array containing all of the elements in this set. If this set makes any guarantees as to what order its elements are
   * returned by its iterator, this method must return the elements in the same order.
   *
   * <p>
   * The returned array will be "safe" in that no references to it are maintained by this set. (In other words, this method must allocate a
   * new array even if this set is backed by an array). The caller is thus free to modify the returned array.
   *
   * <p>
   * This method acts as bridge between array-based and collection-based APIs.
   *
   * @return an array containing all the elements in this set
   */
  @Override
  public Object[] toArray() {
    return map.keySet().toArray();
  }

  /**
   * Returns an array containing all of the elements in this set; the runtime type of the returned array is that of the specified array. If
   * the set fits in the specified array, it is returned therein. Otherwise, a new array is allocated with the runtime type of the specified
   * array and the size of this set.
   *
   * <p>
   * If this set fits in the specified array with room to spare (i.e., the array has more elements than this set), the element in the array
   * immediately following the end of the set is set to
   * <tt>null</tt>. (This is useful in determining the length of this set <i>only</i> if the caller knows that this set does not contain any
   * null elements.)
   *
   * <p>
   * If this set makes any guarantees as to what order its elements are returned by its iterator, this method must return the elements in
   * the same order.
   *
   * <p>
   * Like the {@link #toArray()} method, this method acts as bridge between array-based and collection-based APIs. Further, this method
   * allows precise control over the runtime type of the output array, and may, under certain circumstances, be used to save allocation
   * costs.
   *
   * <p>
   * Suppose <tt>x</tt> is a set known to contain only strings. The following code can be used to dump the set into a newly allocated array
   * of <tt>String</tt>:
   *
   * <pre>
   *     String[] y = x.toArray(new String[0]);</pre>
   *
   * Note that <tt>toArray(new Object[0])</tt> is identical in function to
   * <tt>toArray()</tt>.
   *
   * @param a the array into which the elements of this set are to be stored, if it is big enough; otherwise, a new array of the same
   * runtime type is allocated for this purpose.
   *
   * @return an array containing all the elements in this set
   */
  @Override
  @SuppressWarnings("SuspiciousToArrayCall")
  public <T> T[] toArray(final T[] a) {
    //Preconditions
    assert a != null;

    return map.keySet().toArray(a);
  }

  /**
   * Adds the specified element to this set if it is not already present (optional operation).
   *
   * @param e element to be added to this set
   *
   * @return <tt>true</tt> if this set did not already contain the specified element
   */
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

  /**
   * Removes the specified element from this set if it is present (optional operation).
   *
   * @param o object to be removed from this set, if present
   *
   * @return <tt>true</tt> if this set contained the specified element
   */
  @Override
  public boolean remove(final Object o) {
    //Preconditions
    assert o != null;

    @SuppressWarnings("element-type-mismatch")
    final Object obj = map.remove(o);
    return obj != null;
  }

  /**
   * Returns <tt>true</tt> if this set contains all of the elements of the specified collection. If the specified collection is also a set,
   * this method returns <tt>true</tt> if it is a <i>subset</i> of this set.
   *
   * @param c collection to be checked for containment in this set
   *
   * @return <tt>true</tt> if this set contains all of the elements of the specified collection
   */
  @Override
  public boolean containsAll(final Collection<?> c) {
    //Preconditions
    assert c != null;

    return map.keySet().containsAll(c);
  }

  /**
   * Adds all of the elements in the specified collection to this set if they're not already present (optional operation). If the specified
   * collection is also a set, the <tt>addAll</tt> operation effectively modifies this set so that its value is the <i>union</i> of the two
   * sets. The behavior of this operation is undefined if the specified collection is modified while the operation is in progress.
   *
   * @param c collection containing elements to be added to this set
   *
   * @return <tt>true</tt> if this set changed as a result of the call
   *
   */
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

  /**
   * Retains only the elements in this set that are contained in the specified collection (optional operation). In other words, removes from
   * this set all of its elements that are not contained in the specified collection. If the specified collection is also a set, this
   * operation effectively modifies this set so that its value is the
   * <i>intersection</i> of the two sets.
   *
   * @param c collection containing elements to be retained in this set
   *
   * @return <tt>true</tt> if this set changed as a result of the call
   */
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

  /**
   * Removes from this set all of its elements that are contained in the specified collection (optional operation). If the specified
   * collection is also a set, this operation effectively modifies this set so that its value is the <i>asymmetric set difference</i> of the
   * two sets.
   *
   * @param c collection containing elements to be removed from this set
   *
   * @return <tt>true</tt> if this set changed as a result of the call
   */
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
   * Removes all of the elements from this set (optional operation). The set will be empty after this call returns.
   */
  @Override
  public void clear() {
    map.clear();
  }

  /**
   * Returns a string representation of the object.
   *
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return map.keySet().toString();
  }

  /**
   * Compares the specified object with this set for equality. Returns
   * <tt>true</tt> if the specified object is also a set, the two sets have the same size, and every member of the specified set is
   * contained in this set (or equivalently, every member of this set is contained in the specified set).
   *
   * @param obj object to be compared for equality with this set
   *
   * @return <tt>true</tt> if the specified object is equal to this set
   */
  @Override
  public boolean equals(final Object obj) {
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

  /**
   * Returns the hash code value for this set. The hash code of a set is defined to be the sum of the hash codes of the elements in the set,
   * where the hash code of a <tt>null</tt> element is defined to be zero. This ensures that <tt>s1.equals(s2)</tt> implies that
   * <tt>s1.hashCode()==s2.hashCode()</tt> for any two sets <tt>s1</tt>
   * and <tt>s2</tt>, as required by the general contract of {@link Object#hashCode}.
   *
   * @return the hash code value for this set
   */
  @Override
  public int hashCode() {
    return map.keySet().hashCode();
  }
}
