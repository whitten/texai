/*
 * AICoinMain.java
 *
 * Created on Sep 21, 2011, 6:23:02 PM
 *
 * Description: The main program which executes an A.I. Coin cryptocurrency network node.
 *
 * Copyright (C) Sep 21, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.main;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import static org.texai.ahcs.NodeRuntime.CACHE_X509_CERTIFICATES;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodesInitializer;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.KBAccess;
import org.texai.kb.persistence.RDFEntityPersister;
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
  final String nodesPath = "data/nodes.xml";
  // the tamper-evident hash of the nodes path file, use "1234" after revsing the nodes.xml file
  final String nodesFileHashString = "qqYwj2MH8bNf0T6mS5eQNar3b00y+WAJzhc81RfcSy2+PiVczKwI5g3BffRNJdw9M3dUPh0yapqdbaegWjnurw==";
  /**
   * the node runtime application thread
   */
  private Thread nodeRuntimeApplicationThread;
  /**
   * the indicator that initialization has completed, and that the node runtime
   * should be persisted upon shutdown
   */
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);

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

    Logger.getLogger(DistributedRepositoryManager.class).setLevel(Level.WARN);
    Logger.getLogger(KBAccess.class).setLevel(Level.WARN);
    Logger.getLogger(NodesInitializer.class).setLevel(Level.WARN);
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    Logger.getLogger(X509Utils.class).setLevel(Level.WARN);

    LOGGER.info("starting the node runtime in the container named " + containerName);
    nodeRuntime = new NodeRuntime(containerName);
    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    CacheInitializer.initializeCaches();
    CacheInitializer.addNamedCaches(NAMED_CACHES);
    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    // load the repositories with the node and role types
    LOGGER.info("loading nodes and their roles, and installing skills for each role ...");
    final NodesInitializer nodesInitializer = new NodesInitializer(
            true, // isClassExistsTested,
            keyStorePassword,
            nodeRuntime,
            "data/keystore.uber"); // keyStoreFilePath
    nodesInitializer.process(
            nodesPath,
            nodesFileHashString);
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
    LOGGER.info("shutting down the node runtime");
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

    LOGGER.info("node runtime completed");
    if (!Thread.currentThread().equals(shutdownHook)) {
      System.exit(0);
    }

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
      LOGGER.info("starting the node runtime");

      // send the initialize message to the <container>.TopmostFriendshipAgent.TopmostFriendshipRole
      final String recipientQualifiedName = nodeRuntime.getContainerName() + ".TopmostFriendshipAgent.TopmostFriendshipRole";
      final Message initializeMessage = new Message(
              nodeRuntime.getNodeRuntimeSkill().getRole().getQualifiedName(), // senderQualifiedName
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
        LOGGER.warn("***** shutdown, finalizing resources *****");
        finalization();
      }
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