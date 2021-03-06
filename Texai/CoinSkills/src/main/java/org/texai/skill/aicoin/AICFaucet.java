package org.texai.skill.aicoin;

import java.io.Serializable;
import java.math.BigInteger;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.tamperEvidentLogs.TELogAccess;
import org.texai.util.StringUtils;

/**
 * AICFaucet.java

 Description: Provides a tamper evident aicoin faucet. A faucet is an internet web site or API that enables a new wallet user to
 * immediately receive a small amount of aicoin at no charge. Abuse is prevented by recording the IP address and usage.
 *
 * Copyright (C) Dec 31, 2014, Stephen L. Reed, Texai.org.
 */
@ThreadSafe
public class AICFaucet extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICFaucet.class);
  // the name of the aicoin faucet payments tamper-evident log
  private static final String AIC_FAUCET_PAYMENTS = "AICFaucetPayments";
  // the tamper-evident log access object
  private TELogAccess teLogAccess;
  // the maximum total amount of aicoin payments to a single user, 5 AIC
  private final BigInteger AMOUNT_CLAIMED_LIMIT = new BigInteger("500000000");

  /**
   * Creates a new instance of AICFaucet.
   */
  public AICFaucet() {
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
       * This task message is sent from the parent AICNetworkOperationAgent.AICNetworkOperationRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        teLogAccess  = new TELogAccess(getRDFEntityManager());
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
       * This task message is sent from the network-singleton, parent AICNetworkOperationAgent.AICNetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
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

  }

  //TODO for production, add an SMS API and use the mobile number as the key
  // http://www.cdyne.com/pricing/default.aspx?product=1
  /**
   * Process an A.I. Coin faucet payment request. This method is synchronized to make a critical section in which the faucet payment can be
   * made without conflict from another simultaneous request.
   *
   * @param message the received faucet payment request sensation message
   */
//  private synchronized void faucetPaymentRequest(final Message message) {
//    //Preconditio
//    assert message != null : "message must not be null";
//    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
//
//    LOGGER.info("processing a faucet payment request");
//    final BigInteger requestedFaucetPaymentAmount = (BigInteger) message.get(AHCSConstants.MSG_PARM_CURRENCY_AMOUNT);
//    assert requestedFaucetPaymentAmount != null;
//    assert requestedFaucetPaymentAmount.compareTo(BigInteger.ZERO) > 0;
//    LOGGER.info("requested faucet payment amount: " + requestedFaucetPaymentAmount);
//    final String ipAddressString = (String) message.get(AHCSConstants.MSG_PARM_IP_ADDRESS_STRING);
//    assert StringUtils.isNonEmptyString(ipAddressString);
//    final BigInteger unclaimedAmount = getUnclaimedAmount(ipAddressString);
//    LOGGER.info("faucet payment unclaimed amount: " + unclaimedAmount);
//
//    final Message replyMessage;
//    if (isFaucetExhausted(requestedFaucetPaymentAmount)) {
//      // not enough remaining in the faucet to grant the request
//      replyMessage = makeMessage(
//              message.getSenderQualifiedName(), // recipientQualifiedName
//              message.getSenderService(), // recipientService
//              AHCSConstants.FAUCET_EXHAUSTED_TASK); // operation
//      replyMessage.put(AHCSConstants.FAUCET_PAYMENT_REQUEST_REFUSED_TASK_UNCLAIMED_CURRENCY_AMOUNT, unclaimedAmount);
//    } else if (unclaimedAmount.compareTo(requestedFaucetPaymentAmount) >= 0) {
//      // enough remaining, make the faucet payment
//      replyMessage = makeMessage(
//              message.getSenderQualifiedName(), // recipientQualifiedName
//              message.getSenderService(), // recipientService
//              AHCSConstants.FAUCET_PAYMENT_REQUEST_GRANTED_TASK); // operation
//      replyMessage.put(AHCSConstants.FAUCET_PAYMENT_REQUEST_GRANTED_TASK_UNCLAIMED_CURRENCY_AMOUNT, unclaimedAmount);
//
//    } else {
//      // not enough remaining - refuse the payment request
//      replyMessage = makeMessage(
//              message.getSenderQualifiedName(), // recipientQualifiedName
//              message.getSenderService(), // recipientService
//              AHCSConstants.FAUCET_PAYMENT_REQUEST_REFUSED_TASK); // operation
//      replyMessage.put(AHCSConstants.FAUCET_PAYMENT_REQUEST_REFUSED_TASK_UNCLAIMED_CURRENCY_AMOUNT, unclaimedAmount);
//    }
//    replyMessage.put(AHCSConstants.MSG_PARM_CURRENCY_AMOUNT, requestedFaucetPaymentAmount);
//    assert message.get(AHCSConstants.MSG_PARM_SESSION) != null;
//    replyMessage.put(AHCSConstants.MSG_PARM_SESSION, message.get(AHCSConstants.MSG_PARM_SESSION));
//    sendMessage(replyMessage);
//  }

  /**
   * Returns the current unclaimed amount for the given IP address.
   *
   * @param key the given IP address
   *
   * @return the unclaimed amount in Satoshis
   */
  protected BigInteger getUnclaimedAmount(final String key) {
    //Preconditions
    assert StringUtils.isNonEmptyString(key) : "key must be a non-empty string";
    assert teLogAccess != null : "teLogAccess must not be null";

    if (teLogAccess.findTELogHeader(AIC_FAUCET_PAYMENTS) == null) {
      teLogAccess.createTELogHeader(AIC_FAUCET_PAYMENTS);
    }
    final UsersFaucetConsumption usersFaucetConsumptionPrevious
            = (UsersFaucetConsumption) teLogAccess.findTEKeyedLogItem(AIC_FAUCET_PAYMENTS, key);
    BigInteger totalAmountClaimed;
    if (usersFaucetConsumptionPrevious == null) {
      totalAmountClaimed = BigInteger.ZERO;
    } else {
      totalAmountClaimed = usersFaucetConsumptionPrevious.totalAmountClaimed;
    }
    LOGGER.info("total amount claimed: " + totalAmountClaimed);
    if (totalAmountClaimed.compareTo(AMOUNT_CLAIMED_LIMIT) < 0) {
      return AMOUNT_CLAIMED_LIMIT.subtract(totalAmountClaimed);
    } else {
      return BigInteger.ZERO;
    }
  }

  /**
   * Returns whether the faucet wallet has sufficent funds to pay the requested payment amount.
   *
   * @param requestedFaucetPaymentAmount the requested faucet payment amount
   *
   * @return whether the faucet wallet has sufficent funds
   */
//  private boolean isFaucetExhausted(final BigInteger requestedFaucetPaymentAmount) {
//    //Preconditions
//    assert requestedFaucetPaymentAmount != null;
//    assert requestedFaucetPaymentAmount.compareTo(BigInteger.ZERO) > 0;
//
//    //TODO
//    return true;
//  }

  /** Converts an amount from the aicoind or aicoin-qt RPC interface to satoshis.
   *
   * @param value the RPC amount as a double, e.g. 1 AIC = 1.00000000
   * @return the amount as satoshis, e.g. 1 XAH = 100000000 satoshis
   */
  public BigInteger JSONtoAmount(final double value) {
    return new BigInteger(Long.toString((long) (value * 100000000L)));
  }

  /**
   * Contains faucet consumption for a single user.
   */
  @Immutable
  static class UsersFaucetConsumption implements Serializable {

    // the serial version UID
    private static final long serialVersionUID = 1L;
    // the IP address
    final String ipAddressString;
    // the total amount of aicoin satoshis claimed by the user
    final BigInteger totalAmountClaimed;

    /**
     * Create a new UsersFaucetConsumption instance.
     *
     * @param ipAddressString the IP address
     * @param totalAmountClaimed the total amount of aicoin satoshis claimed by the user
     */
    UsersFaucetConsumption(
            final String ipAddressString,
            final BigInteger totalAmountClaimed) {
      //Preconditions
      assert StringUtils.isNonEmptyString(ipAddressString) : "ipAddressString must be a non-empty string";
      assert totalAmountClaimed != null : "totalAmountClaimed must not be null";

      this.ipAddressString = ipAddressString;
      this.totalAmountClaimed = totalAmountClaimed;
    }
  }

}
