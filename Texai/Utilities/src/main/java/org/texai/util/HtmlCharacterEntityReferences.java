/*
 * HtmlCharacterEntityReferences.java
 *
 * Created on Sep 6, 2010, 3:48:28 PM
 *
 * Description: Represents a set of character entity references defined by the HTML 4.0 standard.
 *
 * Copyright (C) Sep 6, 2010, Stephen L. Reed.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/** Represents a set of character entity references defined by the HTML 4.0 standard.
 *
 * <p>A complete description of the HTML 4.0 character set can be found
 * at http://www.w3.org/TR/html4/charset.html.
 *
 * @author Juergen Hoeller
 * @author Martin Kersten
 * @since 1.2.1
 */
@NotThreadSafe
public class HtmlCharacterEntityReferences {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(HtmlCharacterEntityReferences.class);
  private static final char REFERENCE_START = '&';
  private static final String DECIMAL_REFERENCE_START = "&#";
  private static final String HEX_REFERENCE_START = "&#x";
  private static final char REFERENCE_END = ';';
  public static final char CHAR_NULL = (char) -1;
  private static final String PROPERTIES_FILE = "HtmlCharacterEntityReferences.properties";
  private final String[] characterToEntityReferenceMap = new String[3000];
  private final Map<String, Character> entityReferenceToCharacterMap = new HashMap<>(252);

  /** Maps a new set of character entity references reflecting the HTML 4.0 character set. */
  public HtmlCharacterEntityReferences() {
    final Properties entityReferences = new Properties();

    // load reference definition file
    final InputStream inputStream = getClass().getResourceAsStream(PROPERTIES_FILE);
    if (inputStream == null) {
      throw new IllegalStateException("Cannot find reference definition file [HtmlCharacterEntityReferences.properties] as class path resource");
    }
    try {
      try {
        entityReferences.load(inputStream);
      } finally {
        inputStream.close();
      }
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to parse reference definition file [HtmlCharacterEntityReferences.properties]: " + ex.getMessage());
    }

    // parse reference definition properitesp
    Enumeration<?> keys = entityReferences.propertyNames();
    while (keys.hasMoreElements()) {
      final String key = (String) keys.nextElement();
      final int referredChar = Integer.parseInt(key);
      assert (referredChar < 1000 || (referredChar >= 8000 && referredChar < 10000)) :
              "Invalid reference to special HTML entity: " + referredChar;
      final int index = (referredChar < 1000 ? referredChar : referredChar - 7000);
      final String reference = entityReferences.getProperty(key);
      characterToEntityReferenceMap[index] = REFERENCE_START + reference + REFERENCE_END;
      LOGGER.debug("index: " + index + " --> " + REFERENCE_START + reference + REFERENCE_END);
      final Character ch = (char) referredChar;
      entityReferenceToCharacterMap.put(reference, ch);
      LOGGER.debug(reference + " --> " + ch);
    }
  }

  /** Returns the number of supported entity references.
   *
   * @return the number of supported entity references
   */
  public int getSupportedReferenceCount() {
    return this.entityReferenceToCharacterMap.size();
  }

  /** Returns whether the given character is mapped to a supported entity reference.
   *
   * @param character the given character
   * @return whether the given character is mapped to a supported entity reference
   */
  public boolean isMappedToReference(final char character) {
    return (convertToReference(character) != null);
  }

  /** Returns the reference mapped to the given character or <code>null</code>.
   *
   * @param character the given character
   * @return the reference mapped to the given character or <code>null</code>
   */
  public String convertToReference(final char character) {
    if (character < 1000 || (character >= 8000 && character < 10000)) {
      int index = (character < 1000 ? character : character - 7000);
      String entityReference = characterToEntityReferenceMap[index];
      if (entityReference != null) {
        return entityReference;
      }
    }
    return null;
  }

  /** Returns the char mapped to the given entityReference or -1.
   *
   * @param entityReference the entity reference
   * @return the char mapped to the given entityReference or -1
   */
  public char convertToCharacter(final String entityReference) {
    //Preconditions
    assert entityReference != null : "entityReference must not be null";

    Character referredCharacter = entityReferenceToCharacterMap.get(entityReference);
    if (referredCharacter != null) {
      return referredCharacter;
    }
    return CHAR_NULL;
  }
}
