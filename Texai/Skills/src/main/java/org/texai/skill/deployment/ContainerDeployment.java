/**
 * ContainerDeployment.java
 *
 * Description: Performs software deployment at the container level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
import org.texai.util.ZipUtils;
import org.texai.x509.MessageDigestUtils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerDeployment extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(ContainerDeployment.class);

  /**
   * Constructs a new SkillTemplate instance.
   */
  public ContainerDeployment() {
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
       * This task message is sent from the parent NetworkDeploymentAgent.NetworkDeploymentRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkDeploymentAgent.NetworkDeploymentRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(message);
        return;

      /**
       * Become Ready Task
       *
       * This task message is sent from the network-singleton parent NetworkDeploymentAgent.NetworkDeploymentRole.
       *
       * It results in the skill set to the ready state
       */
      case AHCSConstants.BECOME_READY_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) : "prior state must be isolated-from-network";
        setSkillState(AHCSConstants.State.READY);
        LOGGER.info("now ready");
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkDeploymentAgent.NetworkDeploymentRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkDeploymentAgent.NetworkDeploymentRole. It commands this
       * network-connected role to deploy included files according to the included manifest.
       *
       * When all the files are deployed, a Task Accomplished Info message is sent back to the NetworkDeployment skill as a response.
       */
      case AHCSConstants.DEPLOY_FILES_TASK:
        deployFiles(message);
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
      AHCSConstants.BECOME_READY_TASK,
      AHCSConstants.DEPLOY_FILES_TASK,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
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
   * Perform the specified file deployment task, and reply with a task accomplished message when done.
   *
   * @param message the received perform mission task message
   */
  private void deployFiles(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("deploying files ...");
    final byte[] zippedBytes = (byte[]) message.get(AHCSConstants.DEPLOY_FILES_TASK_ZIPPED_BYTES);
    final ZipFile zipFile = ZipUtils.temporaryZipFile(zippedBytes);
    LOGGER.info("zipFile: " + zipFile.getName());

    final String manifestJSONString = (String) message.get(AHCSConstants.DEPLOY_FILES_TASK_MANIFEST);
    LOGGER.info("  manifest:\n" + manifestJSONString);
    try {
      final JSONObject jsonObject = (JSONObject) JSONValue.parseWithException(manifestJSONString);
      LOGGER.info("jsonObject: " + jsonObject);
      @SuppressWarnings("unchecked")
      final List<JSONObject> manifestItems = (List<JSONObject>) jsonObject.get("manifest");

      // iterate over the manifest items
      for (final JSONObject manifestItem : manifestItems) {
        LOGGER.info(manifestItem);
        final String command = (String) manifestItem.get("command");
        LOGGER.info("  command: " + command);
        final String fileToDeployPath = (String) manifestItem.get("path");
        LOGGER.info("  path: " + fileToDeployPath);
        final File fileToDeploy = new File(fileToDeployPath);

        switch (command) {
          case "add":
          case "replace":
            final String fileHashString = (String) manifestItem.get("hash");
            LOGGER.info("  hash: " + fileHashString);
            writeManifestEntry(
                    fileToDeploy,
                    zipFile,
                    fileHashString);
            break;

          case "add-dir":
            LOGGER.info("creating directory " + fileToDeployPath);
            final File directory = new File(fileToDeployPath);
            final boolean isOk = directory.mkdirs();
            if (!isOk) {
              throw new TexaiException("cannot create directory " + directory);
            }
            break;

          case "delete-dir":
          case "delete":
            FileUtils.deleteQuietly(new File(fileToDeployPath));
            break;

          default:
            assert false;
        }
      }
    } catch (ParseException ex) {
      throw new TexaiException(ex);
    }
    sendMessage(Message.replyTaskAccomplished(message));
  }

  /** Extracts the file to deploy from the zip file, writes it to the deployed path, and then verifies its contents using
   * the given file hash string.
   *
   * @param fileToDeploy the file which will be written
   * @param zipFile the zip archive containing the file's bytes
   * @param fileHashString the previously computed SHA-256 hash of the file's contents for verification
   */
  private void writeManifestEntry(
          final File fileToDeploy,
          final ZipFile zipFile,
          final String fileHashString) {

    try {
      final FileOutputStream fileOutputStream = new FileOutputStream(fileToDeploy);
      final ZipEntry zipEntry = zipFile.getEntry(fileToDeploy.getName());
      LOGGER.info("  extracting " + fileToDeploy.getName());
      LOGGER.info("writing " + fileToDeploy);
      try (BufferedInputStream bufferedInputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry))) {
        int count;
        final int bufferSize = 4096;
        byte data[] = new byte[bufferSize];
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, bufferSize)) {
          while ((count = bufferedInputStream.read(data, 0, bufferSize))
                  != -1) {
            bufferedOutputStream.write(data, 0, count);
          }
          bufferedOutputStream.flush();
        }
      }
      MessageDigestUtils.verifyFileHash(
              fileToDeploy.getPath(), // filePath
              fileHashString); // fileHashString
    } catch (IOException ex) {
      throw new TexaiException(ex);
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
}
