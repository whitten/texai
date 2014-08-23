/*
 * Node.java
 *
 * Created on Jan 16, 2008, 10:57:07 AM
 *
 * Description: Provides a persistent container for Texai launcher information.
 *
 * Copyright (C) Jan 16, 2008 Stephen L. Reed.
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
package org.texai.texaiLauncher.domainEntity;

import java.util.List;
import java.util.UUID;
import javax.persistence.Id;
import net.jcip.annotations.ThreadSafe;
import org.openrdf.model.URI;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;
import org.texai.kb.util.UUIDUtils;

/** Provides a persistent container for Texai launcher information.
 *
 * @author Stephen L. Reed
 */
@ThreadSafe
@RDFEntity(context = "texai:AlbusHierarchicalControlSystemContext")
public class TexaiLauncherInfo implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the id assigned by the persistence framework */
  @Id
  private URI id;
  /** the node runtime information objects */
  @RDFProperty
  private List<NodeRuntimeInfo> nodeRuntimeInfos;

  /** Constructs a new Node instance. */
  public TexaiLauncherInfo() {
  }

  /** Gets the id assigned by the persistence framework.
   *
   * @return the id assigned by the persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the UUID which is the alias of the launcher's X509 certificate.
   *
   * @return the uuid
   */
  public UUID getUUID() {
    return UUIDUtils.uriToUUID(id);
  }

  /** Gets the node runtime information objects.
   *
   * @return the node runtime information objects
   */
  public List<NodeRuntimeInfo> getNodeRuntimeInfos() {
    return nodeRuntimeInfos;
  }

  /** Sets the node runtime information objects.
   *
   * @param nodeRuntimeInfos the node runtime information objects
   */
  public void setNodeRuntimeInfos(final List<NodeRuntimeInfo> nodeRuntimeInfos) {
    //Preconditions
    assert nodeRuntimeInfos != null : "nodeRuntimeInfos must not be null";

    this.nodeRuntimeInfos = nodeRuntimeInfos;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("[Texai launcher");
    for (final NodeRuntimeInfo nodeRuntimeInfo : nodeRuntimeInfos) {
      stringBuilder.append("\n  ");
      stringBuilder.append(nodeRuntimeInfo);
    }
    stringBuilder.append("]");
    return stringBuilder.toString();
  }
}
