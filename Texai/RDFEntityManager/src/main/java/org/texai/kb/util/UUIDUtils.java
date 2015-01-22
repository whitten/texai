/*
 * UUIDUtils.java
 *
 * Created on Apr 20, 2010, 8:07:53 PM
 *
 * Description: Provides UUID utilities.
 *
 * Copyright (C) Apr 20, 2010, Stephen L. Reed.
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
