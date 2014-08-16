/*
 * AbstractUnsharedSubSkill.java
 *
 * Created on Feb 17, 2012, 11:37:31 AM
 *
 * Description: An abstract skill that provides unshared, single session, sub-behavior for another skill.
 *
 * Copyright (C) Feb 17, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ahcsSupport;

import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractUnsharedSubSkill extends AbstractSubSkill {

  /** the parent skill */
  private AbstractSkill parentSkill;

  /** Constructs a new AbstractUnsharedSubSkill instance. */
  public AbstractUnsharedSubSkill() {
  }

  /** Gets the parent skill.
   *
   * @return the parentSkill
   */
  public AbstractSkill getParentSkill() {
    return parentSkill;
  }

  /** Sets the parent skill.
   *
   * @param parentSkill the parentSkill to set
   */
  public void setParentSkill(final AbstractSkill parentSkill) {
    //Preconditions
    assert parentSkill != null : "parentSkill must not be null";

    this.parentSkill = parentSkill;
  }

}
