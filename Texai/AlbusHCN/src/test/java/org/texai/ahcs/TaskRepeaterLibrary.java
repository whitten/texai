/*
 * TaskRepeaterLibrary.java
 *
 * Created on Mar 17, 2010, 2:33:42 PM
 *
 * Description: Provides a test libary that resends task messages from the parent node down to the child node.
 *
 * Copyright (C) Mar 17, 2010 reed.
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

import org.texai.ahcsSupport.Message;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcsSupport.domainEntity.Role;

/** Provides a test libary that resends task messages from the parent node down to the child node.
 *
 * @author reed
 */
@NotThreadSafe
public class TaskRepeaterLibrary 
//        extends AbstractSkillLibrary
{

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(TaskRepeaterLibrary.class);

  /** Constructs a new TaskRepeaterLibrary instance. */
  public TaskRepeaterLibrary() {
  }

//  /** initializes this library */
//  @Override
//  public void initialize() {
//    LOGGER.info("initializing");
//  }
//
//  /** shuts down this library and releases its resources */
//  @Override
//  public void shutdown() {
//  }
//
//  /** Receives and attempts to process the given message.
//   *
//   * @param message the given message
//   * @return whether the message was successfully processed
//   */
//  @Override
//  public boolean receiveMessage(final Message message) {
//    //Preconditions
//    assert message != null : "message must not be null";
//    assert getSkill() != null : "skill must not be null";
//
//    LOGGER.info("received message: " + message);
//    if (message.getOperation().equals("Test_Task")) {
//      final Role role = getSkill().getRole();
//      if (role.getChildRoleIdStrings().isEmpty()) {
//        LOGGER.info("no child, ignoring message");
//      } else {
//        LOGGER.info("repeating the task message to the child");
//        final Message repeatedMessage = new Message(
//                role.getId(), // senderRoleId
//                new URIImpl(role.getChildRoleIdStrings().toArray()[0].toString()), // recipientRoleId
//                message.getConversationId(),
//                message.getReplyWith(),
//                message.getInReplyTo(),
//                message.getReplyByDateTime(),
//                message.mustRoleBeReady(),
//                message.getOperation(),
//                message.getParameterDictionary());
//        role.sendMessage(repeatedMessage);
//      }
//      return true;
//    } else {
//      return false;
//    }
//  }
}
