/*
 * Copyright (C) 2015 Stephen L. Reed
 */
package org.texai.skill.deployment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
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
import org.texai.skill.deployment.NetworkDeployment.CheckForDeployment;
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class NetworkDeploymentTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkDeploymentTest.class);
  private static final String containerName = "Test";
  private static final String skillClassName = NetworkDeployment.class.getName();
  private static final String nodeName = "NetworkDeploymentAgent";
  private static final String roleName = "NetworkDeploymentRole";
  private static SkillTestHarness skillTestHarness;

  public NetworkDeploymentTest() {
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
   * Test of class NetworkDeployment initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.AHCS_INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    final Message initializeMessage = new Message(
            containerName + ".NetworkOperationAgent", // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);
    skillTestHarness.getSkillState(skillClassName);
    assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    assertEquals("[AHCS initialize_Task, org.texai.skill.deployment.NetworkDeployment]", skillTestHarness.getOperationAndServiceInfo().toString());
  }

  /**
   * Test of class CheckForDeployment.
   */
  @Test
  public void testCheckForDeployment() {
    LOGGER.info("CheckForDeployment");
    final File deploymentLogFile = new File("deployment/deployment.log");
    if (deploymentLogFile.exists()) {
      deploymentLogFile.delete();
    }
    assertFalse(deploymentLogFile.exists());
    skillTestHarness.reset();
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    final CheckForDeployment checkForDeployment = networkDeployment.makeCheckForDeploymentForUnitTest();
    checkForDeployment.run();
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-21T14:30:04.299-06:00\n"
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
            + "]"));
    assertTrue(deploymentLogFile.exists());
    try {
      assertEquals(
              "deployed to Test\n",
              FileUtils.readFileToString(deploymentLogFile));
      sentMessage.serializeToFile("data/test-messages/deployFileTaskMessage.ser");
    } catch (IOException ex) {
      fail();
    }
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkDeployment.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    NetworkDeployment instance = new NetworkDeployment();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

  /**
   * Test of getLogger method, of class NetworkDeployment.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    NetworkDeployment instance = new NetworkDeployment();
    assertNotNull(instance.getLogger());
  }

}
