/*
 * NodeRuntime.java
 *
 * Created on Mar 12, 2010, 12:12:19 PM
 *
 * Description: Provides runtime support for nodes in the local JVM.
 *
 * Copyright (C) Mar 12, 2010 reed.
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
package org.texai.ahcsSupport;

import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.x509.X509SecurityInfo;

/** Provides runtime support for nodes in the local JVM.
 *
 * @author reed
 */
public interface NodeRuntime extends AlbusMessageDispatcher {

  /** Registers the given role.
   *
   * @param role the given role
   */
  void registerRole(final Role role);

  /** Unregisters the given role.
   *
   * @param role the given role
   */
  void unregisterRole(final Role role);

  /** Registers the role for remote communications.
   *
   * @param roleInfo the role information
   */
  void registerRoleForRemoteCommunications(final RoleInfo roleInfo);

  /** Unregisters the role for remote communications.
   *
   * @param roleInfo the role information
   */
  void unregisterRoleForRemoteCommunications(final RoleInfo roleInfo);

  /** Gets the role id that identifies this node runtime when it communicates on behalf of itself.
   *
   * @return the role id that identifies this node runtime when it communicates on behalf of itself
   */
  URI getRoleId();

  /** Gets the node runtime RDF entity manager.
   *
   * @return the node runtime RDF entity manager
   */
  RDFEntityManager getRdfEntityManager();

  /** Sets X.509 security information and the id for the given new role.
   *
   * @param role the given unpersisted role
   */
  void setX509SecurityInfoAndIdForRole(final Role role);

  /** Gets the X.509 security information for this node runtime.
   *
   * @return the X.509 security information for this node runtime
   */
  X509SecurityInfo getX509SecurityInfo();

  /** Gets the executor.
   *
   * @return the executor
   */
  Executor getExecutor();

  /** Gets the local area network ID.
   *
   * @return the local area network ID
   */
  UUID getLocalAreaNetworkID();

  /** Gets the host address as presented to the Internet, e.g. texai.dyndns.org.
   *
   * @return the host address as presented to the Internet
   */
  String getExternalHostName();

  /** Gets the TCP port as presented to the Internet.
   *
   * @return the TCP port as presented to the Internet
   */
  int getExternalPort();

  /** Gets the host address as presented to the LAN, e.g. turing.
   *
   * @return the host address as presented to the LAN
   */
  String getInternalHostName();

  /** Gets the TCP port as presented to the LAN.
   *
   * @return the TCP port as presented to the LAN
   */
  int getInternalPort();

  /** Gets the launcher role id.
   *
   * @return the launcher role id
   */
  URI getLauncherRoleId();

  /** Gets the top friendship role.
   *
   * @return the top friendship role
   */
  Role getTopFriendshipRole();

  /** Gets the node access object.
   *
   * @return the node access object
   */
  NodeAccess getNodeAccess();

  /** Gets the local role having the given id.
   *
   * @param roleId the role id
   * @return the local role having the given id, or null if not found
   */
  Role getLocalRole(final URI roleId);

  /** Returns the singleton node within the scope of this node runtime having the given node type.
   *
   * @param nodeTypeName the given node type
   * @return the singleton node, or null if not found
   */
  Node getSingletonNodeOfType(final String nodeTypeName);

  /** Returns the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @return the node having the given nickname, or null if not found
   */
  Node getNode(final String nodeNickname);

  /** Adds the given local node.
   *
   * @param node the local node to add
   */
  void addNode(final Node node);

  /** Removes the given local node.
   *
   * @param node the given local node to remove
   */
  void removeNode(final Node node);

  /** Returns the id of the role having the given type contained in the node having the given nickname.
   *
   * @param nodeNickname the given nickname
   * @param roleTypeName the given role type
   * @return the id of the role having the given type contained in the node having the given nickname, or null if not found
   */
  URI getRoleId(
          final String nodeNickname,
          final String roleTypeName);

  /** Gets the timer.
   *
   * @return the timer
   */
  Timer getTimer();

  /** Gets the node runtime id.
   *
   * @return the node runtime id
   */
  URI getNodeRuntimeId();

  /** Shuts down the node runtime. */
  void shutdown();

  /** Adds the given operation to the list of logged operations.
   *
   * @param operation the given operation
   */
  void addLoggedOperation(final String operation);

  /** Removes the given operation from the list of logged operations.
   *
   * @param operation the given operation
   */
  void removeLoggedOperation(final String operation);

  /** Returns whether the given message is to be logged.
   *
   * @param message the given message
   * @return whether the given message is to be logged
   */
  boolean isMessageLogged(final Message message);


  /** Returns whether the given role id belongs to a local role.
   *
   * @param roleId the given role id
   * @return whether the given role id belongs to a local role
   */
  boolean isLocalRole(final URI roleId);
}
