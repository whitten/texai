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
            "[deployFile_Task Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment --> Test.ContainerDeploymentAgent.ContainerDeploymentRole:ContainerDeployment 2015-01-22T12:01:37.255-06:00\n"
            + "  deployFile_Task_manifest={\"manifest\":[\n"
            + "{\"path\":\"Main-1.0\\/data\\/nodes.xml\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o\\/\\/\\/wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/AlbusHCN-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"Cts103fDFjpJfa3V3GU4gv0EBMpqqJS2l2kkSJvQWekdUAsY4w8\\/fLDjCORxYlcvCnUPBHubTgLzk2P3hvM\\/2w==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/AlbusHCNSupport-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"D3kAyYCmdeJeCN7EdoxZ+ol2cqyeYVV1sGEzxt2rDHLmtIcxAo6Go+5KkbGg3YLkQV6gPJJbpLvP81EvFgBsKA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/BitTorrentSupport-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"okB8CutCa0Pu8oLQ1ruKznEumdn5dxFEEIgwuyzOqX\\/dtxDKKmA2e\\/MiHA96g3X8UsNo2uz9Ki+NerblOJbfhQ==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/CoinSkills-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"wsx8emhdPs5Uh0b7mbl21zs2tpiUSgb\\/xmsWMzgCivWxA0x2cruYtM0mcUHMASZfP6J1WMwDrVVap7FOuuHsdA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Inference-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"ocIbSCHc78TPCUSEJjABICVTO7TZOKrhut8pMfzgTJbsqy\\/4oSDfSkBHzC78aOeo+Neq+SWssI4c1NJaX9I4gg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/JavaBitcoindRpcClient-0.9.0.jar\",\n"
            + "  \"command\":\"add\",\n"
            + "  \"hash\":\"VmlCyM3MdelJ1\\/gmx+L5DVQ5xtFr7UmsZmyU3dxFMXOAaPdV7LZ\\/itEbTfV4Aqcz+JCojATIXTDwXtxJr5grOw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Main-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"EJ8r4wU2K1p5bytERCzFSqARgLAeeqe8t\\/ITBNoGwNkS1QSCZIMUkH3Gk4KG7YRgKi2i\\/FCBjITxk3WqC\\/Iplg==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Network-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"fXPARlQATJ4CMQhF6UY6WjAdr\\/AW1ygQ1EZT3pcer8NiwCEDWD3CaCKpJrjOOj0pw6hQ7a1zDbFT2nzZ0BHGBw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/RDFEntityManager-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"cPtSjyKw0WXcx+Nr7+mP+H5cPCF4SHDDSVkxuUPJSXsB21QO5YXKnfc7v1sdEA2\\/B\\/JcSB+McsTLVb5ewsN3zw==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/Skills-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"BDPQqC39mCJ661fs8yJEXgKG0nMsZDLG0hYauS5SzjgGpV\\/EjkKtGAoVkIuq2NEycIUFoIMekk2Y7+5A3FzqHg==\"},\n"
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
            + "  \"hash\":\"ikfmtVvTZXy6bjUG7nWkwMO+S9XnbttbqJ4mTdKbYcxFD4LmUnBTMJ3NaVyy\\/HKPHCVDUD2yzyAan5Sbzs33tA==\"},\n"
            + "  \n"
            + "{\"path\":\"Main-1.0\\/lib\\/X509Security-1.0.jar\",\n"
            + "  \"command\":\"replace\",\n"
            + "  \"hash\":\"QnfrQAx4bNCPOvhWgpIhXJJgPFku0s3zioDhl\\/uNhOa47hQXJ+mreeiQy7QCurgM6DX8cwQP3kM\\/fRbOtxXQXQ==\"},\n"
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
            + "  deployFile_Task_zippedBytes=byte[](length=69601292)\n"
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
