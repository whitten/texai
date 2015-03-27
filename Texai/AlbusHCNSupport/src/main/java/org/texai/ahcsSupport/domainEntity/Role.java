/*
 * Role.java
 *
 * Created on Mar 11, 2010, 2:16:21 PM
 *
 * Description: Provides a role in an Albus Hierarchical Control System node.
 *
 * Copyright (C) Mar 11, 2010 reed.
 */
package org.texai.ahcsSupport.domainEntity;

import java.util.Objects;
import org.texai.ahcsSupport.skill.AbstractSkill;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSubSkill;
import org.texai.ahcsSupport.MessageDispatcher;
import org.texai.ahcsSupport.skill.ManagedSessionSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.ahcsSupport.skill.SessionManagerSkill;
import org.texai.kb.persistence.CascadePersistence;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

/**
 * Provides a role in an Albus Hierarchical Control System node.
 *
 * @author reed
 */
@ThreadSafe
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SE_TRANSIENT_FIELD_NOT_RESTORED", justification = "handled by NodesInitializer")
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class Role implements CascadePersistence, MessageDispatcher, Comparable<Role> {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the logger
  private static final Logger LOGGER = Logger.getLogger(Role.class);
  // the id assigned by the persistence framework
  @Id
  private URI id;
  // the qualified role name, i.e. container.nodename.rolename
  @RDFProperty()
  private final String qualifiedName;
  // the role's description in English
  @RDFProperty
  private final String description;
  // the containing node, or null if no node is associated with an abstract role
  @RDFProperty(predicate = "texai:ahcsNode_role", inverse = true)
  private Node node;
  // the parent qualified role name, i.e. container.nodename.rolename, which is null if this is a top level role
  @RDFProperty()
  private String parentQualifiedName;
  // the qualified child role names, i.e. container.nodename.rolename, which are empty if this is a lowest level role.
  @RDFProperty()
  private final Set<String> childQualifiedNames;
  // the skill class names, which are objects that verify and format the class names
  @RDFProperty
  private final Set<SkillClass> skillClasses;
  // the state variable names
  @RDFProperty()
  private final Set<String> variableNames;
  // the indicator whether this role is permitted to send a message to a recipient in another container, which requires
  // an X.509 certificate
  @RDFProperty()
  private final boolean areRemoteCommunicationsPermitted;
  // the cached X.509 certificate information used to authenticate, sign and encrypt remote communications, or null if this
  // role performs only communications local to the container.
  private transient X509SecurityInfo x509SecurityInfo;
  // the node runtime
  private transient BasicNodeRuntime nodeRuntime;
  // the transient role's skill dictionary, service (skill class name) --> skill
  private transient final Map<String, AbstractSkill> skillDictionary = new HashMap<>();
  // the role state
  private transient final AtomicReference<State> roleState = new AtomicReference<>(State.UNINITIALIZED);
  // the subskills dictionary, subskill class name --> subskill shared instance
  private transient final Map<String, AbstractSubSkill> subSkillsDictionary = new HashMap<>();

  /**
   * Constructs a new Role instance. Used by the persistence framework.
   */
  public Role() {
    qualifiedName = null;
    description = null;
    node = null;
    parentQualifiedName = null;
    childQualifiedNames = null;
    skillClasses = null;
    variableNames = null;
    areRemoteCommunicationsPermitted = false;
  }

  /**
   * Constructs a new Role instance.
   *
   * @param qualifiedName the role qualified name
   * @param description the role's description in English
   * @param parentQualifiedName the parent qualified role name, i.e. container.nodename.rolename, which is null if this is a top level role
   * @param childQualifiedNames the qualified child role names, i.e. container.nodename.rolename, which are empty if this is a lowest level
   * role.
   * @param skillClasses the skill class names, which are objects that contain, verify and format the class names
   * @param variableNames the state variable names
   * @param areRemoteCommunicationsPermitted the indicator whether this role is permitted to send a message to a recipient in another
   * container, which requires an X.509 certificate
   */
  public Role(
          final String qualifiedName,
          final String description,
          final String parentQualifiedName,
          final Set<String> childQualifiedNames,
          final Set<SkillClass> skillClasses,
          final Set<String> variableNames,
          final boolean areRemoteCommunicationsPermitted) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";
    assert StringUtils.isNonEmptyString(description) : "description must be a non-empty string";
    assert childQualifiedNames != null : "childQualifiedNames must not be null";
    assert skillClasses != null : "skillClasses must not be null";
    assert variableNames != null : "variableNames must not be null";

    this.qualifiedName = qualifiedName;
    this.description = description;
    this.parentQualifiedName = parentQualifiedName;
    this.childQualifiedNames = childQualifiedNames;
    this.skillClasses = skillClasses;
    this.variableNames = variableNames;
    this.areRemoteCommunicationsPermitted = areRemoteCommunicationsPermitted;
  }

  /**
   * Initializes a role object retrieved from the quad store.
   *
   * @param nodeRuntime the node runtime
   * @param x509SecurityInfo the cached X.509 certificate information used to authenticate, sign and encrypt remote communications, or null
   * if this role performs only communications local to the container.
   */
  public void initialize(
          final BasicNodeRuntime nodeRuntime,
          final X509SecurityInfo x509SecurityInfo) {
    //Preconditions
    assert nodeRuntime != null : "nodeRuntime must not be null";
    assert node != null : "node must not be null";
    assert node.getNodeRuntime() != null && node.getNodeRuntime().equals(nodeRuntime);

    this.nodeRuntime = nodeRuntime;
    this.x509SecurityInfo = x509SecurityInfo;
    installSkills();
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
   * Gets the qualified name, i.e. container.nodename.rolename.
   *
   * @return the qualified name
   */
  public String getQualifiedName() {
    return qualifiedName;
  }

  /**
   * Extracts the role name from the given qualified name string, container-name.agent-name.role-name.
   *
   * @param qualifiedName the given qualified name string
   *
   * @return the role name, or null if not present
   */
  public static String extractRoleName(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    final String[] names = qualifiedName.split("\\.");
    assert names.length == 3 : "cannot find role in: " + qualifiedName;

    return names[2];
  }

  /**
   * Gets the containing node.
   *
   * @return the containing node
   */
  public synchronized Node getNode() {
    return node;
  }

  public synchronized void setNode(final Node node) {
    //Preconditions
    assert node != null : "node must not be null";

    this.node = node;
  }

  /**
   * Gets the role state.
   *
   * @return the role state
   */
  public synchronized State getRoleState() {
    return roleState.get();
  }

  /**
   * Installs the skills for this role.
   *
   */
  private void installSkills() {
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
          LOGGER.info("      " + this + " constructed managed session skill: " + skill);

        } else {
          // ordinary skill that does not need sessions managed
          skill.setRole(this);
          skillDictionary.put(skillClassName, skill);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("      " + this + " constructed skill: " + skill);
          }
        }
      }
    }
  }

  /**
   * Gets an unmodifiable copy of the role's skills.
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

  /**
   * Gets the sibling role having the given role name.
   *
   * @param roleName the given role name
   *
   * @return the sibling role
   */
  public Role getSiblingRole(final String roleName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(roleName) : "roleName must be a non-empty string";

    for (final Role role : node.getRoles()) {
      final String siblingRoleName = Role.extractRoleName(role.qualifiedName);
      if (siblingRoleName.equals(roleName)) {
        return role;
      }
    }
    return null;
  }

  /**
   * Finds the role's skill instance having the specified class name (service).
   *
   * @param skillClassName the specified class name (service)
   *
   * @return the skill
   */
  public AbstractSkill getSkill(final String skillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(skillClassName) : "skillClassName must be a non-empty string";

    synchronized (skillDictionary.values()) {
      return skillDictionary.get(skillClassName);
    }
  }

  /**
   * Gets the node runtime.
   *
   * @return the node runtime
   */
  public BasicNodeRuntime getNodeRuntime() {
    return nodeRuntime;
  }

  /**
   * Gets whether this role is permitted to send a message to a recipient in another container, which requires an X.509 certificate.
   *
   * @return whether this role is permitted to send a message to a recipient in another container
   */
  public boolean areRemoteCommunicationsPermitted() {
    return areRemoteCommunicationsPermitted;
  }

  /**
   * Returns the X.509 certificate belonging to this role, or null if this role is not permitted to communicate with another container.
   *
   * @return the X.509 certificate belonging to this role
   */
  public X509Certificate getX509Certificate() {
    if (x509SecurityInfo == null) {
      assert !areRemoteCommunicationsPermitted;
      return null;
    } else {
      assert areRemoteCommunicationsPermitted;
      return x509SecurityInfo.getX509Certificate();
    }
  }

  /**
   * Gets X.509 security information for this role.
   *
   * @return the X.509 security information
   */
  public X509SecurityInfo getX509SecurityInfo() {
    //Preconditions
    assert areRemoteCommunicationsPermitted() : "role must be permitted to perform remote communications";

    return nodeRuntime.getX509SecurityInfo(this);
  }

  /**
   * Gets the state value associated with the given variable name.
   *
   * @param stateVariableName the given variable name
   *
   * @return the state value associated with the given variable name
   */
  public Object getStateValue(final String stateVariableName) {
    //Preconditions
    assert stateVariableName != null : "stateVariableName must not be null";
    assert !stateVariableName.isEmpty() : "stateVariableName must not be empty";

    return node.getStateValue(stateVariableName);
  }

  /**
   * Sets the state value associated with the given variable name.
   *
   * @param variableName the given variable name
   * @param value the state value
   */
  public void setStateValue(final String variableName, final Object value) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    node.setStateValue(variableName, value);
  }

  /**
   * Removes the role state value binding for the given variable.
   *
   * @param variableName the variable name
   */
  public void removeStateValueBinding(final String variableName) {
    //Preconditions
    assert variableName != null : "variableName must not be null";
    assert !variableName.isEmpty() : "variableName must not be empty";

    node.removeStateValueBinding(variableName);
  }

  /**
   * Sends the given message to the addressed sub-skill and returns the response message.
   *
   * @param message the message for the addressed sub-skill
   *
   * @return the response message
   */
  public Message converseMessageWithSubSkill(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    AbstractSkill skill;
    synchronized (skillDictionary) {
      skill = skillDictionary.get(message.getRecipientService());
    }
    if (skill == null) {
      LOGGER.info(getNode().getName() + ": subskill not found for service: " + message.getRecipientService() + ", constructing it");
      // ordinary skill that does not need sessions managed
      try {
        skill = (AbstractSkill) Class.forName(message.getRecipientService()).newInstance();
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
        throw new TexaiException(ex);
      }
      skill.setRole(this);
      skillDictionary.put(message.getRecipientService(), skill);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info(getNode().getName() + ": " + this + " constructed skill: " + skill);
      }
    }
    return skill.converseMessage(message);
  }

  /**
   * Receives an inbound message for this role.
   *
   * @param message the Albus message
   */
  @Override
  public void dispatchMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return;
    }

    // the message service field optionally names a skill interface
    AbstractSkill skill = null;
    if (message.getRecipientService() != null) {
      synchronized (skillDictionary) {
        skill = skillDictionary.get(message.getRecipientService());
        if (skill == null) {
          // not a primary skill for this role, try the shared subskill dictionary
          skill = findSubSkill(message.getRecipientService());
        }
        assert skill != null :
                "service not found " + message.getRecipientService() + "\n" + message.toDetailedString()
                + "\n skillDictionary: " + skillDictionary;
      }
    }

    if (skill == null) {
      // dispatch the message to any skill that understands the operation
      boolean isSkillFound = false;
      for (final AbstractSkill skill1 : getSkills()) {
        if (skill1.isOperationUnderstood(message.getOperation())) {
          isSkillFound = true;
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(this + ", skill " + skill1.toString() + " understands " + message.getOperation());
          }
          skill1.receiveMessage(message);
        }
      }
      if (!isSkillFound) {
        if (message.getRecipientService() == null) {
          LOGGER.info("no skill understands the operation " + message.getOperation() + " ...");
          getSkills().stream().sorted().forEach((AbstractSkill skill1) -> {
            LOGGER.info("  " + skill1 + " understands");
            for (final String understoodOperation : skill1.getUnderstoodOperations()) {
              LOGGER.info("    " + understoodOperation);
            }
          });
        } else {
          LOGGER.info(getNode().getName() + ": skill not found for service: " + message.toDetailedString() + "\nservice: " + message.getRecipientService());
          LOGGER.info(getNode().getName() + ": skillDictionary:\n  " + skillDictionary);
        }
        // not understood
        final Message notUnderstoodInfoMessage = new Message(
                qualifiedName, // senderQualifiedName
                getClass().getName(), // senderService,
                message.getSenderQualifiedName(), // recipientQualifiedName
                message.getSenderService(), // service
                AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO); // operation
        notUnderstoodInfoMessage.put(AHCSConstants.MSG_PARM_ORIGINAL_MESSAGE, message);
        sendMessage(message, notUnderstoodInfoMessage);
      }

    } else {
      // the recipientService was found
      skill.receiveMessage(message);
    }
  }

  /**
   * Sends the given message via the node runtime.
   *
   * @param receivedMessage the received message which invoked the skill, which may be null
   * @param message the given message
   */
  public void sendMessageViaSeparateThread(final Message receivedMessage, final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    getNodeRuntime().getExecutor().execute(new MessageSendingRunnable(
            receivedMessage,
            message,
            this));
  }

  /**
   * Sends the given message via the node runtime.
   *
   * @param receivedMessage the received message which invoked the skill, which may be null
   * @param message the given message
   */
  public void sendMessage(
          final Message receivedMessage,
          final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert nodeRuntime != null : "nodeRuntime must not be null";

    if (!message.getSenderQualifiedName().equals(qualifiedName)) {
      LOGGER.warn("cannot send message for which this role is not the sender role " + message.toDetailedString());
      throw new TexaiException("cannot send message for which this role is not the sender " + message.toDetailedString());
    }

    traceMessage(receivedMessage, message);

    if (areRemoteCommunicationsPermitted && message.isBetweenContainers()) {
      // sign messages sent between containers
      assert x509SecurityInfo != null;
      message.sign(x509SecurityInfo.getPrivateKey());
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(getNode().getName() + ": sending message: " + message.toDetailedString());
    }
    nodeRuntime.dispatchMessage(message);
  }

  protected void traceMessage(
          final Message receivedMessage,
          final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final StringBuilder newStringBuilder = new StringBuilder();
    final StringBuilder oldStringBuilder;
    if (receivedMessage != null) {
      oldStringBuilder = (StringBuilder) receivedMessage.get(AHCSConstants.MSG_PARM_MESSAGE_TRACE);
      if (oldStringBuilder != null) {
        newStringBuilder.append(oldStringBuilder);
      }
      newStringBuilder
              .append("\n    ")
              .append(receivedMessage.toBriefString());
    }
    message.put(
            AHCSConstants.MSG_PARM_MESSAGE_TRACE, // parameterName
            newStringBuilder); // parameterValue
  }

  /**
   * Propagates the given operation to the child roles.
   *
   * @param receivedMessage the received message
   * @param senderService the sender service
   */
  public void propagateOperationToChildRoles(
          final Message receivedMessage,
          final String senderService) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert StringUtils.isNonEmptyString(senderService) : "senderService must not be null";
    assert !childQualifiedNames.isEmpty() : "childQualifiedNames must not be empty in " + this;

    childQualifiedNames.stream().forEach((childQualifiedName) -> {
      final Message propagatedMessage = new Message(
              qualifiedName, // senderQualifiedName
              senderService,
              childQualifiedName, // recipientQualifiedName
              null, // service
              receivedMessage.getOperation());
      sendMessage(receivedMessage, propagatedMessage);
    });
  }

  /**
   * Propagates the given operation to the child roles, using separate threads.
   *
   * @param receivedMessage the received message
   * @param senderService the sender service
   */
  public void propagateOperationToChildRolesSeparateThreads(
          final Message receivedMessage,
          final String senderService) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert StringUtils.isNonEmptyString(senderService) : "senderService must not be null";
    assert !childQualifiedNames.isEmpty() : "childQualifiedNames must not be empty";

    childQualifiedNames.stream().forEach((childQualifiedName) -> {
      final Message propagatedMessage = new Message(
              qualifiedName, // senderQualifiedName
              senderService,
              childQualifiedName, // recipientQualifiedName
              null, // service
              receivedMessage.getOperation());
      sendMessageViaSeparateThread(receivedMessage, propagatedMessage);
    });
  }

  /**
   * Finds or creates a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   *
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

  /**
   * Returns an instance of the given subskill class.
   *
   * @param clazz the given subskill class
   *
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

  /**
   * Finds a sharable subskill instance.
   *
   * @param subSkillClassName the class name of the sharable subskill
   *
   * @return a sharable subskill instance, or null if not found
   */
  public AbstractSubSkill findSubSkill(final String subSkillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(subSkillClassName) : "subSkillClassName must be a non-empty string";

    synchronized (subSkillsDictionary) {
      return subSkillsDictionary.get(subSkillClassName);
    }
  }

  /**
   * Gets the role's description in English.
   *
   * @return the role's description in English
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the parent qualified role name, i.e. container.nodename.rolename, which is null if this is a top level role.
   *
   * @return the parent qualified role name
   */
  public String getParentQualifiedName() {
    return parentQualifiedName;
  }

  /**
   * Sets the parent qualified role name, i.e. container.nodename.rolename.
   *
   * @param parentQualifiedName the parent qualified role name
   */
  public void setParentQualifiedName(final String parentQualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(parentQualifiedName) : "parentQualifiedName must be a non-empty string";

    this.parentQualifiedName = parentQualifiedName;
  }

  /**
   * Gets the qualified child role names, i.e. container.nodename.rolename, which are empty if this is a lowest level role.
   *
   * @return the qualified child role names
   */
  public Set<String> getChildQualifiedNames() {
    return childQualifiedNames;
  }

  /**
   * Gets the first qualified child role name, i.e. container.nodename.rolename, which matches the given nodename.
   *
   * @param nodeName the given child nodename, e.g. AICOperationAgent
   *
   * @return the qualified child role name, or null if not found
   */
  public String getChildQualifiedNameForAgent(final String nodeName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(nodeName) : "nodeName must be a non-empty string";

    final String target = "." + nodeName + ".";
    for (final String childQualifiedName : childQualifiedNames) {
      if (childQualifiedName.contains(target)) {
        //TODO role and message should validate the qualified name
        return childQualifiedName;
      }
    }
    return null;
  }

  /**
   * Gets the first qualified child role name, i.e. nodename.rolename, which matches the given nodename.
   *
   * @param qualifiedName the given child nodename.rolename, e.g. AICOperationAgent.AICOperationRole
   *
   * @return the qualified child role name, or null if not found
   */
  public String getChildQualifiedNameForAgentRole(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be a non-empty string";

    final String target = "." + qualifiedName;
    for (final String childQualifiedName : childQualifiedNames) {
      if (childQualifiedName.contains(target)) {
        //TODO role and message should validate the qualified name
        return childQualifiedName;
      }
    }
    return null;
  }

  /**
   * Gets the qualified child agent name, i.e. container-name.agent-name, which matches the given child agent name.
   *
   * @param agentName the given child agent, e.g. AICOperationAgent
   *
   * @return the qualified child role name, e.g. Mint.AICOperationAgent.AICOperationAgentRole, or null if not found
   */
  public String getFirstChildQualifiedNameForAgent(final String agentName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(agentName) : "agentName must be a non-empty string";

    final Set<String> childQualifiedNamesForAgent = getChildQualifiedNamesForAgent(agentName);
    Optional<String> optional = childQualifiedNamesForAgent.stream().findFirst();
    if (optional.isPresent()) {
      return optional.get();
    } else {
      return null;
    }
  }

  /**
   * Gets qualified child role names, i.e. container-name.agent-name.role-name, which match the given child agent name.
   *
   * @param agentName the given child agent, e.g. AICOperationAgent
   *
   * @return the qualified child role name, e.g. Mint.AICOperationAgent.AICOperationAgentRole, or null if not found
   */
  public Set<String> getChildQualifiedNamesForAgent(final String agentName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(agentName) : "agentName must be a non-empty string";

    final Set<String> childQualifiedNamesForAgent = new HashSet<>();
    final String target = "." + agentName + ".";
    childQualifiedNames.stream().filter((childQualifiedName) -> (childQualifiedName.contains(target))).forEach((childQualifiedName) -> {
      childQualifiedNamesForAgent.add(childQualifiedName);
    });
    return childQualifiedNamesForAgent;
  }

  /**
   * Gets the skill class names, which are objects that verify and format the class names.
   *
   * @return the skill class names
   */
  public Set<SkillClass> getSkillClasses() {
    return skillClasses;
  }

  /**
   * Gets the state variable names.
   *
   * @return the state variable names
   */
  public Set<String> getVariableNames() {
    return variableNames;
  }

  /**
   * Gets the role's skill dictionary, service (skill class name) --> skill.
   *
   * @return the role's skill dictionary
   */
  public Map<String, AbstractSkill> getSkillDictionary() {
    return skillDictionary;
  }

  /**
   * Gets the subskills dictionary, subskill class name --> subskill shared instance.
   *
   * @return the subskills dictionary
   */
  public Map<String, AbstractSubSkill> getSubSkillsDictionary() {
    return subSkillsDictionary;
  }

  /**
   * Returns whether this is a network singleton role.
   *
   * @return whether this is a network singleton role.
   */
  public boolean isNetworkSingletonRole() {
    return node.isNetworkSingleton();
  }

  /**
   * Returns whether the given role is a network singleton role.
   *
   * @param qualifiedName
   *
   * @return whether this is a network singleton role.
   */
  public boolean isNetworkSingletonRole(final String qualifiedName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "qualifiedName must be an non-empty string";
    assert getNodeRuntime().getLocalRole(qualifiedName) != null : qualifiedName + " must be registered with the node runtime"
            + "\n" + getNodeRuntime().formatLocalRoles();

    return getNodeRuntime().getLocalRole(qualifiedName).isNetworkSingletonRole();
  }

  /**
   * Returns whether the other object equals this one.
   *
   * @param obj the other object
   *
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
    return Objects.equals(this.qualifiedName, other.qualifiedName);
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(this.qualifiedName);
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    //Preconditions
    assert StringUtils.isNonEmptyString(qualifiedName) : "name must be a non empty string";

    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append('[').append(qualifiedName).append("]");
    return stringBuilder.toString();
  }

  /**
   * Ensures that this persistent object is fully instantiated.
   */
  @Override
  public void instantiate() {
    node.getId(); // force instantiation of the Node object.
    skillClasses.stream().forEach((skillClass) -> {
      skillClass.instantiate();
    });
  }

  /**
   * Recursively persists this RDF entity and all its components.
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

    skillClasses.stream().forEach((skillClass) -> {
      skillClass.cascadePersist(
              rootRDFEntity,
              rdfEntityManager,
              overrideContext);
    });
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

    skillClasses.stream().forEach((skillClass) -> {
      skillClass.cascadeRemove(
              rootRDFEntity,
              rdfEntityManager);
    });
    rdfEntityManager.remove(this);
  }

  /**
   * Compares another role with this one, collating by qualified role name, i.e. container.nodename.rolename.
   *
   * @param that the other role
   *
   * @return -1 if less than, 0 if equal, otherwise return +1
   */
  @Override
  public int compareTo(final Role that) {
    //Preconditions
    assert that != null : "that must not be null";

    return this.qualifiedName.compareTo(that.qualifiedName);
  }

  /**
   * Provides a message sending runnable.
   */
  public static class MessageSendingRunnable implements Runnable {

    // the received message which invoked the skill, or null
    final Message receivedMessage;
    // the message to send
    final Message message;
    // the sender role
    final Role role;

    /**
     * Constructs a new MessageSendingRunnable instance.
     *
     * @param receivedMessage the received message which invoked the skill, or null
     * @param message the message to send
     * @param role the sender role
     */
    public MessageSendingRunnable(
            final Message receivedMessage,
            final Message message,
            final Role role) {
      //Preconditions
      assert message != null : "message must not be null";
      assert role != null : "role must not be null";

      this.receivedMessage = receivedMessage;
      this.message = message;
      this.role = role;
    }

    /**
     * Sends the message.
     */
    @Override
    public void run() {
      try {
        role.sendMessage(receivedMessage, message);
      } catch (Throwable ex) {
        LOGGER.info("Caught exception " + ex.getMessage() + ", processing the message " + message);
        LOGGER.info("Stack trace ...\n" + StringUtils.getStackTraceAsString(ex));
        //TODO report to container operations
      }
    }
  }

}
