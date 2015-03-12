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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides runtime support for nodes in a container.
 *
 * @author reed
 */
@NotThreadSafe
public class NodeRuntime extends BasicNodeRuntime {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeRuntime.class);
  // the dictionary of replyWith values used to suspend message-sending threads while awaiting response messages, replyWith --> lock object
  private final Map<UUID, Object> replyWithsDictionary = new HashMap<>();
  // the in-reply-to message dictionary, in-reply-to UUID --> message
  private final Map<UUID, Message> inReplyToDictionary = new HashMap<>();
  // the X.509 certificate dictionary, role qualified name --> X.509 certificate */
  private final Map<String, X509Certificate> x509CertificateDictionary = new HashMap<>();
  // the name of the cache for the X.509 certificates, remote role id --> X.509 certificate
  public static final String CACHE_X509_CERTIFICATES = "X.509 certificates";
  // the message router
  private final MessageRouter messageRouter;
  // the indicator to quit this application
  public final AtomicBoolean isQuit = new AtomicBoolean(false);
  // the indicator whether finalization has occurred
  private final AtomicBoolean isFinalized = new AtomicBoolean(false);
  // the between-container operations in which the sender informs the receiver of its public X.509 certificate
  private static final Set<String> certificateIntroducingOperations = new ArraySet<>();

  static {
    certificateIntroducingOperations.add(AHCSConstants.CONFIGURE_SINGLETON_AGENT_HOSTS_TASK);
    certificateIntroducingOperations.add(AHCSConstants.JOIN_ACKNOWLEDGED_TASK);
    certificateIntroducingOperations.add(AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO);
    certificateIntroducingOperations.add(AHCSConstants.SEED_CONNECTION_REQUEST_INFO);
    certificateIntroducingOperations.add(AHCSConstants.SINGLETON_AGENT_HOSTS_INFO);
    certificateIntroducingOperations.add(AHCSConstants.TRANSFER_FILE_CHUNK_INFO);
  }

  /**
   * Constructs a new singleton NodeRuntime instance.
   *
   * @param containerName the container name
   * @param networkName
   */
  @SuppressWarnings("CallToThreadStartDuringObjectConstruction")
  public NodeRuntime(
          final String containerName,
          final String networkName) {
    super(containerName, networkName);
    messageRouter = new MessageRouter(this);
  }

  /**
   * Finalizes this application.
   */
  public void finalization() {
    isFinalized.getAndSet(true);
    LOGGER.info("finalization");
    if (messageRouter != null) {
      messageRouter.finalization();
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

    synchronized (x509CertificateDictionary) {
      X509Certificate previousX509Certificate = x509CertificateDictionary.get(qualifiedName);
      if (previousX509Certificate != null && !x509Certificate.equals(previousX509Certificate)) {
        LOGGER.info("Replacing previous X.509 certificate for " + qualifiedName);
      }
      x509CertificateDictionary.put(qualifiedName, x509Certificate);
    }
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

    synchronized (x509CertificateDictionary) {
      return x509CertificateDictionary.get(qualifiedName);
    }
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
   * Sets the indicator to quit.
   *
   * @param isQuit whether to quit
   */
  public void setIsQuit(final boolean isQuit) {
    this.isQuit.set(isQuit);
  }

  /**
   * Listens for incomming messages on the given port.
   *
   * @param port the given TCP port
   */
  public void listenForIncommingConnections(final int port) {
    //Preconditions
    assert port > 1024 : "port must not be a reserved port 1-1024";

    messageRouter.listenForIncomingConnections(port);
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

    if (message.getOperation().equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      throw new TexaiException("message not understood\n" + message);
    }

    boolean isMessageLogged = !isMessageFiltered(message);

    if (!isMessageLogged && isMessageLogged(message)) {
      isMessageLogged = true;
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.info(message);
    } else if (isMessageLogged) {
      LOGGER.info(message.toBriefString());
    }
//    LOGGER.info(message);
    if (message.isBetweenContainers()) {
      if (message.getRecipientContainerName().equals(this.getContainerName())) {
        // verify the signature of the inbound message
        final boolean isOK = verifyMessage(message);
        if (!isOK) {
          //TODO report to container operations, which reports it to network operations
          LOGGER.info("Dropping message from remote peer whose signature does not verify, " + message.toBriefString());
          return;
        } else if (message.getDate().plusMinutes(1).isBeforeNow()) {
          //TODO report to container operations, which reports it to network operations
          LOGGER.info("Dropping message from remote peer which was sent more than one minute ago, " + message.toBriefString());
          return;
        }
      } else {
        // route the outbound message
        messageRouter.dispatchMessage(message);
        return;
      }
    }
    final Role role = this.getLocalRole(message.getRecipientQualifiedName());
    if (role == null) {
      throw new TexaiException("recipient not found for " + message);
    }
    role.dispatchMessage(message);
  }

  /**
   * Verifies a message sent between roles in the Albus hierarchical control system network, returning whether the message signature is OK.
   *
   * @param message the message
   *
   * @return whether the message signature is OK
   */
  protected boolean verifyMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO when the certificate agent is written, ensure that duplicate peers cannot join the network
    X509Certificate x509Certificate;
    if (certificateIntroducingOperations.contains(message.getOperation())) {
      // both sides provide their certificate as a parameter, when a new peer joins the network
      LOGGER.info("adding certificate for " + message.getSenderQualifiedName());
      x509Certificate = (X509Certificate) message.get(AHCSConstants.MSG_PARM_X509_CERTIFICATE);
      assert x509Certificate != null;
      addX509Certificate(
              message.getSenderQualifiedName(), // qualifiedName
              x509Certificate);
    }
    x509Certificate = getX509Certificate(message.getSenderQualifiedName());
    if (x509Certificate == null) {
      throw new TexaiException("X.509 certificate not found for sender " + message.getSenderQualifiedName() + "\n" + message.toString());

    }
    final boolean isOK = message.verify(x509Certificate);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("verified message: " + isOK);
    }
    return isOK;
  }

}
