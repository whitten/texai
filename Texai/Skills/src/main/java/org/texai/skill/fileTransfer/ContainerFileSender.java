package org.texai.skill.fileTransfer;

/**
 * ContainerFileSender.java
 *
 * Description: An instance of this skill sends a file from its container to another container.
 *
 * Copyright (C) Jan 29, 2015, Stephen L. Reed.
 */
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.FileTransferState;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.StringUtils;
import org.texai.x509.MessageDigestUtils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerFileSender extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(ContainerFileSender.class);
  // the file transfer dictionary, conversation id --> file transfer request info
  private final Map<UUID, FileTransferRequestInfo> fileTransferDictionary = new HashMap<>();

  /**
   * Constructs a new ContainerFileSender instance.
   */
  public ContainerFileSender() {
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
       * This task message is sent from the parent NetworkFileTransferAgent.NetworkFileTransferRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, NetworkFileTransferAgent.NetworkFileTransferRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent NetworkFileTransferAgent.NetworkFileTransferRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, NetworkFileTransferAgent.NetworkFileTransferRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return;

      /**
       * Prepare To Send File Task
       *
       * This task message is sent from the network-singleton, NetworkFileTransferAgent.NetworkFileTransferRole. It commands this
       * network-connected role to prepare to send a file.
       *
       * Parameters of the message are: sender file path, recipient file path, and recipient container. As a result, a Task Accomplished
       * Information message is replied back to the sending NetworkFileTransferRole, which continues the file transfer conversation.
       */
      case AHCSConstants.PREPARE_TO_SEND_FILE_TASK:
        handlePrepareToSendFileTask(message);
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
  public Message converseMessage(Message message) {
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
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PREPARE_TO_SEND_FILE_TASK
    };
  }

  /**
   * Perform this role's mission.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

  }

  /**
   * Handles the prepare to send file task message.
   *
   * @param message the received prepare to send file task message
   */
  private void handlePrepareToSendFileTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("handling a prepare to send file task");
    final UUID conversationId = message.getConversationId();
    final String senderFilePath = (String) message.get(AHCSConstants.MSG_PARM_SENDER_FILE_PATH);

    final File file = new File(senderFilePath);
    if (!file.exists()) {
      LOGGER.info("file does not exist: " + file);
      sendMessage(makeExceptionMessage(
              message, // receivedMessage
              "file does not exist: " + file)); // reason
      return;
    } else if (file.isDirectory()) {
      LOGGER.info("file is a directory - not an ordinary file: " + file);
      sendMessage(makeExceptionMessage(
              message, // receivedMessage
              "file is a directory - not an ordinary file: " + file)); // reason
      return;
    }

    // record the file transfer information for use with subsequent messages in the conversation
    final String recipientFilePath = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH);
    final String recipientContainerName = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME);
    final FileTransferRequestInfo fileTransferRequestInfo = new FileTransferRequestInfo(
            conversationId,
            senderFilePath,
            recipientFilePath,
            recipientContainerName);
    synchronized (fileTransferDictionary) {
      fileTransferDictionary.put(
              conversationId,
              fileTransferRequestInfo);
    }

    fileTransferRequestInfo.fileHash = MessageDigestUtils.fileHashString(file);
    fileTransferRequestInfo.fileSize = file.length();
    fileTransferRequestInfo.fileTransferState = FileTransferState.OK_TO_SEND;

    // reply with the hash and the size of the file to be transferred
    final Message taskAccomplishedMessage = makeReplyMessage(
            message, // receivedMessage
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
    taskAccomplishedMessage.put(AHCSConstants.MSG_PARM_FILE_HASH, fileTransferRequestInfo.fileHash);
    taskAccomplishedMessage.put(AHCSConstants.MSG_PARM_FILE_SIZE, fileTransferRequestInfo.fileSize);

    sendMessage(taskAccomplishedMessage);
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
    // the recipient container name
    private final String recipientContainerName;
    // the hash of the transferred file's contents
    private String fileHash;
    // the size of the transferred file's contents
    private long fileSize = -1;
    // the file transfer state
    private AHCSConstants.FileTransferState fileTransferState = AHCSConstants.FileTransferState.UNINITIALIZED;
    // the number of transferred file chunks
    private int fileChunksCnt = -1;

    /**
     * Creates a new FileTransferRequestInfo instance.
     *
     * @param conversationId the file transfer conversation id
     * @param senderFilePath the sender file path
     * @param recipientFilePath the recipient file path
     * @param recipientContainerName the starting date time of the file transfer
     */
    FileTransferRequestInfo(
            final UUID conversationId,
            final String senderFilePath,
            final String recipientFilePath,
            final String recipientContainerName) {
      //Preconditions
      assert conversationId != null : "conversationId must not be null";
      assert StringUtils.isNonEmptyString(senderFilePath) : "senderFilePath must be a non-empty string";
      assert StringUtils.isNonEmptyString(recipientFilePath) : "recipientFilePath must be a non-empty string";
      assert StringUtils.isNonEmptyString(recipientContainerName) : "recipientContainerName must be a non-empty string";

      this.conversationId = conversationId;
      this.senderFilePath = senderFilePath;
      this.recipientFilePath = recipientFilePath;
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
              .append(senderFilePath)
              .append(" --> ")
              .append(recipientContainerName)
              .append(':')
              .append(recipientFilePath)
              .append('\n');
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
