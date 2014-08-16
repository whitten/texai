/*
 * InvalidBEncodingException - Thrown when a bencoded stream is corrupted.
 * Copyright (C) 2003 Mark J. Wielaard
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
package org.texai.torrent.bencode;

import java.io.IOException;

/** Exception thrown when a bencoded stream is corrupted.
 *
 * @author Mark Wielaard (mark@klomp.org)
 */
public class InvalidBEncodingException extends IOException {
  /** the default serial version UID */
  private static final long serialVersionUID = 1L;

  /** Constructs a new InvalidBEncodingException instance.
   *
   * @param message the exception message
   */
  public InvalidBEncodingException(final String message) {
    super(message);
  }
}
