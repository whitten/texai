package org.texai.skill.testHarness;

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
import org.texai.skill.deployment.NetworkDeployment;
import org.texai.skill.network.NetworkOperation;
import org.texai.util.ArraySet;

/**
 * SkillTestHarnessTest.java
 *
 * Description:
 *
 * Copyright (C) Jan 16, 2015, Stephen L. Reed.
 */
public class SkillTestHarnessTest {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(SkillTestHarnessTest.class);

  /**
   * Creates a new instance of SkillTestHarnessTest.
   */
  public SkillTestHarnessTest() {
  }

  @BeforeClass
  public static void setUpClass() {
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
   * Test of class SkillTestHarness.
   */
  @Test
  public void testSkillTestHarness() {
    LOGGER.info("SkillTestHarness");

    final String skillClassName = NetworkDeployment.class.getName();
    final String nodeName = "NetworkDeploymentAgent";
    final String roleName = "NetworkDeploymentRole";

    final String containerName = "TestMint";
    final Set<SkillClass> skillClasses = new ArraySet<>();
    final SkillClass skillClass = new SkillClass(
          skillClassName, // skillClassName
          true); // isClassExistsTested
    skillClasses.add(skillClass);
    final Set<String> variableNames = new ArraySet<>();
    final Set<String> childQualifiedNames = new ArraySet<>();
    childQualifiedNames.add(containerName + ".ContainerDeploymentAgent.ContainerDeploymentRole");
    final SkillTestHarness skillTestHarness = new SkillTestHarness(
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

    final Message initializeMessage = new Message(
          containerName + ".NetworkOperationAgent", // senderQualifiedName
          NetworkOperation.class.getName(), // senderService
          containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
          skillClassName, // recipientService
          AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);
    skillTestHarness.getSkillState(skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertEquals("[initialize_Task, org.texai.skill.deployment.NetworkDeployment]", skillTestHarness.getOperationAndSenderServiceInfo().toString());
  }

}
