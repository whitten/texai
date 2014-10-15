/*
 * AbstractReteNode.java
 *
 * Created on Aug 12, 2010, 8:47:34 PM
 *
 * Description: Provides an abstract Rete node.
 *
 * Copyright (C) Aug 12, 2010, Stephen L. Reed.
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
package org.texai.inference.rete;

import java.util.LinkedList;
import net.jcip.annotations.NotThreadSafe;

/** Provides an abstract Rete node.
 *
 * @author reed
 */
@NotThreadSafe
public abstract class AbstractReteNode {

  /** the child nodes */
  private final LinkedList<AbstractReteNode> children = new LinkedList<>();
  /** the parent node, or null if this is topmost */
  private final AbstractReteNode parent;
  /** the graph id */
  private int id;

  /** Constructs a new AbstractReteNode instance.
   *
   * @param parent the parent node, or null if this is topmost
   */
  public AbstractReteNode(final AbstractReteNode parent) {
    this.parent = parent;
  }

  /** Gets the parent node, or null if this is topmost.
   *
   * @return the parent node
   */
  public AbstractReteNode getParent() {
    return parent;
  }

  /** Gets the child nodes.
   *
   * @return the child nodes
   */
  public LinkedList<AbstractReteNode> getChildren() {
    return children;
  }

  /** Gets the graph id.
   *
   * @return the graph id
   */
  public int getId() {
    return id;
  }

  /** Sets the graph id.
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }
}
