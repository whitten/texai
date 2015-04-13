package org.texai.network.netty.handler;

import com.google.bitcoin.core.Message;

/**
 * Created on Sep 10, 2014, 2:13:57 PM.
 *
 * Description: Defines the interface for a bitcoin message receiver, which is the skill that handles outbound bitcoin messages
 * from the local peer.
 *
 * Copyright (C) Sep 10, 2014, Stephen L. Reed, Texai.org.
 */
public interface BitcoinMessageReceiver {

  /** Receives an outbound bitcoin message from the local bitcoind instance.
   *
   * @param message the given bitcoin protocol message
   */
  void receiveMessageFromBitcoind(final Message message);
}
