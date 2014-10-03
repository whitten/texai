/*
 * Copyright (C) Sep 29, 2014, Stephen L. Reed, Texai.org.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.tamperEvidentLogs;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.sf.ehcache.CacheManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
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
import org.texai.kb.persistence.RDFEntityRemover;
import org.texai.tamperEvidentLogs.domainEntity.AbstractTELogEntry;
import org.texai.tamperEvidentLogs.domainEntity.TEKeyedLogItemEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogAuthenticatorEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogHeader;
import org.texai.tamperEvidentLogs.domainEntity.TELogItemEntry;
import org.texai.util.ByteUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 *
 * @author reed
 */
public class TELogAccessTest {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TELogAccessTest.class);
  // the RDF entity manager
  private final static RDFEntityManager rdfEntityManager = new RDFEntityManager();
  // the test log name
  private final static String TEST_LOG = "TestLog";

  public TELogAccessTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    Logger.getLogger(RDFEntityPersister.class).setLevel(Level.WARN);
    Logger.getLogger(RDFEntityRemover.class).setLevel(Level.WARN);
    JournalWriter.deleteJournalFiles();
    CacheInitializer.initializeCaches();
    DistributedRepositoryManager.addTestRepositoryPath(
            "TamperEvidentLogs",
            true); // isRepositoryDirectoryCleaned
    X509Utils.addBouncyCastleSecurityProvider();
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
    // remove test objects from the persistent store
    final TELogAccess teLogAccess = new TELogAccess(rdfEntityManager);
    final TELogHeader teLogHeader = teLogAccess.findTELogHeader(TEST_LOG);
    if (teLogHeader != null) {
      rdfEntityManager.remove(teLogHeader);
    }
    teLogAccess.clearTELogHeaderDictionary();
    // test that there is no log header
    assertNull(teLogAccess.findTELogHeader(TEST_LOG));
    Iterator<TELogItemEntry> teLogItemEntries_iter = rdfEntityManager.rdfEntityIterator(TELogItemEntry.class);
    while (teLogItemEntries_iter.hasNext()) {
      final TELogItemEntry teLogItemEntry = teLogItemEntries_iter.next();
      rdfEntityManager.remove(teLogItemEntry);
    }
    // test that there are no log entries
    teLogItemEntries_iter = rdfEntityManager.rdfEntityIterator(TELogItemEntry.class);
    assertFalse(teLogItemEntries_iter.hasNext());
  }

  /**
   * Test of verify method, of class TELogAccess.
   */
  @Test
  public void testPersistentBase64EncodingOfByteArray() {
    LOGGER.info("base64");
    final Serializable item = 1;
    final AbstractTELogEntry previousTELogEntry = null;
    final DateTime timestamp = new DateTime();
    final String chaosValue = "chaos value 1";
    final String encodedItem = TELogItemEntry.encodeItem(item);
    final byte[] digest = TELogItemEntry.makeTELogItemEntryDigest(
            encodedItem,
            previousTELogEntry,
            timestamp,
            chaosValue);
    assertNotNull(digest);
    assertTrue(digest.length > 0);
    final byte[] digest2 = TELogItemEntry.makeTELogItemEntryDigest(
            encodedItem,
            previousTELogEntry,
            timestamp,
            chaosValue);
    assertNotNull(digest2);
    assertTrue(ByteUtils.areEqual(digest, digest2));
    final String encodedDigest = TELogItemEntry.encodeDigest(digest);
    final TELogItemEntry teLogItemEntry = new TELogItemEntry(
            encodedItem,
            previousTELogEntry,
            timestamp,
            chaosValue,
            encodedDigest); // digest
    assertNotNull(teLogItemEntry);
    LOGGER.info("chaosValue: " + teLogItemEntry.getChaosValue());
    LOGGER.info("encodedDigest:        " + teLogItemEntry.getEncodedDigest());
    assertTrue(teLogItemEntry.verifyDigest());
    rdfEntityManager.persist(teLogItemEntry);
    final URI id = teLogItemEntry.getId();
    assertNotNull(id);
    final TELogItemEntry loadedLogItemEntry = (TELogItemEntry) rdfEntityManager.find(id);
    assertEquals(teLogItemEntry.getEncodedItem(), loadedLogItemEntry.getEncodedItem());
    assertNull(teLogItemEntry.getPreviousTELogEntry());
    assertNull(loadedLogItemEntry.getPreviousTELogEntry());
    assertEquals(teLogItemEntry.getTimestamp().toString(), loadedLogItemEntry.getTimestamp().toString());
    assertEquals(teLogItemEntry.getChaosValue(), loadedLogItemEntry.getChaosValue());
    assertNotNull(loadedLogItemEntry);
    LOGGER.info("loaded encodedDigest: " + loadedLogItemEntry.getEncodedDigest());
  }

  /**
   * Test of createTELogHeader method, of class TELogAccess.
   */
  @Test
  public void testCreateTELogHeader() {
    LOGGER.info("createTELogHeader");
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    TELogHeader result = instance.createTELogHeader(TEST_LOG);
    assertNotNull(result);
    final URI id = result.getId();
    assertNotNull(id);
    assertEquals(TEST_LOG, result.getName());
    assertNull(result.getHeadTELogEntry());
    assertEquals("[TELogHeader TestLog]", result.toString());
    final TELogHeader loadedTELogHeader = rdfEntityManager.find(TELogHeader.class, id);
    assertNotNull(loadedTELogHeader);
    assertEquals(result, loadedTELogHeader);
  }

  /**
   * Test of createTELogHeader method, of class TELogAccess.
   */
  @Test
  public void testCreateTELogHeader2() {
    LOGGER.info("createTELogHeader2");
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    instance.createTELogHeader(TEST_LOG);
    try {
      // expect an exception to occur when creating a duplicate log header
      instance.createTELogHeader(TEST_LOG);
      fail();
    } catch (TexaiException ex) {
    }
  }

  /**
   * Test of findTELogHeader method, of class TELogAccess.
   */
  @Test
  public void testFindTELogHeader() {
    LOGGER.info("findTELogHeader");
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    instance.createTELogHeader(TEST_LOG);
    String name = TEST_LOG;
    TELogHeader result = instance.findTELogHeader(name);
    assertNotNull(result);
    assertEquals("[TELogHeader TestLog]", result.toString());
  }

  /**
   * Test of appendTELogItemEntry method, of class TELogAccess.
   */
  @Test
  public void testAppendTELogItemEntry() {
    LOGGER.info("appendTELogItemEntry");
    String name = TEST_LOG;
    Serializable item = "test item string";
    String chaosValue = "test chaos value 1";
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    instance.createTELogHeader(TEST_LOG);
    final TELogItemEntry teLogItemEntry = instance.appendTELogItemEntry(name, item, chaosValue);
    assertNotNull(teLogItemEntry);
  }

  /**
   * Test of appendTEKeyedLogItemEntry method, of class TELogAccess.
   */
  @Test
  public void testAppendTEKeyedLogItemEntry() {
    LOGGER.info("appendTEKeyedLogItemEntry");
    String name = TEST_LOG;
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    instance.createTELogHeader(TEST_LOG);
    TEKeyedLogItemEntry teKeyedLogItemEntry = instance.appendTEKeyedLogItemEntry(
            name,
            "item a", // item
            "key a", // key
            "chaos 0"); // chaos value
    assertNotNull(teKeyedLogItemEntry);
    assertEquals("item a", teKeyedLogItemEntry.getItem());
    assertEquals("key a", teKeyedLogItemEntry.getKey());
    assertEquals("chaos 0", teKeyedLogItemEntry.getChaosValue());
    final URI id = teKeyedLogItemEntry.getId();
    assertNotNull(id);
    final TEKeyedLogItemEntry loadedTEKeyedLogItemEntry = rdfEntityManager.find(TEKeyedLogItemEntry.class, id);
    assertNotNull(loadedTEKeyedLogItemEntry);
    assertEquals("item a", loadedTEKeyedLogItemEntry.getItem());
    assertEquals("key a", loadedTEKeyedLogItemEntry.getKey());
    assertEquals("chaos 0", loadedTEKeyedLogItemEntry.getChaosValue());
    assertEquals(teKeyedLogItemEntry.toString(), loadedTEKeyedLogItemEntry.toString());
    assertEquals(teKeyedLogItemEntry.getTimestamp().toString(), loadedTEKeyedLogItemEntry.getTimestamp().toString());
    // test persistence and loading of the DateTime object and ensuring the default ISO chronolgy is used
    assertEquals(teKeyedLogItemEntry.getTimestamp(), loadedTEKeyedLogItemEntry.getTimestamp());
    assertEquals(teKeyedLogItemEntry.hashCode(), loadedTEKeyedLogItemEntry.hashCode());
    assertEquals(teKeyedLogItemEntry, loadedTEKeyedLogItemEntry);
    instance.appendTEKeyedLogItemEntry(name, "item b", "key b", "chaos 1");
    instance.appendTEKeyedLogItemEntry(name, "item e", "key e", "chaos 2");
    instance.appendTEKeyedLogItemEntry(name, "item f", "key f", "chaos 3");
    instance.appendTEKeyedLogItemEntry(name, "item g", "key g", "chaos 4");
    instance.appendTEKeyedLogItemEntry(name, "item c", "key c", "chaos 5");
    // duplicate key to an earlier entry
    instance.appendTEKeyedLogItemEntry(name, "item g2", "key g", "chaos 6");
    instance.appendTEKeyedLogItemEntry(name, "item d", "key d", "chaos 7");
    assertNull(instance.findTEKeyedLogItem(TEST_LOG, "wrong key"));
    Serializable item = instance.findTEKeyedLogItem(TEST_LOG, "key a");
    assertNotNull(item);
    assertEquals("item a", item);
    assertEquals("item b", instance.findTEKeyedLogItem(TEST_LOG, "key b"));
    assertEquals("item c", instance.findTEKeyedLogItem(TEST_LOG, "key c"));
    assertEquals("item d", instance.findTEKeyedLogItem(TEST_LOG, "key d"));
    assertEquals("item e", instance.findTEKeyedLogItem(TEST_LOG, "key e"));
    assertEquals("item f", instance.findTEKeyedLogItem(TEST_LOG, "key f"));
    assertEquals("item g2", instance.findTEKeyedLogItem(TEST_LOG, "key g"));
  }

  /**
   * Test of appendTELogAuthenticatorEntry method, of class TELogAccess.
   */
  @Test
  public void testAppendTELogAuthenticatorEntry() {
    LOGGER.info("appendTELogAuthenticatorEntry");
    String name = TEST_LOG;
    TELogAccess instance = new TELogAccess(rdfEntityManager);
    instance.createTELogHeader(TEST_LOG);
    Serializable item = "test item string";
    String chaosValue = "test chaos value 1";
    instance.appendTELogItemEntry(name, item, chaosValue);

    String signingAgentName = "test-signing-agent";
    try {

      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue2 = "test chaos value 2";
      final TELogAuthenticatorEntry teLogAuthenticatorEntry = instance.appendTELogAuthenticatorEntry(
              name,
              signingAgentName,
              x509Certificate,
              privateKey,
              chaosValue2);
      assertNotNull(teLogAuthenticatorEntry);
      assertTrue(teLogAuthenticatorEntry.verify(x509Certificate));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of getTELogEntries method, of class TELogAccess.
   */
  @Test
  public void testGetTELogEntries() {
    LOGGER.info("getTELogEntries");
    String name = TEST_LOG;
    int nbrEntries = 5;
    TELogAccess instance = new TELogAccess(rdfEntityManager);

    instance.createTELogHeader(TEST_LOG);
    instance.appendTELogItemEntry(name, 1, "test chaos value 1");
    instance.appendTELogItemEntry(name, 2, "test chaos value 2");
    instance.appendTELogItemEntry(name, "test item string 3", "test chaos value 3");
    instance.appendTELogItemEntry(name, "test item string 4", "test chaos value 4");

    try {
      String signingAgentName = "test-signing-agent";
      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue = "test chaos value 2";
      instance.appendTELogAuthenticatorEntry(name, signingAgentName, x509Certificate, privateKey, chaosValue);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }

    instance.appendTELogItemEntry(name, "test item string 5", "test chaos value 5");

    List<AbstractTELogEntry> result = instance.getTELogEntries(name, nbrEntries);
    String resultString = result.toString();
    LOGGER.info(resultString);
    Collections.reverse(result);
    Collections.sort(result);
    assertEquals(resultString, result.toString());
    assertTrue(result.get(0) instanceof TELogItemEntry);
    TELogItemEntry teLogItemEntry = (TELogItemEntry) result.get(0);
    assertEquals(2, teLogItemEntry.getItem());
    assertTrue(result.get(3) instanceof TELogAuthenticatorEntry);
    TELogAuthenticatorEntry teLogAuthenticatorEntry = (TELogAuthenticatorEntry) result.get(3);
    assertEquals("test-signing-agent", teLogAuthenticatorEntry.getSigningAgentName());
  }

  /**
   * Test of verify method, of class TELogAccess.
   */
  @Test
  public void testVerify() {
    LOGGER.info("verify");
    String name = TEST_LOG;
    int nbrEntries = -1;
    TELogAccess instance = new TELogAccess(rdfEntityManager);

    instance.createTELogHeader(TEST_LOG);
    instance.appendTELogItemEntry(name, 1, "test chaos value 0");
    instance.appendTELogItemEntry(name, 2, "test chaos value 1");
    instance.appendTELogItemEntry(name, "test item string 3", "test chaos value 2");
    instance.appendTELogItemEntry(name, "test item string 4", "test chaos value 3");

    try {
      String signingAgentName = "test-signing-agent";
      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue = "test chaos value 4";
      instance.appendTELogAuthenticatorEntry(name, signingAgentName, x509Certificate, privateKey, chaosValue);

      instance.appendTELogItemEntry(name, "test item string 5", "test chaos value 5");
      instance.appendTELogItemEntry(name, "test item string 6", "test chaos value 6");

      List<AbstractTELogEntry> teLogEntries = instance.getTELogEntries(name, nbrEntries);
      assertEquals(7, teLogEntries.size());
      assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of verify method, of class TELogAccess.
   */
  @Test
  public void testVerify2() {
    LOGGER.info("verify2");
    String name = TEST_LOG;
    int nbrEntries = -1;
    TELogAccess instance = new TELogAccess(rdfEntityManager);

    instance.createTELogHeader(TEST_LOG);
    instance.appendTELogItemEntry(name, 1, "test chaos value 0");
    instance.appendTELogItemEntry(name, 2, "test chaos value 1");
    instance.appendTELogItemEntry(name, "test item string 3", "test chaos value 2");
    instance.appendTELogItemEntry(name, "test item string 4", "test chaos value 3");

    try {
      String signingAgentName = "test-signing-agent";
      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue = "test chaos value 4";
      instance.appendTELogAuthenticatorEntry(name, signingAgentName, x509Certificate, privateKey, chaosValue);

      instance.appendTELogItemEntry(name, "test item string 5", "test chaos value 5");
      instance.appendTELogItemEntry(name, "test item string 6", "test chaos value 6");

      List<AbstractTELogEntry> teLogEntries = instance.getTELogEntries(name, nbrEntries);
      assertEquals(7, teLogEntries.size());

      // get TELogItemEntry for tamper tests
      assertTrue(teLogEntries.get(3) instanceof TELogItemEntry);
      final TELogItemEntry teLogItemEntry3 = (TELogItemEntry) teLogEntries.get(3);
      assertEquals("test chaos value 3", teLogItemEntry3.getChaosValue());

      // get TELogAuthenticatorEntry for tamper tests
      assertTrue(teLogEntries.get(4) instanceof TELogAuthenticatorEntry);
      final TELogAuthenticatorEntry teLogAuthenticatorEntry4 = (TELogAuthenticatorEntry) teLogEntries.get(4);
      assertEquals("test chaos value 4", teLogAuthenticatorEntry4.getChaosValue());

      // corrupt each field in turn to demonstrate tamper-evidence - relection is used to mutate private fields
      try {
        // previousTELogEntry
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));
        AbstractTELogEntry previousTELogEntry = teLogItemEntry3.getPreviousTELogEntry();
        Field field = AbstractTELogEntry.class.getDeclaredField("previousTELogEntry");
        field.setAccessible(true);
        assertNotNull(teLogItemEntry3.getPreviousTELogEntry());
        field.set(teLogItemEntry3, null); // corrupt
        assertNull(teLogItemEntry3.getPreviousTELogEntry());
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogItemEntry3, previousTELogEntry); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // timestamp
        DateTime timestamp = teLogItemEntry3.getTimestamp();
        field = AbstractTELogEntry.class.getDeclaredField("timestamp");
        field.setAccessible(true);
        field.set(teLogItemEntry3, new DateTime()); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogItemEntry3, timestamp); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // chaosValue
        String chaosValue1 = teLogItemEntry3.getChaosValue();
        field = AbstractTELogEntry.class.getDeclaredField("chaosValue");
        field.setAccessible(true);
        field.set(teLogItemEntry3, "corrupt chaos value"); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogItemEntry3, chaosValue1); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // encodedDigest
        String encodedDigest = teLogItemEntry3.getEncodedDigest();
        assertTrue(StringUtils.isNonEmptyString(encodedDigest));
        assertFalse(encodedDigest.equals(teLogAuthenticatorEntry4.getEncodedDigest()));
        field = AbstractTELogEntry.class.getDeclaredField("encodedDigest");
        field.setAccessible(true);
        field.set(teLogItemEntry3, teLogAuthenticatorEntry4.getEncodedDigest()); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogItemEntry3, encodedDigest); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // encodedItem
        String encodedItem = teLogItemEntry3.getEncodedItem();
        field = TELogItemEntry.class.getDeclaredField("encodedItem");
        field.setAccessible(true);
        field.set(teLogItemEntry3, TELogItemEntry.encodeItem("corrupted item")); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogItemEntry3, encodedItem); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // signingAgentName
        String signingAgentName1 = teLogAuthenticatorEntry4.getSigningAgentName();
        field = TELogAuthenticatorEntry.class.getDeclaredField("signingAgentName");
        field.setAccessible(true);
        field.set(teLogAuthenticatorEntry4, "corrupted signing agent name"); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogAuthenticatorEntry4, signingAgentName1); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

        // encodedSignatureBytes
        String encodedSignatureBytes = teLogAuthenticatorEntry4.getEncodedSignatureBytes();
        field = TELogAuthenticatorEntry.class.getDeclaredField("encodedSignatureBytes");
        field.setAccessible(true);
        byte[] corruptSignatureBytes = teLogAuthenticatorEntry4.getSignatureBytes();
        if (corruptSignatureBytes[0] < 0) {
          corruptSignatureBytes[0] = (byte) (corruptSignatureBytes[0] + 1);
        } else {
          corruptSignatureBytes[0] = (byte) (corruptSignatureBytes[0] - 1);
        }
        field.set(teLogAuthenticatorEntry4, TELogAuthenticatorEntry.encodeSignatureBytes(corruptSignatureBytes)); // corrupt
        assertFalse(TELogAccess.verify(teLogEntries, x509Certificate));
        field.set(teLogAuthenticatorEntry4, encodedSignatureBytes); // valid
        assertTrue(TELogAccess.verify(teLogEntries, x509Certificate));

      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
        LOGGER.info(StringUtils.getStackTraceAsString(ex));
        fail(ex.getMessage());
      }

    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test of verifyAndSign method, of class TELogAccess.
   */
  @Test
  public void testVerifyAndSign() {
    LOGGER.info("verifyAndSign");
    String name = TEST_LOG;
    int nbrEntries = -1;
    TELogAccess instance = new TELogAccess(rdfEntityManager);

    instance.createTELogHeader(TEST_LOG);
    instance.appendTELogItemEntry(name, 1, "test chaos value 1");
    instance.appendTELogItemEntry(name, 2, "test chaos value 2");
    instance.appendTELogItemEntry(name, "test item string 3", "test chaos value 3");
    instance.appendTELogItemEntry(name, "test item string 4", "test chaos value 4");

    try {
      String signingAgentName = "test-signing-agent";
      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue = "test chaos value 2";
      instance.appendTELogAuthenticatorEntry(name, signingAgentName, x509Certificate, privateKey, chaosValue);

      instance.appendTELogItemEntry(name, "test item string 5", "test chaos value 5");

      List<AbstractTELogEntry> teLogEntries = instance.getTELogEntries(name, nbrEntries);
      assertEquals(6, teLogEntries.size());
      byte[] result = TELogAccess.verifyAndSign(teLogEntries, x509Certificate, privateKey);
      assertNotNull(result);
    } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | CertificateParsingException | CertificateEncodingException | SignatureException | InvalidKeyException | IOException ex) {
      fail(ex.getMessage());
    }
  }

}
