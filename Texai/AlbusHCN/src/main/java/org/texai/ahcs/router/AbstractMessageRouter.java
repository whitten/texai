/*
 * AbstractMessageRouter.java
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
package org.texai.ahcs.router;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AlbusMessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.util.NetworkUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
public abstract class AbstractMessageRouter extends AbstractAlbusHCSMessageHandler implements AlbusMessageDispatcher {

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
   * the role/channel dictionary, role id string --> channel to peer message router
   */
  private final Map<String, Channel> roleChannelDictionary = new HashMap<>();
  /**
   * the message router external IP address
   */
  private String externalIPAddress;
  /**
   * the host address as presented to the Internet, e.g. texai.dyndns.org
   */
  private String externalHostName;

  /**
   * Constructs a new AbstractMessageRouter instance.
   *
   * @param nodeRuntime the node runtime
   */
  public AbstractMessageRouter(final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.nodeRuntime = nodeRuntime;
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  abstract protected Logger getLogger();

  /**
   * Gets the node runtime.
   *
   * @return the node runtime
   */
  public NodeRuntime getNodeRuntime() {
    return nodeRuntime;
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
    getLogger().info("configuring the NAT to forward port " + externalPort + " to the message router internal port " + internalPort);
    try {
      final InternetGatewayDevice[] internetGatewayDevices = InternetGatewayDevice.getDevices(UPNP_DISCOVERY_TIMEOUT);
      if (internetGatewayDevices == null) {
        getLogger().warn("no UPnP router found");
        isMapped = false;
      } else {
        // use the the first device found
        internetGatewayDevice = internetGatewayDevices[0];
        getLogger().info("found device " + internetGatewayDevice.getIGDRootDevice().getModelDescription());
        externalIPAddress = internetGatewayDevice.getExternalIPAddress();
        getLogger().info("external IP address: " + externalIPAddress);
        externalHostName = externalIPAddress;
        // open the port
        final InetAddress localHostAddress = NetworkUtils.getLocalHostAddress();
        getLogger().info("local host address: " + localHostAddress.getHostAddress());
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
          getLogger().info("port " + externalPort + " mapped to " + localHostAddress.getHostAddress());
          final ActionResponse actionResponse = internetGatewayDevice.getSpecificPortMappingEntry(
                  null, // remoteHost
                  externalPort, // external port
                  "TCP");  // protocol
          getLogger().info("mapping info:\n" + actionResponse);
        } else {
          getLogger().info("port " + externalPort + " cannot be mapped at " + internetGatewayDevice.getIGDRootDevice().getModelDescription());
          for (int i = 0; i < internetGatewayDevice.getNatMappingsCount(); i++) {
            final ActionResponse actionResponse = internetGatewayDevice.getGenericPortMappingEntry(i);
            getLogger().info("index " + i + " mapping info:\n" + actionResponse);
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
   * Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  public abstract void registerRoleForRemoteCommunications(final RoleInfo roleInfo);

  /**
   * Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  public abstract void unregisterRoleForRemoteCommunications(final RoleInfo roleInfo);

  /**
   * Receives a Netty message object from a remote message router peer. The received message is verified before relaying to the local node
   * runtime.
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
    if (getLogger().isDebugEnabled()) {
      getLogger().debug("***** received from remote message router: " + message);
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

  /**
   * Gets the role channel dictionary.
   *
   * @return the role channel dictionary
   */
  public Map<String, Channel> getRoleChannelDictionary() {
    return roleChannelDictionary;
  }

  /**
   * Dispatch the given message to its recipient.
   *
   * @param message the Albus message
   */
  @Override
  public abstract void dispatchAlbusMessage(final Message message);

  /**
   * Provides a thread to execute the SSLProxy or SSL endpoint that handles an inbound Chord message, and which might then subsequently
   * block the I/O thread while awaiting a conversational response from a Chord network peer. The parent thread is an
   * AbstractAlbusHCSMessageHandler and must not block.
   */
  static class AlbusMessageDispatchRunner implements Runnable {

    /**
     * the SSL proxy, SSL endpoint, or node runtime
     */
    private final AlbusMessageDispatcher albusMessageDispatcher;
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
            final AlbusMessageDispatcher albusMessageDispatcher,
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
      albusMessageDispatcher.dispatchAlbusMessage(message);
    }
  }

  /**
   * Verifies a message sent between roles in the Albus hierarchical control system network, throwing an exception if the message's digital
   * signature fails verification.
   *
   * @param message the message
   */
  protected abstract void verifyMessage(final Message message);

  /**
   * Gets the role information,e.g. location and creditials, using the given role id as the key.
   *
   * @param roleId the given role id
   *
   * @return the role information
   */
  public abstract RoleInfo getRoleInfo(final URI roleId);

  /**
   * Routes the given message to the responsible peer message router.
   *
   * @param message the Albus message
   */
  protected abstract void routeAlbusMessageToPeerRouter(final Message message);

  /**
   * Finalizes the message router and releases its resources.
   */
  public abstract void finalization();
}
