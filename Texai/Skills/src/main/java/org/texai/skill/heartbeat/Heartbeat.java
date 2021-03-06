/*
 * Heartbeat.java
 *
 * Created on May 6, 2010, 10:10:33 AM
 *
 * Description: Provides heartbeat behavior, in which remote role communication leases are periodically
 * renewed.
 *
 * Copyright (C) May 6, 2010, Stephen L. Reed.
 */
package org.texai.skill.heartbeat;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.StringUtils;

/**
 * Provides heartbeat behavior, in which remote role communication leases are periodically renewed.
 *
 * @author reed
 */
@ThreadSafe
public class Heartbeat extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(Heartbeat.class);
  // the outbound parent heartbeat information
  private OutboundHeartbeatInfo outboundParentHeartbeatInfo;
  // the outbound heartbeat period milliseconds, which indicates how often to send
  private final static long OUTBOUND_HEARTBEAT_PERIOD_MILLIS = 60_000;

  /**
   * Constructs a new Heartbeat instance.
   */
  public Heartbeat() {
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
  public void receiveMessage(final Message receivedMessage) {
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
       * This task message is sent from the parent ContainerOperationAgent.ContainerHeartbeatRole. It is expected to be the first task
       * message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton parent ContainerOperationAgent.ContainerHeartbeatRole.
       *
       * It results in the skill set to the ready state, and the skill performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info(getQualifiedName() + " now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(receivedMessage);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;

    }

    // otherwise the received message is not understood
    sendMessage(
            receivedMessage,
            Message.notUnderstoodMessage(
            receivedMessage, // receivedMessage
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
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK,};
  }

  /**
   * Initializes this skill.
   *
   * @param message the initialization message
   */
  protected void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing " + toString() + " in role " + getRole());
    }

    // all nodes send a heartbeat to the parent heartbeat role,
    outboundParentHeartbeatInfo = new OutboundHeartbeatInfo(
            getRole().getParentQualifiedName(), // recipientQualifiedName
            "org.texai.skill.heartbeat.ContainerHeartbeat", // service
            this); // heartbeat

    // create a timer that periodically reviews the outbound heartbeat information objects
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.scheduleAtFixedRate(
              new HeartbeatProcessor(this), // task
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS, // delay
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS); // period
    }
    if (getNodeRuntime().isFirstContainerInNetwork()) {
      setSkillState(AHCSConstants.State.READY);
    } else {
      setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
    }
  }

  /**
   * Perform this role's mission.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getChildQualifiedNames().isEmpty() : "must not have child roles";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("performing the mission");
    }
  }

  /**
   * Periodically processes the outbound and inbound heartbeat information objects.
   */
  protected class HeartbeatProcessor extends TimerTask {

    /**
     * the heartbeat skill
     */
    private final Heartbeat heartbeat;

    /**
     * Constructs a new HeartbeatProcessor instance.
     *
     * @param heartbeat the heartbeat skill
     */
    HeartbeatProcessor(final Heartbeat heartbeat) {
      //Preconditions
      assert heartbeat != null : "heartbeat must not be null";

      this.heartbeat = heartbeat;
    }

    /**
     * Processes the outbound information objects.
     */
    @Override
    public void run() {
      final long currentTimeMillis = System.currentTimeMillis();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(heartbeat + " HeartbeatProcessor ...");
      }
      if (!heartbeat.getSkillState().equals(State.READY)) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("No heartbeat because this skill has not yet joined the network.");
        }
        return;
      }

      // send heartbeats after waiting at least the specified duration
      final long outboundHeartbeatReceivedThresholdMillis = currentTimeMillis - OUTBOUND_HEARTBEAT_PERIOD_MILLIS;
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  " + outboundParentHeartbeatInfo);
      }
      if (outboundParentHeartbeatInfo.heartbeatSentMillis < outboundHeartbeatReceivedThresholdMillis) {
        sendHeartbeat(outboundParentHeartbeatInfo, heartbeat);
      }
    }

    /**
     * Sends a heartbeat message to the given role.
     *
     * @param outboundHeartbeatInfo the given role heartbeat information
     * @param heartbeat the heartbeat skill
     */
    protected void sendHeartbeat(
            final OutboundHeartbeatInfo outboundHeartbeatInfo,
            final AbstractSkill heartbeat) {
      //Preconditions
      assert outboundHeartbeatInfo != null : "inboundHeartbeatInfo must not be null";
      assert heartbeat != null : "heartbeat must not be null";

      final Message message = new Message(
              heartbeat.getQualifiedName(), // senderQualifiedName
              heartbeat.getClass().getName(), // senderService
              outboundHeartbeatInfo.recipentQualifiedName,
              outboundHeartbeatInfo.service,
              AHCSConstants.KEEP_ALIVE_INFO); // operation
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  dispatching keep-alive message");
        LOGGER.debug("    from " + heartbeat.getQualifiedName());
        LOGGER.debug("    to " + outboundHeartbeatInfo.recipentQualifiedName);
      }
      sendMessageViaSeparateThread(
              null, // receivedMessage, which is null because this is triggered by a timer
              message);

      outboundHeartbeatInfo.heartbeatSentMillis = System.currentTimeMillis();
    }
  }

  /**
   * Provides a container for outbound heartbeat information.
   */
  protected static class OutboundHeartbeatInfo {

    /**
     * the heartbeat recipient's qualified name
     */
    protected final String recipentQualifiedName;
    /**
     * the millisecond time at which the most recent heartbeat was sent
     */
    protected long heartbeatSentMillis;
    /**
     * the recipient's service (skill class)
     */
    protected final String service;
    /**
     * the heartbeat
     */
    protected final Heartbeat heartbeat;

    /**
     * Constructs a new OutboundHeartbeatInfo instance.
     *
     * @param recipentQualifiedName the heartbeat recipient's qualified name
     * @param service the service, or null if not specified
     * @param heartbeat the heartbeat skill instance
     */
    OutboundHeartbeatInfo(
            final String recipentQualifiedName,
            final String service,
            final Heartbeat heartbeat) {
      //Preconditions
      assert StringUtils.isNonEmptyString(recipentQualifiedName) : "recipentQualifiedName must be a non-empty string";
      assert service != null : "service must not be null";
      assert !service.isEmpty() : "service must not be empty";
      assert heartbeat != null : "heartbeat must not be null";

      this.recipentQualifiedName = recipentQualifiedName;
      this.service = service;
      this.heartbeat = heartbeat;
    }

    /**
     * Returns whether some other object equals this one.
     *
     * @param obj the other object
     *
     * @return whether some other object equals this one
     */
    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final OutboundHeartbeatInfo other = (OutboundHeartbeatInfo) obj;
      return Objects.equals(this.recipentQualifiedName, other.recipentQualifiedName);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 89 * hash + Objects.hashCode(this.recipentQualifiedName);
      return hash;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[Outbound heartbeat from: ");
      stringBuilder.append(heartbeat.getRole());
      if (heartbeatSentMillis > 0) {
        stringBuilder.append(", last sent ");
        final long secondsAgo = (System.currentTimeMillis() - heartbeatSentMillis) / 1000;
        stringBuilder.append(secondsAgo);
        stringBuilder.append(" seconds ago]");
      }
      return stringBuilder.toString();
    }
  }
}
