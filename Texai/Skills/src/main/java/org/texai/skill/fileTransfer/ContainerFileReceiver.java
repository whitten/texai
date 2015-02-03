package org.texai.skill.fileTransfer;

/**
 * ContainerFileReceiver.java
 *
 * Description: An instance of this skill receives a file into its container, sent from another container.
 *
 Copyright (C) Jan 29, 2015, Stephen L. Reed.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

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
       * This task message is sent from the network-singleton, NetworkFileTransferAgent.NetworkFileTransferRole. It indicates that
       * the parent is ready to converse with this role as needed.
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
      AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
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
    final FileTransferInfo fileTransferRequestInfo = new FileTransferInfo(
            conversationId,
            senderFilePath,
            recipientFilePath,
            senderContainerName,
            recipientContainerName);
    fileTransferRequestInfo.setFileHash(fileHash);
    fileTransferRequestInfo.setFileSize(fileSize);

    synchronized (fileTransferDictionary) {
      fileTransferDictionary.put(
              conversationId,
              fileTransferRequestInfo);
    }

    fileTransferRequestInfo.setFileTransferState(FileTransferInfo.FileTransferState.OK_TO_RECEIVE);

    final Message taskAccomplishedMessage = makeReplyMessage(
            message, // receivedMessage
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation

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
  protected FileTransferInfo getFileTransferRequestInfo(final UUID conversationId) {
    synchronized (fileTransferDictionary) {
      return fileTransferDictionary.get(conversationId);
    }
  }
}
