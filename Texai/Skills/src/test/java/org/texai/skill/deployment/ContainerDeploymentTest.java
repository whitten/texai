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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
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
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class ContainerDeploymentTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerDeploymentTest.class);
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkDeploymentAgent.NetworkDeploymentRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
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
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            containerName + ".NetworkDeploymentAgent", // senderQualifiedName
            NetworkDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final ContainerDeployment containerDeployment = (ContainerDeployment) skillTestHarness.getSkill(skillClassName);
    if (containerDeployment.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndServiceInfo());
  }

  /**
   * Test of class ContainerDeployment deploy files task message.
   */
  @Test
  public void testDeployFilesTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.DEPLOY_FILES_TASK + " message");

    Logger.getLogger(X509Utils.class).setLevel(Level.WARN);
    FileUtils.deleteQuietly(new File("Main-1.0"));
    FileUtils.deleteQuietly(new File("bin"));
    (new File("Main-1.0/data")).mkdirs();
    (new File("Main-1.0/lib")).mkdirs();
    (new File("bin")).mkdir();
    skillTestHarness.reset();
    skillTestHarness.setSkillState(
            AHCSConstants.State.READY, // state
            skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    final Message deplopyFilesTaskMessage = Message.deserializeMessage("data/test-messages/deployFileTaskMessage.ser");
    LOGGER.info(deplopyFilesTaskMessage.toString());
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-22T15:17:58.185-06:00\n"
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
            + "]",
            deplopyFilesTaskMessage.toString()));
    skillTestHarness.dispatchMessage(deplopyFilesTaskMessage);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    final File mainDirectory = new File("Main-1.0");
    assert mainDirectory.exists();
    assert mainDirectory.isDirectory();
    final File dataDirectory = new File("Main-1.0/data");
    assert dataDirectory.exists();
    assert dataDirectory.isDirectory();
    assertEquals("[nodes.xml]", getSortedFileNamesInDirectory(dataDirectory));
    final File libDirectory = new File("Main-1.0/lib");
    assert libDirectory.exists();
    assert libDirectory.isDirectory();
    assertEquals("[AlbusHCN-1.0.jar, AlbusHCNSupport-1.0.jar, BitTorrentSupport-1.0.jar, CoinSkills-1.0.jar, Inference-1.0.jar, JavaBitcoindRpcClient-0.9.0.jar, Main-1.0.jar, Network-1.0.jar, RDFEntityManager-1.0.jar, Skills-1.0.jar, TamperEvidentLog-1.0.jar, UPNPLib-1.0.jar, Utilities-1.0.jar, X509Security-1.0.jar, json-simple-1.1.1.jar]", getSortedFileNamesInDirectory(libDirectory));
    final File binDirectory = new File("bin");
    assert binDirectory.exists();
    assert binDirectory.isDirectory();
    assertEquals("[aicoin-cli, aicoin-qt]", getSortedFileNamesInDirectory(binDirectory));
    FileUtils.deleteQuietly(new File("Main-1.0"));
    FileUtils.deleteQuietly(new File("bin"));

    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertTrue(Message.areMessageStringsEqualIgnoringDate(
            sentMessage.toString(),
            "[taskAccomplished_Info Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment --> Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment 2015-01-22T15:27:28.990-06:00]"));
  }

  /** Returns a string representation of the sorted file names contained in the given directory.
   *
   * @param directory the given directory
   * @return the sorted file names
   */
  private String getSortedFileNamesInDirectory(final File directory) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";

    final List<String> fileNames = new ArrayList<>();
    for (final File file : directory.listFiles()) {
      fileNames.add(file.getName());
    }
    Collections.sort(fileNames);
    return fileNames.toString();
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
    assertTrue(sentMessage.toString().startsWith(""));
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
