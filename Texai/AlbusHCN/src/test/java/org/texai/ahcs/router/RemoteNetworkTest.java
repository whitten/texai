/*
 * RemoteNetworkTest.java
 *
 * Created on Mar 18, 2010, 10:28:17 AM
 *
 * Description: Tests a simulated network spanning three JVMs in which the inter-node communications take place over
 * SSL transport.
 *
 * Copyright (C) Mar 18, 2010 reed.
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
package org.texai.ahcs.router;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.texai.kb.persistence.RDFEntityManager;

/** Tests a simulated network spanning two JVMs in which the inter-node communications take place over
 * SSL transport.
 *
 * @author reed
 */
@NotThreadSafe
public class RemoteNetworkTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(RemoteNetworkTest.class);
  /** the RDF entity manager */
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();
  /** the message router */
  private static CPOSMessageRouter messageRouter;

  /** sets debugging */
//  static {
//    System.setProperty("javax.net.debug", "all");
//  }
  /** Constructs a new RemoteNetworkTest instance. */
  public RemoteNetworkTest() {
  }

  @Test
  public void test1() {
  }

//  @BeforeClass
//  public static void setUpClass() throws Exception {
//    if (!X509Utils.isTrustedDevelopmentSystem()) {
//      return;
//    }
//    System.setProperty(Log4jLogger.PROPERTIES_FILE_PROPERTY, "log4j.properties");
//    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
//    Logger.getLogger(TaggedObjectEncoder.class).setLevel(Level.WARN);
//    Logger.getLogger(TaggedObjectDecoder.class).setLevel(Level.WARN);
//    Logger.getLogger(PortUnificationHandler.class).setLevel(Level.WARN);
//    JournalWriter.deleteJournalFiles();
//    CacheInitializer.initializeCaches();
//    DistributedRepositoryManager.addRepositoryPath(
//            "Nodes",
//            System.getenv("REPOSITORIES_TMPFS") + "/Nodes");
//    DistributedRepositoryManager.clearNamedRepository("Nodes");
//
//    final URI messageRouterRoleId = new URIImpl(Constants.TEXAI_NAMESPACE + UUID.randomUUID());
//    LOGGER.info("messageRouterRoleId: " + messageRouterRoleId);
//    KeyPair keyPair = null;
//    try {
//      keyPair = X509Utils.generateRSAKeyPair2048();
//    } catch (Exception ex) {
//      fail(ex.getMessage());
//    }
//    final PrivateKey issuerPrivateKey = X509Utils.getRootPrivateKey();
//    final X509Certificate issuerCertificate = X509Utils.getRootX509Certificate();
//    final UUID uid = UUIDUtils.uriToUUID(messageRouterRoleId);
//    final char[] keystorePassword = "my-password".toCharArray();
//    X509SecurityInfo x509SecurityInfo = X509Utils.generateX509SecurityInfo(
//            keyPair,
//            issuerPrivateKey,
//            issuerCertificate,
//            uid,
//            keystorePassword,
//            X509Utils.isJCEUnlimitedStrengthPolicy());
//    messageRouter = new MessageRouter(
//            x509SecurityInfo,
//            messageRouterRoleId); // random test role id
//  }
//
//  @AfterClass
//  public static void tearDownClass() throws Exception {
//    if (!X509Utils.isTrustedDevelopmentSystem()) {
//      return;
//    }
//    rdfEntityManager.close();
//    DistributedRepositoryManager.shutDown();
//    CacheManager.getInstance().shutdown();
//  }
//
//  /**
//   * Test of small local network without message queueing.
//   */
//  @Test
//  public void testSmallRemoteNetork1() {
//    if (!X509Utils.isTrustedDevelopmentSystem()) {
//      return;
//    }
//    LOGGER.info("small remote network with direct execution");
//
//    // construct top level node in node runtime 1
//    final URI nodeRuntimeRoleId1 = new URIImpl(Constants.TEXAI_NAMESPACE + UUID.randomUUID());
//    LOGGER.info("nodeRuntimeRoleId1: " + nodeRuntimeRoleId1);
//    final NodeRuntime nodeRuntime1 = new NodeRuntime(
//            KeyStoreTestUtils.getClientX509SecurityInfo(),
//            nodeRuntimeRoleId1); // random test role id
//    nodeRuntime1.connectToMessageRouter();
//    final URI nodeType1 = new URIImpl(Constants.TEXAI_NAMESPACE + "TestNodeLevel1");
//    final Node node1 = new Node(nodeType1);
//    final URI roleType = new URIImpl(Constants.TEXAI_NAMESPACE + "TestRole");
//    final Skill sensationRepeaterSkill1 = new Skill("org.texai.ahcs.SensationRepeaterLibrary");
//    final Skill taskRepeaterSkill1 = new Skill("org.texai.ahcs.TaskRepeaterLibrary");
//    final Role role1 = new Role(
//            roleType,
//            nodeRuntime1,
//            nodeRuntime1.getRoleId());
//    role1.setNode(node1);
//    role1.getSkills().add(sensationRepeaterSkill1);
//    sensationRepeaterSkill1.setRole(role1);
//    role1.getSkills().add(taskRepeaterSkill1);
//    taskRepeaterSkill1.setRole(role1);
//    rdfEntityManager.persist(role1);
//    LOGGER.info("role1 id: " + role1.getId());
//    KeyPair keyPair1 = null;
//    try {
//      keyPair1 = X509Utils.generateRSAKeyPair2048();
//    } catch (Exception ex) {
//      fail(ex.getMessage());
//    }
//    final PrivateKey issuerPrivateKey = X509Utils.getRootPrivateKey();
//    final X509Certificate issuerCertificate = X509Utils.getRootX509Certificate();
//    final UUID uid1 = UUIDUtils.uriToUUID(role1.getId());
//    final char[] keystorePassword1 = "my-password".toCharArray();
//    X509SecurityInfo x509SecurityInfo1 = X509Utils.generateX509SecurityInfo(
//            keyPair1,
//            issuerPrivateKey,
//            issuerCertificate,
//            uid1,
//            keystorePassword1,
//            X509Utils.isJCEUnlimitedStrengthPolicy());
//    role1.setX509SecurityInfo(x509SecurityInfo1);
//    sensationRepeaterSkill1.setX509SecurityInfo(x509SecurityInfo1);
//    taskRepeaterSkill1.setX509SecurityInfo(x509SecurityInfo1);
//    assertEquals(X509Utils.getUUID(x509SecurityInfo1.getX509Certificate()), UUIDUtils.uriToUUID(role1.getId()));
//    nodeRuntime1.registerRole(role1);
//    node1.addRole(role1);
//    node1.setPrimaryRole(role1);
//    rdfEntityManager.persist(node1);
//    role1.enableRemoteMessaging();
//
//    // construct second level node in node runtime 2
//    final URI nodeRuntimeRoleId2 = new URIImpl(Constants.TEXAI_NAMESPACE + UUID.randomUUID());
//    LOGGER.info("nodeRuntimeRoleId2: " + nodeRuntimeRoleId2);
//    final NodeRuntime nodeRuntime2 = new NodeRuntime(
//            KeyStoreTestUtils.getClientX509SecurityInfo(),
//            nodeRuntimeRoleId2); // random test role id
//    nodeRuntime2.connectToMessageRouter();
//    final URI nodeType2 = new URIImpl(Constants.TEXAI_NAMESPACE + "TestNodeLevel2");
//    final Node node2 = new Node(nodeType2);
//    final Skill sensationRepeaterSkill2 = new Skill("org.texai.ahcs.SensationRepeaterLibrary");
//    final Skill taskRepeaterSkill2 = new Skill("org.texai.ahcs.TaskRepeaterLibrary");
//    final Role role2 = new Role(
//            roleType,
//            nodeRuntime2,
//            nodeRuntime2.getRoleId());
//    role2.setNode(node2);
//    role2.getSkills().add(sensationRepeaterSkill2);
//    sensationRepeaterSkill2.setRole(role2);
//    role2.getSkills().add(taskRepeaterSkill2);
//    taskRepeaterSkill2.setRole(role2);
//    rdfEntityManager.persist(role2);
//    LOGGER.info("role2 id: " + role2.getId());
//    KeyPair keyPair2 = null;
//    try {
//      keyPair2 = X509Utils.generateRSAKeyPair2048();
//    } catch (Exception ex) {
//      fail(ex.getMessage());
//    }
//    final UUID uid2 = UUIDUtils.uriToUUID(role2.getId());
//    final char[] keystorePassword2 = "my-password".toCharArray();
//    X509SecurityInfo x509SecurityInfo2 = X509Utils.generateX509SecurityInfo(
//            keyPair2,
//            issuerPrivateKey,
//            issuerCertificate,
//            uid2,
//            keystorePassword2,
//            X509Utils.isJCEUnlimitedStrengthPolicy());
//    role2.setX509SecurityInfo(x509SecurityInfo2);
//    sensationRepeaterSkill2.setX509SecurityInfo(x509SecurityInfo2);
//    taskRepeaterSkill2.setX509SecurityInfo(x509SecurityInfo2);
//    nodeRuntime2.registerRole(role2);
//    node2.addRole(role2);
//    node2.setPrimaryRole(role2);
//    rdfEntityManager.persist(node2);
//    role2.enableRemoteMessaging();
//
//    // connect the roles
//    role1.getChildRoleIdStrings().add(role2.getId().toString());
//    role2.setParentRoleIdString(role1.getId().toString());
//
//    // send a task to the level 2 role commanding it to send a test sensation to its parent, thus starting the test
//    final UUID conversationId = UUID.randomUUID();
//    final UUID replyWith = UUID.randomUUID();
//    final DateTime replyByDateTime = null;
//    final boolean mustRoleBeReady = true;
//    final String operation = "Start_Task";
//    final Map<String, Object> parameterDictionary = new HashMap<String, Object>();
//    final Message taskMessage = new Message(
//            role2.getId(), // senderRoleId - as if sent from self
//            "MyService", // senderService
//            role2.getId(), // recipientRoleId
//            conversationId,
//            replyWith,
//            null, // inReplyTo
//            replyByDateTime,
//            "MyService", // service
//            operation,
//            parameterDictionary);
//    taskMessage.sign(x509SecurityInfo2.getPrivateKey());
//    role2.sendMessage(taskMessage);
//    try {
//      Thread.sleep(5000);
//    } catch (InterruptedException ex) {
//      fail(ex.getMessage());
//    }
//  }
}
