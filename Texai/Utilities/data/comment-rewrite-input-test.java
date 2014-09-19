/*
 * AHCSConstants.java
 *
 * Created on Jun 26, 2009, 2:32:17 PM
 *
 * Description: Provides constants for the Albus hierarchical control system.
 *
 */
package org.texai.ahcsSupport;

import net.jcip.annotations.NotThreadSafe;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.texai.kb.Constants;

/**
 * Provides constants for the Albus hierarchical control system.
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public final class AHCSConstants {

  /**
   * comment 1
   * comment 2
   */
  public enum State {

    UNINITIALIZED,
    INITIALIZED,
    READY,
    SHUTDOWN
  }

  //
  // sensation messages - end with _Sensation, and are sent from a child node to its parent node
  //
  /**
   * the exitApplication_Sensation operation
   */
  public static final String AHCS_EXIT_APPLICATION_SENSATION = "AHCS exitApplication_Sensation";
  //
  /** the texai:role_name term */
  public static final URI ROLE_NAME_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "role_name");
  /**
   * the texai:skillClass_skillClassName term */
  public static final URI SKILL_CLASS_SKILL_CLASS_NAME_TERM = new URIImpl(Constants.TEXAI_NAMESPACE + "skillClass_skillClassName");

  /**
   * Prevents the construction this utility class.
   */
  private AHCSConstants() {
  }

  /**
   * a method.
   */
  private void aMethod() {
    // a single line comment
    /**
     * a block comment
     */
    return;
  }

}
