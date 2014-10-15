/*
 * SessionInfo.java
 *
 * Created on Feb 13, 2012, 12:33:38 PM
 *
 * Description: Provides container for session information.
 *
 * Copyright (C) Feb 13, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.ahcsSupport;

import java.util.Objects;
import net.jcip.annotations.NotThreadSafe;
import org.texai.util.StringUtils;

/** Provides container for session information.
 *
 * @author reed
 */
@NotThreadSafe
public class SessionInfo {

  // the session handle
  private final String sessionHandle;

  /** Constructs a new SessionInfo instance.
   *
   * @param sessionHandle the session handle
   */
  public SessionInfo(final String sessionHandle) {
    //Preconditions
    assert StringUtils.isNonEmptyString(sessionHandle) : "sessionHandle must be a non-empty string";

    this.sessionHandle = sessionHandle;
  }

  /** Gets the session handle.
   *
   * @return the session handle
   */
  public String getSessionHandle() {
    return sessionHandle;
  }

  /** Returns whether some other object equals this one.
   *
   * @param obj the other object
   * @return whether some other object equals this one
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final SessionInfo other = (SessionInfo) obj;
    return Objects.equals(this.sessionHandle, other.sessionHandle);
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    int hash = 3;
    hash = 29 * hash + Objects.hashCode(this.sessionHandle);
    return hash;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return (new StringBuilder()).append("[session ").append(sessionHandle).append(']').toString();
  }
}
