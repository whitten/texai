package org.texai.skill.aicoin.support;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.texai.util.EnvironmentUtils;
import org.texai.util.TexaiException;

/**
 * AICoinUtils.java
 *
 * Description:
 *
 * Copyright (C) Nov 21, 2014, Stephen L. Reed, Texai.org.
 *
 * See the file "LICENSE" for the full license governing this code.
 */
public class AICoinUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(AICoinUtils.class);

  /**
   * Prevents the instantiation of BitcoinUtils.
   */
  private AICoinUtils() {
  }

  /**
   * Executes a host command using the specified command strings.
   *
   * @param cmdArray the specified command strings
   */
  public synchronized static void executeHostCommand(final String[] cmdArray) {
    //Preconditions
    assert cmdArray != null : "cmdArray must not be null";
    assert cmdArray.length > 0 : "cmdArray must not be empty";
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("must be running on Linux");
    }

    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
      final Process process = processBuilder.start();
      int exitVal = process.waitFor();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("exitVal: " + exitVal);
      }
    } catch (final IOException | InterruptedException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Executes a host command using the specified command strings, without waiting for the command to finish.
   *
   * @param cmdArray the specified command strings
   * @return the started host process
   *
    */
  public synchronized static Process executeHostCommandWithoutWaitForCompletion(final String[] cmdArray) {
    //Preconditions
    assert cmdArray != null : "cmdArray must not be null";
    assert cmdArray.length > 0 : "cmdArray must not be empty";
    if (!EnvironmentUtils.isLinux()) {
      throw new TexaiException("must be running on Linux");
    }

    try {
      final ProcessBuilder processBuilder = new ProcessBuilder(cmdArray);
      processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
      return processBuilder.start();
    } catch (final IOException ex) {
      throw new TexaiException(ex);
    }
  }
}
