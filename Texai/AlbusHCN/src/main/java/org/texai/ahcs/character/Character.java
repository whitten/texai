/*
 * AbstractCharacter.java
 *
 * Created on Feb 2, 2009, 7:46:12 AM
 *
 * Description: Defines a character interface for a skill, in which the skill's virtue can be estabished and it's behavior
 * explained.
 *
 * Copyright (C) Feb 2, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.ahcs.character;

import java.util.Set;
import net.jcip.annotations.NotThreadSafe;

/** Defines a character interface for a skill, in which the skill's virtue can be estabished and it's behavior
 * explained.
 *
 * <p>
 * Autonomous Military Robotics: Risk, Ethics, and Design (2009) Lin, Abney, and Bekey. 
 * http://ethics.calpoly.edu/ONR_report.pdf
 * <blockquote>
 * The  challenge  for  the  military  will  reside  in  preventing  the  development  of  lethal  robotic  systems 
 * from outstripping the ability of engineers to assure the safety of these systems.  Implementing moral 
 * decision-making  faculties  within  robots  will  proceed  slowly.    While  there  are  aspects  of  moral 
 * judgment  that  can  be  isolated  and  codified  for  tightly  defined  contexts,  moral  intelligence  for 
 * autonomous entities is a complex activity dependent on the integration of a broad array of discrete 
 * skills.  Robots initially will be built to perform specified tasks.  However, as computer scientists learn 
 * to build more sophisticated systems that can analyze and accommodate the moral challenges posed 
 * by  new  contexts,  autonomous  robots  can  and  will  be  deployed  for  a  broad  array  of  military 
 * applications.  So for the foreseeable future and as a more reasonable goal, it seems best to attempt 
 * to program a virtuous partial character into a robot and ensure it only enters situations in which its 
 * character can function appropriately.  
 * </blockquote>
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public interface Character {

  /** Gets propositional statements representing the virtues of this skill.
   *
   * @return propositional statements representing the virtues of this skill
   */
  Set<Virtue> getVirtues();

  /** Gets an explanation of the given observed behavior.
   *
   * @param observedBehavior the given observed behavior
   * @return an explanation of the given observed behavior
   */
  Rationale getRationale(final ObservedBehavior observedBehavior);

  /** Gets an explanation of the given proposed behavior.
   *
   * @param proposedBehavior the given proposed behavior
   * @return an explanation of the given proposed behavior
   */
  Rationale getRationale(final ProposedBehavior proposedBehavior);
}
