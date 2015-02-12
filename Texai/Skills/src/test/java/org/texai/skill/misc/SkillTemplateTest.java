/*
 * Copyright (C) 2015 Texai
 *
 */
package org.texai.skill.misc;

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
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class SkillTemplateTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SkillTemplateTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the test child qualified name
  private static final String childQualifiedName = containerName + ".ContainerDeploymentAgent.ContainerDeploymentRole";
  // the class name of the tested skill
  private static final String skillClassName = SkillTemplate.class.getName();
  // the test node name
  private static final String nodeName = "SkillTemplateAgent";
  // the test role name
  private static final String roleName = "SkillTemplateRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public SkillTemplateTest() {
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
    childQualifiedNames.add(childQualifiedName);
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
   * Test of class SkillTemplate initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final SkillTemplate skillTemplate = (SkillTemplate) skillTestHarness.getSkill(skillClassName);
    if (skillTemplate.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
  }

  /**
   * Test of class SkillTemplate - Message Not Understood Info.
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
    assertEquals("[messageNotUnderstood_Info, Test.SkillTemplateAgent.SkillTemplateRole:SkillTemplate --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation]",
            sentMessage.toBriefString());
  }

  /**
   * Test of class SkillTemplate - Join Acknowledged Task.
   */
  @Test
  public void testJoinAcknowledgedTask() {
    LOGGER.info("testing " + AHCSConstants.JOIN_ACKNOWLEDGED_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK, skillClassName);
    final Message joinAcknowledgedTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.JOIN_ACKNOWLEDGED_TASK); // operation

    skillTestHarness.dispatchMessage(joinAcknowledgedTaskMessage);

    assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[removeUnjoinedRole_Info, Test.SkillTemplateAgent.SkillTemplateRole:SkillTemplate --> Test.ContainerOperationAgent.ContainerSingletonConfigurationRole:ContainerSingletonConfiguration]",
            sentMessage.toBriefString());
  }

  /**
   * Test of getLogger method, of class SkillTemplate.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    SkillTemplate instance = new SkillTemplate();
    assertNotNull(instance.getLogger());
    assertEquals(SkillTemplate.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class SkillTemplate.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    SkillTemplate instance = new SkillTemplate();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

}
