/*
 * BitTorrentBitFieldMessage.java
 *
 * Created on Feb 9, 2010, 10:42:39 PM
 *
 * Description: Provides a bit torrent bitfield message.
 *
 * Copyright (C) Feb 9, 2010 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.torrent.message;

import java.io.UnsupportedEncodingException;
import net.jcip.annotations.NotThreadSafe;
import org.texai.torrent.support.BitTorrentConstants;
import org.texai.util.ByteUtils;
import org.texai.util.TexaiException;

/** Provides a bit torrent bitfield message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentBitFieldMessage implements BitTorrentMessage {

  /** the bit field */
  private final byte[] bitField;
  /** the peer identification bytes */
  private final byte[] peerIdBytes;

  /** Constructs a new BitTorrentBitFieldMessage instance.
   *
   * @param bitField the bit field
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentBitFieldMessage(
          final byte[] bitField,
          final byte[] peerIdBytes) {
    //Preconditions
    assert bitField != null : "bitField must not be null";
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must have length 20";

    this.bitField = bitField;
    this.peerIdBytes = peerIdBytes;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[bit field, peer id: ");
    try {
      stringBuilder.append(new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Returns a detailed string representation of this object.
   *
   * @return a detailed string representation of this object
   */
  public String toDetailedString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[bit field, peer id: ");
    try {
      stringBuilder.append(new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    stringBuilder.append(" bitfield: ");
    stringBuilder.append(ByteUtils.toHex(bitField));
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Encodes the bit torrent message into a byte array.
   *
   * @return the byte array
   */
  @Override
  public byte[] encode() {
    final int bitField_len = bitField.length;
    final byte[] bytes = new byte[25 + bitField_len];
    final byte[] lengthBytes = ByteUtils.toBytes(21 + bitField_len);
    bytes[0] = lengthBytes[0];
    bytes[1] = lengthBytes[1];
    bytes[2] = lengthBytes[2];
    bytes[3] = lengthBytes[3];
    bytes[4] = BitTorrentConstants.BIT_TORRENT_BITFIELD_MESSAGE_ID;
    System.arraycopy(
            bitField,  // source
            0, // source offset
            bytes, // destination
            5, // destination offset
            bitField_len); // length
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            bitField_len + 5, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the bit torrent message from the given byte array.
   *
   * @param bytes the given byte array
   * @return the bit torrent message
   */
  public static BitTorrentBitFieldMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length > 25 : "bytes must be length greater than 25";

    final int bitField_len = bytes.length - 25;
    final byte[] bitField = new byte[bitField_len];
    System.arraycopy(
            bytes,  // source
            5, // source offset
            bitField, // destination
            0, // destination offset
            bitField_len); // length
    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            bitField_len + 5, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentBitFieldMessage(bitField, peerIdBytes);
  }

  /** Gets the bit field.
   *
   * @return the bitField
   */
  public byte[] getBitField() {
    return bitField;
  }

  /** Gets the peer identification bytes.
   *
   * @return the peer identification bytes
   */
  @Override
  public byte[] getPeerIdBytes() {
    return peerIdBytes;
  }
}
