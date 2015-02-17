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
import org.texai.skill.governance.TopmostFriendship;
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class XAINetworkOperationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(XAINetworkOperationTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = XAINetworkOperation.class.getName();
  // the test node name
  private static final String nodeName = "XAINetworkOperationAgent";
  // the test node name
  private static final String roleName = "XAINetworkOperationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public XAINetworkOperationTest() {
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
    childQualifiedNames.add(containerName + ".XAIBlockchainArchiveAgent.XAIBlockchainArchiveRole");
    childQualifiedNames.add(containerName + ".XAIClientGatewayAgent.XAIClientGatewayRole");
    childQualifiedNames.add(containerName + ".XAIContainerAuditAgent.XAIContainerAuditRole");
    childQualifiedNames.add(containerName + ".XAIContainerCertificateAuthorityAgent.XAIContainerCertificateAuthorityRole");
    childQualifiedNames.add(containerName + ".XAIFaucetAgent.XAIFaucetRole");
    childQualifiedNames.add(containerName + ".XAIFinancialAccountingAndControlAgent.XAIClientGatewayRole");
    childQualifiedNames.add(containerName + ".XAIMintAgent.XAIMintRole");
    childQualifiedNames.add(containerName + ".XAINetworkEpisodicMemoryAgent.XAINetworkEpisodicMemoryRole");
    childQualifiedNames.add(containerName + ".XAINetworkSeedAgent.XAINetworkSeedRole");
    childQualifiedNames.add(containerName + ".XAIOperationAgent.XAIOperationRole");
    childQualifiedNames.add(containerName + ".XAIPrimaryAuditAgent.XAIPrimaryAuditRole");
    childQualifiedNames.add(containerName + ".XAIRecoveryAgent.XAIRecoveryRole");
    childQualifiedNames.add(containerName + ".XAIRewardAllocationAgent.XAIRewardAllocationRole");

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
    final AbstractSkill xaiNetworkOperation = skillTestHarness.getSkill(skillClassName);
    assertNotNull(xaiNetworkOperation);
    assertTrue(xaiNetworkOperation instanceof XAINetworkOperation);
    assertTrue(xaiNetworkOperation.isUnitTest());
    assertEquals(13, xaiNetworkOperation.getRole().getChildQualifiedNames().size());
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
   * Test of class XAINetworkOperation initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService,
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final XAINetworkOperation xaiNetworkOperation = (XAINetworkOperation) skillTestHarness.getSkill(skillClassName);
    if (xaiNetworkOperation.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals(
            "[initialize_Task, org.texai.skill.aicoin.XAINetworkOperation]",
            skillTestHarness.getOperationAndSenderServiceInfo().toString());
    assertEquals(
            "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIBlockchainArchiveAgent.XAIBlockchainArchiveRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIClientGatewayAgent.XAIClientGatewayRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIContainerAuditAgent.XAIContainerAuditRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIContainerCertificateAuthorityAgent.XAIContainerCertificateAuthorityRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIFaucetAgent.XAIFaucetRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIFinancialAccountingAndControlAgent.XAIClientGatewayRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIMintAgent.XAIMintRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAINetworkEpisodicMemoryAgent.XAINetworkEpisodicMemoryRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAINetworkSeedAgent.XAINetworkSeedRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIOperationAgent.XAIOperationRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIPrimaryAuditAgent.XAIPrimaryAuditRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIRecoveryAgent.XAIRecoveryRole:]\n"
            + "[initialize_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIRewardAllocationAgent.XAIRewardAllocationRole:]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class XAINetworkOperation perform mission task message.
   */
  @Test
  public void testPerformMissionTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.PERFORM_MISSION_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final AbstractSkill xaiNetworkOperation = skillTestHarness.getSkill(XAINetworkOperation.class.getName());
    assertNotNull(xaiNetworkOperation);
    assertTrue(xaiNetworkOperation instanceof XAINetworkOperation);
    assertTrue(xaiNetworkOperation.isUnitTest());
    assertEquals(13, xaiNetworkOperation.getRole().getChildQualifiedNames().size());
    final Message performTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService,
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    LOGGER.info(performTaskMessage);

    skillTestHarness.dispatchMessage(performTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertEquals("[performMission_Task, org.texai.skill.aicoin.XAINetworkOperation]", skillTestHarness.getOperationAndSenderServiceInfo().toString());

    LOGGER.info("traced sent messages ...");
    skillTestHarness.getSentMessages().stream().sorted().forEach((Message sentMessage) -> {
      LOGGER.info(sentMessage.toTraceString());
      LOGGER.info("");
    });
    assertEquals(
            "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIBlockchainArchiveAgent.XAIBlockchainArchiveRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIClientGatewayAgent.XAIClientGatewayRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIContainerAuditAgent.XAIContainerAuditRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIContainerCertificateAuthorityAgent.XAIContainerCertificateAuthorityRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIFaucetAgent.XAIFaucetRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIFinancialAccountingAndControlAgent.XAIClientGatewayRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIMintAgent.XAIMintRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAINetworkEpisodicMemoryAgent.XAINetworkEpisodicMemoryRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAINetworkSeedAgent.XAINetworkSeedRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIOperationAgent.XAIOperationRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIPrimaryAuditAgent.XAIPrimaryAuditRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIRecoveryAgent.XAIRecoveryRole:]\n"
            + "[performMission_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIRewardAllocationAgent.XAIRewardAllocationRole:]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class XAINetworkOperation - Restart Container Task.
   */
  @Test
  public void testRestartContainer() {
    LOGGER.info("testing " + AHCSConstants.RESTART_CONTAINER_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message taskAccomplishedInfoMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.RESTART_CONTAINER_TASK); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    assertEquals(
            "[shutdownAicoind_Task, Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.XAIOperationAgent.XAIOperationRole:XAIOperation]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class XAINetworkOperation - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info Test.XAINetworkOperationAgent.XAINetworkOperationRole:XAINetworkOperation --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation "));
  }

  /**
   * Test of getLogger method, of class XAINetworkOperation.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    XAINetworkOperation instance = new XAINetworkOperation();
    assertNotNull(instance.getLogger());
    assertEquals(XAINetworkOperation.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class XAINetworkOperation.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    XAINetworkOperation instance = new XAINetworkOperation();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals(
            "[delegatePerformMission_Task, initialize_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task, restartContainer_Task]",
            understoodOperations.toString());
  }

}
