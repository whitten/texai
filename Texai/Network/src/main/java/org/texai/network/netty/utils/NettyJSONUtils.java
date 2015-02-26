/*
 * NettyJSONUtils.java
 *
 * Description: Provides JSON utilities.
 *
 * Copyright (C) Jan 31, 2012, Stephen L. Reed.
 *
 */
package org.texai.network.netty.utils;

import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 * Provides JSON utilities.
 *
 * @author reed
 */
@ThreadSafe
public class NettyJSONUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(NettyJSONUtils.class);

  /**
   * Prevents the instantiation of this utility class.
   */
  private NettyJSONUtils() {
  }

  /**
   * gets the session cookie from the given JSON text.
   *
   * @param jsonText the given JSON text
   *
   * @return the session cookie
   */
  public static String getTexaiSessionCookie(final String jsonText) {
    //Preconditions
    assert StringUtils.isNonEmptyString(jsonText) : "jsonText must be a non-empty string";

    //    {"type":"xxxx",
    //     "data":{"cookie":"9fcad0a9-f2cc-4b5c-9438-c3f2439ecb56"}}
    //LOGGER.info("jsonText: '" + jsonText + "'");
    final JSONObject jsonObject;
    try {
      jsonObject = (JSONObject) new JSONParser().parse(jsonText);
    } catch (ParseException ex) {
      throw new TexaiException(ex);
    }
    assert jsonObject != null;
    final JSONObject jsonObject2 = (JSONObject) jsonObject.get("data");
    assert jsonObject2 != null;
    return (String) jsonObject2.get("texai-session");
  }
}
