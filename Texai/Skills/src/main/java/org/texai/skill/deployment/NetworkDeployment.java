/**
 * NetworkDeployment.java
 *
 * Description: Performs software deployment at the network level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.Node;
import org.texai.skill.network.NetworkOperation;
import org.texai.util.TexaiException;
import org.texai.util.ZipUtils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class NetworkDeployment extends AbstractNetworkSingletonSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeployment.class);
  // the millisecond period between checks for new software /data deployment
//  private static final long CHECK_FOR_DEPLOYMENT_PERIOD_MILLIS = 60 * 1000 * 5;
  private static final long CHECK_FOR_DEPLOYMENT_PERIOD_MILLIS = 60 * 1000 * 1;
  // the indicator that a software deployment is in progress
  private final AtomicBoolean isSoftwareDeploymentUnderway = new AtomicBoolean(false);
  // the names of containers which have not completed a software and data file deployment task
  protected final Set<String> undeployedContainerNames = new HashSet<>();

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
      sendMessage(Message.operationNotPermittedMessage(
              message, // receivedMessage
              this)); // skill
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        // initialize child roles
        propagateOperationToChildRoles(operation);

        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Delegate Become Ready Task
       *
       * A container has completed joining the network. Propagate a Delegate Become Ready Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_BECOME_READY_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegateBecomeReadyTask(message);
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
        handleDelegatePerformMissionTask(message);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        performMission(message);
        return;

      /**
       * Task Accomplished Info
       *
       * This information message is sent from the child ContainerDeploymentAgent.ContainerDeploymentRole. It notifies this network
       * singleton role that the container has completed the software and data file deployment task.
       *
       * When all the containers have respondedk, an Deployment Completed Info message is sent to the parent
       * NetworkOperationAgent.NetworkOperationRole so that it can restart the network.
       */
      case AHCSConstants.TASK_ACCOMPLISHED_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleTaskAccomplishedInfo(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

      // handle other operations ...
    }
    // otherwise, the message is not understood
    sendMessage(Message.notUnderstoodMessage(
            message, // receivedMessage
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
  public Message converseMessage(Message message) {
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
      AHCSConstants.AHCS_INITIALIZE_TASK,
      AHCSConstants.DELEGATE_BECOME_READY_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.PERFORM_MISSION_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO
    };
  }

  /**
   * Performs this role's mission. It starts a timer task that periodically checks for files to deploy to the network.
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
   * Receives notification a child container deployment has completed its software and data file deployment task.
   *
   * @param message the received perform mission task message
   */
  private void handleTaskAccomplishedInfo(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    final String containerName = message.getSenderContainerName();
    int undeployedContainerNames_size;
    LOGGER.info(containerName + " completed software and data file deployment");
    synchronized (undeployedContainerNames) {
      final boolean isRemoved = undeployedContainerNames.remove(containerName);
      assert isRemoved : "container was not previously present: " + containerName;
      undeployedContainerNames_size = undeployedContainerNames.size();
    }
    LOGGER.info("number of undeployed containers remaining: " + undeployedContainerNames_size);
    if (undeployedContainerNames_size == 0) {
      // notify network operations that the deployment is complete, and that the network should be restarted
      final Message networkRestartRequestInfo = makeMessage(getRole().getParentQualifiedName(), // recipientQualifiedName
              NetworkOperation.class.getName(), // recipientService
              AHCSConstants.NETWORK_RESTART_REQUEST_INFO);
      sendMessage(networkRestartRequestInfo);
    }
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
   * Returns a new CheckForDeployment instance for unit testing.
   *
   * @return a new CheckForDeployment instance
   */
  protected CheckForDeployment makeCheckForDeploymentForUnitTest() {
    return new CheckForDeployment(
            this, // networkDeployment
            true); // isUnitTest
  }

  /**
   * Periodically checks for a manifest to deploy.
   */
  protected class CheckForDeployment extends TimerTask {

    // the network deployment skill
    final NetworkDeployment networkDeployment;
    // the indicator whether this timer test is executed as part of a unit test, in which case there is no Timer
    final boolean isUnitTest;

    /**
     * Creates a new CheckForDeployment instance.
     *
     * @param networkDeployment the network deployment skill
     */
    CheckForDeployment(final NetworkDeployment networkDeployment) {
      //Preconditions
      assert networkDeployment != null : "networkDeployment must not be null";

      this.networkDeployment = networkDeployment;
      isUnitTest = false;
    }

    /**
     * Creates a new CheckForDeployment instance.
     *
     * @param networkDeployment the network deployment skill
     * @param isUnitTest the indicator whether this timer test is executed as part of a unit test, in which case there is no Timer
     */
    CheckForDeployment(
            final NetworkDeployment networkDeployment,
            final boolean isUnitTest) {
      //Preconditions
      assert networkDeployment != null : "networkDeployment must not be null";

      this.networkDeployment = networkDeployment;
      this.isUnitTest = isUnitTest;
    }

    /**
     * Check for a manifest to deploy.
     */
    @Override
    public void run() {
      //TODO add a tamper-evident log and peer verification

      // scan the deployment directory looking for "deployed.log"
      final File deploymentDirectory = new File("deployment");
      LOGGER.info("checking for a new deployment in " + deploymentDirectory.getAbsolutePath());
      if (!deploymentDirectory.exists()) {
        LOGGER.info("creating the deployment directory");
        deploymentDirectory.mkdir();
        return;
      }
      assert deploymentDirectory.isDirectory();

      final File[] files = deploymentDirectory.listFiles();
      if (files.length == 0) {
        LOGGER.info("No files in the deployment directory to deploy");
        return;
      }
      for (final File file : files) {
        if (file.getName().equals("deployment.log")) {
          LOGGER.info("Files in the deployment directory have already been deployed.");
          return;
        }
      }
      if (isUnitTest) {
        // no timer thread when unit testing, and need to wait for the results
        (new DeploymentRunable(networkDeployment, files)).run();
      } else {
        // process the deployment in separate thread in order to immediately release the shared timer thread
        networkDeployment.getNodeRuntime().getExecutor().execute(new DeploymentRunable(networkDeployment, files));
      }
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
      final File deploymentDirectory = new File("deployment");
      LOGGER.info("Software and data deployment starting ...");
      LOGGER.info(files.length + " files");
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
      LOGGER.info("manifestFile: " + manifestFile);
      final StringBuilder stringBuilder = new StringBuilder();
      try {
        final String manifestJSONString = FileUtils.readFileToString(manifestFile);
        final JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(manifestJSONString);
        LOGGER.info("jsonObject: " + jsonObject);
        @SuppressWarnings("unchecked")
        final List<JSONObject> manifestItems = (List<JSONObject>) jsonObject.get("manifest");
        final byte[] zippedBytes = ZipUtils.archiveFilesToByteArray(deploymentDirectory);
        LOGGER.info("manifestItems: " + manifestItems);
        for (final JSONObject manifestItem : manifestItems) {
          LOGGER.info(manifestItem);
          final String command = (String) manifestItem.get("command");
          LOGGER.info("  command: " + command);
          final String fileToDeployPath = (String) manifestItem.get("path");
          LOGGER.info("  path: " + fileToDeployPath);
          final File fileToDeploy = new File(fileToDeployPath);
          final File fileToSend = new File("deployment/" + fileToDeploy.getName());
          LOGGER.info("  fileToSend: " + fileToSend);
          if (command.equals("add") || command.equals("change")) {
            final String hash = (String) manifestItem.get("hash");
            LOGGER.info("  hash: " + hash);

          }
        }
        // make a copy of the child container deployment roles for multithreading safety
        final List<String> containerDeploymentRoleNames = new ArrayList<>(networkDeployment.getRole().getChildQualifiedNames());
        // send the manifest and zipped bytes to each child container deployment role
        containerDeploymentRoleNames.stream().sorted().forEach((String containerDeploymentRoleName) -> {

          final String containerName = Node.extractContainerName(containerDeploymentRoleName);
          final Message deployFileMessage = networkDeployment.makeMessage(containerDeploymentRoleName, // recipientQualifiedName
                  ContainerDeployment.class.getName(), // recipientService
                  AHCSConstants.DEPLOY_FILES_TASK); // operation
          deployFileMessage.put(AHCSConstants.DEPLOY_FILES_TASK_ZIPPED_BYTES, zippedBytes);
          deployFileMessage.put(AHCSConstants.DEPLOY_FILES_TASK_MANIFEST, manifestJSONString);

          synchronized (undeployedContainerNames) {
            // track the containers that have been tasked to deploy, so that when subsequent Task Accomplished messages are
            // received, it can determine when all have deployed and then restart the network
            undeployedContainerNames.add(containerName);
          }
          sendMessage(deployFileMessage);

          stringBuilder
                  .append("deployed to ")
                  .append(containerName)
                  .append('\n').toString();
          // pause this thread to keep from creating too many long-running downstream threads
          try {
            Thread.sleep(500);
          } catch (InterruptedException ex) {
          }
        });
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("deployment/deployment.log"))) {
          bufferedWriter.write(stringBuilder.toString());
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
        //   RESTART_CONTAINER_TASK --> ContainerOperationRole
        // the network operations node as the sole singleton agent host and await the connecting peers
        //   ContainerDeploymentRole --> TASK_ACCOMPLISHED_INFO
        // as peers acknowledge that their updates completed, write the deployed.log with the details the deployment
        // ask the network operations agent to shutdown the other peers who automatically restart in a random interval in
        // excess of the time required to restart the network operations node
        //   NETWORK_RESTART_REQUEST_INFO --> NetworkOperationRole
        //   RESTART_CONTAINER_TASK --> ContainerOperationRole
        // the network operations node as the sole singleton agent host and await the connecting peers
      } catch (IOException | ParseException ex) {
        throw new TexaiException(ex);
      } finally {
        isSoftwareDeploymentUnderway.set(false);
      }
    }

  }

}
