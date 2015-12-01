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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.bouncycastle.util.Arrays;
import org.texai.ahcsSupport.ContainerInfoAccess;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.SingletonAgentHostsAccess;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.util.NetworkUtils;
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
  // the network name, mainnet or testnet
  private final String networkName;
  // the local node dictionary, container-name.node-name --> node
  private final Map<String, Node> nodeDictionary = new HashMap<>();
  // the local role dictionary, container-name.node-name.role-name --> role
  private final Map<String, Role> localRoleDictionary = new HashMap<>();
  // the executor
  private final ExecutorService executor = Executors.newCachedThreadPool();
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
  // the singleton agent hosts access object
  private final SingletonAgentHostsAccess singletonAgentHostsAccess;
  // the container infos access object
  private final ContainerInfoAccess containerInfoAccess;

  /**
   * Constructs a new BasicNodeRuntime instance.
   *
   * @param containerName the container name
   * @param networkName the network name, mainnet or testnet
   */
  public BasicNodeRuntime(
          final String containerName,
          final String networkName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "container name must be a non-empty string";
    assert NetworkUtils.TEXAI_MAINNET.equals(networkName) || NetworkUtils.TEXAI_TESTNET.equals(networkName) :
            "network name must be " + NetworkUtils.TEXAI_MAINNET + " or " + NetworkUtils.TEXAI_TESTNET;

    this.containerName = containerName;
    this.networkName = networkName;
    nodeAccess = new NodeAccess(rdfEntityManager);
    singletonAgentHostsAccess = new SingletonAgentHostsAccess(
            rdfEntityManager,
            this); // basicNodeRuntime
    singletonAgentHostsAccess.initializeSingletonAgentsHosts();
    containerInfoAccess = new ContainerInfoAccess(rdfEntityManager, networkName);
    containerInfoAccess.initializeContainerInfos();
  }

  /**
   * Gets the singleton agent hosts, whose dictionary allows host lookup for network singleton software agents, for example
   * NetworkOperationAgent.
   *
   * @return the singleton agent hosts
   */
  public SingletonAgentHosts getSingletonAgentHosts() {
    return singletonAgentHostsAccess.getEffectiveSingletonAgentHosts();
  }

  /**
   * Updates the singleton agent hosts.
   *
   * @param singletonAgentHosts the singleton agent hosts
   */
  public void updateSingletonAgentHosts(final SingletonAgentHosts singletonAgentHosts) {
    //Preconditions
    assert singletonAgentHosts != null : "singletonAgentHosts must not be null";

    singletonAgentHostsAccess.updateSingletonAgentHosts(singletonAgentHosts);
  }

  /**
   * Adds a new container information object, which contains information about super peer status and liveness.
   *
   * @param containerInfo the new container information object
   */
  public void addContainerInfo(final ContainerInfo containerInfo) {
    //Preconditions
    assert containerInfo != null : "containerInfo must not be null";

    containerInfoAccess.addContainerInfo(containerInfo);
  }

  /**
   * Gets the container info by its name.
   *
   * @param containerName the container name
   *
   * @return the container info
   */
  public ContainerInfo getContainerInfo(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must a non-empty string";

    return containerInfoAccess.getContainerInfo(containerName);
  }

  /**
   * Gets the container infos, each containing information about super peer status and liveness.
   *
   * @return the container infos
   */
  public List<ContainerInfo> getContainerInfos() {
    return containerInfoAccess.getContainerInfos();
  }

  /**
   * Replaces the current container infos with the given ones.
   *
   * @param containerInfos the given container infos
   */
  public void updateContainerInfos(final Collection<ContainerInfo> containerInfos) {
    //Preconditions
    assert containerInfos != null : "containerInfos must not be null";

    containerInfoAccess.updateContainerInfos(containerInfos);
  }

  /** Returns whether this container is a super-peer.
   *
   * @return whether this container is a super-peer
   */
  public boolean isSuperPeer() {
    final ContainerInfo containerInfo = getContainerInfo(containerName);
    if (containerInfo == null) {
      return false;
    } else {
      return containerInfo.isSuperPeer();
    }
  }

  /** Returns whether this container hosts an Insight blockchain explorer instance.
   *
   * @return whether this container hosts an Insight blockchain explorer instance
   */
  public boolean isBlockExplorer() {
    final ContainerInfo containerInfo = getContainerInfo(containerName);
    if (containerInfo == null) {
      return false;
    } else {
      return containerInfo.isBlockExplorer();
    }
  }

  /** Returns whether this container is a gateway for remote wallet connections.
   *
   * @return whether this container is a gateway for remote wallet connections
   */
  public boolean isClientGateway() {
    final ContainerInfo containerInfo = getContainerInfo(containerName);
    if (containerInfo == null) {
      return false;
    } else {
      return containerInfo.isClientGateway();
    }
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

  /**
   * Returns a formatted list of the keys of the localRoleDictionary.
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
  public RDFEntityManager getRDFEntityManager() {
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
    return new Timer();
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

  /**
   * Returns whether this is the first container in the network. If so, then all the network singletons are in this container, and the
   * procedure of joining the network can be skipped.
   *
   * @return whether this is the first container in the network
   */
  public boolean isFirstContainerInNetwork() {
    if (isFirstContainerInNetworkCached == null) {
      final ContainerInfo containerInfo = containerInfoAccess.getContainerInfo(containerName);
      assert containerInfo != null : "no containerInfo for containerName: " + containerName;
      isFirstContainerInNetworkCached = containerInfo.isFirstContainer();
    }
    return isFirstContainerInNetworkCached;
  }

  /**
   * Terminates this JVM with a normal exit code that causes the bash wrapper script to restart the Java application.
   */
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "DM_EXIT", justification = "desired behavior")
  public void restartJVM() {
    System.exit(0);
  }

  /**
   * Gets the network name, mainnet or testnet.
   *
   * @return the network name
   */
  public String getNetworkName() {
    return networkName;
  }

  /**
   * Gets the singleton agent hosts access object.
   *
   * @return the singleton agent hosts access object
   */
  public SingletonAgentHostsAccess getSingletonAgentHostsAccess() {
    return singletonAgentHostsAccess;
  }

  /**
   * Gets the container info access object.
   *
   * @return the container info access object
   */
  public ContainerInfoAccess getContainerInfoAccess() {
    return containerInfoAccess;
  }

}
