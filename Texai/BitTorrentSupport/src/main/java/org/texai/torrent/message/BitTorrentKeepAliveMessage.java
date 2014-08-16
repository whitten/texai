/*
 * BitTorrentKeepAliveMessage.java
 *
 * Created on Feb 9, 2010, 10:40:09 PM
 *
 * Description: Provides a bit torrent keep-alive message.
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
import org.texai.util.TexaiException;

/** Provides a bit torrent keep-alive message. See http://wiki.theory.org/BitTorrentSpecification.
 *
 * @author reed
 */
@NotThreadSafe
public class BitTorrentKeepAliveMessage implements BitTorrentMessage {

  /** the peer id bytes */
  private final byte[] peerIdBytes;

  /** Constructs a new BitTorrentKeepAliveMessage instance.
   *
   * @param peerIdBytes the peer identification bytes
   */
  public BitTorrentKeepAliveMessage(final byte[] peerIdBytes) {
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
    stringBuilder.append("[keep-alive, peer id: ");
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
    final byte[] bytes = new byte[24];
    System.arraycopy(
            peerIdBytes,  // source
            0, // source offset
            bytes, // destination
            4, // destination offset
            20); // length
    return bytes;
  }

  /** Decodes the given bytes into a new BitTorrentKeepAliveMessage instance.
   *
   * @param bytes the given bytes
   * @return a new instance
   */
  public static BitTorrentKeepAliveMessage decode(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";
    assert bytes.length == 24 : "bytes must be length 24";

    final byte[] peerIdBytes = new byte[20];
    System.arraycopy(
            bytes,  // source
            4, // source offset
            peerIdBytes, // destination
            0, // destination offset
            20); // length
    return new BitTorrentKeepAliveMessage(peerIdBytes);
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
