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
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.ahcsSupport.skill.AbstractSkill;
import org.texai.skill.aicoin.AICNetworkOperation.ContainerNameRingItem;
import org.texai.skill.aicoin.AICNetworkOperation.GenerateNewBlockRunnable;
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class AICNetworkOperationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICNetworkOperationTest.class);
  // the container name
  private static final String containerName = "TestMint";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = AICNetworkOperation.class.getName();
  // the test node name
  private static final String nodeName = "AICNetworkOperationAgent";
  // the test node name
  private static final String roleName = "AICNetworkOperationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public AICNetworkOperationTest() {
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
    childQualifiedNames.add(containerName + ".AICBlockchainArchiveAgent.AICBlockchainArchiveRole");
    childQualifiedNames.add(containerName + ".AICClientGatewayAgent.AICClientGatewayRole");
    childQualifiedNames.add(containerName + ".AICContainerAuditAgent.AICContainerAuditRole");
    childQualifiedNames.add(containerName + ".AICContainerCertificateAuthorityAgent.AICContainerCertificateAuthorityRole");
    childQualifiedNames.add(containerName + ".AICFaucetAgent.AICFaucetRole");
    childQualifiedNames.add(containerName + ".AICFinancialAccountingAndControlAgent.AICClientGatewayRole");
    childQualifiedNames.add(containerName + ".AICMintAgent.AICMintRole");
    childQualifiedNames.add(containerName + ".AICNetworkEpisodicMemoryAgent.AICNetworkEpisodicMemoryRole");
    childQualifiedNames.add(containerName + ".AICNetworkSeedAgent.AICNetworkSeedRole");
    childQualifiedNames.add(containerName + ".AICOperationAgent.AICOperationRole");
    childQualifiedNames.add(containerName + ".AICPrimaryAuditAgent.AICPrimaryAuditRole");
    childQualifiedNames.add(containerName + ".AICRecoveryAgent.AICRecoveryRole");
    childQualifiedNames.add(containerName + ".AICRewardAllocationAgent.AICRewardAllocationRole");

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
    assertTrue(xaiNetworkOperation instanceof AICNetworkOperation);
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
   * Test of class AICNetworkOperation initialize task message.
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

    final AICNetworkOperation xaiNetworkOperation = (AICNetworkOperation) skillTestHarness.getSkill(skillClassName);
    if (xaiNetworkOperation.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals(
            "[initialize_Task, org.texai.skill.aicoin.AICNetworkOperation]",
            skillTestHarness.getOperationAndSenderServiceInfo().toString());
    assertEquals(
            "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICBlockchainArchiveAgent.AICBlockchainArchiveRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICClientGatewayAgent.AICClientGatewayRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICContainerAuditAgent.AICContainerAuditRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICContainerCertificateAuthorityAgent.AICContainerCertificateAuthorityRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICFaucetAgent.AICFaucetRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICFinancialAccountingAndControlAgent.AICClientGatewayRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICMintAgent.AICMintRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICNetworkEpisodicMemoryAgent.AICNetworkEpisodicMemoryRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICNetworkSeedAgent.AICNetworkSeedRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICOperationAgent.AICOperationRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICPrimaryAuditAgent.AICPrimaryAuditRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICRecoveryAgent.AICRecoveryRole:]\n"
            + "[initialize_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICRewardAllocationAgent.AICRewardAllocationRole:]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICNetworkOperation perform mission task message.
   */
  @Test
  public void testPerformMissionTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.PERFORM_MISSION_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final AbstractSkill aicNetworkOperation = skillTestHarness.getSkill(AICNetworkOperation.class.getName());
    assertNotNull(aicNetworkOperation);
    assertTrue(aicNetworkOperation instanceof AICNetworkOperation);
    assertTrue(aicNetworkOperation.isUnitTest());
    assertEquals(13, aicNetworkOperation.getRole().getChildQualifiedNames().size());
    final Message performTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService,
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    LOGGER.info(performTaskMessage);

    skillTestHarness.dispatchMessage(performTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertEquals("[performMission_Task, org.texai.skill.aicoin.AICNetworkOperation]", skillTestHarness.getOperationAndSenderServiceInfo().toString());

    LOGGER.info("traced sent messages ...");
    skillTestHarness.getSentMessages().stream().sorted().forEach((Message sentMessage) -> {
      LOGGER.info(sentMessage.toTraceString());
      LOGGER.info("");
    });
    assertEquals(
            "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICBlockchainArchiveAgent.AICBlockchainArchiveRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICClientGatewayAgent.AICClientGatewayRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICContainerAuditAgent.AICContainerAuditRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICContainerCertificateAuthorityAgent.AICContainerCertificateAuthorityRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICFaucetAgent.AICFaucetRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICFinancialAccountingAndControlAgent.AICClientGatewayRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICMintAgent.AICMintRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICNetworkEpisodicMemoryAgent.AICNetworkEpisodicMemoryRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICNetworkSeedAgent.AICNetworkSeedRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICOperationAgent.AICOperationRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICPrimaryAuditAgent.AICPrimaryAuditRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICRecoveryAgent.AICRecoveryRole:]\n"
            + "[performMission_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICRewardAllocationAgent.AICRewardAllocationRole:]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICNetworkOperation - Restart Container Task.
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
            "[shutdownAicoind_Task, TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestMint.AICOperationAgent.AICOperationRole:AICOperation]\n",
            Message.toBriefString(skillTestHarness.getSentMessages()));
  }

  /**
   * Test of class AICNetworkOperation - GenerateNewBlockRunnable.
   */
  @Test
  public void testGenerateNewBlockRunnable() {
    LOGGER.info("testing GenerateNewBlockRunnable");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    skillTestHarness.getNodeRuntime().getContainerInfos().stream().forEach((ContainerInfo containerInfo) -> {
      containerInfo.setIsAlive(true);
    });
    final AICNetworkOperation aicNetworkOperation = (AICNetworkOperation) skillTestHarness.getSkill(AICNetworkOperation.class.getName());
    List<ContainerNameRingItem> superPeerContainerNameRing = aicNetworkOperation.createSuperPeerContainerNameRing();
    LOGGER.info(superPeerContainerNameRing);
    assertEquals("[[TestAlice], [TestBlockchainExplorer], [TestMint]]", superPeerContainerNameRing.toString());
    final GenerateNewBlockRunnable generateNewBlockRunnable = new GenerateNewBlockRunnable(aicNetworkOperation);
    generateNewBlockRunnable.run();
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final List<Message> sentMessages = skillTestHarness.getSentMessages();
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info(sentMessage);
    assertTrue(sentMessage.toString().startsWith("[generateCoinBlock_Task TestMint.AICNetworkOperationAgent.AICNetworkOperationRole:AICNetworkOperation --> TestAlice.AICMintAgent.AICMintRole:AICMint "));
  }

  /**
   * Test of getLogger method, of class AICNetworkOperation.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    AICNetworkOperation instance = new AICNetworkOperation();
    assertNotNull(instance.getLogger());
    assertEquals(AICNetworkOperation.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class AICNetworkOperation.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    AICNetworkOperation instance = new AICNetworkOperation();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals(
            "[delegatePerformMission_Task, initialize_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task, restartContainer_Task]",
            understoodOperations.toString());
  }

}
