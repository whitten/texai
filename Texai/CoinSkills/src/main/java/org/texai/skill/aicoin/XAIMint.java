package org.texai.skill.aicoin;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
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
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
@ThreadSafe
public final class XAIMint extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAIMint.class);
  // the timer
  private final Timer mintTimer;

  /**
   * Constructs a new XTCMint instance.
   */
  public XAIMint() {
    mintTimer = new Timer(
            "mint timer", // name
            true); // isDaemon
  }

  /** Gets the logger.
   *
   * @return  the logger
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
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.MINT_NEW_BLOCKS_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.READY) : "prior state must be ready";
        mintNewBlocks();
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
    }

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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.MINT_NEW_BLOCKS_TASK
    };
  }

  /**
   * Mints a new Bitcoin block every 10 minutes.
   */
  private void mintNewBlocks() {
    LOGGER.info("mint new blocks");
    mintTimer.scheduleAtFixedRate(
            null,
            0l, // delay,
            10000l); // period - 10 minutes
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

    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("cd ");
    stringBuilder.append(System.getProperty("user.dir"));
    stringBuilder.append("git/bitcoin/src/ ; ./bitcoin-cli setgenerate true");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    try {
      Runtime.getRuntime().exec(cmdArray);
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }
}
