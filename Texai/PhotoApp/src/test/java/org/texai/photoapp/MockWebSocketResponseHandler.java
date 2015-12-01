/*
 * MockWebSocketResponseHandler.java
 *
 * Created on Jan 30, 2012, 12:20:45 PM
 *
 * Description: .
 *
 * Copyright (C) Jan 30, 2012, Stephen L. Reed.
 *
 */
package org.texai.photoapp;

import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.texai.network.netty.handler.AbstractWebSocketResponseHandler;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
@NotThreadSafe
public class MockWebSocketResponseHandler extends AbstractWebSocketResponseHandler {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(MockWebSocketResponseHandler.class);
  // the web socket client handshaker
  private final WebSocketClientHandshaker webSocketClientHandshaker;
  // the synchronization lock to resume the client when the handshake is completed
  final Object clientResume_lock;

  /**
   * Constructs a new MockWebSocketResponseHandler instance.
   *
   * @param webSocketClientHandshaker the web socket client handshaker
   * @param clientResume_lock the synchronization lock to resume the client when the handshake is completed
   * @param testPhotoAppActions the test photo application actions
   */
  public MockWebSocketResponseHandler(
          final WebSocketClientHandshaker webSocketClientHandshaker,
          final Object clientResume_lock,
          final TestPhotoAppActions testPhotoAppActions) {
    //Preconditions
    assert webSocketClientHandshaker != null : "webSocketClientHandshaker must not be null";
    assert clientResume_lock != null : "clientResume_lock must not be null";
    assert testPhotoAppActions != null : "testPhotoAppActions must not be null";

    this.webSocketClientHandshaker = webSocketClientHandshaker;
    this.clientResume_lock = clientResume_lock;
  }

  /**
   * Handles a received web socket message.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   *
   * @throws Exception when an exception occurs
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings({"NO_NOTIFY_NOT_NOTIFYALL"})
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    final Channel channel = channelHandlerContext.getChannel();
    if (!webSocketClientHandshaker.isHandshakeComplete()) {
      LOGGER.info("finishing web socket client handshake");
      webSocketClientHandshaker.finishHandshake(channel, (HttpResponse) messageEvent.getMessage());
      LOGGER.info("web socket client handshake completed");
      synchronized (clientResume_lock) {
        // resume the waiting client thread
        clientResume_lock.notify();
      }
      return;
    }

    LOGGER.debug("received " + messageEvent.getMessage().getClass().getSimpleName());
    if (messageEvent.getMessage() instanceof HttpResponse) {
      final HttpResponse httpResponse = (HttpResponse) messageEvent.getMessage();
      throw new TexaiException("Unexpected HttpResponse (status=" + httpResponse.getStatus() + ", content="
              + httpResponse.getContent().toString(CharsetUtil.UTF_8) + ")");
    }

    final WebSocketFrame webSocketFrame = (WebSocketFrame) messageEvent.getMessage();
    if (webSocketFrame instanceof TextWebSocketFrame) {
      TextWebSocketFrame textWebSocketFrame = (TextWebSocketFrame) webSocketFrame;
      final String jsonMessage = textWebSocketFrame.getText();
      LOGGER.info("********** client receiving ************");
      LOGGER.info("web socket client received message: " + jsonMessage);
      parseJSONMessage(jsonMessage, channel);
    } else if (webSocketFrame instanceof PongWebSocketFrame) {
      LOGGER.info("web socket client received pong");
    } else if (webSocketFrame instanceof CloseWebSocketFrame) {
      LOGGER.info("web socket client received closing");
      channel.close();
    }
  }

  /**
   * Parse the JSON message from the server.
   *
   * @param jsonMessage the JSON formatted message
   * @param channel the channel
   */
  private void parseJSONMessage(
          final String jsonMessage,
          final Channel channel) {
    //Preconditions
    assert StringUtils.isNonEmptyString(jsonMessage) : "jsonMessage must not be null";
    assert channel != null : "channel must not be null";

    final JSONParser jsonParser = new JSONParser();
    final JSONObject jsonObject;
    try {
      jsonObject = (JSONObject) jsonParser.parse(jsonMessage);
    } catch (ParseException ex) {
      throw new TexaiException(ex);
    }

    final String operation = (String) jsonObject.get("operation");
    switch (operation) {
      case "errorNotification": {
        //  {
        //    "operation": "errorNotification",
        //    "errorMessage": "[for example] recipient not logged in"
        //  }
        final String errorMessage = (String) jsonObject.get("errorMessage");
        LOGGER.info("operation: errorNotification");
        LOGGER.info("errorMessage: " + errorMessage);
        break;
      }
      case "provisionUser": {
      //  {
        //    “operation”: “provisionUser”,
        //    “buddyList”: {“1”: “Bob”}
        //  }
        LOGGER.info("operation: provisionUser");
        final JSONObject buddyElement = (JSONObject) jsonObject.get("buddyList");
        final String buddy1 = (String) buddyElement.get("1");
        LOGGER.info("buddy1: " + buddy1);
        break;
      }
      case "storageResponse": {
        //  {
        //    "operation": "storageResponse",
        //    "timestamp": "the date and time that the photo was stored in the Amazon cloud, as a UTC string"
        //    "duplicate": "yes or no"
        //  }
        LOGGER.info("operation: storageResponse");
        final String timestamp = (String) jsonObject.get("timestamp");
        final String duplicate = (String) jsonObject.get("duplicate");
        LOGGER.info("timestamp: " + timestamp);
        LOGGER.info("duplicate: " + duplicate);
        break;
      }
      case "receivePhoto": {
        //  {
        //    “operation”: “receivePhoto”,
        //    “photo”: “photo encoded in Base 64 notation”,
        //    “photoHash”: “the SHA-1 hash of the selected photo encoded in Base 64 notation”,
        //    “timestamp”: “the date and time that the photo was stored in the Amazon cloud, as a UTC string”
        //    “sender”: “Alice”
        //  }
        LOGGER.info("operation: receivePhoto");
        final String photo = (String) jsonObject.get("photo");
        final String photoHash = (String) jsonObject.get("photoHash");
        final String timestamp = (String) jsonObject.get("timestamp");
        final String sender = (String) jsonObject.get("sender");
        LOGGER.info("photo: " + photo);
        LOGGER.info("photoHash: " + photoHash);
        LOGGER.info("timestamp: " + timestamp);
        LOGGER.info("sender: " + sender);
        break;
      }
      case "photoDelivered": {
        //  {
        //    “operation”: “photoDelivered”,
        //    “photoHash”: “the SHA-1 hash of the selected photo encoded in Base 64 notation”,
        //    “timestamp”: “the date and time that the photo was delivered, as a UTC string”
        //    “recipient”: “Bob”
        //  }
        LOGGER.info("operation: photoDelivered");
        final String photoHash = (String) jsonObject.get("photoHash");
        final String timestamp = (String) jsonObject.get("timestamp");
        final String recipient = (String) jsonObject.get("recipient");
        LOGGER.info("photoHash: " + photoHash);
        LOGGER.info("timestamp: " + timestamp);
        LOGGER.info("recipient: " + recipient);
        break;
      }
      default: {
        LOGGER.info("invalid operation: '" + operation + "'");
      }
    }
  }

  /**
   * Closes the channel.
   *
   * @param channelHandlerContext the channel handler context
   * @param channelStateEvent the channel state event
   *
   * @throws Exception when an exception occurs
   */
  @Override
  public void channelClosed(
          final ChannelHandlerContext channelHandlerContext,
          final ChannelStateEvent channelStateEvent) throws Exception {
    LOGGER.info("web socket client disconnected");
  }

  /**
   * Catches an otherwise uncaught exception.
   *
   * @param channelHandlerContext the channel handler context
   * @param exceptionEvent
   *
   * @throws Exception
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) throws Exception {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    LOGGER.info(StringUtils.getStackTraceAsString(exceptionEvent.getCause()));
    exceptionEvent.getChannel().close();
  }
}
