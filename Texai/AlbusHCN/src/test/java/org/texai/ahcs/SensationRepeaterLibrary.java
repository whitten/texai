/*
 * SensationRepeaterLibrary.java
 *
 * Created on Mar 17, 2010, 11:10:57 AM
 *
 * Description: Provides a test libary that resends sensation messages from the child node up to the parent node.
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcsSupport.domainEntity.Role;

/** Provides a test libary that resends sensation messages from the child node up to the parent node.
 *
 * @author reed
 */
@NotThreadSafe
public final class SensationRepeaterLibrary 
//        extends AbstractSkillLibrary
{

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(SensationRepeaterLibrary.class);

  /** Constructs a new SensationRepeaterLibrary instance. */
  public SensationRepeaterLibrary() {
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
//    final Role role = getSkill().getRole();
//    if (message.getOperation().equals("Start_Task")) {
//      LOGGER.info("sending the first sensation message to the parent");
//      sendSensationMessageToParent(message);
//      return true;
//    }
//    if (message.getOperation().equals("Test_Sensation")) {
//      if (role.getParentRoleIdString() == null) {
//        LOGGER.info("no parent, sending first task message to the child");
//        final URI recipientRoleId = new URIImpl(role.getChildRoleIdStrings().toArray()[0].toString());
//        final Message taskMessage = new Message(
//                role.getId(), // senderRoleId
//                recipientRoleId,
//                message.getConversationId(),
//                UUID.randomUUID(), // replyWith,
//                message.getReplyWith(), // inReplyTo
//                null, // replyByDateTime
//                true, // mustRoleBeReady
//                "Test_Task", // operation
//                new HashMap<String, Object>()); // parameterDictionary
//        role.sendMessage(taskMessage);
//      } else {
//        LOGGER.info("repeating the sensation message to the parent");
//        final Message repeatedMessage = new Message(
//                role.getId(), // senderRoleId
//                new URIImpl(role.getParentRoleIdString()), // recipientRoleId
//                message.getConversationId(),
//                message.getReplyWith(),
//                message.getInReplyTo(),
//                message.getReplyByDateTime(),
//                message.mustRoleBeReady(),
//                message.getOperation(),
//                message.getParameterDictionary());
//        // send message
//        role.sendMessage(repeatedMessage);
//      }
//      return true;
//    } else {
//      return false;
//    }
//  }
//
//  /** Sends a test sensation message to the parent role.
//   *
//   * @param message the received task message
//   */
//  public void sendSensationMessageToParent(final Message message) {
//    //Preconditions
//    assert message != null : "message must not be null";
//    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
//
//    final Role role = getSkill().getRole();
//    assert role != null;
//    final UUID conversationId = UUID.randomUUID();
//    final UUID replyWith = UUID.randomUUID();
//    final UUID inReplyTo = UUID.randomUUID();
//    final DateTime replyByDateTime = null;
//    final boolean mustRoleBeReady = true;
//    final String operation = "Test_Sensation";
//    final Map<String, Object> parameterDictionary = new HashMap<String, Object>();
//    final Message sensationMessage = new Message(
//            role.getId(), // senderRoleId
//            new URIImpl(role.getParentRoleIdString()), // recipientRoleId
//            conversationId,
//            replyWith,
//            inReplyTo,
//            replyByDateTime,
//            mustRoleBeReady,
//            operation,
//            parameterDictionary);
//    sensationMessage.sign(x509SecurityInfo.getPrivateKey());
//    // send message
//    role.sendMessage(sensationMessage);
//  }
}
