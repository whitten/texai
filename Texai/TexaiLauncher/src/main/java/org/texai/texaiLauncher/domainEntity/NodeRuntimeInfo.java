/*
 * NodeRuntimeInfo.java
 *
 * Created on Sep 17, 2011, 10:40:48 AM
 *
 * Description: Describes a node runtime in an Albus Hierarchical Control System for the launcher.
 *
 * Copyright (C) Sep 17, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.texaiLauncher.domainEntity;

import java.security.cert.X509Certificate;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.netty.channel.Channel;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.NetworkUtils;

/** Describes a node runtime in an Albus Hierarchical Control System for the launcher.
 *
 * @author reed
 */
@NotThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class NodeRuntimeInfo implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the node runtime internal port, which is used with the LAN */
  @RDFProperty
  private int internalPort;
  /** the node runtime external port, which is exposed to the Internet */
  @RDFProperty
  private int externalPort;
  /** the communications channel connecting the launcher with the node runtime */
  private Channel channel;
  /** the node runtime X509 certificate */
  private X509Certificate x509Certificate;

  /** Constructs a new NodeRuntimeInfo instance. */
  public NodeRuntimeInfo() {
    internalPort = 0;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the node runtime internal port.
   *
   * @return the port
   */
  public int getInternalPort() {
    return internalPort;
  }

  /** Sets the node runtime internal port.
   *
   * @param internalPort the internal port
   */
  public void setInternalPort(final int internalPort) {
    //Preconditions
    assert internalPort > 0 : "internalPort must be positive";

    this.internalPort = internalPort;
  }

  /** Gets the communications channel connecting the launcher with the node runtime.
   *
   * @return the channel
   */
  public Channel getChannel() {
    return channel;
  }

  /** Sets the communications channel connecting the launcher with the node runtime.
   *
   * @param channel the channel
   */
  public void setChannel(final Channel channel) {
    //Preconditions
    assert channel != null : "channel must not be null";

    this.channel = channel;
  }

  /** Gets the node runtime X509 certificate.
   *
   * @return the x509Certificate
   */
  public X509Certificate getX509Certificate() {
    return x509Certificate;
  }

  /** Sets the node runtime X509 certificate.
   *
   * @param x509Certificate the x509Certificate
   */
  public void setX509Certificate(final X509Certificate x509Certificate) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";

    this.x509Certificate = x509Certificate;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[node runtime " + NetworkUtils.getHostName() + "/" + internalPort + "]";
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NodeRuntimeInfo other = (NodeRuntimeInfo) obj;
    if (this.id != other.id && (this.id == null || !this.id.equals(other.id))) {
      return false;
    }
    return true;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 29 * hash + (this.id != null ? this.id.hashCode() : 0);
    return hash;
  }

  /** Gets the node runtime external port, which is exposed to the Internet.
   *
   * @return the externalPort the node runtime external port
   */
  public int getExternalPort() {
    return externalPort;
  }

  /** Sets the node runtime external port, which is exposed to the Internet.
   *
   * @param externalPort the node runtime external port
   */
  public void setExternalPort(final int externalPort) {
    //Preconditions
    assert externalPort > 0 : "externalPort must be positive";

    this.externalPort = externalPort;
  }

}
