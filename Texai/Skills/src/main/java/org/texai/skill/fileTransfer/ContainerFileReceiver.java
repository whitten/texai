package org.texai.skill.fileTransfer;

/**
 * ContainerFileReceiver.java
 *
 * Description: An instance of this skill receives a file into its container, sent from another container.
 *
 * Copyright (C) Jan 29, 2015, Stephen L. Reed.
 */
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.fileTransfer.FileTransferInfo.FileTransferState;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerFileReceiver extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(ContainerFileReceiver.class);
  // the file transfer dictionary, conversation id --> file transfer request info
  private final Map<UUID, FileTransferInfo> fileTransferDictionary = new HashMap<>();
  // the indicator to clean up the file transfer dictionary after a completed file transfer
  private boolean isFileTransferDictionaryCleaned = true;

  /**
   * Constructs a new ContainerFileReceiver instance.
   */
  public ContainerFileReceiver() {
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
      case AHCSConstants.INITIALIZE_TASK:
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
       * Prepare To Receive File Task
       *
       * This task message is sent from the network-singleton, NetworkFileTransferAgent.NetworkFileTransferRole. It commands this
       * network-connected role to prepare to receive a file.
       *
       * Parameters of the message are: sender file path, recipient file path, file hash and file size. As a result, a Task Accomplished
       * Information message is replied back to the sending NetworkFileTransferRole, which continues the file transfer conversation.
       */
      case AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK:
        handlePrepareToReceiveFileTask(message);
        return;

      /**
       * Transfer File Chunk Information
       *
       * This information message is sent from the peer ContainerFileTransferAgent.ContainerFileSenderRole. It requests this
       * network-connected role to accept the chunk of bytes in the message and write them to the output file. If the number of bytes is
       * less than the maximum buffer size, then the output file is closed and the file transfer operation completed.
       *
       * Parameters of the message are: file chunk bytes and the size of the chunk.
       *
       * As a result, a Task Accomplished Information message is replied back to the sending
       * ContainerFileTransferAgent.ContainerFileSenderRole, which continues the file transfer conversation.
       */
      case AHCSConstants.TRANSFER_FILE_CHUNK_INFO:
        transferFileChunkTask(message);
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
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.TRANSFER_FILE_CHUNK_INFO
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
   * Handles the prepare to receive file task message.
   *
   * @param message the received prepare to send file task message
   */
  private void handlePrepareToReceiveFileTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("handling a prepare to receive file task");
    // record the file transfer information for use with subsequent messages in the conversation
    final UUID conversationId = message.getConversationId();
    final String recipientFilePath = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH);
    final String senderFilePath = (String) message.get(AHCSConstants.MSG_PARM_SENDER_FILE_PATH);
    final String senderContainerName = (String) message.get(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME);
    final String recipientContainerName = (String) message.get(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME);
    final String fileHash = (String) message.get(AHCSConstants.MSG_PARM_FILE_HASH);
    final long fileSize = (long) message.get(AHCSConstants.MSG_PARM_FILE_SIZE);

    final FileTransferInfo fileTransferInfo = new FileTransferInfo(
            conversationId,
            senderFilePath,
            recipientFilePath,
            senderContainerName,
            recipientContainerName);

    fileTransferInfo.setFileHash(fileHash);
    fileTransferInfo.setFileSize(fileSize);
    final File file = new File(recipientFilePath);
    try {
      fileTransferInfo.setBufferedOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    } catch (FileNotFoundException ex) {
      LOGGER.info("file does not exist: " + file);
      sendMessage(makeExceptionMessage(
              message, // receivedMessage
              "file does not exist: " + file)); // reason
      return;
    }

    synchronized (fileTransferDictionary) {
      fileTransferDictionary.put(
              conversationId,
              fileTransferInfo);
    }

    fileTransferInfo.setFileTransferState(FileTransferInfo.FileTransferState.OK_TO_RECEIVE);

    final Message taskAccomplishedMessage = makeReplyMessage(
            message, // receivedMessage
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation

    sendMessage(taskAccomplishedMessage);
  }

  /**
   * Appends the file chunk bytes in the message to the output file.
   *
   * @param message the received transfer file chunk information message
   */
  private void transferFileChunkTask(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("handling a transfer file task");
    final UUID conversationId = message.getConversationId();
    final FileTransferInfo fileTransferInfo;
    synchronized (fileTransferDictionary) {
      fileTransferInfo = fileTransferDictionary.get(conversationId);
    }
    assert fileTransferInfo.getFileTransferState().equals(FileTransferInfo.FileTransferState.OK_TO_RECEIVE);
    fileTransferInfo.setFileTransferState(FileTransferInfo.FileTransferState.FILE_TRANSFER_STARTED);

    transferFileChunk(message, fileTransferInfo);
  }

  /**
   * Transfers a file chunks from a container.
   *
   * @param message the received message
   * @param fileTransferInfo the file transfer information
   */
  private void transferFileChunk(
          final Message message,
          final FileTransferInfo fileTransferInfo) {
    //Preconditions
    assert message != null : "message must not be null";
    assert fileTransferInfo != null : "fileTransferInfo must not be null";
    assert fileTransferInfo.getBufferedOutputStream() != null : "bufferedOutputStream must not be null";
    assert fileTransferInfo.getConversationId().equals(message.getConversationId()) :
            "invalid conversation id\n" + message + '\n' + fileTransferInfo;
    assert fileTransferInfo.getFileTransferState().equals(FileTransferState.FILE_TRANSFER_STARTED);

    final byte[] buffer = (byte[]) message.get(AHCSConstants.MSG_PARM_BYTES);
    assert buffer != null;
    final int bytesSize = (int) message.get(AHCSConstants.MSG_PARM_BYTES_SIZE);
    assert bytesSize == buffer.length;

    fileTransferInfo.incrementFileChunksCnt();
    final int fileChunksCnt = fileTransferInfo.getFileChunksCnt();
    LOGGER.info("writing received file chunk " + fileChunksCnt);

    try {
      fileTransferInfo.getBufferedOutputStream().write(buffer);
      fileTransferInfo.getBufferedOutputStream().flush();
      if (bytesSize < 8192) {
        fileTransferInfo.getBufferedOutputStream().close();
        if (isFileTransferDictionaryCleaned) {
          final FileTransferInfo removedFileTransferInfo = fileTransferDictionary.remove(message.getConversationId());
          assert removedFileTransferInfo != null;
        }
        LOGGER.info("File transfer completed.");
      }
    } catch (IOException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      //TODO send exception message back to the sending ContainerFileTransferAgent.ContainerFileSenderRole.
    }

    final Message taskAccomplishedInfoMessage = makeReplyMessage(
            message,
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT, fileChunksCnt);

    sendMessage(taskAccomplishedInfoMessage);
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
