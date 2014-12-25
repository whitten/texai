/*
 * AHCSConstants.java
 *
 * Created on Jun 26, 2009, 2:32:17 PM
 *
 * Description: Provides constants for the Albus hierarchical control system.
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

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;

/**
 * Provides constants for the Albus hierarchical control system.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class AHCSConstants {

  // the role and skill states
  public enum State {

    UNINITIALIZED,
    ISOLATED_FROM_NETWORK,
    READY,
    INACTIVE
  }

  //
  //
  // sensation messages - end with _Sensation, and are sent from a child node to its parent node
  //
  // the exitApplication_Sensation operation
  public static final String AHCS_EXIT_APPLICATION_SENSATION = "AHCS exitApplication_Sensation";
  //
  // the detected face_Sensation operation
  public static final String DETECTED_FACE_SENSATION = "detected face_Sensation";
  //
  // the disconnectedSession_Sensation operation
  public static final String DISCONNECTED_SESSION_SENSATION = "disconnectedSession_Sensation";
  //
  // the networkJoinComplete_Sensation operation
  public static final String NETWORK_JOIN_COMPLETE_SENSATION = "networkJoinComplete_Sensation";
  //
  // the recognizedFace_Sensation operation
  public static final String RECOGNIZED_FACE_SENSATION = "recognizedFace_Sensation";
  //
  // the sensedWord_Sensation operation
  public static final String SENSED_WORD_SENSATION = "sensedWord_Sensation";
  //
  // the sensedWord parameter of the sensedWord_Sensation
  public static final String SENSED_WORD_SENSATION_SENSED_WORD = "sensedWord_Sensation_sensedWord";
  //
  // the unrecognizedFace_Sensation operation
  public static final String UNRECOGNIZED_FACE_SENSATION = "unrecognizedFace_Sensation";
  //
  // the utteranceMeaning_Sensation operation
  public static final String UTTERANCE_MEANING_SENSATION = "utteranceMeaning_Sensation";
  // the meaningStatements parameter of the the utteranceMeaning_Sensation
  public static final String UTTERANCE_MEANING_SENSATION_MEANING_STATEMENTS = "utteranceMeaning_Sensation_meaningStatements";
  // the discourseContext parameter of the the utteranceMeaning_Sensation
  public static final String UTTERANCE_MEANING_SENSATION_DISCOURSE_CONTEXT = "utteranceMeaning_Sensation_discourseContext";
  // the utterance parameter of the the utteranceMeaning_Sensation
  public static final String UTTERANCE_MEANING_SENSATION_UTTERANCE = "utteranceMeaning_Sensation_utterance";
  // the receiverNodeNickname parameter of the the utteranceMeaning_Sensation
  public static final String UTTERANCE_MEANING_SENSATION_RECEIVER_NODE_NICKNAME = "utteranceMeaning_Sensation_receiverNodeNickname";
  // the effectiveUtterance parameter of the the utteranceMeaning_Sensation
  public static final String UTTERANCE_MEANING_SENSATION_EFFECTIVE_UTTERANCE = "utteranceMeaning_Sensation_effectiveUtterance";
  //
  // the webcam_image_Sensation operation
  public static final String WEBCAM_IMAGE_SENSATION = "webcamImage_Sensation";
  //
  // the webcamPresent_Sensation operation
  public static final String WEBCAM_PRESENT_SENSATION = "webcamPresent_Sensation";
  //
  //
  // task messages - end with _Task, and are sent from the parent node to one of its child nodes
  //
  // the addUnrecognizedFaceToTrainingSet_Task operation
  public static final String ADD_UNRECOGNIZED_FACE_TO_TRAINING_SET_TASK = "addUnrecognizedFaceToTrainingSet_Task";
  //
  // the initialize_Task operation
  public static final String AHCS_INITIALIZE_TASK = "AHCS initialize_Task";
  //
  // the quiesce_Task operation
  public static final String AHCS_QUIESCE_TASK = "AHCS quiesce_Task";
  //
  // the restart_Task operation
  public static final String AHCS_RESTART_TASK = "AHCS restart_Task";
  // the node runtime id
  public static final String AHCS_RESTART_TASK_NODE_RUNTIME_ID = "AHCS restart_Task_nodeRuntimeId";
  // the indicator whether repositories are backed up
  public static final String AHCS_RESTART_TASK_ARE_REPOSITORIES_BACKED_UP = "AHCS restart_Task_areRepositoriesBackedUp";
  // the indicator whether remote debugging is enabled
  public static final String AHCS_RESTART_TASK_IS_REMOTE_DEBUGGING_ENABLED = "AHCS restart_Task_isRemoteDebuggingEnabled";
  //
  // the shutdown_Task operation
  public static final String AHCS_SHUTDOWN_TASK = "AHCS shutdown_Task";
  //
  // the becomeReady_Task operation
  public static final String BECOME_READY_TASK = "becomeReady_Task";
  //
  // the delegateConfigureSingletonAgentHosts_Task operation
  public static final String CONFIGURE_SINGLETON_AGENT_HOSTS_TASK = "configureSingletonAgentHosts_Task";
  //
  // the connect child role to parent_Task operation
  public static final String CONNECT_CHILD_ROLE_TO_PARENT_TASK = "connect child role to parent_Task";
  //
  // the converseYesNoQueryWithUser_Task operation
  public static final String CONVERSE_YES_NO_QUERY_WITH_USER_TASK = "converseYesNoQueryWithUser_Task";
  //
  // the create node_Task operation
  public static final String CREATE_NODE_TASK = "create node_Task";
  //
  // the defineNewNode_Task operation
  public static final String DEFINE_NEW_NODE_TASK = "defineNewNode_Task";
  //
  // the defineNewSimpleNode_Task operation
  public static final String DEFINE_NEW_SIMPLE_NODE_TASK = "defineNewSimpleNode_Task";
  //
  // the defineNewRoleType_Task operation
  public static final String DEFINE_NEW_SKILL_CLASS_TASK = "defineNewSkillClass_Task";
  //
  // the delegateBecomeReady_Task operation
  public static final String DELEGATE_BECOME_READY_TASK = "delegateBecomeReady_Task";
  //
  // the configureSingletonAgentHosts_Task operation
  public static final String DELEGATE_CONFIGURE_SINGLETON_AGENT_HOSTS_TASK = "delegateConfigureSingletonAgentHosts_Task";
  //
  // the delegatePerformMission_Task operation
  public static final String DELEGATE_PERFORM_MISSION_TASK = "delegatePerformMission_Task";
  //
  // the describe nodes_Task operation
  public static final String DESCRIBE_NODES_TASK = "describe nodes_Task";
  //
  // the directNextUtteranceBackToSender_Task operation
  public static final String DIRECT_NEXT_UTTERANCE_BACK_TO_SENDER_TASK = "directNextUtteranceBackToSender_Task";
  // the senderSkill parameter
  public static final String SENDER_SKILL = "senderSkill";
  //
  // the generate_Task operation
  public static final String GENERATE_FROM_CONSTITUENTS_TASK = "generateFromConstitutents_Task";
  // the rootSemanticConstituentNode user parameter of the generate_Task
  public static final String GENERATE_FROM_CONSTITUENTS_TASK_ROOT_SEMANTIC_CONSTITUENT_NODE = "generateFromConstitutents_Task_rootSemanticConstituentNode";
  //
  // the greeting_Task operation
  public static final String GREETING_TASK = "greeting_Task";
  //
  // the interpret fact acquisition script_Task operation
  public static final String INTERPRET_FACT_ACQUISITION_SCRIPT_TASK = "interpret fact acquisition script_Task";
  // the script name
  public static final String INTERPRET_FACT_ACQUISITION_SCRIPT_TASK_NAME = "interpret fact acquisition script_Task_name";
  //
  // the joinAcknowledged_Task operation
  public static final String JOIN_ACKNOWLEDGED_TASK = "joinAcknowledged_Task";
  //
  // the joinNetwork_Task operation
  public static final String JOIN_NETWORK_TASK = "joinNetwork_Task";
  //
  // the learn faces_Task operation
  public static final String LEARN_FACES_TASK = "learn faces_Task";
  //
  // the listenForConnections_Task operation
  public static final String LISTEN_FOR_CONNECTIONS_TASK = "listenForConnections_Task";
  //
  // the logOperation_Task operation
  public static final String LOG_OPERATION_TASK = "logOperation_Task";
  // logged operation message parameter
  public static final String LOG_OPERATION_TASK_LOGGED_OPERATION = "logOperation_Task_loggedOperation";
  //
  // the logUserWord_Task operation
  public static final String LOG_USER_WORD_TASK = "logUserWord_Task";
  // the logUserWord_Task_word parameter
  public static final String LOG_USER_WORD_SKILL_WORD = "logUserWord_Skill_word";
  //
  // the performMission_Task operation
  public static final String PERFORM_MISSION_TASK = "performMission_Task";
  // the registerSensedUtterance_Task operation
  public static final String REGISTER_SENSED_UTTERANCE_PROCESSOR_TASK = "registerSensedUtterance_Task";
  // the understoodUtteranceTemplate message parameter
  public static final String REGISTER_SENSED_UTTERANCE_PROCESSOR_TASK_UNDERSTOOD_UTTERANCE_TEMPLATE = "registerSensedUtterance_Task_understoodUtteranceTemplate";
  //
  // the removeRoleType_Task operation
  public static final String REMOVE_ROLE_TYPE_TASK = "removeRoleType_Task";
  //
  //
  // the removeRoleType_Task operation
  public static final String REMOVE_SKILL_CLASS_TASK = "removeSkillClass_Task";
  //
  // the sendGreetingToUser_Task operation
  public static final String SEND_GREETING_TO_USER_TASK = "sendGreetingToUser_Task";
  //
  // the sendIDoNotUnderstandThatToUser_Task operation
  public static final String SEND_DO_NOT_UNDERSTAND_TO_USER_TASK = "sendIDoNotUnderstandThatToUser_Task";
  //
  // the sendInterjectionToUser_Task operation
  public static final String SEND_INTERJECTION_TO_USER_TASK = "sendInterjectionToUser_Task";
  // the statements parameter of the sendInterjectionToUser_Task
  public static final String SEND_INTERJECTION_TO_USER_TASK_STATEMENTS = "sendInterjectionToUser_Task_statements";
  //
  // the sendOKToUser_Task operation
  public static final String SEND_OK_TO_USER_TASK = "sendOKToUser_Task";
  //
  // the sendSpeechActToUser_Task operation
  public static final String SEND_SPEECH_ACT_TO_USER_TASK = "sendSpeechActToUser_Task";
  //
  // the sendTextToUser_Task operation
  public static final String SEND_TEXT_TO_USER_TASK = "sendTextToUser_Task";
  //
  // the set logging level operation
  public static final String SET_LOGGING_LEVEL = "set_logging_level_Task";
  //
  // the shutdown node runtime operation
  public static final String SHUTDOWN_NODE_RUNTIME = "shutdown_node_runtime_Task";
  //
  // the take_the_dialog_initiative_Task operation
  public static final String TAKE_THE_DIALOG_INITIATIVE_TASK = "take_the_dialog_initiative_Task";
  //
  // the unlogOperation_Task operation
  public static final String UNLOG_OPERATION_TASK = "unlogOperation_Task";
  // unlogged operation message parameter
  public static final String UNLOG_OPERATION_TASK_UNLOGGED_OPERATION = "unlogOperation_Task_unloggedOperation";
  //
  // the verifyNodesRolesAndSkills_Task operation
  public static final String VERIFY_NODES_ROLES_AND_SKILLS_TASK = "verifyNodesRolesAndSkills_Task";
  //
  // visuallyRecognizeUser_Task operation
  public static final String VISUALLY_RECOGNIZE_USER_TASK = "visuallyRecognizeUser_Task";
  //
  // the want_the_dialog_initiative_Task operation
  public static final String WANT_THE_DIALOG_INITIATIVE_TASK = "want_the_dialog_initiative_Task";
  //
  //
  // the writeConfigurationFile_Task operation
  public static final String WRITE_CONFIGURATION_FILE_TASK = "writeConfigurationFile_Task";
  // the configuration file directory path parameter
  public static final String WRITE_CONFIGURATION_FILE_TASK_DIRECTORY_PATH = "writeConfigurationFile_Task_directoryPath";
  //
  // the writeFormFillTextToUser_Task operation
  public static final String WRITE_FORM_FILL_TEXT_TO_USER_TASK = "writeFormFillTextToUser_Task";
  // the text parameter of the the writeFormFillTextToUser_Task
  public static final String WRITE_FORM_FILL_TEXT_TO_USER_TASK_TEXT = "writeFormFillTextToUser_Task_text";
  //
  // the writeSpokenTextToUser_Task operation
  public static final String WRITE_SPOKEN_TEXT_TO_USER_TASK = "writeSpokenTextToUser_Task";
  // the text parameter of the the writeSpokenTextToUser_Task
  public static final String WRITE_SPOKEN_TEXT_TO_USER_TASK_TEXT = "writeSpokenTextToUser_Task_text";
  //
  // the writeWordToUser_Task operation
  public static final String WRITE_WORD_TO_USER_TASK = "writeWordToUser_Task";
  // the word parameter of the writeWordToUser_Task
  public static final String WRITE_WORD_TO_USER_TASK_WORD = "writeWordToUser_Task_word";
  //
  //
  // information messages - end with _Info
  //
  // the addUnjoinedRole_Info operation
  public static final String ADD_UNJOINED_ROLE_INFO = "addUnjoinedRole_Info";
  //
  // the acknowledged_Info operation
  public static final String ACKNOWLEDGED_INFO = "acknowledged_Info";
  //
  // the joinNetworkSingleton_Info operation
  public static final String JOIN_NETWORK_SINGLETON_AGENT_INFO = "joinNetworkSingletonAgent_Info";
  //
  // the keep-alive information operation
  public static final String KEEP_ALIVE_INFO = "keepAlive_Info";
  //
  // the task accomplished information operation
  public static final String TASK_ACCOMPLISHED_INFO = "taskAccomplished_Info";
  //
  // the message timeout information operation
  public static final String MESSAGE_TIMEOUT_INFO = "messageTimeout_Info";
  //
  // the message timeout error information operation
  public static final String MESSAGE_TIMEOUT_ERROR_INFO = "messageTimeoutError_Info";
  //
  // the messageNotUnderstood_Info operation
  public static final String MESSAGE_NOT_UNDERSTOOD_INFO = "messageNotUnderstood_Info";
  //
  // the networkJoinComplete_Info operation
  public static final String NETWORK_JOIN_COMPLETE_INFO = "networkJoinComplete_Info";
  //
  // the operationNotPermitted_Info operation
  public static final String OPERATION_NOT_PERMITTED_INFO = "operationNotPermitted_Info";
  //
  // the removeUnjoinedRole_Info operation
  public static final String REMOVE_UNJOINED_ROLE_INFO = "removeUnjoinedRole_Info";
  //
  // the returnFromConverseYesNoQueryWithUser_Info operation
  public static final String RETURN_FROM_CONVERSE_YES_NO_QUERY_WITH_USER_INFO = "returnFromConverseYesNoQueryWithUser_Info";
  public static final String RETURN_FROM_CONVERSE_YES_NO_QUERY_WITH_USER_INFO_RESPONSE = "returnFromConverseYesNoQueryWithUser_Info_response";
  //
  // the seedConnectionRequest_Info operation
  public static final String SEED_CONNECTION_REQUEST_INFO = "seedConnectionRequest_Info";
  // the port parameter of the seedConnectionRequest_Info
  public static final String SEED_CONNECTION_REQUEST_INFO_PORT = "seedConnectionRequest_Info_Port";
  //
  // the singletonAgentHosts_Info operation
  public static final String SINGLETON_AGENT_HOSTS_INFO = "singletonAgentHosts_Info";

  //
  // shared message parameters
  //
  // the AHCS originalMessage parameter
  public static final String AHCS_ORIGINAL_MESSAGE = "AHCS originalMessage";
  // the message parameter naming the session value
  public static final String SESSION = "AHCS session";
  // the message parameter for a X.509 certificate
  public static final String MSG_PARM_X509_CERTIFICATE = "X.509 Certificate";
  // the message parameter for the child role ID
  public static final String MSG_PARM_CHILD_ROLE_ID = "child role id";
  // the message parameter for class name
  public static final String MSG_PARM_CLASS_NAME = "class name";
  // the message parameter for conversation ID
  public static final String MSG_PARM_CONTAINER_NAME = "containerName";
  // the message parameter for conversation ID
  public static final String MSG_PARM_CONVERSATION_ID = "conversationId";
  // the formFillText
  public static final String MSG_PARM_FORM_FILL_TEXT = "formFillText";
  // the message parameter for the host name
  public static final String MSG_PARM_HOST_NAME = "hostName";
  // the message parameter for image
  public static final String MSG_PARM_IMAGE = "image";
  // the message parameter for the IP address
  public static final String MSG_PARM_IP_ADDRESS = "ipAddress";
  // the message parameter for logging level
  public static final String MSG_PARM_LOGGING_LEVEL = "logging level";
  // the message parameter for the node type name
  public static final String MSG_PARM_OPERATION = "operation";
  // the message parameter for the role type name
  public static final String MSG_PARM_ROLE_QUALIFIED_NAME = "role qualified name";
  // the message parameter for the singleton agent hosts
  public static final String MSG_PARM_SINGLETON_AGENT_HOSTS = "singleton agent hosts";
  // the message parameter for the skill class name
  public static final String MSG_PARM_SKILL_CLASS_NAME = "skill class name";
  // the spokenText parameter
  public static final String MSG_PARM_SPOKEN_TEXT = "spokenText";
  // the registered user parameter
  public static final String MSG_PARM_REGISTERED_USER = "registeredUser";
  // the message parameter for the role id
  public static final String MSG_PARM_ROLE_ID = "role id";
  // the message parameter for service
  public static final String MSG_PARM_SERVICE = "service";
  // the text parameter
  public static final String MSG_PARM_TEXT = "text";
  //
  // the Albus Hierarchical Control System Levels
  //
  // the 2 years (24 months) granularity level
  public static final URI ALBUS_HCS_2_YEAR_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS2YearGranularityLevel");
  // the 3 months (91.31 days) granularity level
  public static final URI ALBUS_HCS_3_MONTH_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS3MonthGranularityLevel");
  // the 10 days granularity level
  public static final URI ALBUS_HCS_10_DAY_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS10DayGranularityLevel");
  // the 1 day (86,400 seconds) granularity level
  public static final URI ALBUS_HCS_1_DAY_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS1DayGranularityLevel");
  // the 10,000 seconds (2.78 hours) granularity level
  public static final URI ALBUS_HCS_10000_SECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS10000SecondGranularityLevel");
  // the 1000 seconds (16.67 minutes) granularity level
  public static final URI ALBUS_HCS_1000_SECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS1000SecondGranularityLevel");
  // the 100 seconds (1.67 minutes) granularity level
  public static final URI ALBUS_HCS_100_SECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS100SecondGranularityLevel");
  // the 10 seconds granularity level
  public static final URI ALBUS_HCS_10_SECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS10SecondGranularityLevel");
  // the 1 second granularity level
  public static final URI ALBUS_HCS_1_SECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS1SecondGranularityLevel");
  // 100 milliseconds granularity level
  public static final URI ALBUS_HCS_100_MILLISECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS100MillisecondGranularityLevel");
  // the 10 milliseconds granularity level
  public static final URI ALBUS_HCS_10_MILLISECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS10MillisecondGranularityLevel");
  // the 1 millisecond granularity level
  public static final URI ALBUS_HCS_1_MILLISECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS1MillisecondGranularityLevel");
  // the 100 microseconds granularity level
  public static final URI ALBUS_HCS_100_MICROSECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS100MicrosecondGranularityLevel");
  // the 10 microseconds granularity level
  public static final URI ALBUS_HCS_10_MICROSECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS10MicroSecondGranularityLevel");
  // the 1 microsecond granularity level
  public static final URI ALBUS_HCS_1_MICROSECOND_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "AlbusHCS1MicrosecondGranularityLevel");
  //
  // role names
  //
  // the top friendship qualified role name
  public static final String NODE_NAME_TOPMOST_FRIENDSHIP_ROLE = "TopmostFriendshipAgent.TopmostFriendshipRole";

  //
  // node or role state variables
  //
  // the chat session role id
  public static final String VAR_CHAT_SESSION_ROLE_ID = "chatSessionRoleId";
  // the chat session service
  public static final String VAR_CHAT_SESSION_SERVICE = "chatSessionService";
  // the conversation skill dictionary
  public static final String CONVERSATION_SKILL_DICTIONARY = "conversationSkillDictionary";
  // the is-unit-test
  public static final String VAR_IS_UNIT_TEST = "isUnitTest";
  //
  // Miscellaneous
  //
  // the predicate that relates the role type with the role granularity level
  public static final URI ALBUS_HCS_GRANULARITY_LEVEL = new URIImpl(Constants.TEXAI_NAMESPACE + "albusHCSGranularityLevel");
  // the texai:role_name term
  public static final URI ROLE_NAME_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "role_name");
  // the texai:skillClass_skillClassName term
  public static final URI SKILL_CLASS_SKILL_CLASS_NAME_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "skillClass_skillClassName");

  /**
   * Constructs a new AHCSConstants instance.
   */
  private AHCSConstants() {
  }

}
