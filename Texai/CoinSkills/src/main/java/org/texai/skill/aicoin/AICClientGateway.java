package org.texai.skill.aicoin;

import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.texai.ahcs.NodeRuntime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.skill.aicoin.support.BitcoinProtocolMessageHandler;
import org.texai.skill.aicoin.support.BitcoinProtocolMessageHandlerFactory;

/**
 * Created on Aug 29, 2014, 6:48:25 PM.
 *
 * Description: Provides a wallet and processor client gateway for the A.I. Coin network.
 *
 * Copyright (C) Aug 29, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
 *
 * Copyright (C) 2014 Texai
 */
@ThreadSafe
public final class AICClientGateway extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICClientGateway.class);
  // the Bitcoin protocol message handler dictionary, channel --> Bitcoin protocol message handler
  private final Map<Channel, BitcoinProtocolMessageHandler> bitcoinProtocolMessageHandlerDictionary = new HashMap<>();

  /**
   * Constructs a new AICClientGateway instance.
   */
  public AICClientGateway() {
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
       * This task message is sent from the container-local parent AICNetworkOperationAgent.AICNetworkOperationRole. It is expected to be
       * the first task message that this role receives and it results in the role being initialized.
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
       * This task message is sent from the network-singleton, parent AICNetworkOperationAgent.AICNetworkOperationRole. It indicates that
       * the parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton parent AICNetworkOperationAgent.AICNetworkOperationRole.
       *
       * It results in the skill set to the ready state, and the skill performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
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
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK
    };
  }

  /**
   * Perform this role's mission, which is to be a gateway for client connections into the network.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";
    assert NodeRuntime.class.isAssignableFrom(getNodeRuntime().getClass()) : "must be NodeRuntime";

  }

  /** Provides a client listener runnable that waits 15 seconds before starting the listener socket. */
  static class ClientListenerRunnable implements Runnable {

    // the AIC client gateway skill

    final AICClientGateway aicClientGateway;

    ClientListenerRunnable(final AICClientGateway aicClientGateway) {
      //Preconditions
      assert aicClientGateway != null : "aicClientGateway must not be null";

      this.aicClientGateway = aicClientGateway;
    }

    @Override
    public void run() {
      // wait 30 seconds for the bitcoind (aicoind) to initialize after AICOperation launches it.
      try {
        Thread.sleep(15_000);
      } catch (InterruptedException ex) {
        // ignore
      }
      // start listening for connection requests from clients
      final BitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory
              = new BitcoinProtocolMessageHandlerFactory((NodeRuntime) aicClientGateway.getNodeRuntime());
      bitcoinProtocolMessageHandlerFactory.openBitcoinProtocolListeningSocket();
    }
  }

}
