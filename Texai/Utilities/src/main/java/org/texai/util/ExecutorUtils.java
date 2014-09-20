/*
 * ExecutorUtils.java
 *
 * Created on Mar 30, 2010, 10:28:46 AM
 *
 * Description: Provides executor utilities.
 *
 * Copyright (C) Mar 30, 2010, Stephen L. Reed.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.jcip.annotations.NotThreadSafe;

/** Provides executor utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class ExecutorUtils {

  // the executor service
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

  /** Prevents the instantiation of this utility class. */
  private ExecutorUtils() {

  }
  /** Gets the executor service.
   *
   * @return the executorService
   */
  public static ExecutorService getExecutorService() {
    return EXECUTOR_SERVICE;
  }

}
