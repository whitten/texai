/*
 * AlbusHCSMessageHandlerFactory.java
 *
 * Created on Feb 9, 2010, 11:27:59 AM
 *
 * Description: Provides an Albus hierarchical control system message handler factory.
 *
 * Copyright (C) Feb 9, 2010 reed.
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
package org.texai.ahcs;

import net.jcip.annotations.ThreadSafe;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;

/** Provides an Albus hierarchical control system message handler factory, that returns the singleton handler.
 *
 * @author reed
 */
@ThreadSafe
public class AlbusHCSMessageHandlerFactory extends AbstractAlbusHCSMessageHandlerFactory {

  /** the Albus hierarchical control system message handler */
  private final AbstractAlbusHCSMessageHandler albusHCSMessageHandler;

  /** Constructs a new AlbusHCSMessageHandlerFactory instance.
   *
   * @param albusHCSMessageHandler the Albus hierarchical control system message handler
   */
  public AlbusHCSMessageHandlerFactory(final AbstractAlbusHCSMessageHandler albusHCSMessageHandler) {
    //Preconditions
    assert albusHCSMessageHandler != null : "albusHCSMessageHandler must not be null";

    this.albusHCSMessageHandler = albusHCSMessageHandler;
  }

  /** Gets the channel handler.
   *
   * @return the channel handler
   */
  @Override
  public AbstractAlbusHCSMessageHandler getHandler() {
    return albusHCSMessageHandler;
  }
}
