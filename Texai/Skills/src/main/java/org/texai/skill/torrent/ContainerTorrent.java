/*
 * ContainerTorrent.java
 *
 * Created on Jun 24, 2010, 6:38:08 PM
 *
 * Description: Provides effector-level bit torrent file sharing behavior.
 *
 * Copyright (C) Jun 24, 2010, Stephen L. Reed.
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
package org.texai.skill.torrent;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;

/** Provides effector-level bit torrent file sharing behavior.
 *
 * @author reed
 */
@NotThreadSafe
public class ContainerTorrent extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerTorrent.class);

  /** Constructs a new TorrentEffector instance. */
  public ContainerTorrent() {
  }

  /** Gets the logger.
   * 
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
  
  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    if (operation.equals(AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO)) {
      LOGGER.warn(message);
      return true;
    }

    //TODO handle operations

    sendMessage(notUnderstoodMessage(message));
    return true;
  }

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations

    return notUnderstoodMessage(message);
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[] {
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
    };
  }

}
