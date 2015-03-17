/*
 * Copyright (C) 2015 Stephen L. Reed
 */
package org.texai.skill.deployment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
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
import org.texai.skill.deployment.NetworkDeployment.CheckForDeployment;
import org.texai.skill.fileTransfer.NetworkFileTransfer;
import org.texai.skill.governance.TopmostFriendship;
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class NetworkDeploymentTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeploymentTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".TopmostFriendshipAgent.TopmostFriendshipRole";
  // the test parent service
  private static final String parentService = TopmostFriendship.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = NetworkDeployment.class.getName();
  // the test node name
  private static final String nodeName = "NetworkOperationAgent";
  // the test node name
  private static final String roleName = "NetworkDeploymentRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public NetworkDeploymentTest() {
  }

  @BeforeClass
  public static void setUpClass() {
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
            containerName + ".NetworkOperationAgent.NetworkOperationRole", // parentQualifiedName
            childQualifiedNames,
            skillClasses,
            variableNames,
            false); // areRemoteCommunicationsPermitted
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.setIsUnitTest(true);
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
   * Test of class NetworkDeployment initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            containerName + ".NetworkOperationAgent", // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    if (networkDeployment.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals("[initialize_Task, org.texai.skill.deployment.NetworkDeployment]", skillTestHarness.getOperationAndSenderServiceInfo().toString());
  }

  /**
   * Test of class CheckForDeployment.
   */
  @Test
  public void testCheckForDeployment() {
    LOGGER.info("CheckForDeployment");
    final File deploymentLogFile = new File("deployment/deployment.log");
    if (deploymentLogFile.exists()) {
      final boolean isOK = deploymentLogFile.delete();
      assertTrue(isOK);
    }
    assertFalse(deploymentLogFile.exists());
    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.pendingFileTransferContainerNames.clear();

    final CheckForDeployment checkForDeployment = networkDeployment.makeCheckForDeploymentForUnitTest();

    checkForDeployment.run();

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals("[Test]", networkDeployment.pendingFileTransferContainerNames.toString());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[transferFileRequest_Info, Test.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment --> Test.NetworkOperationAgent.NetworkFileTransferRole:NetworkFileTransfer]", sentMessage.toBriefString());
    assertTrue(deploymentLogFile.exists());
  }

  /**
   * Test of class NetworkDeployment task accomplished task message.
   */
  @Test
  public void testTaskAccomplishedInfo() {
    LOGGER.info("testing " + AHCSConstants.TASK_ACCOMPLISHED_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.pendingFileTransferContainerNames.clear();
    networkDeployment.pendingFileTransferContainerNames.add("TestRecipient");
    networkDeployment.pendingDeploymentContainerNames.clear();
    networkDeployment.pendingDeploymentContainerNames.add("TestRecipient");
    networkDeployment.zippedBytesHash = "uuPRHZF6ivQaTwMXfBf3xJHTS1lrPFHmSOP0cC1tjL1vGhiOIdwbuYHhNt2SxuD9xdfjqS2vkMfaWDunk6ZbwA==";

    final File deploymentDirectory = new File("deployment");
    if (!deploymentDirectory.exists()) {
      fail();
    }
    assertTrue(deploymentDirectory.isDirectory());

    final File[] files = deploymentDirectory.listFiles();
    if (files.length == 0) {
      fail();
    }
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
      fail();
    }
    LOGGER.info("manifestFile: " + manifestFile);

    try {
    networkDeployment.manifestJSONString = FileUtils.readFileToString(manifestFile);
    } catch (IOException ex) {
      fail();
    }

    final UUID conversationId = UUID.randomUUID();
    final Message taskAccomplishedInfoMessage = new Message(
            containerName + ".NetworkOperationAgent.NetworkFileTransferRole", // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            skillClassName, // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_DURATION, 120000L);

    networkDeployment.clearDeploymentFileTransferConversationDictionary();
    assertNotNull(taskAccomplishedInfoMessage.getConversationId());
    networkDeployment.putDeploymentFileTransferConversationDictionary(
            taskAccomplishedInfoMessage.getConversationId(),
            "TestRecipient"); // recipientContainer

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertTrue(networkDeployment.pendingFileTransferContainerNames.isEmpty());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[deployFile_Task, Test.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment --> TestRecipient.ContainerOperationAgent.ContainerDeploymentRole:ContainerDeployment]", sentMessage.toBriefString());
  }

  /**
   * Test of class NetworkDeployment task accomplished task message.
   */
  @Test
  public void testTaskAccomplishedInfo2() {
    LOGGER.info("testing " + AHCSConstants.TASK_ACCOMPLISHED_INFO + " message");
    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.pendingFileTransferContainerNames.clear();
    networkDeployment.pendingDeploymentContainerNames.clear();
    networkDeployment.pendingDeploymentContainerNames.add("TestDeployer");
    networkDeployment.zippedBytesHash = "uuPRHZF6ivQaTwMXfBf3xJHTS1lrPFHmSOP0cC1tjL1vGhiOIdwbuYHhNt2SxuD9xdfjqS2vkMfaWDunk6ZbwA==";
    final File deploymentDirectory = new File("deployment");
    if (!deploymentDirectory.exists()) {
      fail();
    }
    assertTrue(deploymentDirectory.isDirectory());

    final File[] files = deploymentDirectory.listFiles();
    if (files.length == 0) {
      fail();
    }
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
      fail();
    }
    LOGGER.info("manifestFile: " + manifestFile);

    try {
    networkDeployment.manifestJSONString = FileUtils.readFileToString(manifestFile);
    } catch (IOException ex) {
      fail();
    }


    // receive a Task Accomplished Info message from the last remaining container that has completed deploying files
    final UUID conversationId = UUID.randomUUID();
    final Message taskAccomplishedInfoMessage = new Message(
            "TestDeployer.ContainerOperationAgent.ContainerDeploymentRole", // senderQualifiedName
            ContainerDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            skillClassName, // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version
    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertTrue(networkDeployment.pendingDeploymentContainerNames.isEmpty());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[networkRestartRequest_Info, Test.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation]", sentMessage.toBriefString());
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info Test.NetworkOperationAgent.NetworkDeploymentRole:NetworkDeployment --> Test.TopmostFriendshipAgent.TopmostFriendshipRole:TopmostFriendship "));
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkDeployment.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    NetworkDeployment instance = new NetworkDeployment();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[delegatePerformMission_Task, initialize_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task, taskAccomplished_Info]", understoodOperations.toString());
  }

  /**
   * Test of getLogger method, of class NetworkDeployment.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    NetworkDeployment instance = new NetworkDeployment();
    assertNotNull(instance.getLogger());
  }

}
