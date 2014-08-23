/*
 * NodeRuntimeImpl.java
 *
 * Created on Apr 27, 2010, 10:55:43 PM
 *
 * Description: Provides runtime support for nodes in the local JVM.
 *
 * Copyright (C) Apr 27, 2010, Stephen L. Reed.
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
package org.texai.ahcs.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertPath;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcs.router.AbstractMessageRouter;
import org.texai.ahcs.router.CPOSMessageRouter;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AlbusMessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.NodeTypeInitializer;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.ahcsSupport.RoleTypeInitializer;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.NodeRuntimeConfiguration;
import org.texai.ahcsSupport.domainEntity.NodeType;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.CacheInitializer;
import org.texai.kb.Constants;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/** Provides runtime support for nodes in the local JVM.
 *
 * @author reed
 */
@NotThreadSafe
public class NodeRuntimeImpl implements NodeRuntime, AlbusMessageDispatcher {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NodeRuntimeImpl.class);
  /** the role remote messaging dictionary, role id --> role */
  private final Map<URI, Role> roleRemoteMessagingDictionary = new HashMap<>();
  /** the executor */
  private final ExecutorService executor = Executors.newCachedThreadPool();
  /** the role id that identifies this node runtime when it communicates on behalf of itself */
  private URI roleId;
  /** the dictionary of replyWith values used to suspend message-sending threads while awaiting response messages,
   * replyWith --> lock object
   */
  private final Map<UUID, Object> replyWithsDictionary = new HashMap<>();
  /** the in-reply-to message dictionary, in-reply-to UUID --> message */
  private final Map<UUID, Message> inReplyToDictionary = new HashMap<>();
  /** the X.509 security information for this node runtime */
  private X509SecurityInfo x509SecurityInfo;
  /** the X.509 certificate dictionary, role id --> X.509 certificate */
  private final Map<URI, X509Certificate> x509CertificateDictionary = new HashMap<>();
  /** the name of the cache for the X.509 certificates, remote role id --> X.509 certificate */
  public static final String CACHE_X509_CERTIFICATES = "X.509 certificates";
  /** the names of caches used in the node runtime */
  static final String[] NAMED_CACHES = {
    CACHE_X509_CERTIFICATES
  };
  /** the node runtime application thread */
  private final Thread nodeRuntimeApplicationThread;
  /** the message router */
  private final AbstractMessageRouter messageRouter;
  /** the node runtime keystore, which contains the single X509 certificate chain used by SSL client authentication */
  private KeyStore nodeRuntimeKeyStore;
  /** the node runtime keystore lock */
  private final Object nodeRuntimeKeyStore_lock = new Object();
  /** the role keystore, which contains all the local role X509 certificates */
  private KeyStore roleKeyStore;
  /** the role keystore lock */
  private final Object roleKeyStore_lock = new Object();
  /** the node runtime RDF entity manager */
  private final RDFEntityManager rdfEntityManager;
  /** the node access object */
  private final NodeAccess nodeAccess;
  // the intermediate certificate-signing certificate
  /** the signing private key */
  private PrivateKey certificateSigningPrivateKey;
  /** the signing X.509 certificate path */
  private CertPath certificateSigningCertPath;
  /** the signing X.509 certificate */
  private X509Certificate certificateSigningX509Certificate;
  /** the node runtime key store file path */
  private String nodeRuntimeKeyStoreFilePath;
  /** the role key store file path */
  private String roleKeyStoreFilePath;
  /** the key store password */
  //TODO substitute user-specified password
  public static final char[] KEY_STORE_PASSWORD = "node-runtime-keystore-password".toCharArray();
  /** the node runtime X.509 certificate */
  private X509Certificate nodeRuntimeX509Certificate;
  /** the indicator to quit this application */
  private final AtomicBoolean isQuit = new AtomicBoolean(false);
  /** the launcher role id */
  private final URI launcherRoleId;
  /** the indicator whether finalization has occurred */
  private final AtomicBoolean isFinalized = new AtomicBoolean(false);
  /** the indicator that initialization has completed, and that the node runtime should be persisted upon shutdown */
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  /** the shutdown hook */
  private final ShutdownHook shutdownHook;
  /** the local area network ID */
  private final UUID localAreaNetworkID;
  /** the TCP port as presented to the Internet */
  private final int externalPort;
  /** the host address as presented to the LAN, e.g. turing */
  private final String internalHostName = NetworkUtils.getHostName();
  /** the TCP port as presented to the LAN */
  private final int internalPort;
  /** the timer */
  private final Timer timer = new Timer();
  /** the node runtime configuration */
  private final NodeRuntimeConfiguration nodeRuntimeConfiguration;
  /** the operations to be logged */
  private final Set<String> loggedOperations = new HashSet<>();
  /** the indicator to reload the node and role type definitions */
  private static final boolean IS_RELOAD_NODE_AND_ROLE_TYPES = false;

  /** Constructs a new singleton NodeRuntime instance.
   *
   * @param launcherRoleId the launcher role id
   * @param nodeRuntimeId the node runtime id
   * @param internalPort the internal port
   * @param externalPort the external port
   * @param localAreaNetworkID the local area network ID
   */
  @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
  public NodeRuntimeImpl(
          final URI launcherRoleId,
          final URI nodeRuntimeId,
          final int internalPort,
          final int externalPort,
          final UUID localAreaNetworkID) {
    //Preconditions
    assert launcherRoleId != null : "launcherRoleId must not be null";
    assert nodeRuntimeId != null : "nodeRuntimeId must not be null";
    assert internalPort > 0 : "internalPort must be positive";
    assert externalPort > 0 : "externalPort must be positive";
    assert localAreaNetworkID != null : "localAreaNetworkID must not be null";

    this.launcherRoleId = launcherRoleId;
    this.internalPort = internalPort;
    this.externalPort = externalPort;
    this.localAreaNetworkID = localAreaNetworkID;

    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    CacheInitializer.initializeCaches();
    CacheInitializer.addNamedCaches(NAMED_CACHES);
    rdfEntityManager = new RDFEntityManager();
    nodeAccess = new NodeAccess(rdfEntityManager);

    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    if (IS_RELOAD_NODE_AND_ROLE_TYPES) {
      // load the repositories with the node and role types
      LOGGER.info("reloading node types and role types");
      DistributedRepositoryManager.clearNamedRepository("NodeRoleTypes");
      final RoleTypeInitializer roleTypeInitializer = new RoleTypeInitializer();
      roleTypeInitializer.initialize(rdfEntityManager);
      if ((new File("data/role-types.xml")).exists()) {
        roleTypeInitializer.process("data/role-types.xml");
      } else {
        roleTypeInitializer.process("../Main/data/role-types.xml");
      }
      roleTypeInitializer.finalization();
      final NodeTypeInitializer nodeTypeInitializer = new NodeTypeInitializer();
      nodeTypeInitializer.initialize(rdfEntityManager);
      if ((new File("data/node-types.xml")).exists()) {
        nodeTypeInitializer.process("data/node-types.xml");
      } else {
        nodeTypeInitializer.process("../Main/data/node-types.xml");
      }
      nodeTypeInitializer.finalization();
    }

    // instantiate the node runtime configuration: nodes, roles, and state/value bindings
    nodeRuntimeConfiguration = nodeAccess.getNodeRuntimeConfiguration(nodeRuntimeId);
    assert nodeRuntimeConfiguration != null : "configuration not found for " + nodeRuntimeId;
    LOGGER.info("instantiating " + nodeRuntimeConfiguration);

    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    nodeRuntimeConfiguration.instantiate();

    initializeX509SecurityInfo();
    messageRouter = new CPOSMessageRouter(this);

    if (getNode(AHCSConstants.NODE_NICKNAME_TOPPER) == null) {
      // first time direct assembly of the top friendship node - the Bootstrap skill does the rest
      installTopFriendshipNode();
    } else {
      postLoadingDependencyInjection();
    }

    // start up
    isInitialized.set(true);
    nodeRuntimeApplicationThread = new Thread(new RunApplication(this));
    nodeRuntimeApplicationThread.start();
  }

  /** Finalizes this application. */
  private void finalization() {
    isFinalized.getAndSet(true);
    LOGGER.info("finalization");
    if (isInitialized.get()) {
      LOGGER.info("persisting nodes, roles, and state values");
      nodeAccess.persistNodeRuntimeConfiguration(nodeRuntimeConfiguration);
    }
    if (messageRouter != null) {
      messageRouter.finalization();
    }
    executor.shutdownNow();
    LOGGER.info("shutting down the node runtime");
    if (rdfEntityManager != null) {
      rdfEntityManager.close();
    }

    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();

    // advance the random number generator and save it
    for (int i = 0; i < 100; i++) {
      X509Utils.getSecureRandom().nextInt();
    }
    X509Utils.serializeSecureRandom(X509Utils.DEFAULT_SECURE_RANDOM_PATH);

    LOGGER.info("node runtime completed");
    if (!Thread.currentThread().equals(shutdownHook)) {
      System.exit(0);
    }
  }

  /** Adds the given operation to the list of logged operations.
   *
   * @param operation the given operation
   */
  @Override
  public void addLoggedOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (loggedOperations) {
      loggedOperations.add(operation);
    }
  }

  /** Removes the given operation from the list of logged operations.
   *
   * @param operation the given operation
   */
  @Override
  public void removeLoggedOperation(final String operation) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    synchronized (loggedOperations) {
      loggedOperations.remove(operation);
    }
  }

  /** Returns whether the given message is to be logged.
   *
   * @param message the given message
   * @return whether the given message is to be logged
   */
  @Override
  public boolean isMessageLogged(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    synchronized (loggedOperations) {
      return loggedOperations.contains(message.getOperation());
    }
  }

  /** Gets the local area network ID.
   *
   * @return the local area network ID
   */
  @Override
  public UUID getLocalAreaNetworkID() {
    return localAreaNetworkID;
  }

  /** Gets the TCP port as presented to the Internet.
   *
   * @return the TCP port as presented to the Internet
   */
  @Override
  public int getExternalPort() {
    return externalPort;
  }

  /** Gets the host address as presented to the LAN, e.g. turing.
   *
   * @return the host address as presented to the LAN
   */
  @Override
  public String getInternalHostName() {
    return internalHostName;
  }

  /** Gets the TCP port as presented to the LAN.
   *
   * @return the TCP port as presented to the LAN
   */
  @Override
  public int getInternalPort() {
    return internalPort;
  }

  /** Gets the top friendship role.
   *
   * @return the top friendship role
   */
  @Override
  public Role getTopFriendshipRole() {
    return nodeRuntimeConfiguration.getTopFriendshipRole();
  }

  /** Gets the node access object.
   *
   * @return the node access object
   */
  @Override
  public NodeAccess getNodeAccess() {
    return nodeAccess;
  }

  /** Gets the node runtime id.
   *
   * @return the node runtime id
   */
  @Override
  public URI getNodeRuntimeId() {
    //Preconditions
    assert nodeRuntimeConfiguration != null : "nodeRuntimeConfiguration must not be null";

    return nodeRuntimeConfiguration.getNodeRuntimeId();
  }

  /** Shuts down the node runtime. */
  @Override
  public void shutdown() {
    finalization();
  }

  /** Gets the node runtime configuration.
   *
   * @return the node runtime configuration
   */
  public NodeRuntimeConfiguration getNodeRuntimeConfiguration() {
    return nodeRuntimeConfiguration;
  }

  /** Persists the node runtime configuration, including nodes, roles and state values. */
  public void persistNodeRuntimeConfiguration() {
    nodeAccess.persistNodeRuntimeConfiguration(nodeRuntimeConfiguration);
  }

  /** Returns the id of the role having the given type contained in the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @param roleTypeName the given role type
   * @return the id of the role having the given type contained in the node having the given nickname, or null if not found
   */
  @Override
  public URI getRoleId(
          final String nodeNickname,
          final String roleTypeName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(nodeNickname) : "nodeNickname must be a non-empty string";
    assert StringUtils.isNonEmptyString(roleTypeName) : "roleTypeName must be a non-empty string";

    final Node node = getNode(nodeNickname);
    if (node == null) {
      return null;
    }
    final Role role = node.getRoleForTypeName(roleTypeName);
    if (role == null) {
      return null;
    } else {
      return role.getId();
    }
  }

    @Override
    public String getExternalHostName() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

  /** Provides a shutdown hook to finalize resources when the JVM is unexpectedly shutdown. */
  class ShutdownHook extends Thread {

    @Override
    public void run() {
      Thread.currentThread().setName("shutdown");
      if (!isFinalized.get()) {
        LOGGER.warn("***** shutdown, finalizing resources *****");
        finalization();
      }
    }
  }

  /** Registers the given role.
   *
   * @param role the given role
   */
  @Override
  public void registerRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";
    assert role.getId() != null : "role must have been persisted, and thus have an id";

    nodeRuntimeConfiguration.addRole(role);
    final URI registeredRoleId = role.getId();
    synchronized (x509CertificateDictionary) {
      x509CertificateDictionary.put(registeredRoleId, role.getX509Certificate());
    }
  }

  /** Unregisters the given role.
   *
   * @param role the given role
   */
  @Override
  public void unregisterRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";
    assert role.getId() != null : "role must have been persisted, and thus have an id";
    final URI registeredRoleId = role.getId();
    synchronized (roleRemoteMessagingDictionary) {
      assert !roleRemoteMessagingDictionary.containsKey(registeredRoleId) : "role must not be enabled for remote messaging";
    }

    nodeRuntimeConfiguration.removeRole(role);
    synchronized (x509CertificateDictionary) {
      x509CertificateDictionary.remove(registeredRoleId);
    }
  }

  /** Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void registerRoleForRemoteCommunications(final RoleInfo roleInfo) {
    //Preconditions
    assert roleInfo != null : "roleInfo must not be null";

    messageRouter.registerRoleForRemoteCommunications(roleInfo);
  }

  /** Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void unregisterRoleForRemoteCommunications(final RoleInfo roleInfo) {
    //Preconditions
    assert roleInfo != null : "roleInfo must not be null";

    messageRouter.unregisterRoleForRemoteCommunications(roleInfo);
  }

  /** Gets an unmodifiable copy of the local nodes.
   *
   * @return the local nodes
   */
  public Set<Node> getNodes() {
    return nodeRuntimeConfiguration.getNodes();
  }

  /** Returns the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @return the node having the given nickname, or null if not found
   */
  @Override
  public final Node getNode(final String nodeNickname) {
    //Preconditions
    assert nodeNickname != null : "nodeNickname must not be null";
    assert !nodeNickname.isEmpty() : "nodeNickname must not be empty";

    return nodeRuntimeConfiguration.getNode(nodeNickname);
  }

  /** Adds the given local node.
   *
   * @param node the local node to add
   */
  @Override
  public void addNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    // persist the node to ensure that it has an id before adding it to the set of nodes
    nodeAccess.persistNode(node);
    assert node.getId() != null;

    nodeRuntimeConfiguration.addNode(node);
  }

  /** Removes the given local node.
   *
   * @param node the given local node to remove
   */
  @Override
  public void removeNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    nodeRuntimeConfiguration.removeNode(node);
  }

  /** Sets the indicator to quit.
   *
   * @param isQuit whether to quit
   */
  public void setIsQuit(final boolean isQuit) {
    this.isQuit.set(isQuit);
  }

  /** Dispatches a message in an Albus hierarchical control system.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchAlbusMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final URI senderRoleId = message.getSenderRoleId();

    if (!x509CertificateDictionary.containsKey(senderRoleId)) {
      final X509Certificate x509Certificate = getRemoteRoleX509Certificate(senderRoleId);
      assert x509Certificate != null : "X509 certificate not found for " + senderRoleId;
      x509CertificateDictionary.put(senderRoleId, x509Certificate);
    }

    if (!isLocalRole(senderRoleId)) {
      // messages from non-local roles must be signed
      message.verify(x509CertificateDictionary.get(senderRoleId));
    }

    assert nodeAccess != null : "nodeAccess must not be null";

    if (isQuit.get()) {
      LOGGER.info("quitting, ignoring message:\n  " + message.toString(this));
      return;
    }

    final URI recipientRoleId = message.getRecipientRoleId();
    final Role role;
    role = nodeRuntimeConfiguration.getLocalRole(recipientRoleId);
    if (role == null) {
      // remote role
      if (isMessageLogged(message) || LOGGER.isDebugEnabled()) {
        LOGGER.info("relaying message with non-local role to router:\n  " + message.toString());
      }
      messageRouter.dispatchAlbusMessage(message);
    } else {
      // local role
      if (isMessageLogged(message) || LOGGER.isDebugEnabled()) {
        LOGGER.info("relaying message to local role " + message.toString(this));
      }
      role.dispatchAlbusMessage(message);
    }
  }

  /** Gets the local role having the given id.
   *
   * @param roleId the role id
   * @return the local role having the given id, or null if not found
   */
  @Override
  public Role getLocalRole(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    return nodeRuntimeConfiguration.getLocalRole(roleId);
  }

  /** Returns whether the given role id belongs to a local role.
   *
   * @param roleId the given role id
   * @return whether the given role id belongs to a local role
   */
  @Override
  public boolean isLocalRole(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    return nodeRuntimeConfiguration.isLocalRole(roleId);
  }

  //TODO - use this method or delete it.
  /** Resumes the thread that was suspended awaiting the reply message.
   *
   * @param message the reply message
   */
  private void resumeThread(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final UUID inReplyTo = message.getInReplyTo();
    synchronized (inReplyToDictionary) {
      inReplyToDictionary.put(inReplyTo, message);
    }

    @SuppressWarnings("UnusedAssignment")
    Object threadLock = null;
    synchronized (replyWithsDictionary) {
      threadLock = replyWithsDictionary.get(inReplyTo);
      replyWithsDictionary.remove(inReplyTo);
    }
    assert threadLock != null;
    LOGGER.info("reply received, resuming suspended thread");
    synchronized (threadLock) {
      threadLock.notifyAll();
    }
  }

  /** Obtains the X.509 certificate that is owned by the identified role from the message router.
   *
   * @param ownerRoleId the owner's role id
   * @return the X.509 certificate
   */
  private X509Certificate getRemoteRoleX509Certificate(final URI ownerRoleId) {
    //Preconditions
    assert ownerRoleId != null : "ownerRoleId must not be null";

    LOGGER.info("getting the X509Certificate for remote role " + ownerRoleId);
    final X509Certificate x509Certificate;
    final Cache cache = CacheManager.getInstance().getCache(CACHE_X509_CERTIFICATES);
    assert cache != null : "cache not found for: " + CACHE_X509_CERTIFICATES;
    Element element = cache.get(ownerRoleId);
    if (element == null) {
      // obtain x509Certificate from the message router
      final RoleInfo roleInfo = messageRouter.getRoleInfo(ownerRoleId);
      x509Certificate = roleInfo.getRoleX509Certificate();
      assert x509Certificate != null : "X509 certificate not found in Chord for " + ownerRoleId;
      element = new Element(ownerRoleId, x509Certificate);
      cache.put(element);
    } else {
      x509Certificate = (X509Certificate) element.getValue();
    }

    //Postconditions
    assert x509Certificate != null : "x509Certificate must not be null";

    return x509Certificate;
  }

  /** Sets X.509 security information and the id for the given new role.
   *
   * @param role the given unpersisted role
   */
  @Override
  public void setX509SecurityInfoAndIdForRole(final Role role) {
    //Preconditions
    assert role != null : "role must not be null";

    final String roleAlias = generateAlias();
    role.setRoleAlias(roleAlias);
    role.setId(new URIImpl(
            Constants.TEXAI_NAMESPACE
            + role.getClass().getName()
            + "_"
            + roleAlias));
    role.setX509SecurityInfo(X509Utils.getX509SecurityInfo(
            roleKeyStoreFilePath,
            KEY_STORE_PASSWORD,
            roleAlias));
  }

  /** Returns the next preexisting X509 certificate alias, or creates a new certificate and returns its alias.
   *
   * @return the X509 certificate alias
   */
  public String generateAlias() {
    final String alias;
    final KeyPair roleKeyPair;
    final CertPath roleCertPath;
    try {
      LOGGER.info("generating public and private keys for a new role certificate");
      roleKeyPair = X509Utils.generateRSAKeyPair2048();
      roleCertPath = X509Utils.generateX509CertificatePath(
              roleKeyPair.getPublic(),
              certificateSigningPrivateKey,
              certificateSigningX509Certificate,
              certificateSigningCertPath, null);
      assert !roleCertPath.getCertificates().isEmpty();
      final X509Certificate roleX509Certificate = (X509Certificate) roleCertPath.getCertificates().get(0);
      alias = X509Utils.getUUID(roleX509Certificate).toString();
      setRoleKeyStore(X509Utils.addEntryToKeyStore( // refresh loaded keystore
              roleKeyStoreFilePath,
              KEY_STORE_PASSWORD,
              alias, // alias
              roleCertPath,
              roleKeyPair.getPrivate())); // privateKey
      roleKeyStore.store(new FileOutputStream(getRoleKeyStoreFilePath()), getKeyStorePassword());
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException | KeyStoreException ex) {
      throw new TexaiException(ex);
    }
    return alias;
  }

  /** When implemented by a message router, registers the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  @Override
  public void registerSSLProxy(final Object sslProxy) {
    throw new UnsupportedOperationException("Not implemented.");
  }

  /** Gets the role id that identifies this node runtime when it communicates on behalf of itself.
   *
   * @return the role id that identifies this node runtime when it communicates on behalf of itself
   */
  @Override
  public URI getRoleId() {
    return roleId;
  }

  /** Sets the role id that identifies this node runtime when it communicates on behalf of itself.
   *
   * @param roleId the role id that identifies this node runtime when it communicates on behalf of itself
   */
  public void setRoleId(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    this.roleId = roleId;
  }

  /** Gets the X.509 security information for this node runtime.
   *
   * @return the X.509 security information for this node runtime
   */
  @Override
  public X509SecurityInfo getX509SecurityInfo() {
    return x509SecurityInfo;
  }

  /** Sets the X.509 security information for this node runtime.
   *
   * @param x509SecurityInfo the X.509 security information for this node runtime
   */
  public void setX509SecurityInfo(final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    this.x509SecurityInfo = x509SecurityInfo;
  }

  /** Gets the executor.
   * @return the executor
   */
  @Override
  public Executor getExecutor() {
    return executor;
  }

  /** Gets the timer.
   *
   * @return the timer
   */
  @Override
  public Timer getTimer() {
    return timer;
  }

  /** Gets the node runtime keystore.
   *
   * @return the node runtime keystore
   */
  public KeyStore getNodeRuntimeKeyStore() {
    synchronized (nodeRuntimeKeyStore_lock) {
      return nodeRuntimeKeyStore;
    }
  }

  /** Sets the node runtime keystore.
   *
   * @param nodeRuntimeKeyStore the node runtime keystore
   */
  public void setNodeRuntimeKeyStore(final KeyStore nodeRuntimeKeyStore) {
    //Preconditions
    assert nodeRuntimeKeyStore != null : "nodeRuntimeKeyStore must not be null";

    synchronized (nodeRuntimeKeyStore_lock) {
      this.nodeRuntimeKeyStore = nodeRuntimeKeyStore;
    }
  }

  /** Gets the role keystore.
   *
   * @return the role keystore
   */
  public KeyStore getRoleKeyStore() {
    synchronized (roleKeyStore_lock) {
      return roleKeyStore;
    }
  }

  /** Sets the role keystore.
   *
   * @param roleKeyStore the role keystore
   */
  public void setRoleKeyStore(final KeyStore roleKeyStore) {
    //Preconditions
    assert roleKeyStore != null : "roleKeyStore must not be null";

    synchronized (roleKeyStore_lock) {
      this.roleKeyStore = roleKeyStore;
    }
  }

  /** Gets the node runtime RDF entity manager.
   *
   * @return the node runtime RDF entity manager
   */
  @Override
  public RDFEntityManager getRdfEntityManager() {
    return rdfEntityManager;
  }

  /** Gets the certificate-signing private key.
   *
   * @return the certificate-signing private key
   */
  public PrivateKey getCertificateSigningPrivateKey() {
    return certificateSigningPrivateKey;
  }

  /** Sets the certificate-signing private key.
   *
   * @param certificateSigningPrivateKey the certificate-signing private key
   */
  public void setCertificateSigningPrivateKey(final PrivateKey certificateSigningPrivateKey) {
    //Preconditions
    assert certificateSigningPrivateKey != null : "certificateSigningPrivateKey must not be null";

    this.certificateSigningPrivateKey = certificateSigningPrivateKey;
  }

  /** Gets the certificate-signing X.509 certificate path.
   *
   * @return the certificate-signing X.509 certificate path
   */
  public CertPath getCertificateSigningCertPath() {
    return certificateSigningCertPath;
  }

  /** Sets the certificate-signing X.509 certificate path.
   *
   * @param certificateSigningCertPath the certificate-signing X.509 certificate path
   */
  public void setCertificateSigningCertPath(final CertPath certificateSigningCertPath) {
    //Preconditions
    assert certificateSigningCertPath != null : "certificateSigningCertPath must not be null";

    this.certificateSigningCertPath = certificateSigningCertPath;
  }

  /** Gets the certificate-signing X.509 certificate.
   *
   * @return the certificate-signing X.509 certificate
   */
  public X509Certificate getCertificateSigningX509Certificate() {
    return certificateSigningX509Certificate;
  }

  /** Sets the certificate-signing X.509 certificate.
   *
   * @param certificateSigningX509Certificate the certificate-signing X.509 certificate
   */
  public void setCertificateSigningX509Certificate(final X509Certificate certificateSigningX509Certificate) {
    //Preconditions
    assert certificateSigningX509Certificate != null : "certificateSigningX509Certificate must not be null";

    this.certificateSigningX509Certificate = certificateSigningX509Certificate;
  }

  /** Gets the node runtime key store file path.
   *
   * @return the node runtime key store file path
   */
  public String getNodeRuntimeKeyStoreFilePath() {
    return nodeRuntimeKeyStoreFilePath;
  }

  /** Sets the node runtime key store file path.
   *
   * @param nodeRuntimeKeyStoreFilePath the node runtime key store file path
   */
  public void setNodeRuntimeKeyStoreFilePath(final String nodeRuntimeKeyStoreFilePath) {
    //Preconditions
    assert nodeRuntimeKeyStoreFilePath != null : "nodeRuntimeKeyStoreFilePath must not be null";
    assert !nodeRuntimeKeyStoreFilePath.isEmpty() : "nodeRuntimeKeyStoreFilePath must not be empty";

    this.nodeRuntimeKeyStoreFilePath = nodeRuntimeKeyStoreFilePath;
  }

  /** Gets the role key store file path.
   *
   * @return the role key store file path
   */
  public String getRoleKeyStoreFilePath() {
    return roleKeyStoreFilePath;
  }

  /** Sets the role key store file path.
   *
   * @param roleKeyStoreFilePath the role key store file path
   */
  public void setRoleKeyStoreFilePath(final String roleKeyStoreFilePath) {
    //Preconditions
    assert roleKeyStoreFilePath != null : "roleKeyStoreFilePath must not be null";
    assert !roleKeyStoreFilePath.isEmpty() : "roleKeyStoreFilePath must not be empty";

    this.roleKeyStoreFilePath = roleKeyStoreFilePath;
  }

  /** Gets the key store password.
   *
   * @return the key store password
   */
  public char[] getKeyStorePassword() {
    return KEY_STORE_PASSWORD;
  }

  /** Gets the node runtime X.509 certificate.
   *
   * @return the node runtime X.509 certificate
   */
  public X509Certificate getNodeRuntimeX509Certificate() {
    return nodeRuntimeX509Certificate;
  }

  /** Sets the node runtime X.509 certificate.
   *
   * @param nodeRuntimeX509Certificate the node runtime X.509 certificate
   */
  public void setNodeRuntimeX509Certificate(final X509Certificate nodeRuntimeX509Certificate) {
    //Preconditions
    assert nodeRuntimeX509Certificate != null : "nodeRuntimeX509Certificate must not be null";

    this.nodeRuntimeX509Certificate = nodeRuntimeX509Certificate;
  }

  /** Initializes the X.509 certificate dictionary with the node runtime role id and certificate. */
  public void initializeX509CertificateDictionary() {
    //Preconditions
    assert roleId != null : "roleId must not be null";
    assert nodeRuntimeX509Certificate != null : "nodeRuntimeX509Certificate must not be null";

    synchronized (x509CertificateDictionary) {
      x509CertificateDictionary.put(roleId, nodeRuntimeX509Certificate);
    }
  }

  /** Gets the node runtime key store entry alias.
   *
   * @return the node runtime key store entry alias
   */
  public String getNodeRuntimeKeyStoreEntryAlias() {
    return nodeRuntimeConfiguration.getNodeRuntimeKeyStoreEntryAlias();
  }

  /** Gets the certificate-signing key store entry alias.
   *
   * @return the certificate-signing key store entry alias
   */
  public String getCertificateSigningKeyStoreEntryAlias() {
    return nodeRuntimeConfiguration.getCertificateSigningKeyStoreEntryAlias();
  }

  /** Gets the top friendship role id.
   *
   * @return the topFriendshipRoleId
   */
  public URI getTopFriendshipRoleId() {
    return nodeRuntimeConfiguration.getTopFriendshipRole().getId();
  }

  /** Gets the launcher role id.
   *
   * @return the launcher role id
   */
  @Override
  public URI getLauncherRoleId() {
    return launcherRoleId;
  }

  /** Sets up the node runtime's X.509 security information either after a new installation, or during a restart. */
  private void initializeX509SecurityInfo() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "the JCE unlimited strength jurisdiction policy files must be installed";
    assert nodeAccess != null : "nodeAccess must not be null";

    LOGGER.info("initializing node runtime X.509 security information");
    final String basePath;
    if (System.getProperty("org.texai.basepath") == null) {
      basePath = ".";
    } else {
      basePath = System.getProperty("org.texai.basepath");
    }
    LOGGER.info("basePath: " + basePath);
    nodeRuntimeKeyStoreFilePath = basePath + "/data/node-runtime-keystore.uber";
    LOGGER.info("nodeRuntimeKeyStoreFilePath " + getNodeRuntimeKeyStoreFilePath());
    try {

      nodeRuntimeKeyStore = X509Utils.findKeyStore(
              nodeRuntimeKeyStoreFilePath,
              getKeyStorePassword());

      Enumeration<String> aliases = getNodeRuntimeKeyStore().aliases();
      while (aliases.hasMoreElements()) {
        final String existingAlias = aliases.nextElement();
        LOGGER.info("  node runtime X509 certificate alias: " + existingAlias);
      }

      // node runtime certificate
      final PasswordProtection passwordProtection = new PasswordProtection(getKeyStorePassword());
      PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) getNodeRuntimeKeyStore().getEntry(
              getNodeRuntimeKeyStoreEntryAlias(), // alias
              passwordProtection);
      assert privateKeyEntry != null;
      setNodeRuntimeX509Certificate((X509Certificate) privateKeyEntry.getCertificate());
      LOGGER.info("node runtime certificate principal: " + getNodeRuntimeX509Certificate().getSubjectX500Principal());
      LOGGER.info("                         issuer:    " + getNodeRuntimeX509Certificate().getIssuerX500Principal());
      setRoleId(new URIImpl(Constants.TEXAI_NAMESPACE + "NodeRuntime_" + getNodeRuntimeKeyStoreEntryAlias()));
      LOGGER.info("node runtime roleId " + getRoleId());
      initializeX509CertificateDictionary();

      LOGGER.info("setting the X.509 security information for the node runtime");
      setX509SecurityInfo(X509Utils.getX509SecurityInfo(
              getNodeRuntimeKeyStoreFilePath(),
              getKeyStorePassword(),
              getNodeRuntimeKeyStoreEntryAlias()));

      roleKeyStoreFilePath = basePath + "/data/role-keystore.uber";
      roleKeyStore = X509Utils.findKeyStore(
              roleKeyStoreFilePath,
              getKeyStorePassword());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info("roleKeyStoreFilePath " + getRoleKeyStoreFilePath());

        aliases = getRoleKeyStore().aliases();
        while (aliases.hasMoreElements()) {
          final String existingAlias = aliases.nextElement();
        LOGGER.info("  role X509 certificate alias: " + existingAlias);
        }
      }
      // signing certificate
      privateKeyEntry =
              (PrivateKeyEntry) getRoleKeyStore().getEntry(
              getCertificateSigningKeyStoreEntryAlias(), // alias
              passwordProtection);
      assert privateKeyEntry != null;
      setCertificateSigningX509Certificate((X509Certificate) privateKeyEntry.getCertificate());
      setCertificateSigningPrivateKey(privateKeyEntry.getPrivateKey());
      setCertificateSigningCertPath(X509Utils.generateCertPath(privateKeyEntry.getCertificateChain()));


    } catch (TexaiException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | UnrecoverableEntryException ex) {
      throw new TexaiException(ex);
    }

    //Postconditions
    assert getX509SecurityInfo() != null;
  }

  /** Installs the top friendship node - if this is the first execution of this node runtime. */
  private void installTopFriendshipNode() {

    // directly assemble the top friendship node
    LOGGER.info("assembling top friendship node");
    final NodeType topFriendshipNodeType = getNodeAccess().findNodeType(AHCSConstants.TOP_FRIENDSHIP_NODE_TYPE);
    assert topFriendshipNodeType != null;
    LOGGER.info("  top friendship node type: " + topFriendshipNodeType);
    final Node topFriendshipNode = new Node(topFriendshipNodeType, this);
    LOGGER.info("  top friendship node: " + topFriendshipNode);
    topFriendshipNode.setNodeNickname(AHCSConstants.NODE_NICKNAME_TOPPER);
    topFriendshipNode.installRoles(getNodeAccess());
    rdfEntityManager.persist(topFriendshipNode);
    addNode(topFriendshipNode);

    //Postconditions
    assert getTopFriendshipRole() != null;
  }

  /** Perform post-loading node, role and skill dependency injection. */
  private void postLoadingDependencyInjection() {
    LOGGER.info("installing roles and their skills, from the existing node runtime configuration");
    getNodeRuntimeConfiguration().getNodes().stream().map((node) -> {
          node.setNodeRuntime(this);
          return node;
      }).map((node) -> {
          node.installRoles(nodeAccess);
          return node;
      }).forEach((node) -> {
          node.getRoles().stream().map((role) -> {
            role.setNodeRuntime(this);
            return role;
        }).map((role) -> {
            role.setNode(node);
            return role;
        }).forEach((role) -> {
            role.setX509SecurityInfo(X509Utils.getX509SecurityInfo(
                    roleKeyStoreFilePath,
                    KEY_STORE_PASSWORD,
                    role.getRoleAlias()));
        });
      });
  }

  /** Provides a thread to run the node runtime application, by initializing it and thereafter sleeping to keep the it from
   * otherwise terminating.
   */
  static class RunApplication implements Runnable {

    /** the containing node runtime */
    private final NodeRuntimeImpl nodeRuntime;

    /** Constructs a new RunApplication instance.
     *
     * @param nodeRuntime the containing node runtime
     */
    RunApplication(final NodeRuntimeImpl nodeRuntime) {
      //Preconditions
      assert nodeRuntime != null : "nodeRuntime must not be null";

      this.nodeRuntime = nodeRuntime;
    }

    /** Executes the the node runtime application. */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
      Thread.currentThread().setName("Node Runtime");
      LOGGER.info("starting the node runtime");

      // send an initialize task message to the BootstrapRole, which configures and initializes the remainder of the nodes
      final Node topFriendshipNode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_TOPPER);
      assert topFriendshipNode != null;
      final Role bootstrapRole = topFriendshipNode.getRoleForTypeName(AHCSConstants.BOOTSTRAP_ROLE_TYPE);
      assert bootstrapRole != null;
      final Message message1 = new Message(
              nodeRuntime.getRoleId(), // senderRoleId
              nodeRuntime.getClass().getName(), // senderService
              bootstrapRole.getId(), // recipientRoleId
              "org.texai.skill.lifecycle.Bootstrap", // service
              AHCSConstants.AHCS_INITIALIZE_TASK); // operation
      message1.sign(nodeRuntime.getX509SecurityInfo().getPrivateKey());
      nodeRuntime.dispatchAlbusMessage(message1);

      while (!nodeRuntime.isQuit.get()) {
        try {
          // wait here until the runtime quits
          Thread.sleep(30000);
        } catch (InterruptedException ex) {
        }
      }
      nodeRuntime.finalization();
    }
  }
}
