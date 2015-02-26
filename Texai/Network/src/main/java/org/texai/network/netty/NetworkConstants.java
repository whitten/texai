/*
 * NetworkConstants.java
 *
 * Description: Provides network constants.
 *
 * Copyright (C) Feb 3, 2010 by Stephen Reed.
 *
 */
package org.texai.network.netty;

import net.jcip.annotations.ThreadSafe;

/**
 * Provides network constants.
 *
 * @author reed
 */
@ThreadSafe
public final class NetworkConstants {

  // the object serialization protocol identification byte
  public static final byte OBJECT_SERIALIZATION_PROTOCOL = 1;

  /**
   * Prevents the instantiation of this utility class.
   */
  private NetworkConstants() {
  }
}
