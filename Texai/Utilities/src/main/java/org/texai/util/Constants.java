/*
 * Constants.java
 *
 * Created on Jan 15, 2010, 8:15:30 AM
 *
 * Description: Provides utility constants.
 *
 * Copyright (C) Jan 15, 2010 reed.
 */
package org.texai.util;

/**
 * Provides utility constants.
 *
 * @author reed
 */
public final class Constants {

  // but will eventually get assigned by IANA
  public static final int HTTP_SERVER_PORT = 53110;
  // the lowest IANA Dynamic and/or Private Port
  public static final int LOWEST_DYNAMIC_OR_PRIVATE_PORT = 49152;
  // the highest IANA Dynamic and/or Private Port
  public static final int HIGHEST_DYNAMIC_OR_PRIVATE_PORT = 65535;

  /**
   * This class is never instantiated.
   */
  private Constants() {
  }
}
