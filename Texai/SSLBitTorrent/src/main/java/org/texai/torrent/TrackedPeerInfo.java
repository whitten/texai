/*
 * TrackedPeerInfo - All public information concerning a peer. Copyright (C) 2003 Mark J.
 * Wielaard
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import org.texai.torrent.bencode.BDecoder;
import org.texai.torrent.bencode.BEValue;
import org.texai.torrent.bencode.InvalidBEncodingException;
import org.texai.util.TexaiException;

/** All public information concerning a peer. */
public final class TrackedPeerInfo implements Comparable<TrackedPeerInfo> {

  /** the peer id bytes */
  private byte[] peerIdBytes;
  /** the internet address */
  private final InetAddress inetAddress;
  /** the connection port */
  private final int port;
  /** the hash */
  private final int hash;

  /** Constructs a new TrackedPeerInfo instance.
   *
   * @param peerIdBytes the peer id bytes
   * @param inetAddress the internet address
   * @param port the connection port
   */
  public TrackedPeerInfo(
          final byte[] peerIdBytes,
          final InetAddress inetAddress,
          final int port) {
    this.peerIdBytes = peerIdBytes;
    this.inetAddress = inetAddress;
    this.port = port;

    hash = calculateHash();
  }

  /** Creates a TrackedPeerInfo from a BDecoder.
   *
   * @param bDecoder the BDecoder
   * @throws IOException when an input/output error occurs
   */
  public TrackedPeerInfo(final BDecoder bDecoder) throws IOException {
    this(bDecoder.bdecodeMap().getMap());
  }

  /** Creates a TrackedPeerInfo from a Map containing BEncoded peer id, ip and port.
   *
   * @param map the map containing BEncoded peer id, ip and port
   * @throws InvalidBEncodingException when the map is missing the internet address or connection port
   * @throws UnknownHostException when the host cannot be found
   */
  public TrackedPeerInfo(final Map<String, BEValue> map)
          throws InvalidBEncodingException, UnknownHostException {
    BEValue bevalue = map.get("peer id");
    if (bevalue == null) {
      throw new InvalidBEncodingException("peer id missing");
    }
    peerIdBytes = bevalue.getBytes();

    bevalue = map.get("ip");
    if (bevalue == null) {
      throw new InvalidBEncodingException("ip missing");
    }
    inetAddress = InetAddress.getByName(bevalue.getString());

    bevalue = map.get("port");
    if (bevalue == null) {
      throw new InvalidBEncodingException("port missing");
    }
    port = bevalue.getInt();

    hash = calculateHash();
  }

  /** Gets the peer id bytes.
   *
   * @return the peer id bytes
   */
  public byte[] getPeerIdBytes() {
    return peerIdBytes;
  }

  /** Sets the peer id bytes
   *
   * @param peerIdBytes the peer id bytes
   */
  public void setID(final byte[] peerIdBytes) {
    this.peerIdBytes = peerIdBytes;
  }

  /** Gets the internet address.
   *
   * @return the internet address
   */
  public InetAddress getInetAddress() {
    return inetAddress;
  }

  /** Gets the connection port.
   *
   * @return the connection port
   */
  public int getPort() {
    return port;
  }

  /** Calculates the hash for this peer.
   *
   * @return the hash for this peer
   */
  private int calculateHash() {
    return (inetAddress.hashCode()) ^ port;
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return hash;
  }

  /** Returns true if and only if this peerID and the given peerID have the
   * same 20 bytes as their IDs.
   *
   * @param pid the other peer id
   * @return whether peerID and the given peerID have the
   * same 20 bytes as their IDs
   */
  public boolean samePeerIdBytes(final TrackedPeerInfo pid) {
    boolean equal = true;
    for (int i = 0; equal && i < peerIdBytes.length; i++) {
      equal = peerIdBytes[i] == pid.peerIdBytes[i];
    }
    return equal;
  }

  /** Returns whether some other peed id is equal to this one.  If both peer id byte arrays are non-zero, then
   * they are used for the equality test, otherwise the internet address and port numbers are used.
   *
   * @param obj the other object
   * @return whether some other peed id is equal to this one
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof TrackedPeerInfo) {
      final TrackedPeerInfo pid = (TrackedPeerInfo) obj;
      boolean isEitherAllZero = false;
      final int peerIdBytes_len = peerIdBytes.length;
      boolean isAllZero = true;
      for (int i = 0; i < peerIdBytes_len; i++) {
        if (peerIdBytes[i] != 0) {
          isAllZero = false;
          break;
        }
      }
      if (isAllZero) {
        isEitherAllZero = true;
      } else {
        final byte[] pidPeerIdBytes = pid.peerIdBytes;
        final int pidPeerIdBytes_len = pidPeerIdBytes.length;
        isAllZero = true;
        for (int i = 0; i < pidPeerIdBytes_len; i++) {
          if (pidPeerIdBytes[i] != 0) {
            isAllZero = false;
            break;
          }
        }
        if (isAllZero) {
          isEitherAllZero = true;
        }
      }
      if (isEitherAllZero) {
        return port == pid.port && inetAddress.equals(pid.inetAddress);
      } else {
        return Arrays.equals(peerIdBytes, pid.peerIdBytes);
      }
    } else {
      return false;
    }
  }

  /** Compares port, address and id.
   *
   * @param pid the other peer id.
   * @return -1 if this peer id is less than the other one, 0 if they are equal, otherwise +1
   */
  @Override
  public int compareTo(final TrackedPeerInfo pid) {

    int result = port - pid.port;
    if (result != 0) {
      return result;
    }

    result = inetAddress.hashCode() - pid.inetAddress.hashCode();
    if (result != 0) {
      return result;
    }

    for (int i = 0; i < peerIdBytes.length; i++) {
      result = peerIdBytes[i] - pid.peerIdBytes[i];
      if (result != 0) {
        return result;
      }
    }

    return 0;
  }

  /** Returns a string representation for this object.
   *
   * @return the String "address:port"
   */
  @Override
  public String toString() {
    return inetAddress.getHostAddress() + ":" + port;
  }

  /** Returns an ID string representation for this object.
   *
   * @return an ID string representation for this object
   */
  public String toIDString() {
    try {
      return new String(peerIdBytes, "US-ASCII");
    } catch (UnsupportedEncodingException ex) {
      throw new  TexaiException(ex);
    }
  }

  /** Encodes an id as a hex encoded string.
   *
   * @param idBytes the id bytes
   * @return a hex encoded string
   */
  public static String hexEncode(final byte[] idBytes) {
    boolean leading_zeros = true;

    final StringBuffer stringBuffer = new StringBuffer(idBytes.length * 2);
    for (byte element : idBytes) {
      final int hexDigit = element & 0xFF;
      if (leading_zeros && hexDigit == 0) {
        continue;
      } else {
        leading_zeros = false;
      }

      if (hexDigit < 16) {
        stringBuffer.append('0');
      }
      stringBuffer.append(Integer.toHexString(hexDigit));
    }

    return stringBuffer.toString();
  }
}
