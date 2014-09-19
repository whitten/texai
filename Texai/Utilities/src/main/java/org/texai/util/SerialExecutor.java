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
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.util;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import net.jcip.annotations.NotThreadSafe;

/** Provides a serial executor.
 *
 * @author reed
 */
@NotThreadSafe
public class SerialExecutor implements Executor {

  /** the tasks queue */
  private final Queue<Runnable> tasks = new ArrayDeque<>();
  /** the executor that serially executes the tasks */
  private final Executor executor;
  /** the active task */
  Runnable activeTask;

  /** Constructs a new SerialExecutor instance.
   *
   * @param executor the executor that serially executes the tasks
   */
  public SerialExecutor(final Executor executor) {
    //Preconditions
    assert executor != null : "executor must not be null";

    this.executor = executor;
  }

  /** Executes the given command at some time in the future.
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

  /** Schedules the next task. */
  private synchronized void scheduleNext() {
    activeTask = tasks.poll();
    if (activeTask != null) {
      executor.execute(activeTask);
    }
  }
}
