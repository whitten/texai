/*
 * TexaiException.java
 *
 * Created on October 18, 2006, 10:03 AM
 *
 * Description: Provides an unchecked exception with which to wrap the various checked exceptions that
 * can be thrown by called methods.
 *
 * Copyright (C) 2006 Stephen L. Reed.
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

/**
 *
 * @author reed
 */
public final class TexaiException extends RuntimeException {

  /** the default serial version UID */
  private static final long serialVersionUID = 1L;

  /** Creates a new instance of <code>TexaiException</code> without detail message.
   */
  public TexaiException() {
  }


  /** Constructs an instance of <code>TexaiException</code> with the specified detail message.
   *
   * @param message the detail message.
   */
  public TexaiException(final String message) {
    super(message);
  }

  /** Constructs an instance of <code>TexaiException</code> with the specified cause.
   *
   * @param throwable the wrapped exception
   */
  public TexaiException(final Throwable throwable) {
    super(throwable);
  }

  /** Constructs an instance of <code>TexaiException</code> with the specified cause.
   *
   * @param message the detail message.
   * @param throwable the wrapped exception
   */
  public TexaiException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
