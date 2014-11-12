/*
 * AbstractSubSkill.java
 *
 * Created on Jun 30, 2010, 11:17:19 AM
 *
 * Description: An abstract skill that provides sub-behavior for another skill.
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
