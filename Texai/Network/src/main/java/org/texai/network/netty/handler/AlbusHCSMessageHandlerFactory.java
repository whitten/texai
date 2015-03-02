/*
 * AlbusHCSMessageHandlerFactory.java
 *
 * Created on Feb 9, 2010, 11:27:59 AM
 *
 * Description: Provides an Albus hierarchical control system message handler factory.
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.ThreadSafe;

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
