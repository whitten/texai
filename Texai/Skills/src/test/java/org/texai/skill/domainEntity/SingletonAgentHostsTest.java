/*
 * Copyright (C) 2014 Texai
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
package org.texai.skill.domainEntity;

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
import org.texai.util.Base64Coder;
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
   * Test of verify method, of class SingletonAgentHosts.
   */
  @Test
  public void testVerify() {
    LOGGER.info("verify");
    SingletonAgentHosts singletonAgentHosts = makeSingletonAgentHosts();
    assertTrue(SingletonAgentHosts.verify(singletonAgentHosts, x509Certificate));
  }

  /**
   * Test of sha512Hash method, of class SingletonAgentHosts.
   */
  @Test
  public void testSha512Hash() {
    LOGGER.info("sha512Hash");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("6Kdo6Hjo+sjOXCzZfTEx4Ep6LZ+1IaxY29Zl7VgtgmMoLuw/iVJfydGZEuWJgwTAKLbkCoCjHNzDrLU0vi2iMg==", new String(Base64Coder.encode(instance.sha512Hash())));
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
   * Test of getAuthorQualifiedName method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetAuthorQualifiedName() {
    LOGGER.info("getAuthorQualifiedName");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("TestContainer.TestAgent.TestRole", instance.getAuthorQualifiedName());
  }

  /**
   * Test of getCreatedDateTime method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetCreatedDateTime() {
    LOGGER.info("getCreatedDateTime");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    assertEquals("2014-11-13T12:15:05.000-06:00", instance.getCreatedDateTime().toString());
  }

  /**
   * Test of getAuthorSignatureBytes method, of class SingletonAgentHosts.
   */
  @Test
  public void testGetAuthorSignatureBytes() {
    LOGGER.info("getAuthorSignatureBytes");
    SingletonAgentHosts instance = makeSingletonAgentHosts();
    final byte[] authorSignatureBytes = instance.getAuthorSignatureBytes();
    assertNotNull(authorSignatureBytes);
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
    final String authorQualifiedName = "TestContainer.TestAgent.TestRole";
    final DateTime createdDateTime = new DateTime(
            2014, // year
            11, // monthOfYear,
            13, // dayOfMonth
            12, // hourOfDay
            15, // minuteOfHour,
            5, // secondOfMinute,
            0, // millisOfSecond,
            DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
    final byte[] authorSignatureBytes = SingletonAgentHosts.signSingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime,
            authorQualifiedName,
            createdDateTime,
            keyPair.getPrivate()); // privateKey

    return new SingletonAgentHosts(
            singletonAgentDictionary,
            effectiveDateTime,
            terminationDateTime,
            authorQualifiedName,
            createdDateTime,
            authorSignatureBytes);
  }
}
