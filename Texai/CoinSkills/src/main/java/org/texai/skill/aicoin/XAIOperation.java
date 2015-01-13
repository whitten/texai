package org.texai.skill.aicoin;

import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.aicoin.support.AICoinUtils;
import org.texai.skill.aicoin.support.XAIBitcoinMessageReceiver;
import org.texai.util.EnvironmentUtils;
import org.texai.util.TexaiException;

/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Operates a slave bitcoind instance that runs in a separate continueConversation in the same container.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 *
 * Copyright (C) 2014 Texai
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
@ThreadSafe
public final class XAIOperation extends AbstractSkill implements XAIBitcoinMessageReceiver {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAIOperation.class);
  // the conversationId state variable
  private static final String CONVERSATION_ID = "conversationId";
  // the replyWith state variable
  private static final String REPLY_WITH = "replyWith";
  // the path to the aicoin-qt configuration and data directory
  private static final String AICOIN_DIRECTORY_PATH = "../.aicoin";
  // the output and error stream consumers from the launched VNC server
  private Object[] streamConsumers;

  /**
   * Constructs a new XTCOperation instance.
   */
  public XAIOperation() {
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
   * Receives and attempts to continueConversation the given message. The skill is thread safe, given that any contained libraries are
   * single threaded with regard to the conversation.
   *
   * @param message the given message
   *
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }

    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return true;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent XAINetworkOperationAgent.XAINetworkOperationRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent XAINetworkOperationAgent.XAINetworkOperationRole. It indicates that
       * the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return true;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent XAINetworkEpisodicMemoryAgent.XAINetworkEpisodicMemoryRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return true;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        performMission(message);
        return true;

      /**
       * Task Accomplished Info
       *
       * This information message is sent from this role's XAIWriteConfigurationFile skill indicating that it has emitted the aicoin.conf
       * file.
       */
      case AHCSConstants.TASK_ACCOMPLISHED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        continueConversation(message);
        return true;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
        LOGGER.warn(message);
        return true;

      // handle other operations ...
    }

    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

    sendMessage(notUnderstoodMessage(message));
    return true;
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

    //TODO handle operations
    return notUnderstoodMessage(message);
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
      AHCSConstants.BECOME_READY_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.TASK_ACCOMPLISHED_INFO
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
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");
    // write the aicoind configuration file
    final Message writeConfigurationFileTaskmessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService
            getRole().getQualifiedName(), // recipientQualifiedName
            UUID.randomUUID(), // conversationId,
            UUID.randomUUID(), // replyWith,
            XAIWriteConfigurationFile.class.getName(), // recipientService
            AHCSConstants.WRITE_CONFIGURATION_FILE_TASK); // operation
    writeConfigurationFileTaskmessage.put(AHCSConstants.WRITE_CONFIGURATION_FILE_TASK_DIRECTORY_PATH, AICOIN_DIRECTORY_PATH);
    // set timeout
    setMessageReplyTimeout(
            writeConfigurationFileTaskmessage,
            10000, // timeoutMillis
            false, // isRecoverable
            null); // recoveryAction
    sendMessageViaSeparateThread(writeConfigurationFileTaskmessage);
  }

  /**
   * Continues the conversation with the XAIWriteConfigurationFile skill.
   *
   * @param message the received message
   */
  private synchronized void continueConversation(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert message.getInReplyTo() != null : "message must have inReplyTo value " + message.toDetailedString();

    removeMessageTimeOut(message.getInReplyTo());
    LOGGER.info("The bitcoind configuration file has been written");
    launchAicoind();
    if ("BlockchainExplorer".equals(getContainerName())) {
      // after a pause, launch the Insight blockchain explorer instance
      getNodeRuntime().getExecutor().execute(new InsightRunner());
    }
  }

  /**
   * Launches the aicoind instance.
   */
  private void launchAicoind() {
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    final String display = System.getenv("DISPLAY");
    LOGGER.info("X11 DISPLAY=" + display);
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("../bin/aicoin-qt -debug -shrinkdebugfile=1 -datadir=").append(AICOIN_DIRECTORY_PATH);
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the slave aicoin-qt instance");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  class InsightRunner implements Runnable {

    @Override
    public void run() {
      try {
        LOGGER.info("Waiting 5 minutes before launching the block explorer ...");
        Thread.sleep(5 * 60 * 1000);
//        LOGGER.info("Waiting 1 minute before launching the block explorer ...");
//        Thread.sleep(60 * 1000);
      } catch (InterruptedException ex) {
      }
      launchInsight();
    }

  }

  /**
   * Launches the Insight blockchain explorer instance.
   */
  private void launchInsight() {
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    final String display = System.getenv("DISPLAY");
    LOGGER.info("X11 DISPLAY=" + display);
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("cd /root/insight && npm start");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the Insight blockchain explorer instance.");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  /**
   * Shuts down the aicoind instance.
   */
  private void shutdownAicoind() {
    //TODO
  }

  @Override
  /**
   * Receives an outbound bitcoin message from the slave peer.
   *
   * @param message the given bitcoin protocol message
   */
  public void receiveBitcoinMessageFromSlave(final com.google.bitcoin.core.Message message) {
    // send the outbound bitcoin message from the slave peer to the Texai network recipient.

  }

}
