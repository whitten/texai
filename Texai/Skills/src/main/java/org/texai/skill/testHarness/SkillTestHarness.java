package org.texai.skill.testHarness;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
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
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;
import org.texai.x509.X509Utils;

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
  // the container name
  private final String containerName;
  // the mock NodeRuntime
  private NodeRuntime nodeRuntime;
  // the mock Node
  private final Node node;
  // the mock Role
  private final MockRole role;
  // the messages sent by the mock role
  private final List<Message> sentMessages = new ArrayList<>();
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

    containerName = Node.extractContainerName(qualifiedName);
    nodeRuntime = new MockNodeRuntime(
            containerName,
            NetworkUtils.TEXAI_TESTNET); // networkName

    final Set<Role> roles = new ArraySet<>();
    role = new MockRole(
            qualifiedName,
            description,
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            areRemoteCommunicationsPermitted,
            sentMessages);
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
    role.getSkillDictionary().values().forEach((AbstractSkill skill) -> {
      skill.setIsUnitTest(true);
    });
  }

  /**
   * Resets the test harness.
   */
  public void reset() {
    nodeRuntime.getSingletonAgentHostsAccess().initializeSingletonAgentsHosts();
    nodeRuntime.getContainerInfoAccess().initializeContainerInfos();
    sentMessages.clear();
    role.operationAndServiceInfo = null;
    isTerminated = false;
  }

  /**
   * Returns whether the JVM has been terminated by the application.
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

    final AbstractSkill skill = role.getSkill(skillClassName);

    //Postconditions
    assert skill.isUnitTest() : "skill must be running in unit test mode " + skillClassName;

    return skill;
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
    if (sentMessages.isEmpty()) {
      return null;
    } else {
      return sentMessages.get(sentMessages.size() - 1);
    }
  }

  /**
   * Returns the messages sent by the mock role.
   *
   * @return the most recent message sent by the mock role
   */
  public List<Message> getSentMessages() {
    return sentMessages;
  }

  /**
   * Returns the most recent operation and service information propagated by the mock role.
   *
   * @return the most recent operation and service information propagated by the mock role
   */
  public OperationAndSenderServiceInfo getOperationAndSenderServiceInfo() {
    return role.operationAndServiceInfo;
  }

  class MockNodeRuntime extends NodeRuntime {

    /**
     * Constructs a new singleton NodeRuntime instance.
     *
     * @param containerName the container name
     * @param networkName the network name, mainnet or testnet
     */
    MockNodeRuntime(
            final String containerName,
            final String networkName) {
      super(containerName, networkName);
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

  static class MockNode extends Node {

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

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code
     */
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /**
     * Returns whether another object equals this one.
     *
     * @param obj the other object
     *
     * @return whether another object equals this one
     */
    @Override
    public boolean equals(Object obj) {
      if (this.getClass().equals(obj.getClass())) {
        return false;
      } else {
        return super.equals(obj);
      }
    }

  }

  static class MockRole extends Role {

    // the default serial version UID
    private static final long serialVersionUID = 1L;
    // the sent messages
    final List<Message> sentMessages;
    // the operation and service information
    OperationAndSenderServiceInfo operationAndServiceInfo;
    // the X.509 security information
    private X509SecurityInfo x509SecurityInfo;
    // the test keystore path
    private final static String KEY_STORE_FILE_PATH = "data/test-keystore.uber";

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
     * @param sentMessages the sent messages
     */
    public MockRole(
            final String qualifiedName,
            final String description,
            final String parentQualifiedName,
            final Set<String> childQualifiedNames,
            final Set<SkillClass> skillClasses,
            final Set<String> variableNames,
            final boolean areRemoteCommunicationsPermitted,
            final List<Message> sentMessages) {
      super(
              qualifiedName,
              description,
              parentQualifiedName,
              childQualifiedNames,
              skillClasses,
              variableNames,
              areRemoteCommunicationsPermitted);
      this.sentMessages = sentMessages;
    }

    /**
     * Sends the given message via the node runtime.
     *
     * @param message the given message
     */
    @Override
    public void sendMessageViaSeparateThread(final Message receivedMessage, final Message message) {
      //Preconditions
      assert message != null : "message must not be null";
      assert getNodeRuntime() != null : "nodeRuntime must not be null";

      sendMessage(receivedMessage, message);
    }

    /**
     * Sends the given message via the node runtime.
     *
     * @param message the given message
     */
    @Override
    public void sendMessage(final Message receivedMessage, final Message message) {
      //Preconditions
      assert message != null : "message must not be null";
      assert getNodeRuntime() != null : "nodeRuntime must not be null";

      traceMessage(receivedMessage, message);
      sentMessages.add(message);
    }

    /**
     * Propagates the given operation to the child roles.
     *
     * @param receivedMessage the received message
     * @param senderService the sender service
     */
    @Override
    public void propagateOperationToChildRoles(
            final Message receivedMessage,
            final String senderService) {
      //Preconditions
      assert receivedMessage != null : "operation must not be null";
      assert StringUtils.isNonEmptyString(senderService) : "senderService must not be null";
      assert !getChildQualifiedNames().isEmpty() : "childQualifiedRoles must not be empty";

      operationAndServiceInfo = new OperationAndSenderServiceInfo(
              receivedMessage.getOperation(),
              senderService);

      super.propagateOperationToChildRoles(receivedMessage, senderService);
    }

    /**
     * Propagates the given operation to the child roles, using separate threads.
     *
     * @param receivedMessage the received message
     * @param senderService the sender service
     */
    @Override
    public void propagateOperationToChildRolesSeparateThreads(
            final Message receivedMessage,
            final String senderService) {
      //Preconditions
      assert receivedMessage != null : "operation must not be null";
      assert StringUtils.isNonEmptyString(senderService) : "senderService must not be null";
      assert !getChildQualifiedNames().isEmpty() : "childQualifiedRoles must not be empty";

      operationAndServiceInfo = new OperationAndSenderServiceInfo(
              receivedMessage.getOperation(),
              senderService);

      super.propagateOperationToChildRolesSeparateThreads(receivedMessage, senderService);

    }

    /**
     * Returns the X.509 certificate belonging to this role, or null if this role is not permitted to communicate with another container.
     *
     * @return the X.509 certificate belonging to this role
     */
    @Override
    public X509Certificate getX509Certificate() {
      if (areRemoteCommunicationsPermitted()) {
        if (x509SecurityInfo == null) {
          KeyStore keyStore;
          final char[] keyStorePassword = "test-password".toCharArray();
          try {
            LOGGER.debug("getting the keystore " + KEY_STORE_FILE_PATH);
            keyStore = X509Utils.findOrCreateUberKeyStore(
                    KEY_STORE_FILE_PATH, // keyStoreFilePath
                    keyStorePassword);
            try (final FileOutputStream keyStoreOutputStream = new FileOutputStream(new File(KEY_STORE_FILE_PATH))) {
              keyStore.store(keyStoreOutputStream, keyStorePassword);
            }
            if (!X509Utils.keyStoreContains(
                    KEY_STORE_FILE_PATH,
                    keyStorePassword,
                    getQualifiedName())) {
              LOGGER.debug("    generating a new certificate");
              final KeyPair keyPair;
              keyPair = X509Utils.generateRSAKeyPair3072();
              x509SecurityInfo = X509Utils.generateX509SecurityInfo(
                      keyStore,
                      keyStorePassword,
                      keyPair,
                      null, // uid
                      getQualifiedName(), // domainComponent
                      getQualifiedName()); // certificateAlias
            }

            x509SecurityInfo = X509Utils.getX509SecurityInfo(
                    keyStore,
                    keyStorePassword, // keyStorePassword
                    getQualifiedName()); // alias
          } catch (InvalidAlgorithmParameterException | KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException ex) {
            LOGGER.error(StringUtils.getStackTraceAsString(ex));
            throw new TexaiException(ex);
          }
        }
        return x509SecurityInfo.getX509Certificate();
      } else {
        return null;
      }
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code
     */
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /**
     * Returns whether another object equals this one.
     *
     * @param obj the other object
     *
     * @return whether another object equals this one
     */
    @Override
    public boolean equals(Object obj) {
      if (this.getClass().equals(obj.getClass())) {
        return false;
      } else {
        return super.equals(obj);
      }
    }

  }

  /**
   * Provides a container for operation and sender service information.
   */
  public static class OperationAndSenderServiceInfo {

    // the given operation
    final String operation;
    // the sender service
    final String senderService;

    OperationAndSenderServiceInfo(
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
