/*
 * MockAlbusHCSMessageHandlerFactory.java
 *
 * Description: Provides a test Albus hierachical control system message handler factory.
 *
 * Copyright (C) Feb 9, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a test Albus hierachical control system message handler factory.
 *
 * @author reed
 */
@NotThreadSafe
public class MockAlbusHCSMessageHandlerFactory extends AbstractAlbusHCSMessageHandlerFactory {

  /**
   * Constructs a new MockAlbusHCSMessageHandlerFactory instance.
   */
  public MockAlbusHCSMessageHandlerFactory() {
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractAlbusHCSMessageHandler getHandler() {
    return new MockAlbusHCSMessageHandler(null, 0);
  }
}
