/*
 * RoleInfo.java
 *
 * Created on Apr 13, 2010, 10:32:10 AM
 *
 * Description: Provides a role information container for persisting in the Chord distributed hash table.
 *
 * Copyright (C) Apr 13, 2010, Stephen L. Reed.
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

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.openrdf.model.URI;
import org.texai.kb.util.UUIDUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SerializableObjectSigner;
import org.texai.x509.X509Utils;

/** Provides a role information container for persisting in the Chord distributed hash table.
 *
 * @author reed
 */
@Immutable
public class RoleInfo implements Serializable {

  /** the serial version universal identifier */
  private static final long serialVersionUID = 1L;
  /** the role id, which is the key to retrieving this RoleInfo from the Chord distributed hash table */
  private final URI roleId;
  /** the role's local area network ID, used to determine whether the internal or external host address should be used to access it */
  private final UUID localAreaNetworkID;
  /** the host address as presented to the Internet, e.g. texai.dyndns.org */
  private final String externalHostName;
  /** the TCP port as presented to the Internet */
  private final int externalPort;
  /** the host address as presented to the LAN, e.g. turing */
  private final String internalHostName;
  /** the TCP port as presented to the LAN */
  private final int internalPort;
  /** the role's X.509 certificate path */
  private final CertPath certPath;
  /** the signature bytes of this RoleInfo by the role's X.509 certificate */
  private byte[] signatureBytes;

  /** Constructs a new RoleInfo instance.
   * @param roleId the role id, which is the key to retrieving this RoleInfo from the Chord distributed hash table
   * @param certPath the role's X.509 certificate path
   * @param privateKey the role's private key
   * @param localAreaNetworkID the role's local area network ID
   * @param externalHostName the external host name
   * @param externalPort the external port
   * @param internalHostName the internal host name
   * @param internalPort the internal port
   */
  public RoleInfo(
          final URI roleId,
          final CertPath certPath,
          final PrivateKey privateKey,
          final UUID localAreaNetworkID,
          final String externalHostName,
          final int externalPort,
          final String internalHostName,
          final int internalPort) {
    //Preconditions
    assert roleId != null : "roleId must not be null";
    assert certPath != null : "certPath must not be null";
    assert !certPath.getCertificates().isEmpty() : "certPath must not be empty";
    assert X509Utils.getUUID((X509Certificate) certPath.getCertificates().get(0)).equals(UUIDUtils.uriToUUID(roleId)) :
            "roleId must equal certificate UID, roleId: " + roleId + " as UUID: " + UUIDUtils.uriToUUID(roleId)
            + "\ncertificate UID: " + X509Utils.getUUID((X509Certificate) certPath.getCertificates().get(0))
            + "\nX.509 certificate:\n" + certPath.getCertificates().get(0);
    assert privateKey != null : "privateKey must not be null";
    assert localAreaNetworkID != null : "localAreaNetworkID must not be null";
    assert externalHostName != null : "externalHostName must not be null";
    assert !externalHostName.isEmpty() : "externalHostName must not be empty";
    assert externalPort > 0 : "externalPort must be positive";
    assert internalHostName != null : "internalHostName must not be null";
    assert !internalHostName.isEmpty() : "internalHostName must not be empty";
    assert internalPort > 0 : "internalPort must be positive";

    this.roleId = roleId;
    this.certPath = certPath;
    this.localAreaNetworkID = localAreaNetworkID;
    this.externalHostName = externalHostName;
    this.externalPort = externalPort;
    this.internalHostName = internalHostName;
    this.internalPort = internalPort;
    try {
      signatureBytes = SerializableObjectSigner.sign(this, privateKey);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Gets the role id, which is the key to retrieving this RoleInfo from the Chord distributed hash table.
   *
   * @return the role id
   */
  public URI getRoleId() {
    return roleId;
  }

  /** Gets the role's X.509 certificate path.
   *
   * @return the role's X.509 certificate path
   */
  public CertPath getCertPath() {
    return certPath;
  }

  /** Gets the role's X.509 certificate.
   *
   * @return the role's X.509 certificate
   */
  public X509Certificate getRoleX509Certificate() {
    return (X509Certificate) certPath.getCertificates().get(0);
  }

  /** Gets the signature bytes of this RoleInfo by the role's X.509 certificate.
   *
   * @return the signature bytes
   */
  public byte[] getSignatureBytes() {
    return signatureBytes;
  }

  /** Returns whether the contained signature verifies this RoleInfo, throwing an exception if not OK.
   *
   * @return whether the given signature verifies the given file
   */
  public boolean verify() {
    //Preconditions
    assert signatureBytes.length > 0 : "signatureBytes must not be empty";

    final byte[] savedSignatureBytes = signatureBytes;
    signatureBytes = null;
    final boolean result;
    try {
      result = SerializableObjectSigner.verify(
              this,
              (X509Certificate) certPath.getCertificates().get(0),
              savedSignatureBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
    signatureBytes = savedSignatureBytes;
    return result;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether the other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof RoleInfo) {
      final RoleInfo that = (RoleInfo) obj;
      if (this.roleId.equals(that.roleId)) {
        assert this.certPath.equals(that.certPath);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return roleId.hashCode();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[RoleInfo ");
    stringBuilder.append(roleId);
    stringBuilder.append(", lan: ");
    stringBuilder.append(localAreaNetworkID);
    stringBuilder.append(", external host address: ");
    stringBuilder.append(externalHostName);
    stringBuilder.append(":");
    stringBuilder.append(externalPort);
    stringBuilder.append(", internal host address: ");
    stringBuilder.append(internalHostName);
    stringBuilder.append(":");
    stringBuilder.append(internalPort);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Gets the host address as presented to the Internet, e.g. texai.dyndns.org.
   *
   * @return the host address as presented to the Internet
   */
  public String getExternalHostName() {
    return externalHostName;
  }

  /** Gets the TCP port as presented to the Internet.
   *
   * @return the TCP port as presented to the Internet
   */
  public int getExternalPort() {
    return externalPort;
  }

  /** Gets the host address as presented to the LAN, e.g. turing.
   *
   * @return the host address as presented to the LAN
   */
  public String getInternalHostName() {
    return internalHostName;
  }

  /** Gets the TCP port as presented to the LAN.
   *
   * @return the TCP port as presented to the LAN
   */
  public int getInternalPort() {
    return internalPort;
  }

  /** Gets the role's local area network ID, used to determine whether the internal or external host address should be used to access it.
   *
   * @return the role's local area network ID
   */
  public UUID getLocalAreaNetworkID() {
    return localAreaNetworkID;
  }
}
