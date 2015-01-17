/**
 * NetworkDeployment.java
 *
 * Description: Performs software deployment at the network level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class NetworkDeployment extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeployment.class);
  // the millisecond period between checks for new software /data deployment
  private static final long CHECK_FOR_DEPLOYMENT_PERIOD_MILLIS = 60 * 1000 * 5;
  // the indicator that a software deployment is in progress
  private final AtomicBoolean isSoftwareDeploymentUnderway = new AtomicBoolean(false);

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
   */
  @Override
  public void receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return;
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
        return;

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      // handle other operations ...
      // handle other operations ...
    }
    // otherwise, the message is not understood
    sendMessage(notUnderstoodMessage(message));
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

    createCheckForDeploymentTimerTask();
  }

  /**
   * Creates a timer that periodically checks for software / data deployments.
   */
  private void createCheckForDeploymentTimerTask() {
    final Timer timer = getNodeRuntime().getTimer();
    synchronized (timer) {
      timer.scheduleAtFixedRate(
              new CheckForDeployment(this), // task
              CHECK_FOR_DEPLOYMENT_PERIOD_MILLIS, // delay
              CHECK_FOR_DEPLOYMENT_PERIOD_MILLIS); // period
    }
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

    // the network deployment skill
    final NetworkDeployment networkDeployment;

    /**
     * Creates a new CheckForDeployment instance.
     *
     * @param networkDeployment the network deployment skill
     */
    CheckForDeployment(final NetworkDeployment networkDeployment) {
      //Preconditions
      assert networkDeployment != null : "networkDeployment must not be null";

      this.networkDeployment = networkDeployment;
    }

    /**
     * Check for a manifest to deploy.
     */
    @Override
    public void run() {
      //TODO add a tamper-evident log and peer verification

      LOGGER.info("checking for a new deployment");
      // scan the deployment directory looking for "deployed.log"
      final File deploymentDirectory = new File("deployment");
      if (!deploymentDirectory.exists()) {
        LOGGER.info("creating the deployment directory");
        deploymentDirectory.mkdir();
        return;
      }
      assert deploymentDirectory.isDirectory();
      final File[] files = deploymentDirectory.listFiles();
      for (final File file : files) {
        if (file.getName().equals("deployment.log")) {
          return;
        }
      }
      // process the deployment in separate thread in order to immediately release the shared timer thread
      networkDeployment.getNodeRuntime().getExecutor().execute(new DeploymentRunable(networkDeployment, files));
    }
  }

  class DeploymentRunable implements Runnable {

    // the network deployment skill
    final NetworkDeployment networkDeployment;
    // the deployment directory files
    final File[] files;

    /**
     * Creates a new CheckForDeployment instance.
     *
     * @param networkDeployment the network deployment skill
     * @param files the deployment directory files
     */
    DeploymentRunable(
            final NetworkDeployment networkDeployment,
            final File[] files) {
      //Preconditions
      assert networkDeployment != null : "networkDeployment must not be null";
      assert files != null : "files must not be null";
      assert files.length > 0 : "files must be present";

      this.networkDeployment = networkDeployment;
      this.files = files;
    }

    /**
     * Runs the software deployment process.
     */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
      if (isSoftwareDeploymentUnderway.getAndSet(true)) {
        LOGGER.info("exiting thread because software deployment is underway");
        return;
      }
      LOGGER.info("Software and data deployment starting ...");
      // find the manifest and verify it is non empty
      File manifestFile = null;
      for (final File file : files) {
        if (file.getName().startsWith("manifest-")) {
          manifestFile = file;
          break;
        }
      }
      if (manifestFile == null) {
        LOGGER.info("missing manifest file");
        //TODO report to network operations
        return;
      }

      try {
        final JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(new FileReader(manifestFile));
        @SuppressWarnings("unchecked")
        final List<JSONObject> manifestItems = (List<JSONObject>) jsonObject.get("manifest");
        for (final JSONObject manifestItem : manifestItems) {
          LOGGER.info(manifestItem);
          final String command = (String) manifestItem.get("command");
          LOGGER.info("  command: " + command);
          final String fileToDeployPath = (String) manifestItem.get("command");
          LOGGER.info("  path: " + fileToDeployPath);
          final File fileToDeploy = new File(fileToDeployPath);
          final File fileToSend = new File(fileToDeploy.getName());
          LOGGER.info("  fileToSend: " + fileToSend);

          // make a copy of the child container deployment roles for multithreading safety
          final List<String> containerDeploymentRoleNames = new ArrayList<>(networkDeployment.getRole().getChildQualifiedNames());
          // send the file to each child container deployment role
          containerDeploymentRoleNames.stream().sorted().forEach((String containerDeploymentRoleName) -> {

            final Message deployFileMessage = networkDeployment.makeMessage(
                    containerDeploymentRoleName, // recipientQualifiedName
                    ContainerDeployment.class.getName(), // recipientService
                    AHCSConstants.DEPLOY_FILE_TASK); // operation
            deployFileMessage.put(AHCSConstants.DEPLOY_FILE_TASK_COMMAND, command);
            deployFileMessage.put(AHCSConstants.DEPLOY_FILE_TASK_PATH, fileToDeployPath);
            if (command.equals("add") || command.equals("change")) {
              try {
                deployFileMessage.put(AHCSConstants.DEPLOY_FILE_TASK_BYTES, FileUtils.readFileToByteArray(fileToSend));
              } catch (IOException ex) {
                throw new TexaiException(ex);
              }
            }

            sendMessage(deployFileMessage);
            // pause this thread to keep from creating too many long-running downstream threads
            try {
              Thread.sleep(500);
            } catch (InterruptedException ex) {
            }
          });

          // propagate each manifest command and file to each peer
          //   ContainerDeploymentRole --> TASK_ACCOMPLISHED_INFO
          // as peers acknowledge that their updates completed, write the deployed.log with the details the deployment
          // ask the network operations agent to shutdown the other peers who automatically restart in a random interval in
          // excess of the time required to restart the network operations node
          //   NETWORK_RESTART_REQUEST_INFO --> NetworkOperationRole
          //   RESTART_CONTAINER_TASK --> ContainerOperationRole
          // the network operations node as the sole singleton agent host and await the connecting peers
        }
      } catch (IOException | ParseException ex) {
        throw new TexaiException(ex);
      } finally {
        isSoftwareDeploymentUnderway.set(false);
      }
    }

  }

}
