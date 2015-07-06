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
package org.texai.skill.singletonConfiguration;

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
import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
import org.texai.ahcsSupport.domainEntity.SkillClass;
import org.texai.skill.governance.TopmostFriendship;
import org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration.BroadcastContainerInfos;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class NetworkSingletonConfigurationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkSingletonConfigurationTest.class);
  // the container name
  private static final String containerName = "TestMint";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".TopmostFriendshipAgent.TopmostFriendshipRole";
  // the test parent service
  private static final String parentService = TopmostFriendship.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = NetworkSingletonConfiguration.class.getName();
  // the test node name
  private static final String nodeName = "NetworkOperationAgent";
  // the test role name
  private static final String roleName = "NetworkSingletonConfigurationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public NetworkSingletonConfigurationTest() {
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
    childQualifiedNames.add(containerName + ".ContainerOperationsAgent.ContainerSingletonConfigurationRole");
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
   * Test of class NetworkSingletonConfiguration initialize task message.
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

    final NetworkSingletonConfiguration networkSingletonConfiguration = (NetworkSingletonConfiguration) skillTestHarness.getSkill(skillClassName);
    if (networkSingletonConfiguration.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals("[initialize_Task, org.texai.skill.singletonConfiguration.NetworkSingletonConfiguration]", skillTestHarness.getOperationAndSenderServiceInfo().toString());
  }

  /**
   * Test of class NetworkSingletonConfiguration - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info TestMint.NetworkOperationAgent.NetworkSingletonConfigurationRole:NetworkSingletonConfiguration --> TestMint.TopmostFriendshipAgent.TopmostFriendshipRole:TopmostFriendship "));
  }

  /**
   * Test of class NetworkSingletonConfiguration.
   */
  @Test
  public void testBroadcastContainerInfos() {
    LOGGER.info("testing BroadcastContainerInfos");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);

    final NetworkSingletonConfiguration networkSingletonConfiguration = (NetworkSingletonConfiguration) skillTestHarness.getSkill(skillClassName);
    final BroadcastContainerInfos broadcastContainerInfos = new BroadcastContainerInfos(networkSingletonConfiguration);
    broadcastContainerInfos.run();
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(sentMessage.toString().startsWith("[networkConfiguration_Task TestMint.NetworkOperationAgent.NetworkSingletonConfigurationRole:NetworkSingletonConfiguration --> TestMint.ContainerOperationsAgent.ContainerSingletonConfigurationRole:ContainerSingletonConfiguration "));
    final SingletonAgentHosts singletonAgentHosts = (SingletonAgentHosts) sentMessage.get(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS);
    assertNotNull(singletonAgentHosts);
    @SuppressWarnings("unchecked")
    final List<ContainerInfo> containerInfos = (List<ContainerInfo>) sentMessage.get(AHCSConstants.MSG_PARM_CONTAINER_INFOS);
    assertNotNull(containerInfos);
  }

  /**
   * Test of getLogger method, of class NetworkSingletonConfiguration.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    final NetworkSingletonConfiguration instance = new NetworkSingletonConfiguration();
    assertNotNull(instance.getLogger());
    assertEquals(NetworkSingletonConfiguration.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkSingletonConfiguration.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    final NetworkSingletonConfiguration instance = new NetworkSingletonConfiguration();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[configureSingletonAgentHostsRequest_Info, delegatePerformMission_Task, initialize_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, joinNetwork_Task, messageNotUnderstood_Info, messageTimeout_Info, networkJoinComplete_Info, performMission_Task, taskAccomplished_Info]", understoodOperations.toString());
  }
}
