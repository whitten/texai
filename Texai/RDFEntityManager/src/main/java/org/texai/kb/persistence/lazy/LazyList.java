/*
 * LazyList.java
 *
 * Created on January 18, 2007, 12:48 PM
 *
 * Description: Provides a means to lazily load a List field.
 *
 * Copyright (C) 2007 Stephen L. Reed.
 */
package org.texai.kb.persistence.lazy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityLoader;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.TexaiException;

/** Provides a facility that lazily loads a List field. The list value is loaded automatically from the RDF store when any of its methods are invoked.
 * The method call is delegated to the loaded list.  Subsequent references to the list field obtain the loaded list directly.  Note that because
 * not-yet-loaded lazy lists are not persisted to the RDF store, before they are copied into another persistent field they should first be
 * initialized (loaded) by invoking any of their defined methods (e.g. size()).
 *
 * @author reed
 */
@ThreadSafe
@edu.umd.cs.findbugs.annotations.SuppressWarnings({"SE_BAD_FIELD", "SE_TRANSIENT_FIELD_NOT_RESTORED"})
public final class LazyList implements List, Serializable {

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
  /** the loaded list */
  private List loadedList;
  /** the indicator that the lazy list is currently being loaded */
  private boolean isLoading = false;

  /*** Creates a new instance of LazyList.
   *
   * @param repositoryConnection the repository connection
   * @param rdfEntity the RDF instance
   * @param field the RDF instance field
   * @param rdfProperty the RDF property
   * @param predicateValuesDictionary the predicate values dictionary, predicate --> RDF values
   */
  public LazyList(
          final RepositoryConnection repositoryConnection,
          final RDFPersistent rdfEntity,
          final Field field,
          final RDFProperty rdfProperty,
          final Map<URI, List<Value>> predicateValuesDictionary) {
    super();
    //Preconditions
    assert repositoryConnection != null : "repositoryConnection must not be null";
    assert rdfEntity != null : "rdfEntity must not be null";
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

  /** Gets the loaded list.
   *
   * @return the loaded list
   */
  public synchronized List getLoadedList() {
    if (isLoading) {
      return null;                                                   // NOPMD
    } else {
      loadTheList();
      return loadedList;
    }
  }

  /** Lazily loads the list. */
  private synchronized void loadTheList() {
    if (!isLoading && loadedList == null) {
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
      loadedList = (List) rdfEntityLoader.loadLazyRDFEntityField(
              repositoryConnection,
              rdfEntity,
              field,
              rdfProperty,
              predicateValuesDictionary);
      assert loadedList != null : "loadedList must not be null";
      try {
        repositoryConnection.close();
      } catch (final RepositoryException ex) {
        throw new TexaiException(ex);
      } finally {
        isLoading = false;
      }
    }
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    String string;
    if (loadedList == null || isLoading) {
      string = "[LazyList for " + rdfProperty + "]";
    } else {
      string = loadedList.toString();
    }
    return string;
  }

  // the List methods
  /**
   * Returns the number of elements in this list.  If this list contains
   * more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this list
   */
  @Override
  public synchronized int size() {
    int size;
    if (isLoading) {
      size = 0;
    } else {
      loadTheList();
      size = loadedList.size();
    }
    return size;
  }

  /**
   * Returns <tt>true</tt> if this list contains no elements.
   *
   * @return <tt>true</tt> if this list contains no elements
   */
  @Override
  public synchronized boolean isEmpty() {
    if (isLoading) {
      return true;                                                   // NOPMD
    } else {
      loadTheList();
      return loadedList.isEmpty();
    }
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified element.
   * More formally, returns <tt>true</tt> if and only if this list contains
   * at least one element <tt>e</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
   *
   * @param element element whose presence in this list is to be tested
   * @return <tt>true</tt> if this list contains the specified element
   */
  @Override
  public synchronized boolean contains(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");           // NOPMD
    } else {
      loadTheList();
      return loadedList.contains(element);
    }
  }

  /**
   * Returns an iterator over the elements in this list in proper sequence.
   *
   * @return an iterator over the elements in this list in proper sequence
   */
  @Override
  public synchronized Iterator<?> iterator() {
    Iterator<?> iterator;
    if (isLoading) {
      iterator = (new ArrayList(0)).iterator();
    } else {
      loadTheList();
      iterator = loadedList.iterator();
    }
    return iterator;
  }

  /**
   * Returns an array containing all of the elements in this list in proper
   * sequence (from first to last element).
   *
   * <p>The returned array will be "safe" in that no references to it are
   * maintained by this list.  (In other words, this method must
   * allocate a new array even if this list is backed by an array).
   * The caller is thus free to modify the returned array.
   *
   * <p>This method acts as bridge between array-based and collection-based
   * APIs.
   *
   * @return an array containing all of the elements in this list in proper
   *         sequence
   */
  @Override
  public synchronized Object[] toArray() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.toArray();
    }
  }

  /**
   * Returns an array containing all of the elements in this list in
   * proper sequence (from first to last element); the runtime type of
   * the returned array is that of the specified array.  If the list fits
   * in the specified array, it is returned therein.  Otherwise, a new
   * array is allocated with the runtime type of the specified array and
   * the size of this list.
   *
   * <p>If the list fits in the specified array with room to spare (i.e.,
   * the array has more elements than the list), the element in the array
   * immediately following the end of the list is set to <tt>null</tt>.
   * (This is useful in determining the length of the list <i>only</i> if
   * the caller knows that the list does not contain any null elements.)
   *
   * <p>Like the {@link #toArray()} method, this method acts as bridge between
   * array-based and collection-based APIs.  Further, this method allows
   * precise control over the runtime type of the output array, and may,
   * under certain circumstances, be used to save allocation costs.
   *
   * <p>Suppose <tt>x</tt> is a list known to contain only strings.
   * The following code can be used to dump the list into a newly
   * allocated array of <tt>String</tt>:
   *
   * <pre>
   *     String[] y = x.toArray(new String[0]);</pre>
   *
   * Note that <tt>toArray(new Object[0])</tt> is identical in function to
   * <tt>toArray()</tt>.
   *
   * @param array the array into which the elements of this list are to
   *          be stored, if it is big enough; otherwise, a new array of the
   *          same runtime type is allocated for this purpose.
   * @return an array containing the elements of this list
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized Object[] toArray(final Object[] array) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.toArray(array);
    }
  }

  // Modification Operations
  /**
   * Appends the specified element to the end of this list (optional
   * operation).
   *
   * <p>Lists that support this operation may place limitations on what
   * elements may be added to this list.  In particular, some
   * lists will refuse to add null elements, and others will impose
   * restrictions on the type of elements that may be added.  List
   * classes should clearly specify in their documentation any restrictions
   * on what elements may be added.
   *
   * @param element element to be appended to this list
   * @return <tt>true</tt> (as specified by {@link Collection#add})
   */
  @Override
  @SuppressWarnings("unchecked")
  public synchronized boolean add(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.add(element);
    }
  }

  /**
   * Removes the first occurrence of the specified element from this list,
   * if it is present (optional operation).  If this list does not contain
   * the element, it is unchanged.  More formally, removes the element with
   * the lowest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
   * (if such an element exists).  Returns <tt>true</tt> if this list
   * contained the specified element (or equivalently, if this list changed
   * as a result of the call).
   *
   * @param element element to be removed from this list, if present
   * @return <tt>true</tt> if this list contained the specified element
   */
  @Override
  public synchronized boolean remove(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.remove(element);
    }
  }

  // Bulk Modification Operations
  /**
   * Returns <tt>true</tt> if this list contains all of the elements of the
   * specified collection.
   *
   * @param  collection collection to be checked for containment in this list
   * @return <tt>true</tt> if this list contains all of the elements of the
   *         specified collection
   * @see #contains(Object)
   */
  @SuppressWarnings("unchecked")
  public synchronized boolean containsAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.containsAll(collection);
    }
  }

  /**
   * Appends all of the elements in the specified collection to the end of
   * this list, in the order that they are returned by the specified
   * collection's iterator (optional operation).  The behavior of this
   * operation is undefined if the specified collection is modified while
   * the operation is in progress.  (Note that this will occur if the
   * specified collection is this list, and it's nonempty.)
   *
   * @param collection collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @see #add(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean addAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.addAll(collection);
    }
  }

  /**
   * Inserts all of the elements in the specified collection into this
   * list at the specified position (optional operation).  Shifts the
   * element currently at that position (if any) and any subsequent
   * elements to the right (increases their indices).  The new elements
   * will appear in this list in the order that they are returned by the
   * specified collection's iterator.  The behavior of this operation is
   * undefined if the specified collection is modified while the
   * operation is in progress.  (Note that this will occur if the specified
   * collection is this list, and it's nonempty.)
   *
   * @param index index at which to insert the first element from the
   *              specified collection
   * @param collection collection containing elements to be added to this list
   * @return <tt>true</tt> if this list changed as a result of the call
   */
  @SuppressWarnings("unchecked")
  public synchronized boolean addAll(final int index, final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.addAll(index, collection);
    }
  }

  /**
   * Removes from this list all of its elements that are contained in the
   * specified collection (optional operation).
   *
   * @param collection collection containing elements to be removed from this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @see #remove(Object)
   * @see #contains(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean removeAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.removeAll(collection);
    }
  }

  /**
   * Retains only the elements in this list that are contained in the
   * specified collection (optional operation).  In other words, removes
   * from this list all the elements that are not contained in the specified
   * collection.
   *
   * @param collection collection containing elements to be retained in this list
   * @return <tt>true</tt> if this list changed as a result of the call
   * @see #remove(Object)
   * @see #contains(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean retainAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.retainAll(collection);
    }
  }

  /**
   * Removes all of the elements from this list (optional operation).
   * The list will be empty after this call returns.
   */
  @Override
  public synchronized void clear() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      loadedList.clear();
    }
  }

  // Comparison and hashing
  /**
   * Compares the specified object with this list for equality.  Returns
   * <tt>true</tt> if and only if the specified object is also a list, both
   * lists have the same size, and all corresponding pairs of elements in
   * the two lists are <i>equal</i>.  (Two elements <tt>e1</tt> and
   * <tt>e2</tt> are <i>equal</i> if <tt>(e1==null ? e2==null :
   * e1.equals(e2))</tt>.)  In other words, two lists are defined to be
   * equal if they contain the same elements in the same order.  This
   * definition ensures that the equals method works properly across
   * different implementations of the <tt>List</tt> interface.
   *
   * @param obj the object to be compared for equality with this list
   * @return <tt>true</tt> if the specified object is equal to this list
   */
  @Override
  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public synchronized boolean equals(final Object obj) {
    if (isLoading) {
      return super.equals(obj);                              // NOPMD
    } else {
      loadTheList();
      return loadedList.equals(obj);
    }
  }

  /**
   * Returns the hash code value for this list.  The hash code of a list
   * is defined to be the result of the following calculation:
   * <pre>
   *  int hashCode = 1;
   *  Iterator&lt;E&gt; i = list.iterator();
   *  while (i.hasNext()) {
   *      E obj = i.next();
   *      hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
   *  }
   * </pre>
   * This ensures that <tt>list1.equals(list2)</tt> implies that
   * <tt>list1.hashCode()==list2.hashCode()</tt> for any two lists,
   * <tt>list1</tt> and <tt>list2</tt>, as required by the general
   * contract of {@link Object#hashCode}.
   *
   * @return the hash code value for this list
   * @see Object#equals(Object)
   * @see #equals(Object)
   */
  @Override
  public synchronized int hashCode() {
    if (isLoading) {
      return super.hashCode();                                       // NOPMD
    } else {
      loadTheList();
      return loadedList.hashCode();
    }
  }

  // Positional Access Operations
  /**
   * Returns the element at the specified position in this list.
   *
   * @param index index of the element to return
   * @return the element at the specified position in this list
   */
  @Override
  public synchronized Object get(final int index) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.get(index);
    }
  }

  /**
   * Replaces the element at the specified position in this list with the
   * specified element (optional operation).
   *
   * @param index index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized Object set(final int index, final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.set(index, element);
    }
  }

  /**
   * Inserts the specified element at the specified position in this list
   * (optional operation).  Shifts the element currently at that position
   * (if any) and any subsequent elements to the right (adds one to their
   * indices).
   *
   * @param index index at which the specified element is to be inserted
   * @param element element to be inserted
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized void add(final int index, final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      loadedList.add(index, element);
    }
  }

  /**
   * Removes the element at the specified position in this list (optional
   * operation).  Shifts any subsequent elements to the left (subtracts one
   * from their indices).  Returns the element that was removed from the
   * list.
   *
   * @param index the index of the element to be removed
   * @return the element previously at the specified position
   */
  @Override
  public synchronized Object remove(final int index) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.remove(index);
    }
  }

  // Search Operations
  /**
   * Returns the index of the first occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   * More formally, returns the lowest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   *
   * @param element element to search for
   * @return the index of the first occurrence of the specified element in
   *         this list, or -1 if this list does not contain the element
   */
  @Override
  public synchronized int indexOf(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.indexOf(element);
    }
  }

  /**
   * Returns the index of the last occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   * More formally, returns the highest index <tt>i</tt> such that
   * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
   * or -1 if there is no such index.
   *
   * @param element element to search for
   * @return the index of the last occurrence of the specified element in
   *         this list, or -1 if this list does not contain the element
   */
  @Override
  public synchronized int lastIndexOf(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.lastIndexOf(element);
    }
  }

  // List Iterators
  /**
   * Returns a list iterator over the elements in this list (in proper
   * sequence).
   *
   * @return a list iterator over the elements in this list (in proper
   *         sequence)
   */
  @Override
  public synchronized ListIterator<?> listIterator() {
    ListIterator<?> listIterator;
    if (isLoading) {
      listIterator = (new ArrayList(0)).listIterator();
    } else {
      loadTheList();
      listIterator = loadedList.listIterator();
    }
    return listIterator;
  }

  /**
   * Returns a list iterator of the elements in this list (in proper
   * sequence), starting at the specified position in this list.
   * The specified index indicates the first element that would be
   * returned by an initial call to {@link ListIterator#next next}.
   * An initial call to {@link ListIterator#previous previous} would
   * return the element with the specified index minus one.
   *
   * @param index index of first element to be returned from the
   *              list iterator (by a call to the <tt>next</tt> method)
   * @return a list iterator of the elements in this list (in proper
   *         sequence), starting at the specified position in this list
  final    */
  @Override
  public synchronized ListIterator<?> listIterator(final int index) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.listIterator(index);
    }
  }

  // View
  /**
   * Returns a view of the portion of this list between the specified
   * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.  (If
   * <tt>fromIndex</tt> and <tt>toIndex</tt> are equal, the returned list is
   * empty.)  The returned list is backed by this list, so non-structural
   * changes in the returned list are reflected in this list, and vice-versa.
   * The returned list supports all of the optional list operations supported
   * by this list.<p>
   *
   * This method eliminates the need for explicit range operations (of
   * the sort that commonly exist for arrays).   Any operation that expects
   * a list can be used as a range operation by passing a subList view
   * instead of a whole list.  For example, the following idiom
   * removes a range of elements from a list:
   * <pre>
   *      list.subList(from, to).clear();
   * </pre>
   * Similar idioms may be constructed for <tt>indexOf</tt> and
   * <tt>lastIndexOf</tt>, and all of the algorithms in the
   * <tt>Collections</tt> class can be applied to a subList.<p>
   *
   * The semantics of the list returned by this method become undefined if
   * the backing list (i.e., this list) is <i>structurally modified</i> in
   * any way other than via the returned list.  (Structural modifications are
   * those that change the size of this list, or otherwise perturb it in such
   * a fashion that iterations in progress may yield incorrect results.)
   *
   * @param fromIndex low endpoint (inclusive) of the subList
   * @param toIndex high endpoint (exclusive) of the subList
   * @return a view of the specified range within this list
   */
  @Override
  public synchronized List subList(final int fromIndex, final int toIndex) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheList();
      return loadedList.subList(fromIndex, toIndex);
    }
  }
}
