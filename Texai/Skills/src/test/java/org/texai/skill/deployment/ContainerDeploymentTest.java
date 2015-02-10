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
import java.util.UUID;
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
  private static final String parentQualifiedName = "TestNO.NetworkOperationAgent.NetworkDeploymentRole";
  // the test parent service
  private static final String parentService = NetworkDeployment.class.getName();
  private static final String skillClassName = ContainerDeployment.class.getName();
  private static final String nodeName = "ContainerOperationAgent";
  private static final String roleName = "ContainerDeploymentRole";
  private static SkillTestHarness skillTestHarness;

  public ContainerDeploymentTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    ContainerDeployment.setIsUnitTest(true);
    final Set<SkillClass> skillClasses = new ArraySet<>();
    final SkillClass skillClass = new SkillClass(
            skillClassName, // skillClassName
            true); // isClassExistsTested
    skillClasses.add(skillClass);
    final Set<String> variableNames = new ArraySet<>();
    final Set<String> childQualifiedNames = new ArraySet<>();
    childQualifiedNames.add(containerName + ".ContainerOperationAgent.ContainerDeploymentRole");
    skillTestHarness = new SkillTestHarness(
            containerName + "." + nodeName, // name
            "test mission description", // missionDescription
            true, // isNetworkSingleton
            containerName + "." + nodeName + "." + roleName, // qualifiedName
            "test role description", // description
            containerName + ".NetworkOperationAgent.NetworkDeploymentRole", // parentQualifiedName
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
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            NetworkDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final ContainerDeployment containerDeployment = (ContainerDeployment) skillTestHarness.getSkill(skillClassName);
    if (containerDeployment.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
  }

  /**
   * Test of class ContainerDeployment deploy files task message.
   */
  @Test
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"RV_RETURN_VALUE_IGNORED_BAD_PRACTICE"})
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

//    [Message ...
//      senderQualifiedName:    Test.NetworkOperationAgent.NetworkDeploymentRole
//      senderService:          org.texai.skill.deployment.NetworkDeploymentnull
//      recipientQualifiedName: TestRecipient.ContainerOperationAgent.ContainerDeploymentRole
//      conversationId:         61c01972-67c4-4818-91b7-d0fb1751d0cf
//      replyWith:              null
//      inReplyTo:              null
//      dateTime:               2015-02-09T23:04:52.051-06:00
//      replyByDateTime:        null
//      recipientService:       org.texai.skill.deployment.ContainerDeployment
//      operation:              deployFile_Task
//        parameter: deployFile_Task_manifest={"manifest":[
//    {"path":"Main-1.0\/data\/nodes.xml",
//      "command":"replace",
//      "hash":"fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o\/\/\/wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA=="},
//
//    {"path":"Main-1.0\/lib\/TamperEvidentLog-1.0.jar",
//      "command":"replace",
//      "hash":"Xf2hZN+vwWhYShIKX75IUO5iGDXi177n9tNkeyLmBTiMKp8LKBwlfzkzWaohKnwUBKhevjmzsQSX0ZT4DeeJKw=="},
//
//    {"path":"Main-1.0\/lib\/UPNPLib-1.0.jar",
//      "command":"replace",
//      "hash":"otrR8RCJrBlu9gIxDl+B7TMuUfpgtkdNKNLQkDiUOA7zDxhw\/NhHD8F5hpg8IJJm2hu1Pcpsn1fRR5ijWuapNQ=="}]}
//    ,
//        parameter: filePath=data/deployment.zip,
//        parameter: fileHash=uuPRHZF6ivQaTwMXfBf3xJHTS1lrPFHmSOP0cC1tjL1vGhiOIdwbuYHhNt2SxuD9xdfjqS2vkMfaWDunk6ZbwA==
//    ]
    final Message deployFilesTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            UUID.fromString("61c01972-67c4-4818-91b7-d0fb1751d0cf"), // conversationId
            (UUID) null, // replyWith
            (UUID) null, // inReplyTo
            skillClassName, // recipientService
            AHCSConstants.DEPLOY_FILES_TASK); // operation
    final String manifestJSONString = "{\"manifest\":[\n"
            + "{\"path\":\"Main-1.0/data/nodes.xml\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o///wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0/lib/TamperEvidentLog-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"Xf2hZN+vwWhYShIKX75IUO5iGDXi177n9tNkeyLmBTiMKp8LKBwlfzkzWaohKnwUBKhevjmzsQSX0ZT4DeeJKw==\"},\n"
            + "\n"
            + " {\"path\":\"Main-1.0/lib/UPNPLib-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"otrR8RCJrBlu9gIxDl+B7TMuUfpgtkdNKNLQkDiUOA7zDxhw/NhHD8F5hpg8IJJm2hu1Pcpsn1fRR5ijWuapNQ==\"}]}";
    deployFilesTaskMessage.put(AHCSConstants.MSG_PARM_FILE_PATH, "data/deployment.zip");
    deployFilesTaskMessage.put(AHCSConstants.MSG_PARM_FILE_HASH, "Aj4bjulM+XjGAVmFNSsdcLY/V0fw9VTjT19UIOupuD3TnKjhwBownfMHaWbYZSTGOf2bEtQJBnHcDeRoDQ14Fg==");
    deployFilesTaskMessage.put(AHCSConstants.DEPLOY_FILES_TASK_MANIFEST, manifestJSONString);

    LOGGER.info(deployFilesTaskMessage.toString());
    assertEquals("[deployFile_Task, TestNO.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerOperationAgent.ContainerDeploymentRole:ContainerDeployment]", deployFilesTaskMessage.toBriefString());

    skillTestHarness.dispatchMessage(deployFilesTaskMessage);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());

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
    assertEquals(
            "[taskAccomplished_Info, Test.ContainerOperationAgent.ContainerDeploymentRole:ContainerDeployment --> TestNO.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment]",
            sentMessage.toBriefString());
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
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
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
    assertEquals("[deployFile_Task, initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
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
