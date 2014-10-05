/*
 * Patterns.java
 *
 * Created on Nov 2, 2011, 1:07:25 PM
 *
 * Description: Provides text-matching patterns.
 *
 * Copyright (C) Nov 2, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/**
 * Provides text-matching patterns.
 *
 * @author reed
 */
@NotThreadSafe
public class Patterns {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(Patterns.class);
  // the pattern dictionary, pattern --> operation
  private final Map<Pattern, String> patternDictionary = new HashMap<>();
  // the matched operation
  private String matchedOperation;
  // the matcher
  private Matcher matchedMatcher;

  /**
   * Constructs a new Patterns instance.
   */
  public Patterns() {
  }

  /**
   * Creates a pattern from the the given pattern string and adds it to the patterns.
   *
   * @param operation the operation associated with the pattern
   * @param patternString the given pattern string
   */
  public void addPattern(
          final String operation,
          final String patternString) {
    //Preconditions
    assert operation != null : "operation must not be null";
    assert !operation.isEmpty() : "operation must not be empty";
    assert patternString != null : "patternString must not be null";
    assert !patternString.isEmpty() : "patternString must not be empty";

    synchronized (patternDictionary) {
      patternDictionary.put(Pattern.compile(patternString), operation);
    }
  }

  /**
   * Returns whether the given input string was matched by a pattern.
   *
   * @param input the given input string
   *
   * @return whether the given input string was matched by a pattern
   */
  public boolean matches(final String input) {
    //Preconditions
    assert input != null : "input must not be null";
    assert !input.isEmpty() : "input must not be empty";

    matchedOperation = null;
    matchedMatcher = null;
    for (final Entry<Pattern, String> entry : patternDictionary.entrySet()) {
      final Pattern pattern = entry.getKey();
      final String operation = entry.getValue();
      final Matcher matcher = pattern.matcher(input);
      if (matcher.matches()) {
        matchedMatcher = matcher;
        matchedOperation = operation;
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the matched operation.
   *
   * @return the matched operation
   */
  public String getMatchedOperation() {
    return matchedOperation;
  }

  /**
   * Gets the matched groups.
   *
   * @return the matched groups
   */
  public List<String> getMatchedGroups() {
    //Preconditions
    assert matchedMatcher != null : "matchedMatcher must not be null";

    final List<String> matchedGroups = new ArrayList<>();
    final int groupCnt = matchedMatcher.groupCount();
    for (int i = 1; i <= groupCnt; i++) {
      matchedGroups.add(matchedMatcher.group(i));
    }
    return matchedGroups;
  }
}
