/*
 * MessageRouter.java
 *
 * Created on Mar 19, 2010, 9:37:26 AM
 *
 * Description: Provides a message router that connects with remote message routers over the internet using SSL
 * transport.  OpenChord provides the naming service.
 *
 * Copyright (C) Mar 19, 2010 reed.
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
package org.texai.ahcs.router;

import java.io.IOException;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.texai.ahcsSupport.RoleInfo;
import de.uniba.wiai.lspi.chord.com.Endpoint;
import de.uniba.wiai.lspi.chord.console.command.entry.Key;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.ThreadSafe;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.openrdf.model.URI;
import org.texai.ahcs.AlbusHCSMessageHandlerFactory;
import org.texai.ahcsSupport.AlbusMessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.chord.com.ssl.MessageRouterProvider;
import org.texai.chord.com.ssl.RequestMessage;
import org.texai.chord.com.ssl.ResponseMessage;
import org.texai.chord.com.ssl.SSLEndpoint;
import org.texai.chord.com.ssl.SSLProxy;
import org.texai.network.netty.ConnectionUtils;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/** Provides a message router that connects with remote message routers over the Internet using SSL
 * transport.  OpenChord provides the naming service.
 *
 * @author reed
 */
@ThreadSafe
public final class MessageRouter extends AbstractAlbusHCSMessageHandler implements AlbusMessageDispatcher {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MessageRouter.class);
  /** the UPNP discovery timeout of 5 seconds */
  private static final int UPNP_DISCOVERY_TIMEOUT = 3000;
  /** the role/channel dictionary, role id string --> channel to peer message router */
  private final Map<String, Channel> roleChannelDictionary = new HashMap<>();
  /** the Chord proxy dictionary, proxy URI string --> proxy */
  private final Map<String, SSLProxy> proxyDictionary = new HashMap<>();
  /** the X.509 certificate dictionary, role id string --> X.509 certificate */
  private final Map<String, X509Certificate> x509CertificateDictionary = new HashMap<>();
  /** the role id strings of local roles that are registered for communication with remote roles */
  private final Set<String> localRoles = new HashSet<>();
  /** the node runtime */
  private final NodeRuntime nodeRuntime;
  /** the Chord endpoint */
  private SSLEndpoint sslEndpoint;
  /** the Chord implementation */
  private Chord chord;
  /** the host address as presented to the Internet, e.g. texai.dyndns.org */
  private String externalHostName;
  /** the host address as presented to the LAN, e.g. turing */
  private final int externalPort;
  /** the message router external IP address */
  private String externalIPAddress;
//  /** the effective external IP address and port, which is most likely mapped to an internal IP address and port */
//  private InetSocketAddress effectiveSocketAddress;
  /** the Internet gateway device */
  private InternetGatewayDevice internetGatewayDevice;
  /** the local URL */
  private URL localURL;
  /** the indicator whether this router has joined the Chord network */
  private AtomicBoolean isJoinedChordNetwork = new AtomicBoolean(false);
  /** the lock for retrieving role information from the Chord distributed hash table */
  private final Object roleInfoRetrievalLock = new Object();
  // the indicator whether to perform dynamic port forwarding of the NAT router
  private static final boolean DO_DYNAMIC_PORT_FORWARDING = true;

  // TODO close unused channels after an inactivity period
  /** Constructs a new MessageRouter instance.
   *
   * @param nodeRuntime the node runtime
   * @param internalPort the internal port
   * @param externalPort the external port
   * @param localURL the URL of this node in the Chord network
   * @param bootstrapURL the bootstrap URL, or null if this is the first node in the Chord network
   */
  public MessageRouter(
          final NodeRuntime nodeRuntime,
          final int internalPort,
          final int externalPort,
          final URL localURL,
          final URL bootstrapURL) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert internalPort > 0 : "internalPort must be positive";
    assert externalPort > 0 : "externalPort must be positive";
    assert localURL != null : "localURL must not be null";

    this.nodeRuntime = nodeRuntime;
    this.externalPort = externalPort;
    this.localURL = localURL;

    if (DO_DYNAMIC_PORT_FORWARDING
            && NetworkUtils.isPrivateNetworkAddress(NetworkUtils.getLocalHostAddress())) {
      // there is a NAT router between this host and the internet
      boolean isOK = configureSSLServerPortForwarding(internalPort, externalPort);
      if (!isOK) {
        LOGGER.warn("cannot forward a server port to this host using UPnP");
      }
    } else {
      // there is no need to configure a NAT router between this host and the internet
      externalHostName = NetworkUtils.getHostName();
      externalIPAddress = NetworkUtils.getLocalHostAddress().getHostAddress();
    }

    // initialize the secure random number so that does not delay the first time establishing an SSL server
    X509Utils.getSecureRandom();

    // accept SSL connections and Albus messages
    final AlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory = new AlbusHCSMessageHandlerFactory(this);
    ConnectionUtils.createPortUnificationServer(
            internalPort,
            nodeRuntime.getX509SecurityInfo(), // sslHandshakeCompletedListener
            albusHCSMessageHandlerFactory,
            null, // bitTorrentHandlerFactory
            null, // httpRequestHandlerFactory
            nodeRuntime.getExecutor(), // bossExecutor
            nodeRuntime.getExecutor()); // workerExecutor
    LOGGER.warn("message router is ready to accept SSL connections ...");
    LOGGER.warn("  internet-facing IP address: " + externalHostName + ":" + externalPort);
    LOGGER.warn("  LAN IP address: " + nodeRuntime.getInternalHostName() + ":" + internalPort);

    joinChordNetwork(bootstrapURL);

    // insert this message router's role information
    final RoleInfo roleInfo = new RoleInfo(
            nodeRuntime.getRoleId(),
            nodeRuntime.getX509SecurityInfo().getCertPath(),
            nodeRuntime.getX509SecurityInfo().getPrivateKey(), // node runtime's private key signs the role info
            nodeRuntime.getLocalAreaNetworkID(),
            externalHostName,
            externalPort,
            nodeRuntime.getInternalHostName(),
            internalPort);
    registerRoleForRemoteCommunications(roleInfo);

    // verify that role info can be retrieved from the chord network
    final RoleInfo retrievedRoleInfo = getRoleInfo(nodeRuntime.getRoleId());
    assert retrievedRoleInfo != null;
    assert retrievedRoleInfo.equals(roleInfo);

    LOGGER.info("message router initialized");
  }

  /** Join the Chord network.
   *
   * @param bootstrapURL the bootstrap URL, or null if this is the first node in the Chord network
   */
  private void joinChordNetwork(final URL bootstrapURL) {
    //Preconditions
    assert localURL != null : "localURL must not be null";
    assert nodeRuntime.getX509SecurityInfo() != null : "node runtime X.509 security info must not be null";

    LOGGER.info("joining the Chord network");
    // the number of bytes of displayed IDs
    System.getProperties().put("de.uniba.wiai.lspi.chord.data.ID.number.of.displayed.bytes", String.valueOf(4));

    // the representation chosen when displaying IDs. 0 = binary, 1 = decimal, 2 = hexadecimal
    System.getProperties().put("de.uniba.wiai.lspi.chord.data.ID.displayed.representation", String.valueOf(2));

    // the number of successors, which must be greater or equal to 1
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.successors", String.valueOf(2));

    // the number of threads for asynchronous executions
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.AsyncThread.no", String.valueOf(Runtime.getRuntime().availableProcessors()));

    // the log properties file location
    System.getProperties().put("log4j.properties.file", System.getProperty("user.home") + "/TexaiLauncher-1.0/log4j.properties");

    // the time in seconds until the stabilize task is started for the first time
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.StabilizeTask.start", String.valueOf(0));
    // the time in seconds between two invocations of the stabilize task
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.StabilizeTask.interval", String.valueOf(12));

    // the time in seconds until the fix finger task is started for the first time
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.FixFingerTask.start", String.valueOf(0));
    // the time in seconds between two invocations of the fix finger task
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.FixFingerTask.interval", String.valueOf(12));

    // the time in seconds until the check predecessor task is started for the first time
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.CheckPredecessorTask.start", String.valueOf(0));
    // the time in seconds between two invocations of the check predecessor task
    System.getProperties().put("de.uniba.wiai.lspi.chord.service.impl.ChordImpl.CheckPredecessorTask.interval", String.valueOf(12));

    // inject dependencies
    final MessageRouterProvider messageRouterProvider = new MessageRouterProvider();
    messageRouterProvider.setMessageRouter(this);
    SSLEndpoint.setMessageRouterProvider(messageRouterProvider);
    SSLProxy.setMessageRouterProvider(messageRouterProvider);
    SSLEndpoint.setX509SecurityInfo(nodeRuntime.getX509SecurityInfo());

    // logging
    Logger.getLogger(SSLEndpoint.class).setLevel(Level.INFO);
    Logger.getLogger(SSLProxy.class).setLevel(Level.INFO);

    chord = new ChordImpl();
    try {
      if (bootstrapURL == null || bootstrapURL.equals(localURL)) {
        LOGGER.info("creating first node in the Chord network with " + localURL);
        chord.create(localURL);
      } else {
        LOGGER.info("joining the Chord network with " + localURL + ", bootstrap node at " + bootstrapURL);
        chord.join(localURL, bootstrapURL);
      }
    } catch (ServiceException ex) {
      throw new TexaiException(ex);
    }
    sslEndpoint = (SSLEndpoint) Endpoint.getEndpoint(localURL);
    LOGGER.info("created Chord endpoint " + sslEndpoint);
    assert sslEndpoint != null;
    isJoinedChordNetwork.set(true);
  }

  /** Leaves the chord network. */
  public void leaveChordNetwork() {
    LOGGER.warn("leaving the Chord network");
    try {
      chord.leave();
    } catch (ServiceException ex) {
      LOGGER.warn("exception when leaving the Chord network: " + ex.getMessage());
    }
  }

  /** Uses Universal Plug and Play to configure the NAT router to forward the given port to this host.
   *
   * @param internalPort the port for this message router
   * @param externalPort the NAT port to forward to this host
   * @return whether there is no UPnP router or whether mapping succeeded
   */
  private boolean configureSSLServerPortForwarding(final int internalPort, final int externalPort) {
    //Preconditions
    assert internalPort > 0 : "internalPort must be positive";
    assert externalPort > 0 : "externalPort must be positive";

    final boolean isMapped;
    LOGGER.info("configuring the NAT to forward port " + externalPort + " to the message router internal port " + internalPort);
    try {
      final InternetGatewayDevice[] internetGatewayDevices = InternetGatewayDevice.getDevices(UPNP_DISCOVERY_TIMEOUT);
      if (internetGatewayDevices == null) {
        LOGGER.warn("no UPnP router found");
        isMapped = false;
      } else {
        // use the the first device found
        internetGatewayDevice = internetGatewayDevices[0];
        LOGGER.info("found device " + internetGatewayDevice.getIGDRootDevice().getModelDescription());
        externalIPAddress = internetGatewayDevice.getExternalIPAddress();
        LOGGER.info("external IP address: " + externalIPAddress);
        externalHostName = externalIPAddress.toString();
        // open the port
        final InetAddress localHostAddress = NetworkUtils.getLocalHostAddress();
        LOGGER.info("local host address: " + localHostAddress.getHostAddress());
        // assume that localHostIP is something other than 127.0.0.1
        isMapped = internetGatewayDevice.addPortMapping(
                "Texai SSL message router", // description
                null, // remote host
                internalPort,
                externalPort,
                localHostAddress.getHostAddress(),
                0, // lease duration in seconds, 0 for an infinite time
                "TCP");  // protocol
        if (isMapped) {
          LOGGER.info("port " + externalPort + " mapped to " + localHostAddress.getHostAddress());
          final ActionResponse actionResponse = internetGatewayDevice.getSpecificPortMappingEntry(
                  null, // remoteHost
                  externalPort, // external port
                  "TCP");  // protocol
          LOGGER.info("mapping info:\n" + actionResponse);
        } else {
          LOGGER.info("port " + externalPort + " cannot be mapped at " + internetGatewayDevice.getIGDRootDevice().getModelDescription());
          for (int i = 0; i < internetGatewayDevice.getNatMappingsCount(); i++) {
            final ActionResponse actionResponse = internetGatewayDevice.getGenericPortMappingEntry(i);
            LOGGER.info("index " + i + " mapping info:\n" + actionResponse);
          }
        }
      }
    } catch (IOException | UPNPResponseException ex) {
      throw new TexaiException(ex);
    }
    try {
      // wait for discovery listener thread to finish
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      throw new TexaiException(ex);
    }
    return isMapped;
  }

  /** Registers the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  @Override
  public void registerSSLProxy(final Object sslProxy) {
    //Preconditions
    assert sslProxy != null : "sslProxy must not be null";

    final SSLProxy sslProxy1 = (SSLProxy) sslProxy;
    LOGGER.info("registering SSLProxy, " + sslProxy1.getLocalNodeURL().toString() + " --> " + sslProxy);
    synchronized (proxyDictionary) {
      proxyDictionary.put(sslProxy1.getLocalNodeURL().toString(), sslProxy1);
    }
  }

  /** Unregisters the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  public void unregisterSSLProxy(final SSLProxy sslProxy) {
    //Preconditions
    assert sslProxy != null : "sslProxy must not be null";

    LOGGER.info("unregistering SSLProxy " + sslProxy);
    synchronized (proxyDictionary) {
      proxyDictionary.remove(sslProxy.getLocalNodeURL().toString());
    }
  }

  /** Catches a channel exception.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   */
  @Override
  public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    throw new TexaiException(exceptionEvent.getCause());
  }

  /** Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  public void registerRoleForRemoteCommunications(final RoleInfo roleInfo) {
    //Preconditions
    assert roleInfo != null : "roleInfo must not be null";

    final String roleIdString = roleInfo.getRoleId().toString();
    LOGGER.info("registering role " + roleIdString);
    synchronized (localRoles) {
      localRoles.add(roleIdString);
    }
    try {
      LOGGER.info("inserting into chord " + roleIdString + "-->" + roleInfo);
      chord.insert(new Key(roleIdString), roleInfo);
    } catch (ServiceException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  public void unregisterRoleForRemoteCommunications(final RoleInfo roleInfo) {
    //Preconditions
    assert roleInfo != null : "roleInfo must not be null";

    final String roleIdString = roleInfo.getRoleId().toString();
    LOGGER.info("unregistering role " + roleIdString);
    synchronized (localRoles) {
      localRoles.remove(roleIdString);
    }
    try {
      chord.remove(new Key(roleIdString), roleInfo);
    } catch (ServiceException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Receives a Netty message object from a remote message router peer.  The received message is
   * verified before relaying to the local node runtime.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert messageEvent != null : "messageEvent must not be null";
    assert messageEvent.getMessage() instanceof Message;

    final Message message = (Message) messageEvent.getMessage();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("***** received from remote message router: " + message);
    }

    final String senderRoleIdString = message.getSenderRoleId().toString();
    synchronized (roleChannelDictionary) {
      Channel channel = roleChannelDictionary.get(senderRoleIdString);
      if (channel == null || channel != channelHandlerContext.getChannel()) {
        // record the incoming message channel so that it can be used for outbound messages to the same peer
        roleChannelDictionary.put(senderRoleIdString, channelHandlerContext.getChannel());
      }
    }

    dispatchAlbusMessage(message);
  }

  /** Receives a message sent from the Chord endpoint or from a local Chord proxy, whose recipient is a remote Chord
   * endpoint.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchAlbusMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("dispatching message " + message);
      LOGGER.debug("  reply-with: " + message.getReplyWith());
    }

    if (sslEndpoint == null) {
      sslEndpoint = (SSLEndpoint) Endpoint.getEndpoint(localURL);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  my sslEndpoint: " + sslEndpoint);
    }

    LOGGER.info("");
    final String recipientRoleIdString = message.getRecipientRoleId().toString();
    if (RequestMessage.isRequestMessage(message)) {
      // This is an inbound Chord request message sent between message routers. The endpoint handles incoming Chord requests.
      verifyMessage(message); // will not block
      if (sslEndpoint.getURL().toString().equals(recipientRoleIdString)) {
        LOGGER.info("<====== dispatching inbound Chord request message to endpoint: " + message);
        // use a separate thread for the SSLEndpoint because it might block via a call to SSLProxy
        nodeRuntime.getExecutor().execute(new AlbusMessageDispatchRunner(
                sslEndpoint, // albusMessageDispatcher
                message));
      } else {
        // outbound message
        LOGGER.info("======> dispatching outbound Chord request message to remote router: " + message);
        routeAlbusMessageToPeerRouter(message);
      }

    } else if (ResponseMessage.isResponseMessage(message)) {
      // This is an inbound Chord response message sent between message routers.  The proxy handles incoming Chord responses.
      verifyMessage(message); // will not block
      synchronized (proxyDictionary) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("proxyDictionary: " + proxyDictionary);
        }
        // because proxies have the URL of the remote node, the recipient id here is the same as the sender
        if (recipientRoleIdString.equals(localURL.toString())) {
          if (proxyDictionary.containsKey(recipientRoleIdString)) {
            final SSLProxy sslProxy = proxyDictionary.get(recipientRoleIdString);
            LOGGER.info("<======  dispatching inbound Chord response message to proxy: " + message);
            // use a separate thread for the SSLProxy because it might block
            nodeRuntime.getExecutor().execute(new AlbusMessageDispatchRunner(
                    sslProxy, // albusMessageDispatcher
                    message));
          } else {
            assert false : "SSL proxy not found for " + message;
          }
        } else {
          LOGGER.info("======> dispatching outbound Chord response message to remote router: " + message);
          routeAlbusMessageToPeerRouter(message);
        }
      }

    } else {
      // Albus message sent between roles via their respective message routers
      final boolean isLocalRole;
      synchronized (localRoles) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("recipientRoleIdString: " + recipientRoleIdString);
          LOGGER.debug("localRoles: ");
        }
        isLocalRole = localRoles.contains(recipientRoleIdString);
      }
      if (isLocalRole) {
        // route to local node runtime
        LOGGER.info("<====== dispatching inbound role message " + message);
        // use a separate thread for the role message because it might block, e.g. to retrieve the sender's X509 certificate
        nodeRuntime.getExecutor().execute(new AlbusMessageDispatchRunner(
                nodeRuntime, // albusMessageDispatcher
                message));
      } else {
        LOGGER.info("======> dispatching outbound role message " + message);
        synchronized (localRoles) {
          // if the sending role has not otherwise been registered for remote communications, allow incoming messages on the LAN to reach it
          final String senderRoleIdString = message.getSenderRoleId().toString();
          if (!localRoles.contains(senderRoleIdString)) {
            localRoles.add(senderRoleIdString);
          }
        }
        routeAlbusMessageToPeerRouter(message);
      }
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  dispatch completed");
    }
  }

  /** Provides a thread to execute the SSLProxy or SSL endpoint that handles an inbound Chord message, and which might then subsequently
   * block the I/O thread while awaiting a conversational response from a Chord network peer. The parent thread is an
   * AbstractAlbusHCSMessageHandler and must not block.
   */
  static class AlbusMessageDispatchRunner implements Runnable {

    /** the SSL proxy, SSL endpoint, or node runtime */
    private final AlbusMessageDispatcher albusMessageDispatcher;
    /** the message */
    private final Message message;

    /** Constructs a new AlbusMessageDispatchRunner instance.
     *
     * @param albusMessageDispatcher the SSL proxy, SSL endpoint, or node runtime
     * @param message the message
     */
    AlbusMessageDispatchRunner(
            final AlbusMessageDispatcher albusMessageDispatcher,
            final Message message) {
      //Preconditions
      assert albusMessageDispatcher != null : "albusMessageDispatcher must not be null";
      assert message != null : "message must not be null";

      this.albusMessageDispatcher = albusMessageDispatcher;
      this.message = message;
    }

    /** Executes this runnable. */
    @Override
    public void run() {
      albusMessageDispatcher.dispatchAlbusMessage(message);
    }
  }

  /** Retrieves the role information from the Chord network using the given role id as the key.
   *
   * @param roleId the given role id
   * @return the role information from the Chord network
   */
  public RoleInfo getRoleInfo(final URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    final String chordRoleIdString = roleId.toString();
    final Set<Serializable> roleInfos;
    try {
      LOGGER.info("retrieving role information from chord for: " + roleId + ", string: " + chordRoleIdString);
      String threadName = Thread.currentThread().getName();
      Thread.currentThread().setName("role-info-retriever-thread");
      synchronized (roleInfoRetrievalLock) {
        roleInfos = chord.retrieve(new Key(chordRoleIdString));
      }
      LOGGER.info("role information objects retrieved " + roleInfos);
      Thread.currentThread().setName(threadName);
    } catch (ServiceException ex) {
      throw new TexaiException(ex);
    }
    if (roleInfos.isEmpty()) {
      throw new TexaiException("unknown router/role information for " + chordRoleIdString);
    } else if (roleInfos.size() != 1) {
      throw new TexaiException("  non-unique router/role information for " + chordRoleIdString + " --> " + roleInfos);
    }
    final RoleInfo roleInfo = (RoleInfo) roleInfos.toArray()[0];

    //Postconditions
    roleInfo.verify();

    return roleInfo;
  }

  /** Verifies a message sent between roles in the Albus hierarchical control system network, throwing an exception
   * if the message's digital  signature fails verification.
   *
   * @param message the message
   */
  private void verifyMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (message.isChordOperation()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("non-verifiable Chord operation message " + message);
      }
      return;
    }
    final String senderRoleIdString = message.getSenderRoleId().toString();
    X509Certificate x509Certificate = x509CertificateDictionary.get(senderRoleIdString);
    if (x509Certificate == null) {
      LOGGER.info("sender's X.509 certificate not found, requesting it from the Chord network");
      final Key key = new Key(senderRoleIdString);
      final Set<Serializable> roleInfos;
      try {
        roleInfos = chord.retrieve(key);
      } catch (ServiceException ex) {
        throw new TexaiException(ex);
      }
      if (roleInfos.isEmpty()) {
        throw new TexaiException("sending role does not have a registered X.509 certificate " + senderRoleIdString);
      }
      assert roleInfos.size() == 1;
      final RoleInfo roleInfo = (RoleInfo) roleInfos.toArray()[0];
      assert roleInfo.getRoleId().toString().equals(senderRoleIdString);
      x509Certificate = roleInfo.getRoleX509Certificate();
      assert X509Utils.getUUID(x509Certificate).toString().equals(senderRoleIdString) :
              "message: " + message
              + "\nX509 certificate ...\n" + x509Certificate;
      x509CertificateDictionary.put(senderRoleIdString, x509Certificate);
      LOGGER.info("sender's X.509 certificate obtained from the Chord network");
    }

    //Postconditions
    message.verify(x509Certificate);
    LOGGER.info("verified message");
  }

  /** Routes the given message to the responsible peer message router.
   *
   * @param message the Albus message
   */
  private void routeAlbusMessageToPeerRouter(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String recipientRoleIdString = message.getRecipientRoleId().toString();
    Channel channel;
    RoleInfo roleInfo = null;
    if (!message.getRecipientRoleId().getLocalName().isEmpty()) {
      synchronized (roleChannelDictionary) {
        channel = roleChannelDictionary.get(recipientRoleIdString);
      }
      if (channel == null) {
        // retrieve the role information from the Chord DHT before routing the message
        // to routeAlbusMessageToPeerRouter - avoiding deadlock on synchronized resources
        roleInfo = getRoleInfo(message.getRecipientRoleId());
        assert roleInfo != null : "roleInfo not found in Chord DHT for " + message.getRecipientRoleId();
      }
    }

    synchronized (roleChannelDictionary) {
      channel = roleChannelDictionary.get(recipientRoleIdString);
      if (channel == null) {
        // open a channel between this message router and the peer message router that services the recipient role
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("finding a channel ...");
        }
        if (message.getRecipientRoleId().getLocalName().isEmpty()) {
          // the message is a Chord operation, e.g. the receipient is http://mccarthy.local:5048/
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  creating a channel to route Chord operation");
            LOGGER.debug("    from node runtime: " + nodeRuntime.getX509SecurityInfo().getX509Certificate().getSubjectDN());
          }
          channel = ConnectionUtils.openAlbusHCSConnection(
                  NetworkUtils.makeInetSocketAddress(recipientRoleIdString), // inetSocketAddress
                  nodeRuntime.getX509SecurityInfo(), // albusHCSMessageHandler
                  this, // sslHandshakeCompletedListener
                  nodeRuntime.getExecutor(), // bossExecutor
                  nodeRuntime.getExecutor()); // workerExecutor
        } else {
          // the message is between two roles
          LOGGER.info("  routing message beteen two roles");
          assert roleInfo != null;
          final InetSocketAddress inetSocketAddress;
          if (roleInfo.getLocalAreaNetworkID().equals(nodeRuntime.getLocalAreaNetworkID())) {
            // both roles are hosted on the same LAN - so use its internal LAN address
            inetSocketAddress = new InetSocketAddress(roleInfo.getInternalHostName(), roleInfo.getInternalPort());
          } else {
            // the remote role is hosted on another LAN - so use its external, i.e. NAT-mapped, address
            inetSocketAddress = new InetSocketAddress(roleInfo.getExternalHostName(), roleInfo.getExternalPort());
          }

          // search for an existing channel to the target node runtime that was established for some other role
          for (final Channel channel1 : roleChannelDictionary.values()) {
            final InetSocketAddress inetSocketAddress1 = (InetSocketAddress) channel1.getRemoteAddress();
            if (inetSocketAddress1.equals(inetSocketAddress)) {
              channel = channel1;
              LOGGER.info("  found existing channel to " + inetSocketAddress);
              break;
            }
          }
          if (channel == null) {
            // no channel to the target node runtime exists yet, so create one
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("creating a channel to route between two roles");
            }
            channel = ConnectionUtils.openAlbusHCSConnection(
                    inetSocketAddress,
                    nodeRuntime.getX509SecurityInfo(), // albusHCSMessageHandler
                    this, // sslHandshakeCompletedListener
                    nodeRuntime.getExecutor(), // bossExecutor
                    nodeRuntime.getExecutor()); // workerExecutor
          }
        }
        assert channel != null;
        roleChannelDictionary.put(recipientRoleIdString, channel);
      }

      if (!channel.isBound()) {
        LOGGER.info("peer has shutdown " + message.getRecipientRoleId());
        return;
      }
      assert channel.isConnected() : "channel must be connected";
      assert channel.isReadable() : "channel must be readable";
      assert channel.isWritable() : "channel must be writable";
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("transmitting outbound message on channel " + channel);
      }
      synchronized (channel) {
        final ChannelFuture channelFuture = channel.write(message);
        if (LOGGER.isDebugEnabled()) {
          channelFuture.addListener(new ChannelFutureListener() {

            public void operationComplete(final ChannelFuture future) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("  message transmission completed");
              }
            }
          });
        }
      }
    }
  }

  /** Gets the NAT's external IP address.
   *
   * @return the NAT's external IP address
   */
  public String getExternalIPAddress() {
    return externalIPAddress;
  }

  /** Gets the host address as presented to the Internet, e.g. texai.dyndns.org.
   *
   * @return the host address as presented to the Internet
   */
  public String getExternalHostName() {
    return externalHostName;
  }

  /** Finalizes the message router and releases its resources. */
  public void finalization() {
    LOGGER.warn("releasing resources held by the message router");
    if (isJoinedChordNetwork.get()) {
      leaveChordNetwork();
    }
    // removes NAT mapping
    try {
      final boolean isUnmapped = internetGatewayDevice.deletePortMapping(null, externalPort, "TCP");
      if (isUnmapped) {
        LOGGER.warn("Port " + externalPort + " unmapped");
      }
    } catch (IOException | UPNPResponseException ex) {
      throw new TexaiException(ex);
    }
  }
}
