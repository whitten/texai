/*
 * MockAlbusHCSMessageHandler.java
 *
 * Description: A channel handler that transports messages from node to node in an Albus hierarchical control network.
 *
 */
package org.texai.network.netty.handler;

import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.joda.time.DateTime;
import org.texai.ahcsSupport.Message;
import org.texai.util.TexaiException;

/**
 * A channel handler that transports messages from node to node in an Albus hierarchical control network.
 *
 * @author reed
 */
@NotThreadSafe
public final class MockAlbusHCSMessageHandler extends AbstractAlbusHCSMessageHandler {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(MockAlbusHCSMessageHandler.class);
  // the lock that allows the client to resume when the messaging is done
  final Object clientResume_lock;
  // the test iteration limit
  final int iterationLimit;

  /**
   * Constructs a new MockAlbusHCSMessageHandler instance.
   *
   * @param clientResume_lock the lock that allows the client to resume when the messaging is done, or null if this is the client side
   * handler
   * @param iterationLimit the test iteration limit
   */
  public MockAlbusHCSMessageHandler(final Object clientResume_lock, final int iterationLimit) {
    //Preconditions
    assert iterationLimit >= 0 : "iterationLimit must not be negative";

    this.clientResume_lock = clientResume_lock;
    this.iterationLimit = iterationLimit;
  }

  /**
   * Receives a message object from a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void messageReceived(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    final boolean isClient = clientResume_lock != null;
    if (isClient) {
      LOGGER.info("client received messageEvent: " + messageEvent);
    } else {
      LOGGER.info("server received messageEvent: " + messageEvent);
    }
    final Channel channel = channelHandlerContext.getChannel();
    assert messageEvent.getMessage() instanceof Message;
    final Message message = (Message) messageEvent.getMessage();
    int count = (Integer) message.get("count");
    LOGGER.info("count: " + count);
    // the test service is a valid class with a no-argument constructor, the Skills defining module is dependent on this so an actual skill class cannot be used here
    final String service = "org.texai.kb.persistence.domainEntity.RepositoryContentDescription";
    if (message.getOperation().equals("Echo_Task")) {
      // increment the count and send a repsonse message back to the sender

      final Message responseMessage = new Message(
              message.getSenderQualifiedName(),
              service,
              message.getRecipientQualifiedName(),
              message.getConversationId(),
              UUID.randomUUID(), // replyWith
              message.getReplyWith(),
              new DateTime(), // replyByDateTime
              service,
              "Response_Task",
              message.getParameterDictionary(),
              "1.0.0"); // version
      count++;
      responseMessage.put("count", count);
      LOGGER.info("about to write message " + responseMessage + "\nto channel pipeline" + channel.getPipeline());
      channel.write(responseMessage);
    } else {
      assert isClient;
      if (count > iterationLimit) {
        // signal the client thread to finish
        synchronized (clientResume_lock) {
          clientResume_lock.notifyAll();
        }
      } else {
        // send another request message
        final Message echoMessage = new Message(
                message.getSenderQualifiedName(),
                service,
                message.getRecipientQualifiedName(),
                message.getConversationId(),
                UUID.randomUUID(), // replyWith
                message.getReplyWith(),
                new DateTime(), // replyByDateTime
                message.getSenderService(),
                "Echo_Task",
                message.getParameterDictionary(),
                "1.0.0"); // version
        channel.write(echoMessage);
      }
    }
  }

  /**
   * Writes a message object to a remote peer.
   *
   * @param channelHandlerContext the channel handler context
   * @param messageEvent the message event
   */
  @Override
  public void writeRequested(
          final ChannelHandlerContext channelHandlerContext,
          final MessageEvent messageEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert messageEvent != null : "messageEvent must not be null";

    LOGGER.info("write requested messageEvent: " + messageEvent);
    LOGGER.info("pipeline: " + channelHandlerContext.getPipeline().toString());
    channelHandlerContext.sendDownstream(messageEvent);
  }

  /**
   * Handles a caught exception.
   *
   * @param channelHandlerContext the channel handler event
   * @param exceptionEvent the exception event
   */
  @Override
  public void exceptionCaught(
          final ChannelHandlerContext channelHandlerContext,
          final ExceptionEvent exceptionEvent) {
    //Preconditions
    assert channelHandlerContext != null : "channelHandlerContext must not be null";
    assert exceptionEvent != null : "exceptionEvent must not be null";

    if (clientResume_lock == null) {
      LOGGER.error("server exceptionEvent: " + exceptionEvent);
    } else {
      LOGGER.error("client exceptionEvent: " + exceptionEvent);
    }
    throw new TexaiException(exceptionEvent.getCause());
  }
}
