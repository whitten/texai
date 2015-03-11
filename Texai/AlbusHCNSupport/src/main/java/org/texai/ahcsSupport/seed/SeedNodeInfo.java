package org.texai.ahcsSupport.seed;

import java.io.Serializable;
import java.util.Objects;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.StringUtils;

/**
 * SeedNodeInfo.java

 Description: Contains the location and credentials for a network seed node.
 *
 * Copyright (C) Nov 11, 2014, Stephen L. Reed, Texai.org.
 */
public class SeedNodeInfo implements Serializable {

  // the default serial version UID

  private static final long serialVersionUID = 1L;
  // the qualified name of the role, container.agent.role
  private final String qualifiedName;
  // the internet host name
  private final String hostName;
  // the TCP port
  private final int port;

  /**
   * Creates a new instance of SeedNodesInfo.
   * @param qualifiedName the qualified name of the role, container.agent.role
   * @param hostName the internet host name
   * @param port the TCP port
   */
  public SeedNodeInfo(
          final String qualifiedName,
          final String hostName,
          final int port) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";
    assert Role.extractRoleName(qualifiedName).equals("ContainerSingletonConfigurationRole") : "must be ContainerSingletonConfigurationRole";
    assert StringUtils.isNonEmptyString(hostName) : "hostName must be a non-empty string";
    assert port > 1024 : "port must be greater than 1024";

    this.qualifiedName = qualifiedName;
    this.hostName = hostName;
    this.port = port;
  }

  /** Gets the qualified name of the role, container.agent.role.
   *
   * @return the qualified name of the role, container.agent.role
   */
  public String getQualifiedName() {
    return qualifiedName;
  }

  /** Gets the internet host name.
   *
   * @return the internet host name
   */
  public String getHostName() {
    return hostName;
  }

  /** Gets the TCP port.
   *
   * @return the TCP port
   */
  public int getPort() {
    return port;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 79 * hash + Objects.hashCode(this.qualifiedName);
    return hash;
  }

  /** Returns whether another object equals this one.
   *
   * @param obj the other object
   * @return  whether another object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SeedNodeInfo other = (SeedNodeInfo) obj;
    return this.qualifiedName.equals(other.qualifiedName) && this.hostName.equals(other.hostName);
  }



  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[Seed ")
            .append(qualifiedName)
            .append(' ')
            .append(hostName)
            .append(':')
            .append(port)
            .append(']')
            .toString();
  }
}
