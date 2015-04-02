package org.texai.ahcsSupport.domainEntity;

import java.util.Objects;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;

/**
 * ContainerInfo.java
 *
 * Description: Provides a container for node information, such as container name and IP address.
 *
 * Copyright (C) Mar 26, 2015, Stephen L. Reed.
 */
@NotThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class ContainerInfo implements CascadePersistence, Comparable<ContainerInfo> {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerInfo.class);
  // the default serial version UID
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  private URI id;
  @RDFProperty()
  // the container name
  private final String containerName;
  // the IP address
  @RDFProperty()
  private String ipAddress;
  // the indicator whether this node is a super peer
  @RDFProperty()
  private boolean isSuperPeer = false;
  // the indicator whether this node is alive
  private transient boolean isAlive = false;

  /**
   * Creates a new default instance of ContainerInfo. */
  public ContainerInfo() {
    this.containerName = null;
  }

  /**
   * Creates a new instance of ContainerInfo.
   *
   * @param containerName the container name
   */
  public ContainerInfo(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

    this.containerName = containerName;
  }

  /**
   * Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /**
   * Gets the container name.
   *
   * @return the container name
   */
  public String getContainerName() {
    return containerName;
  }

  /**
   * Gets the IP address.
   *
   * @return the IP address
   */
  public synchronized String getIpAddress() {
    return ipAddress;
  }

  /**
   * Sets the IP address.
   *
   * @param ipAddress the IP address
   */
  public synchronized void setIpAddress(final String ipAddress) {
    //Preconditions
    assert StringUtils.isNonEmptyString(ipAddress) : "ipAddress must be a non-empty string";

    this.ipAddress = ipAddress;
  }

  /**
   * Gets the indicator whether this node is a super peer.
   *
   * @return whether this node is a super peer
   */
  public synchronized boolean isSuperPeer() {
    return isSuperPeer;
  }

  /**
   * Sets the indicator whether this node is a super peer.
   *
   * @param isSuperPeer the indicator whether this node is a super peer
   */
  public synchronized void setIsSuperPeer(final boolean isSuperPeer) {
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
    final ContainerInfo other = (ContainerInfo) obj;
    return Objects.equals(this.containerName, other.containerName);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("[container ")
            .append(containerName);
    if (ipAddress != null) {
      stringBuilder
              .append(",  ")
              .append(ipAddress);
    }
    if (isAlive) {
      stringBuilder.append(", alive");
    }
    if (isSuperPeer) {
      stringBuilder.append(", super peer");
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /**
   * Returns the indicator whether this node is alive.
   *
   * @return whether this node is alive
   */
  public synchronized boolean isAlive() {
    return isAlive;
  }

  /**
   * Sets the indicator whether this node is alive.
   *
   * @param isAlive whether this node is alive
   */
  public synchronized void setIsAlive(final boolean isAlive) {
    this.isAlive = isAlive;
  }

  /**
   * Ensures that this persistent object is fully instantiated.
   */
  @Override
  public void instantiate() {
  }

  /**
   * Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager, URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.persist(this, overrideContext);
  }

  /**
   * Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    rdfEntityManager.remove(this);
  }

  /**
   * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer as this object is
   * less than, equal to, or greater than the specified object.
   * @param that the other NodeInfo
   */
  @Override
  public int compareTo(final ContainerInfo that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.containerName.compareTo(that.containerName);
  }

}
