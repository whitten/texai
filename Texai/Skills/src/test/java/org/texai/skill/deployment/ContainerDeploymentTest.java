/*
 * Copyright (C) 2015 Texai
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
package org.texai.skill.deployment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class ContainerDeploymentTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerDeploymentTest.class);
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkDeploymentAgent.NetworkDeploymentRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  private static final String skillClassName = ContainerDeployment.class.getName();
  private static final String nodeName = "ContainerDeploymentAgent";
  private static final String roleName = "ContainerDeploymentRole";
  private static SkillTestHarness skillTestHarness;

  public ContainerDeploymentTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    ContainerDeployment.isUnitTest = true;
    final Set<SkillClass> skillClasses = new ArraySet<>();
    final SkillClass skillClass = new SkillClass(
            skillClassName, // skillClassName
            true); // isClassExistsTested
    skillClasses.add(skillClass);
    final Set<String> variableNames = new ArraySet<>();
    final Set<String> childQualifiedNames = new ArraySet<>();
    childQualifiedNames.add(containerName + ".ContainerDeploymentAgent.ContainerDeploymentRole");
    skillTestHarness = new SkillTestHarness(
            containerName + "." + nodeName, // name
            "test mission description", // missionDescription
            true, // isNetworkSingleton
            containerName + "." + nodeName + "." + roleName, // qualifiedName
            "test role description", // description
            containerName + ".NetworkOperationAgent.NetworkOperationRole", // parentQualifiedName
            childQualifiedNames,
            skillClasses,
            variableNames,
            false); // areRemoteCommunicationsPermitted
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of class ContainerDeployment initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.AHCS_INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            containerName + ".NetworkDeploymentAgent", // senderQualifiedName
            NetworkDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final ContainerDeployment containerDeployment = (ContainerDeployment) skillTestHarness.getSkill(skillClassName);
    if (containerDeployment.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndServiceInfo());
  }

  /**
   * Test of class ContainerDeployment deploy files task message.
   */
  @Test
  public void testDeployFilesTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.DEPLOY_FILES_TASK + " message");

    Logger.getLogger(X509Utils.class).setLevel(Level.WARN);
    FileUtils.deleteQuietly(new File("Main-1.0"));
    FileUtils.deleteQuietly(new File("bin"));
    (new File("Main-1.0/data")).mkdirs();
    (new File("Main-1.0/lib")).mkdirs();
    (new File("bin")).mkdir();
    skillTestHarness.reset();
    skillTestHarness.setSkillState(
            AHCSConstants.State.READY, // state
            skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    Message deployFilesTaskMessage = Message.deserializeMessage("data/test-messages/deployFileTaskMessage0.ser");
    LOGGER.info(deployFilesTaskMessage.toString());
    assertEquals("[deployFile_Task, Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment]", deployFilesTaskMessage.toBriefString());

    skillTestHarness.dispatchMessage(deployFilesTaskMessage);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());

    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage1.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage2.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage3.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage4.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage5.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage6.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage7.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage8.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage9.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage10.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage11.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage12.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage13.ser"));

    final File mainDirectory = new File("Main-1.0");
    assert mainDirectory.exists();
    assert mainDirectory.isDirectory();
    final File dataDirectory = new File("Main-1.0/data");
    assert dataDirectory.exists();
    assert dataDirectory.isDirectory();
    assertEquals("[nodes.xml]", getSortedFileNamesInDirectory(dataDirectory));
    final File libDirectory = new File("Main-1.0/lib");
    assert libDirectory.exists();
    assert libDirectory.isDirectory();
    assertEquals("[TamperEvidentLog-1.0.jar, UPNPLib-1.0.jar]", getSortedFileNamesInDirectory(libDirectory));
    final File binDirectory = new File("bin");
    assert binDirectory.exists();
    assert binDirectory.isDirectory();
    assertEquals("[]", getSortedFileNamesInDirectory(binDirectory));
    FileUtils.deleteQuietly(new File("Main-1.0"));
    FileUtils.deleteQuietly(new File("bin"));

    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[taskAccomplished_Info Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment --> Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment 2015-01-22T15:27:28.990-06:00]"));
  }

  /**
   * Returns a string representation of the sorted file names contained in the given directory.
   *
   * @param directory the given directory
   *
   * @return the sorted file names
   */
  private String getSortedFileNamesInDirectory(final File directory) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";

    final List<String> fileNames = new ArrayList<>();
    for (final File file : directory.listFiles()) {
      fileNames.add(file.getName());
    }
    Collections.sort(fileNames);
    return fileNames.toString();
  }

  /**
   * Test of class NetworkSingletonSkillTemplate - Message Not Understood Info.
   */
  @Test
  public void testMessageNotUnderstoodInfo() {
    LOGGER.info("testing " + AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message taskAccomplishedInfoMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            "an-unknown-operation_Task"); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(sentMessage.toString().startsWith(""));
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkDeployment.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerDeployment instance = new ContainerDeployment();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, becomeReady_Task, deployFile_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

  /**
   * Test of getLogger method, of class NetworkDeployment.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    ContainerDeployment instance = new ContainerDeployment();
    assertNotNull(instance.getLogger());
  }

}
