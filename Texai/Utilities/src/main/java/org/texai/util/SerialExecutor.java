/*
 * SerialExecutor.java
 *
 * Created on Mar 17, 2010, 5:52:02 PM
 *
 * Description: Provides a serial executor.
 *
 * Adapted from the example provided in the Javadoc for the java.util.concurrent.Executor class.
 *
 * Copyright (C) Mar 17, 2010 reed.
 */
package org.texai.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a serial executor.
 *
 * @author reed
 */
@NotThreadSafe
public class SerialExecutor implements Executor {

  // the tasks queue
  private final Queue<Runnable> tasks = new ArrayDeque<>();
  // the executor that serially executes the tasks
  private final Executor executor;
  // the active task
  Runnable activeTask;

  /**
   * Constructs a new SerialExecutor instance.
   *
   * @param executor the executor that serially executes the tasks
   */
  public SerialExecutor(final Executor executor) {
    //Preconditions
    assert executor != null : "executor must not be null";

    this.executor = executor;
  }

  /**
   * Executes the given command at some time in the future.
   *
   * @param task the runnable task
   */
  @Override
  public synchronized void execute(final Runnable task) {
    //Preconditions
    assert task != null : "task must not be null";

    tasks.offer((Runnable) new Runnable() {

      @Override
      public void run() {
        try {
          task.run();
        } finally {
          scheduleNext();
        }
      }
    });
    if (activeTask == null) {
      scheduleNext();
    }
  }

  /**
   * Schedules the next task.
   */
  private synchronized void scheduleNext() {
    activeTask = tasks.poll();
    if (activeTask != null) {
      executor.execute(activeTask);
    }
  }
}
