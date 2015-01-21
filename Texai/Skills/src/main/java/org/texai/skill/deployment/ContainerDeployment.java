/**
 * ContainerDeployment.java
 *
 * Description: Performs software deployment at the container level.
 *
 * Copyright (C) Jan 10, 2015, Stephen L. Reed.
 */
package org.texai.skill.deployment;

import java.io.File;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.ahcsSupport.Message;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerDeployment extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeployment.class);

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
      sendMessage(operationNotPermittedMessage(message));
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

      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(message);
        return;

      case AHCSConstants.DEPLOY_FILES_TASK:
        deployFiles(message);
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;

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
      AHCSConstants.DEPLOY_FILES_TASK,
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
   * Perform the specified file deployment task.
   *
   * @param message the received perform mission task message
   */
  private void deployFiles(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready";

    LOGGER.info("deploying files ...");
    final byte[] zippedBytes = (byte[]) message.get(AHCSConstants.DEPLOY_FILES_TASK_ZIPPED_BYTES);
    final String manifestJSONString = (String) message.get(AHCSConstants.DEPLOY_FILES_TASK_MANIFEST);
    LOGGER.info("  manifest:    " + manifestJSONString);

    //TODO first time, record the manifest and ensure that each item has been processed and that every item is actually on the manifest
    //otherwise ensure that the given manifest is the same as the first one.



//    switch (command) {
//      case "add":
//      case "replace":
//        final String hash = (String) message.get(AHCSConstants.DEPLOY_FILE_TASK_HASH);
//        LOGGER.info("  hash: " + hash);
//
//        LOGGER.info("writing " + fileToDeployPath);
//        try {
//          final FileOutputStream fileOutputStream = new FileOutputStream(fileToDeployPath);
//          final byte[] bytes = (byte[]) message.get(AHCSConstants.DEPLOY_FILE_TASK_BYTES);
//          MessageDigestUtils.verifyFileHash(hash, hash); // throws an exception if verification fails
//          fileOutputStream.write(bytes);
//          if (command.equals("add") && fileToDeployPath.startsWith("deployment-manifests/manifest-")) {
//            //TODO parse manifest file and ensure all files are processed
//          }
//        } catch (IOException ex) {
//          throw new TexaiException(ex);
//        }
//        break;
//
//      case "add-dir":
//        LOGGER.info("creating directory " + fileToDeployPath);
//        final File directory = new File(fileToDeployPath);
//        final boolean isOk = directory.mkdirs();
//        if (!isOk) {
//          throw new TexaiException("cannot create directory " + directory);
//        }
//        break;
//
//      case "delete-dir":
//        deleteDirectory(new File(fileToDeployPath));
//        break;
//
//      case "delete":
//        deleteFile(new File(fileToDeployPath));
//        break;
//
//      default:
//        assert false;
//    }
  }

  /**
   * Recursively deletes the given directory and its contained files and subdirectories.
   *
   * @param directory the given directory
   */
  private void deleteDirectory(final File directory) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";

    for (final File file : directory.listFiles()) {
      if (file.isDirectory()) {
        deleteDirectory(file);
      } else {
        deleteFile(file);
      }
    }
    LOGGER.info("deleting directory " + directory);
    final boolean isOk = directory.delete();
    if (!isOk) {
      throw new TexaiException("cannot delete " + directory);
    }
  }

  /**
   * Deletes the given file.
   *
   * @param file the given file
   */
  private void deleteFile(final File file) {
    //Preconditions
    assert file != null : "file must not be null";
    assert file.isFile() : "file must be a file";

    LOGGER.info("deleting file " + file);
    final boolean isOk = file.delete();
    if (!isOk) {
      throw new TexaiException("cannot delete " + file);
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
