package org.texai.skill.fileTransfer;

/**
 * NetworkFileTransfer.java
 *
 * Description: This is a network singleton skill that coordinates file transfers between two containers.
 *
 * Copyright (C) Jan 29, 2015, Stephen L. Reed.
 */
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.util.StringUtils;

@ThreadSafe
public final class NetworkFileTransfer extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkFileTransfer.class);
  // the file transfer dictionary, conversation id --> file transfer request info
  private final Map<UUID, FileTransferRequestInfo> fileTransferDictionary = new HashMap<>();

  /**
   * Creates a new instance of NetworkFileTransfer.
   */
  public NetworkFileTransfer() {
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   */
  @Override
  public void receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(Message.operationNotPermittedMessage(
              message, // receivedMessage
              this)); // skill
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return;

      /**
       * Join Network Singleton Agent Info
       *
       * This task message is sent to this network singleton agent/role from a child role in another container.
       *
       * The sender is requesting to join the network as child of this role.
       *
       * The message parameter is the X.509 certificate belonging to the sender agent / role.
       *
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(message);
        return;

      /**
       * Delegate Become Ready Task
       *
       * A container has completed joining the network. Propagate a Delegate Become Ready Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_BECOME_READY_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegateBecomeReadyTask(message);
        return;

      /**
       * Delegate Perform Mission Task
       *
       * A container has completed joining the network. Propagate a Delegate Perform Mission Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegatePerformMissionTask(message);
        return;

      /**
       * Transfer File Request Info
       *
       * A peer agent has requested a file transer between two containers.
       *
       * Parameters of the message are: sender file path, recipient file path, sender container name and recipient container name.
       *
       * As a result of processing this message, a Prepare To Send File Task message is first sent to the sender container's Container File
       * Transfer Role. The reply message continues the conversation.
       *
       */
      case AHCSConstants.TRANSFER_FILE_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleTransferFileRequestTask(message);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;
      // handle other operations ...
    }
    sendMessage(Message.notUnderstoodMessage(
            message, // receivedMessage
            this)); // skill
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single threaded with regard
   * to the conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // handle operations
    return Message.notUnderstoodMessage(
            message, // receivedMessage
            this); // skill
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.DELEGATE_BECOME_READY_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.TRANSFER_FILE_REQUEST_INFO
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    LOGGER.info("performing the mission");

  }

  /**
   * Handles a file transfer request task message.
   *
   * @param message file transfer request task message
   */
  private void handleTransferFileRequestTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    LOGGER.info("handling a file transfer request");
    final UUID conversationId = message.getConversationId();
    final String senderFilePath = (String) message.get(AHCSConstants.MSG_PARM_SENDER_FILE_PATH);
    final String recipientFilePath = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH);
    final String senderContainerName = (String) message.get(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME);
    final String recipientContainerName = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME);

    // record the file transfer information for use with subsequent messages in the conversation
    fileTransferDictionary.put(
            conversationId,
            new FileTransferRequestInfo(
                    conversationId,
                    senderFilePath,
                    recipientFilePath,
                    senderContainerName,
                    recipientContainerName));

    // continue the file transfer conversation by preparing the sending container to send the file
    final Message prepareToSendFileTaskMessage = makeMessage(
            recipientContainerName + ".ContainerFileSenderRole" , // recipientQualifiedName
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_SEND_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, senderFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, recipientContainerName);

    sendMessage(prepareToSendFileTaskMessage);
  }

  /** Returns the file transfer information given its corresponding conversation id. Intended for unit testing.
   *
   * @param conversationId the conversation id
   * @return the file transfer information
   */
  protected FileTransferRequestInfo getFileTransferRequestInfo(final UUID conversationId) {
    return fileTransferDictionary.get(conversationId);
  }

  /**
   * Provides a container for a file transfer request.
   */
  final static class FileTransferRequestInfo {

    // the file transfer conversation id
    private final UUID conversationId;
    // the sender file path
    private final String senderFilePath;
    // the recipient file path
    private final String recipientFilePath;
    // the sender container name
    private final String senderContainerName;
    // the recipient container name
    private final String recipientContainerName;
    // the starting date time of the file transfer
    private final DateTime startingDateTime = new DateTime();

    /**
     * Creates a new FileTransferRequestInfo instance.
     *
     * @param conversationId the file transfer conversation id
     * @param senderFilePath the sender file path
     * @param recipientFilePath the recipient file path
     * @param senderContainerName the sender container name
     * @param recipientContainerName the starting date time of the file transfer
     */
    FileTransferRequestInfo(
            final UUID conversationId,
            final String senderFilePath,
            final String recipientFilePath,
            final String senderContainerName,
            final String recipientContainerName) {
      //Preconditions
      assert conversationId != null : "conversationId must not be null";
      assert StringUtils.isNonEmptyString(senderFilePath) : "senderFilePath must be a non-empty string";
      assert StringUtils.isNonEmptyString(recipientFilePath) : "recipientFilePath must be a non-empty string";
      assert StringUtils.isNonEmptyString(senderContainerName) : "senderContainerName must be a non-empty string";
      assert StringUtils.isNonEmptyString(recipientContainerName) : "recipientContainerName must be a non-empty string";

      this.conversationId = conversationId;
      this.senderFilePath = senderFilePath;
      this.recipientFilePath = recipientFilePath;
      this.senderContainerName = senderContainerName;
      this.recipientContainerName = recipientContainerName;
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return (new StringBuilder())
              .append('[')
              .append(senderContainerName)
              .append(':')
              .append(senderFilePath)
              .append(" --> ")
              .append(recipientContainerName)
              .append(':')
              .append(recipientFilePath)
              .append(' ')
              .append(conversationId)
              .append(']')
              .toString();
    }

  }

}
