/*
 * UUIDGenerator.java
 *
 * Created on April 19, 2007, 2:09 PM
 *
 * Description: Generates a UUID.
 *
 * Copyright (C) 2007 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.texai.util;

import java.util.UUID;
import org.apache.log4j.Logger;

/** Generates a UUID.
 *
 * @author reed
 */
public final class UUIDGenerator {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(UUIDGenerator.class);

  /** Creates a new instance of UUIDGenerator. */
  public UUIDGenerator() {
    super();
  }

  /** Generates a UUID. */
  public void generateUUID() {
    final UUID uuid = UUID.randomUUID();
    LOGGER.info("generated UUID: " + uuid);
  }

  /** Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final UUIDGenerator uuidGenerator = new UUIDGenerator();
    uuidGenerator.generateUUID();
  }
}
