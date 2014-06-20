/*
 * BitTorrentPieceMessage.java
 *
 * Created on Feb 9, 2010, 10:46:46 PM
 *
 * Description: Provides a bit torrent piece message.
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

/** Provides a bit torrent piece message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentPieceMessage implements BitTorrentMessage {

  /** the piece index */
  private final int pieceIndex;
  /** the offset within the piece where the block begins */
  private final int offset;
  /** the chunk bytes */
  private final byte[] chunkBytes;
  /** the peer id bytes */
  private final byte[] peerIdBytes;


  /** Constructs a new BitTorrentPieceMessage instance.
   *
   * @param pieceIndex the piece index
   * @param offset the offset within the piece where the chunk begins
   * @param chunkBytes the chunk bytes
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentPieceMessage(
          final int pieceIndex,
          final int offset,
          final byte[] chunkBytes,
          final byte[] peerIdBytes) {
    //Preconditions
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert offset >= 0 : "offset must not be negative";
    assert chunkBytes != null : "chunkBytes must not be null";
    assert chunkBytes.length > 0 : "chunkBytes must not be empty";
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must be length 20";

    this.pieceIndex = pieceIndex;
    this.offset = offset;
    this.chunkBytes = chunkBytes;
    this.peerIdBytes = peerIdBytes;
  }

  /** Encodes the bit torrent message into a byte array.
   *
   * @return the byte array
   */
  @Override
  public byte[] encode() {
    final int dataBlock_len = chunkBytes.length;
    final byte[] bytes = new byte[33 + dataBlock_len];

    // message length field does not include the length of its own four bytes
    final byte[] lengthBytes = ByteUtils.toBytes(29 + dataBlock_len);
    bytes[0] = lengthBytes[0];
    bytes[1] = lengthBytes[1];
    bytes[2] = lengthBytes[2];
    bytes[3] = lengthBytes[3];

    bytes[4] = BitTorrentConstants.BIT_TORRENT_PIECE_MESSAGE_ID;

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

    System.arraycopy(
            chunkBytes,  // source
            0, // source offset
            bytes, // destination
            13, // destination offset
           dataBlock_len); // length
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            dataBlock_len + 13, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentPieceMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentPieceMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length > 33 : "bytes must be length greater than 33";

    final int dataBlock_len = bytes.length - 33;
    final byte[] dataBlock = new byte[dataBlock_len];
    System.arraycopy(
            bytes,  // source
            13, // source offset
            dataBlock, // destination
            0, // destination offset
           dataBlock_len); // length
    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            dataBlock_len + 13, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentPieceMessage(
          ByteUtils.byteArrayToInt(bytes, 5), // pieceIndex
          ByteUtils.byteArrayToInt(bytes, 9), // offset
          dataBlock,
          peerIdBytes);
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[piece: ");
    stringBuilder.append(pieceIndex);
    stringBuilder.append(", offset: ");
    stringBuilder.append(offset);
    stringBuilder.append(", length: ");
    stringBuilder.append(chunkBytes.length);
    stringBuilder.append(", peer id: ");
    try {
      stringBuilder.append(new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Gets the piece index.
   *
   * @return the piece index
   */
  public int getPieceIndex() {
    return pieceIndex;
  }

  /** Gets the offset within the piece where the chunk begins.
   *
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /** Gets the chunk bytes.
   *
   * @return the chunk bytes
   */
  public byte[] getChunkBytes() {
    return chunkBytes;
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
