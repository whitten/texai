/*
 * MockNodeRuntime.java
 *
 * Created on Nov 9, 2011, 9:04:38 AM
 *
 * Description: .
 *
 * Copyright (C) Nov 9, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ahcsSupport;

import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.Executor;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.x509.X509SecurityInfo;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class MockNodeRuntime implements NodeRuntime {

  /** Constructs a new MockNodeRuntime instance. */
  public MockNodeRuntime() {
  }

  @Override
  public void registerRole(Role role) {
  }

  @Override
  public void unregisterRole(Role role) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void registerRoleForRemoteCommunications(RoleInfo roleInfo) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void unregisterRoleForRemoteCommunications(RoleInfo roleInfo) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public URI getRoleId() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public RDFEntityManager getRdfEntityManager() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void setX509SecurityInfoAndIdForRole(Role role) {
  }

  @Override
  public X509SecurityInfo getX509SecurityInfo() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Executor getExecutor() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public UUID getLocalAreaNetworkID() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getExternalHostName() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int getExternalPort() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public String getInternalHostName() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public int getInternalPort() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public URI getLauncherRoleId() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Role getTopFriendshipRole() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public NodeAccess getNodeAccess() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Role getLocalRole(URI roleId) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Node getNode(String nodeNickname) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void addNode(Node node) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removeNode(Node node) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Timer getTimer() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public URI getNodeRuntimeId() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void shutdown() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void addLoggedOperation(String operation) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void removeLoggedOperation(String operation) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isMessageLogged(Message message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isLocalRole(URI roleId) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void dispatchAlbusMessage(Message message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void registerSSLProxy(Object sslProxy) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public URI getRoleId(String nodeNickname, String roleType) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public Node getSingletonNodeOfType(String nodeTypeName) {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
