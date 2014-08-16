/*
 * BitTorrentConstants.java
 *
 * Created on Feb 9, 2010, 5:08:36 PM
 *
 * Description: Provides bit torrent constants.
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

import net.jcip.annotations.Immutable;

/** Provides bit torrent constants.
 *
 * @author reed
 */
@Immutable
public final class BitTorrentConstants {

  /** the bit torrent handshake protocol identification byte */
  public static final byte BIT_TORRENT_HANDSHAKE_PROTOCOL = 19;
  /** the bit torrent choke message id byte */
  public static final byte BIT_TORRENT_CHOKE_MESSAGE_ID = 0;
  /** the bit torrent unchoke message id byte */
  public static final byte BIT_TORRENT_UNCHOKE_MESSAGE_ID = 1;
  /** the bit torrent interested message id byte */
  public static final byte BIT_TORRENT_INTERESTED_MESSAGE_ID = 2;
  /** the bit torrent not-interested message id byte */
  public static final byte BIT_TORRENT_NOT_INTERESTED_MESSAGE_ID = 3;
  /** the bit torrent have message id byte */
  public static final byte BIT_TORRENT_HAVE_MESSAGE_ID = 4;
  /** the bit torrent bitfield message id byte */
  public static final byte BIT_TORRENT_BITFIELD_MESSAGE_ID = 5;
  /** the bit torrent request message id byte */
  public static final byte BIT_TORRENT_REQUEST_MESSAGE_ID = 6;
  /** the bit torrent piece message id byte */
  public static final byte BIT_TORRENT_PIECE_MESSAGE_ID = 7;
  /** the bit torrent cancel message id byte */
  public static final byte BIT_TORRENT_CANCEL_MESSAGE_ID = 7;
  /** the bit torrent choke message length */
  public static final byte BIT_TORRENT_CHOKE_MESSAGE_LENGTH = 21;
  /** the bit torrent unchoke message length */
  public static final byte BIT_TORRENT_UNCHOKE_MESSAGE_LENGTH = 21;
  /** the bit torrent interested message length */
  public static final byte BIT_TORRENT_INTERESTED_MESSAGE_LENGTH = 21;
  /** the bit torrent not-interested message length */
  public static final byte BIT_TORRENT_NOT_INTERESTED_MESSAGE_LENGTH = 21;
  /** the bit torrent have message length */
  public static final byte BIT_TORRENT_HAVE_MESSAGE_LENGTH = 25;
  /** the bit torrent request message length */
  public static final byte BIT_TORRENT_REQUEST_MESSAGE_LENGTH = 33;
  /** the bit torrent cancel message length */
  public static final byte BIT_TORRENT_CANCEL_MESSAGE_LENGTH = 33;

  /** Prevents the instantiation of this utility class. */
  private BitTorrentConstants() {
  }
}
