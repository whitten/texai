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
package org.texai.skill.deployment;

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
public class ContainerDeploymentTest {

  // the logger

  private static final Logger LOGGER = Logger.getLogger(ContainerDeploymentTest.class);
  private static final String containerName = "Test";
  private static final String skillClassName = ContainerDeployment.class.getName();
  private static final String nodeName = "ContainerDeploymentAgent";
  private static final String roleName = "ContainerDeploymentRole";
  private static SkillTestHarness skillTestHarness;

  public ContainerDeploymentTest() {
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
    childQualifiedNames.add(containerName + ".ContainerDeploymentAgent.ContainerDeploymentRole");
    skillTestHarness = new SkillTestHarness(
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
   * Test of class ContainerDeployment initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.AHCS_INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    final Message initializeMessage = new Message(
            containerName + ".NetworkDeploymentAgent", // senderQualifiedName
            NetworkDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);
    skillTestHarness.getSkillState(skillClassName);
    assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
  }

  /**
   * Test of class ContainerDeployment deploy files task message.
   */
  @Test
  public void testDeployFilesTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.DEPLOY_FILES_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(
            AHCSConstants.State.READY, // state
            skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    final Message deplopyFilesTaskMessage = Message.deserializeMessage("data/test-messages/deployFileTaskMessage.ser");
    LOGGER.info(deplopyFilesTaskMessage.toString());
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-21T21:50:44.365-06:00\n"
            + "  deployFile_Task_manifest={\"manifest\":[\n"
            + "{\"path\":\"Documents.csv\",\n"
            + "  \"command\":\"remove\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/data\\/SingletonConfiguration.crt\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"UMzMC+4EfDl1vMlGoiXYJcy77nhgB5rDLM+R2PS82y\\/iWidHG+gKtCsarPSlB0gSloAVISgtUfeW\\/vRq0CbzSA==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/data\\/nodes.xml\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"teMoqtBq7Eujb1f8L96DUN+IXlGub0LOqgzA858Q0H0R+ipJZg7zfp\\/s78Ds72SaY1YIK6yACUHf6Hze8VeZLw==\"},\n"
            + "\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/json-simple-1.1.1.jar\",\n"
            + "  \"command\":\"remove\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0\\/lib\\/json-simple-1.1.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"+cqvwEHuqYLVuiZkgkGNygXUb7mSvvPwdqg9VkdlCDoIcOMPaOfG\\/IyA69PIiwh+7NL4RVwS2q99s7WpdbYi5A==\"},\n"
            + "\n"
            + "{\"path\":\"Main-1.0.tar.gz\",\n"
            + "  \"command\":\"remove\"},\n"
            + "\n"
            + "{\"path\":\"deployment\",\n"
            + "  \"command\":\"remove-dir\"}]}\n"
            + ",\n"
            + "  deployFile_Task_zippedBytes=byte[](length=17946)\n"
            + "]",
            deplopyFilesTaskMessage.toString()));
    skillTestHarness.dispatchMessage(deplopyFilesTaskMessage);
    skillTestHarness.getSkillState(skillClassName);
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkDeployment.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerDeployment instance = new ContainerDeployment();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, becomeReady_Task, deployFile_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

  /**
   * Test of getLogger method, of class NetworkDeployment.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    ContainerDeployment instance = new ContainerDeployment();
    assertNotNull(instance.getLogger());
  }

}
