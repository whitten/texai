/*
 * NodeRuntime.java
 *
 * Created on Apr 27, 2010, 10:55:43 PM
 *
 * Description: Provides runtime support for nodes in a container.
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
package org.texai.ahcs;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.BasicNodeRuntime;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodesInitializer;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.CacheInitializer;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * Provides runtime support for nodes in a container.
 *
 * @author reed
 */
@NotThreadSafe
public class NodeRuntime extends BasicNodeRuntime {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeRuntime.class);
  /**
   * the dictionary of replyWith values used to suspend message-sending threads while awaiting response messages, replyWith --> lock object
   */
  private final Map<UUID, Object> replyWithsDictionary = new HashMap<>();

  /**
   * the in-reply-to message dictionary, in-reply-to UUID --> message
   */
  private final Map<UUID, Message> inReplyToDictionary = new HashMap<>();

  /* the X.509 certificate dictionary, role qualified name --> X.509 certificate */
  private final Map<String, X509Certificate> x509CertificateDictionary = new HashMap<>();

  /**
   * the name of the cache for the X.509 certificates, remote role id --> X.509 certificate
   */
  public static final String CACHE_X509_CERTIFICATES = "X.509 certificates";

  /**
   * the names of caches used in the node runtime
   */
  static final String[] NAMED_CACHES = {
    CACHE_X509_CERTIFICATES
  };
  /**
   * the node runtime application thread
   */
  private final Thread nodeRuntimeApplicationThread;
  /**
   * the message router
   */
  private final MessageRouter messageRouter;
  /**
   * the indicator to quit this application
   */
  private final AtomicBoolean isQuit = new AtomicBoolean(false);
  /**
   * the indicator whether finalization has occurred
   */
  private final AtomicBoolean isFinalized = new AtomicBoolean(false);
  /**
   * the indicator that initialization has completed, and that the node runtime should be persisted upon shutdown
   */
  private final AtomicBoolean isInitialized = new AtomicBoolean(false);
  /**
   * the shutdown hook
   */
  private final ShutdownHook shutdownHook;
  /**
   * the key store password
   */
  //TODO substitute user-specified password
  private final char[] keyStorePassword = "node-runtime-keystore-password".toCharArray();
  // the path to the XML file which defines the nodes, roles and skills
  final String nodesPath = "data/nodes.xml";
  // the tamper-evident hash of the nodes path file
  final String nodesFileHashString = "";

  /**
   * Constructs a new singleton BasicNodeRuntime instance.
   *
   * @param containerName the container name
   */
  @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
  public NodeRuntime(final String containerName) {
    super(containerName);
    //Preconditions
    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    // configure a shutdown hook to run the finalization method in case the JVM is abnormally ended
    shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    CacheInitializer.initializeCaches();
    CacheInitializer.addNamedCaches(NAMED_CACHES);
    assert !Logger.getLogger(RDFEntityPersister.class).isInfoEnabled();

    // load the repositories with the node and role types
    LOGGER.info("loading nodes, roles and skills ...");
    final NodesInitializer nodesInitializer = new NodesInitializer(
            true, // isClassExistsTested,
            keyStorePassword,
            this); // nodeRuntime
    nodesInitializer.process(
            nodesPath,
            nodesFileHashString);
    nodesInitializer.finalization();

    messageRouter = new MessageRouter(this);

    // start up
    isInitialized.set(true);
    nodeRuntimeApplicationThread = new Thread(new RunApplication(this));
    nodeRuntimeApplicationThread.start();
  }

  /**
   * Finalizes this application.
   */
  private void finalization() {
    isFinalized.getAndSet(true);
    LOGGER.info("finalization");
    if (messageRouter != null) {
      messageRouter.finalization();
    }
    LOGGER.info("shutting down the node runtime");
    if (getRdfEntityManager() != null) {
      getRdfEntityManager().close();
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
   * Associates the given role qualified name with the given X.509 certificate, for subsequent retrieval of the certificate for a given
   * role.
   *
   * @param qualifiedName the given role qualified name, i.e. container-name.agent-name.role-name
   * @param x509Certificate the given X.509 certificate
   */
  public void addX509Certificate(
          final String qualifiedName,
          final X509Certificate x509Certificate) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";
    assert x509Certificate != null : "x509Certificate must not be null";
    if (!x509Certificate.getSubjectDN().toString().contains(qualifiedName)) {
      throw new TexaiException("qualifiedName: " + qualifiedName + " is not the subject of the certificate\n"
              + x509Certificate);
    }

    x509CertificateDictionary.put(qualifiedName, x509Certificate);
  }

  /**
   * Gets the X.509 certificate associated with the given role qualified name.
   *
   * @param qualifiedName the given role qualified name, i.e. container-name.agent-name.role-name
   *
   * @return the retrieved X.509 certificate, or null if not found
   */
  public X509Certificate getX509Certificate(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    return x509CertificateDictionary.get(qualifiedName);
  }

  /**
   * Shuts down the node runtime.
   */
  public void shutdown() {
    finalization();
  }

  public String getExternalHostName() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Provides a shutdown hook to finalize resources when the JVM is unexpectedly shutdown.
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
   * Sets the indicator to quit.
   *
   * @param isQuit whether to quit
   */
  public void setIsQuit(final boolean isQuit) {
    this.isQuit.set(isQuit);
  }

  /**
   * Dispatches a message, which is between roles in this container, or inbound from another container, or outbound to another container.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (isQuit.get()) {
      LOGGER.info("quitting, ignoring message:\n  " + message);
      return;
    }
    if (message.isBetweenContainers()) {
      if (message.getRecipientContainerName().equals(this.getContainerName())) {
        // verify the signature of the inbound message
        verifyMessage(message);
      } else {
        // route the outbound message
        messageRouter.dispatchMessage(message);
        return;
      }
    }
    if (isMessageLogged(message) || LOGGER.isDebugEnabled()) {
      LOGGER.info("relaying message to local role " + message);
    }
    final Role role = this.getLocalRole(message.getRecipientQualifiedName());
    if (role == null) {
      throw new TexaiException("recipient not found for " + message);
    }
    role.dispatchMessage(message);
  }

  /**
   * Verifies a message sent between roles in the Albus hierarchical control system network, throwing an exception if the message's digital
   * signature fails verification.
   *
   * @param message the message
   */
  protected void verifyMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    X509Certificate x509Certificate = getX509Certificate(message.getSenderQualifiedName());
    if (x509Certificate == null) {
      throw new TexaiException("X.509 certificate not found for sender " + message.getSenderQualifiedName());
    }
    message.verify(x509Certificate);
    LOGGER.info("verified message");
  }

  //TODO - use this method or delete it.
  /**
   * Resumes the thread that was suspended awaiting the reply message.
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

  /**
   * Provides a thread to run the node runtime application, by initializing it and thereafter sleeping to keep the it from otherwise
   * terminating.
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

      // send an initialize task message to the BootstrapRole, which configures and initializes the remainder of the nodes
      //TODO callback to the ContainerGovernanceRole
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
