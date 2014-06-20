/*
 * Message.java
 *
 * Created on Jun 26, 2009, 11:40:48 AM
 *
 * Description: Provides a message adapted from the FIPA standard.
 *
 * Copyright (C) Jun 26, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
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
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SerializableObjectSigner;

/** Provides a message.  See the FIPA standard at
 * http://www.fipa.org/specs/fipa00061/SC00061G.html
 *
 * @author Stephen L. Reed
 */
@Immutable
public class Message implements Serializable {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the sender role's id */
  private final URI senderRoleId;
  /** the sender's service, which is typically a skill interface name */
  private final String senderService;
  /** the sender's digital signature, which is populated when the message is sent between JVMs */
  private byte[] signatureBytes;
  /** the recipient role's id */
  private final URI recipientRoleId;
  /** the conversation id */
  private final UUID conversationId;
  /** the reply-with UUID */
  private final UUID replyWith;
  /** the in-reply-to UUID */
  private final UUID inReplyTo;
  /** the date/time */
  private final DateTime dateTime = new DateTime();
  /** the reply-by date/time, or null if not applicable */
  private final DateTime replyByDateTime;
  /** the recipient service , which is typically a skill interface name */
  private final String service;
  /** the operation */
  private final String operation;
  /** the parameter name/value dictionary, name --> value */
  private final Map<String, Object> parameterDictionary = new HashMap<>();
  /** the message service/operation version */
  private final String version;
  /** the default version */
  public static final String DEFAULT_VERSION = "1.0.0";

  /** Constructs a new Message instance, tailored for sending to a sub-skill within the same role.
   *
   * @param sendingSkill the sending skill
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   */
  public Message(
          final AbstractSkill sendingSkill,
          final String service,
          final String operation) {
    this(
            sendingSkill.getRole().getId(), // senderRoleId
            sendingSkill.getClassName(), // senderService
            sendingSkill.getRole().getId(), // recipientRoleId
            service,
            operation,
            new HashMap<>(),
            DEFAULT_VERSION);
  }

  /** Constructs a new Message instance.
   *
   * @param senderRoleId the sender role's id
   * @param senderService the senders service
   * @param recipientRoleId the recipient role's id
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   */
  public Message(
          final URI senderRoleId,
          final String senderService,
          final URI recipientRoleId,
          final String service,
          final String operation) {
    this(
            senderRoleId,
            senderService,
            recipientRoleId,
            service,
            operation,
            new HashMap<>(),
            DEFAULT_VERSION);
  }

  /** Constructs a new Message instance.
   *
   * @param senderRoleId the sender role's id
   * @param senderService the senders service
   * @param recipientRoleId the recipient role's id
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   * @param parameterDictionary the operations parameter dictionary, name --> value
   * @param version the message service/operation version
   */
  public Message(
          final URI senderRoleId,
          final String senderService,
          final URI recipientRoleId,
          final String service,
          final String operation,
          final Map<String, Object> parameterDictionary,
          final String version) {
    //Preconditions
    assert senderRoleId != null : "senderRoleId must not be null";
    assert recipientRoleId != null : "recipientRoleId must not be null";
    assert operation != null : "operation must not be null";
    assert operation.endsWith("_Info")
            || operation.endsWith("_Sensation")
            || operation.endsWith("_Task") : "invalid performative name '" + operation + "' must end with _Info, _Sensation or _Task";
    assert parameterDictionary != null : "parameterDictionary must not be null";
    assert version != null : "version must not be null";
    assert !version.isEmpty() : "version must not be empty";

    this.senderRoleId = senderRoleId;
    this.senderService = senderService;
    this.recipientRoleId = recipientRoleId;
    this.conversationId = null;
    this.replyWith = null;
    this.inReplyTo = null;
    this.replyByDateTime = null;
    this.service = service;
    this.operation = operation;
    for (final Entry<String, Object> parameter : parameterDictionary.entrySet()) {
      final String name = parameter.getKey();
      final Object value = parameter.getValue();
      assert value instanceof Serializable : "parameter value must be marked serializable: " + value;
      this.parameterDictionary.put(name, value);
    }
    this.version = version;
  }

  /** Constructs a new Message instance.
   *
   * @param senderRoleId the sender role's id
   * @param senderService the sender service
   * @param recipientRoleId the recipient role's id
   * @param conversationId the conversation id
   * @param replyWith the reply-with UUID
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   */
  public Message(
          final URI senderRoleId,
          final String senderService,
          final URI recipientRoleId,
          final UUID conversationId,
          final UUID replyWith,
          final String service,
          final String operation) {
    this(
            senderRoleId,
            senderService,
            recipientRoleId,
            conversationId,
            replyWith,
            null, // inReplyTo
            null, // replyByDateTime
            service,
            operation,
            new HashMap<>(), // parameterDictionary,
            Message.DEFAULT_VERSION);
  }

  /** Constructs a new Message instance.
   *
   * @param senderRoleId the sender role's id
   * @param senderService the sender service
   * @param recipientRoleId the recipient role's id
   * @param conversationId the conversation id
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   * @param inReplyTo the in-reply-to UUID
   */
  public Message(
          final URI senderRoleId,
          final String senderService,
          final URI recipientRoleId,
          final UUID conversationId,
          final String service,
          final String operation,
          final UUID inReplyTo) {
    this(
            senderRoleId,
            senderService,
            recipientRoleId,
            conversationId,
            null, // replyWith
            inReplyTo,
            null, // replyByDateTime
            service,
            operation,
            new HashMap<>(), // parameterDictionary,
            Message.DEFAULT_VERSION);
  }

  /** Constructs a new Message instance.
   *
   * @param senderRoleId the sender role's id
   * @param senderService the sender service
   * @param recipientRoleId the recipient role's id
   * @param conversationId the conversation id
   * @param replyWith the reply-with UUID
   * @param inReplyTo in-reply-to UUID
   * @param replyByDateTime the reply-by date/time, or null if not applicable
   * @param service the service
   * @param operation the operation, which can be a task, sensation, or information
   * @param parameterDictionary the operations parameter dictionary, name --> value
   * @param version the message service/operation version
   */
  public Message(
          final URI senderRoleId,
          final String senderService,
          final URI recipientRoleId,
          final UUID conversationId,
          final UUID replyWith,
          final UUID inReplyTo,
          final DateTime replyByDateTime,
          final String service,
          final String operation,
          final Map<String, Object> parameterDictionary,
          final String version) {
    //Preconditions
    assert senderRoleId != null : "senderRoleId must not be null";
    assert recipientRoleId != null : "recipientRoleId must not be null";
    assert operation != null : "operation must not be null";
    assert operation.endsWith("_Info")
            || operation.endsWith("_Sensation")
            || operation.endsWith("_Task") : "invalid performative name " + operation;
    assert parameterDictionary != null : "parameterDictionary must not be null";

    this.senderRoleId = senderRoleId;
    this.senderService = senderService;
    this.recipientRoleId = recipientRoleId;
    this.conversationId = conversationId;
    this.replyWith = replyWith;
    this.inReplyTo = inReplyTo;
    this.replyByDateTime = replyByDateTime;
    this.service = service;
    this.operation = operation;
    for (final Entry<String, Object> parameter : parameterDictionary.entrySet()) {
      final String name = parameter.getKey();
      final Object value = parameter.getValue();
      assert value instanceof Serializable : "parameter value must be marked serializable: " + value;
      this.parameterDictionary.put(name, value);
    }
    this.version = version;
  }

  /** Returns a new message for forwarding to the given recipient.
   *
   * @param message the given message
   * @param recipientRoleId the given recipient
   * @param service the service
   * @return a new message for forwarding to the given recipient
   */
  public static Message forward(
          final Message message,
          final URI recipientRoleId,
          final String service) {
    //Preconditions
    assert message != null : "message must not be null";
    assert recipientRoleId != null : "recipientRoleId must not be null";

    return new Message(
            message.recipientRoleId, // senderRoleId
            message.service, // senderService,
            recipientRoleId,
            message.conversationId,
            message.replyWith,
            message.inReplyTo,
            message.replyByDateTime,
            service,
            message.operation,
            message.parameterDictionary,
            message.version);
  }

  /** Returns a new message for replying to the given recipient.
   *
   * @param message the given message
   * @param skill the skill that is sending the reply message
   * @return a new message for forwarding to the given recipient
   */
  public static Message reply(
          final Message message,
          final AbstractSkill skill) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.isTask() : "message must be a task";
    assert skill != null : "skill must not be null";

    return new Message(
            skill.getRoleId(), // senderRoleId
            skill.getClassName(), // senderService,
            message.getSenderRoleId(), // recipientRoleId
            message.getSenderService(), // service
            message.getOperation().replace("_Task", "_Info")); // operation
  }

  /** Returns a clone of this object.
   *
   * @return a clone of this object
   */
  @Override
  @SuppressWarnings({"CloneDeclaresCloneNotSupported", "CloneDoesntCallSuperClone"})
  public Message clone() {
    return new Message(
            recipientRoleId, // senderRoleId
            service, // senderService,
            recipientRoleId,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            operation,
            new HashMap<>(parameterDictionary),
            version);
  }

  /** Returns whether the given message is an information message.
   *
   * @return whether the given message is an information message
   */
  public boolean isInfo() {
    return getOperation().endsWith("_Info");
  }

  /** Returns whether the given message is a sensation message sent from a child node to its parent node.
   *
   * @return whether the given message is a task message
   */
  public boolean isSensation() {
    return operation.endsWith("_Sensation");
  }

  /** Returns whether the given message is a task message sent from a parent node to one of its child nodes.
   *
   * @return whether the given message is a task message
   */
  public boolean isTask() {
    return getOperation().endsWith("_Task");
  }

  /** Gets the sender service.
   *
   * @return the sender service
   */
  public String getSenderService() {
    return senderService;
  }

  /** Gets the sender role's id.
   *
   * @return the sender role's id
   */
  public URI getSenderRoleId() {
    return senderRoleId;
  }

  /** Gets the senders digital signature, which is populated when the message is sent between JVMs.
   *
   * @return the senders digital signature
   */
  public byte[] getSignatureBytes() {
    return Arrays.copyOf(signatureBytes, signatureBytes.length);
  }

  /** Gets the recipient role's id.
   *
   * @return the recipient role's id
   */
  public URI getRecipientRoleId() {
    return recipientRoleId;
  }

  /** Gets the conversation id.
   *
   * @return the conversation id
   */
  public UUID getConversationId() {
    return conversationId;
  }

  /** Gets the reply-with UUID.
   *
   * @return the reply-with UUID
   */
  public UUID getReplyWith() {
    return replyWith;
  }

  /** Gets the in-reply-to UUID.
   *
   * @return the in-reply-to UUID
   */
  public UUID getInReplyTo() {
    return inReplyTo;
  }

  /** Gets the reply-by date/time.
   *
   * @return the reply-by date/time
   */
  public DateTime getReplyByDateTime() {
    return replyByDateTime;
  }

  /** Gets the creation date/time.
   *
   * @return the creation date/time
   */
  public DateTime getDate() {
    return dateTime;
  }

  /** Gets the service.
   *
   * @return the service
   */
  public String getService() {
    return service;
  }

  /** Gets the operation.
   *
   * @return the operation
   */
  public String getOperation() {
    return operation;
  }

  /** Gets the parameter name/value dictionary, name --> value.
   *
   * @return the parameter name/value dictionary
   */
  public Map<String, Object> getParameterDictionary() {
    return Collections.unmodifiableMap(parameterDictionary);
  }

  /** Gets the parameter value corresponding to the given parameter name, or null if not found.
   *
   * @param parameterName the parameter name
   * @return the parameter value, or null if not found
   */
  public Object get(final String parameterName) {
    //Preconditions
    assert parameterName != null : "parameterName must not be null";
    assert !parameterName.isEmpty() : "parameterName must not be empty";

    return parameterDictionary.get(parameterName);
  }

  /** Puts the parameter name and non-null value into the parameter dictionary.
   *
   * @param parameterName the parameter name
   * @param parameterValue the parameter value
   */
  public void put(final String parameterName, final Object parameterValue) {
    //Preconditions
    assert parameterName != null : "parameterName must not be null";
    assert !parameterName.isEmpty() : "parameterName must not be empty";
    assert parameterValue != null : "parameterValue must not be null";

    parameterDictionary.put(parameterName, parameterValue);
  }

  /** Copies the given message's parameters into this message's parameter dictionary.
   *
   * @param message the given message
   */
  public void copyParametersFrom(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    parameterDictionary.putAll(message.getParameterDictionary());
  }

  /** Copies the given message's parameter into this message's parameter dictionary.
   *
   * @param parameterName the parameter name
   * @param message the given message
   */
  public void copyParameterFrom(final String parameterName, final Message message) {
    //Preconditions
    assert StringUtils.isNonEmptyString(parameterName) : "parameterName must be a non-empty string";
    assert message != null : "message must not be null";

    final Object parameterValue = message.get(parameterName);
    if (parameterValue != null) {
      parameterDictionary.put(parameterName, parameterValue);
    }
  }

  /** Gets the message service/operation version.
   *
   * @return the message service/operation version
   */
  public String getVersion() {
    return version;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    int index = senderService.lastIndexOf('.');
    if (index > -1) {
      final String senderServiceName = senderService.substring(index + 1);
      stringBuilder.append(senderServiceName);
    } else {
      stringBuilder.append(senderService);
    }
    stringBuilder.append(" (");
    stringBuilder.append(operation);
    stringBuilder.append(") --> ");
    if (service != null) {
      index = service.lastIndexOf('.');
      if (index > -1) {
        final String serviceName = service.substring(index + 1);
        stringBuilder.append(serviceName);
      } else {
        stringBuilder.append(service);
      }
    }
    stringBuilder.append('{');
    boolean isFirst = true;
    for (final Entry<String, Object> entry : parameterDictionary.entrySet()) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(entry.getKey());
      stringBuilder.append('=');
      stringBuilder.append(entry.getValue().toString());
    }
    stringBuilder.append('}');
    stringBuilder.append(" sender: ");
    stringBuilder.append(senderRoleId);
    stringBuilder.append(" recipient: ");
    stringBuilder.append(recipientRoleId);
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Returns a string representation of this object, in which sender and recipient nicknames are substituted for their
   * respective role ids.
   *
   * @param nodeRuntime the node runtime
   * @return a string representation of this object
   */
  public String toString(final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    final String senderNodeNickname;
    final Role localSenderRole = nodeRuntime.getLocalRole(senderRoleId);
    if (localSenderRole == null) {
      senderNodeNickname = null;
    } else {
      senderNodeNickname = localSenderRole.getNode().getNodeNickname();
    }

    final String recipientNodeNickname;
    final Role localRecipientRole = nodeRuntime.getLocalRole(recipientRoleId);
    if (localRecipientRole == null) {
      recipientNodeNickname = null;
    } else {
      recipientNodeNickname = localRecipientRole.getNode().getNodeNickname();
    }
    return toString(
            senderNodeNickname,
            recipientNodeNickname,
            nodeRuntime.getNodeAccess());
  }

  /** Returns a string representation of this object.
   *
   *   [Governor:GovernanceManagementRoleType:GovernanceManagement --> Linguist:GovernanceRoleType:Governance (AHCS ready_Task) {}]
   *
   * @param senderNodeNickname the sender node nickname
   * @param recipientNodeNickname the recipient node nickname
   * @param nodeAccess the node access object
   * @return a string representation of this object
   */
  private String toString(
          final String senderNodeNickname,
          final String recipientNodeNickname,
          final NodeAccess nodeAccess) {

    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[");
    if (senderNodeNickname != null) {
      stringBuilder.append(senderNodeNickname).append(':');
    }

    final String senderRoleTypeName;
    if (senderRoleId.toString().startsWith("http://texai.org/texai/org.texai.texailauncher.domainEntity.TexaiLauncherInfo")) {
      // the Sesame Launcher repository is locked by the Texai Launcher, so do not use nodeAccess
      senderRoleTypeName = "Launcher";
    } else if (senderRoleId.getLocalName().startsWith("NodeRuntime_")) {
      senderRoleTypeName = "NodeRuntime";
    } else {
      senderRoleTypeName = nodeAccess.getRoleType(senderRoleId).getTypeName();
    }
    stringBuilder.append(senderRoleTypeName);
    stringBuilder.append(':');
    if (senderService != null) {
      int index = senderService.lastIndexOf('.');
      if (index > -1) {
        stringBuilder.append(senderService.substring(index + 1));
      } else {
        stringBuilder.append(senderService);
      }
    }
    stringBuilder.append(" --> ");
    if (recipientNodeNickname != null) {
      stringBuilder.append(recipientNodeNickname).append(':');
    }
    final String recipientRoleTypeName;
    if (recipientRoleId.toString().startsWith("http://texai.org/texai/org.texai.texailauncher.domainEntity.TexaiLauncherInfo")) {
      // the Sesame Launcher repository is locked by the Texai Launcher, so do not use nodeAccess
      recipientRoleTypeName = "Launcher";
    } else if (recipientRoleId.getLocalName().startsWith("NodeRuntime_")) {
      recipientRoleTypeName = "NodeRuntime";
    } else {
      recipientRoleTypeName = nodeAccess.getRoleType(recipientRoleId).getTypeName();
    }
    stringBuilder.append(recipientRoleTypeName);
    stringBuilder.append(':');
    if (service != null) {
      int index = service.lastIndexOf('.');
      if (index > -1) {
        stringBuilder.append(service.substring(index + 1));
      } else {
        stringBuilder.append(service);
      }
    }
    stringBuilder.append(" (");
    stringBuilder.append(operation);
    stringBuilder.append(") {");
    if (conversationId != null) {
        stringBuilder.append("\n  conversationId=");
        stringBuilder.append(conversationId);
    }
    if (replyWith != null) {
        stringBuilder.append("\n  replyWith=");
        stringBuilder.append(replyWith);
    }
    if (!parameterDictionary.isEmpty()) {
      boolean isFirst = true;
      for (final Entry<String, Object> entry : parameterDictionary.entrySet()) {
        if (isFirst) {
          isFirst = false;
        } else {
          stringBuilder.append(',');
        }
        stringBuilder.append("\n  ");
        stringBuilder.append(entry.getKey());
        stringBuilder.append('=');
        stringBuilder.append(entry.getValue().toString());
      }
      stringBuilder.append('\n');
    }
    stringBuilder.append('}');
    if (senderNodeNickname == null && recipientNodeNickname == null) {
      stringBuilder.append(" sender: ");
      stringBuilder.append(senderRoleId);
      stringBuilder.append(" recipient: ");
      stringBuilder.append(recipientRoleId);
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
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
    final Message other = (Message) obj;
    if (this.senderRoleId != other.senderRoleId && !this.senderRoleId.equals(other.senderRoleId)) {
      return false;
    }
    if (this.senderService != null && !this.senderService.equals(other.senderService)) {
      return false;
    }
    if (this.recipientRoleId != other.recipientRoleId && !this.recipientRoleId.equals(other.recipientRoleId)) {
      return false;
    }
    if (this.conversationId != other.conversationId && !this.conversationId.equals(other.conversationId)) {
      return false;
    }
    if (this.replyWith != other.replyWith && !this.replyWith.equals(other.replyWith)) {
      return false;
    }
    if (this.inReplyTo != other.inReplyTo && !this.inReplyTo.equals(other.inReplyTo)) {
      return false;
    }
    if (this.dateTime != other.dateTime && !this.dateTime.equals(other.dateTime)) {
      return false;
    }
    if (this.replyByDateTime != other.replyByDateTime && !this.replyByDateTime.equals(other.replyByDateTime)) {
      return false;
    }
    if (!this.service.equals(other.service)) {
      return false;
    }
    if (!this.operation.equals(other.operation)) {
      return false;
    }
    if (this.parameterDictionary != other.parameterDictionary && !this.parameterDictionary.equals(other.parameterDictionary)) {
      return false;
    }
    return this.version.equals(other.version);
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return senderRoleId.hashCode() + recipientRoleId.hashCode();
  }

  /** Returns whether this message has been signed.
   *
   * @return whether this message has been signed
   */
  public boolean isSigned() {
    return signatureBytes == null;
  }

  /** Signs this message.
   *
   * @param privateKey the senders private key
   */
  public void sign(final PrivateKey privateKey) {
    //Preconditions
    assert privateKey != null : "privateKey must not be null";
    assert signatureBytes == null : "message must not be already signed";

    try {
      signatureBytes = SerializableObjectSigner.sign(this, privateKey);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns whether the given signature verifies this message.
   *
   * @param x509Certificate the senders X.509 certificate, that contains the public key
   * @return whether the given signature verifies the given file
   */
  public boolean verify(final X509Certificate x509Certificate) {
    //Preconditions
    assert x509Certificate != null : "x509Certificate must not be null";
    assert signatureBytes != null : "message must signed: " + this;
    assert signatureBytes.length > 0 : "signatureBytes must not be empty";

    final byte[] savedSignatureBytes = signatureBytes;
    signatureBytes = null;
    final boolean result;
    try {
      result = SerializableObjectSigner.verify(this, x509Certificate, savedSignatureBytes);
    } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
      throw new TexaiException(ex);
    }
    signatureBytes = savedSignatureBytes;
    return result;
  }

  /** Return whether this message is a Chord operation message, which is not signed.
   *
   * @return whether this message is a Chord operation message
   */
  public boolean isChordOperation() {
    return senderRoleId.getLocalName().isEmpty();
  }
}
