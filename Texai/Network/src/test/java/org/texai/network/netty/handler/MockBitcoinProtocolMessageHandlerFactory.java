/*
 * MockBitcoinProtocolMessageHandlerFactory.java
 *
 * Description: Provides a test Bitcoin protocol message handler factory.
 *
 * Copyright (C) Feb 25, 2015 by Stephen Reed.
 *
 */
package org.texai.network.netty.handler;

import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a test Bitcoin protocol message handler factory.
 *
 * @author reed
 */
@NotThreadSafe
public class MockBitcoinProtocolMessageHandlerFactory extends AbstractBitcoinProtocolMessageHandlerFactory {

  // the client resume lock
  final Object clientResume_lock;

  /**
   * Constructs a new MockBitcoinProtocolMessageHandlerFactory instance.
   *
   * @param clientResume_lock the lock which permits the client to resume the unit test
   */
  public MockBitcoinProtocolMessageHandlerFactory(final Object clientResume_lock) {
    //Preconditions
    assert clientResume_lock != null : "clientResume_lock must not be null";

    this.clientResume_lock = clientResume_lock;
  }

  /**
   * Gets a new channel handler.
   *
   * @return a new channel handler
   */
  @Override
  public AbstractBitcoinProtocolMessageHandler getHandler() {
    return new MockBitcoinProtocolMessageHandler(clientResume_lock);
  }
}
