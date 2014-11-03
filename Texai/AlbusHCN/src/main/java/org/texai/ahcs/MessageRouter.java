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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.network.netty.ConnectionUtils;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

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
   * the message router external IP address
   */
  private String externalIPAddress;
  /**
   * the host address as presented to the Internet, e.g. texai.dyndns.org
   */
  private String externalHostName;
  // the local container name
  final String localContainerName;

  /**
   * Constructs a new AbstractMessageRouter instance.
   *
   * @param nodeRuntime the node runtime
   */
  public MessageRouter(final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.nodeRuntime = nodeRuntime;
    localContainerName = nodeRuntime.getContainerName();
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
   */
  public void openChannelToPeerContainer(
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
      LOGGER.debug("openning a channel to " + containerName + " at " + hostName + ":" + port);
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
  }

  /**
   * Close the communication channels.
   */
  public void finalization() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("closing channels ...");
    }
    containerChannelDictionary.values().stream().forEach(channel -> {
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

    throw new TexaiException(exceptionEvent.getCause());
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
    assert messageEvent.getMessage() instanceof Message;

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
   * Provides a thread to execute the SSLProxy or SSL endpoint that handles an inbound Chord message, and which might then subsequently
   * block the I/O thread while awaiting a conversational response from a Chord network peer. The parent thread is an
   * AbstractAlbusHCSMessageHandler and must not block.
   */
  static class AlbusMessageDispatchRunner implements Runnable {

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
    AlbusMessageDispatchRunner(
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
    }

    LOGGER.info("");
    final String recipientQualifiedName = message.getRecipientQualifiedName();
    // Albus message sent between roles via their respective message routers
    final Role localRecipientRole = nodeRuntime.getLocalRole(recipientQualifiedName);
    if (localRecipientRole == null) {
      LOGGER.info("======> dispatching outbound role message " + message);
      routeAlbusMessageToPeerRouter(message);
    } else {
      // route to local node runtime
      LOGGER.info("<====== dispatching inbound role message " + message);
      // use a separate thread for the role message because it might block, e.g. to retrieve the sender's X509 certificate
      nodeRuntime.getExecutor().execute(new AlbusMessageDispatchRunner(
              nodeRuntime, // albusMessageDispatcher
              message));
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
  protected void routeAlbusMessageToPeerRouter(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    Channel channel;
    synchronized (containerChannelDictionary) {
      channel = containerChannelDictionary.get(message.getRecipientContainerName());
    }

    if (!channel.isBound()) {
      LOGGER.info("peer has shutdown " + message.getRecipientQualifiedName());
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
        channelFuture.addListener((final ChannelFuture future) -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  message transmission completed");
          }
        });
      }
    }
  }

}