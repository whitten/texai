/*
 * NetworkConstants.java
 *
 * Created on Feb 3, 2010, 3:52:55 PM
 *
 * Description: Provides network constants.
 *
 * Copyright (C) Feb 3, 2010 reed.
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
package org.texai.network.netty;

import net.jcip.annotations.ThreadSafe;

/**  Provides network constants.
 *
 * @author reed
 */
@ThreadSafe
public final class NetworkConstants {

  /** the object serialization protocol identification byte */
  public static final byte OBJECT_SERIALIZATION_PROTOCOL = 1;

  /** Prevents the instantiation of this utility class. */
  private NetworkConstants() {
  }
}
