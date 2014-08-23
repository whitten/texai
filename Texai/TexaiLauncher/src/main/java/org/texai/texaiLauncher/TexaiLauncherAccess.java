/*
 * TexaiLauncherAccess.java
 *
 * Created on Sep 15, 2011, 11:17:44 AM
 *
 * Description: Provides access to persistent Texai launcher information objects.
 *
 * Copyright (C) Sep 15, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.texaiLauncher;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.texaiLauncher.domainEntity.NodeRuntimeInfo;
import org.texai.texaiLauncher.domainEntity.TexaiLauncherInfo;

/** Provides access to persistent Texai launcher information objects.
 *
 * @author reed
 */
@NotThreadSafe
public class TexaiLauncherAccess {

  /** the RDF entity manager */
  private final RDFEntityManager rdfEntityManager;

  /** Creates a new TexaiLauncherAccess instance.
   *
   * @param rdfEntityManager the RDF entity manager
   */
  public TexaiLauncherAccess(final RDFEntityManager rdfEntityManager) {
    //Preconditions
    assert rdfEntityManager != null : "RDFEntityManager must not be null";

    this.rdfEntityManager = rdfEntityManager;
  }

  /** Finds the Texai launcher information object.
   *
   * @return the Texai launcher information object, or null if not found
   */
  public TexaiLauncherInfo findTexaiLauncherInfo() {
    final Iterator<TexaiLauncherInfo> texaiLauncherInfo_iter = rdfEntityManager.rdfEntityIterator(TexaiLauncherInfo.class);
    if (texaiLauncherInfo_iter.hasNext()) {
      final TexaiLauncherInfo texaiLauncherInfo = texaiLauncherInfo_iter.next();
      assert !texaiLauncherInfo_iter.hasNext();
      return texaiLauncherInfo;
    } else {
      return null;
    }
  }

  /** Finds or creates the Texai launcher information object.
   *
   * @return the Texai launcher information object
   */
  public TexaiLauncherInfo findOrCreateTexaiLauncherInfo() {
    TexaiLauncherInfo texaiLauncherInfo = findTexaiLauncherInfo();
    if (texaiLauncherInfo == null) {
      texaiLauncherInfo = new TexaiLauncherInfo();
      rdfEntityManager.persist(texaiLauncherInfo);
    }
    return texaiLauncherInfo;
  }

  /** Persists the given Texai launcher information object.
   *
   * @param texaiLauncherInfo the given Texai launcher information object
   */
  public void persistTexaiLauncherInfo(final TexaiLauncherInfo texaiLauncherInfo) {
    //Preconditions
    assert texaiLauncherInfo != null : "texaiLauncherInfo must not be null";

    rdfEntityManager.persist(texaiLauncherInfo);
  }

  /** Gets a copy of the node runtime information objects.
   *
   * @return a copy of the node runtime information objects
   */
  public List<NodeRuntimeInfo> getNodeRuntimeInfos() {
    final List<NodeRuntimeInfo> nodeRuntimeInfos = new ArrayList<>();
    final Iterator<NodeRuntimeInfo> nodeRuntimeInfos_iter = rdfEntityManager.rdfEntityIterator(NodeRuntimeInfo.class);
    while (nodeRuntimeInfos_iter.hasNext()) {
      nodeRuntimeInfos.add(nodeRuntimeInfos_iter.next());
    }
    return nodeRuntimeInfos;
  }
}
