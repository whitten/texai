/*
 * NodeAccess.java
 *
 * Created on May 10, 2010, 7:44:57 AM
 *
 * Description: Provides access to persistent albus node objects.
 *
 * Copyright (C) May 10, 2010, Stephen L. Reed.
 *
 */
package org.texai.ahcsSupport;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFUtility;
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

    final URI predicate = RDFUtility.getDefaultPropertyURI(
            SkillClass.class.getName(), // className
            "skillClassName", // fieldName
            String.class); // fieldType
    final List<SkillClass> skillClasses = rdfEntityManager.find(
            predicate,
            skillClassName, // value
            SkillClass.class); // clazz

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
