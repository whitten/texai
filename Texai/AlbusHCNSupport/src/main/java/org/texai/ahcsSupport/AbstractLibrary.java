/*
 * AbstractLibrary.java
 *
 * Created on Jun 30, 2010, 12:41:59 PM
 *
 * Description: Provides an abstract library to support skills.
 *
 * Copyright (C) Jun 30, 2010, Stephen L. Reed.
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
package org.texai.ahcsSupport;

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
