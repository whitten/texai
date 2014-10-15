package org.texai.tamperEvidentLogs.domainEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.DigestOutputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import net.jcip.annotations.Immutable;
import org.bouncycastle.util.Arrays;
import org.joda.time.DateTime;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SerializableObjectSigner;
import org.texai.x509.X509Utils;

/**
 * TELogAuthenticatorEntry.java
 *
 * Description: Provides a tamper-evident log authenticator entry, in which the logging agent or a verifying peer digitally signs a log.
 *
 * Copyright (C) Sep 30, 2014, Stephen L. Reed, Texai.org.
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
@Immutable
@RDFEntity(context = "texai:TamperEvidentLogContext")
public class TELogAuthenticatorEntry extends AbstractTELogEntry {

  // the default serial version uid
  private static final long serialVersionUID = 1L;
  // the signing agent's name
  @RDFProperty
  private final String signingAgentName;
  // the signing agent's X.509 signature, persisted as an 8-bit string, based upon the previous entry's digest
  @RDFProperty
  private final String encodedSignatureBytes;

  /**
   * Creates a new empty instance of TELogAuthenticatorEntry, which is used by the persistence framework.
   */
  public TELogAuthenticatorEntry() {
    signingAgentName = null;
    encodedSignatureBytes = null;
  }

  /**
   * Constructs a new TELogAuthenticatorEntry.
   *
   * @param signingAgentName the signing agent's name
   * @param encodedSignatureBytes the signing agent's X.509 signature, encoded in base 64 based upon the previous entry's digest
   * @param previousTELogEntry the previous tamper-evident log entry, or empty if this is the first
   * @param timestamp the logger's local timestamp, which is constrained to be unique for ordering the log
   * @param chaosValue an optional chaos value
   * @param encodedDigest the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation
   * fields, encoded in base 64
   */
  public TELogAuthenticatorEntry(
          final String signingAgentName,
          final String encodedSignatureBytes,
          final AbstractTELogEntry previousTELogEntry,
          final DateTime timestamp,
          final String chaosValue,
          final String encodedDigest) {
    super(previousTELogEntry, timestamp, chaosValue, encodedDigest);
    //Preconditions
    assert StringUtils.isNonEmptyString(signingAgentName) : "signingAgentName must be a non-empty string";
    assert StringUtils.isNonEmptyString(encodedSignatureBytes) : "encodedSignatureBytes must not be null";
    assert StringUtils.isNonEmptyString(encodedDigest) : "encodedDigest must not be null";

    this.signingAgentName = signingAgentName;
    this.encodedSignatureBytes = encodedSignatureBytes;
  }

  /**
   * Verifies whether this tamper-evident log entry's digest is equal to a SHA-512 rehash of its fields, excluding the signature bytes.
   *
   * @return whether this tamper-evident log entry's digest is equal to a SHA-512 rehash of its fields
   */
  @Override
  public boolean verifyDigest() {
    final byte[] recomputedDigest = makeTELogAuthenticatorEntryDigest(
            signingAgentName,
            encodedSignatureBytes,
            getPreviousTELogEntry(),
            getTimestamp(),
            getChaosValue());
    return Arrays.areEqual(recomputedDigest, Base64Coder.decode(getEncodedDigest()));
  }

  /**
   * Makes a SHA-512 hash digest of the given TELogAuthenticatorEntry fields.
   *
   * @param signingAgentName the signing agent's name
   * @param encodedSignatureBytes the base 64 encoded signature of the previous entries digest
   * @param previousTELogEntry the previous tamper-evident log entry
   * @param timestamp the logger's local timestamp, which is constrained to be unique for ordering the log
   * @param chaosValue an optional chaos value
   *
   * @return a SHA-512 hash digest
   */
  public static byte[] makeTELogAuthenticatorEntryDigest(
          final String signingAgentName,
          final String encodedSignatureBytes,
          final AbstractTELogEntry previousTELogEntry,
          final DateTime timestamp,
          final String chaosValue) {
    //Preconditions
    assert StringUtils.isNonEmptyString(signingAgentName) : "signingAgentName must be a non-empty string";
    assert timestamp != null : "timestamp must not be null";
    if (previousTELogEntry == null) {
      throw new TexaiException("previousTELogEntry must not be null");
    }

    try {
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", X509Utils.BOUNCY_CASTLE_PROVIDER);
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      final DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, messageDigest);
      final ObjectOutputStream objectOutputStream = new ObjectOutputStream(digestOutputStream);
      // link the previous log entry in a hash chain
      objectOutputStream.writeObject(previousTELogEntry.getEncodedDigest());
      // hash the fields for this log entry
      objectOutputStream.writeObject(signingAgentName);
      objectOutputStream.writeObject(encodedSignatureBytes);
      objectOutputStream.writeObject(timestamp);
      if (chaosValue != null) {
        objectOutputStream.writeObject(chaosValue);
      }
      final byte[] digest = messageDigest.digest();

      //Postconditions
      assert digest != null : "digest must not be null";
      assert digest.length > 0 : "digest must not be empty";

      return digest;
    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Signs the given previous tamper-evident log entry's digest.
   *
   * @param digest the given previous entry's digest
   * @param x509Certificate the signing agent's X.509 certificate
   * @param privateKey the signing agent's private key
   *
   * @return an X.509 signature
   */
  public static byte[] signTELogAuthenticatorEntry(
          final byte[] digest,
          final X509Certificate x509Certificate,
          final PrivateKey privateKey) {
    //Preconditions
    assert digest != null : "digest must not be null";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert privateKey != null : "privateKey must not be null";

    try {
      final byte[] signatureBytes = SerializableObjectSigner.sign(
              digest,
              privateKey);

      //Postconditions
      assert signatureBytes != null : "signatureBytes must not be null";
      assert signatureBytes.length > 0 : "signatureBytes must not be empty";

      return signatureBytes;
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns whether the given X.509 certificate verifies this entry's signature.
   *
   * @param x509Certificate the sender's X.509 certificate, that contains the public key
   *
   * @return whether the given X.509 certificate verifies this entry's signature
   */
  public boolean verify(final X509Certificate x509Certificate) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";
    assert getPreviousTELogEntry() != null : "previousTELogEntry must not be null";

    try {
      return SerializableObjectSigner.verify(
              getPreviousTELogEntry().getDigest(),
              x509Certificate,
              getSignatureBytes());
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Return the signing agent's name, i.e. container-name.agent-name.role-name, from its X.509 certificate.
   *
   * @return the signing agent's name
   */
  public String getSigningAgentName() {
    return signingAgentName;
  }

  /**
   * Encodes a base 64 encoded version of the given the agent X.509 signature for persistence.
   *
   * @param signatureBytes the agent's X.509 signature
   *
   * @return a base 64 encoded version of the given digest
   */
  public static String encodeSignatureBytes(final byte[] signatureBytes) {
    assert signatureBytes != null : "digest must not be null";
    assert signatureBytes.length > 0 : "digest must not be empty";

    return new String(Base64Coder.encode(signatureBytes));
  }

  /**
   * Gets the base 64 encoded agent X.509 signature.
   *
   * @return the base 64 encoded agent X.509 signature
   */
  public String getEncodedSignatureBytes() {
    return encodedSignatureBytes;
  }

  /**
   * Gets the agent X.509 signature.
   *
   * @return the agent X.509 signature
   */
  public byte[] getSignatureBytes() {
    return Base64Coder.decode(encodedSignatureBytes);
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 53 * hash + Objects.hashCode(this.signingAgentName);
    hash = 53 * hash + Objects.hashCode(this.encodedSignatureBytes);
    return hash;
  }

  /**
   * Returns whether another object is equal to this one.
   *
   * @param obj the other object
   *
   * @return whether another object is equal to this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final TELogAuthenticatorEntry other = (TELogAuthenticatorEntry) obj;
    if (!Objects.equals(this.getPreviousTELogEntry(), other.getPreviousTELogEntry())) {
      return false;
    }
    if (!Objects.equals(this.getTimestamp(), other.getTimestamp())) {
      return false;
    }
    if (!Objects.equals(this.getChaosValue(), other.getChaosValue())) {
      return false;
    }
    if (!Objects.equals(this.getEncodedDigest(), other.getEncodedDigest())) {
      return false;
    }
    if (!this.signingAgentName.equals(other.signingAgentName)) {
      return false;
    } else {
      return Objects.equals(this.encodedSignatureBytes, other.encodedSignatureBytes);
    }
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[TELogAuthenticatorEntry ")
            .append(getTimestamp())
            .append(", chaosValue: ")
            .append(getChaosValue())
            .append(", signingAgentName: ")
            .append(signingAgentName)
            .append(']')
            .toString();
  }
}
