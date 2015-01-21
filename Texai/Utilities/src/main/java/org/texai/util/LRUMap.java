/*
 * LRUMap.java
 *
 * Created on Oct 28, 2010, 10:24:20 PM
 *
 * Description: Provides a fixed size cache that evicts the least recently used entry when its capacity is exceeded.
 *
 * Copyright (C) Oct 28, 2010, Stephen L. Reed.
 */
package org.texai.util;

import java.util.LinkedHashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

/**
 * Provides a fixed size cache that evicts the least recently used entry when its capacity is exceeded.
 *
 * @param <K> the key class
 * @param <V> the value class
 *
 * @author reed
 */
@NotThreadSafe
public class LRUMap<K, V> extends LinkedHashMap<K, V> {

  // the serial version UID
  private static final long serialVersionUID = 1L;
  // the maximum capacity of the cache
  private final int maxCapacity;

  /**
   * Constructs a new LRUMap instance.
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

  /**
   * Returns <tt>true</tt> if this map should remove its eldest entry. This method is invoked by <tt>put</tt> and <tt>putAll</tt> after
   * inserting a new entry into the map. It provides the implementor with the opportunity to remove the eldest entry each time a new one is
   * added. This is useful if the map represents a cache: it allows the map to reduce memory consumption by deleting stale entries.
   *
   * <p>
   * Sample use: this override will allow the map to grow up to 100 entries and then delete the eldest entry each time a new entry is added,
   * maintaining a steady state of 100 entries.
   * <pre>
   *     private static final int MAX_ENTRIES = 100;
   *
   *     protected boolean removeEldestEntry(Map.Entry eldest) {
   *        return size() &gt; MAX_ENTRIES;
   *     }
   * </pre>
   *
   * <p>
   * This method typically does not modify the map in any way, instead allowing the map to modify itself as directed by its return value. It
   * <i>is</i> permitted for this method to modify the map directly, but if it does so, it <i>must</i> return
   * <tt>false</tt> (indicating that the map should not attempt any further modification). The effects of returning <tt>true</tt>
   * after modifying the map from within this method are unspecified.
   *
   * <p>
   * This implementation merely returns <tt>false</tt> (so that this map acts like a normal map - the eldest element is never removed).
   *
   * @param eldest The least recently inserted entry in the map, or if this is an access-ordered map, the least recently accessed entry.
   * This is the entry that will be removed it this method returns <tt>true</tt>. If the map was empty prior to the <tt>put</tt> or
   * <tt>putAll</tt> invocation resulting in this invocation, this will be the entry that was just inserted; in other words, if the map
   * contains a single entry, the eldest entry is also the newest.
   *
   * @return   <tt>true</tt> if the eldest entry should be removed from the map; <tt>false</tt> if it should be retained.
   */
  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
    return size() > this.maxCapacity;
  }
}
