/*
 * EnvironmentUtils.java
 *
 * Created on Jan 25, 2010, 8:00:53 AM
 *
 * Description: Provides runtime and system property utilities.
 *
 * Copyright (C) Jan 25, 2010 reed.
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
package org.texai.util;

import com.jezhumble.javasysmon.JavaSysMon;
import com.jezhumble.javasysmon.MemoryStats;
import java.io.File;
import java.util.Map.Entry;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/** Provides runtime and system property utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class EnvironmentUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(EnvironmentUtils.class);

  /** Private constructor to ensure non-instantiation. */
  private EnvironmentUtils() {
  }

  /** Returns whether this is the correct Java version to run the Texai application.
   *
   * @return whether this is the correct Java version to run the Texai application
   */
  public static boolean isCorrectJavaVersion() {
    return System.getProperty("java.specification.version").equals("1.8");
  }

  /** Returns whether the operating system is a version of Windows.
   *
   * @return whether the operating system is a version of Windows.
   */
  public static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  /** Returns whether the operating system is a version of Linux.
   *
   * @return whether the operating system is a version of Linux.
   */
  public static boolean isLinux() {
    return System.getProperty("os.name").toLowerCase().startsWith("linux");
  }

  /** Logs the runtime environment. */
  public static void logRuntimeEnvironment() {
    LOGGER.info("Runtime environment:");
    final long megabyte = 1024 * 1024;
    final Runtime runtime = Runtime.getRuntime();
    LOGGER.info("available processors: " + runtime.availableProcessors());
    final long freeMemory = runtime.freeMemory();
    LOGGER.info("free memory:          " + (freeMemory / megabyte) + "m");
    final long maxMemory = runtime.maxMemory();
    LOGGER.info("maximum memory:       " + (maxMemory / megabyte) + "m");
    final long totalMemory = runtime.totalMemory();
    LOGGER.info("total memory:         " + (totalMemory / megabyte) + "m");
    for (File root : File.listRoots()) {
      LOGGER.info("File system root: " + root.getAbsolutePath());
      LOGGER.info("Total space (megabytes): " + root.getTotalSpace() / megabyte);
      LOGGER.info("Free space (megabytes): " + root.getFreeSpace() / megabyte);
      LOGGER.info("Usable space (megabytes): " + root.getUsableSpace() / megabyte);
    }
  }

  /** Logs the system properties. */
  public static void logSystemProperties() {
    LOGGER.info("System properties:");
    for (final Entry<Object, Object> entry : System.getProperties().entrySet()) {
      LOGGER.info("  " + entry.getKey().toString() + " = " + entry.getValue().toString());
    }
  }

  /** Logs the host system monitor. */
  public static void logSystemMonitor() {
    final JavaSysMon javaSysMon = new JavaSysMon();
    final long giga = 1024 * 1024 * 1024;

    final long cpuFrequencyInHz = javaSysMon.cpuFrequencyInHz();
    final double cpuFrequencyInGHz = ((double) cpuFrequencyInHz) / ((double) giga);
    LOGGER.info("cpuFrequencyInGHz: " + cpuFrequencyInGHz);

    final MemoryStats memoryStats = javaSysMon.physical();
    LOGGER.info("GB RAM: " + ((double) memoryStats.getTotalBytes()) / ((double) giga));
  }

  /** Returns the X.509 certificate server host.
   *
   * @return the X.509 certificate server host
   */
  public static String certificateServerHost() {
    String certificateServerHost = System.getenv("CERTIFICATE_SERVER");
    if (certificateServerHost == null) {
      certificateServerHost = "turing";
    }
    return certificateServerHost;
  }
}
