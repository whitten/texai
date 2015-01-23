/*
 * BasicNodeRuntime.java
 *
 * Created on Mar 12, 2010, 12:12:19 PM
 *
 * Description: Provides basic node runtime support. The NodeRuntime class is defined in a downstream package and extends
 * this class.
 *
 * Copyright (C) Mar 12, 2010 reed.
 */
package org.texai.ahcsSupport.skill;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bouncycastle.util.Arrays;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

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
  // the operations to be filtered from logging
  private final Set<String> filteredOperations = new HashSet<>();
  // The NodeRuntimeSkill instance which is used to send and receive messages on behalf of this node runtime
  private AbstractSkill nodeRuntimeSkill;
  // the key store for certain roles' X.509Certificates and the private keys
  private KeyStore keyStore;
  // the key store password
  private char[] keyStorePassword;
  // the cached value of isFirstContainerInNetwork()
  private Boolean isFirstContainerInNetworkCached = null;

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
   * Gets the key store.
   *
   * @return the key store
   */
  public KeyStore getKeyStore() {
    return keyStore;
  }

  /**
   * Sets the key store.
   *
   * @param keyStore the given key store
   */
  public void setKeyStore(final KeyStore keyStore) {
    //Preconditions
    assert keyStore != null : "keyStore must not be null";

    this.keyStore = keyStore;
  }

  /**
   * Gets the key store password.
   *
   * @return the key store password
   */
  public char[] getKeyStorePassword() {
    return Arrays.copyOf(keyStorePassword, keyStorePassword.length);
  }

  /**
   * Sets the key store password. A copy is used to preserve immutability.
   *
   * @param keyStorePassword the given key store password
   */
  public void setKeyStorePassword(final char[] keyStorePassword) {
    //Preconditions
    assert keyStorePassword != null : "keyStorePassword must not be null";

    this.keyStorePassword = Arrays.copyOf(keyStorePassword, keyStorePassword.length);
  }

  /**
   * Gets X.509 security information for the given local role.
   *
   * @param role the given local role
   *
   * @return the X.509 security information
   */
  public X509SecurityInfo getX509SecurityInfo(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";
    assert localRoleDictionary.containsKey(role.getQualifiedName()) : "role must be a local role";
    assert role.areRemoteCommunicationsPermitted() : "role must be permitted to perform remote communications";
    try {
      assert getKeyStore().containsAlias(role.getQualifiedName()) : "no key store entry for " + role;
    } catch (KeyStoreException ex) {
      throw new TexaiException(ex);
    }

    return X509Utils.getX509SecurityInfo(
            getKeyStore(),
            getKeyStorePassword(),
            role.getQualifiedName()); // alias
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

  /** Returns a formatted list of the keys of the localRoleDictionary.
   *
   * @return a formatted list of the keys of the localRoleDictionary
   */
  public String formatLocalRoles() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("local role dictionary entries ...");
    synchronized (localRoleDictionary) {
      localRoleDictionary.keySet().stream().sorted().forEach(qualifiedName -> {
        stringBuilder.append("  ").append(qualifiedName).append('\n');
      });
    }
    return stringBuilder.toString();
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
   * Adds the given operation to the list of operations to filtered from logging.
   *
   * @param operation the given operation
   */
  public void addFilteredOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (filteredOperations) {
      filteredOperations.add(operation);
    }
  }

  /**
   * Removes the given operation from the list of operations to filtered from logging.
   *
   * @param operation the given operation
   */
  public void removeFilteredOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (filteredOperations) {
      filteredOperations.remove(operation);
    }
  }

  /**
   * Returns whether the given message is to be filtered from logging.
   *
   * @param message the given message
   *
   * @return whether the given message is to be filtered from logging
   */
  public boolean isMessageFiltered(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    synchronized (filteredOperations) {
      return filteredOperations.contains(message.getOperation());
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
   * Sets the NodeRuntimeSkill instance which is used to send and receive messages on behalf of this node runtime.
   *
   * @param nodeRuntimeSkill the NodeRuntimeSkill instance
   */
  public void setNodeRuntimeSkill(AbstractSkill nodeRuntimeSkill) {
    //Preconditions
    assert nodeRuntimeSkill != null : "nodeRuntimeSkill must not be null";

    this.nodeRuntimeSkill = nodeRuntimeSkill;
  }

  /**
   * Returns the NodeRuntimeSkill instance which is used to send and receive messages on behalf of this node runtime.
   *
   * @return the NodeRuntimeSkill instance
   */
  public AbstractSkill getNodeRuntimeSkill() {
    return nodeRuntimeSkill;
  }

  /** Returns whether this is the first container in the network. If so, then all the network singletons are in this container,
   * and the procedure of joining the network can be skipped.
   *
   * @return whether this is the first container in the network
   */
  public boolean isFirstContainerInNetwork() {
    if (isFirstContainerInNetworkCached == null) {
      final String isFirstContainerInNetwork = System.getenv("FIRST_CONTAINER");
      isFirstContainerInNetworkCached = "true".equals(isFirstContainerInNetwork);
    }
    return isFirstContainerInNetworkCached;
  }

  /** Terminates this JVM with an exit code that causes the bash wrapper script to restart the Java application. */
  public void restartJVM() {
    System.exit(100);
  }

}
