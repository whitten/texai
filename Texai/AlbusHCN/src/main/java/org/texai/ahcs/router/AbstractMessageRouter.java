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
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.sbbi.upnp.impls.InternetGatewayDevice;
import net.sbbi.upnp.messages.ActionResponse;
import net.sbbi.upnp.messages.UPNPResponseException;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AlbusMessageDispatcher;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.network.netty.ConnectionUtils;
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
     * the role/channel dictionary, role id string --> channel to peer message
     * router
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
     * the role id strings of local roles that are registered for communication
     * with remote roles
     */
    private final Set<String> localRoles = new HashSet<>();

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
     * Uses Universal Plug and Play to configure the NAT router to forward the
     * given port to this host.
     *
     * @param internalPort the port for this message router
     * @param externalPort the NAT port to forward to this host
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
     * Receives a Netty message object from a remote message router peer. The
     * received message is verified before relaying to the local node runtime.
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
     * Route the given message.
     *
     * @param message the Albus message
     */
    @Override
    public abstract void dispatchAlbusMessage(final Message message);

    /**
     * Provides a thread to execute the SSLProxy or SSL endpoint that handles an
     * inbound Chord message, and which might then subsequently block the I/O
     * thread while awaiting a conversational response from a Chord network
     * peer. The parent thread is an AbstractAlbusHCSMessageHandler and must not
     * block.
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
         * @param albusMessageDispatcher the SSL proxy, SSL endpoint, or node
         * runtime
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
     * Verifies a message sent between roles in the Albus hierarchical control
     * system network, throwing an exception if the message's digital signature
     * fails verification.
     *
     * @param message the message
     */
    protected abstract void verifyMessage(final Message message);

    /**
     * Retrieves the role information from the Chord network using the given
     * role id as the key.
     *
     * @param roleId the given role id
     * @return the role information from the Chord network
     */
    public abstract RoleInfo getRoleInfo(final URI roleId);

    /**
     * Routes the given message to the responsible peer message router.
     *
     * @param message the Albus message
     */
    protected void routeAlbusMessageToPeerRouter(final Message message) {
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
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("finding a channel ...");
                }
                if (message.getRecipientRoleId().getLocalName().isEmpty()) {
                    // the message is a Chord operation, e.g. the receipient is http://mccarthy.local:5048/
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("  creating a channel to route Chord operation");
                        getLogger().debug("    from node runtime: " + nodeRuntime.getX509SecurityInfo().getX509Certificate().getSubjectDN());
                    }
                    channel = ConnectionUtils.openAlbusHCSConnection(
                            NetworkUtils.makeInetSocketAddress(recipientRoleIdString), // inetSocketAddress
                            nodeRuntime.getX509SecurityInfo(), // albusHCSMessageHandler
                            this, // sslHandshakeCompletedListener
                            nodeRuntime.getExecutor(), // bossExecutor
                            nodeRuntime.getExecutor()); // workerExecutor
                } else {
                    // the message is between two roles
                    getLogger().info("  routing message beteen two roles");
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
                            getLogger().info("  found existing channel to " + inetSocketAddress);
                            break;
                        }
                    }
                    if (channel == null) {
                        // no channel to the target node runtime exists yet, so create one
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("creating a channel to route between two roles");
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
                getLogger().info("peer has shutdown " + message.getRecipientRoleId());
                return;
            }
            assert channel.isConnected() : "channel must be connected";
            assert channel.isReadable() : "channel must be readable";
            assert channel.isWritable() : "channel must be writable";
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("transmitting outbound message on channel " + channel);
            }
            synchronized (channel) {
                final ChannelFuture channelFuture = channel.write(message);
                if (getLogger().isDebugEnabled()) {
                    channelFuture.addListener((final ChannelFuture future) -> {
                        if (getLogger().isDebugEnabled()) {
                            getLogger().debug("  message transmission completed");
                        }
                    });
                }
            }
        }
    }

    /**
     * Finalizes the message router and releases its resources.
     */
    public abstract void finalization();
}
