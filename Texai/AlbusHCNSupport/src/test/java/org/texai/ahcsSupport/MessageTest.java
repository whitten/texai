/*
 * MessageTest.java
 *
 * Created on Jun 30, 2008, 2:33:34 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 3, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.ahcsSupport;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
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

/**
 *
 * @author reed
 */
public class MessageTest {

  /** the logger */
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    assertTrue(instance.toString().startsWith("[container1.agent1.role1:SenderService --> container2.agent2.role2:MyService (ABC_Task) {"));
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
    final String service = "MyService";
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
    //assertTrue(instance1.equals(instance2));  only true if both instances are created at the same time
    Message instance3 = new Message(
            "container1.agent1.role1", // senderQualifiedName
            "SenderService", // senderService
            "container2.agent2.role2", // recipientQualifiedName
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    final String service = "MyService";
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
    assertTrue(instance.toString().startsWith("[container1.agent1.role1:SenderService --> container2.agent2.role2:MyService (ABC_Task) {"));
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
    } catch (Exception ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
  }
}
