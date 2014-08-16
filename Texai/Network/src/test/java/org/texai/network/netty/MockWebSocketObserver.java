/*
 * MockWebSocketObserver.java
 *
 * Created on Apr 10, 2012, 5:32:29 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 10, 2012, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.network.netty;

import com.unitt.framework.websocket.WebSocketObserver;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.texai.util.StringUtils;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class MockWebSocketObserver implements WebSocketObserver {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(MockWebSocketObserver.class);

  /** Constructs a new MockWebSocketObserver instance. */
  public MockWebSocketObserver() {
  }

  /** Receives notification that the web socket connects and is ready for reading and writing.
   *
   * @param aProtocol the protocol
   * @param aExtensions  the extensions
   */
  @Override
  public void onOpen(
          final String aProtocol,
          final List<String> aExtensions) {
    LOGGER.info("onOpen");
  }

  /** Receives notification that the web socket closes. anError will be null if it closes cleanly.
   *
   * @param aStatusCode the status code
   * @param aMessage the web socket message
   * @param aException an exception
   */
  @Override
  public void onClose(
          final int aStatusCode,
          final String aMessage,
          final Exception aException) {
    LOGGER.info("onClose");
  }

  /** Receives notification that the web socket receives an error. Such an error can result in the
  socket being closed.
   *
   * @param aException
   */
  @Override
  public void onError(final Exception aException) {
    LOGGER.info("onError...\n" + StringUtils.getStackTraceAsString(aException));
  }

  /** Receives notification that the web socket receives a message.
   *
   * @param aMessage the web socket message
   */
  @Override
  public void onTextMessage(final String aMessage) {
    //Hooray! I got a message to print.
    LOGGER.info("onTextMessage: " + aMessage);
  }

  /** Receives notification that the web socket receives a message.
   *
   * @param aMessage the web socket message
   */
  @Override
  public void onBinaryMessage(final byte[] aMessage) {
    LOGGER.info("onBinaryMessage");
  }

  /** Receives notification that pong is sent... For keep-alive optimization.
   *
   * @param aMessage the web socket message
   */
  @Override
  public void onPong(final String aMessage) {
    LOGGER.info("onPong");
  }
}
