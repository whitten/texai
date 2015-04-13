/*
 * AICNetworkOperation.java
 *
 * Created on Sep 18, 2014, 8:10:02 AM
 *
 * Description: Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * Copyright (C) 2014 Stephen L. Reed
 *
 */
package org.texai.skill.aicoin;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.util.StringUtils;

/**
 * Manages the network, the containers, and the A.I. Coin agents within the containers. Interacts with human operators.
 *
 * @author reed
 */
@ThreadSafe
public final class AICNetworkOperation extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICNetworkOperation.class);
  // the timer
  private final Timer mintTimer;
  // name of the container last commanded to generate a new block
  private String previousMintContainerName;

  /**
   * Constructs a new XTCNetworkOperation instance.
   */
  public AICNetworkOperation() {
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
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
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
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        propagateOperationToChildRoles(receivedMessage);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(receivedMessage);
        return;

      /**
       * Join Network Singleton Agent Info
       *
       * This task message is sent to this network singleton agent/role from a child role in another container.
       *
       * The sender is requesting to join the network as child of this role.
       *
       * The message parameter is the X.509 certificate belonging to the sender agent / role.
       *
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(receivedMessage);
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
        handleDelegatePerformMissionTask(receivedMessage);
        return;

      /**
       * Restart Container Task
       *
       * This message is sent the parent AICNetworkOperationAgent.AICNetworkOperationRole instructing the container to restart following a
       * given delay.
       *
       * As a result, a Shutdown Aicoind Task is sent to each child AICOperationRole.
       */
      case AHCSConstants.RESTART_CONTAINER_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleRestartContainerTask(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
      // handle other operations ...
    }

    sendDoNotUnderstandInfoMessage(receivedMessage);
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
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.RESTART_CONTAINER_TASK
    };
  }

  /**
   * Perform this role's mission, which is to manage the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    LOGGER.info("performing the mission");
    propagateOperationToChildRolesSeparateThreads(receivedMessage);
    mintNewBlocksEvery10Minutes();
  }

  /**
   * Handles the received Restart Container Task by sending a Shutdown Aicoind Task to each child AICOperationRole.
   *
   * @param receivedMessage the received Restart Container Task message
   */
  private void handleRestartContainerTask(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";

    // send the restart container task to every child container operation role.
    if (LOGGER.isDebugEnabled()) {
      final List<String> childQualifiedNames = new ArrayList<>(getRole().getChildQualifiedNames());
      LOGGER.debug("childQualifiedNames...");
      childQualifiedNames.stream().sorted().forEach((String childQualifiedName) -> {
        LOGGER.debug("  " + childQualifiedName);
      });
    }
    getRole().getChildQualifiedNamesForAgent("AICOperationAgent").forEach((String childQualifiedName) -> {
      final Message restartContainerTaskMessage2 = new Message(
              getQualifiedName(), // senderQualifiedName
              getClassName(), // senderService
              childQualifiedName, // recipientQualifiedName
              AICOperation.class.getName(), // recipientService
              AHCSConstants.SHUTDOWN_AICOIND_TASK); // operation
      sendMessage(receivedMessage, restartContainerTaskMessage2);
    });
  }

  /**
   * Mints a new Bitcoin block every 10 minutes.
   */
  private void mintNewBlocksEvery10Minutes() {
    LOGGER.info("mint new blocks");

    // calculate the milliseconds delay until the next 10 minute mark ...
    final Calendar calendar = Calendar.getInstance();
    final int minutes = calendar.get(Calendar.MINUTE);
    final int modulo10minutes = minutes % 10;
    long delay = (10 - modulo10minutes) * 60000L;
    assert delay <= 600000;

    mintTimer.scheduleAtFixedRate(
            new MintTimerTask(this), // task
            delay,
            600000l); // period - 10 minutes
    //60000l); // period - 1 minute
  }

  /**
   * Provides a mint timer task
   */
  static class MintTimerTask extends TimerTask {

    // the AIC network operation skill
    private final AICNetworkOperation aicNetworkOperation;

    /**
     * Create a new MintTimerTask instance.
     *
     * @param aicNetworkOperation the AIC network operation skill
     */
    MintTimerTask(final AICNetworkOperation aicNetworkOperation) {
      //Preconditions
      assert aicNetworkOperation != null : "aicNetworkOperation must not be null";

      this.aicNetworkOperation = aicNetworkOperation;
    }

    /**
     * Executes this timer task
     */
    @Override
    public void run() {
      // use a runable so that an exception does not kill the timer thread
      final GenerateNewBlockRunnable generateNewBlockRunnable = new GenerateNewBlockRunnable(aicNetworkOperation);
      aicNetworkOperation.execute(generateNewBlockRunnable);
    }
  }

  protected static class GenerateNewBlockRunnable implements Runnable {

    // the AIC network operation skill

    private final AICNetworkOperation aicNetworkOperation;

    /**
     * Create a new GenerateNewBlockRunnable instance.
     *
     * @param aicNetworkOperation the AIC network operation skill
     */
    GenerateNewBlockRunnable(final AICNetworkOperation aicNetworkOperation) {
      //Preconditions
      assert aicNetworkOperation != null : "aicNetworkOperation must not be null";

      this.aicNetworkOperation = aicNetworkOperation;
    }

    /**
     * Sends a task message to the next super peer for round-robin minting.
     */
    @Override
    public void run() {
      final List<ContainerNameRingItem> superPeerContainerNameRing = aicNetworkOperation.createSuperPeerContainerNameRing();
      String mintContainerName = null;
      for (final ContainerNameRingItem containerNameRingItem : superPeerContainerNameRing) {
        if (containerNameRingItem.containerName.equals(aicNetworkOperation.previousMintContainerName)) {
          mintContainerName = containerNameRingItem.followingContainerContainerNameRingItem.containerName;
        }
      }
      if (mintContainerName == null) {
        mintContainerName = superPeerContainerNameRing.get(0).containerName;
      }
      final Message generateCoinBlockTaskMessage = aicNetworkOperation.makeMessage(
              mintContainerName + ".AICMintAgent.AICMintRole",
              AICMint.class.getName(),
              AHCSConstants.GENERATE_COIN_BLOCK_TASK); // operation
      aicNetworkOperation.sendMessageViaSeparateThread(
              null, // receivedMessage
              generateCoinBlockTaskMessage); // message
      aicNetworkOperation.previousMintContainerName = mintContainerName;
    }
  }

  /**
   * Creates a ring of the super peers eligible to mint.
   *
   * @return the list of ring items
   */
  protected List<ContainerNameRingItem> createSuperPeerContainerNameRing() {
    final List<String> superPeerContainerNames = new ArrayList<>();
    LOGGER.info(getNodeRuntime().getContainerInfos());
    getNodeRuntime().getContainerInfos().stream().forEach((ContainerInfo containerInfo) -> {
      if (containerInfo.isAlive() && containerInfo.isSuperPeer()) {
        superPeerContainerNames.add(containerInfo.getContainerName());
      }
      if (superPeerContainerNames.isEmpty()) {
        LOGGER.info("*****************************************");
        LOGGER.info("NO SUPER PEERS FOR MINTING");
        LOGGER.info("*****************************************");
        //TODO escalate to network operation
      }
    });

    LOGGER.info(superPeerContainerNames);
    final List<ContainerNameRingItem> containerNameRingItems = new ArrayList<>();
    final int superPeerContainerNames_size = superPeerContainerNames.size();
    for (int i = 0; i < superPeerContainerNames_size; i++) {
      containerNameRingItems.add(new ContainerNameRingItem(superPeerContainerNames.get(i)));
    }
    if (superPeerContainerNames_size == 1) {
      containerNameRingItems.get(0).followingContainerContainerNameRingItem = containerNameRingItems.get(0);
    } else {
      for (int i = 0; i < superPeerContainerNames_size; i++) {
        if (i == 0) {
          containerNameRingItems.get(i).followingContainerContainerNameRingItem
                  = containerNameRingItems.get(i + 1);
        } else if (i == (superPeerContainerNames_size - 1)) {
          containerNameRingItems.get(i).followingContainerContainerNameRingItem
                  = containerNameRingItems.get(0);
        } else {
          containerNameRingItems.get(i).followingContainerContainerNameRingItem
                  = containerNameRingItems.get(i + 1);
        }
      }
    }
    return containerNameRingItems;
  }

  /**
   * Provides an item in the ring of candidate mint super peers.
   */
  static class ContainerNameRingItem {

    // the container name
    private final String containerName;
    // the following container name in the ring
    private ContainerNameRingItem followingContainerContainerNameRingItem;

    ContainerNameRingItem(final String containerName) {
      //Preconditions
      assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

      this.containerName = containerName;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      return (new StringBuilder())
              .append('[')
              .append(containerName)
              .append(']')
              .toString();
    }
  }
}
