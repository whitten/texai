/*
 * ThreadUtils.java
 *
 * Created on Feb 26, 2010, 12:10:25 PM
 *
 * Description: Provides thread utility methods.
 *
 * Copyright (C) Feb 26, 2010 reed.
 */
package org.texai.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;

/**
 *
 * @author reed
 */
@ThreadSafe
public final class ThreadUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ThreadUtils.class);
  // the root JVM thread group
  private static ThreadGroup rootThreadGroup = null;

  /**
   * Prevents the instantiation of this utility class.
   */
  private ThreadUtils() {
  }

  /**
   * Returns a list of all threads.
   *
   * @return a list of all threads
   */
  public static List<Thread> getAllThreads() {
    final ThreadGroup root = getRootThreadGroup();
    final ThreadMXBean thbean = ManagementFactory.getThreadMXBean();
    int nAlloc = thbean.getThreadCount();
    @SuppressWarnings("UnusedAssignment")
    int n = 0;
    Thread[] threads;
    do {
      nAlloc *= 2;
      threads = new Thread[nAlloc];
      n = root.enumerate(threads, true);
    } while (n == nAlloc);
    final List<Thread> threadList = new ArrayList<>();
    for (final Thread thread : threads) {
      if (thread != null) {
        threadList.add(thread);
      }
    }
    return threadList;
  }

  /**
   * Returns the root thread group.
   *
   * @return the root thread group
   */
  public synchronized static ThreadGroup getRootThreadGroup() {
    if (rootThreadGroup == null) {
      rootThreadGroup = Thread.currentThread().getThreadGroup();
      ThreadGroup parentThreadGroup;
      while ((parentThreadGroup = rootThreadGroup.getParent()) != null) {
        rootThreadGroup = parentThreadGroup;
      }
    }
    return rootThreadGroup;
  }

  /**
   * Logs all threads.
   */
  public static void logThreads() {
    LOGGER.info("all threads ...");
    for (final Thread thread : getAllThreads()) {
      LOGGER.info("  " + thread.getName());
    }
  }
}
