package org.texai.skill.testHarness;

import java.util.Set;
import org.apache.log4j.Logger;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;

/**
 * SkillTestHarness.java
 *
 * Description: Provides a test harness for skills, including a mock NodeRuntime, mock Nodes, and Mock Roles.
 *
 * Copyright (C) Jan 16, 2015, Stephen L. Reed.
 */
public class SkillTestHarness {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(SkillTestHarness.class);
  // the mock NodeRuntime
  private final NodeRuntime nodeRuntime;
  // the mock Node
  private final Node node;
  // the mock Role
  private final Role role;
  // the most recent message sent by the mock role
  private Message sentMessage;
  // the most recent operation and service information propagated by the mock role
  private OperationAndServiceInfo operationAndServiceInfo;
  // the indicator whether the JVM has been terminated by the application
  private boolean isTerminated = false;

  /**
   * Creates a new instance of SkillTestHarness.
   *
   * @param name the node name which must end in "Agent"
   * @param missionDescription the node's mission described in English
   * @param isNetworkSingleton the indicator whether this node is a singleton nomadic agent, in which case only one container in the network
   * hosts the active node and all other nodes having the same name are inactive
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
  public SkillTestHarness(
          final String name,
          final String missionDescription,
          final boolean isNetworkSingleton,
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

    final String containerName = Node.extractContainerName(qualifiedName);
    nodeRuntime = new MockNodeRuntime(containerName); // containerName

    final Set<Role> roles = new ArraySet<>();
    role = new MockRole(
            qualifiedName,
            description,
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            areRemoteCommunicationsPermitted);
    roles.add(role);

    node = new MockNode(
            name,
            missionDescription,
            roles,
            isNetworkSingleton);
    node.setNodeRuntime(nodeRuntime);

    nodeRuntime.registerRole(role);
    role.setNode(node);
    role.initialize(
            nodeRuntime,
            null); // x509SecurityInfo
  }

  /**
   * Resets the test harness.
   */
  public void reset() {
    sentMessage = null;
    operationAndServiceInfo = null;
    isTerminated = false;
  }

  /** Returns whether the JVM has been terminated by the application.
   *
   * @return whether the JVM has been terminated by the application
   */
  public boolean isTerminated() {
    return isTerminated;
  }

  /**
   * Sends the given message to the mock role.
   *
   * @param message the given message
   */
  public void dispatchMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert role != null : "role must not be null";

    role.dispatchMessage(message);
  }

  /**
   * Gets the state of the role's skill having the given class name.
   *
   * @param skillClassName the given class name
   *
   * @return the state of the role's skill
   */
  public AHCSConstants.State getSkillState(final String skillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(skillClassName) : "skillClassName must be a non-empty string";
    assert role.getSkillDictionary().containsKey(skillClassName) :
            "skillClassName is not a skill for this role, skillDictonary ...\n " + role.getSkillDictionary();

    final AbstractSkill skill = role.getSkill(skillClassName);
    return skill.getSkillState();
  }

  /**
   * Sets the state of the role's skill having the given class name.
   *
   * @param state the state of the role's skill
   * @param skillClassName the given class name
   */
  public void setSkillState(
          final AHCSConstants.State state,
          final String skillClassName) {
    //Preconditions
    assert state != null : "state must not be null";
    assert StringUtils.isNonEmptyString(skillClassName) : "skillClassName must be a non-empty string";
    assert role.getSkillDictionary().containsKey(skillClassName) :
            "skillClassName is not a skill for this role, skillDictonary ...\n " + role.getSkillDictionary();

    final AbstractSkill skill = role.getSkill(skillClassName);
    skill.setSkillState(state);
  }

  /**
   * Gets the role's skill having the given class name.
   *
   * @param skillClassName the given class name
   *
   * @return the skill
   */
  public AbstractSkill getSkill(final String skillClassName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(skillClassName) : "skillClassName must be a non-empty string";
    assert role.getSkillDictionary().containsKey(skillClassName) :
            "skillClassName is not a skill for this role, skillDictonary ...\n " + role.getSkillDictionary();

    return role.getSkill(skillClassName);
  }

  /**
   * Gets the mock node runtime.
   *
   * @return the mock node runtime
   */
  public NodeRuntime getNodeRuntime() {
    return nodeRuntime;
  }

  /**
   * Gets the mock node.
   *
   * @return the mock node
   */
  public Node getNode() {
    return node;
  }

  /**
   * Gets the mock role.
   *
   * @return the mock role
   */
  public Role getRole() {
    return role;
  }

  /**
   * Returns the most recent message sent by the mock role.
   *
   * @return the most recent message sent by the mock role
   */
  public Message getSentMessage() {
    return sentMessage;
  }

  /**
   * Returns the most recent operation and service information propagated by the mock role.
   *
   * @return the most recent operation and service information propagated by the mock role
   */
  public OperationAndServiceInfo getOperationAndServiceInfo() {
    return operationAndServiceInfo;
  }

  class MockNodeRuntime extends NodeRuntime {

    /**
     * Constructs a new singleton NodeRuntime instance.
     *
     * @param containerName the container name
     */
    MockNodeRuntime(final String containerName) {
      super(containerName);
    }

    /**
     * Terminates this JVM with an exit code that causes the bash wrapper script to restart the Java application.
     */
    @Override
    public void restartJVM() {
      LOGGER.info("MOCK - the JVM has terminated");
      isTerminated = true;
    }
  }

  class MockNode extends Node {

    // the default serial version UID
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MockNode instance.
     *
     * @param name the node name which must end in "Agent"
     * @param missionDescription the node's mission described in English
     * @param roles the roles
     * @param isNetworkSingleton the indicator whether this node is a singleton nomadic agent, in which case only one container in the
     * network hosts the active node and all other nodes having the same name are inactive
     */
    public MockNode(
            final String name,
            final String missionDescription,
            final Set<Role> roles,
            final boolean isNetworkSingleton) {
      super(name,
              missionDescription,
              roles,
              isNetworkSingleton);
    }
  }

  class MockRole extends Role {

    // the default serial version UID
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new MockRole instance.
     *
     * @param qualifiedName the role qualified name
     * @param description the role's description in English
     * @param parentQualifiedName the parent qualified role name, i.e. container.nodename.rolename, which is null if this is a top level
     * role
     * @param childQualifiedNames the qualified child role names, i.e. container.nodename.rolename, which are empty if this is a lowest
     * level role.
     * @param skillClasses the skill class names, which are objects that contain, verify and format the class names
     * @param variableNames the state variable names
     * @param areRemoteCommunicationsPermitted the indicator whether this role is permitted to send a message to a recipient in another
     * container, which requires an X.509 certificate
     */
    public MockRole(
            final String qualifiedName,
            final String description,
            final String parentQualifiedName,
            final Set<String> childQualifiedNames,
            final Set<SkillClass> skillClasses,
            final Set<String> variableNames,
            final boolean areRemoteCommunicationsPermitted) {
      super(
              qualifiedName,
              description,
              parentQualifiedName,
              childQualifiedNames,
              skillClasses,
              variableNames,
              areRemoteCommunicationsPermitted);
    }

    /**
     * Sends the given message via the node runtime.
     *
     * @param message the given message
     */
    @Override
    public void sendMessageViaSeparateThread(final Message message) {
      //Preconditions
      assert message != null : "message must not be null";
      assert getNodeRuntime() != null : "nodeRuntime must not be null";

      sentMessage = message;
    }

    /**
     * Sends the given message via the node runtime.
     *
     * @param message the given message
     */
    @Override
    public void sendMessage(final Message message) {
      //Preconditions
      assert message != null : "message must not be null";
      assert getNodeRuntime() != null : "nodeRuntime must not be null";

      sentMessage = message;
    }

    /**
     * Propagates the given operation to the child roles.
     *
     * @param operation the given operation
     * @param senderService the sender service
     */
    @Override
    public void propagateOperationToChildRoles(
            final String operation,
            final String senderService) {
      //Preconditions
      assert operation != null : "operation must not be null";
      assert !operation.isEmpty() : "operation must not be empty";
      assert senderService != null : "senderService must not be null";
      assert !senderService.isEmpty() : "senderService must not be empty";

      operationAndServiceInfo = new OperationAndServiceInfo(operation, senderService);
    }

    /**
     * Propagates the given operation to the child roles, using separate threads.
     *
     * @param operation the given operation
     * @param senderService the sender service
     */
    @Override
    public void propagateOperationToChildRolesSeparateThreads(
            final String operation,
            final String senderService) {
      //Preconditions
      assert operation != null : "operation must not be null";
      assert !operation.isEmpty() : "operation must not be empty";
      assert senderService != null : "senderService must not be null";
      assert !senderService.isEmpty() : "senderService must not be empty";

      operationAndServiceInfo = new OperationAndServiceInfo(operation, senderService);
    }

  }

  /**
   * Provides a container for operation and sender service information.
   */
  public static class OperationAndServiceInfo {

    // the given operation
    final String operation;
    // the sender service
    final String senderService;

    OperationAndServiceInfo(
            final String operation,
            final String senderService) {
      //Preconditions
      assert operation != null : "operation must not be null";
      assert !operation.isEmpty() : "operation must not be empty";
      assert senderService != null : "senderService must not be null";
      assert !senderService.isEmpty() : "senderService must not be empty";

      this.operation = operation;
      this.senderService = senderService;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return (new StringBuilder().append('[').append(operation).append(", ").append(senderService).append(']').toString());
    }
  }

}
