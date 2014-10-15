/*
 * NodeAccess.java
 *
 * Created on May 10, 2010, 7:44:57 AM
 *
 * Description: Provides access to persistent albus node objects.
 *
 * Copyright (C) May 10, 2010, Stephen L. Reed.
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
package org.texai.ahcsSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides access to persistent Albus node objects.
 *
 * @author reed
 */
@ThreadSafe
public final class NodeAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeAccess.class);
  // the RDF entity manager
  private final RDFEntityManager rdfEntityManager;

  /**
   * Creates a new NodeAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public NodeAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "RDFEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /**
   * Returns a list of the instantiated nodes as persisted in the repository.
   *
   * @return a list of the instantiated nodes as persisted in the repository
   */
  public synchronized List<Node> getNodes() {
    final List<Node> nodes = new ArrayList<>();
    final Iterator<Node> nodes_iter = rdfEntityManager.rdfEntityIterator(Node.class);
    while (nodes_iter.hasNext()) {
      final Node node = nodes_iter.next();
      node.instantiate();
      nodes.add(node);
    }
    return nodes;
  }

  /**
   * Persists the given node.
   *
   * @param node the given node
   */
  public synchronized void persistNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    node.cascadePersist(
            rdfEntityManager,
            null); // overrideContext
  }

  /**
   * Persists the given role.
   *
   * @param role the given role
   */
  public synchronized void persistRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    rdfEntityManager.persist(role);
  }

  /**
   * Returns the skill class having the given fully qualified name.
   *
   * @param skillClassName the given fully qualified name
   * @return the skill class having the given fully qualified name, or null if not found
   */
  public synchronized SkillClass findSkillClass(final String skillClassName) {
    //Preconditions
    assert skillClassName != null : "skillClassName must not be null";
    assert !skillClassName.isEmpty() : "skillClassName must not be empty";

    final List<SkillClass> skillClasses = rdfEntityManager.find(
            AHCSConstants.SKILL_CLASS_SKILL_CLASS_NAME_TERM,
            skillClassName,
            SkillClass.class);

    if (skillClasses.size() == 1) {
      return skillClasses.get(0);
    } else if (skillClasses.isEmpty()) {
      return null;
    } else {
      throw new TexaiException("unexpected duplicate skill class: " + skillClasses.toString());
    }
  }

  /**
   * Persists the given skill class.
   *
   * @param skillClass the given skill class
   */
  public synchronized void persistSkillClass(final SkillClass skillClass) {
    //Preconditions
    assert skillClass != null : "skillClass must not be null";

    rdfEntityManager.persist(skillClass);
  }

  /**
   * Removes the given skill class.
   *
   * @param skillClass the given skill class
   */
  public synchronized void removeSkillClass(final SkillClass skillClass) {
    //Preconditions
    assert skillClass != null : "skillClass must not be null";

    rdfEntityManager.remove(skillClass);
  }

  /**
   * Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  public RDFEntityManager getRDFEntityManager() {
    return rdfEntityManager;
  }
}
