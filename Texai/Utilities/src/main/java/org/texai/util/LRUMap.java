/*
 * LRUMap.java
 *
 * Created on Oct 28, 2010, 10:24:20 PM
 *
 * Description: Provides a fixed size cache that evicts the least recently used entry when its capacity is exceeded.
 *
 * Copyright (C) Oct 28, 2010, Stephen L. Reed.
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

import java.util.LinkedHashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

/** Provides a fixed size cache that evicts the least recently used entry when its capacity is exceeded.
 *
 * @param <K> the key class
 * @param <V> the value class
 * @author reed
 */
@NotThreadSafe
public class LRUMap<K, V> extends LinkedHashMap<K, V> {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;
  /** the maximum capacity of the cache */
  private final int maxCapacity;

  /** Constructs a new LRUMap instance.
   *
   * @param initialCapacity the initial capacity of the cache
   * @param maxCapacity the maximum capacity of the cache
   */
  public LRUMap(final int initialCapacity, final int maxCapacity) {
    super(
            initialCapacity,
            0.75f, // loadFactor
            true);  // accessOrder
    this.maxCapacity = maxCapacity;
  }

  /** Returns <tt>true</tt> if this map should remove its eldest entry.
   * This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
   * inserting a new entry into the map.
   *
   * @param    eldest The least recently inserted entry in the map, or if
   *           this is an access-ordered map, the least recently accessed
   *           entry.  This is the entry that will be removed it this
   *           method returns <tt>true</tt>.  If the map was empty prior
   *           to the <tt>put</tt> or <tt>putAll</tt> invocation resulting
   *           in this invocation, this will be the entry that was just
   *           inserted; in other words, if the map contains a single
   *           entry, the eldest entry is also the newest.
   * @return   <tt>true</tt> if the eldest entry should be removed
   *           from the map; <tt>false</tt> if it should be retained.
   */
  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > this.maxCapacity;
  }
}
