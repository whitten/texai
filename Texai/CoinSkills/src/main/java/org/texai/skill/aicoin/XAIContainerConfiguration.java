/*
 * XAIContainerConfiguration.java
 *
 * Created on Mar 14, 2012, 10:49:55 PM
 *
 * Description: Configures the roles in this container whose parents are nomadic singleton roles hosted
 * on a probably remote super-peer.
 *
 * Copyright (C) Mar 14, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.skill.aicoin;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.AbstractSkill;
import org.texai.ahcsSupport.Message;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class XAIContainerConfiguration extends AbstractSkill {

  // the log4j logger
  private static final Logger LOGGER = Logger.getLogger(XAIContainerConfiguration.class);

  /** Constructs a new SkillTemplate instance. */
  public XAIContainerConfiguration() {
  }
  
  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   * @return whether the message was successfully processed
   */
  @Override
  public boolean receiveMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(operationNotPermittedMessage(message));
      return true;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        setSkillState(State.INITIALIZED);
        return true;

      case AHCSConstants.AHCS_READY_TASK:
        assert getSkillState().equals(State.INITIALIZED) : "prior state must be initialized";
        setSkillState(State.READY);
        return true;
        
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return true;

      //TODO REQUEST_CONFIGURATON_TASK  
        // seed another peer who requests the locations of the nomadic singleton agents.
        
        
      // handle other operations ...
    }
    // otherwise, the message is not understood
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
  public Message converseMessage(Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    //TODO handle operations

    return (notUnderstoodMessage(message));
  }

  /** Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
              AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
              AHCSConstants.AHCS_INITIALIZE_TASK,
              AHCSConstants.AHCS_READY_TASK,
            };
  }
  
  /** Gets the logger.
   * 
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
  
}
