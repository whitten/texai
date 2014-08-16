/*
 * BitTorrentUnchokeMessage.java
 *
 * Created on Feb 9, 2010, 10:40:47 PM
 *
 * Description: Provides a bit torrent unchoke message.
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
import org.texai.util.TexaiException;

/** Provides a bit torrent unchoke message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentUnchokeMessage implements BitTorrentMessage {

  /** the peer id bytes */
  private final byte[] peerIdBytes;

  /** Constructs a new BitTorrentUnchokeMessage instance.
   *
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentUnchokeMessage(final byte[] peerIdBytes) {
    //Preconditions
    assert peerIdBytes != null : "peerIdBytes must not be null";
    assert peerIdBytes.length == 20 : "peerIdBytes must be length 20";

    this.peerIdBytes = peerIdBytes;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[unchoke, peer id: ");
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
    final byte[] bytes = new byte[25];
    bytes[3] = BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_LENGTH;
    bytes[4] = BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_ID;
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            5, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentUnchokeMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentUnchokeMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length == 25 : "bytes must be length 25";

    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            5, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentUnchokeMessage(peerIdBytes);
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
