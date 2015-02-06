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
import org.texai.skill.network.NetworkOperation;
import org.texai.skill.testHarness.SkillTestHarness;
import org.texai.util.ArraySet;

/**
 *
 * @author reed
 */
public class ContainerFileSenderTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerFileSenderTest.class);
  // the container name
  private static final String containerName = "TestSender";
  // the test parent qualified name
  private static final String parentQualifiedName = "Test.NetworkFileTransferAgent.NetworkFileTransferRole";
  // the test parent service
  private static final String parentService = NetworkFileTransfer.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = ContainerFileSender.class.getName();
  // the test node name
  private static final String nodeName = "ContainerFileTransferAgent";
  // the test role name
  private static final String roleName = "ContainerFileSenderRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public ContainerFileSenderTest() {
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
   * Test of class ContainerFileSender initialize task message.
   */
  @Test
  public void testInitializeTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.INITIALIZE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.UNINITIALIZED, skillClassName);
    final Message initializeMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            NetworkOperation.class.getName(), // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.INITIALIZE_TASK); // operation

    skillTestHarness.dispatchMessage(initializeMessage);

    final ContainerFileSender skillTemplate = (ContainerFileSender) skillTestHarness.getSkill(skillClassName);
    if (skillTemplate.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndServiceInfo());
  }

  /**
   * Test of class ContainerFileSender.
   */
  @Test
  public void testFileTransferConversation() {
    LOGGER.info("testing file transfer conversation");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);

    final UUID conversationId = UUID.randomUUID();
    final Message prepareToSendFileTaskMessage = new Message(
            "Test.NetworkFileTransferAgent.NetworkFileTransferRole", // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_SEND_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, "deployment/nodes.xml");
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, "data/nodes.xml");
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, "TestRecipient");

    skillTestHarness.dispatchMessage(prepareToSendFileTaskMessage);

    final ContainerFileSender containerFileSender = (ContainerFileSender) skillTestHarness.getSkill(skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[taskAccomplished_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    assertEquals(
            sentMessage.get(AHCSConstants.MSG_PARM_FILE_HASH).toString(),
            "fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o///wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==");
    assertEquals(
            (long) sentMessage.get(AHCSConstants.MSG_PARM_FILE_SIZE),
            29795);
    LOGGER.info("FileTransferRequestInfo ...\n" + containerFileSender.getFileTransferRequestInfo(conversationId).toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            containerFileSender.getFileTransferRequestInfo(conversationId).toBriefString());

    // test the received File Transfer Task message
    skillTestHarness.reset();
    final Message transferFileTaskMessage = new Message(
            "Test.NetworkFileTransferAgent.NetworkFileTransferRole", // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.TRANSFER_FILE_TASK); // operation

    skillTestHarness.dispatchMessage(transferFileTaskMessage);

    int fileChunkCnt = 1;
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[transferFileChunk_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole:ContainerFileReceiver]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    final FileTransferInfo fileTransferInfo = containerFileSender.getFileTransferRequestInfo(conversationId);
    LOGGER.info("FileTransferRequestInfo ...\n" + fileTransferInfo.toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            fileTransferInfo.toBriefString());
    assertEquals(
            (long) sentMessage.get(AHCSConstants.MSG_PARM_BYTES_SIZE),
            FileTransferInfo.MAXIMUM_FILE_CHUNK_SIZE);
    final byte[] bytes = (byte[]) sentMessage.get(AHCSConstants.MSG_PARM_BYTES);
    assertNotNull(bytes);
    assertEquals(FileTransferInfo.MAXIMUM_FILE_CHUNK_SIZE, bytes.length);
    assertEquals(fileChunkCnt, fileTransferInfo.getFileChunksCnt());

    // test the received Task Accomplished Info message
    skillTestHarness.reset();
    Message taskAccomplishedInfoMessage = new Message(
            "TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole", // senderQualifiedName
            ContainerFileReceiver.class.getName(), // senderService
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT, fileChunkCnt);

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[transferFileChunk_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole:ContainerFileReceiver]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    LOGGER.info("FileTransferRequestInfo ...\n" + fileTransferInfo.toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            fileTransferInfo.toBriefString());

    // repeat until all file chunks have been processed
    while (true) {
      skillTestHarness.reset();
      taskAccomplishedInfoMessage = new Message(
              "TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole", // senderQualifiedName
              ContainerFileReceiver.class.getName(), // senderService
              "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
              conversationId,
              null, // replyWith
              null, // inReplyTo
              ContainerFileSender.class.getName(), // recipientService
              AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
      taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT, ++fileChunkCnt);

      skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

      final byte[] fileChunkBytes = fileTransferInfo.getFileChunkBytes();
      if (fileChunkBytes.length < FileTransferInfo.MAXIMUM_FILE_CHUNK_SIZE) {
        break;
      } else if (fileChunkCnt > 100) {
        fail();
      }
    }

    skillTestHarness.reset();
    containerFileSender.setIsFileTransferDictionaryCleaned(false);
    taskAccomplishedInfoMessage = new Message(
            "TestRecipient.ContainerFileTransferAgent.ContainerFileRecipientRole", // senderQualifiedName
            ContainerFileReceiver.class.getName(), // senderService
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.TASK_ACCOMPLISHED_INFO); // operation
    taskAccomplishedInfoMessage.put(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT, ++fileChunkCnt);

    skillTestHarness.dispatchMessage(taskAccomplishedInfoMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[taskAccomplished_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    LOGGER.info("FileTransferRequestInfo ...\n" + fileTransferInfo.toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            fileTransferInfo.toBriefString());
  }

  /**
   * Test of class ContainerFileSender prepare to send file task message.
   */
  @Test
  public void testPrepareToSendFileTaskMessage2() {
    LOGGER.info("testing " + AHCSConstants.PREPARE_TO_SEND_FILE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);

    final UUID conversationId = UUID.randomUUID();
    final Message prepareToSendFileTaskMessage = new Message(
            "Test.NetworkFileTransferAgent.NetworkFileTransferRole", // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            "TestSender.ContainerFileTransferAgent.ContainerFileSenderRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileSender.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_SEND_FILE_TASK); // operation
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, "deployment/does-not-exist.txt");
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, "data/nodes.xml");
    prepareToSendFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, "TestRecipient");

    skillTestHarness.dispatchMessage(prepareToSendFileTaskMessage);

    final ContainerFileSender containerFileSender = (ContainerFileSender) skillTestHarness.getSkill(skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[exception_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    assertNull(containerFileSender.getFileTransferRequestInfo(conversationId));
    final Message originalMessage = (Message) sentMessage.get(AHCSConstants.AHCS_ORIGINAL_MESSAGE);
    assertNotNull(originalMessage);
    assertEquals(originalMessage.toBriefString(), prepareToSendFileTaskMessage.toBriefString());
    assertEquals(sentMessage.get(AHCSConstants.MSG_PARM_REASON),
            "file does not exist: deployment/does-not-exist.txt");
  }

  /**
   * Test of class ContainerFileSender - Message Not Understood Info.
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
    assertEquals("[messageNotUnderstood_Info, TestSender.ContainerFileTransferAgent.ContainerFileSenderRole:ContainerFileSender --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer]",
            sentMessage.toBriefString());
  }

  /**
   * Test of getLogger method, of class ContainerFileSender.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    ContainerFileSender instance = new ContainerFileSender();
    assertNotNull(instance.getLogger());
    assertEquals(ContainerFileSender.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class ContainerFileSender.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerFileSender instance = new ContainerFileSender();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[AHCS initialize_Task, messageNotUnderstood_Info, performMission_Task, prepareToSendFile_Task, taskAccomplished_Info, transferFile_Task]", understoodOperations.toString());
  }

}
