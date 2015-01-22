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
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            containerName + ".NetworkOperationAgent", // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);
    skillTestHarness.getSkillState(skillClassName);

    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    if (networkDeployment.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndServiceInfo());
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
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.undeployedContainerNames.clear();

    final CheckForDeployment checkForDeployment = networkDeployment.makeCheckForDeploymentForUnitTest();

    checkForDeployment.run();

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[Test]", networkDeployment.undeployedContainerNames.toString());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-22T14:55:07.919-06:00\n"
            + "  deployFile_Task_manifest={\"manifest\":[\n"
            + "{\"path\":\"Main-1.0\\/data\\/nodes.xml\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o\\/\\/\\/wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/AlbusHCN-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"S3AQfCcfxHaNOq0Jtkp5LrS3s9inNd8EDfkrjPNwjTm149iGBxUI+14FzWtYez5NYh4W6oWFQn\\/LEFDWyEImRQ==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/AlbusHCNSupport-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"ulT5yhYY1QmyY\\/pwURZjC28ntzy5oWOMLRh7C5BFWZRVAGaH918YooZvhdR1xMnv35Vm8WbztQc5PZ7krps5yw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/BitTorrentSupport-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"a0Sli0FLeFdmrFB3AkUFIme0zr7an\\/OEQ\\/AMK6lszP0dZ\\/4G99JL\\/mDtcIIQ5uIAK3SIjannykEmpu6Oxb6UFA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/CoinSkills-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"Y4ieVHP7E6oqPjg51wNTFUqSeE5AH\\/LAy2LhnpbG7MlY3pRcbN5JGrjcGumblcRQ3GuACangS24pN0ftaMMDIg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Inference-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"rYsFthEVgNkhIG6PB7lbsCCDLO0586XhmkNcseizYUi8PehlgalwQDqHlBXFjgnoNEke7D2t6NoyOaTDYzeZHg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/JavaBitcoindRpcClient-0.9.0.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"VmlCyM3MdelJ1\\/gmx+L5DVQ5xtFr7UmsZmyU3dxFMXOAaPdV7LZ\\/itEbTfV4Aqcz+JCojATIXTDwXtxJr5grOw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Main-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"wdPbfwE62\\/hlTuhJacjQh43S2pLCnMhzolygYJTI1fUlyF8IFgFuGYhoILNFW3Mxu88v+YArrCMZ6AF995td0g==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Network-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"1oHJOLqPZPxcm8Oq4vQME4slFcdAYhBhU3c7dgy+6Ho439slVFOGrN39M1cP6wnzANmW4x7GHdhLy5X7DAxa0w==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/RDFEntityManager-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"FQSFPCF8jMFO2Wvhs7plRV\\/cuHQIdZpwbSM4ir9MqwqMdHwPuL29j31FlMh5URWhA1VdlR83WL0MSQmWlyp+xg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Skills-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"w4xainXmaZgXX1neN9KSUk3uU94r8\\/6eShEbhLUzY0tdG6CvlZP5jJUZHZ7GGKV5y\\/YXxy145W7jItzF6P0p2g==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/TamperEvidentLog-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"Xf2hZN+vwWhYShIKX75IUO5iGDXi177n9tNkeyLmBTiMKp8LKBwlfzkzWaohKnwUBKhevjmzsQSX0ZT4DeeJKw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/UPNPLib-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"otrR8RCJrBlu9gIxDl+B7TMuUfpgtkdNKNLQkDiUOA7zDxhw\\/NhHD8F5hpg8IJJm2hu1Pcpsn1fRR5ijWuapNQ==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Utilities-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"G6hqLWM7QsRSOGU9zY53Bx1RR6s8wjhZ\\/qq0jB5H0L7ysQWHzNtcW7EukG0Xps48Pa4JxfMNtI2EjEtRMLu2Tg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/X509Security-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"smask70ELe9vHay0Xk6q1P4RAMHW5eBnFjkGcr4VkaJNpHnoAQwTIzKyxKWtqPZ\\/H6WN3lAdnUAq0awYMzXacw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/json-simple-1.1.1.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"+HmL+8yKuAAbr5DOR+wiZCNNwdotSql\\/3NwJkEcqa1paMvgo53YUB3fVmKmdigwPUcbQdnrhqClpCrkgCuNXQg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/json-simple-1.1.jar\",\n"
            + "  \"command\":\"delete\"},\n"
            + "  \n"
            + "{\"path\":\"bin\\/aicoin-cli\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"tj0GSSJxBp8pxvzldlxRrd116pmk62I9Gy42Kn452JTpmpXnqV\\/AZ5j2N18oPqZ+3AiRr3CHZwg27pF8TUvBoQ==\"},\n"
            + "  \n"
            + "{\"path\":\"bin\\/aicoin-qt\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"eVoMC+v8\\/GpQkkCOrRM7si4ucqVVgzMbAhPPxJyOXW4830nSN9dJ0y4HK0nvESBn3ynlsFumnbIe3hMnN2Z\\/EQ==\"}]}\n"
            + ",\n"
            + "  deployFile_Task_zippedBytes=byte[](length=39956604)\n"
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
   * Test of class NetworkDeployment task accomplished task message.
   */
  @Test
  public void testTaskAccomplishedInfo() {
    LOGGER.info("testing " + AHCSConstants.TASK_ACCOMPLISHED_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final NetworkDeployment networkDeployment = (NetworkDeployment) skillTestHarness.getSkill(skillClassName);
    networkDeployment.undeployedContainerNames.add("Test");
    final Message taskAccomplishedInfoMessage = new Message(
            containerName + ".NetworkOperationAgent", // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    assertTrue(networkDeployment.undeployedContainerNames.isEmpty());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[networkRestartRequest_Info Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation 2015-01-22T15:16:22.534-06:00]"));
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
