/*
 * HTTPUtils.java
 *
 * Created on Jan 29, 2010, 9:21:55 AM
 *
 * Description: Provides HTTP utilities.
 *
 * Copyright (C) Jan 29, 2010 reed.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/** Provides HTTP utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class HTTPUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(HTTPUtils.class);
  /** the HTTP response buffer size */
  public static final int HTTP_RESPONSE_BUFFER_SIZE = 2048;

  /** Prevents the instantiation of this utility class. */
  private HTTPUtils() {
  }

  /** Consumes bytes from the given pushback input stream and returns the HTTP message when the end of it is reached.  The
   * input stream remains open for the next message in the pipeline.
   * Per: http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6
   *
   * @param pushbackInputStream the given pushback input stream
   * @return the HTTP message
   */
  public static byte[] consumeHTTPMessage(final PushbackInputStream pushbackInputStream) {
    //Preconditions
    assert pushbackInputStream != null : "inputStream must not be null";

    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    byte previousByte = -1;
    final byte[] buffer = new byte[HTTP_RESPONSE_BUFFER_SIZE];
    int nbrBytesRead = 0;
    int index = 0;
    int contentLength = 0;
    final StringBuilder stringBuilder = new StringBuilder();
    try {

      // Consume the status lines and response headers, each which is terminated by CRLF.
      // Parse the Content-Length header to obtain the length of the subsequent message body.
      // An empty line terminates the headers.
      while (true) {
        if (index + 1 > nbrBytesRead) {
          nbrBytesRead = pushbackInputStream.read(buffer);
          if (nbrBytesRead == -1) {
            throw new TexaiException("unexpected end of stream while reading response headers");
          }
          index = 0;
        }
        final byte currentByte = buffer[index++];
        byteArrayOutputStream.write(currentByte);
        stringBuilder.append((char) currentByte);
        if (previousByte == '\r' && currentByte == '\n') {
          final String headerLine = stringBuilder.substring(0, stringBuilder.length() - 2);
          stringBuilder.setLength(0);
          LOGGER.debug("headerLine: " + headerLine);
          if (headerLine.isEmpty()) {
            break;
          } else if (headerLine.startsWith("Content-Length:")) {
            contentLength = Integer.parseInt(headerLine.substring(15).trim());
          }
        }
        previousByte = currentByte;
      }

      // consume the message body
      for (int i = 0; i < contentLength; i++) {
        if (index + 1 > nbrBytesRead) {
          nbrBytesRead = pushbackInputStream.read(buffer);
          if (nbrBytesRead == -1) {
            throw new TexaiException("unexpected end of stream while reading response headers");
          }
          index = 0;
        }
        final byte currentByte = buffer[index++];
        //LOGGER.info("body: " + String.valueOf((char) currentByte));
        byteArrayOutputStream.write(currentByte);
      }

      // push back any remaining bytes in the buffer
      final int nbrBytesRemaining = nbrBytesRead - index;
      if (nbrBytesRemaining > 0) {
        pushbackInputStream.unread(buffer, index, nbrBytesRead - index);
      }
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    return byteArrayOutputStream.toByteArray();
  }

  /** Returns the parameter dictionary, parameter --> value, parsed from the given well-formed HTTP query.
   *
   * @param query the given well-formed HTTP query
   * @return the parameter dictionary, parameter --> value
   */
  public static Map<String, String> getQueryMap(final String query) {
    //Preconditions
    assert query != null : "query must not be null";
    assert !query.isEmpty() : "query must not be empty";

    final String[] parameterAssignments = query.split("&");
    final Map<String, String> parameterDictionary = new HashMap<>();
    for (String parameterAssignment : parameterAssignments) {
      final String parameter = parameterAssignment.split("=")[0];
      final String value = parameterAssignment.split("=")[1];
      parameterDictionary.put(parameter, value);
    }
    return parameterDictionary;
  }

  /** Gets the version number from the given user-agent string.
   *
   * @param userAgent the user-agent string
   * @param position the position
   * @return the version number
   */
  @SuppressWarnings("fallthrough")
  public static String getVersionNumber(final String userAgent, final int position) {
    int position1 = position;
    if (position1 < 0) {
      return "";
    }
    final StringBuilder stringBuilder = new StringBuilder();
    int status = 0;

    while (position1 < userAgent.length()) {
      final char ch = userAgent.charAt(position1);
      switch (status) {
        case 0: // No valid digits encountered yet
          if (ch == ' ' || ch == '/') {
            break;
          }
          if (ch == ';' || ch == ')') {
            return "";
          }
          status = 1; // intentional fallthrough to case 1
        case 1: // Version number in progress
          if (ch == ';' || ch == '/' || ch == ')' || ch == '(' || ch == '[') {
            return stringBuilder.toString().trim();
          }
          if (ch == ' ') {
            status = 2;
          }
          stringBuilder.append(ch);
          break;
        case 2: // Space encountered - Might need to end the parsing
          if ((Character.isLetter(ch)
                  && Character.isLowerCase(ch))
                  || Character.isDigit(ch)) {
            stringBuilder.append(ch);
            status = 1;
          } else {
            return stringBuilder.toString().trim();
          }
          break;
      }
      position1++;
    }
    return stringBuilder.toString().trim();
  }

  /** Gets the first version number in the given user-agent string, truncated to the given number of digits
   *
   * @param userAgent the user agent string
   * @param position the position
   * @param numDigits the number of digits to truncate
   * @return
   */
  private static String getFirstVersionNumber(final String userAgent, final int position, final int numDigits) {
    final String versionNumber = getVersionNumber(userAgent, position);
    if (versionNumber == null) {
      return "";
    }
    int i = 0;
    String truncatedVersionNumber = "";
    while (i < versionNumber.length() && i < numDigits) {
      truncatedVersionNumber += String.valueOf(versionNumber.charAt(i));
      i++;
    }
    return truncatedVersionNumber;
  }

  /** Returns a string array of the given three strings.
   *
   * @param string1 the first string
   * @param string2 the second string
   * @param string3 the third string
   * @return a string array of the given three strings
   */
  private static String[] getArray(final String string1, final String string2, final String string3) {
    final String[] stringArray = new String[3];
    stringArray[0] = string1;
    stringArray[1] = string2;
    stringArray[2] = string3;
    return stringArray;
  }

  /** Returns the name and version of the bot if the given user-agent is a bot.
   *
   * @param userAgent the given user-agent string
   * @return the name of the bot if the given user-agent is a bot, otherwise returns null
   */
  @SuppressWarnings("UnusedAssignment")
  public static String[] getBotName(final String userAgent) {
    final String userAgentLowerCase = userAgent.toLowerCase();
    int position = 0;
    final String botName;
    if ((position = userAgentLowerCase.indexOf("google/")) > -1) {
      botName = "Google";
      position += 7;
    } else if ((position = userAgentLowerCase.indexOf("msnbot/")) > -1) {
      botName = "MSNBot";
      position += 7;
    } else if ((position = userAgentLowerCase.indexOf("googlebot/")) > -1) {
      botName = "Google";
      position += 10;
    } else if ((position = userAgentLowerCase.indexOf("webcrawler/")) > -1) {
      botName = "WebCrawler";
      position += 11;
    } else if ((position = userAgentLowerCase.indexOf("gulper web bot")) > -1) {
      botName = "Gulper";
      position += 15;
    } else // The following bots don't have any version number in their User-Agent strings.
    if ((position = userAgentLowerCase.indexOf("inktomi")) > -1) {
      botName = "Inktomi";
      position = -1;
    } else if ((position = userAgentLowerCase.indexOf("teoma")) > -1) {
      botName = "Teoma";
      position = -1;
    } else if ((position = userAgentLowerCase.indexOf("yahoo! slurp")) > -1) {
      botName = "Yahoo";
      position = -1;
    } else if ((position = userAgentLowerCase.indexOf("scoutjet")) > -1) {
      botName = "ScoutJet";
      position = -1;
    } else {
      return null;
    }
    return getArray(botName, botName, botName + getVersionNumber(userAgentLowerCase, position));
  }

  /** Gets the client operating system from the given user-agent string.
   *
   * @param userAgent the given user-agent string
   * @return the client operating system
   */
  @SuppressWarnings("UnusedAssignment")
  public static String[] getOS(final String userAgent) {
    if (getBotName(userAgent) != null) {
      return getArray("Bot", "Bot", "Bot");
    }
    String[] result = null;
    int position;
    if ((position = userAgent.indexOf("Windows-NT")) > -1) {
      result = getArray("Win", "WinNT", "Win" + getVersionNumber(userAgent, position + 8));
    } else if (userAgent.contains("Windows NT")) {
      // The different versions of Windows NT are decoded in the verbosity level 2
      // ie: Windows NT 5.1 = Windows XP
      if ((position = userAgent.indexOf("Windows NT 5.1")) > -1) {
        result = getArray("Win", "WinXP", "Win" + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT 6.0")) > -1) {
        result = getArray("Win", "Vista", "Vista" + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT 5.0")) > -1) {
        result = getArray("Win", "Seven", "Seven " + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT 5.0")) > -1) {
        result = getArray("Win", "Win2000", "Win" + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT 5.2")) > -1) {
        result = getArray("Win", "Win2003", "Win" + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT 4.0")) > -1) {
        result = getArray("Win", "WinNT4", "Win" + getVersionNumber(userAgent, position + 7));
      } else if ((position = userAgent.indexOf("Windows NT)")) > -1) {
        result = getArray("Win", "WinNT", "WinNT");
      } else if ((position = userAgent.indexOf("Windows NT;")) > -1) {
        result = getArray("Win", "WinNT", "WinNT");
      } else {
        result = getArray("Win", "<b>WinNT?</b>", "<b>WinNT?</b>");
      }
    } else if (userAgent.contains("Win")) {
      if (userAgent.contains("Windows")) {
        if ((position = userAgent.indexOf("Windows 98")) > -1) {
          result = getArray("Win", "Win98", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows_98")) > -1) {
          result = getArray("Win", "Win98", "Win" + getVersionNumber(userAgent, position + 8));
        } else if ((position = userAgent.indexOf("Windows 2000")) > -1) {
          result = getArray("Win", "Win2000", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows 95")) > -1) {
          result = getArray("Win", "Win95", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows 9x")) > -1) {
          result = getArray("Win", "Win9x", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows ME")) > -1) {
          result = getArray("Win", "WinME", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows CE")) > -1) {
          result = getArray("Win", "WinCE", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows XP")) > -1) {
          result = getArray("Win", "WinXP", "Win" + getVersionNumber(userAgent, position + 7));
        } else if ((position = userAgent.indexOf("Windows 3.1")) > -1) {
          result = getArray("Win", "Win31", "Win" + getVersionNumber(userAgent, position + 7));
        }
        // If no version was found, rely on the following code to detect "WinXX"
        // As some User-Agents include two references to Windows
        // Ex: Mozilla/5.0 (Windows; U; Win98; en-US; rv:1.5)
      }
      if (result == null) {
        if ((position = userAgent.indexOf("Win98")) > -1) {
          result = getArray("Win", "Win98", "Win" + getVersionNumber(userAgent, position + 3));
        } else if ((position = userAgent.indexOf("Win31")) > -1) {
          result = getArray("Win", "Win31", "Win" + getVersionNumber(userAgent, position + 3));
        } else if ((position = userAgent.indexOf("Win95")) > -1) {
          result = getArray("Win", "Win95", "Win" + getVersionNumber(userAgent, position + 3));
        } else if ((position = userAgent.indexOf("Win 9x")) > -1) {
          result = getArray("Win", "Win9x", "Win" + getVersionNumber(userAgent, position + 3));
        } else if ((position = userAgent.indexOf("WinNT4.0")) > -1) {
          result = getArray("Win", "WinNT4", "Win" + getVersionNumber(userAgent, position + 3));
        } else if ((position = userAgent.indexOf("WinNT")) > -1) {
          result = getArray("Win", "WinNT", "Win" + getVersionNumber(userAgent, position + 3));
        }
      }
      if (result == null) {
        if ((position = userAgent.indexOf("Windows XP")) > -1) {
          result = getArray("Win", "WinXP", "<b>Win?</b>");
        }
      }
      if (result == null) {
        if ((position = userAgent.indexOf("Windows")) > -1) {
          result = getArray("Win", "<b>Win?</b>", "<b>Win?" + getVersionNumber(userAgent, position + 7) + "</b>");
        } else if ((position = userAgent.indexOf("Win")) > -1) {
          result = getArray("Win", "<b>Win?</b>", "<b>Win?" + getVersionNumber(userAgent, position + 3) + "</b>");
        } else // Should not happen at this point
        {
          result = getArray("Win", "<b>Win?</b>", "<b>Win?</b>");
        }
      }
    } else if ((position = userAgent.indexOf("Mac OS X")) > -1) {
      if ((userAgent.indexOf("iPhone")) > -1) {
        position = userAgent.indexOf("iPhone OS");
        result = getArray("Mac", "MacOSX-iPhone", "MacOS-iPhone " + ((position < 0) ? "" : getVersionNumber(userAgent, position + 9)));
      } else {
        result = getArray("Mac", "MacOSX", "MacOS " + getVersionNumber(userAgent, position + 8));
      }
    } else if ((position = userAgent.indexOf("Mac_PowerPC")) > -1) {
      result = getArray("Mac", "MacPPC", "MacOS " + getVersionNumber(userAgent, position + 3));
    } else if ((position = userAgent.indexOf("Macintosh")) > -1) {
      if (userAgent.contains("PPC")) {
        result = getArray("Mac", "MacPPC", "MacOS?");
      } else {
        result = getArray("Mac?", "Mac?", "MacOS?");
      }
    } else if ((position = userAgent.indexOf("FreeBSD")) > -1) {
      result = getArray("*BSD", "*BSD FreeBSD", "FreeBSD " + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("OpenBSD")) > -1) {
      result = getArray("*BSD", "*BSD OpenBSD", "OpenBSD " + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("Android ")) > -1) {
      result = getArray("Android", "Android", "Android " + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("Linux")) > -1) {
      String detail = "Linux " + getVersionNumber(userAgent, position + 5);
      String med = "Linux";
      if ((position = userAgent.indexOf("Ubuntu/")) > -1) {
        detail = "Ubuntu " + getVersionNumber(userAgent, position + 7);
        med += " Ubuntu";
      }
      result = getArray("Linux", med, detail);
    } else if ((position = userAgent.indexOf("CentOS")) > -1) {
      result = getArray("Linux", "Linux CentOS", "CentOS");
    } else if ((position = userAgent.indexOf("NetBSD")) > -1) {
      result = getArray("*BSD", "*BSD NetBSD", "NetBSD " + getVersionNumber(userAgent, position + 6));
    } else if ((position = userAgent.indexOf("Unix")) > -1) {
      result = getArray("Linux", "Linux", "Linux " + getVersionNumber(userAgent, position + 4));
    } else if ((position = userAgent.indexOf("SunOS")) > -1) {
      result = getArray("Unix", "SunOS", "SunOS" + getVersionNumber(userAgent, position + 5));
    } else if ((position = userAgent.indexOf("IRIX")) > -1) {
      result = getArray("Unix", "IRIX", "IRIX" + getVersionNumber(userAgent, position + 4));
    } else if ((position = userAgent.indexOf("SonyEricsson")) > -1) {
      result = getArray("SonyEricsson", "SonyEricsson", "SonyEricsson" + getVersionNumber(userAgent, position + 12));
    } else if ((position = userAgent.indexOf("Nokia")) > -1) {
      result = getArray("Nokia", "Nokia", "Nokia" + getVersionNumber(userAgent, position + 5));
    } else if ((position = userAgent.indexOf("BlackBerry")) > -1) {
      result = getArray("BlackBerry", "BlackBerry", "BlackBerry" + getVersionNumber(userAgent, position + 10));
    } else if ((position = userAgent.indexOf("SymbianOS")) > -1) {
      result = getArray("SymbianOS", "SymbianOS", "SymbianOS" + getVersionNumber(userAgent, position + 10));
    } else if ((position = userAgent.indexOf("BeOS")) > -1) {
      result = getArray("BeOS", "BeOS", "BeOS");
    } else if ((position = userAgent.indexOf("Nintendo Wii")) > -1) {
      result = getArray("Nintendo Wii", "Nintendo Wii", "Nintendo Wii" + getVersionNumber(userAgent, position + 10));
    } else {
      result = getArray("<b>?</b>", "<b>?</b>", "<b>?</b>");
    }
    return result;
  }

  /** Gets the browser and version number from the given user-agent string.
   *
   * @param userAgent the given user-agent string
   * @return the browser and version number
   */
  @SuppressWarnings("UnusedAssignment")
  public static String[] getBrowser(String userAgent) {
    final String[] botName;
    if ((botName = getBotName(userAgent)) != null) {
      return botName;
    }
    final String[] result;
    int position;
    if ((position = userAgent.indexOf("Lotus-Notes/")) > -1) {
      result = getArray("LotusNotes", "LotusNotes", "LotusNotes" + getVersionNumber(userAgent, position + 12));
    } else if ((position = userAgent.indexOf("Opera")) > -1) {
      result = getArray("Opera", "Opera" + getFirstVersionNumber(userAgent, position + 5, 1), "Opera" + getVersionNumber(userAgent, position + 5));
    } else if ((position = userAgent.indexOf("OmniWeb/")) > -1) {
      result = getArray("OmniWeb", "OmniWeb", "OmniWeb" + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("Debian/")) > -1) {
      result = getArray("Debian", "Debian", "Debian" + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("Epiphany/")) > -1) {
      result = getArray("Epiphany", "Epiphany", "Epiphany" + getVersionNumber(userAgent, position + 9));
    } else if ((position = userAgent.indexOf("Galeon/")) > -1) {
      result = getArray("Galeon", "Galeon", "Galeon" + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("ELinks/")) > -1) {
      result = getArray("ELinks", "ELinks", "ELinks" + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("ELinks (")) > -1) {
      result = getArray("ELinks", "ELinks", "ELinks" + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("Links/")) > -1) {
      result = getArray("Links", "Links", "Links" + getVersionNumber(userAgent, position + 6));
    } else if ((position = userAgent.indexOf("Links (")) > -1) {
      result = getArray("Links", "Links", "Links" + getVersionNumber(userAgent, position + 7));
    } else if ((position = userAgent.indexOf("Lynx/")) > -1) {
      result = getArray("Lynx", "Lynx", "Lynx" + getVersionNumber(userAgent, position + 5));
    } else if ((position = userAgent.indexOf("w3m/")) > -1) {
      result = getArray("w3m", "w3m", "w3m" + getVersionNumber(userAgent, position + 4));
    } else if ((position = userAgent.indexOf("HandHTTP ")) > -1) {
      result = getArray("HandHTTP", "HandHTTP", "HandHTTP" + getVersionNumber(userAgent, position + 9));
    } else if (userAgent.contains("MSIE")) {
      if ((position = userAgent.indexOf("MSIE 6.0")) > -1) {
        result = getArray("MSIE", "MSIE6", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 5.0")) > -1) {
        result = getArray("MSIE", "MSIE5", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 5.5")) > -1) {
        result = getArray("MSIE", "MSIE5.5", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 5.")) > -1) {
        result = getArray("MSIE", "MSIE5.x", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 4")) > -1) {
        result = getArray("MSIE", "MSIE4", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 7")) > -1 && !userAgent.contains("Trident/4.0")) {
        result = getArray("MSIE", "MSIE7", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else if ((position = userAgent.indexOf("MSIE 8")) > -1 || userAgent.contains("Trident/4.0")) {
        result = getArray("MSIE", "MSIE8", "MSIE" + getVersionNumber(userAgent, position + 4));
      } else {
        result = getArray("MSIE", "<b>MSIE?</b>", "<b>MSIE?" + getVersionNumber(userAgent, userAgent.indexOf("MSIE") + 4) + "</b>");
      }
    } else if ((position = userAgent.indexOf("Gecko/")) > -1) {
      result = getArray("Gecko", "Gecko", "Gecko" + getFirstVersionNumber(userAgent, position + 5, 4));
      if ((position = userAgent.indexOf("Camino/")) > -1) {
        result[1] += "(Camino)";
        result[2] += "(Camino" + getVersionNumber(userAgent, position + 7) + ")";
      } else if ((position = userAgent.indexOf("Chimera/")) > -1) {
        result[1] += "(Chimera)";
        result[2] += "(Chimera" + getVersionNumber(userAgent, position + 8) + ")";
      } else if ((position = userAgent.indexOf("Firebird/")) > -1) {
        result[1] += "(Firebird)";
        result[2] += "(Firebird" + getVersionNumber(userAgent, position + 9) + ")";
      } else if ((position = userAgent.indexOf("Phoenix/")) > -1) {
        result[1] += "(Phoenix)";
        result[2] += "(Phoenix" + getVersionNumber(userAgent, position + 8) + ")";
      } else if ((position = userAgent.indexOf("Galeon/")) > -1) {
        result[1] += "(Galeon)";
        result[2] += "(Galeon" + getVersionNumber(userAgent, position + 7) + ")";
      } else if ((position = userAgent.indexOf("Firefox/")) > -1) {
        result[1] += "(Firefox)";
        result[2] += "(Firefox" + getVersionNumber(userAgent, position + 8) + ")";
      } else if ((position = userAgent.indexOf("Netscape/")) > -1) {
        if ((position = userAgent.indexOf("Netscape/6")) > -1) {
          result[1] += "(NS6)";
          result[2] += "(NS" + getVersionNumber(userAgent, position + 9) + ")";
        } else if ((position = userAgent.indexOf("Netscape/7")) > -1) {
          result[1] += "(NS7)";
          result[2] += "(NS" + getVersionNumber(userAgent, position + 9) + ")";
        } else {
          result[1] += "(NS?)";
          result[2] += "(NS?" + getVersionNumber(userAgent, userAgent.indexOf("Netscape/") + 9) + ")";
        }
      }
    } else if ((position = userAgent.indexOf("Netscape/")) > -1) {
      if ((position = userAgent.indexOf("Netscape/4")) > -1) {
        result = getArray("NS", "NS4", "NS" + getVersionNumber(userAgent, position + 9));
      } else {
        result = getArray("NS", "NS?", "NS?" + getVersionNumber(userAgent, position + 9));
      }
    } else if ((position = userAgent.indexOf("Chrome/")) > -1) {
      result = getArray("KHTML", "KHTML(Chrome)", "KHTML(Chrome" + getVersionNumber(userAgent, position + 6) + ")");
    } else if ((position = userAgent.indexOf("Safari/")) > -1) {
      result = getArray("KHTML", "KHTML(Safari)", "KHTML(Safari" + getVersionNumber(userAgent, position + 6) + ")");
    } else if ((position = userAgent.indexOf("Konqueror/")) > -1) {
      result = getArray("KHTML", "KHTML(Konqueror)", "KHTML(Konqueror" + getVersionNumber(userAgent, position + 9) + ")");
    } else if ((position = userAgent.indexOf("KHTML")) > -1) {
      result = getArray("KHTML", "KHTML?", "KHTML?(" + getVersionNumber(userAgent, position + 5) + ")");
    } else if ((position = userAgent.indexOf("NetFront")) > -1) {
      result = getArray("NetFront", "NetFront", "NetFront " + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("MultiZilla/")) > -1) {
      result = getArray("MultiZilla", "MultiZilla", "MultiZilla" + getVersionNumber(userAgent, position + 11) + ")");
    } else // We will interpret Mozilla/4.x as Netscape Communicator is and only if x
    // is not 0 or 5
    if (userAgent.indexOf("Mozilla/4.") == 0
            && !userAgent.contains("Mozilla/4.0")
            && !userAgent.contains("Mozilla/4.5 ")) {
      result = getArray("Communicator", "Communicator", "Communicator" + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("Mozilla/")) > -1) {
      result = getArray("Mozilla", "Mozilla", "Mozilla" + getVersionNumber(userAgent, position + 8));
    } else if ((position = userAgent.indexOf("BlackBerry")) > -1) {
      result = getArray("BlackBerry", "BlackBerry", "BlackBerry" + getVersionNumber(userAgent, position + 10));
    } else if ((position = userAgent.indexOf("Avant Browser/")) > -1) {
      result = getArray("Avant", "Avant", "Avant" + getVersionNumber(userAgent, position + 14));
    } else {
      return getArray("<b>?</b>", "<b>?</b>", "<b>?</b>");
    }
    return result;
  }
}
