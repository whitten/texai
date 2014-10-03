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
import java.util.Iterator;
import java.util.List;
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
import org.texai.tamperEvidentLogs.domainEntity.TELogAuthenticatorEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogHeader;
import org.texai.tamperEvidentLogs.domainEntity.TELogItemEntry;
import org.texai.util.ByteUtils;
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
  public void testVerify() {
    LOGGER.info("verify");
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
    instance.appendTELogItemEntry(name, item, chaosValue);
  }

  /**
   * Test of appendTELogAuthenticatorEntry method, of class TELogAccess.
   */
  @Test
  public void testAppendTELogAuthenticatorEntry() {
    LOGGER.info("appendTELogAuthenticatorEntry");
    String name = TEST_LOG;
    String signingAgentName = "test-signing-agent";
    try {

      KeyPair keyPair = X509Utils.generateRSAKeyPair3072();
      X509Certificate x509Certificate = X509Utils.generateSelfSignedEndEntityX509Certificate(
              keyPair,
              UUID.randomUUID(), // uid,
              signingAgentName); // domainComponent
      PrivateKey privateKey = keyPair.getPrivate();
      String chaosValue = "test chaos value 2";
      TELogAccess instance = new TELogAccess(rdfEntityManager);
      instance.createTELogHeader(TEST_LOG);
      instance.appendTELogAuthenticatorEntry(name, signingAgentName, x509Certificate, privateKey, chaosValue);
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
