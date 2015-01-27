/*
 * AICoinMain.java
 *
 * Created on Sep 21, 2011, 6:23:02 PM
 *
 * Description: The main program which executes an A.I. Coin cryptocurrency network node.
 *
 * Copyright (C) Sep 21, 2011, Stephen L. Reed, Texai.org.
 */
package org.texai.main;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.texai.ahcs.NodeRuntime;
import static org.texai.ahcs.NodeRuntime.CACHE_X509_CERTIFICATES;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodesInitializer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.network.netty.handler.PortUnificationHandler;
import org.texai.network.netty.pipeline.AlbusHCNMessageClientPipelineFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.ssl.TexaiSSLContextFactory;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * Executes the Texai node runtime for a certain JVM.
 *
 * @author reed
 */
@NotThreadSafe
public class AICoinMain {

  static {
    // explicitly set the default assertion status because NetBeans ignores -ea when running an application.
    AICoinMain.class.getClassLoader().setDefaultAssertionStatus(true);
  }
  /**
   * the logger
   */
  public static final Logger LOGGER = Logger.getLogger(AICoinMain.class);
  // the node runtime
  private NodeRuntime nodeRuntime;
  // the version
  private static final String VERSION = "1.0";
  /**
   * the shutdown hook
   */
  private ShutdownHook shutdownHook;
  /**
   * the indicator whether finalization has occurred
   */
  private final AtomicBoolean isFinalized = new AtomicBoolean(false);
  /**
   * the names of caches used in the node runtime
   */
  static final String[] NAMED_CACHES = {
    CACHE_X509_CERTIFICATES
  };
  /**
   * the key store password
   */
  //TODO substitute user-specified password from the environment

  private final char[] keyStorePassword = "node-runtime-keystore-password".toCharArray();
  // the path to the XML file which defines the nodes, roles and skills
  private final String nodesPath = "data/nodes.xml";
  // the tamper-evident hash of the nodes path file, use "1234" after revsing the nodes.xml file
  private final String NODES_FILE_HASH =
          "fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o///wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==";
  /**
   * the node runtime application thread
   */
  private Thread nodeRuntimeApplicationThread;
  /**
   * the indicator that initialization has completed, and that the node runtime
   * should be persisted upon shutdown
   */
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  // the configuration certificate path
  private final static String SINGLETON_CONFIGURATION_FILE_PATH = "data/SingletonConfiguration.crt";

  /**
   * Constructs a new TexaiMain instance.
   */
  public AICoinMain() {
  }

  /**
   * Processes this application.
   *
   * @param containerName the container name
   */
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  private void process(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

    Logger.getLogger(AlbusHCNMessageClientPipelineFactory.class).setLevel(Level.WARN);
    Logger.getLogger(DistributedRepositoryManager.class).setLevel(Level.WARN);
    Logger.getLogger(KBAccess.class).setLevel(Level.WARN);
    Logger.getLogger(JournalWriter.class).setLevel(Level.WARN);
//    Logger.getLogger(NodesInitializer.class).setLevel(Level.DEBUG);
    Logger.getLogger(PortUnificationHandler.class).setLevel(Level.WARN);
    Logger.getLogger(PortUnificationChannelPipelineFactory.class).setLevel(Level.WARN);
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    Logger.getLogger(TexaiSSLContextFactory.class).setLevel(Level.WARN);
    Logger.getLogger(X509Utils.class).setLevel(Level.WARN);

    LOGGER.info("A.I. Coin version " + VERSION + ".");
    LOGGER.info("Starting the software agents in the container named " + containerName + ".");
    nodeRuntime = new NodeRuntime(containerName);
    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    CacheInitializer.initializeCaches();
    CacheInitializer.addNamedCaches(NAMED_CACHES);
    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    // for the demo, delete the previous knowledge base journals
    JournalWriter.deleteJournalFiles();

    final String timeZoneId = System.getenv("TIMEZONE");
    if (!StringUtils.isNonEmptyString(timeZoneId)) {
      throw new TexaiException("the TIMEZONE environment variable must be set to a valid timezone\n "
              + "see http://tutorials.jenkov.com/java-date-time/java-util-timezone.html");
    }
    LOGGER.debug("The time zone is " + timeZoneId + ".");
    DateTimeZone.setDefault(DateTimeZone.forID(timeZoneId));
    LOGGER.info("Started " + (new DateTime()).toString("MM/dd/yyyy hh:mm a") + ".");

    if (nodeRuntime.isFirstContainerInNetwork()) {
      LOGGER.info("This is the first container in the network and will host all the network singleton agent / roles.");
    } else {
      LOGGER.info("This container will join the existing network.");
    }

    // load the repositories with the node and role types
    LOGGER.info("Loading agents and their roles, and installing skills for each role ...");
    final NodesInitializer nodesInitializer = new NodesInitializer(
            true, // isClassExistsTested,
            keyStorePassword,
            nodeRuntime,
            "data/keystore.uber", // keyStoreFilePath
            SINGLETON_CONFIGURATION_FILE_PATH); // configurationCertificateFilePath
    nodesInitializer.process(
            nodesPath,
            NODES_FILE_HASH); // nodesFileHashString
    nodesInitializer.finalization();

    // start up
    isInitialized.set(true);
    nodeRuntimeApplicationThread = new Thread(new RunApplication(nodeRuntime));
    nodeRuntimeApplicationThread.start();
  }

  /**
   * Finalizes this application.
   */
  private void finalization() {
    LOGGER.info("Shutting down the node runtime.");
    final Timer timer = nodeRuntime.getTimer();
    synchronized (timer) {
      timer.cancel();
    }

    if (nodeRuntime.getRdfEntityManager() != null) {
      nodeRuntime.getRdfEntityManager().close();
    }

    CacheManager.getInstance().shutdown();
    DistributedRepositoryManager.shutDown();

    // advance the random number generator and save it
    for (int i = 0; i < 100; i++) {
      X509Utils.getSecureRandom().nextInt();
    }
    X509Utils.serializeSecureRandom(X509Utils.DEFAULT_SECURE_RANDOM_PATH);

    LOGGER.info("Node runtime completed.");
  }

  /**
   * Provides a thread to run the node runtime application, by initializing it
   * and thereafter sleeping to keep the it from otherwise terminating.
   */
  static class RunApplication implements Runnable {

    /**
     * the containing node runtime
     */
    private final NodeRuntime nodeRuntime;

    /**
     * Constructs a new RunApplication instance.
     *
     * @param nodeRuntime the containing node runtime
     */
    RunApplication(final NodeRuntime nodeRuntime) {
      //Preconditions
      assert nodeRuntime != null : "nodeRuntime must not be null";

      this.nodeRuntime = nodeRuntime;
    }

    /**
     * Executes the the node runtime application.
     */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
      Thread.currentThread().setName("Node Runtime");
      LOGGER.info("Starting the node runtime.");

      // configure the list of operations to be filtered from logging
      nodeRuntime.addFilteredOperation(AHCSConstants.AHCS_INITIALIZE_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.ADD_UNJOINED_ROLE_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.BECOME_READY_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.DELEGATE_BECOME_READY_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.DELEGATE_PERFORM_MISSION_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.DEPLOY_FILES_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.JOIN_ACKNOWLEDGED_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.KEEP_ALIVE_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.NETWORK_JOIN_COMPLETE_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.NETWORK_JOIN_COMPLETE_SENSATION);
      nodeRuntime.addFilteredOperation(AHCSConstants.PERFORM_MISSION_TASK);
      nodeRuntime.addFilteredOperation(AHCSConstants.REMOVE_UNJOINED_ROLE_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.SEED_CONNECTION_REQUEST_INFO);
      nodeRuntime.addFilteredOperation(AHCSConstants.SINGLETON_AGENT_HOSTS_INFO);

      // send the initialize message to the <container>.TopmostFriendshipAgent.TopmostFriendshipRole
      final String recipientQualifiedName = nodeRuntime.getContainerName() + ".TopmostFriendshipAgent.TopmostFriendshipRole";
      final Message initializeMessage = new Message(
              nodeRuntime.getNodeRuntimeSkill().getQualifiedName(), // senderQualifiedName
              nodeRuntime.getNodeRuntimeSkill().getClassName(), // senderService
              recipientQualifiedName,
              null, // service
              AHCSConstants.AHCS_INITIALIZE_TASK); // operation
      nodeRuntime.dispatchMessage(initializeMessage);

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

  /**
   * Provides a shutdown hook to finalize resources when the JVM is unexpectedly
   * shutdown.
   */
  class ShutdownHook extends Thread {

    @Override
    public void run() {
      Thread.currentThread().setName("shutdown");
      if (!isFinalized.get()) {
        LOGGER.warn("Shutdown, finalizing resources.");
        finalization();
      }
      Runtime.getRuntime().halt(0);
    }
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    final String containerName = System.getenv("CONTAINER");
    if (!StringUtils.isNonEmptyString(containerName)) {
      throw new TexaiException("the environment variable CONTAINER must specify the container name");
    }
    (new AICoinMain()).process(containerName);
  }
}
