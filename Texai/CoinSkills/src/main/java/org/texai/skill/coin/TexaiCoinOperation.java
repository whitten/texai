package org.texai.skill.coin;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.coin.support.BitcoinMessageReceiver;
import org.texai.util.EnvironmentUtils;
import org.texai.util.StreamConsumer;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Operates a Cooperative Coin C++ instance that runs in a separate process in the same container.
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
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
public class TexaiCoinOperation extends AbstractSkill implements BitcoinMessageReceiver {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(TexaiCoinOperation.class);

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

    final String operation = message.getOperation();
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        // initialize child governance roles
        propagateOperationToChildRoles(
                getClassName(), // service
                operation);
        setSkillState(AHCSConstants.State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.INITIALIZED) : "prior state must be initialized";
        // ready child governance roles
        propagateOperationToChildRoles(
                getClassName(), // service
                operation);
        setSkillState(AHCSConstants.State.READY);
        return true;
    }

    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

    // other operations ...
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
      AHCSConstants.AHCS_READY_TASK,};
  }

  /** Starts the bitcoind instance. */
  private void startBitcoind() {
    sendCommandToBitcoind("");
  }

  /** Shuts down the bitcoind instance. */
  private void shutdownBitcoind() {
    sendCommandToBitcoind("");
  }

  /**
   * Perform a remote procedure call to the bitcoind instance with the given command string.
   *
   * @param command the bitcoind command
   */
  private void sendCommandToBitcoind(final String command) {
    //Preconditions
    assert StringUtils.isNonEmptyString(command) : "message must be a non-empty string";
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("must be running on Linux");
    }

    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("bitcoind-cli ");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    try {
      final Process process = Runtime.getRuntime().exec(cmdArray);
      final StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), LOGGER);
      final StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), LOGGER);
      errorConsumer.setName("errorConsumer");
      errorConsumer.start();
      outputConsumer.setName("outputConsumer");
      outputConsumer.start();
      int exitVal = process.waitFor();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("exitVal: " + exitVal);
      }

      process.getInputStream().close();
      process.getOutputStream().close();
    } catch (InterruptedException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("interrupted");
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }

  }

  @Override
  /** Receives an outbound bitcoin message from the slave peer.
   *
   * @param message the given bitcoin protocol message
   */
  public void receiveBitcoinMessageFromSlave(final com.google.bitcoin.core.Message message) {
    // send the outbound bitcoin message from the slave peer to the Texai network recipient.

  }

}
