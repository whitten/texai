/*
 * Copyright (C) 2015 Texai
 *
 */
package org.texai.skill.aicoin;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.InventoryItem;
import com.google.bitcoin.core.InventoryMessage;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.ProtocolException;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.params.TestNet3Params;
import static com.google.bitcoin.utils.TestUtils.roundTripTransaction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
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
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
public class AICMintTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICMintTest.class);
  // the container name
  private static final String containerName = "Test";
  // the test parent qualified name
  private static final String parentQualifiedName = containerName + ".AICNetworkOperationAgent.AICNetworkOperationRole";
  // the test parent service
  private static final String parentService = NetworkOperation.class.getName();
  // the class name of the tested skill
  private static final String skillClassName = AICMint.class.getName();
  // the test node name
  private static final String nodeName = "AICMintAgent";
  // the test role name
  private static final String roleName = "AICMintRole";
  // the skill test harness
  private static SkillTestHarness skillTestHarness;

  public AICMintTest() {
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
   * bitcoinj InventoryMessage serialization test.
   */
  @Test
  public void testInventoryMessageSerialization() {
    LOGGER.info("testing InventoryMessage serialization");

    Transaction transaction = null;
    try {
      transaction = createFakeTx(
              new TestNet3Params(), // networkParameters
              BigInteger.valueOf(50000), // nanocoins
              new ECKey()); // recipientAddress
    } catch (IOException ex) {
      fail();
    }
    LOGGER.info("transaction: " + transaction);

    ObjectOutputStream objectOutputStream = null;
    try {

      // serialize transaction
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(transaction);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize transaction
      final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
      final InputStream inputStream = new ByteArrayInputStream(serializedBytes);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object result = objectInputStream.readObject();
      assert result instanceof Transaction;
      assertEquals(result, transaction);

    } catch (IOException | ClassNotFoundException ex) {
      fail(ex.getMessage());
    } finally {
      try {
        if (objectOutputStream != null) {
          objectOutputStream.close();
        }
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }

    final InventoryMessage inventoryMessage = InventoryMessage.with(transaction);
    LOGGER.info("inventoryMessage: " + inventoryMessage);

    final List<InventoryItem> inventoryItems = inventoryMessage.getItems();
    assertEquals(1, inventoryItems.size());
    final InventoryItem inventoryItem = inventoryItems.get(0);
    LOGGER.info("inventoryItem: " + inventoryItem);

    objectOutputStream = null;
    try {

      // serialize inventoryItem
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(inventoryItem);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize inventoryItem
      final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
      final InputStream inputStream = new ByteArrayInputStream(serializedBytes);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object result = objectInputStream.readObject();
      assert result instanceof InventoryItem;
      assertEquals(result, inventoryItem);

    } catch (IOException | ClassNotFoundException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    } finally {
      try {
        if (objectOutputStream != null) {
          objectOutputStream.close();
        }
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }

    objectOutputStream = null;
    try {

      // serialize inventoryMessage
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(inventoryMessage);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize inventoryMessage
      final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
      final InputStream inputStream = new ByteArrayInputStream(serializedBytes);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object result = objectInputStream.readObject();
      assert result instanceof InventoryMessage;
      assertEquals(result, inventoryMessage);

    } catch (IOException | ClassNotFoundException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    } finally {
      try {
        if (objectOutputStream != null) {
          objectOutputStream.close();
        }
      } catch (IOException ex) {
        fail(ex.getMessage());
      }
    }
  }

  /**
   * Creates a test bitcoin transaction of sufficient realism to exercise the unit tests. Two outputs, one to us, one to somewhere else to
   * simulate change.
   *
   * @param networkParameters the network parameters
   * @param nanocoins the transaction input amount
   * @param recipientAddress the elliptic curve key recipent address
   *
   * @return the transaction
   * @throws java.io.IOException
   */
  public static Transaction createFakeTx(
          final NetworkParameters networkParameters,
          final BigInteger nanocoins,
          final ECKey recipientAddress) throws IOException, ProtocolException {
    //Preconditions
    assert networkParameters != null : "networkParameters must not be null";
    assert nanocoins != null : "nanocoins must not be null";
    assert recipientAddress != null : "recipientAddress must not be null";

    Transaction transaction = new Transaction(networkParameters);
    TransactionOutput outputToMe = new TransactionOutput(networkParameters, transaction, nanocoins, recipientAddress);
    transaction.addOutput(outputToMe);
    TransactionOutput change = new TransactionOutput(networkParameters, transaction, Utils.toNanoCoins(1, 11), new ECKey());
    transaction.addOutput(change);
    // Make a previous tx simply to send us sufficient coins. This prev tx is not really valid but it doesn't
    // matter for our purposes.
    Transaction prevTx = new Transaction(networkParameters);
    TransactionOutput prevOut = new TransactionOutput(networkParameters, prevTx, nanocoins, recipientAddress);
    prevTx.addOutput(prevOut);
    // Connect it.
    transaction.addInput(prevOut);
    // Serialize/deserialize to ensure internal state is stripped, as if it had been read from the wire.
    return roundTripTransaction(networkParameters, transaction);
  }

  /**
   * Test of class AICMint initialize task message.
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

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
  }

  /**
   * Test of class AICMint - Message Not Understood Info.
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
    assertEquals("[messageNotUnderstood_Info, Test.AICMintAgent.AICMintRole:AICMint --> Test.AICNetworkOperationAgent.AICNetworkOperationRole:NetworkOperation]",
            sentMessage.toBriefString());
  }

  /**
   * Test of class AICMint - Join Acknowledged Task.
   */
  @Test
  public void testJoinAcknowledgedTask() {
    LOGGER.info("testing " + AHCSConstants.JOIN_ACKNOWLEDGED_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK, skillClassName);
    final Message joinAcknowledgedTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.JOIN_ACKNOWLEDGED_TASK); // operation

    skillTestHarness.dispatchMessage(joinAcknowledgedTaskMessage);

    assertEquals("ISOLATED_FROM_NETWORK", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNotNull(sentMessage);
    LOGGER.info("sentMessage...\n" + sentMessage);
    assertEquals(
            "[removeUnjoinedRole_Info, Test.AICMintAgent.AICMintRole:AICMint --> Test.ContainerOperationAgent.ContainerSingletonConfigurationRole:ContainerSingletonConfiguration]",
            sentMessage.toBriefString());
  }

  /**
   * Test of class AICMint - Generate Coin Block Task.
   */
  @Test
  public void testGenerateCoinBlockTask() {
    LOGGER.info("testing " + AHCSConstants.GENERATE_COIN_BLOCK_TASK + " message");

    skillTestHarness.reset();
    skillTestHarness.setSkillState(AHCSConstants.State.READY, skillClassName);
    final Message joinAcknowledgedTaskMessage = new Message(
            parentQualifiedName, // senderQualifiedName
            parentService, // senderService
            containerName + "." + nodeName + "." + roleName, // recipientQualifiedName
            skillClassName, // recipientService
            AHCSConstants.GENERATE_COIN_BLOCK_TASK); // operation

    skillTestHarness.dispatchMessage(joinAcknowledgedTaskMessage);

    assertEquals("READY", skillTestHarness.getSkillState(skillClassName).toString());
    assertNull(skillTestHarness.getOperationAndSenderServiceInfo());
    final Message sentMessage = skillTestHarness.getSentMessage();
    assertNull(sentMessage);
  }

  /**
   * Test of getLogger method, of class AICMint.
   */
  @Test
  public void testGetLogger() {
    LOGGER.info("getLogger");
    AICMint instance = new AICMint();
    assertNotNull(instance.getLogger());
    assertEquals(AICMint.class.getName(), instance.getLogger().getName());
  }

  /**
   * Test of getUnderstoodOperations method, of class AICMint.
   */
  @Test
  public void testGetUnderstoodOperations() {
    LOGGER.info("getUnderstoodOperations");
    AICMint instance = new AICMint();
    final List<String> understoodOperations = new ArrayList<>(Arrays.asList(instance.getUnderstoodOperations()));
    Collections.sort(understoodOperations);
    assertEquals("[delegatePerformMission_Task, generateCoinBlock_Task, initialize_Task, joinAcknowledged_Task, messageNotUnderstood_Info, performMission_Task]", understoodOperations.toString());
  }

}
