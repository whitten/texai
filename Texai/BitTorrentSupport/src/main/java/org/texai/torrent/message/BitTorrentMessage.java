/*
 * BitTorrentChokeMessage.java
 *
 * Created on Feb 9, 2010, 10:40:32 PM
 *
 * Description: Defines a bit torrent message.
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

/** Defines a bit torrent message.
 *
 * @author reed
 */
public interface BitTorrentMessage {

  /** Encodes the bit torrent message into a byte array.
   *
   * @return the byte array
   */
  byte[] encode();

  /** Gets the peer identification bytes.
   *
   * @return the peer identification bytes
   */
  byte[] getPeerIdBytes();

}
