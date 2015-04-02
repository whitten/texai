/*
 * Copyright (C) 2014 Stephen L. Reed
 */
package org.texai.skill.domainEntity;

import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openrdf.model.URI;
import org.texai.kb.CacheInitializer;
import org.texai.kb.journal.JournalWriter;
import org.texai.kb.persistence.DistributedRepositoryManager;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.kb.persistence.RDFEntityPersister;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class SingletonAgentHostsTest {

  // the logger
  private final static Logger LOGGER = Logger.getLogger(SingletonAgentHostsTest.class);
  // the test key pair
  private static KeyPair keyPair;
  // the test X.509 certificate
  private static X509Certificate x509Certificate;
  // the RDF entity manager
  private static final RDFEntityManager rdfEntityManager = new RDFEntityManager();

  public SingletonAgentHostsTest() {
  }

  @BeforeClass
  public static void setUpClass() {

    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "MiscAgentObjects",
            true); // isRepositoryDirectoryCleaned
    try {
      keyPair = X509Utils.generateRSAKeyPair3072();
      x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid
              "TestContainer.TestAgent.TestRole"); // domainComponent
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  @AfterClass
  public static void tearDownClass() {
    rdfEntityManager.close();
    DistributedRepositoryManager.shutDown();
    CacheManager.getInstance().shutdown();
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of getId method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetId() {
    LOGGER.info("getId");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    URI id = instance.getId();
    assertNull(id);
    rdfEntityManager.persist(instance);
    id = instance.getId();
    assertNotNull(id);
    SingletonAgentHosts loadedSingletonAgentHosts = rdfEntityManager.find(SingletonAgentHosts.class, id);
    assertNotNull(loadedSingletonAgentHosts);
    assertEquals(instance.getId(), loadedSingletonAgentHosts.getId());
    rdfEntityManager.remove(instance);
    assertNull(rdfEntityManager.find(SingletonAgentHosts.class, id));
  }

  /**
   * Test of getSingletonAgentDictionary method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetSingletonAgentDictionary() {
    LOGGER.info("getSingletonAgentDictionary");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("{NetworkOperationAgent=Test2Container, MintAgent=Test3Container}", instance.getSingletonAgentDictionary().toString());
  }

  /**
   * Test of getContainer method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetContainer() {
    LOGGER.info("getContainer");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("Test3Container", instance.getContainer("MintAgent"));
    assertNull(instance.getContainer("NonexistentAgent"));
  }

  /**
   * Test of toString method, of class SingletonAgentHosts.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    final String result = instance.toString();
    assertEquals("[SingletonAgentHosts, size 2]", result);
  }

  /**
   * Test of toDetailedString method, of class SingletonAgentHosts.
   */
  @Test
  public void testToDetailedString() {
    LOGGER.info("toDetailedString");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("[SingletonAgentHosts, size 2\n  NetworkOperationAgent=Test2Container\n"
            + "  MintAgent=Test3Container\n"
            + "]", instance.toDetailedString());
  }

  /**
   * Test of hashCode method, of class SingletonAgentHosts.
   */
  @Test
  public void testHashCode() {
    LOGGER.info("hashCode");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals(-1615224141, instance.hashCode());
  }

  /**
   * Test of getEffectiveDateTime method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetEffectiveDateTime() {
    LOGGER.info("getEffectiveDateTime");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("2014-11-14T12:15:05.000-06:00", instance.getEffectiveDateTime().toString());
  }

  /**
   * Test of getTerminationDateTime method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetTerminationDateTime() {
    LOGGER.info("getTerminationDateTime");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("2015-11-14T12:15:05.000-06:00", instance.getTerminationDateTime().toString());
  }


  /**
   * Test of isNetworkSingleton method, of class SingletonAgentHosts.
   */
  @Test
  public void testIsNetworkSingleton() {
    LOGGER.info("isNetworkSingleton");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertTrue(instance.isNetworkSingleton("Bob.NetworkOperationAgent.NetworkOperationRole"));
    assertTrue(instance.isNetworkSingleton("Bob.MintAgent.MintRole"));
    assertFalse(instance.isNetworkSingleton("Bob.ContainerOperationAgent.ContainerOperationRole"));
  }

  /**
   * Test of mapNetworkSingleton method, of class SingletonAgentHosts.
   */
  @Test
  public void testMapNetworkSingleton() {
    LOGGER.info("mapNetworkSingleton");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("Test2Container.NetworkOperationAgent.NetworkOperationRole", instance.mapNetworkSingleton("Bob.NetworkOperationAgent.NetworkOperationRole"));
    assertEquals("Test3Container.MintAgent.MintRole", instance.mapNetworkSingleton("Bob.MintAgent.MintRole"));

  }

  /**
   * Makes a test SingletonAgentHosts instance.
   *
   * @return the test SingletonAgentHosts instance
   */
  private static SingletonAgentHosts makeSingletonAgentHosts() {
    final Map<String, String> singletonAgentDictionary = new HashMap<>();
    singletonAgentDictionary.put("NetworkOperationAgent", "Test2Container");
    singletonAgentDictionary.put("MintAgent", "Test3Container");
    final DateTime effectiveDateTime = new DateTime(
            2014, // year
            11, // monthOfYear,
            14, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    final DateTime terminationDateTime = new DateTime(
            2015, // year
            11, // monthOfYear,
            14, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    return new SingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime);
  }
}
