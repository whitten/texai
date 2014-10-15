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

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.openrdf.model.URI;
import org.texai.ahcs.NodeRuntime;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

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

  /** Processes this application.
   *
   * @param containerName the container name
   */
  @SuppressWarnings("ResultOfObjectAllocationIgnored")
  private void process(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

    LOGGER.info("waiting 15 seconds to start Texai node runtime");
    try {
      Thread.sleep(15_000);
    } catch (InterruptedException ex) {
    }
    LOGGER.info("starting the node runtime in the " + containerName + " container");
    new NodeRuntime(containerName);
  }

  /** Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    //Preconditions
    if (args.length != 1) {
      throw new TexaiException("command line argument must specify the container name - found " + args.length + " argument(s)");
    }

    final TexaiMain texaiMain = new TexaiMain();
    final String containerName = args[0];
    if (!StringUtils.isNonEmptyString(containerName)) {
      throw new TexaiException("command line argument must specify the container name");
    }
    texaiMain.process(containerName);
  }
}
