/*
 * MessageDispatcher.java
 *
 * Created on Apr 6, 2010, 2:02:40 PM
 *
 * Description: Defines the interface for dispatching messages in an Albus hierarchical control system.
 *
 * Copyright (C) Apr 6, 2010, Stephen L. Reed.
 *
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
