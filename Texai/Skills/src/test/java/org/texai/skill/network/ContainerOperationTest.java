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
package org.texai.skill.network;

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
import org.texai.skill.heartbeat.ContainerHeartbeat;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class ContainerOperationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerOperationTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = ContainerOperation.class.getName();
  // the test node name
  private static final String nodeName = "ContainerOperationAgent";
  // the test role name
  private static final String roleName = "ContainerOperationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public ContainerOperationTest() {
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
   * Test of class ContainerOperation initialize task message.
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

    final ContainerOperation containerOperation = (ContainerOperation) skillTestHarness.getSkill(skillClassName);
    if (containerOperation.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
  }

  /**
   * Test of class ContainerOperation - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info Test.ContainerOperationAgent.ContainerOperationRole:ContainerOperation --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation "));
  }

  /**
   * Test of class ContainerOperation - Restart Container Task.
   */
  @Test
  public void testRestartContainerTask() {
    LOGGER.info("testing " + AHCSConstants.RESTART_CONTAINER_TASK + " message");

    skillTestHarness.reset();
    assertFalse(skillTestHarness.isTerminated());
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message restartContainerTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.RESTART_CONTAINER_TASK); // operation
    restartContainerTaskMessage.put(AHCSConstants.RESTART_CONTAINER_TASK_DELAY, 5000L);

    skillTestHarness.dispatchMessage(restartContainerTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNull(sentMessage);
    assertTrue(skillTestHarness.isTerminated());
  }

  /**
   * Test of class ContainerOperation - Restart Container Request Info.
   */
  @Test
  public void testRestartContainerRequestInfo() {
    LOGGER.info("testing " + AHCSConstants.RESTART_CONTAINER_REQUEST_INFO + " message");

    skillTestHarness.reset();
    assertFalse(skillTestHarness.isTerminated());
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message restartContainerTaskMessage = new Message(
            "Test.ContainerOperationAgent.ContainerHeartbeatRole", // senderQualifiedName
            ContainerHeartbeat.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.RESTART_CONTAINER_REQUEST_INFO); // operation
    restartContainerTaskMessage.put(AHCSConstants.RESTART_CONTAINER_TASK_DELAY, 5000L);

    skillTestHarness.dispatchMessage(restartContainerTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertEquals("[shutdownAicoindRequest_Info, Test.ContainerOperationAgent.ContainerOperationRole:ContainerOperation --> Test.AICOperationAgent.AICOperationRole:AICOperation]", sentMessage.toBriefString());
    assertTrue(skillTestHarness.isTerminated());
  }

  /**
   * Test of getLogger method, of class ContainerOperation.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    ContainerOperation instance = new ContainerOperation();
    assertNotNull(instance.getLogger());
    assertEquals(ContainerOperation.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class ContainerOperation.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerOperation instance = new ContainerOperation();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, operationNotPermitted_Info, performMission_Task, restartContainerRequest_Info, restartContainer_Task]", understoodOperations.toString());
  }

}
