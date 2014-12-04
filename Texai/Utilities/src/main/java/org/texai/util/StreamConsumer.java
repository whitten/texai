/*
 * StreamConsumer.java
 *
 * Created on Aug 31, 2011, 11:03:18 AM
 *
 * Description: Provides a stream consumer that logs its consumed stream.
 *
 * Copyright (C) Aug 31, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/**
 * Provides a stream consumer that logs its consumed stream.
 *
 * @author reed
 */
@NotThreadSafe
public class StreamConsumer extends Thread {

  // the input stream that consumes the launched process standard output or standard error stream
  private final InputStream inputStream;
  // the logger
  private final Logger logger;

  /**
   * Constructs a new StreamConsumer instance.
   *
   * @param inputStream the input stream
   * @param logger the logger
   */
  public StreamConsumer(
          final InputStream inputStream,
          final Logger logger) {
    //Preconditions
    assert inputStream != null : "inputStream must not be null";
    assert logger != null : "logger must not be null";

    this.inputStream = inputStream;
    this.logger = logger;
  }

  /**
   * Runs this stream consumer.
   */
  @Override
  public void run() {
    try {
      final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      @SuppressWarnings("UnusedAssignment")
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        logger.info("> " + line);
      }
    } catch (IOException ioe) {
        logger.info(ioe.getMessage());
    }
  }
}
