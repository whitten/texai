/*
 * MessageTest.java
 *
 * Created on Jun 30, 2008, 2:33:34 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 3, 2010 reed.
 */
package org.texai.ahcsSupport;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;
import org.texai.util.ByteUtils;
import org.texai.x509.X509Utils;
import static org.junit.Assert.*;
import org.texai.util.ArraySet;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "NP_LOAD_OF_KNOWN_NULL_VALUE", justification = "unit test")
public class MessageTest {

  /**
   * the logger
   */
  private static final Logger LOGGER = Logger.getLogger(MessageTest.class);

  public MessageTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  public static URI randomURI() {
    return new URIImpl(Constants.TEXAI_NAMESPACE + UUID.randomUUID().toString());
  }

  /**
   * Test serialization.
   */
  @Test
  public void testSerialization() {
    LOGGER.info("serialization");
    ObjectOutputStream objectOutputStream = null;
    try {
      String senderQualifiedName = "container1.agent1.role1";
      String recipientQualifiedName = "container2.agent2.role2";
      final Message message1 = new Message(
              senderQualifiedName,
              "SenderService", // senderService
              recipientQualifiedName,
              "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
              "ABC_Task"); // operation

      // serialize
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(message1);
      objectOutputStream.close();
      assertTrue(byteArrayOutputStream.toByteArray().length > 0);

      // deserialize
      final byte[] serializedBytes = byteArrayOutputStream.toByteArray();
      final InputStream inputStream = new ByteArrayInputStream(serializedBytes);
      ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
      Object result = objectInputStream.readObject();
      assert result instanceof Message;
      assertEquals(result, message1);

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
  }

  /**
   * Test of serializeToFile and deserializeMessage methods, of class Message.
   */
  @Test
  public void testSerializeToFile() {
    LOGGER.info("serializeToFile and back again");
    String senderQualifiedName = "container1.agent1.role1";
    String recipientQualifiedName = "container2.agent2.role2";
    final Message message = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "ABC_Task"); // operation
    final String serializedMessageFilePath = "data/test-message.ser";
    final File serializedMessageFile = new File(serializedMessageFilePath);
    if (serializedMessageFile.exists()) {
      final boolean isOK = serializedMessageFile.delete();
      assertTrue(isOK);
    }
    assertFalse(serializedMessageFile.exists());

    message.serializeToFile(serializedMessageFilePath);

    assertTrue(serializedMessageFile.exists());
    assertTrue(serializedMessageFile.isFile());
    final Message deserializedMessage = Message.deserializeMessage(serializedMessageFilePath);
    assertEquals(message, deserializedMessage);
  }

  /**
   * Test of toBriefString method, of class Message.
   */
  @Test
  public void testToBriefString2() {
    LOGGER.info("toBriefString");
    String senderQualifiedName = "container1.agent1.role1";
    String recipientQualifiedName = "container2.agent2.role2";
    final Message message1 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "ABC_Task"); // operation
    final Message message2 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "DEF_Task"); // operation
    final String message2String = message2.toString();
    final Message message3 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "XYZ_Task"); // operation
    final Set<Message> messages = new ArraySet<>();
    messages.add(message1);
    messages.add(message2);
    messages.add(message3);
    assertEquals(
            "[ABC_Task, container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription]\n" +
            "[DEF_Task, container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription]\n" +
            "[XYZ_Task, container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription]\n",
            Message.toBriefString(messages));
  }

  /**
   * Test of areMessageStringsEqualIgnoringDate method, of class Message.
   */
  @Test
  public void testAreMessageStringsEqualIgnoringDate() {
    LOGGER.info("areMessageStringsEqualIgnoringDate");
    String senderQualifiedName = "container1.agent1.role1";
    String recipientQualifiedName = "container2.agent2.role2";

    final Message message1 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "ABC_Task"); // operation
    final String message1String = message1.toString();

    final Message message2 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "ABC_Task"); // operation
    final String message2String = message2.toString();

    assertTrue(Message.areMessageStringsEqualIgnoringDate(message2String, message2String));

    final Message message3 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // recipientService
            "XYZ_Task"); // operation
    final String message3String = message3.toString();

    assertFalse(Message.areMessageStringsEqualIgnoringDate(message1String, message3String));

    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    final Map<String, Object> parameterDictionary = new HashMap<>();
    parameterDictionary.put("test-parmeter", "test-value");
    final Message message4 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // service
            "ABC_Task", // operation,
            parameterDictionary,
            "1.0.0");
    final String message4String = message4.toString();

    final Message message5 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // service
            "ABC_Task", // operation,
            parameterDictionary,
            "1.0.0");
    final String message5String = message5.toString();

    assertTrue(Message.areMessageStringsEqualIgnoringDate(message4String, message5String));
    assertFalse(Message.areMessageStringsEqualIgnoringDate(message1String, message5String));

    final Message message6 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            "org.texai.kb.persistence.domainEntity.RepositoryContentDescription", // service
            "XYZ_Task", // operation,
            parameterDictionary,
            "1.0.0");
    final String message6String = message6.toString();
    assertFalse(Message.areMessageStringsEqualIgnoringDate(message4String, message6String));
  }

  /**
   * Test of getOperation method, of class Message.
   */
  @Test
  public void testGetOperation() {
    LOGGER.info("getOperation");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final String operation = "ABC_Task";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            operation,
            parameterDictionary,
            "1.0.0");

    String result = instance.getOperation();
    assertEquals("ABC_Task", result);
  }

  /**
   * Test of getSenderContainerName, getRecipientContainerName methods, of class Message.
   */
  @Test
  public void testGetSenderContainerName() {
    LOGGER.info("getSenderContainerName");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final String operation = "ABC_Task";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            operation,
            parameterDictionary,
            "1.0.0");

    assertEquals("container1", instance.getSenderContainerName());
    assertEquals("container2", instance.getRecipientContainerName());
  }

  /**
   * Test of isBetweenContainers method, of class Message.
   */
  @Test
  public void testIsBetweenContainers() {
    LOGGER.info("isBetweenContainers");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final String operation = "ABC_Task";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            operation,
            parameterDictionary,
            "1.0.0");

    assertTrue(instance.isBetweenContainers());

    final String senderQualifiedName2 = "container2.agent1.role1";
    Message instance2 = new Message(
            senderQualifiedName2,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            operation,
            parameterDictionary,
            "1.0.0");
    assertFalse(instance2.isBetweenContainers());
  }

  /**
   * Test of isInfo method, of class Message.
   */
  @Test
  public void testIsInfo() {
    LOGGER.info("isInfo");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isInfo());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Info", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(true, instance.isInfo());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Sensation", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isInfo());
  }

  /**
   * Test of isSensation method, of class Message.
   */
  @Test
  public void testIsSensation() {
    LOGGER.info("isSensation");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isInfo());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Info", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(true, instance.isInfo());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Sensation", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isInfo());
  }

  /**
   * Test of isTask method, of class Message.
   */
  @Test
  public void testIsTask() {
    LOGGER.info("isTask");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(true, instance.isTask());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Info", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isTask());

    instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Sensation", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(false, instance.isTask());
  }

  /**
   * Test of get method, of class Message.
   */
  @Test
  public void testGet() {
    LOGGER.info("get");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals("10", instance.get(parameterName).toString());
  }

  /**
   * Test of toString method, of class Message.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    parameterName = "bytes";
    final byte[] byteArray = {1, 2, 3, 4, 5, 6, 7, 8};
    parameterValue = byteArray;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    final String result = instance.toString();
    LOGGER.info(result);
    assertTrue(result.contains("ABC_Task container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription"));
    assertTrue(result.contains("bytes=byte[](length=8)"));
  }

  /**
   * Test of toBriefString method, of class Message.
   */
  @Test
  public void testToBriefString() {
    LOGGER.info("toBriefString");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    parameterName = "bytes";
    final byte[] byteArray = {1, 2, 3, 4, 5, 6, 7, 8};
    parameterValue = byteArray;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals("[ABC_Task, container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription]", instance.toBriefString());
  }

  /**
   * Test of toDetailedString method, of class Message.
   */
  @Test
  public void testToDetailedString() {
    LOGGER.info("toDetailedString");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    parameterName = "bytes";
    final byte[] byteArray = {1, 2, 3, 4, 5, 6, 7, 8};
    parameterValue = byteArray;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    LOGGER.info(instance.toDetailedString());
    assertTrue(instance.toDetailedString().contains("parameter: bytes=byte[](length=8)"));
  }

  /**
   * Test of equals method, of class Message.
   */
  @Test
  public void testEquals() {
    LOGGER.info("equals");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary1 = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary1.put(parameterName, parameterValue);
    Message instance1 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary1,
            "1.0.0");
    final Map<String, Object> parameterDictionary2 = new HashMap<>();
    parameterDictionary2.put(parameterName, parameterValue);
    Message instance2 = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary2,
            "1.0.0");
    assertTrue(instance1.equals(instance2));
    Message instance3 = new Message(
            "container1.agent1.role1", // senderQualifiedName
            "SenderService", // senderService
            "container3.agent2.role2", // recipientQualifiedName
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary1,
            "1.0.0");
    assertFalse(instance1.equals(instance3));
  }

  /**
   * Test of hashCode method, of class Message.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    LOGGER.info("recipientQualifiedName " + recipientQualifiedName);
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    int result = instance.hashCode();
    assertEquals(1577801551, result);
  }

  /**
   * Test of getter methods, of class Message.
   */
  @Test
  public void testGetters() {
    LOGGER.info("getters");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final String senderService = "MySenderService";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    final Map<String, Object> parameterDictionary = new HashMap<>();
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            senderService,
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    assertEquals(senderQualifiedName, instance.getSenderQualifiedName());
    assertEquals(senderService, instance.getSenderService());
    assertEquals(recipientQualifiedName, instance.getRecipientQualifiedName());
    assertEquals(conversationId, instance.getConversationId());
    assertEquals(replyWith, instance.getReplyWith());
    assertEquals(inReplyTo, instance.getInReplyTo());
    assertEquals(replyByDateTime, instance.getReplyByDateTime());
    assertNotNull(instance.getDate());
    assertEquals("1.0.0", instance.getVersion());
  }

  /**
   * Test of digital signature, of class Message.
   */
  @Test
  public void testDigitalSignature() {
    LOGGER.info("digital signature");
    final String senderQualifiedName = "container1.agent1.role1";
    final String recipientQualifiedName = "container2.agent2.role2";
    final UUID conversationId = UUID.randomUUID();
    final UUID replyWith = UUID.randomUUID();
    final UUID inReplyTo = UUID.randomUUID();
    final DateTime replyByDateTime = null;
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    final Map<String, Object> parameterDictionary = new HashMap<>();
    String parameterName = "my-parm";
    Serializable parameterValue = 10;
    parameterDictionary.put(parameterName, parameterValue);
    Message instance = new Message(
            senderQualifiedName,
            "SenderService", // senderService
            recipientQualifiedName,
            conversationId,
            replyWith,
            inReplyTo,
            replyByDateTime,
            service,
            "ABC_Task", // operation
            parameterDictionary,
            "1.0.0");
    LOGGER.info(instance);
    assertTrue(instance.toString().contains("ABC_Task container1.agent1.role1:SenderService --> container2.agent2.role2:RepositoryContentDescription "));
    try {
      final KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      final X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid
              "container1.agent1.role1"); // domainComponent
      LOGGER.info("certificate: " + x509Certificate);
      LOGGER.info("private key: " + keyPair.getPrivate());
      x509Certificate.checkValidity();
      // validate the certificate with the issuer's public key
      x509Certificate.verify(keyPair.getPublic());

      LOGGER.info("signing message");
      instance.sign(keyPair.getPrivate());
      LOGGER.info("Signature(in hex):: " + ByteUtils.toHex(instance.getSignatureBytes()));

      LOGGER.info("verifying message");
      boolean result = instance.verify(x509Certificate);
      LOGGER.info("Signature Verification Result = " + x509Certificate);
      assertTrue(result);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | SignatureException | InvalidKeyException | IOException | CertificateException ex) {
      LOGGER.info(StringUtils.getStackTraceAsString(ex));
      fail(ex.getMessage());
    }
  }
}
