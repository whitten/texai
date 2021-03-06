/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.skill.aicoin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.skill.network.ContainerOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class AICOperationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICOperationTest.class);
  // the container name
  private static final String containerName = "TestMint";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".AICNetworkOperationAgent.AICNetworkOperationRole";
  // the test parent service
  private static final String parentService = AICNetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = AICOperation.class.getName();
  // the test node name
  private static final String nodeName = "AICOperationAgent";
  // the test node name
  private static final String roleName = "AICOperationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public AICOperationTest() {
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

    skillTestHarness = new SkillTestHarness(
            containerName + "." + nodeName, // name
            "test mission description", // missionDescription
            true, // isNetworkSingleton
            containerName + "." + nodeName + "." + roleName, // qualifiedName
            "test role description", // description
            parentQualifiedName,
            childQualifiedNames,
            skillClasses,
            variableNames,
            false); // areRemoteCommunicationsPermitted
    final AbstractSkill xaiOperation = skillTestHarness.getSkill(skillClassName);
    assertNotNull(xaiOperation);
    assertTrue(AICOperation.class.isAssignableFrom(xaiOperation.getClass()));
    assertTrue(xaiOperation.isUnitTest());
    assertEquals(0, xaiOperation.getRole().getChildQualifiedNames().size());
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
   * Test of class AICOperation initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            AICNetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final AICOperation xaiOperation = (AICOperation) skillTestHarness.getSkill(skillClassName);
    if (xaiOperation.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals(
            "",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICOperation perform mission task message.
   */
  @Test
  public void testPerformMissionTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.PERFORM_MISSION_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final AbstractSkill xaiOperation = skillTestHarness.getSkill(AICOperation.class.getName());
    assertNotNull(xaiOperation);
    assertTrue(AICOperation.class.isAssignableFrom(xaiOperation.getClass()));
    assertTrue(xaiOperation.isUnitTest());
    assertEquals(0, xaiOperation.getRole().getChildQualifiedNames().size());
    final Message performTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService,
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    LOGGER.info(performTaskMessage);

    skillTestHarness.dispatchMessage(performTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());

    LOGGER.info("traced sent messages ...");
    skillTestHarness.getSentMessages().stream().sorted().forEach((Message sentMessage) -> {
      LOGGER.info(sentMessage.toTraceString());
      LOGGER.info("");
    });
    assertEquals(
            "[writeConfigurationFile_Info, TestMint.AICOperationAgent.AICOperationRole:AICOperation --> TestMint.AICOperationAgent.AICOperationRole:AICWriteConfigurationFile]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICNetworkOperation - Shutdown Aicoind Task.
   */
  @Test
  public void testShutdownAicoindTask() {
    LOGGER.info("testing " + AHCSConstants.SHUTDOWN_AICOIND_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message taskAccomplishedInfoMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.SHUTDOWN_AICOIND_TASK); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNull(sentMessage);
    assertEquals(
            "",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICNetworkOperation - Shutdown Aicoind Task.
   */
  @Test
  public void testShutdownAicoindRequestInfo() {
    LOGGER.info("testing " + AHCSConstants.SHUTDOWN_AICOIND_REQUEST_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message taskAccomplishedInfoMessage = new Message(
            "TestMint.ContainerOperationAgent.ContainerOperationRole", // senderQualifiedName
            ContainerOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.SHUTDOWN_AICOIND_REQUEST_INFO); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNull(sentMessage);
    assertEquals(
            "",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICOperation - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info TestMint.AICOperationAgent.AICOperationRole:AICOperation --> TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation "));
  }

  /**
   * Test of getLogger method, of class AICOperation.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    AICOperation instance = new AICOperation();
    assertNotNull(instance.getLogger());
    assertEquals(AICOperation.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class AICOperation.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    AICOperation instance = new AICOperation();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals(
            "[bitcoinMessage_Info, connectionRequestApproved_Info, connectionRequest_Info, initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task, shutdownAicoindRequest_Info, shutdownAicoind_Task, taskAccomplished_Info]",
            understoodOperations.toString());
  }

}
