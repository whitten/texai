/*
 * MessageDispatcher.java
 *
 * Created on Apr 6, 2010, 2:02:40 PM
 *
 * Description: Defines the interface for dispatching messages in an Albus hierarchical control system.
 *
 * Copyright (C) Apr 6, 2010, Stephen L. Reed.
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
package org.texai.ahcsSupport;

/** Defines the interface for dispatching messages in an Albus hierarchical control system.
 *
 * @author reed
 */
public interface MessageDispatcher {

  /** Dispatches a message in an Albus hierarchical control system.
   *
   * @param message the Albus message
   */
  void dispatchMessage(final Message message);

}
