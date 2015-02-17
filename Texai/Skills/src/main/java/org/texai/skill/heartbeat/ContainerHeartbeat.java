/*
 * ContainerHeartbeat.java
 *
 * Created on Sep 18, 2014, 8:04:36 AM
 *
 * Description: Manages a container's agents to ensure liveness.
 *
 * Copyright (C) 2014 Stephen L. Reed
 */
package org.texai.skill.heartbeat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.Role;
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
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the container-local parent NetworkOperationAgent.TopLevelHeartbeatRole. It is expected to be the
       * first task message that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        initialization(receivedMessage);
        // initialize child heartbeat roles
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
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.TopLevelHeartbeatRole. It indicates that the
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
       * This task message is sent from the network-singleton parent NetworkOperationAgent.TopLevelHeartbeatRole.
       *
       * It results in the skill set to the ready state, and it performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        if (getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK)) {
          setSkillState(AHCSConstants.State.READY);
          LOGGER.info("now ready");
        }
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        performMission(receivedMessage);
        return;

      case AHCSConstants.KEEP_ALIVE_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";
        recordKeepAlive(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
    }

    // other operations ...
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
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.KEEP_ALIVE_INFO,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("initializing " + toString() + " in role " + getRole());
    }

    outboundParentHeartbeatInfo = new OutboundHeartbeatInfo(
            getRole(),
            TopLevelHeartbeat.class.getName()); // service
    // create a timer that periodically reviews the outbound heartbeat information objects
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.scheduleAtFixedRate(
              new HeartbeatProcessor(this), // task
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS, // delay
              OUTBOUND_HEARTBEAT_PERIOD_MILLIS); // period
    }
  }

  /**
   * Perform this role's mission, which is to manage the network, the containers, and the A.I. Coin agents within the containers.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

    propagateOperationToChildRolesSeparateThreads(receivedMessage);
  }

  /**
   * Records a keep-alive message sent from a role.
   *
   * @param message the keep-alive message
   */
  protected void recordKeepAlive(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(getContainerName() + " recording keep-alive " + message);
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
      LOGGER.debug("    to " + getQualifiedName());
    }
  }

  /**
   * Periodically processes the outbound and inbound heartbeat information objects.
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
      if (!containerHeartbeat.getSkillState().equals(State.READY)) {
        LOGGER.info("No heartbeat because this skill has not yet joined the network.");
        return;
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
     * the role
     */
    protected final Role role;
    /**
     * the millisecond time at which the most recent heartbeat was sent
     */
    protected long heartbeatSentMillis;
    /**
     * the recipient's service (skill class)
     */
    protected final String service;

    /**
     * Constructs a new OutboundHeartbeatInfo instance.
     *
     * @param role the role
     * @param service the service, or null if not specified
     */
    OutboundHeartbeatInfo(
            final Role role,
            final String service) {
      //Preconditions
      assert role != null : "role must not be null";
      assert StringUtils.isNonEmptyString(service) : "service must be a non-empty string";

      this.role = role;
      this.service = service;
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
      return Objects.equals(this.role, other.role);
    }

    /**
     * Returns a hash code for this object.
     *
     * @return a hash code for this object
     */
    @Override
    public int hashCode() {
      int hash = 7;
      hash = 89 * hash + Objects.hashCode(this.role);
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
      stringBuilder.append("[Outbound heartbeat");
      if (heartbeatSentMillis > 0) {
        stringBuilder.append(" last sent ");
        final long secondsAgo = (System.currentTimeMillis() - heartbeatSentMillis) / 1000;
        stringBuilder.append(secondsAgo);
        stringBuilder.append(" seconds ago");
      }
      stringBuilder.append(']');
      return stringBuilder.toString();
    }
  }

  /**
   * Notices that an expected heartbeat message has not yet been received from the given role.
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

    final Message keepAliveInfoMessage = new Message(
            heartbeat.getQualifiedName(), // senderQualifiedName
            heartbeat.getClass().getName(), // senderService
            outboundHeartbeatInfo.role.getParentQualifiedName(), // recipentQualifiedName,
            outboundHeartbeatInfo.service,
            AHCSConstants.KEEP_ALIVE_INFO); // operation
    LOGGER.info(getContainerName() + " sending keep-alive to " + Node.extractContainerName(outboundHeartbeatInfo.role.getParentQualifiedName()));
    sendMessageViaSeparateThread(
            null, // received message, which is null because this sent message is triggered by a timer
            keepAliveInfoMessage);

    outboundHeartbeatInfo.heartbeatSentMillis = System.currentTimeMillis();
  }

}
