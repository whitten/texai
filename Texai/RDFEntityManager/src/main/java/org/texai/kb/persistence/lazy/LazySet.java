/*
 * LazySet.java
 *
 * Created on November 28, 2006, 9:02 AM
 *
 * Description: Provides a means to lazily load a Set field.
 *
 * Copyright (C) 2006 Stephen L. Reed.
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
package org.texai.kb.persistence.lazy;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityLoader;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** Provides a facility that lazily loads a set field. The set value is loaded automatically from the RDF store when any of its methods are invoked.
 * The method call is delegated to the loaded set.  Subsequent references to the set field obtain the loaded set directly.  Note that because
 * not-yet-loaded lazy sets are not persisted to the RDF store, before they are copied into another persistent field they should first be
 * initialized (loaded) by invoking any of their defined methods (e.g. size()).
 *
 * @author reed
 */
@ThreadSafe
public final class LazySet implements Set, Serializable {

  /** the default serial version UID */
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
  private Set loadedSet;
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
  public LazySet(
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

  /** Gets the loaded set.
   *
   * @return the loaded set
   */
  public synchronized Set getLoadedSet() {
    if (isLoading) {
      return null;                                                   // NOPMD
    } else {
      loadTheSet();
      return loadedSet;
    }
  }

  /** Lazily loads the set. Also replaces the lazy set with the loaded set on the RDF entity so that subsequent
   * field value will directly access the set.
   */
  @SuppressWarnings("unchecked")                                     // NOPMD
  private synchronized void loadTheSet() {
    if (!isLoading && loadedSet == null) {
      isLoading = true;
      final RDFEntityLoader rdfEntityLoader = new RDFEntityLoader();
      // obtain a new repository connection from the named repository
      final RepositoryConnection repositoryConnection =
              DistributedRepositoryManager.getInstance().getRepositoryConnectionForRepositoryName(repositoryName);
      if (field == null) {
        try {
          field = rdfEntity.getClass().getField(fieldName);
        } catch (Exception ex) {
          final StringBuilder stringBuilder = new StringBuilder();
          stringBuilder.append("repositoryName: ");
          stringBuilder.append(repositoryName);
          stringBuilder.append("\nrdfEntity class: ");
          final Class<?> clazz = rdfEntity.getClass();
          stringBuilder.append(clazz.getName());
          stringBuilder.append("\nfieldName: '");
          stringBuilder.append(fieldName);
          stringBuilder.append("'\nrdfEntity: ");
          stringBuilder.append(rdfEntity);
          stringBuilder.append("\ndeclared fields...");
          for (final Field field1 : clazz.getDeclaredFields()) {
            stringBuilder.append("\nfield: ");
            stringBuilder.append(field1);
            stringBuilder.append("\n  field name: '");
            stringBuilder.append(field1.getName());
            stringBuilder.append("'");
            if (field1.getName().length() == fieldName.length()) {
              if (field1.getName().equals(fieldName)) {
                stringBuilder.append(" - equals fieldName");
              } else {
                StringUtils.logStringCharacterDifferences(field1.getName(), fieldName);
              }
            }
          }
          stringBuilder.append("\nexception: ");
          stringBuilder.append(ex.getClass().getName());
          stringBuilder.append(ex.getMessage());
          throw new TexaiException(stringBuilder.toString());
        }
      }
      loadedSet = (Set) rdfEntityLoader.loadLazyRDFEntityField(
              repositoryConnection,
              rdfEntity,
              field,
              rdfProperty,
              predicateValuesDictionary);
      assert loadedSet != null : "loadedSet must not be null";
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
    if (loadedSet == null) {
      string = "[LazySet for " + rdfProperty + "]";
    } else {
      string = loadedSet.toString();
    }
    return string;
  }

  // the Set methods
  /**
   * Returns the number of elements in this set (its cardinality).  If this
   * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this set (its cardinality).
   */
  @Override
  public synchronized int size() {
    int size;
    loadTheSet();
    if (isLoading) {
      size = 0;
    } else {
      size = loadedSet.size();
    }
    return size;
  }

  /**
   * Returns <tt>true</tt> if this set contains no elements.
   *
   * @return <tt>true</tt> if this set contains no elements.
   */
  @Override
  public synchronized boolean isEmpty() {
    boolean isEmpty;
    loadTheSet();
    if (isLoading) {
      isEmpty = true;
    } else {
      isEmpty = loadedSet.isEmpty();
    }
    return isEmpty;
  }

  /**
   * Returns <tt>true</tt> if this set contains the specified element.  More
   * formally, returns <tt>true</tt> if and only if this set contains an
   * element <code>e</code> such that <code>(o==null ? e==null :
   * o.equals(e))</code>.
   *
   * @param element element whose presence in this set is to be tested.
   * @return <tt>true</tt> if this set contains the specified element.
   */
  @Override
  public synchronized boolean contains(final Object element) {                       // NOPMD
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy set");      // NOPMD
    } else {
      loadTheSet();
      return loadedSet.contains(element);
    }
  }

  /**
   * Returns an iterator over the elements in this set.  The elements are
   * returned in no particular order (unless this set is an instance of some
   * class that provides a guarantee).
   *
   * @return an iterator over the elements in this set.
   */
  @Override
  public synchronized Iterator iterator() {
    Iterator iter;
    if (isLoading) {
      iter = (new ArrayList(0)).iterator();
    } else {
      loadTheSet();
      iter = loadedSet.iterator();
    }
    return iter;
  }

  /**
   * Returns an array containing all of the elements in this set.
   * Obeys the general contract of the <tt>Collection.toArray</tt> method.
   *
   * @return an array containing all of the elements in this set.
   */
  @Override
  public synchronized Object[] toArray() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.toArray();
    }
  }

  /**
   * Returns an array containing all of the elements in this set; the
   * runtime type of the returned array is that of the specified array.
   * Obeys the general contract of the
   * <tt>Collection.toArray(Object[])</tt> method.
   *
   * @param array the array into which the elements of this set are to
   * be stored, if it is big enough; otherwise, a new array of the
   * same runtime type is allocated for this purpose.
   * @return an array containing the elements of this set.
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized Object[] toArray(final Object[] array) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.toArray(array);
    }
  }

  /**
   * Adds the specified element to this set if it is not already present
   * (optional operation).  More formally, adds the specified element,
   * <code>o</code>, to this set if this set contains no element
   * <code>e</code> such that <code>(o==null ? e==null :
   * o.equals(e))</code>.  If this set already contains the specified
   * element, the call leaves this set unchanged and returns <tt>false</tt>.
   * In combination with the restriction on constructors, this ensures that
   * sets never contain duplicate elements.<p>
   *
   * The stipulation above does not imply that sets must accept all
   * elements; sets may refuse to add any particular element, including
   * <tt>null</tt>, and throwing an exception, as described in the
   * specification for <tt>Collection.add</tt>.  Individual set
   * implementations should clearly document any restrictions on the
   * elements that they may contain.
   *
   * @param element element to be added to this set.
   * @return <tt>true</tt> if this set did not already contain the specified
   *         element.
   *
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean add(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.add(element);
    }
  }

  /**
   * Removes the specified element from this set if it is present (optional
   * operation).  More formally, removes an element <code>e</code> such that
   * <code>(o==null ?  e==null : o.equals(e))</code>, if the set contains
   * such an element.  Returns <tt>true</tt> if the set contained the
   * specified element (or equivalently, if the set changed as a result of
   * the call).  (The set will not contain the specified element once the
   * call returns.)
   *
   * @param element object to be removed from this set, if present.
   * @return true if the set contained the specified element.
   */
  @Override
  public synchronized boolean remove(final Object element) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.remove(element);
    }
  }

  /**
   * Returns <tt>true</tt> if this set contains all of the elements of the
   * specified collection.  If the specified collection is also a set, this
   * method returns <tt>true</tt> if it is a <i>subset</i> of this set.
   *
   * @param  collection collection to be checked for containment in this set.
   * @return <tt>true</tt> if this set contains all of the elements of the
   * specified collection.
   * @see    #contains(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean containsAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.containsAll(collection);
    }
  }

  /**
   * Adds all of the elements in the specified collection to this set if
   * they're not already present (optional operation).  If the specified
   * collection is also a set, the <tt>addAll</tt> operation effectively
   * modifies this set so that its value is the <i>union</i> of the two
   * sets.  The behavior of this operation is unspecified if the specified
   * collection is modified while the operation is in progress.
   *
   * @param collection collection whose elements are to be added to this set.
   * @return <tt>true</tt> if this set changed as a result of the call.
   *
   * @see #add(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean addAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.addAll(collection);
    }
  }

  /**
   * Retains only the elements in this set that are contained in the
   * specified collection (optional operation).  In other words, removes
   * from this set all of its elements that are not contained in the
   * specified collection.  If the specified collection is also a set, this
   * operation effectively modifies this set so that its value is the
   * <i>intersection</i> of the two sets.
   *
   * @param collection collection that defines which elements this set will retain.
   * @return <tt>true</tt> if this collection changed as a result of the
   * call.
   * @see #remove(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean retainAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.retainAll(collection);
    }
  }

  /**
   * Removes from this set all of its elements that are contained in the
   * specified collection (optional operation).  If the specified
   * collection is also a set, this operation effectively modifies this
   * set so that its value is the <i>asymmetric set difference</i> of
   * the two sets.
   *
   * @param  collection collection that defines which elements will be removed from
   * this set.
   * @return <tt>true</tt> if this set changed as a result of the call.
   * @see    #remove(Object)
   */
  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean removeAll(final Collection collection) {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      return loadedSet.removeAll(collection);
    }
  }

  /**
   * Removes all of the elements from this set (optional operation).
   * This set will be empty after this call returns (unless it throws an
   * exception).
   */
  @Override
  public synchronized void clear() {
    if (isLoading) {
      throw new TexaiException("recursive call while loading lazy list");
    } else {
      loadTheSet();
      loadedSet.clear();
    }
  }

  /**
   * Compares the specified object with this set for equality.  Returns
   * <tt>true</tt> if the specified object is also a set, the two sets
   * have the same size, and every member of the specified set is
   * contained in this set (or equivalently, every member of this set is
   * contained in the specified set).  This definition ensures that the
   * equals method works properly across different implementations of the
   * set interface.
   *
   * @param obj Object to be compared for equality with this set.
   * @return <tt>true</tt> if the specified Object is equal to this set.
   */
  @Override
  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  public synchronized boolean equals(final Object obj) {
    boolean isEqual;
    if (isLoading) {
      isEqual = super.equals(obj);
    } else {
      loadTheSet();
      isEqual = loadedSet.equals(obj);
    }
    return isEqual;
  }

  /**
   *
   * Returns the hash code value for this set.  The hash code of a set is
   * defined to be the sum of the hash codes of the elements in the set,
   * where the hashcode of a <tt>null</tt> element is defined to be zero.
   * This ensures that <code>s1.equals(s2)</code> implies that
   * <code>s1.hashCode()==s2.hashCode()</code> for any two sets
   * <code>s1</code> and <code>s2</code>, as required by the general
   * contract of the <tt>Object.hashCode</tt> method.
   *
   * @return the hash code value for this set.
   * @see Object#hashCode()
   * @see Object#equals(Object)
   * @see Set#equals(Object)
   */
  @Override
  public synchronized int hashCode() {
    int hashCode;
    if (isLoading) {
      hashCode = super.hashCode();
    } else {
      loadTheSet();
      hashCode = loadedSet.hashCode();
    }
    return hashCode;
  }
}
