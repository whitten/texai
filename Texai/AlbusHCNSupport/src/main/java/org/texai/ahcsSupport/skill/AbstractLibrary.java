/*
 * AbstractLibrary.java
 *
 * Created on Jun 30, 2010, 12:41:59 PM
 *
 * Description: Provides an abstract library to support skills.
 *
 * Copyright (C) Jun 30, 2010, Stephen L. Reed.
 */
package org.texai.ahcsSupport.skill;

import net.jcip.annotations.NotThreadSafe;

/** Provides an abstract library to support skills.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractLibrary {

  // the containing skill
  private final AbstractSkill skill;

  /** Constructs a new AbstractLibrary instance.
   *
   * @param skill the containing skill
   */
  public AbstractLibrary(final AbstractSkill skill) {
    this.skill = skill;
  }

  /** initializes this library */
  public abstract void initialize();

  /** shuts down this library and releases its resources */
  public abstract void shutdown();

  /** Gets the containing skill.
   *
   * @return the containing skill
   */
  public AbstractSkill getSkill() {
    return skill;
  }
}
