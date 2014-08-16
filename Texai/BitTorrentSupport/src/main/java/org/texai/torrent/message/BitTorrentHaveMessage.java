/*
 * BitTorrentHaveMessage.java
 *
 * Created on Feb 9, 2010, 10:42:22 PM
 *
 * Description: Provides a bit torrent have message.
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

/** Provides a bit torrent have message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentHaveMessage implements BitTorrentMessage {

  /** the piece index */
  private final int pieceIndex;
  /** the peer id bytes */
  private final byte[] peerIdBytes;

  /** Constructs a new BitTorrentHaveMessage instance.
   *
   * @param pieceIndex the piece index
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentHaveMessage(
          final int pieceIndex,
          final byte[] peerIdBytes) {
    //Preconditions
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must be length 20";

    this.pieceIndex = pieceIndex;
    this.peerIdBytes = peerIdBytes;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[have ");
    stringBuilder.append(pieceIndex);
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
    final byte[] bytes = new byte[29];
    final byte[] pieceNbrBytes = ByteUtils.toBytes(pieceIndex);
    bytes[3] = BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_LENGTH;
    bytes[4] = BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_ID;
    System.arraycopy(
            pieceNbrBytes,  // source
            0, // source offset
            bytes, // destination
            5, // destination offset
            4); // length
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            9, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentHaveMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentHaveMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length == 29 : "bytes must be length 29";

    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            9, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentHaveMessage(ByteUtils.byteArrayToInt(bytes, 5), peerIdBytes);
  }

  /** Gets the piece index.
   *
   * @return the piece index
   */
  public int getPieceIndex() {
    return pieceIndex;
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
