/*
 * Heartbeat.java
 *
 * Created on May 6, 2010, 10:10:33 AM
 *
 * Description: Provides heartbeat behavior, in which remote role communication leases are periodically
 * renewed.
 *
 * Copyright (C) May 6, 2010, Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
 * Provides heartbeat behavior, in which remote role communication leases are
 * periodically renewed.
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
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.AHCS_INITIALIZE_TASK:
        initialization(message);
        setSkillState(AHCSConstants.State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        ready();
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      case AHCSConstants.AHCS_SHUTDOWN_TASK:
        finalization();
        return true;
    }

    // otherwise the received message is not understood
    sendMessage(notUnderstoodMessage(message));
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
    // otherwise the received message is not understood
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
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.AHCS_SHUTDOWN_TASK
    };
  }

  /**
   * Initializes this skill.
   *
   * @param message the initialization message
   */
  protected void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing " + toString() + " in role " + getRole());
    }

    // all nodes send a heartbeat to the parent heartbeat role, 
    outboundParentHeartbeatInfo = new OutboundHeartbeatInfo(
            getRole().getParentQualifiedName(), // recipientQualifiedName
            "org.texai.skill.heartbeat.ContainerHeartbeat", // service
            this); // heartbeat
  }

  /**
   * Begins processing this skill.
   */
  private void ready() {
    //Preconditions
    assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("ready " + this);
    }

    // create a timer that periodically reviews the outbound heartbeat information objects
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.scheduleAtFixedRate(
              new HeartbeatProcessor(this), // task
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS, // delay
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS); // period
    }
    setSkillState(State.READY);
  }

  /**
   * Perform this role's mission, which is to manage the network, the
   * containers, and the TexaiCoin agents within the containers.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("performing the mission");
    final Message performMissionMessage = new Message(
            getRole().getQualifiedName(), // senderQualifiedName
            getClassName(), // senderService,
            getRole().getChildQualifiedNameForAgent("XAINetworkOperationAgent"), // recipientQualifiedName,
            "org.texai.skill.aicoin.XAINetworkOperation", // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    sendMessage(performMissionMessage);
  }

  /**
   * Finalizes this skill and releases its resources.
   */
  private void finalization() {
  }

  /**
   * Periodically processes the outbound and inbound heartbeat information
   * objects.
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
              heartbeat.getRole().getQualifiedName(), // senderQualifiedName
              heartbeat.getClass().getName(), // senderService
              outboundHeartbeatInfo.recipentQualifiedName,
              outboundHeartbeatInfo.service,
              AHCSConstants.KEEP_ALIVE_INFO); // operation
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  dispatching keep-alive message");
        LOGGER.debug("    from " + heartbeat.getRole().getQualifiedName());
        LOGGER.debug("    to " + outboundHeartbeatInfo.recipentQualifiedName);
      }
      sendMessage(message);

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
