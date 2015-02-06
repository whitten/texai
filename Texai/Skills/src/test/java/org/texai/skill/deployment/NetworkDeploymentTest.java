/*
 * Copyright (C) 2015 Stephen L. Reed
 */
package org.texai.skill.deployment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = NetworkDeployment.class.getName();
  // the test node name
  private static final String nodeName = "NetworkDeploymentAgent";
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
    assertNotNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[AHCS initialize_Task, org.texai.skill.deployment.NetworkDeployment]", skillTestHarness.getOperationAndServiceInfo().toString());
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
    networkDeployment.undeployedContainerNames.clear();

    final CheckForDeployment checkForDeployment = networkDeployment.makeCheckForDeploymentForUnitTest();

    checkForDeployment.run();

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[Test]", networkDeployment.undeployedContainerNames.toString());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[deployFile_Task, Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment]", sentMessage.toBriefString());
    assertTrue(deploymentLogFile.exists());
    try {
      assertEquals(
              "deployed to Test\n",
              FileUtils.readFileToString(deploymentLogFile));
      final List<Message> sentMessages = skillTestHarness.getSentMessages();
      for (int i = 0; i < sentMessages.size(); i++) {
        final Message sentMessage1 = sentMessages.get(i);
        sentMessage1.serializeToFile("data/test-messages/deployFileTaskMessage" + i + ".ser");
      }
    } catch (IOException ex) {
      fail();
    }
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
    networkDeployment.undeployedContainerNames.add("Test");
    final Message taskAccomplishedInfoMessage = new Message(
            containerName + ".NetworkOperationAgent", // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    assertTrue(networkDeployment.undeployedContainerNames.isEmpty());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[networkRestartRequest_Info Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation 2015-01-22T15:16:22.534-06:00]"));
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation "));
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
    assertEquals("[AHCS initialize_Task, delegateBecomeReady_Task, delegatePerformMission_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
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
