/*
 * BEValue - Holds different types that a bencoded byte array can represent.
 * Copyright (C) 2003 Mark J. Wielaard
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */
package org.texai.torrent.bencode;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/** Holds different types that a bencoded byte array can represent, which is a byte[], Number, List or Map (String --> BEValue).
 *
 * You need to call the correct get method to get the correct java type object. If the  BEValue wasn't actually
 * of the requested type you will get a InvalidBEncodingException.
 *
 * @author Mark Wielaard (mark@klomp.org)
 */
public class BEValue {

  /** the value */
  private final Object value;

  /** Constructs a new BEValue instance.
   *
   * @param value the byte array value
   */
  public BEValue(final byte[] value) {
    this.value = value;
  }

  /** Constructs a new BEValue instance.
   *
   * @param value the number value
   */
  public BEValue(final Number value) {
    this.value = value;
  }

  /** Constructs a new BEValue instance.
   *
   * @param value the BEValue list value
   */
  public BEValue(final List<BEValue> value) {
    this.value = value;
  }

  /** Constructs a new BEValue instance.
   *
   * @param value the BEValue map value
   */
  public BEValue(final Map<String, BEValue> value) {
    this.value = value;
  }

  /** Gets the value.
   *
   * @return the value
   */
  public Object getValue() {
    return value;
  }

  /** Returns this BEValue as a String. This operation only succeeds when the
   * BEValue is a byte[], otherwise it will throw a InvalidBEncodingException.
   * The byte[] will be interpreted as UTF-8 encoded characters.
   *
   * @return this BEValue as a String
   * @throws InvalidBEncodingException when the BEValue is not a byte[]
   */
  public String getString() throws InvalidBEncodingException {
    try {
      return new String(getBytes(), "UTF-8");
    } catch (ClassCastException cce) {
      throw new InvalidBEncodingException(cce.toString());    // NOPMD
    } catch (UnsupportedEncodingException uee) {
      throw new InternalError(uee.toString());    // NOPMD
    }
  }

  /** Returns this BEValue as a byte[]. This operation only succeeds when the
   * BEValue is actually a byte[], otherwise it will throw a
   * InvalidBEncodingException.
   *
   * @return this BEValue as a byte[]
   * @throws InvalidBEncodingException when the BEValue is not a byte[]
   */
  public byte[] getBytes() throws InvalidBEncodingException {
    try {
      return (byte[]) value;
    } catch (ClassCastException cce) {
      throw new InvalidBEncodingException(cce.toString());    // NOPMD
    }
  }

  /** Returns this BEValue as a Number. This operation only succeeds when the
   * BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException.
   *
   * @return this BEValue as a Number
   * @throws InvalidBEncodingException  when the BEValue is not a number
   */
  public Number getNumber() throws InvalidBEncodingException {
    try {
      return (Number) value;
    } catch (ClassCastException cce) {
      throw new InvalidBEncodingException(cce.toString());    // NOPMD
    }
  }

  /** Returns this BEValue as int. This operation only succeeds when the
   * BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException. The returned int is the result of
   * <code>Number.intValue()</code>.
   *
   * @return this BEValue as int
   * @throws InvalidBEncodingException  when the BEValue is not a number
   */
  public int getInt() throws InvalidBEncodingException {
    return getNumber().intValue();
  }

  /** Returns this BEValue as long. This operation only succeeds when the
   * BEValue is actually a Number, otherwise it will throw a
   * InvalidBEncodingException. The returned long is the result of
   * <code>Number.longValue()</code>.
   *
   * @return this BEValue as long
   * @throws InvalidBEncodingException  when the BEValue is not a number
   */
  public long getLong() throws InvalidBEncodingException {
    return getNumber().longValue();
  }

  /**
   * Returns this BEValue as a List of BEValues. This operation only succeeds
   * when the BEValue is actually a List, otherwise it will throw a
   * InvalidBEncodingException.
   *
   * @return this BEValue as a List of BEValues
   * @throws InvalidBEncodingException  when the BEValue is not a list of BEValues
   */
  @SuppressWarnings("unchecked")
  public List<BEValue> getList() throws InvalidBEncodingException {
    try {
      return (List<BEValue>) value;
    } catch (ClassCastException cce) {
      final String errorMessage = value.toString() + "(" + value.getClass().getName() + ") cannot be cast to List<BEValue>";
      throw new InvalidBEncodingException(cce.getMessage() + "\n" + errorMessage);    // NOPMD
    }
  }

  /** Returns this BEValue as a Map of BEValue keys and BEValue values. This
   * operation only succeeds when the BEValue is actually a Map, otherwise it
   * will throw a InvalidBEncodingException.
   *
   * @return this BEValue as a Map of BEValue keys and BEValue values
   * @throws InvalidBEncodingException  when value is not a map
   */
  @SuppressWarnings("unchecked")
  public Map<String, BEValue> getMap() throws InvalidBEncodingException {
    try {
      return (Map<String, BEValue>) value;
    } catch (ClassCastException cce) {
      throw new InvalidBEncodingException(cce.toString());    // NOPMD
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    String valueString;
    if (value instanceof byte[]) {
      final byte[] byteArray = (byte[]) value;
      // XXX - Stupid heuristic...
      if (byteArray.length <= 12) {
        valueString = new String(byteArray);
      } else {
        valueString = "bytes, length:" + byteArray.length;
      }
    } else {
      valueString = value.toString();
    }

    return "BEValue[" + valueString + "]";
  }
}
