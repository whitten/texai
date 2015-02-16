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

import java.nio.charset.Charset;
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
public class ContainerFileReceiverTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerFileReceiverTest.class);
  // the container name
  private static final String containerName = "TestRecipient";
  // the test parent qualified name
  private static final String parentQualifiedName = "Test.NetworkFileTransferAgent.NetworkFileTransferRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = ContainerFileReceiver.class.getName();
  // the test node name
  private static final String nodeName = "ContainerOperationAgent";
  // the test role name
  private static final String roleName = "ContainerFileRecipientRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public ContainerFileReceiverTest() {
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
   * Test of class ContainerFileReceiver initialize task message.
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

    final ContainerFileReceiver skillTemplate = (ContainerFileReceiver) skillTestHarness.getSkill(skillClassName);
    if (skillTemplate.getNodeRuntime().isFirstContainerInNetwork()) {
      assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    } else {
      assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    }
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
  }

  /**
   * Test of class ContainerFileReceiver prepare to receive file task message.
   */
  @Test
  public void testPrepareToReceiveFileTaskMessage() {
    LOGGER.info("testing " + AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);

    final UUID conversationId = UUID.randomUUID();
    final Message prepareToReceiveFileTaskMessage = new Message(
            "Test.NetworkFileTransferAgent.NetworkFileTransferRole", // senderQualifiedName
            NetworkFileTransfer.class.getName(), // senderService
            "TestRecipient.ContainerOperationAgent.ContainerFileRecipientRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileReceiver.class.getName(), // recipientService
            AHCSConstants.PREPARE_TO_RECEIVE_FILE_TASK); // operation
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_FILE_PATH, "deployment/nodes.xml");
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_FILE_PATH, "data/nodes.xml");
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_SENDER_CONTAINER_NAME, "TestSender");
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_RECIPIENT_CONTAINER_NAME, "TestRecipient");
    prepareToReceiveFileTaskMessage.put(
            AHCSConstants.MSG_PARM_FILE_HASH,
            "fukO5UNFNxNm61Lc13blxrDnipjbNHh+1o///wsAQvpB+2nQWLa7PI41gUFDQMzbQuFJ4Mu3QSiQRkSvghIsMA==");
    prepareToReceiveFileTaskMessage.put(AHCSConstants.MSG_PARM_FILE_SIZE, 29795L);

    skillTestHarness.dispatchMessage(prepareToReceiveFileTaskMessage);

    final ContainerFileReceiver containerFileReceiver = (ContainerFileReceiver) skillTestHarness.getSkill(skillClassName);
    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[taskAccomplished_Info, TestRecipient.ContainerOperationAgent.ContainerFileRecipientRole:ContainerFileReceiver --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkFileTransfer]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            containerFileReceiver.getFileTransferInfo(conversationId).toBriefString());

    // test a received Transfer File Chunk Info message
    skillTestHarness.reset();
    containerFileReceiver.setIsFileTransferDictionaryCleaned(false);
    final Message transferFileChunkInfoMessage = new Message(
            "TestSender.ContainerOperationAgent.ContainerFileSenderRole", // senderQualifiedName
            ContainerFileSender.class.getName(), // senderService
            "TestRecipient.ContainerOperationAgent.ContainerFileRecipientRole", // recipientQualifiedName
            conversationId,
            null, // replyWith
            null, // inReplyTo
            ContainerFileReceiver.class.getName(), // recipientService
            AHCSConstants.TRANSFER_FILE_CHUNK_INFO); // operation
    final byte[] truncatedBuffer = "<tag>a test file chunk</tag>".getBytes(Charset.forName("UTF-8"));
    transferFileChunkInfoMessage.put(AHCSConstants.MSG_PARM_BYTES, truncatedBuffer);
    transferFileChunkInfoMessage.put(AHCSConstants.MSG_PARM_BYTES_SIZE, truncatedBuffer.length);

    skillTestHarness.dispatchMessage(transferFileChunkInfoMessage);

    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals("[taskAccomplished_Info, TestRecipient.ContainerOperationAgent.ContainerFileRecipientRole:ContainerFileReceiver --> TestSender.ContainerOperationAgent.ContainerFileSenderRole:ContainerFileSender]",
            sentMessage.toBriefString());
    assertNotNull(sentMessage.getConversationId());
    assertEquals(sentMessage.getConversationId().toString(), conversationId.toString());
    FileTransferInfo fileTransferInfo = containerFileReceiver.getFileTransferInfo(conversationId);
    assertEquals(
            "[TestSender:deployment/nodes.xml --> TestRecipient:data/nodes.xml]",
            fileTransferInfo.toBriefString());
    assertEquals(1, sentMessage.get(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT));
    assertEquals(fileTransferInfo.getFileChunksCnt(), sentMessage.get(AHCSConstants.MSG_PARM_FILE_CHUNKS_CNT));
  }

  /**
   * Test of class ContainerFileReceiver - Message Not Understood Info.
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
    assertEquals(
            "[messageNotUnderstood_Info, TestRecipient.ContainerOperationAgent.ContainerFileRecipientRole:ContainerFileReceiver --> Test.NetworkFileTransferAgent.NetworkFileTransferRole:NetworkOperation]",
            sentMessage.toBriefString());
  }

  /**
   * Test of getLogger method, of class ContainerFileReceiver.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    ContainerFileReceiver instance = new ContainerFileReceiver();
    assertNotNull(instance.getLogger());
    assertEquals(ContainerFileReceiver.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class ContainerFileReceiver.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    ContainerFileReceiver instance = new ContainerFileReceiver();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals(
            "[initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task, prepareToReceiveFile_Task, transferFileChunk_Info]",
            understoodOperations.toString());
  }

}
