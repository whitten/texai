/*
 * UUIDGenerator.java
 *
 * Created on April 19, 2007, 2:09 PM
 *
 * Description: Generates a UUID.
 *
 * Copyright (C) 2007 Stephen L. Reed.
 */
package org.texai.util;

import java.util.UUID;
import org.apache.log4j.Logger;

/**
 * Generates a UUID.
 *
 * @author reed
 */
public final class UUIDGenerator {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(UUIDGenerator.class);

  /**
   * Creates a new instance of UUIDGenerator.
   */
  public UUIDGenerator() {
    super();
  }

  /**
   * Generates a UUID.
   */
  public void generateUUID() {
    final UUID uuid = UUID.randomUUID();
    LOGGER.info("generated UUID: " + uuid);
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments (unused)
   */
  public static void main(final String[] args) {
    final UUIDGenerator uuidGenerator = new UUIDGenerator();
    uuidGenerator.generateUUID();
  }
}
