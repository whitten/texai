/*
 * TexaiLauncher.java
 *
 * Created on Sep 1, 2011, 1:33:47 PM
 *
 * Description: Launches and manages one or more JVMs that each contains Texai nodes, on a single host.
 *
 * Copyright (C) Sep 1, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.texaiLauncher;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcs.router.CPOSMessageRouter;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.texaiLauncher.domainEntity.NodeRuntimeInfo;
import org.texai.texaiLauncher.domainEntity.TexaiLauncherInfo;
import org.texai.util.FileSystemUtils;
import org.texai.util.LinuxScreenUtils;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/** Launches and manages one or more JVMs that each contains Texai nodes, on a single host.
 *
 * @author reed
 */
@NotThreadSafe
public class TexaiLauncher implements NodeRuntime { // implement NodeRuntime in order to have a message router

  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(TexaiLauncher.class);
  /** the indicator whether finalization has occurred */
  private boolean isFinalized = false;
  /** the executor */
  private final ExecutorService executor = Executors.newCachedThreadPool();
  /** the X.509 security information for this node runtime */
  private X509SecurityInfo x509SecurityInfo;
  /** the key store password */
  public static final char[] LAUNCHER_KEY_STORE_PASSWORD = "launcher-keystore-password".toCharArray();
  /** the RDF entity manager */
  private RDFEntityManager rdfEntityManager;
  /** the TexaiLauncherAccess object */
  private TexaiLauncherAccess texaiLauncherAccess;
  /** the Texai launcher info */
  private TexaiLauncherInfo texaiLauncherInfo;
  /** the launcher X.509 certificate */
  private X509Certificate texaiLauncherX509Certificate;
  /** the node runtime dictionary, node runtime role id --> node runtime info */
  private final Map<URI, NodeRuntimeInfo> nodeRuntimeDictionary = new HashMap<>();
  /** the base path */
  private String basePath;
  /** the keystore path */
  private String keyStoreFilePath;
  /** the launcher keystore */
  private KeyStore launcherKeyStore;
  /** the message router */
  private CPOSMessageRouter messageRouter;
  /** the Chord bootstrap URL string */
  private String bootstrapURLString;
  /** the local area network ID */
  private final UUID localAreaNetworkID = UUID.randomUUID();
  /** the host address as presented to the Internet, e.g. texai.dyndns.org (not used) */
  private final String externalHostName = "*not-used*";
  /** the TCP port as presented to the Internet (not used) */
  private final int externalPort = NetworkUtils.TEXAI_PORT;
  /** the host address as presented to the LAN, e.g. turing */
  private final String internalHostName = NetworkUtils.getHostName();
  /** the TCP port as presented to the LAN */
  private final int internalPort = NetworkUtils.TEXAI_PORT;

  /** Constructs a new TexaiLauncher instance. */
  public TexaiLauncher() {
  }

  /** Initializes this application. */
  private void initialization() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "the JCE unlimited strength jurisdiction policy files must be installed";

    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    CacheInitializer.initializeCaches();

    rdfEntityManager = new RDFEntityManager();
    texaiLauncherAccess = new TexaiLauncherAccess(rdfEntityManager);

    // configure logging
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);

    texaiLauncherInfo = texaiLauncherAccess.findTexaiLauncherInfo();
    assert texaiLauncherInfo != null : "cannot find texaiLauncherInfo";
    LOGGER.info("launcher id: " + texaiLauncherInfo.getId());

    initializeX509SecurityInfo();

    // initiate the message router
    final URI localURI;
    localURI = new URIImpl("http://" + internalHostName + ":" + internalPort + "/");
    bootstrapURLString = localURI.toString();
    messageRouter = new CPOSMessageRouter(this); // bootstrapURL

    // register the launcher role-imitator for remote communications with the launched node runtimes
    final RoleInfo roleInfo = new RoleInfo(
            texaiLauncherInfo.getId(), // roleId
            x509SecurityInfo.getCertPath(),
            x509SecurityInfo.getPrivateKey(),
            localAreaNetworkID,
            externalHostName,
            externalPort,
            internalHostName,
            internalPort);
    messageRouter.registerRoleForRemoteCommunications(roleInfo);

    // restart the node runtimes
    for (final NodeRuntimeInfo nodeRuntimeInfo : texaiLauncherInfo.getNodeRuntimeInfos()) {
      nodeRuntimeDictionary.put(nodeRuntimeInfo.getId(), nodeRuntimeInfo);
      startTexaiMain(
              nodeRuntimeInfo,
              false); // isRemoteDebugEnabled
    }
  }

  /** Finalizes this application and releases its resources. */
  private void finalization() {
    isFinalized = true;
    if (messageRouter != null) {
      messageRouter.finalization();
    }
    executor.shutdownNow();
    if (rdfEntityManager != null) {
      rdfEntityManager.close();
    }
    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();
    LOGGER.info("TexaiLauncher completed");
  }

  /** Restarts the TexaiMain application.
   *
   * @param nodeRuntimeInfo the node runtime information
   * @param recipientRoleId the recipient role id
   * @param areRepositoriesBackedUp the indicator whether to backup the repositories
   * @param isRemoteDebugEnabled the indicator whether remote debugging is enabled
   */
  private void restartTexaiMain(
          final NodeRuntimeInfo nodeRuntimeInfo,
          final URI recipientRoleId,
          final boolean areRepositoriesBackedUp,
          final boolean isRemoteDebugEnabled) {
    //Preconditions
    assert nodeRuntimeInfo != null : "nodeRuntimeInfo must not be null";
    assert recipientRoleId != null : "recipientRoleId must not be null";

    shutdownTexaiMain(nodeRuntimeInfo, recipientRoleId);

    long backupTimeMillis = 0;
    if (areRepositoriesBackedUp) {
      try {
        // wait 10 seconds for the node runtime to shut down and to close the repositories
        Thread.sleep(10_000);
      } catch (InterruptedException ex) {
      }
      // backup repositories
      final long beginTimeMillis = System.currentTimeMillis();
      final String repositoriesPath = System.getenv("REPOSITORIES");
      assert repositoriesPath != null;
      File sourceDirectory = new File(repositoriesPath);
      assert sourceDirectory.exists();
      final String archiveRepositoriesPath = System.getenv("ARCHIVE_REPOSITORIES");
      assert archiveRepositoriesPath != null;
      File targetDirectory = new File(archiveRepositoriesPath);
      assert targetDirectory.exists();
      LOGGER.info("backing up the repositories from " + sourceDirectory + " to " + targetDirectory);
      FileSystemUtils.archiveDirectory(sourceDirectory, targetDirectory);

      // backup data
      final String dataPath = "data";
      sourceDirectory = new File(dataPath);
      assert sourceDirectory.exists();
      final String archiveDataPath = System.getenv("ARCHIVE_DATA");
      assert archiveDataPath != null;
      targetDirectory = new File(archiveDataPath);
      assert targetDirectory.exists();
      LOGGER.info("backing up the data from " + sourceDirectory + " to " + targetDirectory);
      FileSystemUtils.archiveDirectory(sourceDirectory, targetDirectory);

      backupTimeMillis = System.currentTimeMillis() - beginTimeMillis;
      LOGGER.info("elapsed time " + (backupTimeMillis / 1000) + " seconds");
    }

    if (backupTimeMillis < 60_000) {
      // wait at least 60 seconds for the Chord network to re-configure after the node shutdown
      try {
        Thread.sleep(60_000 - backupTimeMillis);
      } catch (InterruptedException ex) {
      }
    }
    startTexaiMain(
            nodeRuntimeInfo,
            isRemoteDebugEnabled);
  }

  /** Shuts down the TexaiMain application.
   *
   * @param nodeRuntimeInfo the node runtime information
   * @param recipientRoleId the recipient role id
   */
  private void shutdownTexaiMain(
          final NodeRuntimeInfo nodeRuntimeInfo,
          final URI recipientRoleId) {
    //Preconditions
    assert nodeRuntimeInfo != null : "nodeRuntimeInfo must not be null";
    assert recipientRoleId != null : "recipientRoleId must not be null";
    assert texaiLauncherInfo.getId() != null;

    //Logger.getLogger(MessageRouter.class).setLevel(Level.DEBUG);
    LOGGER.info("shutting down " + nodeRuntimeInfo);
    final Message message = new Message(
            texaiLauncherInfo.getId(), // senderRoleId
            TexaiLauncher.class.getName(), // senderService
            recipientRoleId,
            "org.texai.skill.lifecycle.LifeCycleManagement", // service
            AHCSConstants.SHUTDOWN_NODE_RUNTIME); // operation
    message.sign(x509SecurityInfo.getPrivateKey());

    // send message
    messageRouter.dispatchAlbusMessage(message);
    //Logger.getLogger(MessageRouter.class).setLevel(Level.INFO);
  }

  /** Starts the TexaiMain application.
   *
   * @param nodeRuntimeInfo the node runtime information
   * @param isRemoteDebugEnabled the indicator whether remote debugging is enabled
   */
  private void startTexaiMain(
          final NodeRuntimeInfo nodeRuntimeInfo,
          final boolean isRemoteDebugEnabled) {
    //Preconditions
    assert nodeRuntimeInfo != null : "nodeRuntimeInfo must not be null";

    LOGGER.info("launching " + nodeRuntimeInfo);
    final String localURLString = "http://" + NetworkUtils.getHostName() + ":" + nodeRuntimeInfo.getInternalPort() + "/";
    final String script;
    if (isRemoteDebugEnabled) {
      script = "./run-texai-main-debug.sh ";
    } else {
      script = "./run-texai-main.sh ";
    }
    LinuxScreenUtils.launchDetachedScreenSession(
            System.getProperty("user.dir"), // workingDirectory
            script
            + texaiLauncherInfo.getId()
            + " " + nodeRuntimeInfo.getId()
            + " " + nodeRuntimeInfo.getInternalPort()
            + " " + nodeRuntimeInfo.getExternalPort()
            + " " + localURLString
            + " " + bootstrapURLString
            + "  " + localAreaNetworkID, // command
            "texai-main"); // sessionName
    try {
      // wait ten seconds for the node runtime to start up
      Thread.sleep(10000);
    } catch (InterruptedException ex1) {
    }

    // to observe and reconnect to screen sessions ...
    // > screen -list
    // > screen -R

  }

  /** Initializes the X.509 security information for the launcher, so that it can exchange messages with the node runtimes. */
  protected void initializeX509SecurityInfo() {
    //Preconditions
    assert X509Utils.isJCEUnlimitedStrengthPolicy() : "JCE unlimited strength policy file must be installed";

    LOGGER.info("initializing Texai launcher X.509 security information");

    // launcher keystore file
    if (System.getProperty("org.texai.basepath") == null) {
      basePath = ".";
    } else {
      basePath = System.getProperty("org.texai.basepath");
    }
    LOGGER.info("basePath: " + basePath);
    keyStoreFilePath = basePath + "/data/launcher-keystore.uber";
    LOGGER.info("keyStoreFilePath " + keyStoreFilePath);

    try {
      launcherKeyStore = X509Utils.findKeyStore(keyStoreFilePath, LAUNCHER_KEY_STORE_PASSWORD);
      assert launcherKeyStore != null : "launcher key store not found: " + keyStoreFilePath;
      final Enumeration<String> aliases = launcherKeyStore.aliases();
      assert aliases.hasMoreElements();
      assert texaiLauncherInfo.getUUID() != null
              && (texaiLauncherInfo.getUUID().toString().equals(aliases.nextElement()) || !aliases.hasMoreElements());
      LOGGER.info("existing launcher UUID: " + texaiLauncherInfo.getUUID());
      final PrivateKeyEntry privateKeyEntry = (PrivateKeyEntry) launcherKeyStore.getEntry(
              texaiLauncherInfo.getUUID().toString(), // alias
              new PasswordProtection(LAUNCHER_KEY_STORE_PASSWORD)); // passwordProtection
      assert privateKeyEntry != null;
      assert texaiLauncherInfo.getUUID() != null;
      x509SecurityInfo = X509Utils.getX509SecurityInfo(
              keyStoreFilePath,
              LAUNCHER_KEY_STORE_PASSWORD,
              texaiLauncherInfo.getUUID().toString()); // alias
      assert x509SecurityInfo.getPrivateKey() != null;
      texaiLauncherX509Certificate = (X509Certificate) privateKeyEntry.getCertificate();
    } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | UnrecoverableEntryException ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      throw new TexaiException(ex);
    }

    //Postconditions
    assert x509SecurityInfo != null;
    assert texaiLauncherX509Certificate != null;
  }

  /** Launches and manages one or more JVMs that each contains Texai nodes. */
  private void process() {
    // listen for and process AHCN messages
  }

  /** Dispatches a message in an Albus hierarchical control system.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchAlbusMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // process message
    switch (message.getOperation()) {
      case AHCSConstants.KEEP_ALIVE_INFO:
        LOGGER.info((new DateTime()).toString("MM/dd/yyyy hh:mm a") + " keep-alive from " + message.getSenderRoleId());
        break;

      case AHCSConstants.AHCS_RESTART_TASK:
        final URI nodeRuntimeId = (URI) message.get(AHCSConstants.AHCS_RESTART_TASK_NODE_RUNTIME_ID);
        assert nodeRuntimeId != null : message;
        final NodeRuntimeInfo nodeRuntimeInfo = nodeRuntimeDictionary.get(nodeRuntimeId);
        final boolean areRepositoriesBackedUp = (Boolean) message.get(AHCSConstants.AHCS_RESTART_TASK_ARE_REPOSITORIES_BACKED_UP);
        final boolean isRemoteDebuggingEnabled = (Boolean) message.get(AHCSConstants.AHCS_RESTART_TASK_IS_REMOTE_DEBUGGING_ENABLED);
        restartTexaiMain(
                nodeRuntimeInfo,
                message.getSenderRoleId(), // recipientRoleId
                areRepositoriesBackedUp,
                isRemoteDebuggingEnabled);
        break;

      default:
        LOGGER.warn("received unhandled message\n" + message);
        break;
    }
  }

  /** Gets the launcher X.509 certificate.
   *
   * @return the launcher X.509 certificate
   */
  public X509Certificate getTexaiLauncherX509Certificate() {
    return texaiLauncherX509Certificate;
  }

  /** Gets the RDF entity manager.
   *
   * @return the RDF entity manager
   */
  @Override
  public RDFEntityManager getRdfEntityManager() {
    return rdfEntityManager;
  }

  /** Gets the Texai launcher info.
   *
   * @return the Texai launcher info
   */
  public TexaiLauncherInfo getTexaiLauncherInfo() {
    return texaiLauncherInfo;
  }

  /** Gets the role id that identifies this node runtime when it communicates on behalf of itself.
   *
   * @return the role id that identifies this node runtime when it communicates on behalf of itself
   */
  @Override
  public URI getRoleId() {
    return texaiLauncherInfo.getId();
  }

  /** Sets X.509 security information and the id for the given new role.
   *
   * @param role the given unpersisted role
   */
  @Override
  public void setX509SecurityInfoAndIdForRole(Role role) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Gets the X.509 security information for this node runtime.
   *
   * @return the X.509 security information for this node runtime
   */
  @Override
  public X509SecurityInfo getX509SecurityInfo() {
    return x509SecurityInfo;
  }

  /** Gets the executor.
   *
   * @return the executor
   */
  @Override
  public Executor getExecutor() {
    return executor;
  }

  /** When implemented by a message router, registers the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  @Override
  public void registerSSLProxy(Object sslProxy) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Gets the local area network ID.
   *
   * @return the local area network ID
   */
  @Override
  public UUID getLocalAreaNetworkID() {
    return localAreaNetworkID;
  }

  /** Gets the host address as presented to the Internet, e.g. texai.dyndns.org.
   *
   * @return the host address as presented to the Internet
   */
  @Override
  public String getExternalHostName() {
    return externalHostName;
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

  /** Gets the launcher role id.
   *
   * @return the launcher role id
   */
  @Override
  public URI getLauncherRoleId() {
    return texaiLauncherInfo.getId();
  }

  /** Gets the node runtime id.
   *
   * @return the node runtime id
   */
  @Override
  public URI getNodeRuntimeId() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Registers the given role.
   *
   * @param role the given role
   */
  @Override
  public void registerRole(Role role) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Unregisters the given role.
   *
   * @param role the given role
   */
  @Override
  public void unregisterRole(Role role) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void registerRoleForRemoteCommunications(RoleInfo roleInfo) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void unregisterRoleForRemoteCommunications(RoleInfo roleInfo) {
    throw new UnsupportedOperationException("Not supported.");
  }

  /** Gets the top friendship role.
   *
   * @return the top friendship role
   */
  @Override
  public Role getTopFriendshipRole() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Gets the node access object.
   *
   * @return the node access object
   */
  @Override
  public NodeAccess getNodeAccess() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Gets the local role having the given id.
   *
   * @param roleId the role id
   * @return the local role having the given id, or null if not found
   */
  @Override
  public Role getLocalRole(URI roleId) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Returns the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @return the node having the given nickname, or null if not found
   */
  @Override
  public Node getNode(String nodeNickname) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Adds the given local node.
   *
   * @param node the local node to add
   */
  @Override
  public void addNode(Node node) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Removes the given local node.
   *
   * @param node the given local node to remove
   */
  @Override
  public void removeNode(Node node) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Gets the timer.
   *
   * @return the timer
   */
  @Override
  public Timer getTimer() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Shuts down the node runtime. */
  @Override
  public void shutdown() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Adds the given operation to the list of logged operations.
   *
   * @param operation the given operation
   */
  @Override
  public void addLoggedOperation(String operation) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Removes the given operation from the list of logged operations.
   *
   * @param operation the given operation
   */
  @Override
  public void removeLoggedOperation(String operation) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Returns whether the given message is to be logged.
   *
   * @param message the given message
   * @return whether the given message is to be logged
   */
  @Override
  public boolean isMessageLogged(Message message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Returns whether the given role id belongs to a local role.
   *
   * @param roleId the given role id
   * @return whether the given role id belongs to a local role
   */
  @Override
  public boolean isLocalRole(URI roleId) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Returns the id of the role having the given type contained in the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @param roleTypeName the given role type
   * @return the id of the role having the given type contained in the node having the given nickname, or null if not found
   */
  @Override
  public URI getRoleId(String nodeNickname, String roleTypeName) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Provides a shutdown hook to finalize resources when the JVM is unexpectedly shutdown. */
  class ShutdownHook extends Thread {

    @Override
    public void run() {
      Thread.currentThread().setName("shutdown");
      if (!isFinalized) {
        LOGGER.warn("***** shutdown, finalizing resources *****");
        finalization();
      }
    }
  }

  /** Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    Thread.currentThread().setName("launcher");
    final TexaiLauncher texaiLauncher = new TexaiLauncher();
    try {
      texaiLauncher.initialization();
      texaiLauncher.process();
    } catch (Throwable ex) {
      LOGGER.error(StringUtils.getStackTraceAsString(ex));
      texaiLauncher.finalization();
    }
  }
}
