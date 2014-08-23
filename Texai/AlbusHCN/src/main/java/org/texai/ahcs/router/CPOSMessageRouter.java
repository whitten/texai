/*
 * CPOSMessageRouter.java
 *
 * Created on Aug 22, 2014, 9:24:48 AM
 *
 * Description: Provides a message router for the Cooperative Proof-of-Stake network.
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

import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.network.netty.ConnectionUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
public class CPOSMessageRouter extends AbstractMessageRouter {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(CPOSMessageRouter.class);
  /**
   * the X.509 certificate dictionary, role id string --> role information
   */
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final Map<String, RoleInfo> roleInfoDictionary = new HashMap<>();
  /**
   * the role id strings of local roles that are registered for communication with remote roles
   */
  private final Set<String> localRoles = new HashSet<>();

  /**
   * Constructs a new CPOSMessageRouter instance.
   *
   * @param nodeRuntime the node runtime
   */
  public CPOSMessageRouter(final NodeRuntime nodeRuntime) {
    super(nodeRuntime);
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void registerRoleForRemoteCommunications(RoleInfo roleInfo) {
    //TODO send a message to the roleInfoRecordingAgent registering the role info.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  @Override
  public void unregisterRoleForRemoteCommunications(RoleInfo roleInfo) {
    //TODO send a message to the roleInfoRecordingAgent unregistering the role info.
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * Dispatch the given message to its recipient.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchAlbusMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("dispatching message " + message);
      LOGGER.debug("  reply-with: " + message.getReplyWith());
    }

    LOGGER.info("");
    final String recipientRoleIdString = message.getRecipientRoleId().toString();
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
      getNodeRuntime().getExecutor().execute(new CPOSMessageRouter.AlbusMessageDispatchRunner(
              getNodeRuntime(), // albusMessageDispatcher
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
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  dispatch completed");
    }
  }

  /**
   * Verifies a message sent between roles in the Albus hierarchical control system network, throwing an exception if the message's digital
   * signature fails verification.
   *
   * @param message the message
   */
  @Override
  protected void verifyMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String senderRoleIdString = message.getSenderRoleId().toString();
    final RoleInfo roleInfo = roleInfoDictionary.get(senderRoleIdString);
    assert roleInfo != null : "roleInfo must not be null";
    roleInfo.verify();
    X509Certificate x509Certificate = roleInfo.getRoleX509Certificate();
    assert x509Certificate != null : "x509Certificate must not be null";
    message.verify(x509Certificate);
    LOGGER.info("verified message");
  }

  /**
   * Gets the role information,e.g. location and creditials, using the given role id as the key.
   *
   * @param roleId the given role id
   *
   * @return the role information
   */
  @Override
  public RoleInfo getRoleInfo(URI roleId) {
    //Preconditions
    assert roleId != null : "roleId must not be null";

    final RoleInfo roleInfo = roleInfoDictionary.get(roleId.toString());

    //Postconditions
    assert roleInfo != null : "roleInfo must not be null";

    return roleInfo;
  }

  /**
   * Routes the given message to the responsible peer message router.
   *
   * @param message the Albus message
   */
  @Override
  protected void routeAlbusMessageToPeerRouter(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String recipientRoleIdString = message.getRecipientRoleId().toString();
    Channel channel;
    RoleInfo roleInfo = null;
    if (!message.getRecipientRoleId().getLocalName().isEmpty()) {
      synchronized (getRoleChannelDictionary()) {
        channel = getRoleChannelDictionary().get(recipientRoleIdString);
      }
      if (channel == null) {
        // retrieve the role information from the Chord DHT before routing the message
        // to routeAlbusMessageToPeerRouter - avoiding deadlock on synchronized resources
        roleInfo = getRoleInfo(message.getRecipientRoleId());
        assert roleInfo != null : "roleInfo not foundfor " + message.getRecipientRoleId();
      }
    }

    synchronized (getRoleChannelDictionary()) {
      channel = getRoleChannelDictionary().get(recipientRoleIdString);
      if (channel == null) {
        // open a channel between this message router and the peer message router that services the recipient role
        if (getLogger().isDebugEnabled()) {
          getLogger().debug("finding a channel ...");
        }
        // the message is between two roles
        getLogger().info("  routing message beteen two roles");
        assert roleInfo != null;
        final InetSocketAddress inetSocketAddress;
        if (roleInfo.getLocalAreaNetworkID().equals(getNodeRuntime().getLocalAreaNetworkID())) {
          // both roles are hosted on the same LAN - so use its internal LAN address
          inetSocketAddress = new InetSocketAddress(roleInfo.getInternalHostName(), roleInfo.getInternalPort());
        } else {
          // the remote role is hosted on another LAN - so use its external, i.e. NAT-mapped, address
          inetSocketAddress = new InetSocketAddress(roleInfo.getExternalHostName(), roleInfo.getExternalPort());
        }

        // search for an existing channel to the target node runtime that was established for some other role
        synchronized (getRoleChannelDictionary()) {
          for (final Channel channel1 : getRoleChannelDictionary().values()) {
            final InetSocketAddress inetSocketAddress1 = (InetSocketAddress) channel1.getRemoteAddress();
            if (inetSocketAddress1.equals(inetSocketAddress)) {
              channel = channel1;
              getLogger().info("  found existing channel to " + inetSocketAddress);
              break;
            }
          }
        }
        if (channel == null) {
          // no channel to the target node runtime exists yet, so create one
          if (getLogger().isDebugEnabled()) {
            getLogger().debug("creating a channel to route between two roles");
          }
          channel = ConnectionUtils.openAlbusHCSConnection(
                  inetSocketAddress,
                  getNodeRuntime().getX509SecurityInfo(), // albusHCSMessageHandler
                  this, // sslHandshakeCompletedListener
                  getNodeRuntime().getExecutor(), // bossExecutor
                  getNodeRuntime().getExecutor()); // workerExecutor
        }
        assert channel != null;
        synchronized (getRoleChannelDictionary()) {
          getRoleChannelDictionary().put(recipientRoleIdString, channel);
        }
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
  @Override
  public void finalization() {
  }

  /**
   * When implemented by a Chord message router, registers the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  @Override
  public void registerSSLProxy(Object sslProxy) {
    throw new TexaiException("Not supported for this message router.");
  }

}
