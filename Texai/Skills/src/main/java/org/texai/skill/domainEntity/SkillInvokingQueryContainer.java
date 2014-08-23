/*
 * SkillInvokingQueryContainer.java
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

import java.util.Set;
import net.jcip.annotations.Immutable;
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
public class SkillInvokingQueryContainer extends QueryContainer {

  /** the serial version ID */
  private static final long serialVersionUID = 1L;
  /** the service name, e.g. the fully qualified class name */
  @RDFProperty
  private final String serviceName;
  /** the operation */
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
    if ((this.operation == null) ? (other.operation != null) : !this.operation.equals(other.operation)) {
      return false;
    }
    return true;
  }

}
