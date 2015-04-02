/*
 * SingletonAgentHosts.java
 *
 * Created on Aug 23, 2010, 3:04:17 PM
 *
 * Description: Provides a SPARQL query container that is used to invoke a certain skill at a certain service.
 *
 * Copyright (C) Aug 23, 2010, Stephen L. Reed.
 */
package org.texai.ahcsSupport.domainEntity;

import java.io.Serializable;
import java.util.ArrayList;
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

/**
 * Contains singleton-agent host information.
 *
 * @author reed
 */
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
@Immutable
@ThreadSafe
public final class SingletonAgentHosts implements RDFPersistent, Serializable {

  // the serial version ID
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  final private URI id = null;
  // the singleton agent dictionary, agent name  --> hosting container name
  @RDFProperty(mapKeyType = "java.lang.String", mapValueType = "java.lang.String")
  private final Map<String, String> singletonAgentDictionary;
  // the date time when this assignment of singleton agents to hosts is effective
  @RDFProperty
  private final DateTime effectiveDateTime;
  // the date time when this assignment of singleton agents to hosts is terminated
  @RDFProperty
  private final DateTime terminationDateTime;

  /**
   * Constructs a new SingletonAgentHosts instance for use by the persistence framework.
   */
  public SingletonAgentHosts() {
    singletonAgentDictionary = null;
    effectiveDateTime = null;
    terminationDateTime = null;
  }

  /**
   * Constructs a new SingletonAgentHosts instance.
   *
   * @param singletonAgentDictionary the singleton agent dictionary, agent name --> hosting container name
   * @param effectiveDateTime the date time when this assignment of singleton agents to hosts is effective
   * @param terminationDateTime the date time when this assignment of singleton agents to hosts is terminated
   */
  public SingletonAgentHosts(
          final Map<String, String> singletonAgentDictionary,
          final DateTime effectiveDateTime,
          final DateTime terminationDateTime) {
    //Preconditions
    assert singletonAgentDictionary != null : "singletonAgentDictionary must not be null";
    assert !singletonAgentDictionary.isEmpty() : "singletonAgentDictionary must not be empty";
    assert effectiveDateTime != null : "effectiveDateTime must not be null";
    assert terminationDateTime != null : "terminationDateTime must not be null";
    assert effectiveDateTime.isBefore(terminationDateTime) : "effectiveDateTime must be before terminationDateTime";

    this.singletonAgentDictionary = new HashMap<>(singletonAgentDictionary);
    this.effectiveDateTime = effectiveDateTime;
    this.terminationDateTime = terminationDateTime;
  }

  /**
   * Returns the singleton agent dictionary entries sorted by agent name.
   *
   * @param singletonAgentDictionary the singleton agent dictionary, agent name --> hosting container name
   *
   * @return the singleton agent dictionary entries sorted by agent name
   */
  public static ArrayList<ArrayList<String>> getOrderedEntries(final Map<String, String> singletonAgentDictionary) {
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
   * Returns the container hosting the given agent.
   *
   * @param agentName the given agent name
   *
   * @return the container hosting the given agent
   */
  public String getContainer(final String agentName) {
    return singletonAgentDictionary.get(agentName);
  }

  /**
   * Returns whether the agent referred to by the given qualified name, container-name.agent-name.role-name, is a network singleton.
   *
   * @param qualifiedName the given qualified name, container-name.agent-name.role-name
   *
   * @return whether the agent referred to by the given qualified name is a network singleton
   */
  public boolean isNetworkSingleton(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    final String agentName = Node.extractAgentName(qualifiedName);
    return singletonAgentDictionary.containsKey(agentName);
  }

  /**
   * Returns the network singleton qualified name that has the same agent and role names as the given qualified role name,
   * container-name.agent-name.role-name. Suppose that Alice is joining the network, and the NetworkOperations agent singleton is
   * Mint.NetworkOperations.NetworkOperationsRole. Alice's local agent is Alice.NetworkOperations.NetworkOperationsRole. Any of Alice's
   * roles having this as a parent role need to be changed to Mint.NetworkOperations.NetworkOperationsRole.
   *
   * @param qualifiedName the given qualified role name
   *
   * @return the corresponding network singleton qualified name
   */
  public String mapNetworkSingleton(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    final String agentName = Node.extractAgentName(qualifiedName);
    final String singletonContainerName = singletonAgentDictionary.get(agentName);
    if (singletonContainerName == null) {
      return null;
    } else {
      return singletonContainerName + '.' + Node.extractAgentRoleName(qualifiedName);
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
    hash = 97 * hash + Objects.hashCode(singletonAgentDictionary);
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
    return this.effectiveDateTime.isEqual(other.effectiveDateTime);
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

}
