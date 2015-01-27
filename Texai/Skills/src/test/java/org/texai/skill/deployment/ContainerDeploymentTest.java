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
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-26T23:27:11.113-06:00\n"
            + "  deployFile_Task_manifest={\"manifest\":[\n"
            + "{\"path\":\"Main-1.0\\/data\\/nodes.xml\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o\\/\\/\\/wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/JavaBitcoindRpcClient-0.9.0.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"VmlCyM3MdelJ1\\/gmx+L5DVQ5xtFr7UmsZmyU3dxFMXOAaPdV7LZ\\/itEbTfV4Aqcz+JCojATIXTDwXtxJr5grOw==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/Main-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"wdPbfwE62\\/hlTuhJacjQh43S2pLCnMhzolygYJTI1fUlyF8IFgFuGYhoILNFW3Mxu88v+YArrCMZ6AF995td0g==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/Network-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"1oHJOLqPZPxcm8Oq4vQME4slFcdAYhBhU3c7dgy+6Ho439slVFOGrN39M1cP6wnzANmW4x7GHdhLy5X7DAxa0w==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/TamperEvidentLog-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"Xf2hZN+vwWhYShIKX75IUO5iGDXi177n9tNkeyLmBTiMKp8LKBwlfzkzWaohKnwUBKhevjmzsQSX0ZT4DeeJKw==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/UPNPLib-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"otrR8RCJrBlu9gIxDl+B7TMuUfpgtkdNKNLQkDiUOA7zDxhw\\/NhHD8F5hpg8IJJm2hu1Pcpsn1fRR5ijWuapNQ==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/Utilities-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"G6hqLWM7QsRSOGU9zY53Bx1RR6s8wjhZ\\/qq0jB5H0L7ysQWHzNtcW7EukG0Xps48Pa4JxfMNtI2EjEtRMLu2Tg==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/json-simple-1.1.1.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"+HmL+8yKuAAbr5DOR+wiZCNNwdotSql\\/3NwJkEcqa1paMvgo53YUB3fVmKmdigwPUcbQdnrhqClpCrkgCuNXQg==\"}]}\n"
            + ",\n"
            + "  deployFile_Task_zippedBytes=byte[](length=15360),\n"
            + "  deployFile_Task_chunkNumber=1,\n"
            + "  deployFile_Task_zippedBytesLength=460593\n"
            + "]",
            deployFilesTaskMessage.toString()));

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
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage14.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage15.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage16.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage17.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage18.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage19.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage20.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage21.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage22.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage23.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage24.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage25.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage26.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage27.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage28.ser"));
    skillTestHarness.dispatchMessage(Message.deserializeMessage("data/test-messages/deployFileTaskMessage29.ser"));

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
    assertEquals("[JavaBitcoindRpcClient-0.9.0.jar, Main-1.0.jar, Network-1.0.jar, TamperEvidentLog-1.0.jar, UPNPLib-1.0.jar, Utilities-1.0.jar, json-simple-1.1.1.jar]", getSortedFileNamesInDirectory(libDirectory));
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
