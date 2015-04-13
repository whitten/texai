/**
 * Created on Aug 30, 2014, 11:31:08 PM.
 *
 * Description: Writes the configuration file for the slave bitcoind instance.
 *
 * Copyright (C) Aug 30, 2014, Stephen L. Reed, Texai.org.
 */
package org.texai.skill.aicoin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@ThreadSafe
public class AICWriteConfigurationFile extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICWriteConfigurationFile.class);

  /**
   * Constructs a new AICWriteConfigurationFile instance.
   */
  public AICWriteConfigurationFile() {
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
       * This task message is sent from the parent AICOperationAgent.AICOperationRole. It is expected to be the first task message that this
       * role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendshipAgent.TopmostFriendshipRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        return;

      /**
       * Write Configuration File Info
       *
       * This request message is sent from the sibling AICOperation skill.
       *
       * As a result, the aicoin.conf is generated and written to disk, and a Task Accomplished Info message is sent back to the
       * AICOperation skill as a response.
       */
      case AHCSConstants.WRITE_CONFIGURATION_FILE_INFO:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        writeConfigurationFile(receivedMessage);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
    }

    sendDoNotUnderstandInfoMessage(receivedMessage);
  }

  /**
   * Writes the bitcoin.conf file.
   *
   * @param receivedMessage the task message
   */
  private void writeConfigurationFile(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    final String directoryPath = (String) receivedMessage.get(AHCSConstants.WRITE_CONFIGURATION_FILE_INFO_DIRECTORY_PATH);
    assert StringUtils.isNonEmptyString(directoryPath);
    LOGGER.info("The aicoin-qt configuration directory is " + directoryPath);
    // delete the old version of the configuration file
    final File confFile = new File(directoryPath + "/aicoin.conf");
    LOGGER.info("confFile: " + confFile);
    if (confFile.exists()) {
      LOGGER.info("deleting existing " + confFile);
      final boolean isOK = confFile.delete();
      if (!isOK) {
        LOGGER.info("problem deleting " + confFile);
      }
    }

    // create the .aicoin directory if not already present
    final File directory = new File(directoryPath);
    if (!directory.isDirectory()) {
      final boolean isOK = directory.mkdir();
      assert isOK && directory.isDirectory();
    }

    //TODO implement a skill for AICOperation that that verifies that each user is unique and that each password is unique.
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
    try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(confFile), "UTF-8"))) {
      bufferedWriter.write("# no proof-of-work after block height\n");
      bufferedWriter.write("nopowafter=1\n");
      bufferedWriter.write("# no dns seeds in the demo\n");
      bufferedWriter.write("dnsseed=0\n");
      bufferedWriter.write("# no universal plug and play NAT routing in the demo\n");
      bufferedWriter.write("upnp=0\n");
      //TODO demo code
      switch (getContainerName()) {
        case "Mint":
          bufferedWriter.write("# this instance accepts incoming connections\n");
          bufferedWriter.write("listen=1\n");
          break;
        case "Alice":
        case "Bob":
          bufferedWriter.write("# this instance accepts incoming connections\n");
          bufferedWriter.write("listen=1\n");
          bufferedWriter.write("# connect to the mint\n");
          // on the same host in the development LAN
          if (getNodeRuntime().getNetworkName().equals(NetworkUtils.TEXAI_MAINNET)) {
            bufferedWriter.write("connect=Mint:31417\n");
          } else {
            // testnet
            bufferedWriter.write("connect=TestMint:27185\n");
          }
          break;
        case "BlockchainExplorer":
          bufferedWriter.write("# this instance accepts incoming connections\n");
          bufferedWriter.write("listen=1\n");
          // on the same host in the development LAN
          if (getNodeRuntime().getNetworkName().equals(NetworkUtils.TEXAI_MAINNET)) {
            bufferedWriter.write("connect=Mint:31417\n");
            // on a separate host in the development LAN
            //bufferedWriter.write("connect=192.168.0.7:31417\n");
          } else {
            // testnet
            bufferedWriter.write("connect=TestMint:27185\n");
            // on a separate host in the development LAN
            //bufferedWriter.write("connect=192.168.0.7:27185\n");
          }
          break;
        default:
          // peer on the internet
          bufferedWriter.write("# this instance does not accept incoming connections\n");
          bufferedWriter.write("listen=0\n");
          bufferedWriter.write("# connect to the mint\n");
          if (getNodeRuntime().getNetworkName().equals(NetworkUtils.TEXAI_MAINNET)) {
            bufferedWriter.write("connect=texai.dyndns.org:31417\n");
          } else {
            // testnet
            bufferedWriter.write("connect=texai.dyndns.org:27185\n");
          }
          break;
      }
      if (getNodeRuntime().getNetworkName().equals(NetworkUtils.TEXAI_MAINNET)) {
        bufferedWriter.write("# mainnet listening port\n");
        bufferedWriter.write("port=31417\n");
      } else {
        bufferedWriter.write("# testnet\n");
        bufferedWriter.write("testnet=1\n");
        bufferedWriter.write("# testnet listening port\n");
        bufferedWriter.write("port=27185\n");
        bufferedWriter.write("# testnet rpc port\n");
        bufferedWriter.write("rpcport=18332\n");
      }
      bufferedWriter.write("# how many blocks to verify upon startup\n");
      bufferedWriter.write("checkblocks=5\n");
      bufferedWriter.write("# do not generate a block unless commanded to\n");
      bufferedWriter.write("gen=0\n");
      bufferedWriter.write("# \n");
      bufferedWriter.write("\n");
      bufferedWriter.write("# maintain an extra transaction index that allows the RPC getrawtransaction call to operate\n");
      bufferedWriter.write("txindex=1\n");
      bufferedWriter.write("# allow aicoin-cli to send commands to this instance\n");
      bufferedWriter.write("rpcconnect=127.0.0.1\n");
      bufferedWriter.write("# allow rpc commands\n");
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

    // send a task accomplished info message back to the AICOperation role
    final Message replyMessage = Message.replyTaskAccomplished(receivedMessage);
    sendMessageViaSeparateThread(receivedMessage, replyMessage);
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.WRITE_CONFIGURATION_FILE_INFO
    };
  }

}
