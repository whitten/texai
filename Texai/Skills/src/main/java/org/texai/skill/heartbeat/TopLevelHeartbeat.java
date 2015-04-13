/*
 * TopLevelHeartbeat.java
 *
 * Created on Sep 18, 2014, 8:03:57 AM
 *
 * Description:
 *
 * Copyright (C) 2014 Stephen L. Reed
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
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
@ThreadSafe
public final class TopLevelHeartbeat extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TopLevelHeartbeat.class);
  // heartbeat roles from which keep-alive messages are periodically expected
  private final Map<String, InboundHeartbeatInfo> inboundHeartbeatInfos = new HashMap<>();
  // the inbound heartbeat period milliseconds, which indicates the duration beyond which the expected heartbeat is considered missing
  private final static long INBOUND_HEARTBEAT_PERIOD_MILLIS = 300_000;

  /**
   * Constructs a new TopLevelHeartbeat instance.
   */
  public TopLevelHeartbeat() {
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
       * This task message is sent from the parent TopmostFriendship agent/role. It is expected to be the first task message that this role
       * receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        // initialize child governance roles
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
       * This task message is sent from the network-singleton, parent TopmostFriendship agent/role. It indicates that the parent is ready to
       * converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
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
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent TopmostFriendship agent/role.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) :
                "state must be ready, but is " + getSkillState();
        assert !getRole().getChildQualifiedNames().isEmpty() : "must have at least one child role";

        performMission();
        propagateOperationToChildRolesSeparateThreads(receivedMessage);
        return;

      /**
       * Keep Alive Info
       *
       * This task message is sent to this network singleton agent/role from a child ContainerHeartbeat agent/role.
       *
       * The result is the recording of the liveness of the sending container, and a Keep Alive Acknowledged Task message is sent back as a
       * reply message.
       *
       */
      case AHCSConstants.KEEP_ALIVE_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        recordKeepAlive(receivedMessage);
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

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
    }

    sendMessage(
            receivedMessage,
            Message.notUnderstoodMessage(
                    receivedMessage,
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
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.KEEP_ALIVE_INFO,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK
    };
  }

  /**
   * Performs this skill's mission.
   */
  private void performMission() {
    final HeartbeatProcessor heartbeatProcessor = new HeartbeatProcessor(this);
    getTimer().schedule(
            heartbeatProcessor,
            300000, // delay, 5 minutes
            300000); // period, 5 minutes
  }

  /**
   * Records a keep-alive message sent from a role.
   *
   * @param message the keep-alive message
   */
  protected void recordKeepAlive(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final StringBuilder stringBuilder = new StringBuilder()
            .append(getContainerName())
            .append(" recording keep-alive from ")
            .append(message.getSenderContainerName())
            .append(" at ")
            .append((new DateTime()).toString("MM/dd/yyyy hh:mm a"));
    LOGGER.info(stringBuilder.toString());
    final String senderQualifiedName = message.getSenderQualifiedName();
    InboundHeartbeatInfo inboundHeartbeatInfo = inboundHeartbeatInfos.get(senderQualifiedName);
    if (inboundHeartbeatInfo == null) {
      inboundHeartbeatInfo = new InboundHeartbeatInfo(senderQualifiedName);
      inboundHeartbeatInfos.put(senderQualifiedName, inboundHeartbeatInfo);
    }
    inboundHeartbeatInfo.heartbeatReceivedMillis = System.currentTimeMillis();

    // record container liveness information in the node runtime
    final String containerName = Node.extractContainerName(inboundHeartbeatInfo.senderQualifiedName);
    ContainerInfo containerInfo = getNodeRuntime().getContainerInfo(containerName);
    if (containerInfo == null) {
      LOGGER.info("Adding unexpected " + containerName + " to the list of live containers.");
      containerInfo = new ContainerInfo(
              containerName,
              false, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
      getNodeRuntime().addContainerInfo(containerInfo);
    }
    containerInfo.setIsAlive(true);

    final Message keepAliveAcknowledgedTaskMessage = makeReplyMessage(
            message,
            AHCSConstants.KEEP_ALIVE_ACKNOWLEDGED_TASK); // operation
    sendMessage(
            message, // receivedMessage
            keepAliveAcknowledgedTaskMessage); // message
  }

  /**
   * Periodically processes the outbound and inbound heartbeat information objects.
   */
  protected class HeartbeatProcessor extends TimerTask {

    /**
     * the top level heartbeat skill
     */
    private final TopLevelHeartbeat topLevelHeartbeat;

    /**
     * Constructs a new HeartbeatProcessor instance.
     *
     * @param topLevelHeartbeat the heartbeat skill
     */
    HeartbeatProcessor(final TopLevelHeartbeat topLevelHeartbeat) {
      //Preconditions
      assert topLevelHeartbeat != null : "topLevelHeartbeat must not be null";

      this.topLevelHeartbeat = topLevelHeartbeat;
    }

    /**
     * Processes the outbound and inbound heartbeat information objects.
     */
    @Override
    public void run() {
      final long currentTimeMillis = System.currentTimeMillis();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(topLevelHeartbeat + " HeartbeatProcessor ...");
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
   * Notices that an expected heartbeat message has not yet been received from the given role.
   *
   * @param inboundHeartbeatInfo the given role heartbeat information
   */
  protected void missingHeartbeat(final InboundHeartbeatInfo inboundHeartbeatInfo) {
    //Preconditions
    assert inboundHeartbeatInfo != null : "inboundHeartbeatInfo must not be null";

    LOGGER.warn("Heartbeat missing for " + inboundHeartbeatInfo + ".");
    final String containerName = Node.extractContainerName(inboundHeartbeatInfo.senderQualifiedName);
    ContainerInfo containerInfo = getNodeRuntime().getContainerInfo(containerName);
    if (containerInfo == null) {
      LOGGER.info("Adding " + containerName + " to the list of live containers.");
      containerInfo = new ContainerInfo(
              containerName,
              false, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
      getNodeRuntime().addContainerInfo(containerInfo);
    }
    containerInfo.setIsAlive(false);
  }

}
