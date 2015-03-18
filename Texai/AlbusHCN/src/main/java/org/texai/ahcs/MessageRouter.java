/*
 * MessageRouter.java
 *
 * Created on Aug 21, 2014, 11:58:37 AM
 *
 * Description: Provides an abstract message router for the Texai network.
 *
 * Copyright (C) 2014 Stephen L. Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.texai.ahcs;

import org.texai.network.netty.handler.AlbusHCSMessageHandlerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.network.netty.utils.ConnectionUtils;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class MessageRouter extends AbstractAlbusHCSMessageHandler implements MessageDispatcher {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(MessageRouter.class);
  /**
   * the node runtime
   */
  private final NodeRuntime nodeRuntime;
  /**
   * the Internet gateway device
   */
  private InternetGatewayDevice internetGatewayDevice;
  /**
   * the UPNP discovery timeout of 5 seconds
   */
  private static final int UPNP_DISCOVERY_TIMEOUT = 3000;
  /**
   * the role/channel dictionary, container-name --> channel to peer message router
   */
  private final Map<String, Channel> containerChannelDictionary = new HashMap<>();
  /**
   * the reconnection dictionary, container-name --> channel to peer message router
   */
  private final Map<String, ReconnectionInfo> reconnectionDictionary = new HashMap<>();
  /**
   * the message router external IP address
   */
  private String externalIPAddress;
  /**
   * the host address as presented to the Internet, e.g. texai.dyndns.org
   */
  private String externalHostName;
  // the server bootstrap that listens for incomming messages
  private ServerBootstrap serverBootstrap;

  /**
   * Constructs a new AbstractMessageRouter instance.
   *
   * @param nodeRuntime the node runtime
   */
  public MessageRouter(final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.nodeRuntime = nodeRuntime;
  }

  /**
   * Listens for incoming messages on the given port.
   *
   * @param port the given TCP port
   */
  public void listenForIncomingConnections(final int port) {
    //Preconditions
    assert port > 1024 : "port must not be a reserved port 1-1024";

    final X509SecurityInfo x509SecurityInfo = X509Utils.getX509SecurityInfo(
            nodeRuntime.getKeyStore(),
            nodeRuntime.getKeyStorePassword(),
            nodeRuntime.getNodeRuntimeSkill().getQualifiedName()); // alias

    serverBootstrap = ConnectionUtils.createPortUnificationServer(
            port,
            x509SecurityInfo,
            new AlbusHCSMessageHandlerFactory(this),
            null, // httpRequestHandlerFactory
            nodeRuntime.getExecutor(), // bossExecutor,
            nodeRuntime.getExecutor()); // workerExecutor
  }

  /**
   * Gets the external IP address.
   *
   * @return the external IP address
   */
  public String getExternalIPAddress() {
    return externalIPAddress;
  }

  /**
   * Gets the external host name.
   *
   * @return the external host name
   */
  public String getExternalHostName() {
    return externalHostName;
  }

  /**
   * Opens an encrypted communications channel with the given peer container.
   *
   * @param containerName the unique container name
   * @param hostName the host name, or IP address of the container
   * @param port the container's TCP port number
   * @param x509SecurityInfo the X.509 certificate used by this peer to authenticate and encrypt the channel
   *
   * @return the channel
   */
  public Channel openChannelToPeerContainer(
          final String containerName,
          final String hostName,
          final int port,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";
    assert StringUtils.isNonEmptyString(hostName) : "hostname must be a non-empty string";
    assert port > 0 : "port must be positive";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("opening a channel to " + containerName + " at " + hostName + ":" + port);
    }
    final InetSocketAddress inetSocketAddress = new InetSocketAddress(hostName, port);
    final Channel channel = ConnectionUtils.openAlbusHCSConnection(
            inetSocketAddress,
            x509SecurityInfo, //
            this, //albusHCSMessageHandler
            nodeRuntime.getExecutor(), // bossExecutor
            nodeRuntime.getExecutor()); // workerExecutor
    synchronized (containerChannelDictionary) {
      containerChannelDictionary.put(containerName, channel);
    }
    synchronized (reconnectionDictionary) {
      final ReconnectionInfo reconnectionInfo = new ReconnectionInfo(
              containerName,
              hostName,
              port,
              x509SecurityInfo);
      reconnectionDictionary.put(containerName, reconnectionInfo);
    }
    return channel;
  }

  /**
   * Reopens an encrypted communications channel with the given peer container.
   *
   * @param reconnectionInfo the reconnection information
   *
   * @return the channel
   */
  private Channel reopenChannelToPeerContainer(final ReconnectionInfo reconnectionInfo) {
    assert reconnectionInfo != null : "reconnectionInfo must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("reopening a channel to " + reconnectionInfo.containerName + " at " + reconnectionInfo.hostName + ":" + reconnectionInfo.port);
    }
    final InetSocketAddress inetSocketAddress = new InetSocketAddress(reconnectionInfo.hostName, reconnectionInfo.port);
    final Channel channel = ConnectionUtils.openAlbusHCSConnection(
            inetSocketAddress,
            reconnectionInfo.x509SecurityInfo, //
            this, //albusHCSMessageHandler
            nodeRuntime.getExecutor(), // bossExecutor
            nodeRuntime.getExecutor()); // workerExecutor
    synchronized (containerChannelDictionary) {
      containerChannelDictionary.put(reconnectionInfo.containerName, channel);
    }
    return channel;
  }

  /**
   * Close the communication channels.
   */
  public void finalization() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("closing the listening socket");
    }
    ConnectionUtils.closePortUnificationServer(serverBootstrap);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("closing channels ...");
    }
    containerChannelDictionary.values().stream().forEach(channel -> {
      LOGGER.debug("  " + channel.getRemoteAddress());
      channel.close();
    });
  }

  /**
   * Uses Universal Plug and Play to configure the NAT router to forward the given port to this host.
   *
   * @param internalPort the port for this message router
   * @param externalPort the NAT port to forward to this host
   *
   * @return whether there is no UPnP router or whether mapping succeeded
   */
  protected boolean configureSSLServerPortForwarding(final int internalPort, final int externalPort) {
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
        externalHostName = externalIPAddress;
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

  /**
   * Catches a channel exception.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent the exception event
   */
  @Override
  @SuppressWarnings("ThrowableResultIgnored")
  public void exceptionCaught(final ChannelHandlerContext channelHandlerContext, final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    final Throwable throwable = exceptionEvent.getCause();
    if (throwable.getMessage().contains("Connection refused")) {
      LOGGER.info("Connection refused");
      return;
    }
    if (throwable.getMessage().contains("No route to host")) {
      LOGGER.info("No route to host");
      return;
    }
    LOGGER.info(throwable.getMessage());

    // remove the channel from the container channel dictionary
    final Channel channel = channelHandlerContext.getChannel();
    if (channel == null) {
      LOGGER.info("channel is null, cannot remove from the container channel dictionary");
      return;
    }
    String containerName = null;
    synchronized (containerChannelDictionary) {
      for (final Entry<String, Channel> entry : containerChannelDictionary.entrySet()) {
        if (entry != null && entry.getValue().equals(channel)) {
          containerName = entry.getKey();
        }
      }
      if (containerName != null) {
        LOGGER.info("removing channel to " + containerName);
        containerChannelDictionary.remove(containerName);
      }
    }
    synchronized (channel) {
      channel.close();
    }
  }

  /**
   * Receives a Netty message object from a remote message router peer. The received message is verified before relaying to the role.
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
    assert Message.class.isAssignableFrom(messageEvent.getMessage().getClass());
    final Message message = (Message) messageEvent.getMessage();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("***** received from remote message router: " + message);
    }

    final String senderContainerName = message.getSenderContainerName();
    synchronized (containerChannelDictionary) {
      Channel channel = containerChannelDictionary.get(senderContainerName);
      if (channel == null || channel != channelHandlerContext.getChannel()) {
        // record the incoming message channel so that it can be used for outbound messages to the same peer
        containerChannelDictionary.put(senderContainerName, channelHandlerContext.getChannel());
      }
    }

    dispatchMessage(message);
  }

  /**
   * Dispatch the given message, which is inbound from another container, or outbound to another container.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.isBetweenContainers() : "message must be between containers to use this router";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("dispatching message " + message);
      LOGGER.debug("  reply-with: " + message.getReplyWith());
      LOGGER.debug("");
    }

    final String recipientQualifiedName = message.getRecipientQualifiedName();
    if (Node.extractContainerName(recipientQualifiedName).equals(nodeRuntime.getContainerName())) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("<====== dispatching inbound role message " + message);
      }
      // use a separate thread for the inbound message dispatch
      nodeRuntime.getExecutor().execute(new InboundMessageDispatchRunner(
              nodeRuntime, // albusMessageDispatcher
              message));
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("======> dispatching outbound role message " + message);
      }
      routeAlbusMessageToPeerRouter(message);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  dispatch completed");
    }
  }

  /**
   * Routes the given message to the responsible peer message router.
   *
   * @param message the Albus message
   */
  private void routeAlbusMessageToPeerRouter(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    Channel channel;
    synchronized (containerChannelDictionary) {
      channel = containerChannelDictionary.get(message.getRecipientContainerName());
    }
    if (channel != null && !channel.isBound()) {
      final ReconnectionInfo reconnectionInfo = reconnectionDictionary.get(message.getRecipientContainerName());
      if (reconnectionInfo == null) {
        LOGGER.info("no reconnection information for remote peer at " + message.getRecipientContainerName());
      } else {
        final long waitMillis = 15000;
        LOGGER.info("peer has shutdown " + message.getRecipientQualifiedName()
                + " - attempting reconnection to " + reconnectionInfo.containerName
                + " in " + ((int) (waitMillis/1000)) + " seconds");
        try {
          Thread.sleep(waitMillis);
        } catch (InterruptedException ex) {
          // ignore
        }
        channel = reopenChannelToPeerContainer(reconnectionInfo);
      }
    }
    if (channel == null) {
      if (message.getOperation().equals(AHCSConstants.SEED_CONNECTION_REQUEST_INFO)) {
        final String hostName = message.get(AHCSConstants.MSG_PARM_HOST_NAME).toString();
        final int port = (Integer) message.get(AHCSConstants.SEED_CONNECTION_REQUEST_INFO_PORT);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("retrieving X.509 security info for " + message.getSenderQualifiedName());
        }
        final X509SecurityInfo x509SecurityInfo;
        try {
          x509SecurityInfo = X509Utils.getX509SecurityInfo(
                  nodeRuntime.getKeyStore(),
                  nodeRuntime.getKeyStorePassword(),
                  message.getSenderQualifiedName()); // alias
        } catch (Throwable ex) {
          X509Utils.logAliases(nodeRuntime.getKeyStore(), LOGGER);
          throw new TexaiException(ex);
        }

        LOGGER.info("opening connection to " + hostName + ':' + port);
        channel = openChannelToPeerContainer(
                message.getRecipientContainerName(), // containerName,
                hostName,
                port,
                x509SecurityInfo);
        if (channel == null) {
          LOGGER.info("no connection to " + hostName + ':' + port);
          //TODO report to network operations
          return;
        }
      } else {
        throw new TexaiException("no communcations channel to recipient " + message);
      }
    }

    if (!channel.isBound()) {
      LOGGER.info("peer has shutdown " + message.getRecipientQualifiedName());
      return;
    }
    if (!channel.isConnected() || !channel.isReadable() || !channel.isWritable()) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
      }
    }
    assert channel.isConnected() : "channel must be connected";
    assert channel.isReadable() : "channel must be readable";
    if (!channel.isWritable()) {
      LOGGER.info("queuing outbound message until the channel is writable");
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("transmitting outbound message on channel " + channel);
    }
    synchronized (channel) {
      final ChannelFuture channelFuture = channel.write(message);
      if (LOGGER.isDebugEnabled()) {
        channelFuture.addListener((final ChannelFuture future) -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  message transmission completed");
          }
        });
      }
    }
  }

  /**
   * Holds remote peer reconnection information.
   */
  static class ReconnectionInfo {

    // the container name of the remote peer
    final String containerName;
    // the host name
    final String hostName;
    // the port
    final int port;
    // the X.509 security information of the local agent which initiated the connection
    final X509SecurityInfo x509SecurityInfo;

    /**
     * Constructs a new ReconnectionInfo instance.
     *
     * @param containerName
     * @param hostName
     * @param port
     * @param x509SecurityInfo
     */
    ReconnectionInfo(
            final String containerName,
            final String hostName,
            final int port,
            final X509SecurityInfo x509SecurityInfo) {
      //Preconditions
      assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";
      assert StringUtils.isNonEmptyString(hostName) : "hostname must be a non-empty string";
      assert port > 0 : "port must be positive";
      assert x509SecurityInfo != null : "x509SecurityInfo must not be null";

      this.containerName = containerName;
      this.hostName = hostName;
      this.port = port;
      this.x509SecurityInfo = x509SecurityInfo;
    }

    @Override
    public String toString() {
      return (new StringBuilder()).append("[ReconnectionInfo for ").append(containerName).append(", ").append(hostName).append(':').append(port).append(']').toString();
    }
  }

  /**
   * Provides a thread to dispatch the message.
   */
  static class InboundMessageDispatchRunner implements Runnable {

    /**
     * the SSL proxy, SSL endpoint, or node runtime
     */
    private final MessageDispatcher albusMessageDispatcher;
    /**
     * the message
     */
    private final Message message;

    /**
     * Constructs a new AlbusMessageDispatchRunner instance.
     *
     * @param albusMessageDispatcher the SSL proxy, SSL endpoint, or node runtime
     * @param message the message
     */
    InboundMessageDispatchRunner(
            final MessageDispatcher albusMessageDispatcher,
            final Message message) {
      //Preconditions
      assert albusMessageDispatcher != null : "albusMessageDispatcher must not be null";
      assert message != null : "message must not be null";

      this.albusMessageDispatcher = albusMessageDispatcher;
      this.message = message;
    }

    /**
     * Executes this runnable.
     */
    @Override
    public void run() {
      albusMessageDispatcher.dispatchMessage(message);
    }
  }

}
