/*
 * BitTorrentHandshakeMessage.java
 *
 * Created on Feb 9, 2010, 10:39:41 PM
 *
 * Description: Provides a bit torrent handshake message.
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

/** Provides a bit torrent handshake message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentHandshakeMessage implements BitTorrentMessage {

  /** the torrent info hash */
  private final byte[] infoHash;
  /** the peer id bytes */
  private final byte[] peerIdBytes;

  /** Constructs a new BitTorrentHandshakeMessage instance.
   *
   * @param infoHash the torrent info hash
   * @param peerIdBytes the peer id bytes
   */
  public BitTorrentHandshakeMessage(
          final byte[] infoHash,
          final byte[] peerIdBytes) {
    //Preconditions
    assert infoHash != null : "infoHash must not be null";
    assert infoHash.length == 20 : "infoHash must be length 20";
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must be length 20";

    this.infoHash = infoHash;
    this.peerIdBytes = peerIdBytes;
  }

  /** Gets the torrent info hash.
   *
   * @return the torrent info hash
   */
  public byte[] getInfoHash() {
    return infoHash;
  }

  /** Gets the peer identification bytes.
   *
   * @return the peer identification bytes
   */
  @Override
  public byte[] getPeerIdBytes() {
    return peerIdBytes;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[handshake, peer id: ");
    try {
      stringBuilder.append(new String(peerIdBytes, "US-ASCII"));
    } catch (UnsupportedEncodingException ex) {
      throw new TexaiException(ex);
    }
    stringBuilder.append(", info-hash: ");
    stringBuilder.append(ByteUtils.toHex(infoHash));
    stringBuilder.append(']');
    return stringBuilder.toString();
  }

  /** Encodes the bit torrent message into a byte array.
   *
   * @return the byte array
   */
  @Override
  public byte[] encode() {
    final byte[] bytes = new byte[68];
    bytes[0] = BitTorrentConstants.BIT_TORRENT_HANDSHAKE_PROTOCOL;
    bytes[1] = 'B';
    bytes[2] = 'i';
    bytes[3] = 't';
    bytes[4] = 'T';
    bytes[5] = 'o';
    bytes[6] = 'r';
    bytes[7] = 'r';
    bytes[8] = 'e';
    bytes[9] = 'n';
    bytes[10] = 't';
    bytes[11] = ' ';
    bytes[12] = 'p';
    bytes[13] = 'r';
    bytes[14] = 'o';
    bytes[15] = 't';
    bytes[16] = 'o';
    bytes[17] = 'c';
    bytes[18] = 'o';
    bytes[19] = 'l';

    bytes[20] = 0;
    bytes[21] = 0;
    bytes[22] = 0;
    bytes[23] = 0;
    bytes[24] = 0;
    bytes[25] = 0;
    bytes[26] = 0;
    bytes[27] = 0;

    System.arraycopy(
            infoHash,  // source
            0, // source offset
            bytes, // destination
            28, // destination offset
            20); // length
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            48, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentHandshakeMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentHandshakeMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length == 68 : "bytes must be length 68";

    final byte[] infoHash = new byte[20];
    System.arraycopy(
            bytes,  // source
            28, // source offset
            infoHash, // destination
            0, // destination offset
            20); // length
    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            48, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentHandshakeMessage(infoHash, peerIdBytes);
  }

}
