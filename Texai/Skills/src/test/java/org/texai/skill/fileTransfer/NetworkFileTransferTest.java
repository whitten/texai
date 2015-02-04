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
package org.texai.skill.fileTransfer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class NetworkFileTransferTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NetworkFileTransfer.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".NetworkOperationAgent.NetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the test child qualified name
  private static final String childQualifiedName = containerName + ".FileTransferAgent.FileTransferRole";
  // the class name of the tested skill
  private static final String skillClassName = NetworkFileTransfer.class.getName();
  // the test node name
  private static final String nodeName = "NetworkFileTransferAgent";
  // the test node name
  private static final String roleName = "NetworkFileTransferRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public NetworkFileTransferTest() {
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
    LOGGER.info("testing " + AHCSConstants.AHCS_INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.AHCS_INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final NetworkFileTransfer networkSingletonSkillTemplate = (NetworkFileTransfer) skillTestHarness.getSkill(skillClassName);
    if (networkSingletonSkillTemplate.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNotNull(skillTestHarness.getOperationAndServiceInfo());
    assertEquals("[AHCS initialize_Task, org.texai.skill.fileTransfer.NetworkFileTransfer]", skillTestHarness.getOperationAndServiceInfo().toString());
  }

  /**
   * Test of class NetworkFileTransfer - Transfer File Request Info.
   */
  @Test
  public void testTransferFileRequestInfo() {
    LOGGER.info("testing " + AHCSConstants.TRANSFER_FILE_REQUEST_INFO + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final UUID conversationId = UUID.randomUUID();
    final Message transferFileRequestInfoMessage = new Message(
            "Test.NetworkDeploymentAgent.NetworkDeploymentRole", // senderQualifiedName
            NetworkDeployment.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            NetworkFileTransfer.class.getName(), // recipientService
            AHCSConstants.TRANSFER_FILE_REQUEST_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version
    transferFileRequestInfoMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, "deployment/nodes.xml");
    transferFileRequestInfoMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, "data/nodes.xml");
    transferFileRequestInfoMessage.put(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME, "TestSender");
    transferFileRequestInfoMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, "TestRecipient");

    skillTestHarness.dispatchMessage(transferFileRequestInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[prepareToSendFile_Task, Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer --> TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    final NetworkFileTransfer networkFileTransfer = (NetworkFileTransfer) skillTestHarness.getSkill(NetworkFileTransfer.class.getName());
    LOGGER.info("FileTransferRequestInfo ...\n" + networkFileTransfer.getFileTransferInfo(conversationId).toString());
    final FileTransferInfo fileTransferInfo = networkFileTransfer.getFileTransferInfo(conversationId);
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            fileTransferInfo.toBriefString());
    assertEquals("uninitialized", FileTransferInfo.fileTransferStateToString(fileTransferInfo.getFileTransferState()));

    // Task Accomplished Info response from the TestSender.ContainerFileTransferAgent.ContainerFileSenderRole
    skillTestHarness.reset();
    assertNull(skillTestHarness.getSentMessage());
    Message taskAccomplishedInfoMessage = new Message(
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // senderQualifiedName
            ContainerFileSender.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            NetworkFileTransfer.class.getName(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version
    taskAccomplishedInfoMessage.put(
            AHCSConstants.MSG_PARM_FILE_HASH,
            "fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o///wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==");
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_FILE_SIZE, 29795L);

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[prepareToReceiveFile_Task, Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer --> TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole:ContainerFileReceiver]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    LOGGER.info("FileTransferRequestInfo ...\n" + networkFileTransfer.getFileTransferInfo(conversationId).toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            networkFileTransfer.getFileTransferInfo(conversationId).toBriefString());
    assertEquals("OK to send", FileTransferInfo.fileTransferStateToString(fileTransferInfo.getFileTransferState()));

    // Task Accomplished Info response from the TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole
    skillTestHarness.reset();
    assertNull(skillTestHarness.getSentMessage());
    taskAccomplishedInfoMessage = new Message(
            "TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole", // senderQualifiedName
            ContainerFileReceiver.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            NetworkFileTransfer.class.getName(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[transferFile_Task, Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer --> TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    LOGGER.info("FileTransferRequestInfo ...\n" + networkFileTransfer.getFileTransferInfo(conversationId).toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            networkFileTransfer.getFileTransferInfo(conversationId).toBriefString());
    assertEquals("file transfer started", FileTransferInfo.fileTransferStateToString(fileTransferInfo.getFileTransferState()));

    // Task Accomplished Info response from the TestSender.ContainerFileTransferAgent.ContainerFileSenderRole
    skillTestHarness.reset();
    networkFileTransfer.setIsFileTransferDictionaryCleaned(false);
    assertNull(skillTestHarness.getSentMessage());
    taskAccomplishedInfoMessage = new Message(
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // senderQualifiedName
            ContainerFileSender.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            null, // replyByDateTime
            NetworkFileTransfer.class.getName(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO, // operation
            new HashMap<>(), // parameterDictionary
            Message.DEFAULT_VERSION); // version
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_DURATION, 10000L);

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[taskAccomplished_Info, Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer --> Test.NetworkDeploymentAgent.NetworkDeploymentRole:NetworkDeployment]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    LOGGER.info("FileTransferRequestInfo ...\n" + networkFileTransfer.getFileTransferInfo(conversationId).toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            networkFileTransfer.getFileTransferInfo(conversationId).toBriefString());
    assertEquals(
            FileTransferInfo.fileTransferStateToString(fileTransferInfo.getFileTransferState()),
            "file transfer complete");
  }

  /**
   * Test of class NetworkFileTransfer - Message Not Understood Info.
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
    assertEquals("[messageNotUnderstood_Info, Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer --> Test.NetworkOperationAgent.NetworkOperationRole:NetworkOperation]",
            sentMessage.toBriefString());
  }

  /**
   * Test of getLogger method, of class NetworkFileTransfer.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    NetworkFileTransfer instance = new NetworkFileTransfer();
    assertNotNull(instance.getLogger());
    assertEquals(NetworkFileTransfer.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class NetworkFileTransfer.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    NetworkFileTransfer instance = new NetworkFileTransfer();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals(
            "[AHCS initialize_Task, delegateBecomeReady_Task, delegatePerformMission_Task, joinAcknowledged_Task, joinNetworkSingletonAgent_Info, messageNotUnderstood_Info, performMission_Task, taskAccomplished_Info, transferFileRequest_Info]",
            understoodOperations.toString());
  }

}
