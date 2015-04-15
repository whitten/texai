package org.texai.ahcsSupport.domainEntity;

import java.util.Objects;
import java.util.Set;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.ArraySet;
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
  // the port
  @RDFProperty()
  private int port;
  // the indicator whether this node is a super peer, if so then this container connects to all the other super peer containers
  @RDFProperty()
  private final boolean isSuperPeer;
  // the indicator whether this container is the first host of all the network singleton agents, when the network restarts
  @RDFProperty()
  private final boolean isFirstContainer;
  // the indicator whether this node is a client gateway, accepting connections from wallet clients
  @RDFProperty()
  private final boolean isClientGateway;
  // the indicator whether this node is a block explorer, serving the Insight application
  @RDFProperty()
  private final boolean isBlockExplorer;
  // the names of the few super peer containers to which a non-super peer container connects
  @RDFProperty()
  private final Set<String> superPeerContainerNames = new ArraySet<>();
  // the indicator whether this node is alive
  private transient boolean isAlive = false;

  /**
   * Creates a new default instance of ContainerInfo.
   */
  public ContainerInfo() {
    this.containerName = null;
    this.isSuperPeer = false;
    this.isFirstContainer = false;
    this.isClientGateway = false;
    this.isBlockExplorer = false;
  }

  /**
   * Creates a new instance of ContainerInfo.
   *
   * @param containerName the container name
   * @param isSuperPeer the indicator whether this node is a super peer
   * @param isFirstContainer the indicator whether this node is a client gateway, accepting connections from wallet clients
   * @param isClientGateway the indicator whether this node is a client gateway, accepting connections from wallet clients
   * @param isBlockExplorer the indicator whether this node is a block explorer, serving the Insight application
   */
  public ContainerInfo(
          final String containerName,
          final boolean isSuperPeer,
          final boolean isFirstContainer,
          final boolean isClientGateway,
          final boolean isBlockExplorer) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";
    assert !isFirstContainer || isSuperPeer : "first container must be a super peer";

    this.containerName = containerName;
    this.isSuperPeer = isSuperPeer;
    this.isFirstContainer = isFirstContainer;
    this.isClientGateway = isClientGateway;
    this.isBlockExplorer = isBlockExplorer;
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
  public boolean isSuperPeer() {
    return isSuperPeer;
  }

  /**
   * Gets the indicator whether this container is the first host of all the network singleton agents, when the network restarts.
   *
   * @return whether this container is the first host of all the network singleton agents
   */
  public boolean isFirstContainer() {
    return isFirstContainer;
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
    if (isFirstContainer) {
      stringBuilder.append(", first container");
    }
    if (isClientGateway) {
      stringBuilder.append(", gateway");
    }
    if (isBlockExplorer) {
      stringBuilder.append(", block explorer");
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /**
   * Adds a super peer container name to which this peer connects.
   *
   * @param containerName the container name
   */
  public void addSuperPeerContainerName(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-emtpy string";

    superPeerContainerNames.add(containerName);
  }

  /**
   * Gets the super peer container names to which this peer connects.
   *
   * @return the super peer container names to which a non-super peer connects
   */
  public Set<String> getSuperPeerContainerNames() {
    return superPeerContainerNames;
  }

  /**
   * Gets whether this node is a client gateway, accepting connections from wallet clients.
   *
   * @return the indicator whether this node is a client gateway
   */
  public boolean isClientGateway() {
    return isClientGateway;
  }

  /**
   * Gets whether this node is a block explorer, serving the Insight application.
   *
   * @return whether this node is a block explorer
   */
  public boolean isBlockExplorer() {
    return isBlockExplorer;
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
   *
   * @param that the other NodeInfo
   */
  @Override
  public int compareTo(final ContainerInfo that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.containerName.compareTo(that.containerName);
  }

  /** Gets the port.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /** Sets the port.
   *
   * @param port the port to set
   */
  public void setPort(final int port) {
    //Preconditions
    assert port > 1024 && port < 65535 : "port must be in the range, 1025 ... 65535";

    this.port = port;
  }

}
