package org.texai.tamperEvidentLogs;

import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.tamperEvidentLogs.domainEntity.AbstractTELogEntry;
import org.texai.tamperEvidentLogs.domainEntity.TEKeyedLogItemEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogAuthenticatorEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogItemEntry;
import org.texai.tamperEvidentLogs.domainEntity.TELogHeader;
import org.texai.util.Base64Coder;
import org.texai.util.ByteUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.SerializableObjectSigner;

/**
 * Created on Sep 29, 2014, 12:31:08 PM.
 *
 * Description: Provides thread safe access to tamper-evident logs.
 *
 * See: https://www.usenix.org/legacy/event/sec09/tech/full_papers/crosby.pdf
 *
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
@ThreadSafe
public class TELogAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TELogAccess.class);
  // the RDF entity manager
  private final RDFEntityManager rdfEntityManager;
  // the tamper-evident log header dictionary, name --> tamper-evident log header
  private final Map<String, TELogHeader> teLogHeaderDictionary = new HashMap<>();

  /**
   * Constructs a new TELogAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public TELogAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /**
   * Creates a tamper evident log header for the given log name.
   *
   * @param name the given log name
   *
   * @return the tamper evident log header
   */
  public TELogHeader createTELogHeader(final String name) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    if (this.findTELogHeader(name) != null) {
      throw new TexaiException("TELogHeader for this name already exists: " + name);
    }

    synchronized (teLogHeaderDictionary) {
      if (teLogHeaderDictionary.containsKey(name)) {
        throw new TexaiException("name must be unique");
      }
      final TELogHeader teLogHeader = new TELogHeader(name);
      teLogHeaderDictionary.put(name, teLogHeader);
      rdfEntityManager.persist(teLogHeader);
      return teLogHeader;
    }
  }

  /**
   * Finds the tamper evident log header having the given name.
   *
   * @param name the given name
   *
   * @return the tamper evident log header having the given name, or null if not found
   */
  public TELogHeader findTELogHeader(final String name) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";

    TELogHeader teLogHeader;
    synchronized (teLogHeaderDictionary) {
      teLogHeader = teLogHeaderDictionary.get(name);
      if (teLogHeader == null) {
        final List<TELogHeader> results = rdfEntityManager.find(
                TELogHeader.NAME_FIELD_PREDICATE_TERM, // predicate
                name, // value
                TELogHeader.class); // clazz
        if (results.isEmpty()) {
          return null;
        } else if (results.size() == 1) {
          teLogHeader = results.get(0);
          teLogHeaderDictionary.put(name, teLogHeader);
        } else {
          throw new TexaiException("TELogHeader not unique for name " + name + "\n" + results);
        }
      }
    }
    return teLogHeader;
  }

  /**
   * Clears the tamper-evident log header dictionary - used to tear down unit tests.
   */
  protected void clearTELogHeaderDictionary() {
    teLogHeaderDictionary.clear();
  }

  /**
   * Appends the given log item to the head of the named log hash chain.
   *
   * @param name the name of the log hash chain
   * @param item the logged item, which must be serializable
   * @param chaosValue an optional chaos value
   *
   * @return the persisted tamper-evident log item
   */
  @SuppressWarnings({"SleepWhileInLoop", "SleepWhileHoldingLock"})
  public TELogItemEntry appendTELogItemEntry(
          final String name,
          final Serializable item,
          final String chaosValue) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    assert item != null : "item must not be null";

    final TELogHeader teLogHeader = findTELogHeader(name);
    if (teLogHeader == null) {
      throw new TexaiException("TELogHeader not found for " + name);
    }
    synchronized (teLogHeader) {
      final AbstractTELogEntry previousTELogEntry = teLogHeader.getHeadTELogEntry();
      int counter = 0;
      DateTime timestamp;
      // ensure that timestamps are unique and ordered
      while (true) {
        timestamp = new DateTime();
        if (previousTELogEntry == null) {
          break;
        } else if (timestamp.isAfter(previousTELogEntry.getTimestamp())) {
          break;
        } else {
          counter++;
          if (counter > 10000) {
            throw new TexaiException("waiting too long for out-of-sync timestamp");
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException ex) {
            throw new TexaiException(ex);
          }
        }
      }
      final String encodedItem = new String(Base64Coder.encode(ByteUtils.serialize(item)));
      final byte[] digest = TELogItemEntry.makeTELogItemEntryDigest(
              encodedItem,
              previousTELogEntry,
              timestamp,
              chaosValue);
      final String encodedDigest = new String(Base64Coder.encode(digest));
      final TELogItemEntry teLogItemEntry = new TELogItemEntry(
              encodedItem,
              previousTELogEntry,
              timestamp,
              chaosValue,
              encodedDigest);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info("chaosValue:       " + teLogItemEntry.getChaosValue());
        LOGGER.info("digest:           " + teLogItemEntry.getEncodedDigest());
      }
      rdfEntityManager.persist(teLogItemEntry);
      teLogHeader.setHeadTELogEntry(teLogItemEntry);
      rdfEntityManager.persist(teLogHeader);

      //Postconditions
      assert teLogItemEntry.verifyDigest() : "teLogItemEntry invalid digest";

      return teLogItemEntry;
    }
  }

  /**
   * Appends the given keyed log item to the head of the named log hash chain.
   *
   * @param name the name of the log hash chain
   * @param item the logged item, which must be serializable
   * @param key the key used to retrieve this entry from the persistent store
   * @param chaosValue an optional chaos value
   *
   * @return the persisted tamper-evident log item
   */
  @SuppressWarnings({"SleepWhileInLoop", "SleepWhileHoldingLock"})
  public TEKeyedLogItemEntry appendTEKeyedLogItemEntry(
          final String name,
          final Serializable item,
          final String key,
          final String chaosValue) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    assert item != null : "item must not be null";
    assert StringUtils.isNonEmptyString(key) : "key must be a non-empty string";

    final TELogHeader teLogHeader = this.findTELogHeader(name);
    if (teLogHeader == null) {
      throw new TexaiException("TELogHeader not found for " + name);
    }
    synchronized (teLogHeader) {
      final AbstractTELogEntry previousTELogEntry = teLogHeader.getHeadTELogEntry();
      int counter = 0;
      DateTime timestamp;
      // ensure that timestamps are unique and ordered
      while (true) {
        timestamp = new DateTime();
        if (previousTELogEntry == null) {
          break;
        } else if (timestamp.isAfter(previousTELogEntry.getTimestamp())) {
          break;
        } else {
          counter++;
          if (counter > 10000) {
            throw new TexaiException("waiting too long for out-of-sync timestamp");
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException ex) {
            throw new TexaiException(ex);
          }
        }
      }
      final String encodedItem = new String(Base64Coder.encode(ByteUtils.serialize(item)));
      final byte[] digest = TEKeyedLogItemEntry.makeTEKeyedLogItemEntryDigest(
              encodedItem,
              key,
              previousTELogEntry,
              timestamp,
              chaosValue);
      final String encodedDigest = new String(Base64Coder.encode(digest));
      final TEKeyedLogItemEntry teKeyedLogItemEntry = new TEKeyedLogItemEntry(
              encodedItem,
              key,
              previousTELogEntry,
              timestamp,
              chaosValue,
              encodedDigest);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.info("chaosValue:       " + teKeyedLogItemEntry.getChaosValue());
        LOGGER.info("digest:           " + teKeyedLogItemEntry.getEncodedDigest());
      }
      rdfEntityManager.persist(teKeyedLogItemEntry);
      teLogHeader.setHeadTELogEntry(teKeyedLogItemEntry);
      rdfEntityManager.persist(teLogHeader);

      //Postconditions
      assert teKeyedLogItemEntry.verifyDigest() : "teKeyedLogItemEntry invalid digest";

      return teKeyedLogItemEntry;
    }
  }

  /**
   * Finds the most recently added log item having the given unique key.
   *
   * @param name the name of the log hash chain
   * @param key the given key
   *
   * @return the most recently added log item having the given key, or null if not found
   */
  public Serializable findTEKeyedLogItem(final String name, final String key) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    assert StringUtils.isNonEmptyString(key) : "key must be a non-empty string";

    final TELogHeader teLogHeader = findTELogHeader(name);
    if (teLogHeader == null) {
      throw new TexaiException("TELogHeader not found for " + name);
    }
    synchronized (teLogHeader) {
      final List<TEKeyedLogItemEntry> results = rdfEntityManager.find(
              TEKeyedLogItemEntry.KEY_FIELD_PREDICATE_TERM, // predicate
              key, // value
              TEKeyedLogItemEntry.class); // clazz
      if (results.isEmpty()) {
        return null;
      } else if (results.size() == 1) {
        return results.get(0).getItem();
      } else {
        Collections.sort(results);
        return results.get(results.size() - 1).getItem();
      }
    }
  }

  /**
   * Appends a log authenticator entry to the head of the named log hash chain.
   *
   * @param name the name of the tamper-evident log hash chain
   * @param signingAgentName the signing agent's name
   * @param x509Certificate the signing agents's X.509 certificate
   * @param privateKey the private key for the certificate, which is not stored after performing the signature
   * @param chaosValue an optional chaos value
   *
   * @return the persisted log authenticator entry
   */
  @SuppressWarnings({"SleepWhileInLoop", "SleepWhileHoldingLock"})
  public TELogAuthenticatorEntry appendTELogAuthenticatorEntry(
          final String name,
          final String signingAgentName,
          final X509Certificate x509Certificate,
          final PrivateKey privateKey,
          final String chaosValue) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    assert StringUtils.isNonEmptyString(signingAgentName) : "signingAgentName must be a non-empty string";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert privateKey != null : "privateKey must not be null";

    final TELogHeader teLogHeader = findTELogHeader(name);
    if (teLogHeader == null) {
      throw new TexaiException("TELogHeader not found for " + name);
    }
    synchronized (teLogHeader) {
      final AbstractTELogEntry previousTELogEntry = teLogHeader.getHeadTELogEntry();
      if (teLogHeader.getHeadTELogEntry() == null) {
        throw new TexaiException("previousTELogEntry must not be null");
      }
      int counter = 0;
      DateTime timestamp;
      // ensure that timestamps are unique and ordered
      while (true) {
        timestamp = new DateTime();
        if (timestamp.isAfter(previousTELogEntry.getTimestamp())) {
          break;
        } else {
          counter++;
          if (counter > 10000) {
            throw new TexaiException("waiting too long for out-of-sync timestamp");
          }
          try {
            Thread.sleep(1);
          } catch (InterruptedException ex) {
            throw new TexaiException(ex);
          }
        }
      }
      final byte[] signatureBytes = TELogAuthenticatorEntry.signTELogAuthenticatorEntry(
              previousTELogEntry.getDigest(),
              x509Certificate,
              privateKey);
      final String encodedSignatureBytes = new String(Base64Coder.encode(signatureBytes));
      final byte[] digest = TELogAuthenticatorEntry.makeTELogAuthenticatorEntryDigest(
              signingAgentName,
              TELogAuthenticatorEntry.encodeSignatureBytes(signatureBytes), // encodedSignatureBytes
              previousTELogEntry,
              timestamp,
              chaosValue);
      final String encodedDigest = new String(Base64Coder.encode(digest));
      final TELogAuthenticatorEntry teLogAuthenticatorEntry = new TELogAuthenticatorEntry(
              signingAgentName,
              encodedSignatureBytes,
              previousTELogEntry,
              timestamp,
              chaosValue,
              encodedDigest);
      rdfEntityManager.persist(teLogAuthenticatorEntry);
      teLogHeader.setHeadTELogEntry(teLogAuthenticatorEntry);
      rdfEntityManager.persist(teLogHeader);

      //Postconditions
      assert teLogAuthenticatorEntry.verifyDigest() : "teLogAuthenticatorEntry invalid digest";
      assert teLogAuthenticatorEntry.verify(x509Certificate) : "x509Certificate failed to verify its signature";

      return teLogAuthenticatorEntry;
    }
  }

  /**
   * Returns the list of log entries given beginning and ending indices, with 0 indicating the head (most recent) entry.
   *
   * @param name the name of the log hash chain
   * @param nbrEntries the number of entries to return, or -1 to return them all
   *
   * @return the list of specified log entries
   */
  public List<AbstractTELogEntry> getTELogEntries(
          final String name,
          final int nbrEntries) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";
    assert nbrEntries >= -1 : "nbrEntries must not be less than -1";

    final List<AbstractTELogEntry> teLogEntries = new ArrayList<>();
    final TELogHeader teLogHeader = this.findTELogHeader(name);
    if (teLogHeader == null) {
      throw new TexaiException("missing TELogHeader named " + name);
    }
    AbstractTELogEntry teLogEntry = teLogHeader.getHeadTELogEntry();
    int count = 0;
    while (teLogEntry != null) {
      if (nbrEntries == -1 || count < nbrEntries) {
        teLogEntries.add(teLogEntry);
      }
      teLogEntry = teLogEntry.getPreviousTELogEntry();
      count++;
    }
    Collections.reverse(teLogEntries);
    return teLogEntries;
  }

  /**
   * Verifies the given list of log entries, and if valid returns a signature using the given X.509 certificate and private key, otherwise
   * returns null.
   *
   * @param teLogEntries the tamper-evident log entries
   * @param x509Certificate the verifying agent/role X.509 certificate
   * @param privateKey the verifying agent/role private key for its certificate
   *
   * @return returns a signature using the given X.509 certificate and private key, otherwise returns null
   */
  public static byte[] verifyAndSign(
          final List<AbstractTELogEntry> teLogEntries,
          final X509Certificate x509Certificate,
          final PrivateKey privateKey) {
    //Preconditions
    assert teLogEntries != null : "teLogEntries must not be null";
    assert !teLogEntries.isEmpty() : "teLogEntries must not be empty";
    assert x509Certificate != null : "x509Certificate must not be null";
    assert privateKey != null : "privateKey must not be null";

    if (verify(teLogEntries, x509Certificate)) {
      final AbstractTELogEntry lastTELogEntry = teLogEntries.get(teLogEntries.size() - 1);
      try {
        return SerializableObjectSigner.sign(
                lastTELogEntry.getEncodedDigest(),
                privateKey);
      } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | SignatureException ex) {
        throw new TexaiException(ex);
      }
    } else {
      return null;
    }
  }

  /**
   * Verifies whether the given list of log entries is valid with respect to its hash chain.
   *
   * @param teLogEntries the tamper-evident log entries
   * @param x509Certificate the logging agent/role's X.509 certificate
   *
   * @return whether the given list of log entries is valid with respect to its hash chain
   */
  public static boolean verify(
          final List<AbstractTELogEntry> teLogEntries,
          final X509Certificate x509Certificate) {
    //Preconditions
    assert teLogEntries != null : "teLogEntries must not be null";
    assert !teLogEntries.isEmpty() : "teLogEntries must not be empty";
    assert x509Certificate != null : "x509Certificate must not be null";

    AbstractTELogEntry previousTELogEntry = null;
    for (final AbstractTELogEntry teLogEntry : teLogEntries) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("verifying " + teLogEntry);
      }
      if (!teLogEntry.verifyDigest()) {
        return false;
      }
      if (previousTELogEntry != null && !previousTELogEntry.equals(teLogEntry.getPreviousTELogEntry())) {
        return false;
      } else {
        previousTELogEntry = teLogEntry;
      }
    }
    return true;
  }
}
