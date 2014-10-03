package org.texai.tamperEvidentLogs.domainEntity;

import java.util.Objects;
import javax.persistence.Id;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.util.StringUtils;

/**
 * Created on Sep 29, 2014, 12:10:00 PM.
 *
 * Description: Provides a header for the log entry hash chain.
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
@NotThreadSafe
@RDFEntity(context = "texai:TamperEvidentLogContext")
public class TELogHeader implements RDFPersistent {

  // the default serial version uid
  private static final long serialVersionUID = 1L;
  // the name field predicate term
  public static URI NAME_FIELD_PREDICATE_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "teLogHeader_name");
  // the id assigned by the persistence framework
  @Id
  private URI id;
  // the name of this log hash chain
  @RDFProperty(predicate="teLogHeader_name")
  private final String name;
  // the head of the log item hash chain
  @RDFProperty
  private AbstractTELogEntry headTELogEntry;

  /**
   * Constructs a new empty TELogHeader instance. Used by the persistence framework.
   */
  public TELogHeader() {
    name = null;
    headTELogEntry = null;
  }

  /**
   * Constructs a new TELogHeader instance.
   *
   * @param name
   */
  public TELogHeader(final String name) {
    //Preconditions
    assert StringUtils.isNonEmptyString(name) : "name must be a non-empty string";

    this.name = name;
    headTELogEntry = null;
  }

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
   * Returns the name of this log hash chain.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Return the head of the log item hash chain.
   *
   * @return the head of the log item hash chain
   */
  public AbstractTELogEntry getHeadTELogEntry() {
    return headTELogEntry;
  }

  /**
   * Set the head of the log item hash chain.
   *
   * @param headTELogEntry the head of the log item hash chain
   */
  public void setHeadTELogEntry(final AbstractTELogEntry headTELogEntry) {
    //Preconditions
    assert headTELogEntry != null : "headTELogEntry must not be null";

    this.headTELogEntry = headTELogEntry;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 61 * hash + Objects.hashCode(this.name);
    return hash;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final TELogHeader other = (TELogHeader) obj;
    return Objects.equals(this.name, other.name);
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder()).append("[TELogHeader ").append(name).append(']').toString();
  }
}
