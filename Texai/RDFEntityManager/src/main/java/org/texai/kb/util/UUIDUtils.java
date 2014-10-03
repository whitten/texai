/*
 * UUIDUtils.java
 *
 * Created on Apr 20, 2010, 8:07:53 PM
 *
 * Description: Provides UUID utilities.
 *
 * Copyright (C) Apr 20, 2010, Stephen L. Reed.
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
package org.texai.kb.util;

import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;

/** Provides UUID utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class UUIDUtils {

  /** Prevents the instantiation of this utility class. */
  private UUIDUtils() {
  }

  /** Returns a UUID formed from the local name of the given URI.  When the URI has an embedded underscore character,
   * then the UUID is formed from the portion of the local name following the underscore.
   *
   * @param uri the given URI
   * @return a UUID formed from the local name of the given URI
   */
  public static UUID uriToUUID(final URI uri) {
    //Preconditions
    assert uri != null : "uri must not be null";

    final String localName = uri.getLocalName();
    final int index = localName.indexOf('_');
    if (index == -1) {
      return UUID.fromString(localName);
    } else {
      return UUID.fromString(localName.substring(index + 1));
    }
  }
}
