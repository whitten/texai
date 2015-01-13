/**
 * NetworkDeployment.java
 *
 * Description: Performs software deployment at the network level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import java.util.TimerTask;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class NetworkDeployment extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeployment.class);

  /**
   * Constructs a new SkillTemplate instance.
   */
  public NetworkDeployment() {
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
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        // OPTIONAL
        // initialize child roles
        propagateOperationToChildRoles(operation);

        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return true;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return true;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

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
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
    };
  }

  /**
   * Perform this role's mission.
   *
   * @param message the received perform mission task message
   */
  private void performMission(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

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
   * Periodically checks for a manifest to deploy.
   */
  class CheckForDeployment extends TimerTask {

    /**
     * Check for a manifest to deploy.
     */
    @Override
    public void run() {
      // scan the deployment directory looking for "deployed.log" (think about a tamper-evident log and peer verification)

      // if found then nothing to deploy and return

      // otherwise, find the manifest and verify it is non empty

      // set a node state variable to indicate software deployment is in progress

      // notify all all containers to quiesce except for deployment messages
      //   DEPLOYMENT_QUIESCE_TASK --> ContainerDeploymentRole

      // when all acknowledge, propagate each manifest command and file to each peer
      //   ContainerDeploymentRole --> TASK_ACCOMPLISHED_INFO

      // as peers acknowledge that their updates completed, write the deployed.log with the details the deployment


      // ask the network operations agent to shutdown the other peers who automatically restart in a random interval in
      // excess of the time required to restart the network operations node
      //   NETWORK_RESTART_REQUEST_INFO --> NetworkOperationRole
      //   RESTART_CONTAINER_TASK --> ContainerOperationRole


      // the network operations node as the sole singleton agent host and await the connecting peers
    }

  }

}
