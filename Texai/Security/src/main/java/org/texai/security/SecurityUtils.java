/*
 * SecurityUtils.java
 *
 * Created on Jan 25, 2010, 8:38:30 AM
 *
 * Description: Provides security utilities.
 *
 * Copyright (C) Jan 25, 2010 reed.
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
package org.texai.security;

import net.jcip.annotations.NotThreadSafe;
import org.texai.util.TexaiException;

/** Provides security utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class SecurityUtils {

  /** Prevents the instantiation of this static methods class. */
  private SecurityUtils() {
  }

  /** Returns whether a security manager is installed.
   *
   * @return whether a security manager is installed
   */
  public static boolean isSecurityManagerInstalled() {
    return System.getSecurityManager() != null;
  }

  /** Installs the profiling security manager. */
  public static void installProfilingSecurityManager() {
    //Preconditions
    if (isSecurityManagerInstalled()) {
      throw new TexaiException("security manager: " + System.getSecurityManager() +
              " (" + System.getSecurityManager().getClass().getName() + ") is already installed");
    }

    System.setSecurityManager(new ProfilingSecurityManager());
  }
}
