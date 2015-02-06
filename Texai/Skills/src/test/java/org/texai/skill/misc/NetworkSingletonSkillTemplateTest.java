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
public class NetworkSingletonSkillTemplateTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkSingletonSkillTemplate.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the test child qualified name
  private static final String childQualifiedName = containerName + ".ContainerDeploymentAgent.ContainerDeploymentRole";
  // the class name of the tested skill
  private static final String skillClassName = NetworkSingletonSkillTemplate.class.getName();
  // the test node name
  private static final String nodeName = "NetworkSingletonSkillTemplateAgent";
  // the test role name
  private static final String roleName = "NetworkSingletonSkillTemplateRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public NetworkSingletonSkillTemplateTest() {
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
   * Test of class NetworkDeployment initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            NetworkSingletonSkillTemplate.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final NetworkSingletonSkillTemplate networkSingletonSkillTemplate = (NetworkSingletonSkillTemplate) skillTestHarness.getSkill(skillClassName);
    if (networkSingletonSkillTemplate.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[AHCS initialize_Task, org.texai.skill.misc.NetworkSingletonSkillTemplate]", skillTestHarness.getOperationAndServiceInfo().toString());
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
    assertEquals("[messageNotUnderstood_Info, Test.NetworkSingletonSkillTemplateAgent.NetworkSingletonSkillTemplateRole:NetworkSingletonSkillTemplate --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation]",
            sentMessage.toBriefString());
  }

  /**
   * Test of getLogger method, of class NetworkSingletonSkillTemplate.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    NetworkSingletonSkillTemplate instance = new NetworkSingletonSkillTemplate();
    assertNotNull(instance.getLogger());
    assertEquals(NetworkSingletonSkillTemplate.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkSingletonSkillTemplate.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    NetworkSingletonSkillTemplate instance = new NetworkSingletonSkillTemplate();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, delegateBecomeReady_Task, delegatePerformMission_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

}
