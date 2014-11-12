/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Writes the configuration file for the slave bitcoind instance.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 *
 * Copyright (C) 2014 Texai
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.texai.skill.aicoin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@ThreadSafe
public class XAIWriteConfigurationFile extends AbstractSkill {

  // the logger

  private static final Logger LOGGER = Logger.getLogger(XAIWriteConfigurationFile.class);

  /**
   * Constructs a new XAIWriteConfigurationFile instance.
   */
  public XAIWriteConfigurationFile() {
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
   * Receives and attempts to process the given message. The skill is thread
   * safe, given that any contained libraries are single threaded with regard to
   * the conversation.
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
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        setSkillState(AHCSConstants.State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        assert getSkillState().equals(AHCSConstants.State.INITIALIZED) : "prior state must be initialized";
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.WRITE_CONFIGURATION_FILE_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        return writeConfigurationFile(message);

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
    }

    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /**
   * Writes the bitcoin.conf file.
   *
   * @param message the task message
   * @return whether this task completed OK
   */
  private boolean writeConfigurationFile(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // delete the old version of the configuration file
    final String homeDirectory = System.getProperty("user.home");
    assert StringUtils.isNonEmptyString(homeDirectory);
    final File confFile = new File(homeDirectory + "/.bitcoin/bitcoin.conf");
    LOGGER.info("confFile: " + confFile);
    if (confFile.exists()) {
      LOGGER.info("deleting existing " + confFile);
      confFile.delete();
    }

    // create the .bitcoin directory if not already present
    final File directory = new File(homeDirectory + "/.bitcoin");
    LOGGER.info("directory: " + directory);
    if (!directory.isDirectory()) {
      directory.mkdir();
      assert directory.isDirectory();
    }

    //TODO implement a skill for XAIOperation that that verifies that each user is unique and that each password is unique.
    //TODO implement a parent skill that maintains a tamper-evident log of hashed/salted user and password pairs.

    final String rpcuser = System.getenv("RPC_USER");
    if (!StringUtils.isNonEmptyString(rpcuser)) {
      throw new TexaiException("the RPC_USER environment variable must be assigned a value");
    }
    final String rpcpassword = System.getenv("RPC_PASSWORD");
    if (!StringUtils.isNonEmptyString(rpcpassword)) {
      throw new TexaiException("the RPC_PASSWORD environment variable must be assigned a value");
    }
    // emit the configuration file line by line
    try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(confFile))) {
      bufferedWriter.write("testnet=1\n");
      bufferedWriter.write("server=1\n");
      bufferedWriter.write("rpcuser=");
      bufferedWriter.write(rpcuser);
      bufferedWriter.write("\n");
      bufferedWriter.write("rpcpassword=");
      bufferedWriter.write(rpcpassword);
      bufferedWriter.write("\n");
      bufferedWriter.write("\n");
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }

    // send a task accomplished info message back to the XAIOperation role
    final Message replyMessage = Message.replyTaskAccomplished(message);
    sendMessageViaSeparateThread(replyMessage);
    return true;
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given
   * that any contained libraries are single threaded with regard to the
   * conversation.
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
      AHCSConstants.AHCS_READY_TASK,
      AHCSConstants.WRITE_CONFIGURATION_FILE_TASK};
  }

}
