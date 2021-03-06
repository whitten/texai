/*
 * SkillInvokingQueryContainer.java
 *
 * Created on Aug 23, 2010, 3:04:17 PM
 *
 * Description: Provides a SPARQL query container that is used to invoke a certain skill at a certain service.
 *
 * Copyright (C) Aug 23, 2010, Stephen L. Reed.
 */
package org.texai.skill.domainEntity;

import java.util.Objects;
import java.util.Set;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.texai.inference.sparql.domainEntity.AbstractQuery;
import org.texai.inference.sparql.domainEntity.BaseDeclaration;
import org.texai.inference.sparql.domainEntity.PrefixDeclaration;
import org.texai.inference.sparql.domainEntity.QueryContainer;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;

/** Provides a SPARQL query container that is used to invoke a certain skill at a certain service.
 *
 * @author reed
 */
@RDFEntity(context = "texai:BootstrapSkills")
@Immutable
@ThreadSafe
public final class SkillInvokingQueryContainer extends QueryContainer {

  // the serial version ID
  private static final long serialVersionUID = 1L;
  // the service name, e.g. the fully qualified class name
  @RDFProperty
  private final String serviceName;
  // the operation
  @RDFProperty
  private final String operation;

  /** Constructs a new SkillInvokingQueryContainer instance. */
  public SkillInvokingQueryContainer() {
    serviceName = null;
    operation = null;
  }

  /** Constructs a new SkillInvokingQueryContainer instance.
   *
   * @param queryContainer the query container
   * @param serviceName the service name, e.g. the fully qualified class name
   * @param operation the operation
   */
  public SkillInvokingQueryContainer(
          final QueryContainer queryContainer,
          final String serviceName,
          final String operation) {
    super(
            queryContainer.getName(),
            queryContainer.getBaseDeclaration(),
            queryContainer.getPrefixDeclarations(),
            queryContainer.getQuery());
    //Preconditions
    assert serviceName != null : "serviceName must not be null";
    assert !serviceName.isEmpty() : "serviceName must not be empty";
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";
    assert operation.endsWith("_Task") || operation.endsWith("_Sensation") || operation.endsWith("_Info") :
      "invalid operation suffix: " + operation;

    this.serviceName = serviceName;
    this.operation = operation;
  }

  /** Constructs a new SkillInvokingQueryContainer instance.
   *
   * @param name the query name
   * @param baseDeclaration the base declaration
   * @param prefixDeclarations the prefix declarations
   * @param query the query
   * @param serviceName the service name, e.g. the fully qualified class name
   * @param operation the operation
   */
  public SkillInvokingQueryContainer(
          final String name,
          final BaseDeclaration baseDeclaration,
          final Set<PrefixDeclaration> prefixDeclarations,
          final AbstractQuery query,
          final String serviceName,
          final String operation) {
    super(name, baseDeclaration, prefixDeclarations, query);
    //Preconditions
    assert serviceName != null : "serviceName must not be null";
    assert !serviceName.isEmpty() : "serviceName must not be empty";
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";

    this.serviceName = serviceName;
    this.operation = operation;
  }


  /** Gets the service name, e.g. the fully qualified class name.
   *
   * @return the serviceName
   */
  public String getServiceName() {
    return serviceName;
  }

  /** Gets the operation.
   *
   * @return the operation
   */
  public String getOperation() {
    return operation;
  }

  /** Returns a detailed string representation of this object.
   *
   * @return a detailed string representation of this object
   */
  @Override
  public String toDetailedString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("name: ");
    stringBuilder.append(getName());
    stringBuilder.append("\n");
    stringBuilder.append("service: ");
    stringBuilder.append(serviceName);
    stringBuilder.append("\n");
    stringBuilder.append("operation: ");
    stringBuilder.append(operation);
    stringBuilder.append("\n");
    stringBuilder.append(toString());
    return stringBuilder.toString();
  }

  /** Returns a hash code for this object.
   *
   * @return  a hash code
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 89 * hash + Objects.hashCode(this.serviceName);
    hash = 89 * hash + Objects.hashCode(this.operation);
    return hash;
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
    if (!super.equals(obj)) {
      return false;
    }
    final SkillInvokingQueryContainer other = (SkillInvokingQueryContainer) obj;
    if ((this.serviceName == null) ? (other.serviceName != null) : !this.serviceName.equals(other.serviceName)) {
      return false;
    }
    return !((this.operation == null) ? (other.operation != null) : !this.operation.equals(other.operation));
  }

}
