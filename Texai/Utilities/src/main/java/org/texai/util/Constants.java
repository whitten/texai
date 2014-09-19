/*
 * Constants.java
 *
 * Created on Jan 15, 2010, 8:15:30 AM
 *
 * Description: Provides utility constants.
 *
 * Copyright (C) Jan 15, 2010 reed.
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
package org.texai.util;

/** Provides utility constants.
 *
 * @author reed
 */
public final class Constants {

  // but will eventually get assigned by IANA
  public static final int HTTP_SERVER_PORT = 53110;
  // the lowest IANA Dynamic and/or Private Port
  public static final int LOWEST_DYNAMIC_OR_PRIVATE_PORT = 49152;
  // the highest IANA Dynamic and/or Private Port
  public static final int HIGHEST_DYNAMIC_OR_PRIVATE_PORT = 65535;


  /** This class is never instantiated. */
  private Constants() {
  }
}
