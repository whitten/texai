/*
 * WorkListAccess.java
 *
 * Created on Jun 30, 2009, 5:39:35 PM
 *
 * Description: Provides access to work flow lists.
 *
 * Copyright (C) Jun 30, 2009 Stephen L. Reed.
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
package org.texai.workflow;

import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.workflow.domainEntity.WorkList;

/** Provides access to work flow lists.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class WorkListAccess {

  /** the term texai:workListPurposeTerm */
  private static final URI WORK_LIST_PURPOSE_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "workListPurposeTerm");
  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;

  /** Constructs a new WorkListAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public WorkListAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Returns the work list having the given purpose term, or null if not found.
   *
   * @param purposeTerm the given purpose term
   * @return the work list having the given purpose term, or null if not found
   */
  public WorkList findWorkList(final URI purposeTerm) {
    //Preconditions
    assert purposeTerm != null : "purposeTerm must not be null";

    final List<WorkList> workLists = rdfEntityManager.find(
            WORK_LIST_PURPOSE_TERM,
            purposeTerm,
            WorkList.class);
    assert workLists.size() <= 1;
    if (workLists.isEmpty()) {
      return null;
    } else {
      return workLists.get(0);
    }
  }

  /** Finds an existing work list, or creates a new work list, for the given purpose term.
   *
   * @param purposeTerm the given purpose term
   * @return the work list
   */
  public WorkList findOrCreateWorkList(final URI purposeTerm) {
    //Preconditions
    assert purposeTerm != null : "purposeTerm must not be null";

    WorkList workList = findWorkList(purposeTerm);
    if (workList == null) {
      workList = new WorkList(purposeTerm);
      rdfEntityManager.persist(workList);
    }
    return workList;
  }

  /** Removes the work list having the given purpose term.
   *
   * @param purposeTerm the given purpose term
   */
  public void removeWorkList(final URI purposeTerm) {
    //Preconditions
    assert purposeTerm != null : "purposeTerm must not be null";

    final List<WorkList> workLists = rdfEntityManager.find(
            WORK_LIST_PURPOSE_TERM,
            purposeTerm,
            WorkList.class);
    assert workLists.size() <= 1;
    if (workLists.isEmpty()) {
      return;
    } else {
      rdfEntityManager.remove(workLists.get(0));
    }
  }
}
