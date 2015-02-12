package org.texai.skill.aicoin;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.skill.aicoin.support.AICoinUtils;
import org.texai.util.EnvironmentUtils;
import org.texai.util.TexaiException;

/**
 * Created on Aug 28, 2014, 8:36:17 PM.
 *
 * Description: Provides the skill of creating a new block for the Bitcoin blockchain every 10 minutes.
 *
 * Copyright (C) Aug 28, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 *
 * Copyright (C) 2014 Texai
 */
@ThreadSafe
public final class XAIMint extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAIMint.class);
  // the timer
  private final Timer mintTimer;
  // the path to the aicoin-qt configuration and data directory
  private static final String AICOIN_DIRECTORY_PATH = "../.aicoin";

  /**
   * Constructs a new XTCMint instance.
   */
  public XAIMint() {
    mintTimer = new Timer(
            "mint timer", // name
            true); // isDaemon
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
       * This task message is sent from the parent XAINetworkOperationAgent.XAINetworkOperationRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

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
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent XAINetworkOperationAgent.XAINetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.READY) : "prior state must be ready";
        performMission(message);
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

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;
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
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK
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
    assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";

    LOGGER.info("performing the mission");
    mintNewBlocks();
  }

  /**
   * Mints a new Bitcoin block every 10 minutes.
   */
  private void mintNewBlocks() {
    LOGGER.info("mint new blocks");

    // calculate the milliseconds delay until the next 10 minute mark ...
    final Calendar calendar = Calendar.getInstance();
    final int minutes = calendar.get(Calendar.MINUTE);
    final int modulo10minutes = minutes % 10;
    long delay = (10 - modulo10minutes) * 60000L;
    assert delay <= 600000;

    mintTimer.scheduleAtFixedRate(
            new MintTimerTask(), // task
            delay,
            600000l); // period - 10 minutes
    //60000l); // period - 1 minute
  }

  /**
   * Provides a mint timer task
   */
  class MintTimerTask extends TimerTask {

    /**
     * Executes this timer task
     */
    @Override
    public void run() {
      generateNewBlock();
    }
  }

  /**
   * Uses the Bitcoin command line interface to generate a new block.
   */
  private void generateNewBlock() {
    //Preconditions
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("Operating system must be Linux");
    }

    LOGGER.info("generate a new block");
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder
            .append("../bin/aicoin-cli -datadir=")
            .append(AICOIN_DIRECTORY_PATH)
            .append(" setgenerate true 1");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);

  }

}
