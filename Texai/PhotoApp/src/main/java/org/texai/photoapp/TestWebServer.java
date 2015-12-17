package org.texai.photoapp;

import org.texai.util.EnvironmentUtils;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.handler.TexaiHTTPRequestHandler;
import org.texai.network.netty.pipeline.HTTPServerPipelineFactory;
import org.texai.network.netty.pipeline.WebSocketServerPipelineFactory;

/**
 * TestWebServer.java
 *
 * Description: Test the web server.
 *
 * Copyright (C) Nov 30, 2015, Stephen L. Reed.
 */
public class TestWebServer implements TexaiHTTPRequestHandler {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(TestWebServer.class);
  // the https server port
  private static final int HTTP_SERVER_PORT = 8087;
  // the websocket server port, which is not secure in order to work with iPhones
  private static final int WEBSOCKET_SERVER_PORT = 8086;
  // the photo app server test instance
  private static final PhotoAppServer photoAppServer = new PhotoAppServer();

  /**
   * Prevents the instantiation of this utility test class.
   */
  private TestWebServer() {
  }

  /**
   * Test of class PhotoAppServer.
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    LOGGER.info("testWebServer");

    if (EnvironmentUtils.isWindows()) {
      LOGGER.info("bypassing web server test on Windows");
      return;
    }

    //System.setProperty("javax.net.debug","ssl");

    // configure the HTTP request handler by registering the photo app server
    final HTTPRequestHandler httpRequestHandler = new HTTPRequestHandler();
    httpRequestHandler.register(photoAppServer);

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory(httpRequestHandler);
    final ChannelPipelineFactory channelPipelineFactory = new HTTPServerPipelineFactory(httpRequestHandlerFactory);

    // configure the http server
    final ServerBootstrap httpServerBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    httpServerBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    LOGGER.info("listening for HTTP connections on " + HTTP_SERVER_PORT);
    httpServerBootstrap.bind(new InetSocketAddress(HTTP_SERVER_PORT));



    // configure the websocket request handler by registering the photo app server
    final HTTPRequestHandler webSocketRequestHandler = new HTTPRequestHandler();
    webSocketRequestHandler.register(photoAppServer);

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory webSocketRequestHandlerFactory = new HTTPRequestHandlerFactory(webSocketRequestHandler);
    final ChannelPipelineFactory webSocketChannelPipelineFactory = new WebSocketServerPipelineFactory(webSocketRequestHandlerFactory);

    // configure the websocket server
    final ServerBootstrap webSocketServerBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    webSocketServerBootstrap.setPipelineFactory(webSocketChannelPipelineFactory);

    // bind and start to accept incoming connections
    LOGGER.info("listening for websocket connections on " + WEBSOCKET_SERVER_PORT);
    webSocketServerBootstrap.bind(new InetSocketAddress(WEBSOCKET_SERVER_PORT));


    // wait for the manual browser testing to complete
    try {
      Thread.sleep(360_000);
    } catch (InterruptedException ex) {
    }

  }

  @Override
  public boolean httpRequestReceived(HttpRequest httpRequest, Channel channel) {
    LOGGER.info("httpRequest ...\n" + httpRequest);
    return true;
  }

  @Override
  public boolean textWebSocketFrameReceived(Channel channel, TextWebSocketFrame textWebSocketFrame) {
    LOGGER.info("textWebSocketFrame ...\n" + textWebSocketFrame);
    return true;
  }

}
