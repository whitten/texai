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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class ContainerSingletonConfigurationTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerSingletonConfigurationTest.class);
  // the container name
  private static final String containerName = "TestMint";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkSingletonConfigurationRole";
  // the test parent service
  private static final String parentService = NetworkSingletonConfiguration.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = ContainerSingletonConfiguration.class.getName();
  // the test node name
  private static final String nodeName = "ContainerOperationAgent";
  // the test role name
  private static final String roleName = "ContainerSingletonConfigurationRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public ContainerSingletonConfigurationTest() {
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
    childQualifiedNames.add(containerName + ".ContainerOperationAgent.ConfigureParentToSingletonRole");
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
   * Test of class ContainerSingletonConfiguration initialize task message.
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

    final ContainerSingletonConfiguration containerSingletonConfiguration = (ContainerSingletonConfiguration) skillTestHarness.getSkill(skillClassName);
    if (containerSingletonConfiguration.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndSenderServiceInfo());
    assertEquals("[initialize_Task, org.texai.skill.singletonConfiguration.ContainerSingletonConfiguration]", skillTestHarness.getOperationAndSenderServiceInfo().toString());
  }

  /**
   * Test of class ContainerSingletonConfiguration - Message Not Understood Info.
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
    assertTrue(sentMessage.toString().startsWith("[messageNotUnderstood_Info TestMint.ContainerOperationAgent.ContainerSingletonConfigurationRole:ContainerSingletonConfiguration --> TestMint.NetworkOperationAgent.NetworkSingletonConfigurationRole:NetworkSingletonConfiguration "));
  }

  /**
   * Test of class ContainerSingletonConfiguration - Network Configuration Task.
   */
  @Test
  public void testNetworkConfigurationTask() {
    LOGGER.info("testing " + AHCSConstants.NETWORK_CONFIGURATION_TASK + " message");

    skillTestHarness.reset();
    SingletonAgentHosts singletonAgentHosts = skillTestHarness.getNodeRuntime().getSingletonAgentHosts();
    assertEquals("[SingletonAgentHosts, size 11]", singletonAgentHosts.toString());
    List<ContainerInfo> containerInfos = skillTestHarness.getNodeRuntime().getContainerInfos();
    assertEquals("[[container TestAlice,  TestAlice, super peer, gateway], [container TestBlockchainExplorer,  TestBlockchainExplorer, super peer, block explorer], [container TestBob,  TestBob, gateway], [container TestMint,  TestMint, super peer, first container]]", containerInfos.toString());
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);

    final Message taskAccomplishedInfoMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.NETWORK_CONFIGURATION_TASK); // operation

    // clone and modify the singleton agent hosts
    final Map<String, String> singletonAgentDictionary = new HashMap<>(singletonAgentHosts.getSingletonAgentDictionary());
    singletonAgentDictionary.put("TestAgent", "TestContainer");
    final SingletonAgentHosts singletonAgentHosts1 = new SingletonAgentHosts(
            singletonAgentDictionary,
            singletonAgentHosts.getEffectiveDateTime(),
            singletonAgentHosts.getTerminationDateTime());
    assertEquals("[SingletonAgentHosts, size 12]", singletonAgentHosts1.toString());
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_SINGLETON_AGENT_HOSTS, singletonAgentHosts1);

    final ContainerInfo testNodeInfo = new ContainerInfo(
              "Test",
              true, // isSuperPeer
              false, // isFirstContainer
              false, // isClientGateway
              false); // isBlockExplorer
    testNodeInfo.setIpAddress("Test");

    final List<ContainerInfo> containerInfos1 = new ArrayList<>(containerInfos);
    containerInfos1.add(testNodeInfo);
    Collections.sort(containerInfos);
    assertEquals("[[container TestAlice,  TestAlice, super peer, gateway], [container TestBlockchainExplorer,  TestBlockchainExplorer, super peer, block explorer], [container TestBob,  TestBob, gateway], [container TestMint,  TestMint, super peer, first container], [container Test,  Test, super peer]]", containerInfos1.toString());
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_CONTAINER_INFOS, containerInfos1);

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNull(sentMessage);

    singletonAgentHosts = skillTestHarness.getNodeRuntime().getSingletonAgentHosts();
    assertEquals("[SingletonAgentHosts, size 12]", singletonAgentHosts.toString());
    containerInfos = skillTestHarness.getNodeRuntime().getContainerInfos();
    assertEquals("[[container Test,  Test, super peer], [container TestAlice,  TestAlice, super peer, gateway], [container TestBlockchainExplorer,  TestBlockchainExplorer, super peer, block explorer], [container TestBob,  TestBob, gateway], [container TestMint,  TestMint, super peer, first container]]", containerInfos.toString());
  }

  /**
   * Test of getLogger method, of class ContainerSingletonConfiguration.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    final ContainerSingletonConfiguration instance = new ContainerSingletonConfiguration();
    assertNotNull(instance.getLogger());
    assertEquals(ContainerSingletonConfiguration.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class ContainerSingletonConfiguration.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerSingletonConfiguration instance = new ContainerSingletonConfiguration();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[addUnjoinedRole_Info, initialize_Task, joinAcknowledged_Task, joinNetwork_Task, messageNotUnderstood_Info, messageTimeout_Info, networkConfiguration_Task, performMission_Task, removeUnjoinedRole_Info, seedConnectionRequest_Info, singletonAgentHosts_Info, taskAccomplished_Info]", understoodOperations.toString());
  }
}
