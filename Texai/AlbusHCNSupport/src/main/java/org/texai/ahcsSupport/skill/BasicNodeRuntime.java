/*
 * BasicNodeRuntime.java
 *
 * Created on Mar 12, 2010, 12:12:19 PM
 *
 * Description: Provides basic node runtime support. The BasicNodeRuntime class is defined in a downstream package and extends
 * this class.
 *
 * Copyright (C) Mar 12, 2010 reed.
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
package org.texai.ahcsSupport.skill;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides runtime support for nodes in the local JVM.
 *
 * @author reed
 */
public class BasicNodeRuntime implements MessageDispatcher {

  // the node runtime RDF entity manager
  private final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  // the node access object
  private final NodeAccess nodeAccess;
  // the container name
  private final String containerName;
  // the local node dictionary, container-name.node-name --> node
  private final Map<String, Node> nodeDictionary = new HashMap<>();
  // the local role dictionary, container-name.node-name.role-name --> role
  private final Map<String, Role> localRoleDictionary = new HashMap<>();
  // the executor
  private final ExecutorService executor = Executors.newCachedThreadPool();
  // the timer
  private final Timer timer = new Timer();
  // the operations to be logged
  private final Set<String> loggedOperations = new HashSet<>();
  // The NodeRuntimeSkill instance which is used to send and receive messages on behalf of this node runtime
  private AbstractSkill nodeRuntimeSkill;

  /**
   * Constructs a new BasicNodeRuntime instance.
   *
   * @param containerName the container name
   */
  public BasicNodeRuntime(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "container name must be a non-empty string";

    this.containerName = containerName;
    nodeAccess = new NodeAccess(rdfEntityManager);
  }

  /**
   * Gets the container name.
   *
   * @return the docker container name
   */
  public String getContainerName() {
    return containerName;
  }

  /**
   * Gets an unmodifiable copy of the local nodes.
   *
   * @return the local nodes
   */
  public Set<Node> getNodes() {
    return Collections.unmodifiableSet(new HashSet<Node>(nodeDictionary.values()));
  }

  /**
   * Registers the given role.
   *
   * @param role the given role
   */
  public void registerRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    synchronized (localRoleDictionary) {
      localRoleDictionary.put(role.getQualifiedName(), role);
    }
  }

  /**
   * Gets the local role having the given qualified name, i.e. container-name.agent-name.role-name.
   *
   * @param qualifiedName the given qualified name, i.e. container-name.agent-name.role-name
   *
   * @return the local role, or null if not found
   */
  public Role getLocalRole(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    synchronized (localRoleDictionary) {
      return localRoleDictionary.get(qualifiedName);
    }
  }

  /**
   * Gets the node runtime RDF entity manager.
   *
   * @return the node runtime RDF entity manager
   */
  public RDFEntityManager getRdfEntityManager() {
    return rdfEntityManager;
  }

  /**
   * Gets the node access object.
   *
   * @return the node access object
   */
  public NodeAccess getNodeAccess() {
    return nodeAccess;
  }

  /**
   * Gets the executor.
   *
   * @return the executor
   */
  public Executor getExecutor() {
    return executor;
  }

  /**
   * Gets the timer.
   *
   * @return the timer
   */
  public Timer getTimer() {
    return timer;
  }

  /**
   * Adds the given operation to the list of logged operations.
   *
   * @param operation the given operation
   */
  public void addLoggedOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (loggedOperations) {
      loggedOperations.add(operation);
    }
  }

  /**
   * Removes the given operation from the list of logged operations.
   *
   * @param operation the given operation
   */
  public void removeLoggedOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (loggedOperations) {
      loggedOperations.remove(operation);
    }
  }

  /**
   * Returns whether the given message is to be logged.
   *
   * @param message the given message
   *
   * @return whether the given message is to be logged
   */
  public boolean isMessageLogged(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    synchronized (loggedOperations) {
      return loggedOperations.contains(message.getOperation());
    }
  }

  /**
   * Dispatches a message in an Albus hierarchical control system.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchMessage(final Message message) {
    throw new TexaiException("subclasses must implement this method");
  }

  /**
   * Sets the NodeRuntimeSkill instance which is used to send and receive
   * messages on behalf of this node runtime.
   *
   * @param nodeRuntimeSkill the NodeRuntimeSkill instance
   */
  public void setNodeRuntimeSkill(AbstractSkill nodeRuntimeSkill) {
    //Preconditions
    assert nodeRuntimeSkill != null : "nodeRuntimeSkill must not be null";

    this.nodeRuntimeSkill = nodeRuntimeSkill;
  }

  /**
   * Returns the NodeRuntimeSkill instance which is used to send and receive
   * messages on behalf of this node runtime.
   *
   * @return the NodeRuntimeSkill instance
   */
  public AbstractSkill getNodeRuntimeSkill() {
    return nodeRuntimeSkill;
  }

}
