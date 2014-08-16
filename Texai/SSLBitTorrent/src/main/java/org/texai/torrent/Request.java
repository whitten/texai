/*
 * Request - Holds all information needed for a (partial) piece request.
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

/** Holds all information needed for a partial piece request. */
public final class Request {
  /** the piece index requested */
  private final int pieceIndex;
  /** the byte array where response should be stored */
  private final byte[] pieceBuffer;
  /** the offset into the array */
  private final int offset;
  /** the number of bytes requested */
  private final int length;

  /** Creates a new Request.
   *
   * @param pieceIndex the piece index requested
   * @param pieceBuffer the byte array where response should be stored
   * @param offset the offset into the piece
   * @param length the number of bytes requested
   */
  Request(
          final int pieceIndex,
          final byte[] pieceBuffer,
          final int offset,
          final int length) {
    //preconditions
    if (pieceIndex < 0 || offset < 0 || length <= 0 || offset + length > pieceBuffer.length) {
      throw new IndexOutOfBoundsException("Illegal Request " + toString());
    }

    this.pieceIndex = pieceIndex;
    this.pieceBuffer = pieceBuffer;
    this.offset = offset;
    this.length = length;

  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return pieceIndex ^ offset ^ length;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof Request) {
      final Request request = (Request) obj;
      return request.pieceIndex == pieceIndex && request.offset == offset && request.length == length;
    }

    return false;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "(" + pieceIndex + "," + offset + "," + length + ")";
  }

  /** Gets the piece index.
   *
   * @return the piece index
   */
  public int getPieceIndex() {
    return pieceIndex;
  }

  /** Gets the piece buffer.
   *
   * @return the piece buffer
   */
  public byte[] getPieceBuffer() {
    return pieceBuffer;
  }

  /** Gets the offset into the piece.
   *
   * @return the offset into the piece
   */
  public int getOffset() {
    return offset;
  }

  /** Gets the number of bytes requested.
   *
   * @return the number of bytes requested
   */
  public int getLength() {
    return length;
  }
}
