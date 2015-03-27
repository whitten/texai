package org.texai.skill.support;

import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.StringUtils;

/**
 * NodeInfo.java
 *
 * Description: Provides a container for node information, such as container name and IP address.
 *
 * Copyright (C) Mar 26, 2015, Stephen L. Reed.
 */
@NotThreadSafe
public class NodeInfo {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NodeInfo.class);
  // the container name
  private final String containerName;
  // the IP address
  private String ipAddress;
  // the indicator whether this node is a super peer
  private boolean isSuperPeer = false;

  /**
   * Creates a new instance of NodeInfo.
   *
   * @param containerName the container name
   */
  public NodeInfo(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

    this.containerName = containerName;
  }

  /**
   * Gets the container name.
   *
   * @return the container name
   */
  protected String getContainerName() {
    return containerName;
  }

  /**
   * Gets the IP address.
   *
   * @return the IP address
   */
  protected String getIpAddress() {
    return ipAddress;
  }

  /**
   * Sets the IP address.
   *
   * @param ipAddress the IP address
   */
  protected void setIpAddress(final String ipAddress) {
    //Preconditions
    assert StringUtils.isNonEmptyString(ipAddress) : "ipAddress must be a non-empty string";

    this.ipAddress = ipAddress;
  }

  /**
   * Gets the indicator whether this node is a super peer.
   *
   * @return whether this node is a super peer
   */
  protected boolean isSuperPeer() {
    return isSuperPeer;
  }

  /**
   * Sets the indicator whether this node is a super peer.
   *
   * @param isSuperPeer the indicator whether this node is a super peer
   */
  protected void setIsSuperPeer(final boolean isSuperPeer) {
    this.isSuperPeer = isSuperPeer;
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 53 * hash + Objects.hashCode(this.containerName);
    return hash;
  }

  /**
   * Returns whether another object equals this one.
   *
   * @param obj the other object
   *
   * @return whether another object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NodeInfo other = (NodeInfo) obj;
    if (!Objects.equals(this.containerName, other.containerName)) {
      return false;
    }
    return true;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[container ")
            .append(containerName)
            .append(",  ")
            .append(ipAddress)
            .append(']')
            .toString();
  }

}
