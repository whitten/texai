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
import org.joda.time.Seconds;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.AHCSConstants.FileTransferState;
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
       * Sender Role. The reply message continues the conversation.
       *
       */
      case AHCSConstants.TRANSFER_FILE_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleTransferFileRequestTask(message);
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
        handleTaskAccomplishedInfo(message);
        return;

      //TODO task accomplished info and exception info handlers
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
      AHCSConstants.TASK_ACCOMPLISHED_INFO,
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
   * @param message the received file transfer request task message
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
    final FileTransferRequestInfo fileTransferRequestInfo = new FileTransferRequestInfo(
            conversationId,
            senderFilePath,
            recipientFilePath,
            senderContainerName,
            recipientContainerName);
    synchronized (fileTransferDictionary) {
      fileTransferDictionary.put(
              conversationId,
              fileTransferRequestInfo);
    }

    // continue the file transfer conversation by preparing the sending container to send the file
    final Message prepareToSendFileTaskMessage = makeMessage(
            senderContainerName + ".ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_SEND_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, senderFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, recipientFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, recipientContainerName);

    sendMessage(prepareToSendFileTaskMessage);
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

    LOGGER.info("handling a task accomplished reply");
    final UUID conversationId = message.getConversationId();
    final FileTransferRequestInfo fileTransferRequestInfo;
    synchronized (fileTransferDictionary) {
      fileTransferRequestInfo = fileTransferDictionary.get(conversationId);
    }
    if (fileTransferRequestInfo.fileTransferState.equals(FileTransferState.UNINITIALIZED)) {
      handleReplyFromPreparedFileSender(message, fileTransferRequestInfo);
    }

  }

  /**
   * Handles a task accomplished information message, which is a reply from a Prepare To Send File Task.
   *
   * @param message the receved task accomplished information message
   * @param fileTransferRequestInfo the file transfer information
   */
  private void handleReplyFromPreparedFileSender(
          final Message message,
          final FileTransferRequestInfo fileTransferRequestInfo) {
    //Preconditions
    assert message != null : "message must not be null";
    assert fileTransferRequestInfo != null : "fileTransferRequestInfo must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    fileTransferRequestInfo.fileTransferState = AHCSConstants.FileTransferState.OK_TO_SEND;
    fileTransferRequestInfo.fileHash = (String) message.get(AHCSConstants.MSG_PARM_FILE_HASH);
    fileTransferRequestInfo.fileSize = (long) message.get(AHCSConstants.MSG_PARM_FILE_SIZE);

    LOGGER.info("handling a reply from a prepared file sender, " + fileTransferRequestInfo);

    // continue the file transfer conversation by preparing the receiveing container to receive the file
    final Message prepareToSendFileTaskMessage = makeMessage(
            fileTransferRequestInfo.recipientContainerName + ".ContainerFileTransferAgent.ContainerFileRecipientRole", // recipientQualifiedName
            fileTransferRequestInfo.conversationId,
            ContainerFileReceiver.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, fileTransferRequestInfo.recipientFilePath);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME, fileTransferRequestInfo.senderContainerName);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_FILE_HASH, fileTransferRequestInfo.fileHash);
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_FILE_SIZE, fileTransferRequestInfo.fileSize);

    sendMessage(prepareToSendFileTaskMessage);
  }

  /**
   * Returns the file transfer information given its corresponding conversation id. Intended for unit testing.
   *
   * @param conversationId the conversation id
   *
   * @return the file transfer information
   */
  protected FileTransferRequestInfo getFileTransferRequestInfo(final UUID conversationId) {
    synchronized (fileTransferDictionary) {
      return fileTransferDictionary.get(conversationId);
    }
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
    // the ending date time of the file transfer
    private DateTime endingDateTime;
    // the hash of the transferred file's contents
    private String fileHash;
    // the size of the transferred file's contents
    private long fileSize = -1;
    // the file transfer state
    protected FileTransferState fileTransferState = FileTransferState.UNINITIALIZED;
    // the number of transferred file chunks
    private int fileChunksCnt = -1;

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

    /**
     * Returns a brief string representation of this object.
     *
     * @return a brief string representation of this object
     */
    public String toBriefString() {
      return (new StringBuilder())
              .append('[')
              .append(senderContainerName)
              .append(':')
              .append(senderFilePath)
              .append(" --> ")
              .append(recipientContainerName)
              .append(':')
              .append(recipientFilePath)
              .append(']')
              .toString();
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder
              .append('[')
              .append(senderContainerName)
              .append(':')
              .append(senderFilePath)
              .append(" --> ")
              .append(recipientContainerName)
              .append(':')
              .append(recipientFilePath)
              .append("\nstarted: ")
              .append(startingDateTime)
              .append('\n');
      if (endingDateTime != null) {
        stringBuilder
                .append("duration: ")
                .append(Seconds.secondsBetween(startingDateTime, endingDateTime))
                .append(" seconds\n");
      }
      if (fileHash != null) {
        stringBuilder
                .append("file hash: ")
                .append(fileHash)
                .append('\n');
      }
      if (fileHash != null) {
        stringBuilder
                .append("file hash: ")
                .append(fileHash)
                .append('\n');
      }
      if (fileSize > -1) {
        stringBuilder
                .append("file size: ")
                .append(fileSize)
                .append(" bytes\n");
      }
      stringBuilder
              .append("file transfer state: ")
              .append(AHCSConstants.fileTransferStateToString(fileTransferState))
              .append('\n');
      if (fileChunksCnt > -1) {
        stringBuilder
                .append("file chunks transfered: ")
                .append(fileChunksCnt)
                .append('\n');
      }
      stringBuilder.append(']');
      return stringBuilder.toString();
    }

  }

}
