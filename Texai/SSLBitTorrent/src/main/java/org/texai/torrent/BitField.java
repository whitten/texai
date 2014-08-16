/*
 * BitField - Container of a byte array representing set and unset bits.
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
package org.texai.torrent;

/** Container of a byte array representing set and unset bits. */
public final class BitField {

  /** the bit field */
  private final byte[] fieldBytes;
  /** the number of bits */
  private final int size;

  /**
   * Creates a new BitField that represents <code>size</code> unset bits.
   *
   * @param size the number of bits
   */
  public BitField(final int size) {
    this.size = size;
    fieldBytes = new byte[((size - 1) / 8) + 1];
  }

  /** Creates a new BitField that represents <code>size</code> bits as set by
   * the given byte array. This will make a copy of the array. Extra bytes
   * will be ignored.
   *
   * @param fieldBytes the bit field
   * @param size the number of bits
   */
  public BitField(final byte[] fieldBytes, final int size) {
    this.size = size;
    final int arraysize = ((size - 1) / 8) + 1;
    this.fieldBytes = new byte[arraysize];

    // XXX - More correct would be to check that unused bits are
    // cleared or clear them explicitly ourselves.
    System.arraycopy(fieldBytes, 0, this.fieldBytes, 0, arraysize);
  }

  /** This returns the actual byte array used. Changes to this array effect
   * this BitField. Note that some bits at the end of the byte array are
   * supposed to be always unset if they represent bits bigger then the size
   * of the bitfield.
   *
   * @return  the actual byte array used
   */
  public byte[] getFieldBytes() {
    return fieldBytes;
  }

  /** Return the size of the BitField. The returned value is one bigger then
   * the last valid bit number (since bit numbers are counted from zero).
   *
   * @return the number of bits
   */
  public int size() {
    return size;
  }

  /** Sets the given bit to true.
   *
   * @param bit the zero-indexed bit to set
   */
  public void set(final int bit) {
    //Preconditions
    if (bit < 0 || bit >= size) {
      throw new IndexOutOfBoundsException(Integer.toString(bit));
    }
    final int index = bit / 8;
    final int mask = 128 >> (bit % 8);
    fieldBytes[index] |= mask;
  }

  /** Return true if the bit is set or false if it is not.
   *
   * @param bit the zero-indexed bit to set
   * @return whether the bit is set
   */
  public boolean get(final int bit) {
    //Preconditions
    if (bit < 0 || bit >= size) {
      throw new IndexOutOfBoundsException(Integer.toString(bit));
    }

    final int index = bit / 8;
    final int mask = 128 >> (bit % 8);
    return (fieldBytes[index] & mask) != 0;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    // Not very efficient
    final StringBuffer stringBuffer = new StringBuffer("BitField[");
    for (int i = 0; i < size; i++) {
      if (get(i)) {
        stringBuffer.append(' ');
        stringBuffer.append(i);
      }
    }
    stringBuffer.append(" ]");

    return stringBuffer.toString();
  }

  /** Returns a human-readable representation.
   *
   * @return a human-readable representation
   */
  public String getHumanReadable() {
    final StringBuffer stringBuffer = new StringBuffer();
    for (int i = 0; i < size; i++) {
      if (get(i)) {
        stringBuffer.append('+');
      } else {
        stringBuffer.append('-');
      }
    }
    return stringBuffer.toString();
  }
}
