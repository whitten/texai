/*
 * TexaiMain.java
 *
 * Created on Sep 21, 2011, 6:23:02 PM
 *
 * Description: Executes the Texai node runtime for a certain JVM.
 *
 * Copyright (C) Sep 21, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.main;

import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.ahcs.impl.NodeRuntimeImpl;
import org.texai.util.StringUtils;

/** Executes the Texai node runtime for a certain JVM.
 *
 * @author reed
 */
@NotThreadSafe
public class TexaiMain {

  static {
    // explicitly set the default assertion status because NetBeans ignores -ea when running an application.
    TexaiMain.class.getClassLoader().setDefaultAssertionStatus(true);
  }
  /** the logger */
  public static final Logger LOGGER = Logger.getLogger(TexaiMain.class);
  /** the launcher role id */
  private URI launcherRoleId;
  /** the node runtime role id */
  private URI nodeRuntimeRoleId;

  /** Constructs a new TexaiMain instance. */
  public TexaiMain() {
  }

  /** Initializes this application.
   *
   * @param launcherRoleIdString the launcher role id string
   * @param nodeRuntimeRoleIdString the node runtime role id string
   */
  private void initialization(
          final String launcherRoleIdString,
          final String nodeRuntimeRoleIdString) {
    //Preconditions
    assert StringUtils.isNonEmptyString(launcherRoleIdString) : "launcherRoleIdString must a non-empty string";
    assert StringUtils.isNonEmptyString(launcherRoleIdString) : "nodeRuntimeRoleIdString must a non-empty string";

    LOGGER.info("launcher role id " + launcherRoleIdString);
    launcherRoleId = new URIImpl(launcherRoleIdString);
    LOGGER.info("node runtime id  " + nodeRuntimeRoleIdString);
    nodeRuntimeRoleId = new URIImpl(nodeRuntimeRoleIdString);
  }

  /** Processes this application.
   *
   * @param internalPort the internal port
   * @param externalPort the external port
   * @param localURI the URI of this node in the Texai network
   * @param bootstrapURI the bootstrap node runtime URI, or null if this is the first node in the Texai network
   * @param localAreaNetworkID the local area network ID
   */
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  private void process(
          final int internalPort,
          final int externalPort,
          final URI localURI,
          final URI bootstrapURI,
          final UUID localAreaNetworkID) {
    //Preconditions
    assert internalPort > 0 : "internalPort must be positive";
    assert externalPort > 0 : "externalPort must be positive";
    assert localAreaNetworkID != null : "localAreaNetworkID must not be null";

    LOGGER.info("waiting 15 seconds to start Texai node runtime");
    try {
      Thread.sleep(15_000);
    } catch (InterruptedException ex) {
    }
    LOGGER.info("starting Texai node runtime on local area network ID: " + localAreaNetworkID);
    new NodeRuntimeImpl(
            launcherRoleId,
            nodeRuntimeRoleId,
            internalPort,
            externalPort,
            localURI,
            bootstrapURI,
            localAreaNetworkID);
  }

  /** Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    //Preconditions
    if (args.length != 7) {
      LOGGER.error("command line argument must specify launcher's role id, node runtime id, internal port, external port, local URL, bootstrap URL, and the LAN ID - found " + args.length + " argument(s)");
    }

    final TexaiMain texaiMain = new TexaiMain();
    texaiMain.initialization(
            args[0], // launcherRoleIdString
            args[1]); // nodeRuntimeRoleIdString
    final URI localURI;
    final URI bootstrapURI;
      localURI = new URIImpl(args[4]);
      bootstrapURI = new URIImpl(args[5]);
    final UUID localAreaNetworkID = UUID.fromString(args[6]);
    texaiMain.process(
            Integer.parseInt(args[2]), // internalPort
            Integer.parseInt(args[3]), // externalPort
            localURI,
            bootstrapURI,
            localAreaNetworkID);
  }
}
