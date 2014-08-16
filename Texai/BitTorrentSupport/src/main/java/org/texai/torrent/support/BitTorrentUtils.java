/*
 * BitTorrentUtils.java
 *
 * Created on Feb 9, 2010, 5:05:38 PM
 *
 * Description: Provides bit torrent utilities.
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
package org.texai.torrent.support;

import java.util.Random;
import net.jcip.annotations.NotThreadSafe;

/** Provides bit torrent utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class BitTorrentUtils {

  /** the client id candidate symbols */
  private static final byte[] CANDIDATE_ID_SYMBOLS = new byte[62];

  static {
    for (int i = 0; i < 10; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('0' + i);
    }
    for (int i = 10; i < 36; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('a' + i - 10);
    }
    for (int i = 36; i < 62; ++i) {
      CANDIDATE_ID_SYMBOLS[i] = (byte) ('A' + i - 36);
    }
  }

  /** Prevents the instantiation of this utility class. */
  private  BitTorrentUtils() {
  }

  /** Returns whether this is a bit torrent handshake message.
   *
   * @param magic1 the first byte of the message
   * @return whether this is a bit torrent handshake message
   */
  public static boolean isBitTorrentHandshake(final int magic1) {
    return magic1 == BitTorrentConstants.BIT_TORRENT_HANDSHAKE_PROTOCOL;
  }

  /** Returns whether this is a bit torrent keep-alive message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @return whether this is a bit torrent keep-alive message
   */
  public static boolean isBitTorrentKeepAlive(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == 0;
  }

  /** Returns whether this is a bit torrent choke message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent choke message
   */
  public static boolean isBitTorrentChoke(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_CHOKE_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_CHOKE_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent unchoke message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent unchoke message
   */
  public static boolean isBitTorrentUnchoke(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent interested message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent interested message
   */
  public static boolean isBitTorrentInterested(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_INTERESTED_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_INTERESTED_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent not-interested message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent not-interested message
   */
  public static boolean isBitTorrentNotInterested(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_NOT_INTERESTED_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_NOT_INTERESTED_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent have message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent have message
   */
  public static boolean isBitTorrentHave(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent cancel message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent have message
   */
  public static boolean isBitTorrentCancel(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_CANCEL_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_CANCEL_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent request message.
   *
   * @param magic1 the first byte of the message
   * @param magic2 the second byte of the message
   * @param magic3 the third byte of the message
   * @param magic4 the fourth byte of the message
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent have message
   */
  public static boolean isBitTorrentRequest(
          final int magic1,
          final int magic2,
          final int magic3,
          final int magic4,
          final int magic5) {
    return magic1 == 0 &&
            magic2 == 0 &&
            magic3 == 0 &&
            magic4 == BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_LENGTH &&
            magic5 == BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent bitfield message.
   *
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent bitfield message
   */
  public static boolean isBitTorrentBitfield(final int magic5) {
    return magic5 == BitTorrentConstants.BIT_TORRENT_BITFIELD_MESSAGE_ID;
  }

  /** Returns whether this is a bit torrent piece message.
   *
   * @param magic5 the fifth byte of the message
   * @return whether this is a bit torrent piece message
   */
  public static boolean isBitTorrentPiece(final int magic5) {
    return magic5 == BitTorrentConstants.BIT_TORRENT_PIECE_MESSAGE_ID;
  }

  /** Generates random peer id bytes.
   *
   * @return random peer id bytes
   */
  public static byte[] generateRandomPeerIdBytes() {
    final byte[] idBytes = new byte[20];
    int index = 0;
    idBytes[index++] = '-';
    idBytes[index++] = 'S'; // SN indicates Snark
    idBytes[index++] = 'N';
    idBytes[index++] = '1';
    idBytes[index++] = '0';
    idBytes[index++] = '0';
    idBytes[index++] = '0';
    idBytes[index++] = '-';
    final Random random = new Random();
    while (index < 20) {
      idBytes[index++] = CANDIDATE_ID_SYMBOLS[random.nextInt(CANDIDATE_ID_SYMBOLS.length)];
    }

    return idBytes;
  }
}
