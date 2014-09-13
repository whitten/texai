/*
 * Bootstrap.java
 *
 * Created on Oct 9, 2011, 8:46:22 PM
 *
 * Description: Provides bootstrap installation behavior for the bootstrap role, in which certain nodes are created and parent/child role
 * relationships are connected.
 *
 * Copyright (C) Oct 9, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.lifecycle;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.impl.NodeRuntimeImpl;
import org.texai.skill.governance.TopFriendship;
import org.texai.skill.logging.JVMLoggerManagement;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.NodeAccess;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
import org.texai.util.StringUtils;

/** Provides bootstrap installation behavior for the bootstrap role, in which certain nodes are created and parent/child role
 * relationships are connected.
 *
 * @author reed
 */
@NotThreadSafe
public class Bootstrap extends AbstractSkill {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Bootstrap.class);

  /** Constructs a new Bootstrap instance. */
  public Bootstrap() {
  }

  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    switch (operation) {
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.AHCS_INITIALIZE_TASK:
        initialization(message);
        return true;

      case AHCSConstants.AHCS_SHUTDOWN_TASK:
        finalization();
        return true;
    }

    // otherwise the received message is not understood

    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
              AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
              AHCSConstants.AHCS_INITIALIZE_TASK,
              AHCSConstants.AHCS_READY_TASK
            };
  }

  /** Performs the initialization operation.
   *
   * @param message the initialization message
   */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
    assert getNodeRuntime() != null : "nodeRuntime must not be null";

    if (!getRole().getNode().isTopFriendshipNode()) {
      // the bootstrap role is solely a responsiblity of the top friendship node
      sendDoNotUnderstandMessage(message);
      return;
    }

    if (message.getSenderRoleId().equals(getNodeRuntime().getTopFriendshipRole().getId())) {
      LOGGER.info("ignoring a recursive initialization task from the top friendship role");
      return;
    }

    // Bootstrap is the first role to get initialized. It creates nodes, or restores them from the KB,
    // then connects the roles without initializing them - then sends the init/ready messages to Topper which propagates
    // through its child role hierarchies.  TODO: When initializing, Topper destroys the bootstrap role.

    final NodeRuntimeImpl nodeRuntime = (NodeRuntimeImpl) getNodeRuntime();
    final Node topFriendshipNode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_TOP_LEVEL_FRIENDSHIP_AGENT);
    assert topFriendshipNode != null;

    final Node loggerNode;
    if (nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_NODE_LIFE_CYCLE_AGENT) == null) {
      // create Lifer & other nodes ...

      LOGGER.info("creating the LifeCycleManagementNode - Lifer");
      final Node liferNode = NodeAccess.createNode(
              AHCSConstants.LIFE_CYCLE_MANAGEMENT_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the GoveranceManagementNode - Governor");
      final Node governorNode = NodeAccess.createNode(
              AHCSConstants.GOVERNANCE_MANAGEMENT_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the LoggerManagementNode - TopLogger");
      final Node topLoggerNode = NodeAccess.createNode(
              AHCSConstants.LOGGER_MANAGEMENT_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the JVMLoggerManagementNode - Logger");
      loggerNode = NodeAccess.createNode(
              AHCSConstants.JVM_LOGGER_MANAGEMENT_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the UserCommunicationNode - Communicator");
      final Node userCommunicatorNode = NodeAccess.createNode(
              AHCSConstants.USER_COMMUNICATION_NODE_TYPE, // nodeTypeName
              AHCSConstants.NODE_NICKNAME_USER_COMMUNICATION_AGENT, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the DialogSessionNode - Dialoger");
      final Node dialogSessionNode = NodeAccess.createNode(
              AHCSConstants.DIALOG_SESSION_NODE_TYPE, // nodeTypeName
              AHCSConstants.NODE_NICKNAME_DIALOG_SESSION_AGENT, // nodeNickname
              getRole().getNodeRuntime());

      final Role userCommunicationRole =
              userCommunicatorNode.getRoleForTypeName(AHCSConstants.USER_COMMUNICATION_ROLE_TYPE);
      assert userCommunicationRole != null : userCommunicatorNode.getRoles();
      final Role dialogSessionRole =
              dialogSessionNode.getRoleForTypeName(AHCSConstants.DIALOG_SESSION_ROLE_TYPE);
      assert dialogSessionRole != null : dialogSessionNode.getRoles();

      LOGGER.info("creating the NettyWebChatSessionNode - WebChatter");
      final Node nettyWebChatSessionNode = NodeAccess.createNode(
              AHCSConstants.NETTY_WEB_CHAT_SESSION_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      final Role nettyWebChatSessionRole =
              nettyWebChatSessionNode.getRoleForTypeName(AHCSConstants.NETTY_WEB_CHAT_SESSION_ROLE_TYPE);
      assert nettyWebChatSessionRole != null : nettyWebChatSessionNode.getRoles();
      dialogSessionRole.setRoleStateValue(
              AHCSConstants.VAR_CHAT_SESSION_ROLE_ID, // variableName
              nettyWebChatSessionRole.getId()); // value
      dialogSessionRole.setRoleStateValue(
              AHCSConstants.VAR_CHAT_SESSION_SERVICE, // variableName
              "org.texai.skill.chat.NettyWebChatSession"); // value

      LOGGER.info("creating the face recognition node - FaceRecognizer");
      final Node faceRecognitionNode = NodeAccess.createNode(
              AHCSConstants.FACE_RECOGNITION_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the face detection node - FaceDetector");
      final Node faceDetectionNode = NodeAccess.createNode(
              AHCSConstants.FACE_DETECTION_NODE_TYPE, // nodeTypeName
              null, // nodeNickname
              getRole().getNodeRuntime());

      LOGGER.info("creating the KnowledgeAcquisitionManagementNode - TopKA");
      final Node topKANode = NodeAccess.createNode(
              AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_NODE_TYPE, // nodeTypeName
              AHCSConstants.NODE_NICKNAME_TOP_LEVEL_KNOWLEDGE_ACQUISITION_AGENT, // nodeNickname
              getRole().getNodeRuntime());

      // connect bootstrap role to parent role
      NodeAccess.connectChildRoleToParent(
              topFriendshipNode.getRoleForTypeName(AHCSConstants.BOOTSTRAP_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      // connect top friendship node universal roles to parent roles
      NodeAccess.connectUniversalRoles(
              topFriendshipNode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect life cycle management node universal roles to parent roles
      NodeAccess.connectUniversalRoles(
              liferNode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect life cycle management to parent roles
      NodeAccess.connectChildRoleToParent(
              liferNode.getRoleForTypeName(AHCSConstants.LIFE_CYCLE_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      // connect governance management node universal roles to parent role
      NodeAccess.connectUniversalRoles(
              governorNode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect governance management to parent role
      NodeAccess.connectChildRoleToParent(
              governorNode.getRoleForTypeName(AHCSConstants.GOVERNANCE_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      // connect logger management node universal roles to parent role
      NodeAccess.connectUniversalRoles(
              topLoggerNode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect logger management to parent role
      NodeAccess.connectChildRoleToParent(
              topLoggerNode.getRoleForTypeName(AHCSConstants.LOGGER_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      // connect JVM logger management node universal roles to parent role
      NodeAccess.connectUniversalRoles(
              loggerNode, // childNode
              topLoggerNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect JVM logger management to parent role
      NodeAccess.connectChildRoleToParent(
              loggerNode.getRoleForTypeName(AHCSConstants.JVM_LOGGER_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
              topLoggerNode.getRoleForTypeName(AHCSConstants.LOGGER_MANAGEMENT_ROLE_TYPE).getId(), // parentRoleId
              nodeRuntime);

      // connect user communication node universal roles to parent roles
      NodeAccess.connectUniversalRoles(
              userCommunicatorNode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect user communication to parent role
      NodeAccess.connectChildRoleToParent(
              userCommunicatorNode.getRoleForTypeName(AHCSConstants.USER_COMMUNICATION_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      // connect dialog session node roles to parent universal roles
      NodeAccess.connectUniversalRoles(
              dialogSessionNode, // childNode
              userCommunicatorNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect dialog session to parent role
      NodeAccess.connectChildRoleToParent(
              dialogSessionNode.getRoleForTypeName(AHCSConstants.DIALOG_SESSION_ROLE_TYPE).getId(), // childRoleId
              userCommunicatorNode.getRoleForTypeName(AHCSConstants.USER_COMMUNICATION_ROLE_TYPE).getId(), // parentRoleId
              nodeRuntime);

      // connect Netty web chat session node universal roles to parent roles
      NodeAccess.connectUniversalRoles(
              nettyWebChatSessionNode, // childNode
              dialogSessionNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect Netty web chat session to parent role
      NodeAccess.connectChildRoleToParent(
              nettyWebChatSessionNode.getRoleForTypeName(AHCSConstants.NETTY_WEB_CHAT_SESSION_ROLE_TYPE).getId(), // childRoleId
              dialogSessionNode.getRoleForTypeName(AHCSConstants.DIALOG_SESSION_ROLE_TYPE).getId(), // parentRoleId
              nodeRuntime);

      // connect face recognition node roles to parent universal roles
      NodeAccess.connectUniversalRoles(
              faceRecognitionNode, // childNode
              userCommunicatorNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect face recognition to parent role
      NodeAccess.connectChildRoleToParent(
              faceRecognitionNode.getRoleForTypeName(AHCSConstants.FACE_RECOGNITION_ROLE_TYPE).getId(), // childRoleId
              userCommunicatorNode.getRoleForTypeName(AHCSConstants.USER_COMMUNICATION_ROLE_TYPE).getId(), // parentRoleId
              nodeRuntime);

      // connect face detection node roles to parent universal roles
      NodeAccess.connectUniversalRoles(
              faceDetectionNode, // childNode
              faceRecognitionNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect face detection to parent role
      NodeAccess.connectChildRoleToParent(
              faceDetectionNode.getRoleForTypeName(AHCSConstants.FACE_DETECTION_ROLE_TYPE).getId(), // childRoleId
              faceRecognitionNode.getRoleForTypeName(AHCSConstants.FACE_RECOGNITION_ROLE_TYPE).getId(), // parentRoleId
              nodeRuntime);

      // connect knowledge acquisition management node universal roles to parent role
      NodeAccess.connectUniversalRoles(
              topKANode, // childNode
              topFriendshipNode, // parentHeartbeatNode
              liferNode,
              governorNode,
              loggerNode,
              nodeRuntime);

      // connect knowledge acquisition management to parent role
      NodeAccess.connectChildRoleToParent(
              topKANode.getRoleForTypeName(AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
              nodeRuntime);

      LOGGER.info("persisting the new node runtime configuration");
      nodeRuntime.persistNodeRuntimeConfiguration();
    } else {
      // the nodes have already been loaded with the node runtime configuration object
      loggerNode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_LEVEL_1_LOG_MANAGEMENT_AGENT);
      assert loggerNode != null;

      oneTimeInstallation(
              nodeRuntime,
              topFriendshipNode,
              loggerNode);
    }

    // initialize the top friendship role
    sendMessage(Message.forward(
            message,
            getRole().getNode().getNodeRuntime().getTopFriendshipRole().getId(), // recipientRoleId
            TopFriendship.class.getName())); // service

    // set initial logging levels
//    setLoggingLevel("org.jboss.netty.example.http.websocketx.sslserver.WebSocketSslServerHandler", loggerNode, "debug");
//    setLoggingLevel("org.jboss.netty.example.http.websocketx.server.WebSocketServerHandler", loggerNode, "debug");
//    setLoggingLevel("org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker00", loggerNode, "debug");
//    setLoggingLevel("org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker08", loggerNode, "debug");
//    setLoggingLevel("org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker13", loggerNode, "debug");
//    setLoggingLevel("org.jboss.netty.handler.codec.http.websocketx.WebSocket08FrameDecoder", loggerNode, "debug");
    setLoggingLevel("org.texai.network.netty.handler.HTTPRequestHandler", loggerNode, "warn");
    setLoggingLevel("org.texai.network.netty.handler.PortUnificationHandler", loggerNode, "warn");
    setLoggingLevel("org.texai.network.netty.handler.WebSocketSslServerHandler", loggerNode, "warn");
    setLoggingLevel("org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory", loggerNode, "warn");
    setLoggingLevel("org.texai.skill.chat.NettyWebChatSession", loggerNode, "debug");
    setLoggingLevel("org.texai.skill.dialog.conversation.RoleEditor", loggerNode, "debug");
    setLoggingLevel("org.texai.skill.dialog.DialogSessionSkill", loggerNode, "info");
    setLoggingLevel("org.texai.skill.lifecycle.LifeCycleManagement", loggerNode, "debug");
    setLoggingLevel("org.texai.ssl.TexaiSSLContextFactory", loggerNode, "warn");
    setLoggingLevel("org.texai.webserver.ChatServer", loggerNode, "warn");
  }

  /** Performs one time installation of certain nodes.
   *
   * @param nodeRuntime the node runtime
   * @param topFriendshipNode the top friendship node
   * @param loggerNode the logger node
   */
  private void oneTimeInstallation(
          final NodeRuntimeImpl nodeRuntime,
          final Node topFriendshipNode,
          final Node loggerNode) {


    //TODO rewrite for dynamic creation of new node/role types
    // decide about updating xml type definition files


//    Node topKANode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_TOP_KA);
//    if (topKANode == null) {
//
//      LOGGER.info("******************************************************************");
//      LOGGER.info("* one time installation                                           *");
//      LOGGER.info("******************************************************************");
//
//      // load the repositories with the node and role types
//      DistributedRepositoryManager.clearNamedRepository("NodeRoleTypes");
//      LOGGER.info("reloading node types and role types");
//      final RoleTypeInitializer roleTypeInitializer = new RoleTypeInitializer();
//      roleTypeInitializer.initialize(getRDFEntityManager());
//      if ((new File("data/role-types.xml")).exists()) {
//        roleTypeInitializer.process("data/role-types.xml");
//      } else {
//        roleTypeInitializer.process("../Main/data/role-types.xml");
//      }
//      roleTypeInitializer.finalization();
//      final NodeTypeInitializer nodeTypeInitializer = new NodeTypeInitializer();
//      nodeTypeInitializer.initialize(getRDFEntityManager());
//      if ((new File("data/node-types.xml")).exists()) {
//        nodeTypeInitializer.process("data/node-types.xml");
//      } else {
//        nodeTypeInitializer.process("../Main/data/node-types.xml");
//      }
//      nodeTypeInitializer.finalization();
//
//      // define knowledge acquisition management skill use, role type and node type
//      final SkillUse knowledgeAcquisitionManagementSkillUse = nodeAccess.createSkillUse(
//              KnowledgeAcquisitionManagement.class.getName(), // skillClassName
//              getNodeRuntime().getNodeAccess());
//
//      Collection<RoleType> inheritedRoleTypes = Collections.emptyList();
//      Collection<SkillUse> skillUses = new ArrayList<>();
//      skillUses.add(knowledgeAcquisitionManagementSkillUse);
//
//      final RoleType knowledgeAcquisitionManagementRoleType = nodeAccess.createRoleType(
//              AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_ROLE_TYPE, // typeName
//              inheritedRoleTypes,
//              skillUses,
//              "Manages the knowlege acquisition role hierarchy.", // description
//              null, // semanticConstituentNode
//              null, // albusHCSGranularityLevel
//              getNodeRuntime().getNodeAccess());
//
//      Collection<NodeType> inheritedNodeTypes = new ArrayList<>();
//      final NodeType universalNodeType = getNodeRuntime().getNodeAccess().findNodeType(AHCSConstants.UNIVERSAL_NODE_TYPE);
//      assert universalNodeType != null;
//      inheritedNodeTypes.add(universalNodeType);
//      Collection<RoleType> roleTypes = new ArrayList<>();
//      roleTypes.add(knowledgeAcquisitionManagementRoleType);
//
//      nodeAccess.createNodeType(
//              AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_NODE_TYPE, // typeName
//              inheritedNodeTypes,
//              roleTypes,
//              "Manages the knowlege acquisition role hierarchy.", // missionDescription
//              null, // semanticConstituentNode
//              getNodeRuntime().getNodeAccess());
//
//      final Node liferNode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_LIFER);
//      assert liferNode != null;
//      final Node governorNode = nodeRuntime.getNode(AHCSConstants.NODE_NICKNAME_GOVERNOR);
//      assert governorNode != null;
//
//      LOGGER.info(nodeAccess.displayChildRoles(
//              governorNode.getRoleForTypeName(AHCSConstants.GOVERNANCE_MANAGEMENT_ROLE_TYPE),
//              nodeRuntime));
//
//      LOGGER.info("creating the KnowledgeAcquisitionManagementNode - TopKA");
//      topKANode = nodeAccess.createNode(
//              AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_NODE_TYPE, // nodeTypeName
//              AHCSConstants.NODE_NICKNAME_TOP_KA, // nodeNickname
//              getRole().getNodeRuntime());
//
//      LOGGER.info("topFriendshipNode: " + topFriendshipNode);
//      LOGGER.info("loggerNode: " + loggerNode);
//      LOGGER.info("liferNode: " + liferNode);
//      LOGGER.info("governorNode: " + governorNode);
//      LOGGER.info(nodeAccess.displayChildRoles(
//              governorNode.getRoleForTypeName(AHCSConstants.GOVERNANCE_MANAGEMENT_ROLE_TYPE),
//              nodeRuntime));
//
//      // connect knowledge acquisition management node universal roles to parent role
//      nodeAccess.connectUniversalRoles(
//              topKANode, // childNode
//              topFriendshipNode, // parentHeartbeatNode
//              liferNode,
//              governorNode,
//              loggerNode,
//              nodeRuntime);
//
//      // connect knowledge acquisition management to parent role
//      nodeAccess.connectChildRoleToParent(
//              topKANode.getRoleForTypeName(AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_ROLE_TYPE).getId(), // childRoleId
//              nodeRuntime.getTopFriendshipRole().getId(), // parentRoleId
//              nodeRuntime);
//
//      // define meta fact acquisition skill use, role type and node type
//      final SkillUse metaFactAcquisitionSkillUse = nodeAccess.createSkillUse(
//              MetaFactAcquisition.class.getName(), // skillClassName
//              getNodeRuntime().getNodeAccess());
//
//      inheritedRoleTypes = Collections.emptyList();
//      skillUses = new ArrayList<>();
//      skillUses.add(metaFactAcquisitionSkillUse);
//
//      final RoleType metaFactAcquisitionRoleType = nodeAccess.createRoleType(
//              AHCSConstants.META_FACT_ACQUISITION_ROLE_TYPE, // typeName
//              inheritedRoleTypes,
//              skillUses,
//              "Provides a knowledge acquisition skill that populates fact acquisition scripts.", // description
//              null, // semanticConstituentNode
//              null, // albusHCSGranularityLevel
//              getNodeRuntime().getNodeAccess());
//
//      inheritedNodeTypes = new ArrayList<>();
//      assert universalNodeType != null;
//      inheritedNodeTypes.add(universalNodeType);
//      roleTypes = new ArrayList<>();
//      roleTypes.add(metaFactAcquisitionRoleType);
//
//      nodeAccess.createNodeType(
//              AHCSConstants.META_FACT_ACQUISITION_NODE_TYPE, // typeName
//              inheritedNodeTypes,
//              roleTypes,
//              "Provides a knowledge acquisition skill that populates fact acquisition scripts.", // missionDescription
//              null, // semanticConstituentNode
//              getNodeRuntime().getNodeAccess());
//
//      LOGGER.info("creating the MetaFactAcquisitionNode - FactScripter");
//      final Node factScripterNode = nodeAccess.createNode(
//              AHCSConstants.META_FACT_ACQUISITION_NODE_TYPE, // nodeTypeName
//              AHCSConstants.NODE_NICKNAME_FACT_SCRIPTER, // nodeNickname
//              getRole().getNodeRuntime());
//
//      // connect meta fact acquisition node universal roles to parent role
//      nodeAccess.connectUniversalRoles(
//              factScripterNode, // childNode
//              topKANode, // parentHeartbeatNode
//              liferNode,
//              governorNode,
//              loggerNode,
//              nodeRuntime);
//
//      // connect meta fact acquisition node to parent role
//      nodeAccess.connectChildRoleToParent(
//              factScripterNode.getRoleForTypeName(AHCSConstants.META_FACT_ACQUISITION_ROLE_TYPE).getId(), // childRoleId
//              topKANode.getRoleForTypeName(AHCSConstants.KNOWLEDGE_ACQUISITION_MANAGEMENT_ROLE_TYPE).getId(), // parentRoleId
//              nodeRuntime);
//
//      LOGGER.info("persisting the updated node runtime configuration");
//      nodeRuntime.persistNodeRuntimeConfiguration();
//    }
  }

  /** Sets the given class logging level to debug.
   *
   * @param className the class name
   * @param loggerNode the logger node
   */
  private void setLoggingLevel(final String className, final Node loggerNode, final String level) {
    //Preconditions
    assert StringUtils.isNonEmptyString(className) : "className must be a non-empty string";
    assert loggerNode != null : "loggerNode must not be null";
    assert StringUtils.isNonEmptyString(level) : "level must be a non-empty string";

    final Message message = new Message(
            getRoleId(), // senderRoleId
            getClassName(), // senderService
            loggerNode.getRoleForTypeName(AHCSConstants.JVM_LOGGER_MANAGEMENT_ROLE_TYPE).getId(), // recipientRoleId
            JVMLoggerManagement.class.getName(), // service
            AHCSConstants.SET_LOGGING_LEVEL); // operation
    message.put(
            AHCSConstants.MSG_PARM_CLASS_NAME, // parameterName
            className); // parameterValue
    message.put(
            AHCSConstants.MSG_PARM_LOGGING_LEVEL, // parameterName
            level); // parameterValue
    sendMessage(message);
  }

  /** Performs the finalization operation. */
  private void finalization() {
  }

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    return notUnderstoodMessage(message);
  }
}
