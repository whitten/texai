/*
 * Role.java
 *
 * Created on Mar 11, 2010, 2:16:21 PM
 *
 * Description: Provides a role in an Albus Hierarchical Control System node.
 *
 * Copyright (C) Mar 11, 2010 reed.
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
package org.texai.ahcsSupport.domainEntity;

import java.util.Objects;
import org.texai.ahcsSupport.AbstractSkill;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSubSkill;
import org.texai.ahcsSupport.AlbusMessageDispatcher;
import org.texai.ahcsSupport.ManagedSessionSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.NodeRuntime;
import org.texai.ahcsSupport.RoleInfo;
import org.texai.ahcsSupport.SessionManagerSkill;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;
import org.texai.kb.util.UUIDUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

/** Provides a role in an Albus Hierarchical Control System node.
 *
 * @author reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class Role implements CascadePersistence, AlbusMessageDispatcher, Comparable<Role> {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Role.class);
  /** the id assigned by the persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the role type URI, note that the role type is persisted into a different repository */
  @RDFProperty()
  private final URI roleTypeURI;
  /** the role type name */
  @RDFProperty()
  private final String roleTypeName;
  /** the containing node */
  @RDFProperty(predicate = "texai:ahcsNode_role", inverse = true)
  private Node node;
  /** the parent role id string */
  @RDFProperty()
  private String parentRoleIdString;
  /** the child role id strings */
  @RDFProperty()
  private final Set<String> childRoleIdStrings = new HashSet<>();
  /** the state values */
  @RDFProperty()
  private final Set<StateValueBinding> stateValueBindings = new HashSet<>();
  /** the role X509 certificate alias */
  @RDFProperty
  private String roleAlias;
  /** the node state variable dictionary, state variable name --> state value binding */
  private final Map<String, StateValueBinding> stateVariableDictionary = new HashMap<>();
  /** the node runtime */
  private transient NodeRuntime nodeRuntime;
  /** the X.509 security information for this role */
  private transient X509SecurityInfo x509SecurityInfo;
  /** the role's skill dictionary, service (skill class name) --> skill */
  private transient final Map<String, AbstractSkill> skillDictionary = new HashMap<>();
  /** the role state */
  private final AtomicReference<State> roleState = new AtomicReference<>(State.UNINITIALIZED);
  /** the subskills dictionary, subskill class name --> subskill shared instance */
  private final Map<String, AbstractSubSkill> subSkillsDictionary = new HashMap<>();

  /** Constructs a new Role instance. */
  public Role() {
    roleTypeURI = null;
    roleTypeName = null;
    nodeRuntime = null;
  }

  /** Constructs a new Role instance.
   *
   * @param roleType the role type
   * @param nodeRuntime the node runtime
   */
  public Role(
          RoleType roleType,
          final NodeRuntime nodeRuntime) {
    //Preconditions
    assert roleType != null : "roleType must not be null";
    assert roleType.getId() != null : "roleType id must not be null";
    assert roleType.getTypeName() != null : "role type name must not be null";
    assert !roleType.getTypeName().isEmpty() : "role type name must not be empty";
    // nodeRuntime and nodeRuntimeRoleId may be null only for unit testing

    this.roleTypeURI = roleType.getId();
    this.roleTypeName = roleType.getTypeName();
    this.nodeRuntime = nodeRuntime;
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Sets the id for the persistence framework.
   *
   * @param id the id for the persistence framework
   */
  public void setId(final URI id) {
    //Preconditions
    assert id != null : "id must not be null";

    this.id = id;
  }

  /** Gets the role type URI.
   *
   * @return the role type URI
   */
  public URI getRoleTypeURI() {
    return roleTypeURI;
  }

  /** Gets the containing node.
   *
   * @return the containing node
   */
  public synchronized Node getNode() {
    return node;
  }

  /** Sets the containing node.
   *
   * @param node the containing node
   */
  public synchronized void setNode(final Node node) {
    this.node = node;
  }

  /** Gets the role state.
   *
   * @return the role state
   */
  public synchronized State getRoleState() {
    return roleState.get();
  }

  /** Installs the skills for this role.
   *
   * @param nodeAccess the node access object
   */
  public void installSkills(final NodeAccess nodeAccess) {
    //Preconditions
    assert nodeAccess != null : "nodeAccess must not be null";

    final Set<SkillClass> skillClasses = nodeAccess.getAllSkillClasses(this);
    synchronized (skillDictionary) {
      for (final SkillClass skillClass : skillClasses) {

        final String skillClassName = skillClass.getSkillClassName();
        assert !skillDictionary.containsKey(skillClassName) : "skill must not be previously installed: " + skillClassName;
        final Class<?> clazz;
        try {
          clazz = Class.forName(skillClassName);
        } catch (ClassNotFoundException ex) {
          LOGGER.error("cannot find class for '" + skillClassName + "'");
          throw new TexaiException(ex);
        }
        final AbstractSkill skill;
        try {
          skill = (AbstractSkill) clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
          throw new TexaiException(ex);
        }

        if (clazz.isAnnotationPresent(ManagedSessionSkill.class)) {
          // wrap the skill in a session managing skill
          final SessionManagerSkill sessionManagerSkill = new SessionManagerSkill();
          sessionManagerSkill.setRole(this);
          sessionManagerSkill.setSkillClass(clazz);
          skillDictionary.put(skillClassName, sessionManagerSkill);
          LOGGER.info(getNode().getNodeNickname() + ": " + this + " constructed managed session skill: " + skill);

        } else {
          // ordinary skill that does not need sessions managed
          skill.setRole(this);
          skillDictionary.put(skillClassName, skill);
          LOGGER.info(getNode().getNodeNickname() + ": " + this + " constructed skill: " + skill);
        }
      }
    }
  }

  /** Gets an unmodifiable copy of the role's skills.
   *
   * @return the the role's skills
   */
  public Set<AbstractSkill> getSkills() {
    synchronized (skillDictionary.values()) {
      final Set<AbstractSkill> skills = new HashSet<>();
      skills.addAll(skillDictionary.values());
      return Collections.unmodifiableSet(skills);
    }
  }

  /** Finds the role's skill instance having the specified class name (service).
   *
   * @param subSkillClassName the specified class name (service)
   * @return the skill
   */
  public AbstractSkill getSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    synchronized (skillDictionary.values()) {
      return skillDictionary.get(subSkillClassName);
    }
  }

  /** Gets the parent role id string.
   *
   * @return the parent role id string
   */
  public String getParentRoleIdString() {
    return parentRoleIdString;
  }

  /** Sets the parent role id string.
   *
   * @param parentRoleIdString the parent role id string
   */
  public void setParentRoleIdString(final String parentRoleIdString) {
    //Preconditions
    assert parentRoleIdString != null : "parentRoleIdString must not be null";
    assert !parentRoleIdString.isEmpty() : "parentRoleIdString must not be empty";

    this.parentRoleIdString = parentRoleIdString;
  }

  /** Gets the parent role id.
   *
   * @return the parent role id
   */
  public URI getParentRoleId() {
    if (parentRoleIdString == null) {
      return null;
    } else {
      return new URIImpl(parentRoleIdString);
    }
  }

  /** Gets an unmodifiable copy of the child role id strings.
   *
   * @return the child role id strings
   */
  public Set<String> getChildRoleIdStrings() {
    synchronized (childRoleIdStrings) {
      return Collections.unmodifiableSet(childRoleIdStrings);
    }
  }

  /** Adds a child role.
   *
   * @param childRoleIdString the child role id string
   */
  public void addChildRole(final String childRoleIdString) {
    //Preconditions
    assert childRoleIdString != null : "childRoleIdString must not be null";
    assert !childRoleIdString.isEmpty() : "childRoleIdString must not be empty";

    synchronized (childRoleIdStrings) {
      childRoleIdStrings.add(childRoleIdString);
    }
    final String childRoleClassName = RDFUtility.getDefaultClassFromIdString(childRoleIdString);
    assert childRoleIdString != null;
  }

  /** Removes the given child role.
   *
   * @param childRoleIdString the given child role id string
   */
  public void removeChildRole(final String childRoleIdString) {
    //Preconditions
    assert childRoleIdString != null : "childRoleIdString must not be null";
    assert !childRoleIdString.isEmpty() : "childRoleIdString must not be empty";

    synchronized (childRoleIdStrings) {
      childRoleIdStrings.remove(childRoleIdString);
    }
  }

  /** Gets the node runtime.
   *
   * @return the node runtime
   */
  public NodeRuntime getNodeRuntime() {
    return nodeRuntime;
  }

  /** Sets the node runtime.
   *
   * @param nodeRuntime the node runtime
   */
  public void setNodeRuntime(final NodeRuntime nodeRuntime) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";

    this.nodeRuntime = nodeRuntime;
  }

  /** Gets the X.509 security information.
   *
   * @return the X.509 security information
   */
  public X509SecurityInfo getX509SecurityInfo() {
    return x509SecurityInfo;
  }

  /** Sets the X.509 security information.
   *
   * @param x509SecurityInfo the X.509 security information
   */
  public void setX509SecurityInfo(final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
    assert id != null : "id must not be null";
    assert X509Utils.getUUID(x509SecurityInfo.getX509Certificate()).equals(UUIDUtils.uriToUUID(id)) :
            "X.509 certificate subject's UID must match this role id";

    this.x509SecurityInfo = x509SecurityInfo;
  }

  /** Returns the X.509 certificate belonging to this role.
   *
   * @return the X.509 certificate belonging to this role
   */
  public X509Certificate getX509Certificate() {
    if (x509SecurityInfo == null) {
      return null;
    } else {
      return x509SecurityInfo.getX509Certificate();
    }
  }

  /** Gets the role state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   * @return the state value associated with the given variable name
   */
  public Object getRoleStateValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";

    synchronized (stateVariableDictionary) {
      final StateValueBinding stateValueBinding = stateVariableDictionary.get(stateVariableName);
      if (stateValueBinding == null) {
        return null;
      } else {
        return stateValueBinding.getValue();
      }
    }
  }

  /** Sets the role state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setRoleStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    synchronized (stateVariableDictionary) {
      StateValueBinding stateValueBinding = stateVariableDictionary.get(variableName);
      if (stateValueBinding == null) {
        stateValueBinding = new StateValueBinding(variableName, value);
        stateVariableDictionary.put(variableName, stateValueBinding);
        stateValueBindings.add(stateValueBinding);
      } else {
        stateValueBinding.setValue(value);
      }
    }
  }

  /** Removes the role state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeRoleStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    synchronized (stateVariableDictionary) {
      if (stateVariableDictionary.isEmpty() && !stateValueBindings.isEmpty()) {
        // lazy population of the state value dictionary from the persistent state value bindings
        for (final StateValueBinding stateValueBinding : stateValueBindings) {
          stateVariableDictionary.put(stateValueBinding.getVariableName(), stateValueBinding);
        }
      }
      final StateValueBinding stateValueBinding = stateVariableDictionary.remove(variableName);
      if (stateValueBinding != null) {
        final boolean isRemoved = stateValueBindings.remove(stateValueBinding);
        assert isRemoved;
      }
    }
  }

  /** Gets the node state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   * @return the state value associated with the given variable name
   */
  public Object getNodeStateValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";

    return node.getNodeStateValue(stateVariableName);
  }

  /** Sets the node state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setNodeStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    node.setNodeStateValue(variableName, value);
  }

  /** Removes the node state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeNodeStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    node.removeNodeStateValueBinding(variableName);
  }

  /** Gets the role type name.
   * @return the role type name
   */
  public String getRoleTypeName() {
    return roleTypeName;
  }

  /** Sends the given message to the addressed sub-skill and returns the response message.
   *
   * @param message the message for the addressed sub-skill
   * @return the response message
   */
  public Message converseMessageWithSubSkill(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    AbstractSkill skill;
    synchronized (skillDictionary) {
      skill = skillDictionary.get(message.getService());
    }
    if (skill == null) {
      LOGGER.info(getNode().getNodeNickname() + ": subskill not found for service: " + message.getService() + ", constructing it");
      // ordinary skill that does not need sessions managed
      try {
        skill = (AbstractSkill) Class.forName(message.getService()).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new TexaiException(ex);
      }
      skill.setRole(this);
      skillDictionary.put(message.getService(), skill);
      LOGGER.info(getNode().getNodeNickname() + ": " + this + " constructed skill: " + skill);
    }
    return skill.converseMessage(message);
  }

  /** Receives an inbound message for this role.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchAlbusMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return;
    }

    // the message service field optionally names a skill interface
    AbstractSkill skill = null;
    if (message.getService() != null) {
      synchronized (skillDictionary) {
        skill = skillDictionary.get(message.getService());
        if (skill == null) {
          // not a primary skill for this role, try the shared subskill dictionary
          skill = findSubSkill(message.getService());
        }
        assert skill != null : "service not found " + message.getService() + "\n" + message.toString(getNodeRuntime()) + "\n skillDictionary: " + skillDictionary;
      }
    }
    if (skill == null) {
      // dispatch the message to any skill that understands the operation
      boolean isSkillFound = false;
      for (final AbstractSkill skill1 : getSkills()) {
        if (skill1.isOperationUnderstood(message.getOperation())) {
          isSkillFound = true;
          LOGGER.info(this + ", skill " + skill1.toString() + " understands " + message.getOperation());
          skill1.receiveMessage(message);
        }
      }
      if (!isSkillFound) {
        LOGGER.info(getNode().getNodeNickname() + ": skill not found for service: " + message.toString(getNodeRuntime()));
        LOGGER.info(getNode().getNodeNickname() + ": skillDictionary:\n  " + skillDictionary);
        // not understood
        final Message message1 = new Message(
                id, // senderRoleId
                getClass().getName(), // senderService,
                message.getSenderRoleId(), // recipientRoleId
                message.getSenderService(), // service
                AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO); // operation
        message1.put(AHCSConstants.AHCS_ORIGINAL_MESSAGE, message);
        sendMessage(message1);
      }
    } else {
      skill.receiveMessage(message);
    }
  }

  /** Sends the given message via the node runtime.
   *
   * @param message the given message
   */
  public void sendMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    final URI recipientRoleId = message.getRecipientRoleId();
    if (!message.getSenderRoleId().equals(id)) {
      LOGGER.warn("cannot send message for which this role is not the sender role " + message);
      throw new TexaiException("cannot send message for which this role is not the sender " + message);
    }

    if (!getNodeRuntime().isLocalRole(recipientRoleId)) {
      // sign messages sent between non-local roles
      message.sign(x509SecurityInfo.getPrivateKey());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(getNode().getNodeNickname() + ": sending message: " + message.toString(nodeRuntime));
    }
    nodeRuntime.dispatchAlbusMessage(message);
  }

  /** Propagates the given operation to the child roles.
   *
   * @param operation the given operation
   * @param senderService the sender service
   * @param service the recipient service, which if null indicates that any service that understands the operation will receive the message
   */
  public void propagateOperationToChildRoles(
          final String operation,
          final String senderService,
          final String service) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";
    assert senderService != null : "senderService must not be null";
    assert !senderService.isEmpty() : "senderService must not be empty";
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        if (roleState.get().equals(State.UNINITIALIZED)) {
          LOGGER.info(getRoleTypeName() + " propagating initialize task to child roles");
        } else {
          // the child roles are already initialized
          return;
        }
        break;
      case AHCSConstants.AHCS_READY_TASK:
        if (roleState.get().equals(State.INITIALIZED)) {
          LOGGER.info(getRoleTypeName() + " propagating ready task to child roles");
        } else {
          // the child roles are already ready
          return;
        }
        break;
    }

    for (final String childRoleIdString : getChildRoleIdStrings()) {
      assert !childRoleIdString.equals(id.toString()) : "role " + this + " has itself as a child role";
      sendMessage(new Message(
              id, // senderRoleId
              senderService,
              new URIImpl(childRoleIdString), // recipientRoleId
              null, // service
              operation)); // operation
    }

    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        roleState.set(State.INITIALIZED);
        break;

      case AHCSConstants.AHCS_READY_TASK:
        roleState.set(State.READY);
        break;
    }
  }

  /** When implemented by a message router, registers the given SSL proxy.
   *
   * @param sslProxy the given SSL proxy
   */
  @Override
  public void registerSSLProxy(Object sslProxy) {
    throw new UnsupportedOperationException("Not implemented.");
  }

  /** Enables this role for messaging with remote roles via the local message router. */
  public void enableRemoteComunications() {
    final RoleInfo roleInfo = new RoleInfo(
            id, // roleId
            x509SecurityInfo.getCertPath(),
            x509SecurityInfo.getPrivateKey(),
            nodeRuntime.getLocalAreaNetworkID(),
            nodeRuntime.getExternalHostName(),
            nodeRuntime.getExternalPort(),
            nodeRuntime.getInternalHostName(),
            nodeRuntime.getInternalPort());
    nodeRuntime.registerRoleForRemoteCommunications(roleInfo);
  }

  /** Gets the role X509 certificate alias.
   *
   * @return the role X509 certificate alias
   */
  public String getRoleAlias() {
    return roleAlias;
  }

  /** Sets the role X509 certificate alias.
   *
   * @param roleAlias the roleAlias to set
   */
  public void setRoleAlias(final String roleAlias) {
    //Preconditions
    assert roleAlias != null : "roleAlias must not be null";
    assert !roleAlias.isEmpty() : "roleAlias must not be empty";

    this.roleAlias = roleAlias;
  }

  /** Finds or creates a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   * @return a sharable subskill instance
   */
  public AbstractSubSkill findOrCreateSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    final Class<?> clazz;
    try {
      clazz = Class.forName(subSkillClassName);
    } catch (ClassNotFoundException ex) {
      throw new TexaiException(ex);
    }
    AbstractSubSkill subSkill;
    // find an existing sharable subskill instance or create a new one and initialize it
    synchronized (subSkillsDictionary) {
      subSkill = subSkillsDictionary.get(subSkillClassName);
      if (subSkill == null) {
        subSkill = instantiateSubSkill(clazz);
        subSkill.setRole(this);
        subSkill.initialization();
        subSkillsDictionary.put(subSkillClassName, subSkill);
      }
    }
    return subSkill;
  }

  /** Returns an instance of the given subskill class.
   *
   * @param clazz the given subskill class
   * @return an instance of the given subskill class
   */
  private AbstractSubSkill instantiateSubSkill(final Class<?> clazz) {
    //Preconditions
    assert clazz != null : "clazz must not be null";

    try {
      final AbstractSubSkill subSkill = (AbstractSubSkill) clazz.newInstance();
      subSkill.setSkillState(State.READY);
      return subSkill;
    } catch (InstantiationException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Finds a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   * @return a sharable subskill instance, or null if not found
   */
  public AbstractSubSkill findSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    synchronized (subSkillsDictionary) {
      return subSkillsDictionary.get(subSkillClassName);
    }
  }

  /** Returns whether the other object equals this one.
   *
   * @param obj the other object
   * @return whether the other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Role other = (Role) obj;
    return Objects.equals(this.id, other.id);
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 71 * hash + Objects.hashCode(this.id);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    //Preconditions
    assert roleTypeName != null : "role type name must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('[');
    if (node != null) {
      stringBuilder.append(node.getNodeNickname()).append(':');
    }
    stringBuilder.append(roleTypeName).append("]");
    return stringBuilder.toString();
  }

  /** Ensures that this persistent object is fully instantiated. */
  @Override
  public void instantiate() {
    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.instantiate();
    }

    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      LOGGER.info(this + " state value: " + stateValueBinding);
      stateVariableDictionary.put(
              stateValueBinding.getVariableName(),
              stateValueBinding);
    }
  }

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  public void cascadePersist(
          final RDFEntityManager rdfEntityManager,
          final URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    cascadePersist(this, rdfEntityManager, overrideContext);
  }

  /** Recursively persists this RDF entity and all its components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   * @param overrideContext the user's belief context, or null to persist to each object's default context
   */
  @Override
  public void cascadePersist(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager, URI overrideContext) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    }
    rdfEntityManager.persist(this, overrideContext);
  }

  /** Recursively removes this RDF entity and all its unshared components.
   *
   * @param rootRDFEntity the root RDF entity
   * @param rdfEntityManager the RDF entity manager
   */
  @Override
  public void cascadeRemove(RDFPersistent rootRDFEntity, RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    for (final StateValueBinding stateValueBinding : stateValueBindings) {
      stateValueBinding.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    }
    rdfEntityManager.remove(this);
  }

  /** Compares another role with this one.
   *
   * @param that the other role
   * @return -1 if less than, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final Role that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.roleTypeName.compareTo(that.roleTypeName);
  }
}
