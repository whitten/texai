/*
 * LinuxScreenUtils.java
 *
 * Created on Aug 31, 2011, 10:58:34 AM
 *
 * Description: Provides utility methods to manage Linux Screen sessions.
 *
 * Copyright (C) Aug 31, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.util;

import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/** Provides utility methods to manage Linux Screen sessions.
 *
 * @author reed
 */
@NotThreadSafe
public class LinuxScreenUtils {

  // the logger
  public static final Logger LOGGER = Logger.getLogger(LinuxScreenUtils.class);

  /** Prevents the construction a new LinuxScreenUtils instance. */
  private LinuxScreenUtils() {
  }

  /** Launches a detached screen session with the given command.
   *
   * @param workingDirectory the working directory
   * @param command the given command
   * @param sessionName the name of the screen session
   */
  public static void launchDetachedScreenSession(
          final String workingDirectory,
          final String command,
          final String sessionName) {
    //Preconditions
    assert workingDirectory != null : "workingDirectory must not be null";
    assert !workingDirectory.isEmpty() : "workingDirectory must not be an empty string";
    assert command != null : "command must not be null";
    assert !command.isEmpty() : "command must not be empty";
    assert sessionName != null : "sessionName must not be null";
    assert !sessionName.isEmpty() : "sessionName must not be empty";

    if (!EnvironmentUtils.isLinux()) {
      // do not try to create a screen session on Windows
      return;
    }
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("cd ");
    stringBuilder.append(workingDirectory);
    stringBuilder.append(" ; screen -S ");
    stringBuilder.append(sessionName);
    // start a detached screen session with a forked process
    stringBuilder.append(" -L -h 5000 -d -m ");
    stringBuilder.append(command);
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("shell cmd: " + cmdArray[2]);
    try {
      final Process process = Runtime.getRuntime().exec(cmdArray);
      final StreamConsumer errorConsumer = new StreamConsumer(process.getErrorStream(), LOGGER);
      final StreamConsumer outputConsumer = new StreamConsumer(process.getInputStream(), LOGGER);
      errorConsumer.setName("errorConsumer");
      errorConsumer.start();
      outputConsumer.setName("outputConsumer");
      outputConsumer.start();
      int exitVal = process.waitFor();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("exitVal: " + exitVal);
      }

      process.getInputStream().close();
      process.getOutputStream().close();
    } catch (InterruptedException ex) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("interrupted");
      }
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }

  }

  /** Tests the screen launching method.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    LinuxScreenUtils.launchDetachedScreenSession(
            System.getProperty("user.dir"), // workingDirectory
            "ftp", // command
            "test-session"); // sessionName

    //  reed@mccarthy:~/svn/Texai/Utilities$ screen -list
    //  There is a screen on:
    //          22329.test-session	(08/31/2011 11:51:39 AM)	(Detached)
    //  1 Socket in /var/run/screen/S-reed.
    //
    //  reed@mccarthy:~/svn/Texai/Utilities$ screen -R
    //  ... exit the ftp application
    //  [screen is terminating]
    //  reed@mccarthy:~/svn/Texai/Utilities$ screen -ls
    //  No Sockets found in /var/run/screen/S-reed.


  }
}
