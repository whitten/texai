/*
 * BitTorrentRequestMessage.java
 *
 * Created on Feb 9, 2010, 10:43:17 PM
 *
 * Description: Provides a bit torrent request message.
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

/** Provides a bit torrent request message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentRequestMessage implements BitTorrentMessage {

  /** the piece index */
  private final int pieceIndex;
  /** the offset within the piece where the requested block begins */
  private final int offset;
  /** the length of the requested block */
  private final int length;
  /** the peer id bytes */
  private final byte[] peerIdBytes;


  /** Constructs a new BitTorrentRequestMessage instance.
   * @param pieceIndex the piece index
   * @param offset the offset within the piece where the requested block begins
   * @param length the length of the requested block
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentRequestMessage(
          final int pieceIndex,
          final int offset,
          final int length,
          final byte[] peerIdBytes) {
    //Preconditions
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert offset >= 0 : "offset must not be negative";
    assert length > 0 : "length must be positive";
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must be length 20";

    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.length = length;
    this.peerIdBytes = peerIdBytes;

  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[request piece: ");
    stringBuilder.append(pieceIndex);
    stringBuilder.append(", offset: ");
    stringBuilder.append(offset);
    stringBuilder.append(", length: ");
    stringBuilder.append(length);
    stringBuilder.append(", peer id: ");
    try {
      stringBuilder.append(new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Encodes the bit torrent message into a byte array.
   *
   * @return the byte array
   */
  @Override
  public byte[] encode() {
    final byte[] bytes = new byte[37];
    bytes[3] = BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_LENGTH;
    bytes[4] = BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_ID;

    final byte[] pieceIndexBytes = ByteUtils.toBytes(pieceIndex);
    bytes[5] = pieceIndexBytes[0];
    bytes[6] = pieceIndexBytes[1];
    bytes[7] = pieceIndexBytes[2];
    bytes[8] = pieceIndexBytes[3];

    final byte[] offsetBytes = ByteUtils.toBytes(offset);
    bytes[9] = offsetBytes[0];
    bytes[10] = offsetBytes[1];
    bytes[11] = offsetBytes[2];
    bytes[12] = offsetBytes[3];

    final byte[] lengthBytes = ByteUtils.toBytes(length);
    bytes[13] = lengthBytes[0];
    bytes[14] = lengthBytes[1];
    bytes[15] = lengthBytes[2];
    bytes[16] = lengthBytes[3];

    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            17, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentRequestMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentRequestMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length == 37 : "bytes must be length 37";

    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            17, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentRequestMessage(
          ByteUtils.byteArrayToInt(bytes, 5),
          ByteUtils.byteArrayToInt(bytes, 9),
          ByteUtils.byteArrayToInt(bytes, 13),
          peerIdBytes);
  }

  /** Gets the piece index.
   *
   * @return the piece index
   */
  public int getPieceIndex() {
    return pieceIndex;
  }

  /** Gets the offset within the piece where the requested block begins.
   *
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /** Gets the length of the requested block.
   *
   * @return the length
   */
  public int getLength() {
    return length;
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
