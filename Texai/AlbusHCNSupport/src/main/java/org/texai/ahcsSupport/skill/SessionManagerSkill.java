/*
 * AbstractSkill.java
 *
 * Created on Jan 16, 2008, 10:57:22 AM
 *
 * Description: A skill that provides session-awareness for another skill.
 *
 * Copyright (C) Jan 16, 2008 Stephen L. Reed.
 */
package org.texai.ahcsSupport.skill;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.joda.time.Duration;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.AHCSConstants.State;
import org.texai.ahcsSupport.Message;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/** A skill that provides session-awareness for another skill.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
public class SessionManagerSkill extends AbstractSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SessionManagerSkill.class);
  // the class of the managed sub-skill
  private Class<?> skillClass;
  // the session dictionary, session handle --> managed skill instance
  private final Map<String, SkillInfo> sessionDictionary = new HashMap<>();
  // the duration beyond which unused skill instances are garbage collected
  private long cleanupDurationTimeMillis = 1000 * 60 * 60 * 24; // defaults to one day
  // the dummy session handle
  private static final String DUMMY_SESSION_HANDLE = "dummy session handle";

  /** Constructs a new Skill instance. */
  public SessionManagerSkill() {
  }

  /** Gets the logger.
   *
   * @return  the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /** Returns the class name of this skill.
   *
   * @return the class name of this skill
   */
  @Override
  public String getClassName() {
    return getClass().getName();
  }

  /** Receives and attempts to process the given message.  The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param message the given message
   */
  @Override
  public void receiveMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    final String operation = message.getOperation();
    if (!isOperationPermitted(message)) {
      sendMessage(Message.operationNotPermittedMessage(
              message, // receivedMessage
              this)); // skill
      return;
    }
    switch (operation) {
      case AHCSConstants.AHCS_INITIALIZE_TASK:
        assert getSkillState().equals(State.UNINITIALIZED) : "prior state must be non-initialized";
        getNodeRuntime().getTimer().scheduleAtFixedRate(
                new SkillInfoJanitorProcess(), // task
                3_600_000, // delay - one hour
                3_600_000); // period - one hour
        // initialize a dummy instance of the role
        getSkillInstance(DUMMY_SESSION_HANDLE).receiveMessage(message);
        // initialize child roles
        propagateOperationToChildRoles(operation);
        if (getNodeRuntime().isFirstContainerInNetwork()) {
          setSkillState(AHCSConstants.State.READY);
        } else {
          setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
        }
        return;

      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(message);
        return;
    }
    assert operation.equals(AHCSConstants.REGISTER_SENSED_UTTERANCE_PROCESSOR_TASK)
            || getSkillState().equals(State.READY) : "must be in the ready state, but is " + stateDescription(getSkillState())
            + "\nmessage: " + message;

    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("delegating " + message.getOperation() + " to " + getSkillInstance(getSessionHandle(message)));
    }
    getSkillInstance(getSessionHandle(message)).receiveMessage(message);
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.info("delegating " + message.getOperation() + " to " + getSkillInstance(getSessionHandle(message)));
    }
    return getSkillInstance(getSessionHandle(message)).converseMessage(message);
  }

  /** Returns the skill instance that corresponds to the session handle.
   *
   * @param sessionHandle the session  handle
   * @return the skill instance that corresponds to the session handle
   */
  private AbstractSkill getSkillInstance(final String sessionHandle) {
    //Preconditions
    assert sessionHandle != null : "sessionHandle must not be null";
    assert !sessionHandle.isEmpty() : "sessionHandle must not be empty";
    assert skillClass != null : "skillClass must not be null";

    final AbstractSkill skill;
    synchronized (sessionDictionary) {
      SkillInfo skillInfo = sessionDictionary.get(sessionHandle);
      if (skillInfo == null) {
        try {
          skill = (AbstractSkill) skillClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
          throw new TexaiException(ex);
        }
        skill.setRole(getRole());
        skill.setSessionManagerSkill(this);
        skillInfo = new SkillInfo(skill, sessionHandle);
        sessionDictionary.put(sessionHandle, skillInfo);

        if (!sessionHandle.equals(DUMMY_SESSION_HANDLE)) {
          // initialize the skill instance
          Message message = new Message(
                  getQualifiedName(), // senderQualifiedName
                  getClassName(), // senderService
                  getQualifiedName(), // recipientQualifiedName
                  skillClass.getName(), // service
                  AHCSConstants.AHCS_INITIALIZE_TASK); // operation
          skill.receiveMessage(message);
        }
      } else {
        skill = skillInfo.skill;
      }
    }

    //Postconditions
    assert skill != null : "skill must not be null";

    return skill;
  }

  /** Disconnects the session.
   *
   * @param sessionHandle the session handle
   */
  public void disconnectSession(final String sessionHandle) {
    //Preconditions
    assert StringUtils.isNonEmptyString(sessionHandle) : "sessionHandle must be a non-empty string";

    synchronized (sessionDictionary) {
      final SkillInfo skillInfo = sessionDictionary.remove(sessionHandle);
      if (skillInfo != null) {
        LOGGER.info("removed " + skillInfo.skill.getClass().getSimpleName() + " because session disconnected " + sessionHandle);
      }
    }
  }

  /** Gets the session handle from the given message.
   *
   * @param message the given message
   * @return the session handle
   */
  public String getSessionHandle(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    String sessionHandle = (String) message.get(AHCSConstants.SESSION);
    if (sessionHandle == null) {
      sessionHandle = UUID.randomUUID().toString();
    }
    return sessionHandle;
  }

  /** Sets the class of the managed sub-skill.
   *
   * @param skillClass the class of the managed sub-skill
   */
  public void setSkillClass(Class<?> skillClass) {
    this.skillClass = skillClass;
  }

  /** Gets the duration beyond which unused skill instances are garbage collected.
   *
   * @return the duration beyond which unused skill instances are garbage collected
   */
  public long getCleanupDurationTimeMillis() {
    return cleanupDurationTimeMillis;
  }

  /** Sets the duration beyond which unused skill instances are garbage collected.
   *
   * @param cleanupDurationTimeMillis the duration beyond which unused skill instances are garbage collected
   */
  public void setCleanupDurationTimeMillis(long cleanupDurationTimeMillis) {
    this.cleanupDurationTimeMillis = cleanupDurationTimeMillis;
  }

  /** Provides a periodic task that iterates over the skill infos and removes any sufficiently unused. */
  class SkillInfoJanitorProcess extends TimerTask {

    /** Iterates over the skill infos and removes any sufficiently unused. */
    @Override
    public void run() {
      synchronized (sessionDictionary) {
        final Iterator<SkillInfo> skillInfo_iter = sessionDictionary.values().iterator();
        final long threshhold = System.currentTimeMillis() - cleanupDurationTimeMillis;
        while (skillInfo_iter.hasNext()) {
          final SkillInfo skillInfo = skillInfo_iter.next();
          if (skillInfo.lastUsedTimeMillis < threshhold) {
            LOGGER.info("removing stale skill information session " + skillInfo.sessionHandle);
            skillInfo_iter.remove();
          }
        }
      }
    }
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "session managed " + skillClass.getSimpleName();
  }

  /** Returns a list of the understood operations.
   *
   * @return a list of the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    try {
      return ((AbstractSkill) skillClass.newInstance()).getUnderstoodOperations();
    } catch (InstantiationException | IllegalAccessException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Provides an informational container for a skill session. */
  static class SkillInfo {

    /** the skill instance for a certain session */
    private final AbstractSkill skill;
    /** the session handle */
    private final String sessionHandle;
    /** the system millisecond timestamp of last use */
    private long lastUsedTimeMillis;

    /** Constructs a new SkillInfo instance.
     *
     * @param skill the skill instance for a certain session
     * @param sessionHandle the session handle
     */
    SkillInfo(
            final AbstractSkill skill,
            final String sessionHandle) {
      //Preconditions
      assert skill != null : "skill must not be null";
      assert sessionHandle != null : "sessionHandle must not be null";
      assert !sessionHandle.isEmpty() : "sessionHandle must not be empty";

      this.skill = skill;
      this.sessionHandle = sessionHandle;
      lastUsedTimeMillis = System.currentTimeMillis();
    }

    /** Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
      final StringBuilder stringBuilder = new StringBuilder();
      stringBuilder.append("[");
      stringBuilder.append(sessionHandle);
      stringBuilder.append(" ");
      stringBuilder.append(new Duration(lastUsedTimeMillis, System.currentTimeMillis()));
      stringBuilder.append("]");
      return stringBuilder.toString();
    }
  }
}
