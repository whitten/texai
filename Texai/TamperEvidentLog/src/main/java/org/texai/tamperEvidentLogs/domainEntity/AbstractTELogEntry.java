package org.texai.tamperEvidentLogs.domainEntity;

import java.util.Objects;
import javax.persistence.Id;
import net.jcip.annotations.Immutable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.Base64Coder;
import org.texai.util.StringUtils;

/**
 * Created on Sep 30, 2014, 1:19:13 PM.
 *
 * Description: Provides a persistent tamper-evident log entry, which contains common fields and behavior.
 *
 * Copyright (C) Sep 30, 2014, Stephen L. Reed, Texai.org.
 *
 * @author reed
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
public abstract class AbstractTELogEntry implements RDFPersistent, Comparable<AbstractTELogEntry> {

  // the default serial version uid
  private static final long serialVersionUID = 1L;
  // the id assigned by the persistence framework
  @Id
  private URI id;
  // the previous tamper-evident log entry, or empty if this is the first
  @RDFProperty
  private final AbstractTELogEntry previousTELogEntry;
  // the logger's local timestamp, which is constrained to be unique for ordering the log
  @RDFProperty
  private final DateTime timestamp;
  // an optional chaos value
  @RDFProperty
  private final String chaosValue;
  // the SHA-512 hash digest of the previous entry's encodedDigest, plus this entry's timestamp, chaosValue and implementation fields, base-64 encoded
  @RDFProperty
  private final String encodedDigest;

  /**
   * Constructs a new empty AbstractTELogEntry. Used by the RDF persistance framework.
   */
  public AbstractTELogEntry() {
    previousTELogEntry = null;
    timestamp = null;
    chaosValue = null;
    encodedDigest = null;
  }

  /**
   * Constructs a new AbstractTELogEntry.
   *
   * @param previousTELogEntry the previous tamper-evident log entry, or empty if this is the first
   * @param timestamp the logger's local timestamp, which is constrained to be unique for ordering the log
   * @param chaosValue an optional chaos value
   * @param encodedDigest the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation
   * fields, encoded in base 64 for persistence
   */
  public AbstractTELogEntry(
          final AbstractTELogEntry previousTELogEntry,
          final DateTime timestamp,
          final String chaosValue,
          final String encodedDigest) {
    //Preconditions
    assert timestamp != null : "timestamp must not be null";
    assert StringUtils.isNonEmptyString(encodedDigest) : "encodedDigest must not be empty";

    this.previousTELogEntry = previousTELogEntry;
    this.chaosValue = chaosValue;
    this.timestamp = timestamp;
    this.encodedDigest = encodedDigest;

  }

  /**
   * Verifies whether this tamper-evident log entry's encoded digest is equal to a SHA-512 rehash of its fields.
   *
   * @return whether this tamper-evident log entry's encoded digest is equal to a SHA-512 rehash of its fields
   */
  public abstract boolean verifyDigest();

  /**
   * Returns the id used by the persistence framework.
   *
   * @return the id
   */
  @Override
  public URI getId() {
    return id;
  }

  /**
   * Gets the previous tamper-evident log entry.
   *
   * @return the previous tamper-evident log entry, or empty if this is the first
   */
  public AbstractTELogEntry getPreviousTELogEntry() {
    return previousTELogEntry;
  }

  /**
   * Return the timestamp.
   *
   * @return the timestamp
   */
  public DateTime getTimestamp() {
    return timestamp;
  }

  /**
   * Return the chaos value.
   *
   * @return the chaos value
   */
  public String getChaosValue() {
    return chaosValue;
  }

  /**
   * Encodes a base 64 encoded version of the given digest byte array for persistence.
   *
   * @param digest the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation fields
   *
   * @return a base 64 encoded version of the given digest
   */
  public static String encodeDigest(final byte[] digest) {
    assert digest != null : "digest must not be null";
    assert digest.length > 0 : "digest must not be empty";

    return new String(Base64Coder.encode(digest));
  }

  /**
   * Return the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation
   * fields, encoded in base 64.
   *
   * @return the SHA-512 hash encodedDigest
   */
  public String getEncodedDigest() {
    return encodedDigest;
  }

  /**
   * Return the SHA-512 hash digest of the previous entry's digest, plus this entry's timestamp, chaosValue and implementation
   * fields, decoded from its base 64 string.
   *
   * @return the SHA-512 hash encodedDigest
   */
  public byte[] getDigest() {
    return Base64Coder.decode(encodedDigest);
  }

  /**
   * Compares this object with the specified object for order. Returns a negative integer, zero, or a positive integer as this object is
   * less than, equal to, or greater than the specified object.
   *
   * @param that the specified object
   */
  @Override
  public int compareTo(final AbstractTELogEntry that) {
    //Preconditions
    assert that != null : "that must not be null";

    return DateTimeComparator.getInstance().compare(this.timestamp, that.timestamp);
  }

  /**
   * Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 5;
    hash = 97 * hash + Objects.hashCode(this.previousTELogEntry);
    hash = 97 * hash + Objects.hashCode(this.timestamp);
    hash = 97 * hash + Objects.hashCode(this.chaosValue);
    hash = 97 * hash + Objects.hashCode(this.encodedDigest);
    return hash;
  }

  /**
   * Returns whether another object is equal to this one.
   *
   * @param obj the other object
   *
   * @return whether another object is equal to this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final AbstractTELogEntry other = (AbstractTELogEntry) obj;
    if (!Objects.equals(this.previousTELogEntry, other.previousTELogEntry)) {
      return false;
    }
    if (!Objects.equals(this.timestamp, other.timestamp)) {
      return false;
    }
    if (!Objects.equals(this.chaosValue, other.chaosValue)) {
      return false;
    }
    return Objects.equals(this.encodedDigest, other.encodedDigest);
  }

}
