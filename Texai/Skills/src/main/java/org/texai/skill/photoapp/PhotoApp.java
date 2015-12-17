/**
 * NetworkSingletonSkillTemplate.java
 *
 * Description: Provides a web service for the AI Chain Photo Application.
 *
 * Copyright (C) Jan 21, 2015, Stephen L. Reed.
 */
package org.texai.skill.photoapp;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import net.jcip.annotations.ThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.texai.ahcsSupport.AHCSConstants;
import org.texai.ahcsSupport.Message;
import org.texai.ahcs.skill.AbstractNetworkSingletonSkill;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.pipeline.HTTPServerPipelineFactory;
import org.texai.network.netty.pipeline.WebSocketServerPipelineFactory;
import org.texai.photoapp.PhotoAppServer;

/**
 * Provides a template for a network singleton skill.
 *
 * @author reed
 */
@ThreadSafe
public final class PhotoApp extends AbstractNetworkSingletonSkill {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(PhotoApp.class);
  // the photo app server
  private PhotoAppServer photoAppServer;
  // the https server port
  private static final int HTTP_SERVER_PORT = 8089;
  // the websocket server port, which is not secure in order to work with iPhones
  private static final int WEBSOCKET_SERVER_PORT = 8088;
  // the http server bootstrap
  private ServerBootstrap httpServerBootstrap;
  // the websocket server bootstrap
  private ServerBootstrap webSocketServerBootstrap;

  /**
   * Creates a new instance of NetworkSingletonSkillTemplate.
   */
  public PhotoApp() {
  }

  /**
   * Gets the logger.
   *
   * @return the logger
   */
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Receives and attempts to process the given message. The skill is thread safe, given that any contained libraries are single threaded
   * with regard to the conversation.
   *
   * @param receivedMessage the given message
   */
  @Override
  public void receiveMessage(Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "receivedMessage must not be null";
    assert getRole().getNode().getNodeRuntime() != null;

    final String operation = receivedMessage.getOperation();
    if (!isOperationPermitted(receivedMessage)) {
      sendOperationNotPermittedInfoMessage(receivedMessage);
      return;
    }
    switch (operation) {
      /**
       * Initialize Task
       *
       * This task message is sent from the parent NetworkOperationAgent.NetworkOperationRole. It is expected to be the first task message
       * that this role receives and it results in the role being initialized.
       */
      case AHCSConstants.INITIALIZE_TASK:
        assert this.getSkillState().equals(AHCSConstants.State.UNINITIALIZED) : "prior state must be non-initialized";

        initializeTask();
        return;

      /**
       * Join Acknowledged Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It indicates that the
       * parent is ready to converse with this role as needed.
       */
      case AHCSConstants.JOIN_ACKNOWLEDGED_TASK:
        assert getSkillState().equals(AHCSConstants.State.ISOLATED_FROM_NETWORK) :
                "state must be isolated-from-network, but is " + getSkillState();
        joinAcknowledgedTask(receivedMessage);
        return;

      /**
       * Perform Mission Task
       *
       * This task message is sent from the network-singleton, parent NetworkOperationAgent.NetworkOperationRole. It commands this
       * network-connected role to begin performing its mission.
       */
      case AHCSConstants.PERFORM_MISSION_TASK:
        performMission(receivedMessage);
        return;

      /**
       * Join Network Singleton Agent Info
       *
       * This task message is sent to this network singleton agent/role from a child role in another container.
       *
       * The sender is requesting to join the network as child of this role.
       *
       * The message parameter is the X.509 certificate belonging to the sender agent / role.
       *
       * The result is the sending of a Join Acknowleged Task message to the requesting child role, with this role's X.509 certificate as
       * the message parameter.
       */
      case AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        joinNetworkSingletonAgent(receivedMessage);
        return;

      /**
       * Delegate Perform Mission Task
       *
       * A container has completed joining the network. Propagate a Delegate Perform Mission Task down the role command hierarchy.
       *
       * The container name is a parameter of the message.
       */
      case AHCSConstants.DELEGATE_PERFORM_MISSION_TASK:
        assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready, but is " + getSkillState();
        handleDelegatePerformMissionTask(receivedMessage);
        return;

      case AHCSConstants.OPERATION_NOT_PERMITTED_INFO:
      case AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO:
        LOGGER.warn(receivedMessage);
        return;
      // handle other operations ...
    }
    sendMessage(
            receivedMessage,
            Message.notUnderstoodMessage(
                    receivedMessage, // receivedMessage
                    this)); // skill
  }

  /**
   * Synchronously processes the given message. The skill is thread safe, given that any contained libraries are single threaded with regard
   * to the conversation.
   *
   * @param message the given message
   *
   * @return the response message or null if not applicable
   */
  @Override
  public Message converseMessage(final Message message) {
    //Preconditions
    assert message != null : "message must not be null";

    // handle operations
    return Message.notUnderstoodMessage(
            message, // receivedMessage
            this); // skill
  }

  /**
   * Returns the understood operations.
   *
   * @return the understood operations
   */
  @Override
  public String[] getUnderstoodOperations() {
    return new String[]{
      AHCSConstants.INITIALIZE_TASK,
      AHCSConstants.DELEGATE_PERFORM_MISSION_TASK,
      AHCSConstants.JOIN_NETWORK_SINGLETON_AGENT_INFO,
      AHCSConstants.JOIN_ACKNOWLEDGED_TASK,
      AHCSConstants.MESSAGE_NOT_UNDERSTOOD_INFO,
      AHCSConstants.PERFORM_MISSION_TASK
    };
  }

  /**
   * Initialize the skill.
   */
  private void initializeTask() {

    if (getNodeRuntime().isFirstContainerInNetwork()) {
      setSkillState(AHCSConstants.State.READY);
    } else {
      setSkillState(AHCSConstants.State.ISOLATED_FROM_NETWORK);
    }
    photoAppServer = new PhotoAppServer();

    // configure the HTTP request handler by registering the photo app server
    final HTTPRequestHandler httpRequestHandler = new HTTPRequestHandler();
    httpRequestHandler.register(photoAppServer);

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory(httpRequestHandler);
    final ChannelPipelineFactory channelPipelineFactory = new HTTPServerPipelineFactory(httpRequestHandlerFactory);

    // configure the http server
    httpServerBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    httpServerBootstrap.setPipelineFactory(channelPipelineFactory);




    // configure the websocket request handler by registering the photo app server
    final HTTPRequestHandler webSocketRequestHandler = new HTTPRequestHandler();
    webSocketRequestHandler.register(photoAppServer);

    // configure the server channel pipeline factory
    final AbstractHTTPRequestHandlerFactory webSocketRequestHandlerFactory = new HTTPRequestHandlerFactory(webSocketRequestHandler);
    final ChannelPipelineFactory webSocketChannelPipelineFactory = new WebSocketServerPipelineFactory(webSocketRequestHandlerFactory);

    // configure the websocket server
    webSocketServerBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    webSocketServerBootstrap.setPipelineFactory(webSocketChannelPipelineFactory);

  }

  /**
   * Perform this role's mission.
   *
   * @param receivedMessage the received perform mission task message
   */
  private void performMission(final Message receivedMessage) {
    //Preconditions
    assert receivedMessage != null : "message must not be null";
    assert getSkillState().equals(AHCSConstants.State.READY) : "state must be ready: " + stateDescription(getSkillState());

    LOGGER.info("performing the mission");

    // bind and start to accept incoming connections
    LOGGER.info("listening for http connections on " + HTTP_SERVER_PORT);
    httpServerBootstrap.bind(new InetSocketAddress(HTTP_SERVER_PORT));

    // bind and start to accept incoming connections
    LOGGER.info("listening for websocket connections on " + WEBSOCKET_SERVER_PORT);
    webSocketServerBootstrap.bind(new InetSocketAddress(WEBSOCKET_SERVER_PORT));
  }

}
