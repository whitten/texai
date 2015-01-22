/*
 * CacheInitializer.java
 *
 * Created on January 30, 2007, 9:44 AM
 *
 * Description: Provides a static method to initialize the system caches.
 *
 * Copyright (C) 2007 Stephen L. Reed.
 */
package org.texai.kb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.ThreadSafe;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

/** This class has static methods that initialize the ehcache caches used by the semantic annotation persistence framework.
 * The Constants class contains a list of cache names and initializeCaches() method iterates over those.  New caches may be added without
 * modifying this class.
 *
 * @author reed
 */
@ThreadSafe
public final class CacheInitializer {

  /** indicates whether the caches have been initialized */
  private static boolean areCachesInitialized = false;
  /** the cache access lock */
  private static final Lock cacheAccessLock = new ReentrantLock();
  /** the list of dynamically named caches */
  private static final List<String> dynamicallyNamedCacheList = new ArrayList<>();

  /** This class has only static methods and is never instantiated. */
  private CacheInitializer() {
  }

  /** Adds the given named cache to the list of dynamically named caches.
   *
   * @param namedCache the named cache
   */
  public static void addNamedCache(final String namedCache) {
    //Preconditions
    assert namedCache != null : "namedCache must not be null";
    assert !namedCache.isEmpty() : "namedCache must not be empty";

    try {
      cacheAccessLock.lock();
      assert namedCache != null : "namedCache must not be null";
      assert !namedCache.isEmpty() : "namedCache must not be empty";
      if (!dynamicallyNamedCacheList.contains(namedCache)) {
        initializeCache(namedCache);
        dynamicallyNamedCacheList.add(namedCache);
      }
    } finally {
      cacheAccessLock.unlock();
    }
  }

  /** Adds the given array of named caches to the list of dynamically named caches.
   *
   * @param namedCaches the named caches
   */
  public static void addNamedCaches(final String[] namedCaches) {
    //Preconditions
    assert namedCaches != null : "namedCaches must not be null";

    try {
      cacheAccessLock.lock();
      for (final String namedCache : namedCaches) {
        assert namedCache != null : "namedCache must not be null";
        assert !namedCache.isEmpty() : "namedCache must not be empty";
        if (!dynamicallyNamedCacheList.contains(namedCache)) {
          if (areCachesInitialized) {
            initializeCache(namedCache);
          }
          dynamicallyNamedCacheList.add(namedCache);
        }
      }
    } finally {
      cacheAccessLock.unlock();
    }
  }

  /** Initializes the system caches if not yet done. */
  public static void initializeCaches() {
    try {
      cacheAccessLock.lock();
      if (!areCachesInitialized) {
        areCachesInitialized = true;
        for (final String namedCache : dynamicallyNamedCacheList) {
          initializeCache(namedCache);
        }
      }
    } finally {
      cacheAccessLock.unlock();
    }
  }

  /** Resets the system caches. */
  public static void resetCaches() {
    try {
      cacheAccessLock.lock();
      areCachesInitialized = false;
      final CacheManager cacheManager = CacheManager.getInstance();
      for (final String namedCache : dynamicallyNamedCacheList) {
        if (cacheManager.cacheExists(namedCache)) {
          final Cache cache = cacheManager.getCache(namedCache);
          cache.removeAll();
          cache.clearStatistics();
        } else {
          cacheManager.addCache(namedCache);
        }
      }
    } finally {
      cacheAccessLock.unlock();
    }
  }

  /**
   * Resets the given cache.
   *
   * @param namedCache the name of the given cache
   */
  public static void resetCache(final String namedCache) {
    //preconditions
    assert namedCache != null : "namedCache must not be null";
    assert !namedCache.isEmpty() : "namedCache must not be empty";

    cacheAccessLock.lock();
    try {
      final CacheManager cacheManager = CacheManager.getInstance();
      if (cacheManager.cacheExists(namedCache)) {
        final Cache cache = cacheManager.getCache(namedCache);
        cache.removeAll();
        cache.clearStatistics();
      } else {
        cacheManager.addCache(namedCache);
      }
    } finally {
      cacheAccessLock.unlock();
    }
  }

  /** Initializes the given cache.
   *
   * @param namedCache the name of the given cache
   */
  private static void initializeCache(final String namedCache) {
    //preconditions
    assert namedCache != null : "namedCache must not be null";
    assert !namedCache.isEmpty() : "namedCache must not be empty";

    final CacheManager cacheManager = CacheManager.getInstance();
    if (!cacheManager.cacheExists(namedCache)) {
      cacheManager.addCache(namedCache);
    }
  }
}
