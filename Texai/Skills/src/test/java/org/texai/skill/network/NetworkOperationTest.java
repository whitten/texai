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
import org.texai.skill.governance.TopmostFriendship;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class NetworkOperationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkOperationTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".TopmostFriendshipAgent.TopmostFriendshipRole";
  // the test parent service
  private static final String parentService = TopmostFriendship.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = NetworkOperation.class.getName();
  // the test node name
  private static final String nodeName = "NetworkOperationAgent";
  // the test node name
  private static final String roleName = "NetworkOperationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public NetworkOperationTest() {
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
    childQualifiedNames.add(containerName + ".XAINetworkOperationAgent.XAINetworkOperationRole");
    childQualifiedNames.add(containerName + ".NetworkSingletonConfigurationAgent.NetworkSingletonConfigurationRole");
    childQualifiedNames.add(containerName + ".ContainerOperationAgent.ContainerOperationRole");
    childQualifiedNames.add(containerName + ".NetworkDeploymentAgent.NetworkDeploymentRole");

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
   * Test of class NetworkOperation initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.AHCS_INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final NetworkOperation networkOperation = (NetworkOperation) skillTestHarness.getSkill(skillClassName);
    if (networkOperation.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[AHCS initialize_Task, org.texai.skill.network.NetworkOperation]", skillTestHarness.getOperationAndServiceInfo().toString());
  }

  /**
   * Test of class NetworkOperation perform mission task message.
   */
  @Test
  public void testPerformMissionTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.PERFORM_MISSION_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message performTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            TopmostFriendship.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.PERFORM_MISSION_TASK); // operation
    LOGGER.info(performTaskMessage);

    skillTestHarness.dispatchMessage(performTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    final List<Message> sentMessages = skillTestHarness.getSentMessages();
    LOGGER.info("messages send as a result of receiving " + AHCSConstants.PERFORM_MISSION_TASK + " ...");
    sentMessages.stream().sorted().forEach((Message message) -> {
      LOGGER.info(message);
    });
    assertEquals(4, sentMessages.size());
  }

  /**
   * Test of class NetworkOperation - Network Restart Request Info.
   */
  @Test
  public void testRestartContainer() {
    LOGGER.info("testing " + AHCSConstants.NETWORK_RESTART_REQUEST_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message taskAccomplishedInfoMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.NETWORK_RESTART_REQUEST_INFO); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[restartContainer_Task Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation --> Test.ContainerOperationAgent.ContainerOperationRole:ContainerOperation 2015-01-22T19:35:57.159-06:00\n"
            + "  restartContainer_Task_delay=5000\n"
            + "]"));
  }

  /**
   * Test of class NetworkOperation - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation --> Test.TopmostFriendshipAgent.TopmostFriendshipRole:TopmostFriendship "));
  }

  /**
   * Test of getLogger method, of class NetworkOperation.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    NetworkOperation instance = new NetworkOperation();
    assertNotNull(instance.getLogger());
    assertEquals(NetworkOperation.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkOperation.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    NetworkOperation instance = new NetworkOperation();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, delegateBecomeReady_Task, delegateConfigureSingletonAgentHosts_Task, delegatePerformMission_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, joinNetwork_Task, messageNotUnderstood_Info, networkJoinComplete_Info, networkRestartRequest_Info, performMission_Task]",
            understoodOperations.toString());
  }

}