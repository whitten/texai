/*
 * WorkList.java
 *
 * Created on Jun 9, 2009, 2:00:26 PM
 *
 * Description: Provides a workflow work list.
 *
 * Copyright (C) Jun 9, 2009 Stephen L. Reed.
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
package org.texai.workflow.domainEntity;

import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;

/**  Provides a workflow work list.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(context = "texai:WorkFlowContext")
@ThreadSafe
public class WorkList implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the head of the linked list of work flow items, or null if the list is empty */
  @RDFProperty(predicate = "texai:workListHeadWorkFlowItem")
  private AbstractWorkFlowItem headWorkFlowItem;
  /** the tail of the linked list of work flow items, or null if the list is empty */
  @RDFProperty(predicate = "texai:workListTailWorkFlowItem")
  private AbstractWorkFlowItem tailWorkFlowItem;
  /** the concept term that represents the purpose of this work flow list */
  @RDFProperty(predicate = "texai:workListPurposeTerm")
  private final URI purposeTerm;
  /** the work flow item list size */
  @RDFProperty(predicate = "texai:workListSize")
  private int size = 0;

  /** Constructs a new WorkList instance. */
  public WorkList() {
    purposeTerm = null;
  }

  /** Constructs a new WorkList instance.
   *
   * @param purposeTerm the concept term that represents the purpose of this work flow list
   */
  public WorkList(final URI purposeTerm) {
    //Preconditions
    assert purposeTerm != null : "purposeTerm must not be null";

    this.purposeTerm = purposeTerm;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the head of the linked list of work flow items, or null if the list is empty.
   *
   * @return the head of the linked list of work flow items, or null if the list is empty
   */
  public synchronized AbstractWorkFlowItem getHeadWorkFlowItem() {
    return headWorkFlowItem;
  }

  /** Sets the head of the linked list of work flow items, or null if the list is empty.
   *
   * @param headWorkFlowItem the head of the linked list of work flow items, or null if the list is empty
   */
  public synchronized void setHeadWorkFlowItem(final AbstractWorkFlowItem headWorkFlowItem) {
    this.headWorkFlowItem = headWorkFlowItem;
  }

  /** Gets the tail of the linked list of work flow items, or null if the list is empty.
   *
   * @return the tail of the linked list of work flow items, or null if the list is empty
   */
  public synchronized AbstractWorkFlowItem getTailWorkFlowItem() {
    return tailWorkFlowItem;
  }

  /** Sets the tail of the linked list of work flow items, or null if the list is empty.
   *
   * @param tailWorkFlowItem the tail of the linked list of work flow items, or null if the list is empty
   */
  public synchronized void setTailWorkFlowItem(final AbstractWorkFlowItem tailWorkFlowItem) {
    this.tailWorkFlowItem = tailWorkFlowItem;
  }

  /** Gets the concept term that represents the purpose of this work flow list.
   *
   * @return the concept term that represents the purpose of this work flow list.
   */
  public URI getPurposeTerm() {
    return purposeTerm;
  }

  /**
   * Returns the number of elements in this list.  If this list contains
   * more than <tt>Integer.MAX_VALUE</tt> elements, returns
   * <tt>Integer.MAX_VALUE</tt>.
   *
   * @return the number of elements in this list
   */
  public synchronized int size() {
    return size;
  }

  /**
   * Returns <tt>true</tt> if this list contains no elements.
   *
   * @return <tt>true</tt> if this list contains no elements
   */
  public synchronized boolean isEmpty() {
    return size == 0;
  }

  /**
   * Returns <tt>true</tt> if this list contains the specified element.
   *
   * @param workFlowItem element whose presence in this list is to be tested
   * @return <tt>true</tt> if this list contains the specified element
   */
  public synchronized boolean contains(final AbstractWorkFlowItem workFlowItem) {
    //Preconditions
    assert workFlowItem != null : "workFlowItem must not be null";

    return indexOf(workFlowItem) != -1;
  }

  /**
   * Removes the specified item.
   *
   * @param aWorkFlowItem the specified item
   */
  public synchronized void remove(final AbstractWorkFlowItem aWorkFlowItem) {
    //Preconditions
    assert aWorkFlowItem != null : "aWorkFlowItem must not be null";

    int index = 0;
    AbstractWorkFlowItem workFlowItem = headWorkFlowItem;
    while (true) {
      if (workFlowItem == null) {
        index = -1;
        break;
      } else if (workFlowItem.equals(aWorkFlowItem)) {
        break;
      } else {
        workFlowItem = workFlowItem.getNextWorkFlowItem();
        index++;
      }
    }

    if (index == -1) {
      return;
    } else if (index == 0) {
      removeFirst();
    } else if (index == (size - 1)) {
      removeLast();
    } else {
      // cannot be the first item, so start comparing at the second item
      workFlowItem = headWorkFlowItem.getNextWorkFlowItem();
      while (true) {
        assert workFlowItem != null;
        if (workFlowItem.equals(aWorkFlowItem)) {
          size--;
          final AbstractWorkFlowItem previousWorkFlowItem = workFlowItem.getPreviousWorkFlowItem();
          assert previousWorkFlowItem != null;
          final AbstractWorkFlowItem nextWorkFlowItem = workFlowItem.getNextWorkFlowItem();
          assert nextWorkFlowItem != null;
          previousWorkFlowItem.setNextWorkFlowItem(nextWorkFlowItem);
          nextWorkFlowItem.setPreviousWorkFlowItem(previousWorkFlowItem);
          break;
        } else {
          workFlowItem = workFlowItem.getNextWorkFlowItem();
        }
      }
    }
  }

  /** Returns an iterator over the work flow item list.
   *
   * @return an iterator over the work flow item list
   */
  public synchronized Iterator<AbstractWorkFlowItem> iterator() {
    return new WorkFlowItemIterator(headWorkFlowItem);
  }

  /** Provides a work flow iterm iterator. */
  private static final class WorkFlowItemIterator implements Iterator<AbstractWorkFlowItem> {

    /** the work flow item */
    private AbstractWorkFlowItem workFlowItem;

    /** Constructs a new WorkFlowItemIterator instance.
     *
     * @param headWorkFlowItem the head of the linked list of work flow items, or null if the list is empty
     */
    public WorkFlowItemIterator(final AbstractWorkFlowItem headWorkFlowItem) {
      workFlowItem = headWorkFlowItem;
    }

    /** Returns <tt>true</tt> if the iteration has more elements. (In other
     * words, returns <tt>true</tt> if <tt>next</tt> would return an element
     * rather than throwing an exception.)
     *
     * @return <tt>true</tt> if the iterator has more elements.
     */
    @Override
    public boolean hasNext() {
      return workFlowItem != null;
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration.
     */
    @Override
    public AbstractWorkFlowItem next() {
      if (workFlowItem == null) {
        throw new NoSuchElementException();
      }
      final AbstractWorkFlowItem workFlowItem1 = workFlowItem;
      workFlowItem = workFlowItem.getNextWorkFlowItem();
      return workFlowItem1;
    }

    /** Removes from the underlying collection the last element returned by the
     * iterator (optional operation).  This method can be called only once per
     * call to <tt>next</tt>.  The behavior of an iterator is unspecified if
     * the underlying collection is modified while the iteration is in
     * progress in any way other than by calling this method.
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException("Not supported yet.");
    }
  }

  /** Adds the given work flow item to its priorized position in the work list.
   *
   * @param aWorkFlowItem the given work flow item
   */
  public synchronized void add(final AbstractWorkFlowItem aWorkFlowItem) {
    //Preconditions
    assert aWorkFlowItem != null : "aWorkFlowItem must not be null";

    aWorkFlowItem.initialize();
    aWorkFlowItem.setWorkList(this);
    size++;
    if (headWorkFlowItem == null) {
      // empty list
      assert tailWorkFlowItem == null;
      headWorkFlowItem = aWorkFlowItem;
      tailWorkFlowItem = aWorkFlowItem;
      return;
    } else if (aWorkFlowItem.compareTo(headWorkFlowItem) == -1) {
      // new head of the list
      aWorkFlowItem.setNextWorkFlowItem(headWorkFlowItem);
      assert headWorkFlowItem.getPreviousWorkFlowItem() == null;
      headWorkFlowItem.setPreviousWorkFlowItem(aWorkFlowItem);
      headWorkFlowItem = aWorkFlowItem;
      return;
    }

    AbstractWorkFlowItem previousWorkFlowItem = headWorkFlowItem;
    AbstractWorkFlowItem nextWorkFlowItem = headWorkFlowItem.getNextWorkFlowItem();
    while (true) {
      if (nextWorkFlowItem == null) {
        // new end of the list
        aWorkFlowItem.setPreviousWorkFlowItem(previousWorkFlowItem);
        previousWorkFlowItem.setNextWorkFlowItem(aWorkFlowItem);
        tailWorkFlowItem = aWorkFlowItem;
        return;
      } else if (aWorkFlowItem.compareTo(nextWorkFlowItem) <= 0) {
        // link the given work flow item between the previous and next items
        aWorkFlowItem.setPreviousWorkFlowItem(previousWorkFlowItem);
        aWorkFlowItem.setNextWorkFlowItem(nextWorkFlowItem);
        previousWorkFlowItem.setNextWorkFlowItem(aWorkFlowItem);
        nextWorkFlowItem.setPreviousWorkFlowItem(aWorkFlowItem);
        return;
      } else {
        // advance to the next linked item
        previousWorkFlowItem = nextWorkFlowItem;
        nextWorkFlowItem = nextWorkFlowItem.getNextWorkFlowItem();
      }
    }
  }

  /** Adds the given work flow item to the head of the work list.
   *
   * @param aWorkFlowItem the given work flow item
   */
  public synchronized void addFirst(final AbstractWorkFlowItem aWorkFlowItem) {
    //Preconditions
    assert aWorkFlowItem != null : "aWorkFlowItem must not be null";

    aWorkFlowItem.initialize();
    aWorkFlowItem.setWorkList(this);
    size++;
    if (headWorkFlowItem == null) {
      // empty list
      assert tailWorkFlowItem == null;
      headWorkFlowItem = aWorkFlowItem;
      tailWorkFlowItem = aWorkFlowItem;
      return;
    } else {
      // new head of the list
      aWorkFlowItem.setNextWorkFlowItem(headWorkFlowItem);
      assert headWorkFlowItem.getPreviousWorkFlowItem() == null;
      headWorkFlowItem.setPreviousWorkFlowItem(aWorkFlowItem);
      headWorkFlowItem = aWorkFlowItem;
      return;
    }
  }

  /** Adds the given work flow item to the tail of the work list.
   *
   * @param aWorkFlowItem the given work flow item
   */
  public synchronized void addLast(final AbstractWorkFlowItem aWorkFlowItem) {
    //Preconditions
    assert aWorkFlowItem != null : "aWorkFlowItem must not be null";

    aWorkFlowItem.initialize();
    aWorkFlowItem.setWorkList(this);
    size++;
    if (headWorkFlowItem == null) {
      // empty list
      assert tailWorkFlowItem == null;
      headWorkFlowItem = aWorkFlowItem;
      tailWorkFlowItem = aWorkFlowItem;
      return;
    } else {
      // new tail of the list
      aWorkFlowItem.setPreviousWorkFlowItem(tailWorkFlowItem);
      assert tailWorkFlowItem.getNextWorkFlowItem() == null;
      tailWorkFlowItem.setNextWorkFlowItem(aWorkFlowItem);
      tailWorkFlowItem = aWorkFlowItem;
      return;
    }
  }

  /**
   * Returns the index of the first occurrence of the specified element
   * in this list, or -1 if this list does not contain the element.
   *
   * @param aWorkFlowItem element to search for
   * @return the index of the first occurrence of the specified element in
   *         this list, or -1 if this list does not contain the element
   */
  private int indexOf(final AbstractWorkFlowItem aWorkFlowItem) {
    //Preconditions
    assert aWorkFlowItem != null : "aWorkFlowItem must not be null";

    int index = 0;
    AbstractWorkFlowItem workFlowItem = headWorkFlowItem;
    while (true) {
      if (workFlowItem == null) {
        return -1;
      } else if (workFlowItem.equals(aWorkFlowItem)) {
        return index;
      } else {
        workFlowItem = workFlowItem.getNextWorkFlowItem();
        index++;
      }
    }
  }

  /** Unlinks and returns the first item of the work flow item list.
   *
   * @return the first item of the work flow item list
   */
  public synchronized AbstractWorkFlowItem removeFirst() {
    if (headWorkFlowItem == null) {
      return null;
    } else {
      size--;
      final AbstractWorkFlowItem workFlowItem = headWorkFlowItem;
      headWorkFlowItem = workFlowItem.getNextWorkFlowItem();
      if (headWorkFlowItem == null) {
        tailWorkFlowItem = null;
      } else {
        headWorkFlowItem.setPreviousWorkFlowItem(null);
      }
      workFlowItem.setPreviousWorkFlowItem(null);
      workFlowItem.setNextWorkFlowItem(null);
      return workFlowItem;
    }
  }

  /** Unlinks and returns the last item of the work flow item list.
   *
   * @return the last item of the work flow item list
   */
  public synchronized AbstractWorkFlowItem removeLast() {
    if (headWorkFlowItem == null) {
      return null;
    } else {
      size--;
      final AbstractWorkFlowItem workFlowItem = tailWorkFlowItem;
      tailWorkFlowItem = workFlowItem.getPreviousWorkFlowItem();
      if (tailWorkFlowItem == null) {
        headWorkFlowItem = null;
      } else {
        tailWorkFlowItem.setNextWorkFlowItem(null);
      }
      return workFlowItem;
    }
  }

  /** Returns the head of the linked work item list without removing it.
   *
   * @return  the head of the linked work item list without removing it
   */
  public synchronized AbstractWorkFlowItem peekFirst() {
    return headWorkFlowItem;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[WorkList " + RDFUtility.formatURIAsTurtle(purposeTerm) + " size: " + size + "]";
  }
}
