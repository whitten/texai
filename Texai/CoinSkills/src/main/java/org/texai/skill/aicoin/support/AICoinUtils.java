package org.texai.skill.aicoin.support;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.texai.util.EnvironmentUtils;
import org.texai.util.StreamConsumer;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * AICoinUtils.java

 Description:

 Copyright (C) Nov 21, 2014, Stephen L. Reed, Texai.org.
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
}
