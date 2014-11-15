/*
 * SingletonAgentHosts.java
 *
 * Created on Aug 23, 2010, 3:04:17 PM
 *
 * Description: Provides a SPARQL query container that is used to invoke a certain skill at a certain service.
 *
 * Copyright (C) Aug 23, 2010, Stephen L. Reed.
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
package org.texai.skill.domainEntity;

import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SerializableObjectSigner;

/**
 * Provides a SPARQL query container that is used to invoke a certain skill at a certain service.
 *
 * @author reed
 */
@RDFEntity(context = "texai:BootstrapSkills")
@Immutable
@ThreadSafe
public final class SingletonAgentHosts implements RDFPersistent, Serializable {

  // the serial version ID
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  private URI id;
  // the singleton agent dictionary, agent name  --> hosting container name
  @RDFProperty(mapKeyType = "java.lang.String", mapValueType = "java.lang.String")
  private final Map<String, String> singletonAgentDictionary;
  // the date time when this assignment of singleton agents to hosts is effective
  @RDFProperty
  private final DateTime effectiveDateTime;
  // the date time when this assignment of singleton agents to hosts is terminated
  @RDFProperty
  private final DateTime terminationDateTime;
  // the author agent/role
  @RDFProperty
  private final String authorQualifiedName;
  // the date time when this assignment was created
  @RDFProperty
  private final DateTime createdDateTime;
  // the signature of the author agent/role
  @RDFProperty
  private final byte[] authorSignatureBytes;

  /**
   * Constructs a new SkillInvokingQueryContainer instance for use by the persistence framework.
   */
  public SingletonAgentHosts() {
    singletonAgentDictionary = null;
    effectiveDateTime = null;
    terminationDateTime = null;
    authorQualifiedName = null;
    createdDateTime = null;
    authorSignatureBytes = null;
  }

  /**
   * Constructs a new SkillInvokingQueryContainer instance, making an immutable copy of the singleton agent dictionary.
   *
   * @param singletonAgentDictionary the singleton agent dictionary, agent name --> hosting container name
   * @param effectiveDateTime the date time when this assignment of singleton agents to hosts is effective
   * @param terminationDateTime the date time when this assignment of singleton agents to hosts is terminated
   * @param authorQualifiedName the author agent/role
   * @param createdDateTime the date time when this assignment was created
   * @param authorSignatureBytes the signature of the author agent/role
   */
  public SingletonAgentHosts(
          final Map<String, String> singletonAgentDictionary,
          final DateTime effectiveDateTime,
          final DateTime terminationDateTime,
          final String authorQualifiedName,
          final DateTime createdDateTime,
          final byte[] authorSignatureBytes
  ) {
    //Preconditions
    assert singletonAgentDictionary != null : "singletonAgentDictionary must not be null";
    assert !singletonAgentDictionary.isEmpty() : "singletonAgentDictionary must not be empty";
    assert effectiveDateTime != null : "effectiveDateTime must not be null";
    assert terminationDateTime != null : "terminationDateTime must not be null";
    assert StringUtils.isNonEmptyString(authorQualifiedName) : "authorQualifiedName must be a non-empty string";
    assert createdDateTime != null : "createdDateTime must not be null";
    assert authorSignatureBytes != null : "authorSignatureBytes must not be null";
    assert authorSignatureBytes.length > 0 : "authorSignatureBytes must not be empty";
    assert createdDateTime.isBefore(effectiveDateTime) : "createdDateTime must be before effectiveDateTime";
    assert effectiveDateTime.isBefore(terminationDateTime) : "effectiveDateTime must be before terminationDateTime";

    this.singletonAgentDictionary = new HashMap<>(singletonAgentDictionary);
    this.effectiveDateTime = effectiveDateTime;
    this.terminationDateTime = terminationDateTime;
    this.authorQualifiedName = authorQualifiedName;
    this.createdDateTime = createdDateTime;
    this.authorSignatureBytes = authorSignatureBytes;
  }

  //TODO work out a canonical serialization of the map, e.g. two lists of strings, sorted by key
  /**
   * Signs the fields of a SingletonAgentHosts object.
   *
   * @param singletonAgentDictionary the singleton agent dictionary, agent name --> hosting container name
   * @param effectiveDateTime the date time when this assignment of singleton agents to hosts is effective
   * @param terminationDateTime the date time when this assignment of singleton agents to hosts is terminated
   * @param authorQualifiedName the author agent/role
   * @param createdDateTime the date time when this assignment was created
   * @param privateKey the private key of the signer's X.509 certificate
   *
   * @return the signature bytes
   */
  public static byte[] signSingletonAgentHosts(
          final Map<String, String> singletonAgentDictionary,
          final DateTime effectiveDateTime,
          final DateTime terminationDateTime,
          final String authorQualifiedName,
          final DateTime createdDateTime,
          final PrivateKey privateKey) {
    //Preconditions
    if (singletonAgentDictionary == null) {
      throw new TexaiException("singletonAgentDictionary must not be null");
    }
    if (singletonAgentDictionary.isEmpty()) {
      throw new TexaiException("singletonAgentDictionary must not be empty");
    }
    if (effectiveDateTime == null) {
      throw new TexaiException("effectiveDateTime must not be null");
    }
    if (terminationDateTime == null) {
      throw new TexaiException("terminationDateTime must not be null");
    }
    if (!StringUtils.isNonEmptyString(authorQualifiedName)) {
      throw new TexaiException("authorQualifiedName must be a non-empty string");
    }
    if (createdDateTime == null) {
      throw new TexaiException("createdDateTime must not be null");
    }
    if (!createdDateTime.isBefore(effectiveDateTime)) {
      throw new TexaiException("createdDateTime must be before effectiveDateTime");
    }
    if (!effectiveDateTime.isBefore(terminationDateTime)) {
      throw new TexaiException("effectiveDateTime must be before terminationDateTime");
    }

    final ArrayList<Object> serializableObject = new ArrayList<>();
    serializableObject.add(getOrderedEntries(singletonAgentDictionary));
    serializableObject.add(effectiveDateTime);
    serializableObject.add(terminationDateTime);
    serializableObject.add(authorQualifiedName);
    serializableObject.add(createdDateTime);
    try {
      return SerializableObjectSigner.sign(
              serializableObject,
              privateKey);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Verifies whether the given agent/role's X.509 certificate verifies the given SingletonAgentHosts object.
   *
   * @param singletonAgentHosts the given SingletonAgentHosts
   * @param x509Certificate the given agent/role's X.509 certificate
   *
   * @return whether the signature is valid
   */
  public static boolean verify(
          final SingletonAgentHosts singletonAgentHosts,
          final X509Certificate x509Certificate) {
    //Preconditions
    assert singletonAgentHosts != null : "singletonAgentHosts must not be null";
    assert x509Certificate != null : "x509Certificate must not be null";

    final ArrayList<Object> serializableObject = new ArrayList<>();
    serializableObject.add(getOrderedEntries(singletonAgentHosts.singletonAgentDictionary));
    serializableObject.add(singletonAgentHosts.effectiveDateTime);
    serializableObject.add(singletonAgentHosts.terminationDateTime);
    serializableObject.add(singletonAgentHosts.authorQualifiedName);
    serializableObject.add(singletonAgentHosts.createdDateTime);

    try {
      return SerializableObjectSigner.verify(
              serializableObject,
              x509Certificate,
              singletonAgentHosts.authorSignatureBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns a SHA-512 hash of this object.
   *
   * @return a SHA-512 hash
   */
  public byte[] sha512Hash() {
    final ArrayList<Object> serializableObject = new ArrayList<>();
    serializableObject.add(getOrderedEntries(singletonAgentDictionary));
    serializableObject.add(effectiveDateTime);
    serializableObject.add(terminationDateTime);
    serializableObject.add(authorQualifiedName);
    serializableObject.add(createdDateTime);
    return SerializableObjectSigner.sha512Hash(serializableObject);
  }

  /**
   * Returns the singleton agent dictionary entries sorted by agent name.
   *
   * @param singletonAgentDictionary the singleton agent dictionary, agent name --> hosting container name
   *
   * @return the singleton agent dictionary entries sorted by agent name
   */
  private static ArrayList<ArrayList<String>> getOrderedEntries(final Map<String, String> singletonAgentDictionary) {
    //Preconditions
    assert singletonAgentDictionary != null : "singletonAgentDictionary must not be null";
    assert !singletonAgentDictionary.isEmpty() : "singletonAgentDictionary must not be empty";

    final ArrayList<ArrayList<String>> orderedEntries = new ArrayList<>();
    singletonAgentDictionary.entrySet().stream().sorted(Entry.comparingByKey()).forEach((Entry<String, String> entry) -> {
      final ArrayList<String> keyAndValue = new ArrayList<>(2);
      keyAndValue.add(entry.getKey());
      keyAndValue.add(entry.getValue());
      orderedEntries.add(keyAndValue);
    });
    return orderedEntries;
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
   * Gets a copy of the singleton agent dictionary, agent name --> hosting container name.
   *
   * @return the singleton agent dictionary
   */
  public Map<String, String> getSingletonAgentDictionary() {
    return new HashMap<>(singletonAgentDictionary);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[SingletonAgentHosts, size ")
            .append(singletonAgentDictionary.size())
            .append(']')
            .toString();
  }

  /**
   * Returns a detailed string representation of this object.
   *
   * @return a detailed string representation of this object
   */
  public String toDetailedString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("[SingletonAgentHosts, size ")
            .append(singletonAgentDictionary.size())
            .append('\n');
    singletonAgentDictionary.entrySet().stream().forEach((Entry<String, String> entry) -> {
      stringBuilder.append("  ");
      stringBuilder.append(entry);
      stringBuilder.append('\n');
    });
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 97 * hash + Objects.hashCode(this.singletonAgentDictionary);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SingletonAgentHosts other = (SingletonAgentHosts) obj;
    return Arrays.equals(this.authorSignatureBytes, other.authorSignatureBytes);
  }

  /**
   * Gets the date time when this assignment of singleton agents to hosts is effective.
   *
   * @return the effectiveDateTime
   */
  public DateTime getEffectiveDateTime() {
    return effectiveDateTime;
  }

  /**
   * Gets the date time when this assignment of singleton agents to hosts is terminated.
   *
   * @return the terminationDateTime
   */
  public DateTime getTerminationDateTime() {
    return terminationDateTime;
  }

  /**
   * Gets the author agent/role.
   *
   * @return the authorQualifiedName
   */
  public String getAuthorQualifiedName() {
    return authorQualifiedName;
  }

  /**
   * Gets the date time when this assignment was created.
   *
   * @return the createdDateTime
   */
  public DateTime getCreatedDateTime() {
    return createdDateTime;
  }

  /**
   * Gets the signature of the author agent/role.
   *
   * @return the authorSignatureBytes
   */
  public byte[] getAuthorSignatureBytes() {
    return authorSignatureBytes;
  }
}
