/*
 * WorkFlowItem.java
 *
 * Created on Jun 9, 2009, 2:00:56 PM
 *
 * Description: Provides a comparable workflow item that can be linked into a work list.
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


import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/** Provides a comparable workflow item.
 *
 * @author Stephen L. Reed
 */
@RDFEntity(namespaces = {
@RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
@RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)
}, subject = "texai:org.texai.workflow.domainEntity.AbstractWorkFlowItem", type = "cyc:ObjectType", subClassOf = "cyc:AbstractInformationStructure", context = "texai:WorkFlowContext")
@NotThreadSafe
public abstract class AbstractWorkFlowItem implements Comparable<AbstractWorkFlowItem>, RDFPersistent {

  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the workflow work list containing this item */
  @RDFProperty(predicate = "texai:workFlowItemWorkList")
  private WorkList workList;
  /** the previous workflow item in this work list, or null if this is the first item */
  @RDFProperty(predicate = "texai:workFlowItemPreviousItem")
  private AbstractWorkFlowItem previousWorkFlowItem;
  /** the next workflow item in this work list, or null if this is the last item */
  @RDFProperty(predicate = "texai:workFlowItemNextItem")
  private AbstractWorkFlowItem nextWorkFlowItem;


  /** Constructs a new WorkFlowItem instance. */
  public AbstractWorkFlowItem() {
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Initializes this workflow item. */
  public void initialize() {
    workList = null;
    previousWorkFlowItem = null;
    nextWorkFlowItem = null;
  }

  /** Compares this work flow item with another one, returning -1 if this one is less than, 0 if equal, otherwise returning +1.
   *
   * @param that the other work flow iterm
   * @return -1 if this one is less than, 0 if equal to, otherwise returning +1
   */
  @Override
  public abstract int compareTo(final AbstractWorkFlowItem that);

  /** Gets the workflow work list containing this item.
   *
   * @return the workflow work list containing this item
   */
  public WorkList getWorkList() {
    return workList;
  }

  /** Sets the workflow work list containing this item.
   *
   * @param workList the workflow work list containing this item
   */
  public void setWorkList(final WorkList workList) {
    //Preconditions
    assert workList != null : "workList must not be null";

    this.workList = workList;
  }

  /** Gets the previous workflow item in this work list, or null if this is the first item.
   *
   * @return the previous workflow item in this work list, or null if this is the first item
   */
  public AbstractWorkFlowItem getPreviousWorkFlowItem() {
    return previousWorkFlowItem;
  }

  /** Sets the previous workflow item in this work list, or null if this is the first item.
   *
   * @param previousWorkFlowItem the previous workflow item in this work list, or null if this is the first item
   */
  public void setPreviousWorkFlowItem(final AbstractWorkFlowItem previousWorkFlowItem) {
    this.previousWorkFlowItem = previousWorkFlowItem;
  }

  /** Gets the next workflow item in this work list, or null if this is the last item.
   *
   * @return the next workflow item in this work list, or null if this is the last item
   */
  public AbstractWorkFlowItem getNextWorkFlowItem() {
    return nextWorkFlowItem;
  }

  /** Sets the next workflow item in this work list, or null if this is the last item.
   *
   * @param nextWorkFlowItem the next workflow item in this work list
   */
  public void setNextWorkFlowItem(final AbstractWorkFlowItem nextWorkFlowItem) {
    this.nextWorkFlowItem = nextWorkFlowItem;
  }

}
