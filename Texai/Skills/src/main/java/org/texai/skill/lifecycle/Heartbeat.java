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
package org.texai.skill.lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;

/** Provides heartbeat behavior, in which remote role communication leases are periodically
 * renewed.
 *
 * @author reed
 */
@ThreadSafe
public class Heartbeat extends AbstractSkill {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Heartbeat.class);
  /** heartbeat roles to which keep-alive messages are periodically sent */
  private final Map<URI, InboundHeartbeatInfo> inboundHeartbeatInfos = new HashMap<>();
  /** heartbeat roles from which keep-alive messages are periodically expected */
  private final Map<URI, OutboundHeartbeatInfo> outboundHeartbeatInfos = new HashMap<>();
  /** the outbound heartbeat period milliseconds, which indicates how often to send */
  private final static long OUTBOUND_HEARTBEAT_PERIOD_MILLIS = 30_000;
  /** the inbound heartbeat period milliseconds, which indicates the duration beyond which the expected heartbeat is considered missing */
  private final static long INBOUND_HEARTBEAT_PERIOD_MILLIS = 300_000;

  /** Constructs a new Heartbeat instance. */
  public Heartbeat() {
  }

  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    switch (operation) {
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      case AHCSConstants.AHCS_INITIALIZE_TASK:
        initialization(message);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        ready();
        return true;

      case AHCSConstants.AHCS_SHUTDOWN_TASK:
        finalization();
        return true;

      case AHCSConstants.KEEP_ALIVE_INFO:
        recordKeepAlive(message);
        return true;
    }

    // otherwise the received message is not understood

    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
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

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
              AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
              AHCSConstants.AHCS_INITIALIZE_TASK,
              AHCSConstants.AHCS_READY_TASK,
              AHCSConstants.AHCS_SHUTDOWN_TASK,
              AHCSConstants.KEEP_ALIVE_INFO
            };
  }

  /** Initializes this skill. */
  private void initialization(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert this.getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";

    LOGGER.info("initializing " + toString() + " in role " + getRole());

    if (getRole().getNode().getNodeTypeName().equals(AHCSConstants.TOP_FRIENDSHIP_NODE_TYPE)) {
      // the top friendship node periodically sends a heartbeat to the Texai launcher
      final OutboundHeartbeatInfo outboundHeartbeatInfo = new OutboundHeartbeatInfo(
              getRole().getNodeRuntime().getLauncherRoleId(), // roleId
              "org.texai.texaiLauncher.TexaiLauncher", // service
              this); // heartbeat
      outboundHeartbeatInfos.put(outboundHeartbeatInfo.roleId, outboundHeartbeatInfo);
    } else {
      // all other nodes send a heartbeat to heartbeat parent role
      final OutboundHeartbeatInfo outboundHeartbeatInfo = new OutboundHeartbeatInfo(
              getRole().getParentRoleId(), // roleID
              getClassName(), // service
              this); // heartbeat
      outboundHeartbeatInfos.put(outboundHeartbeatInfo.roleId, outboundHeartbeatInfo);
    }

    // initialize child heartbeat roles
    propagateOperationToChildRoles(
            getClassName(), // service,
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation
    setSkillState(State.INITIALIZED);
  }

  /** Begins processing this skill. */
  private void ready() {
    //Preconditions
    assert this.getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";

    LOGGER.info("ready " + this);

    // create a timer that periodically reviews the inbound and outbound heartbeat information objects
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.scheduleAtFixedRate(
              new HeartbeatProcessor(this), // task
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS, // delay
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS); // period
    }
    // ready child heartbeat roles
    propagateOperationToChildRoles(
            getClassName(), // service,
            AHCSConstants.AHCS_READY_TASK); // operation
    setSkillState(State.READY);
  }

  /** Finalizes this skill and releases its resources. */
  private void finalization() {
    LOGGER.info("finalizing " + this);
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.cancel();
    }
  }

  /** Records a keep-alive message sent from a role.
   *
   * @param message the keep-alive message
   */
  private void recordKeepAlive(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(State.READY) : "must be in the ready state";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("recording keep-alive " + message.toString(getRole().getNodeRuntime()));
    }
    final URI senderRoleId = message.getSenderRoleId();
    InboundHeartbeatInfo inboundHeartBeatInfo = inboundHeartbeatInfos.get(senderRoleId);
    if (inboundHeartBeatInfo == null) {
      inboundHeartBeatInfo = new InboundHeartbeatInfo(senderRoleId);
      inboundHeartbeatInfos.put(senderRoleId, inboundHeartBeatInfo);
    }
    inboundHeartBeatInfo.heartbeatReceivedMillis = System.currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("  received keep-alive message " + (new DateTime()).toString("MM/dd/yyyy hh:mm a"));
      LOGGER.debug("    from " + getRoleQualifiedName(senderRoleId));
      LOGGER.debug("    to " + getRoleQualifiedName(message.getRecipientRoleId()));
    }
  }

  /** Periodically processes the outbound and inbound heartbeat information objects. */
  class HeartbeatProcessor extends TimerTask {

    /** the heartbeat skill */
    private final Heartbeat heartbeat;

    /** Constructs a new HeartbeatProcessor instance.
     *
     * @param heartbeat the heartbeat skill
     */
    HeartbeatProcessor(final Heartbeat heartbeat) {
      //Preconditions
      assert heartbeat != null : "heartbeat must not be null";

      this.heartbeat = heartbeat;
    }

    /** Processes the outbound and inbound heartbeat information objects. */
    @Override
    public void run() {
      final long currentTimeMillis = System.currentTimeMillis();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(heartbeat + " HeartbeatProcessor ...");
      }

      // notice if any expected heartbeats are timed-out
      final long inboundHeartbeatReceivedThresholdMillis = currentTimeMillis - INBOUND_HEARTBEAT_PERIOD_MILLIS;
      synchronized (inboundHeartbeatInfos) {
        for (final InboundHeartbeatInfo inboundHeartbeatInfo : inboundHeartbeatInfos.values()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  " + inboundHeartbeatInfo);
          }
          if (inboundHeartbeatInfo.heartbeatReceivedMillis < inboundHeartbeatReceivedThresholdMillis) {
            missingHeartbeat(inboundHeartbeatInfo);
          }
        }
      }

      // send heartbeats after waiting at least the specified duration
      final long outboundHeartbeatReceivedThresholdMillis = currentTimeMillis - OUTBOUND_HEARTBEAT_PERIOD_MILLIS;
      synchronized (outboundHeartbeatInfos) {
        for (final OutboundHeartbeatInfo outboundHeartbeatInfo : outboundHeartbeatInfos.values()) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("  " + outboundHeartbeatInfo);
          }
          if (outboundHeartbeatInfo.heartbeatSentMillis < outboundHeartbeatReceivedThresholdMillis) {
            sendHeartbeat(outboundHeartbeatInfo, heartbeat);
          }
        }
      }
    }

    /** Notices that an expected heartbeat message has not yet been received from the given role.
     *
     * @param inboundHeartbeatInfo the given role heartbeat information
     */
    private void missingHeartbeat(final InboundHeartbeatInfo inboundHeartbeatInfo) {
      //Preconditions
      assert inboundHeartbeatInfo != null : "inboundHeartbeatInfo must not be null";

      LOGGER.warn("heartbeat missing for " + inboundHeartbeatInfo);
    }

    /** Sends a heartbeat message to the given role.
     *
     * @param outboundHeartbeatInfo the given role heartbeat information
     */
    private void sendHeartbeat(
            final OutboundHeartbeatInfo outboundHeartbeatInfo,
            final AbstractSkill heartbeat) {
      //Preconditions
      assert outboundHeartbeatInfo != null : "inboundHeartbeatInfo must not be null";
      assert heartbeat != null : "heartbeat must not be null";

      final Message message = new Message(
              heartbeat.getRoleId(), // senderRoleId
              heartbeat.getClass().getName(), // senderService
              outboundHeartbeatInfo.roleId, // recipientRoleId
              outboundHeartbeatInfo.service,
              AHCSConstants.KEEP_ALIVE_INFO); // operation
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("  dispatching keep-alive message");
        LOGGER.debug("    from " + heartbeat.getRoleQualifiedName(message.getSenderRoleId()));
        LOGGER.debug("    to role " + heartbeat.getRoleQualifiedName(message.getRecipientRoleId()));
      }
      sendMessage(message);

      outboundHeartbeatInfo.heartbeatSentMillis = System.currentTimeMillis();
    }
  }

  /** Provides a container for inbound heartbeat information. */
  static class InboundHeartbeatInfo {

    /** the heartbeat sender's role ID */
    private final URI roleId;
    /** the millisecond time at which the most recent heartbeat was received */
    private long heartbeatReceivedMillis;

    /** Constructs a new InboundHeartbeatInfo instance.
     *
     * @param roleId the heartbeat sender's role ID
     */
    InboundHeartbeatInfo(final URI roleId) {
      //Preconditions
      assert roleId != null : "roleId must not be null";

      this.roleId = roleId;
    }

    /** Returns whether some other object equals this one.
     *
     * @param obj the other object
     * @return whether some other object equals this one
     */
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final InboundHeartbeatInfo other = (InboundHeartbeatInfo) obj;
      if (!Objects.equals(this.roleId, other.roleId)) {
        return false;
      }
      return true;
    }

    /** Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    public int hashCode() {
      int hash = 7;
      hash = 83 * hash + Objects.hashCode(this.roleId);
      return hash;
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[Inbound heartbeat, role: ");
      stringBuilder.append(roleId);
      if (heartbeatReceivedMillis > 0) {
        stringBuilder.append(", last received ");
        final long secondsAgo = (System.currentTimeMillis() - heartbeatReceivedMillis) / 1000;
        stringBuilder.append(secondsAgo);
        stringBuilder.append(" seconds ago]");
      }
      return stringBuilder.toString();
    }
  }

  /** Provides a container for outbound heartbeat information. */
  static class OutboundHeartbeatInfo {

    /** the heartbeat recipient's role ID */
    private final URI roleId;
    /** the millisecond time at which the most recent heartbeat was sent */
    private long heartbeatSentMillis;
    /** the recipient's service (skill class) */
    private final String service;
    /** the heartbeat */
    private final Heartbeat heartbeat;

    /** Constructs a new OutboundHeartbeatInfo instance.
     *
     * @param roleId the heartbeat sender's role ID
     */
    OutboundHeartbeatInfo(
            final URI roleId,
            final String service,
            final Heartbeat heartbeat) {
      //Preconditions
      assert roleId != null : "roleId must not be null";
      assert service != null : "service must not be null";
      assert !service.isEmpty() : "service must not be empty";
      assert heartbeat != null : "heartbeat must not be null";

      this.roleId = roleId;
      this.service = service;
      this.heartbeat = heartbeat;
    }

    /** Returns whether some other object equals this one.
     *
     * @param obj the other object
     * @return whether some other object equals this one
     */
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final OutboundHeartbeatInfo other = (OutboundHeartbeatInfo) obj;
      if (!Objects.equals(this.roleId, other.roleId)) {
        return false;
      }
      return true;
    }

    /** Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    public int hashCode() {
      int hash = 7;
      hash = 89 * hash + Objects.hashCode(this.roleId);
      return hash;
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[Outbound heartbeat, role: ");
      stringBuilder.append(heartbeat.getRoleQualifiedName(roleId));
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
