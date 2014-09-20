/*
 * VNCFeatureRecognition.java
 *
 * Created on Mar 14, 2012, 12:06:49 PM
 *
 * Description: Provides VNC (remote UI) feature recognition behavior for a particular remote UI session.
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.vncrobot;

import net.jcip.annotations.NotThreadSafe;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.ManagedSessionSkill;
import org.texai.ahcsSupport.Message;

/** Provides VNC (remote UI) feature recognition behavior for a particular remote UI session.
 *
 * @author reed
 */
@NotThreadSafe
@ManagedSessionSkill
public class VNCFeatureRecognition extends AbstractSkill {

  /** Constructs a new VNCFeatureRecognition instance. */
  public VNCFeatureRecognition() {
  }
  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Synchronously processes the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(Message message) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
