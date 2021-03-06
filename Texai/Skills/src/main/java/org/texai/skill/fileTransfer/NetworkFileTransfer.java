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
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.skill.fileTransfer.FileTransferInfo.FileTransferState;

@ThreadSafe
public final class NetworkFileTransfer extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkFileTransfer.class);
  // the file transfer dictionary, conversation id --> file transfer request info
  private final Map<UUID, FileTransferInfo> fileTransferDictionary = new HashMap<>();
  // the indicator to clean up the file transfer dictionary after a completed file transfer
  private boolean isFileTransferDictionaryCleaned = true;

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
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = receivedMessage.getOperation();
    if (!isOperationPermitted(receivedMessage)) {
      sendOperationNotPermittedInfoMessage(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        propagateOperationToChildRoles(receivedMessage);
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
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(receivedMessage);
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
        joinNetworkSingletonAgent(receivedMessage);
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
        handleDelegatePerformMissionTask(receivedMessage);
        return;

      /**
       * Transfer File Request Info
       *
       * A peer agent has requested a file transer between two containers.
       *
       * Parameters of the message are: sender file path, recipient file path, sender container name and recipient container name.
       *
       * As a result of processing this message, a Prepare To Send File Task message is first sent to the sender container's Container File
       * Sender Role. The reply message continues the conversation.
       *
       */
      case AHCSConstants.TRANSFER_FILE_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleTransferFileRequestTask(receivedMessage);
        return;

      /**
       * Task Accomplished Info
       *
       * This message is received in three situations. (1) A child ContainerTransferAgent.ContainerSenderRole replies that it has completed
       * preparing for sending a file, providing the file hash and file size as parameters. (2) A child
       * ContainerTransferAgent.ContainerRecipientRole replies that it has completed preparing for receiving a file. (3) A child
       * ContainerTransferAgent.ContainerSenderRole replies that it has completed a file transfer task, providing the number of sent file
       * chunks as a parameter.
       *
       * This skill uses the conversation ID of the message to look up the file transfer request information, which contains the state of
       * the file transfer that indicates the next step in the conversation.
       *
       * As a result of processing this message in situation (1), a Prepare To Receive File Task message is sent to the recipient
       * container's Container File Recipient Role. In situation (2), a Transfer File Task message is sent to the sending container's
       * Container File Sender Role. And in situation (3), a Task Accomplished Info message is sent in reply to the peer which issued the
       * original Transfer File Request Info message.
       *
       */
      case AHCSConstants.TASK_ACCOMPLISHED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleTaskAccomplishedInfo(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
      // handle other operations ...
    }
    sendMessage(
            receivedMessage,
            Message.notUnderstoodMessage(
            receivedMessage, // receivedMessage
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
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.TASK_ACCOMPLISHED_INFO,
      AHCSConstants.TRANSFER_FILE_REQUEST_INFO
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");
    propagateOperationToChildRolesSeparateThreads(receivedMessage);

  }

  /**
   * Handles a file transfer request task message.
   *
   * @param receivedMessage the received file transfer request task message
   */
  private void handleTransferFileRequestTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    final UUID conversationId = receivedMessage.getConversationId();
    final String senderFilePath = (String) receivedMessage.get(AHCSConstants.MSG_PARM_SENDER_FILE_PATH);
    final String recipientFilePath = (String) receivedMessage.get(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH);
    final String senderContainerName = (String) receivedMessage.get(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME);
    final String recipientContainerName = (String) receivedMessage.get(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME);
    LOGGER.info("handling a file transfer request, sender " + senderContainerName + ", receipient: " + recipientContainerName);

    // record the file transfer information for use with subsequent messages in the conversation
    final FileTransferInfo fileTransferRequestInfo = new FileTransferInfo(
            conversationId,
            senderFilePath,
            recipientFilePath,
            senderContainerName,
            recipientContainerName);
    fileTransferRequestInfo.setRequesterQualifiedName(receivedMessage.getSenderQualifiedName());
    fileTransferRequestInfo.setRequesterService(receivedMessage.getSenderService());

    synchronized (fileTransferDictionary) {
      fileTransferDictionary.put(
              conversationId,
              fileTransferRequestInfo);
    }

    // continue the file transfer conversation by preparing the sending container to send the file
    final Message prepareToSendFileTaskMessage = makeMessage(
            senderContainerName + ".ContainerOperationAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_SEND_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, senderFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, recipientFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, recipientContainerName);

    sendMessage(receivedMessage, prepareToSendFileTaskMessage);
  }

  /**
   * Handles a task accomplished information message.
   *
   * @param message the receved task accomplished information message
   */
  private void handleTaskAccomplishedInfo(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("handling a task accomplished reply");
    }
    final UUID conversationId = message.getConversationId();
    final FileTransferInfo fileTransferInfo;
    synchronized (fileTransferDictionary) {
      fileTransferInfo = fileTransferDictionary.get(conversationId);
    }
    if (fileTransferInfo.getFileTransferState().equals(FileTransferState.UNINITIALIZED)) {
      handleReplyFromPreparedFileSender(message, fileTransferInfo);
    } else if (fileTransferInfo.getFileTransferState().equals(FileTransferState.OK_TO_SEND)) {
      handleReplyFromPreparedFileRecipient(message, fileTransferInfo);
    } else if (fileTransferInfo.getFileTransferState().equals(FileTransferState.FILE_TRANSFER_STARTED)) {
      handleReplyFromTransferFileTask(message, fileTransferInfo);
    }

  }

  /**
   * Handles a task accomplished information message, which is a reply from a Prepare To Send File Task.
   *
   * @param receivedMessage the receved task accomplished information message
   * @param fileTransferInfo the file transfer information
   */
  private void handleReplyFromPreparedFileSender(
          final Message receivedMessage,
          final FileTransferInfo fileTransferInfo) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert fileTransferInfo != null : "fileTransferRequestInfo must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    fileTransferInfo.setFileTransferState(FileTransferState.OK_TO_SEND);
    fileTransferInfo.setFileHash((String) receivedMessage.get(AHCSConstants.MSG_PARM_FILE_HASH));
    fileTransferInfo.setFileSize((long) receivedMessage.get(AHCSConstants.MSG_PARM_FILE_SIZE));

    LOGGER.info("handling a reply from a prepared file sender, " + fileTransferInfo);

    // continue the file transfer conversation by preparing the receiveing container to receive the file
    final Message prepareToReceiveFileTaskMessage = makeMessage(
            fileTransferInfo.getRecipientContainerName() + ".ContainerOperationAgent.ContainerFileRecipientRole", // recipientQualifiedName
            fileTransferInfo.getConversationId(),
            ContainerFileReceiver.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK); // operation
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, fileTransferInfo.getSenderFilePath());
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, fileTransferInfo.getRecipientFilePath());
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME, fileTransferInfo.getSenderContainerName());
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, fileTransferInfo.getRecipientContainerName());
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_FILE_HASH, fileTransferInfo.getFileHash());
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_FILE_SIZE, fileTransferInfo.getFileSize());

    sendMessage(receivedMessage, prepareToReceiveFileTaskMessage);
  }

  /**
   * Handles a task accomplished information message, which is a reply from a Prepare To Receive File Task.
   *
   * @param receivedMessage the receved task accomplished information message
   * @param fileTransferInfo the file transfer information
   */
  private void handleReplyFromPreparedFileRecipient(
          final Message receivedMessage,
          final FileTransferInfo fileTransferInfo) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert fileTransferInfo != null : "fileTransferRequestInfo must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    fileTransferInfo.setFileTransferState(FileTransferState.FILE_TRANSFER_STARTED);

    LOGGER.info("handling a reply from a prepared file receiver, " + fileTransferInfo);

    // continue the file transfer conversation by starting the file transfer process
    final Message prepareToReceiveFileTaskMessage = makeMessage(
            fileTransferInfo.getSenderContainerName() + ".ContainerOperationAgent.ContainerFileSenderRole", // recipientQualifiedName
            fileTransferInfo.getConversationId(),
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.TRANSFER_FILE_TASK); // operation

    sendMessage(receivedMessage, prepareToReceiveFileTaskMessage);
  }

  /**
   * Handles a task accomplished information message, which is a reply from a Transfer File Task.
   *
   * @param receivedMessage the receved task accomplished information message
   * @param fileTransferInfo the file transfer information
   */
  private void handleReplyFromTransferFileTask(
          final Message receivedMessage,
          final FileTransferInfo fileTransferInfo) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert fileTransferInfo != null : "fileTransferRequestInfo must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    fileTransferInfo.setFileTransferState(FileTransferState.FILE_TRANSFER_COMPLETE);

    LOGGER.info("handling a task accomplished (file transfer completed) " + fileTransferInfo);

    // continue the file transfer conversation by replying to the original requestor
    final Message taskAccomplishedInfoMessage = makeMessage(
            fileTransferInfo.getRequesterQualifiedName(), // recipientQualifiedName
            receivedMessage.getConversationId(),
            fileTransferInfo.getRequesterService(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
    final long durationMillis = System.currentTimeMillis() - fileTransferInfo.getStartingDateTime().getMillis();
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_DURATION, durationMillis);
    LOGGER.info("File transfer completed in " + (durationMillis / 1000) + " seconds.");

    if (isFileTransferDictionaryCleaned) {
      final FileTransferInfo removedFileTransferInfo = fileTransferDictionary.remove(receivedMessage.getConversationId());
      assert removedFileTransferInfo != null;
    }

    sendMessage(receivedMessage, taskAccomplishedInfoMessage);
  }

  /**
   * Returns the file transfer information given its corresponding conversation id. Intended for unit testing.
   *
   * @param conversationId the conversation id
   *
   * @return the file transfer information
   */
  protected FileTransferInfo getFileTransferInfo(final UUID conversationId) {
    synchronized (fileTransferDictionary) {
      return fileTransferDictionary.get(conversationId);
    }
  }

  /**
   * Gets whether to clean up the file transfer dictionary after a completed file transfer.
   *
   * @return whether to clean up the file transfer dictionary after a completed file transfer
   */
  public boolean isFileTransferDictionaryCleaned() {
    return isFileTransferDictionaryCleaned;
  }

  /**
   * Sets whether to clean up the file transfer dictionary after a completed file transfer.
   *
   * @param isFileTransferDictionaryCleaned whether to clean up the file transfer dictionary after a completed file transfer
   */
  public void setIsFileTransferDictionaryCleaned(boolean isFileTransferDictionaryCleaned) {
    this.isFileTransferDictionaryCleaned = isFileTransferDictionaryCleaned;
  }

}
