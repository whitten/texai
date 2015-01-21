/*
 * ExecutorUtils.java
 *
 * Created on Mar 30, 2010, 10:28:46 AM
 *
 * Description: Provides executor utilities.
 *
 * Copyright (C) Mar 30, 2010, Stephen L. Reed.
 */
package org.texai.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides executor utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class ExecutorUtils {

  // the executor service
  private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

  /**
   * Prevents the instantiation of this utility class.
   */
  private ExecutorUtils() {

  }

  /**
   * Gets the executor service.
   *
   * @return the executorService
   */
  public static ExecutorService getExecutorService() {
    return EXECUTOR_SERVICE;
  }

}
