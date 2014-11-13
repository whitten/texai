/*
 * SingletonConfiguration.java
 *
 * Created on Mar 14, 2012, 10:49:55 PM
 *
 * Description: Configures the roles in this container whose parents are nomadic singleton roles hosted
 * on a probably remote super-peer.
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.singletonConfiguration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.seed.SeedNodeInfo;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class SingletonConfiguration extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(SingletonConfiguration.class);
  // the locations and credentials for network seed nodes
  private Set<SeedNodeInfo> seedNodesInfos;
  // the SHA-512 hash of the seed node infos serialized file
  private final String seedNodeInfosFileHashString =
          "RreCep5ZgCJd9f0G66/7bRz3bdbWfXSNMMCATJed8YgKBYi4zbbkgC0srC4GECSrIAXKJAkXIfCAI1vvjWOLMg==";

  /**
   * Constructs a new SingletonConfiguration instance.
   */
  public SingletonConfiguration() {
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

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        setSkillState(State.READY);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.TASK_ACCOMPLISHED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        //TODO
        return true;

      case AHCSConstants.SEED_CONNECTION_REQUEST_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";
        seedConnectionRequest(message);
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      //TODO REQUEST_CONFIGURATON_TASK
      // seed another peer who requests the locations of the nomadic singleton agents.
      // handle other operations ...
    }
    // otherwise, the message is not understood
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
  public Message converseMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations
    return (notUnderstoodMessage(message));
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
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.SEED_CONNECTION_REQUEST_INFO,
      AHCSConstants.TASK_ACCOMPLISHED_INFO};
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
   * Initializes the seed node information.
   *
   * @param message the received initialization task message
   */
  @SuppressWarnings({"unchecked"})
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    LOGGER.info("initializing the seed node information ...");

    // deserialize the set of SeedNodeInfo objects from the specified file
    final String seedNodeInfosFilePath = "data/SeedNodeInfos.ser";
    X509Utils.verifyFileHash(
            seedNodeInfosFilePath, // filePath
            seedNodeInfosFileHashString); // fileHashString
    try {
      try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(seedNodeInfosFilePath))) {
        seedNodesInfos = (Set<SeedNodeInfo>) objectInputStream.readObject();
      }
    } catch (IOException | ClassNotFoundException ex) {
      throw new TexaiException("cannot find " + seedNodeInfosFilePath);
    }

    final AtomicBoolean isSeedNode = new AtomicBoolean(false);
    LOGGER.info("the locations and credentials of the network seed nodes ...");
    seedNodesInfos.stream().forEach(seedNodeInfo -> {
      if (getContainerName().equals(Node.extractContainerName(seedNodeInfo.getQualifiedName()))) {
        LOGGER.info("  " + seedNodeInfo + " - (me)");
        isSeedNode.set(true);
      } else {
        LOGGER.info("  " + seedNodeInfo + " connecting ...");




        // create a separate key store for the peer certificates

        // create a dictionary of their security infos so channels can be openned

//        final X509SecurityInfo x509SecurityInfo = ((NodeRuntime) getNodeRuntime()).getNodeRuntimeSkill().getRole().
//
//        ((NodeRuntime) getNodeRuntime()).openChannelToPeerContainer(
//                Node.extractContainerName(seedNodeInfo.getQualifiedName()), // containerName,
//                seedNodeInfo.getInetAddress().getHostAddress(), // hostName,
//                seedNodeInfo.getPort(), // port,
//                x509SecurityInfo);

        //compose and send a message to the seed peer
        final Message connectionRequestMessage = makeMessage(
                seedNodeInfo.getQualifiedName(), // recipientQualifiedName
                getClassName(), // recipientService
                AHCSConstants.SEED_CONNECTION_REQUEST_INFO, // operation
                new HashMap<>()); // parameterDictionary
        sendMessageViaSeparateThread(connectionRequestMessage);
      }
    });
  }

  /**
   * Process the received connection request by responding with the current network configuration.
   *
   * @param message the received seed connection request message
   */
  private void seedConnectionRequest(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("received a seed connection request from " + message.getSenderContainerName());
  }

  /**
   * Perform this role's mission, which is to configure the roles in this container whose parents are nomadic singleton roles hosted on a
   * probably remote super-peer.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");

  }
}
