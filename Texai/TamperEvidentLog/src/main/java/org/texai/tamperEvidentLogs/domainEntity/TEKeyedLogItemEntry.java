package org.texai.tamperEvidentLogs.domainEntity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import net.jcip.annotations.Immutable;
import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.persistence.RDFUtility;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * Created on Sep 25, 2014, 12:44:53 PM.
 *
 * Description: Provides a tamper-evident, keyed, log item entry, whose serialized item which is persisted as an RDF string literal.
 *
 * Copyright (C) Sep 25, 2014, Stephen L. Reed, Texai.org.
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
@Immutable
@RDFEntity(context = "texai:TamperEvidentLogContext")
public class TEKeyedLogItemEntry extends TELogItemEntry {

  // the default serial version uid
  private static final long serialVersionUID = 1L;
  // the key field predicate term used by the persistence framework for retrieval
  public static URI KEY_FIELD_PREDICATE_TERM = RDFUtility.getDefaultPropertyURI(
          TEKeyedLogItemEntry.class.getName(), // className
          "key", // fieldName
          String.class); // fieldType)
  // the logger
  private static final Logger LOGGER = Logger.getLogger(TEKeyedLogItemEntry.class);
  // the key used to retrieve this entry from the persistent store
  @RDFProperty
  private final String key;

  /**
   * Constructs a new empty TELogEntry. Used by the RDF persistance framework.
   */
  public TEKeyedLogItemEntry() {
    key = null;
  }

  /**
   * Constructs a new TELogEntry.
   *
   * @param encodedItem the logged item, which must be serializable, and encoded in base 64
   * @param key the key used to retrieve this entry from the persistent store
   * @param previousTELogEntry the previous tamper-evident log entry, or empty if this is the first
   * @param timestamp the logger's local timestamp, which is constrained to be unique for ordering the log
   * @param chaosValue an optional chaos value
   * @param digest the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation
   * fields, encoded in base 64
   */
  public TEKeyedLogItemEntry(
          final String encodedItem,
          final String key,
          final AbstractTELogEntry previousTELogEntry,
          final DateTime timestamp,
          final String chaosValue,
          final String digest) {
    super(encodedItem, previousTELogEntry, timestamp, chaosValue, digest);
    //Preconditions
    assert StringUtils.isNonEmptyString(key) : "key must not be null";

    this.key = key;
  }

  /**
   * Verifies whether this tamper-evident log entry's digest is equal to a SHA-512 rehash of its fields.
   *
   * @return whether this tamper-evident log entry's digest is equal to a SHA-512 rehash of its fields
   */
  @Override
  public boolean verifyDigest() {
    final byte[] recomputedDigest = makeTEKeyedLogItemEntryDigest(
            getEncodedItem(),
            key,
            getPreviousTELogEntry(),
            getTimestamp(),
            getChaosValue());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("digest:           " + getEncodedDigest());
      LOGGER.info("recomputedDigest: " + encodeDigest(recomputedDigest));
    }
    return Arrays.areEqual(recomputedDigest, this.getDigest());
  }

  /**
   * Makes a SHA-512 hash digest of the given TELogItemEntry fields.
   *
   * @param encodedItem the logged item, which must be serializable, encoded in base 64
   * @param key the key used to retrieve this entry from the persistent store
   * @param previousTELogEntry the previous tamper-evident log entry, or empty if this is the first
   * @param timestamp the logger's local timestamp, which is constrained to be unique for ordering the log
   * @param chaosValue an optional chaos value
   *
   * @return a SHA-512 hash digest
   */
  public static byte[] makeTEKeyedLogItemEntryDigest(
          final String encodedItem,
          final String key,
          final AbstractTELogEntry previousTELogEntry,
          final DateTime timestamp,
          final String chaosValue) {
    //Preconditions
    assert encodedItem != null : "encodeItem must not be null";
    assert StringUtils.isNonEmptyString(key) : "key must not be null";
    assert timestamp != null : "timestamp must not be null";

    try {
      final MessageDigest messageDigest = MessageDigest.getInstance("SHA-512", X509Utils.BOUNCY_CASTLE_PROVIDER);
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      final DigestOutputStream digestOutputStream = new DigestOutputStream(byteArrayOutputStream, messageDigest);
      final ObjectOutputStream objectOutputStream = new ObjectOutputStream(digestOutputStream);
      if (previousTELogEntry != null) {
        // link the previous log entry in a hash chain
        objectOutputStream.writeObject(previousTELogEntry.getEncodedDigest());
      }
      // hash the fields for this log entry
      objectOutputStream.writeObject(encodedItem);
      objectOutputStream.writeObject(key);
      objectOutputStream.writeObject(timestamp);
      if (chaosValue != null) {
        objectOutputStream.writeObject(chaosValue);
      }
      return messageDigest.digest();
    } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Return the key used to retrieve this entry from the persistent store.
   *
   * @return the key.
   */
  public String getKey() {
    return key;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder())
            .append("[TEKeyedLogItemEntry ")
            .append(getTimestamp())
            .append(", key: ")
            .append(getKey())
            .append(", chaosValue: ")
            .append(getChaosValue())
            .append(']')
            .toString();
  }
}
