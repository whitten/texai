/*
 * ContainerHeartbeat.java
 *
 * Created on Sep 18, 2014, 8:04:36 AM
 *
 * Description: Manages a container's agents to ensure liveness.
 *
 * Copyright (C) 2014 Stephen L. Reed
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.texai.skill.heartbeat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.StringUtils;

/**
 * Manages a container's agents to ensure liveness.
 *
 * @author reed
 */
@ThreadSafe
public final class ContainerHeartbeat extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerHeartbeat.class);
  // heartbeat roles from which keep-alive messages are periodically expected
  private final Map<String, InboundHeartbeatInfo> inboundHeartbeatInfos = new HashMap<>();
  // the outbound parent heartbeat information
  private OutboundHeartbeatInfo outboundParentHeartbeatInfo;
  // the outbound heartbeat period milliseconds, which indicates how often to send
  private final static long OUTBOUND_HEARTBEAT_PERIOD_MILLIS = 30_000;
  // the inbound heartbeat period milliseconds, which indicates the duration beyond which the expected heartbeat is considered missing
  private final static long INBOUND_HEARTBEAT_PERIOD_MILLIS = 300_000;

  /**
   * Constructs a new ContainerHeartbeat instance.
   */
  public ContainerHeartbeat() {
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
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(message);
        // initialize child heartbeat roles
        propagateOperationToChildRoles(operation);
        setSkillState(AHCSConstants.State.READY);
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      case AHCSConstants.KEEP_ALIVE_INFO:
        recordKeepAlive(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;
    }

    // other operations ...
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
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.KEEP_ALIVE_INFO};
  }

  /**
   * Initializes this skill.
   *
   * @param message the initialization message
   */
  protected void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing " + toString() + " in role " + getRole());
    }

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

//    LOGGER.info("performing the mission");
//    final Message performMissionMessage = new Message(
//            getRole().getQualifiedName(), // senderQualifiedName
//            getClassName(), // senderService,
//            getRole().getChildQualifiedNameForAgent("XAINetworkOperationAgent"), // recipientQualifiedName,
//            "org.texai.skill.aicoin.XAINetworkOperation", // recipientService
//            AHCSConstants.PERFORM_MISSION_TASK); // operation
//    sendMessage(performMissionMessage);
  }

  /**
   * Records a keep-alive message sent from a role.
   *
   * @param message the keep-alive message
   */
  protected void recordKeepAlive(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "must be in the ready state";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("recording keep-alive " + message);
    }
    final String senderQualifiedName = message.getSenderQualifiedName();
    InboundHeartbeatInfo inboundHeartBeatInfo = inboundHeartbeatInfos.get(senderQualifiedName);
    if (inboundHeartBeatInfo == null) {
      inboundHeartBeatInfo = new InboundHeartbeatInfo(senderQualifiedName);
      inboundHeartbeatInfos.put(senderQualifiedName, inboundHeartBeatInfo);
    }
    inboundHeartBeatInfo.heartbeatReceivedMillis = System.currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  received keep-alive message " + (new DateTime()).toString("MM/dd/yyyy hh:mm a"));
      LOGGER.debug("    from " + senderQualifiedName);
      LOGGER.debug("    to " + getRole().getQualifiedName());
    }
  }

  /**
   * Periodically processes the outbound and inbound heartbeat information
   * objects.
   */
  protected class HeartbeatProcessor extends TimerTask {

    /**
     * the container heartbeat skill
     */
    private final ContainerHeartbeat containerHeartbeat;

    /**
     * Constructs a new HeartbeatProcessor instance.
     *
     * @param containerHeartbeat the heartbeat skill
     */
    HeartbeatProcessor(final ContainerHeartbeat containerHeartbeat) {
      //Preconditions
      assert containerHeartbeat != null : "heartbeat must not be null";

      this.containerHeartbeat = containerHeartbeat;
    }

    /**
     * Processes the outbound and inbound heartbeat information objects.
     */
    @Override
    public void run() {
      final long currentTimeMillis = System.currentTimeMillis();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(containerHeartbeat + " HeartbeatProcessor ...");
      }

      // notice if any expected heartbeats are timed-out
      final long inboundHeartbeatReceivedThresholdMillis = currentTimeMillis - INBOUND_HEARTBEAT_PERIOD_MILLIS;
      synchronized (inboundHeartbeatInfos) {
        inboundHeartbeatInfos.values().stream().map((inboundHeartbeatInfo) -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  " + inboundHeartbeatInfo);
          }
          return inboundHeartbeatInfo;
        }).filter((inboundHeartbeatInfo) -> (inboundHeartbeatInfo.heartbeatReceivedMillis < inboundHeartbeatReceivedThresholdMillis)).forEach((inboundHeartbeatInfo) -> {
          missingHeartbeat(inboundHeartbeatInfo);
        });
      }

      //TODO work out the message sent to the parent role
      if (outboundParentHeartbeatInfo != null) {
        // send heartbeats after waiting at least the specified duration
        final long outboundHeartbeatReceivedThresholdMillis = currentTimeMillis - OUTBOUND_HEARTBEAT_PERIOD_MILLIS;
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("  " + outboundParentHeartbeatInfo);
        }
        if (outboundParentHeartbeatInfo.heartbeatSentMillis < outboundHeartbeatReceivedThresholdMillis) {
          sendHeartbeat(outboundParentHeartbeatInfo, containerHeartbeat);
        }
      }
    }
  }

  /**
   * Provides a container for inbound heartbeat information.
   */
  protected static class InboundHeartbeatInfo {

    /**
     * the heartbeat sender's qualified name
     */
    protected final String senderQualifiedName;
    /**
     * the millisecond time at which the most recent heartbeat was received
     */
    protected long heartbeatReceivedMillis;

    /**
     * Constructs a new InboundHeartbeatInfo instance.
     *
     * @param senderQualifiedName the heartbeat sender's qualified name
     */
    InboundHeartbeatInfo(final String senderQualifiedName) {
      //Preconditions
      assert StringUtils.isNonEmptyString(senderQualifiedName) : "senderQualifiedName must be a non-empty string";

      this.senderQualifiedName = senderQualifiedName;
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
      final InboundHeartbeatInfo other = (InboundHeartbeatInfo) obj;
      return Objects.equals(this.senderQualifiedName, other.senderQualifiedName);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 83 * hash + Objects.hashCode(this.senderQualifiedName);
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
      stringBuilder.append("[Inbound heartbeat, role: ");
      stringBuilder.append(senderQualifiedName);
      if (heartbeatReceivedMillis > 0) {
        stringBuilder.append(", last received ");
        final long secondsAgo = (System.currentTimeMillis() - heartbeatReceivedMillis) / 1000;
        stringBuilder.append(secondsAgo);
        stringBuilder.append(" seconds ago]");
      }
      return stringBuilder.toString();
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

  /**
   * Notices that an expected heartbeat message has not yet been received from
   * the given role.
   *
   * @param inboundHeartbeatInfo the given role heartbeat information
   */
  protected void missingHeartbeat(final InboundHeartbeatInfo inboundHeartbeatInfo) {
    //Preconditions
    assert inboundHeartbeatInfo != null : "inboundHeartbeatInfo must not be null";

    LOGGER.warn("heartbeat missing for " + inboundHeartbeatInfo);
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
