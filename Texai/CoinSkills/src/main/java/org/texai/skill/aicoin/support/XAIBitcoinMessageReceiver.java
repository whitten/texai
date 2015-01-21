package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.Message;

/**
 * Created on Sep 10, 2014, 2:13:57 PM.
 *
 * Description: Defines the interface for a bitcoin message receiver, which is the skill that handles outbound bitcoin messages
 * from the slave peer.
 *
 * Copyright (C) Sep 10, 2014, Stephen L. Reed, Texai.org.
 */
public interface XAIBitcoinMessageReceiver {

  /** Receives an outbound bitcoin message from the slave peer.
   *
   * @param message the given bitcoin protocol message
   */
  void receiveBitcoinMessageFromSlave(final Message message);
}
