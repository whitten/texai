package org.texai.ahcsSupport;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.cert.X509Certificate;
import java.util.Objects;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.StringUtils;

/**
 * SeedNodesInfo.java
 *
 * Description:
 *
 * Copyright (C) Nov 11, 2014, Stephen L. Reed, Texai.org.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
public class SeedNodesInfo implements Serializable {

  // the default serial version UID

  private static final long serialVersionUID = 1L;
  // the qualified name of the role, container.agent.role
  private final String qualifiedName;
  // the internet IP address
  private final InetAddress inetAddress;
  // the TCP port
  private final int port;
  // the configuration role's certificate
  private final X509Certificate x509Certificate;

  /**
   * Creates a new instance of SeedNodesInfo.
   * @param qualifiedName the qualified name of the role, container.agent.role
   * @param inetAddress the internet IP address
   * @param port the TCP port
   * @param x509Certificate the configuration role's certificate
   */
  public SeedNodesInfo(
          final String qualifiedName,
          final InetAddress inetAddress,
          final int port,
          final X509Certificate x509Certificate) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";
    assert Role.extractRoleName(qualifiedName).equals("SingletonConfigurationRole") : "must be SingletonConfigurationRole";
    assert inetAddress != null : "inetAddress must not be null";
    assert port > 1024 : "port must be greater than 1024";
    assert x509Certificate != null : "x509Certificate must not be null";

    this.qualifiedName = qualifiedName;
    this.inetAddress = inetAddress;
    this.port = port;
    this.x509Certificate = x509Certificate;
  }

  /** Gets the qualified name of the role, container.agent.role.
   *
   * @return the qualified name of the role, container.agent.role
   */
  public String getQualifiedName() {
    return qualifiedName;
  }

  /** Gets the internet IP address.
   *
   * @return the internet IP address
   */
  public InetAddress getInetAddress() {
    return inetAddress;
  }

  /** Gets the TCP port.
   *
   * @return the TCP port
   */
  public int getPort() {
    return port;
  }

  /** Gets the configuration role's certificate.
   *
   * @return the configuration role's certificate
   */
  public X509Certificate getX509Certificate() {
    return x509Certificate;
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
    final SeedNodesInfo other = (SeedNodesInfo) obj;
    if (!Objects.equals(this.qualifiedName, other.qualifiedName)) {
      return false;
    }
    return true;
  }



  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[Seed ")
            .append(Node.extractContainerName(qualifiedName))
            .append(' ')
            .append(inetAddress)
            .append(':')
            .append(port)
            .append(']')
            .toString();
  }
}
