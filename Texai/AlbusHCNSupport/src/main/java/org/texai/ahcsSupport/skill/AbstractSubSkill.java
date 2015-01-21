/*
 * AbstractSubSkill.java
 *
 * Created on Jun 30, 2010, 11:17:19 AM
 *
 * Description: An abstract skill that provides sub-behavior for another skill.
 *
 * Copyright (C) Jun 30, 2010, Stephen L. Reed.
 */
package org.texai.ahcsSupport.skill;

import net.jcip.annotations.NotThreadSafe;

/** An abstract skill that provides sub-behavior for another skill. Generally, subskills may be shared among skills and
 * threads and thus should either not preserve state, or preserve state in a dictionary keyed by a message parameter, e.g.
 * inReplyTo.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractSubSkill extends AbstractSkill {

  /** Constructs a new AbstractSubSkill instance. */
  public AbstractSubSkill() {
  }

  /** Initializes this skill */
  public abstract void initialization();

  /** finalizes this skill and releases its resources. */
  public abstract void finalization();

}
