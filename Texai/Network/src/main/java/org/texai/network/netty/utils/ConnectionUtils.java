/*
 * ConnectionUtils.java
 *
 * Description: Provides convenient static methods to establish server and client Netty SSL channels.
 *
 * Copyright (C) Apr 1, 2010, Stephen L. Reed.
 *
 */
package org.texai.network.netty.utils;

import com.google.bitcoin.core.NetworkParameters;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandler;
import org.texai.network.netty.handler.AbstractAlbusHCSMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandler;
import org.texai.network.netty.handler.AbstractBitcoinProtocolMessageHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPResponseHandler;
import org.texai.network.netty.pipeline.AlbusHCNMessageClientPipelineFactory;
import org.texai.network.netty.pipeline.BitcoinProtocolClientPipelineFactory;
import org.texai.network.netty.pipeline.BitcoinProtocolMessageClientPipelineFactory;
import org.texai.network.netty.pipeline.HTTPSClientPipelineFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.util.TexaiException;
import org.texai.x509.X509SecurityInfo;

/**
 * Provides convenient static methods to establish server and client Netty SSL channels.
 *
 * @author reed
 */
@NotThreadSafe
public final class ConnectionUtils {

  //the logger
  private static final Logger LOGGER = Logger.getLogger(ConnectionUtils.class);
  // the connected channel dictionary, channel latch --> channel
  private final static Map<Object, Channel> connectedChannelDictionary = new HashMap<>();

  /**
   * Prevents the instantiation of this utility class.
   */
  private ConnectionUtils() {
  }

  /**
   * Creates a port unification server, handling Albus hierarchical control system messages, bit torrent messages, and HTTP requests, using
   * a single shared socket with SSL encryption.
   *
   * @param port the server port
   * @param x509SecurityInfo the X.509 security information
   * @param albusHCSMessageHandlerFactory the Albus hierarchical control system message handler factory
   * @param httpRequestHandlerFactory the HTTP request message handler factory
   * @param bossExecutor the Executor which will execute the boss threads
   * @param workerExecutor the Executor which will execute the I/O worker threads
   * @param isHTTPS the indicator whether the HTTP connection is encrypted, i.e. HTTPS
   *
   * @return the server bootstrap, which contains a new server-side channel and accepts incoming connections
   */
  public static ServerBootstrap createPortUnificationServer(
          final int port,
          final X509SecurityInfo x509SecurityInfo,
          final AbstractAlbusHCSMessageHandlerFactory albusHCSMessageHandlerFactory,
          final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory,
          final Executor bossExecutor,
          final Executor workerExecutor,
          final boolean isHTTPS) {
    //Preconditions
    assert port >= 0 && port <= 65535 : "invalid port number";
    assert bossExecutor != null : "bossExecutor must not be null";
    assert workerExecutor != null : "workerExecutor must not be null";

    // configure the server channel pipeline factory
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
            albusHCSMessageHandlerFactory,
            httpRequestHandlerFactory,
            x509SecurityInfo,
            isHTTPS);

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            bossExecutor,
            workerExecutor));

    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    final InetSocketAddress inetSocketAddress = new InetSocketAddress(port);
    serverBootstrap.bind(inetSocketAddress);
    try {
      LOGGER.info("accepting connections on " + InetAddress.getLocalHost() + ":" + inetSocketAddress.getPort());
    } catch (UnknownHostException ex) {
      // ignore
    }
    return serverBootstrap;
  }

  /**
   * Releases the resources held by the port unification server, and closes its associated thread pools.
   *
   * @param serverBootstrap the server bootstrap
   */
  public static void closePortUnificationServer(final ServerBootstrap serverBootstrap) {
    serverBootstrap.releaseExternalResources();
  }

  /**
   * Opens an Albus hierarchical control system message connection using SSL encryption.
   *
   * @param inetSocketAddress the IP socket address, host & port
   * @param x509SecurityInfo the X.509 security information
   * @param albusHCSMessageHandler the Albus hierarchical control system message handler
   * @param bossExecutor the Executor which will execute the boss threads
   * @param workerExecutor the Executor which will execute the I/O worker threads
   *
   * @return the communication channel
   */
  public static Channel openAlbusHCSConnection(
          final InetSocketAddress inetSocketAddress,
          final X509SecurityInfo x509SecurityInfo,
          final AbstractAlbusHCSMessageHandler albusHCSMessageHandler,
          final Executor bossExecutor,
          final Executor workerExecutor) {
    //Preconditions
    assert inetSocketAddress != null : "inetSocketAddress must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
    assert albusHCSMessageHandler != null : "albusHCSMessageHandler must not be null";
    assert bossExecutor != null : "bossExecutor must not be null";
    assert workerExecutor != null : "workerExecutor must not be null";

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("creating Albus client bootstrap");
    }
    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            bossExecutor,
            workerExecutor));

    // configure the client pipeline
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("configuring the client pipeline connecting to " + inetSocketAddress);
    }
    final ChannelPipeline channelPipeline = AlbusHCNMessageClientPipelineFactory.getPipeline(
            albusHCSMessageHandler,
            x509SecurityInfo);
    clientBootstrap.setPipeline(channelPipeline);

    // start the connection attempt
    final Semaphore channelConnection_lock = new Semaphore(
            0); // permits
    bossExecutor.execute(new ChannelConnector(
            clientBootstrap,
            inetSocketAddress,
            channelConnection_lock));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("waiting for connection to " + inetSocketAddress);
    }
    try {
      channelConnection_lock.acquire();
    } catch (InterruptedException ex) {
      // ignore
    }
    final Channel channel;
    synchronized (connectedChannelDictionary) {
      channel = connectedChannelDictionary.get(channelConnection_lock);
      if (channel != null) {
        connectedChannelDictionary.remove(channelConnection_lock);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("completed opening the channel to " + inetSocketAddress);
        }
      }
    }
    return channel;
  }

  /**
   * Creates a Bitcoin protocol server.
   *
   * @param port the server port
   * @param bitcoinProtocolMessageHandlerFactory the Bitcoin protocol message handler factory
   * @param networkParameters the network parameters, e.g. MainNetParams or TestNet3Params
   * @param bossExecutor the Executor which will execute the boss threads
   * @param workerExecutor the Executor which will execute the I/O worker threads
   *
   * @return the server bootstrap, which contains a new server-side channel and accepts incoming connections
   */
  public static ServerBootstrap createBitcoinProtocolServer(
          final int port,
          final AbstractBitcoinProtocolMessageHandlerFactory bitcoinProtocolMessageHandlerFactory,
          final NetworkParameters networkParameters,
          final Executor bossExecutor,
          final Executor workerExecutor) {
    //Preconditions
    assert port >= 0 && port <= 65535 : "invalid port number";
    assert bossExecutor != null : "bossExecutor must not be null";
    assert workerExecutor != null : "workerExecutor must not be null";

    // configure the server channel pipeline factory
    final ChannelPipelineFactory channelPipelineFactory = new BitcoinProtocolClientPipelineFactory(
            bitcoinProtocolMessageHandlerFactory,
            networkParameters);

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            bossExecutor,
            workerExecutor));

    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    serverBootstrap.bind(new InetSocketAddress(port));
    LOGGER.info("accepting connections on port " + port);
    return serverBootstrap;
  }

  /**
   * Releases the resources held by the Bitcoin protocol server, and closes its associated thread pools.
   *
   * @param serverBootstrap the server bootstrap
   */
  public static void closeBitcoinProtocolServer(final ServerBootstrap serverBootstrap) {
    serverBootstrap.releaseExternalResources();
  }

  /**
   * Opens a Bitcoin protocol message connection.
   *
   * @param inetSocketAddress the IP socket address, host & port
   * @param bitcoinProtocolMessageHandler the Bitcoin protocol message handler
   * @param bossExecutor the Executor which will execute the boss threads
   * @param workerExecutor the Executor which will execute the I/O worker threads
   *
   * @return the communication channel
   */
  public static Channel openBitcoinProtocolConnection(
          final InetSocketAddress inetSocketAddress,
          final AbstractBitcoinProtocolMessageHandler bitcoinProtocolMessageHandler,
          final Executor bossExecutor,
          final Executor workerExecutor) {
    //Preconditions
    assert inetSocketAddress != null : "inetSocketAddress must not be null";
    assert bitcoinProtocolMessageHandler != null : "bitcoinProtocolMessageHandler must not be null";
    assert bossExecutor != null : "bossExecutor must not be null";
    assert workerExecutor != null : "workerExecutor must not be null";

    LOGGER.info("creating Bitcoin protocol client bootstrap");
    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            bossExecutor,
            workerExecutor));

    // configure the client pipeline
    LOGGER.info("configuring the bitcoin protocol client pipeline connecting to " + inetSocketAddress);
    final ChannelPipeline channelPipeline = BitcoinProtocolMessageClientPipelineFactory.getPipeline(bitcoinProtocolMessageHandler);
    LOGGER.info(channelPipeline);
    clientBootstrap.setPipeline(channelPipeline);

    // start the connection attempt
    final Semaphore channelConnection_lock = new Semaphore(
            0); // permits
    bossExecutor.execute(new ChannelConnector(
            clientBootstrap,
            inetSocketAddress,
            channelConnection_lock));

    LOGGER.info("waiting for connection to " + inetSocketAddress);
    try {
      channelConnection_lock.acquire();
    } catch (InterruptedException ex) {
      // ignore
    }
    final Channel channel;
    synchronized (connectedChannelDictionary) {
      channel = connectedChannelDictionary.get(channelConnection_lock);
      if (channel != null) {
        connectedChannelDictionary.remove(channelConnection_lock);
        LOGGER.info("completed opening the channel to " + inetSocketAddress);
      }
    }
    return channel;
  }

  /**
   * Provides a channel connector.
   */
  static class ChannelConnector implements Runnable {

    /**
     * the client bootstrap
     */
    final ClientBootstrap clientBootstrap;
    /**
     * the IP socket address, host & port
     */
    final InetSocketAddress inetSocketAddress;
    /**
     * the channel connection synchronization lock
     */
    final Semaphore channelConnection_lock;

    /**
     * Constructs a new ChannelConnector instance.
     *
     * @param clientBootstrap the client bootstrap
     * @param inetSocketAddress the IP socket address, host & port
     * @param channelConnection_lock the channel connection synchronization lock
     */
    ChannelConnector(
            final ClientBootstrap clientBootstrap,
            final InetSocketAddress inetSocketAddress,
            final Semaphore channelConnection_lock) {
      //Preconditions
      assert clientBootstrap != null : "clientBootstrap must not be null";
      assert inetSocketAddress != null : "inetSocketAddress must not be null";
      assert channelConnection_lock != null : "channelConnection_lock must not be null";

      this.clientBootstrap = clientBootstrap;
      this.inetSocketAddress = inetSocketAddress;
      this.channelConnection_lock = channelConnection_lock;
    }

    /**
     * Connects to a channel.
     */
    @Override
    public void run() {
      // start the connection attempt
      LOGGER.info("starting the connection attempt to " + inetSocketAddress);
      clientBootstrap.connect(inetSocketAddress).addListener(new MyChannelFutureListener(
              channelConnection_lock,
              inetSocketAddress));
    }
  }

  /**
   * Provides a channel future listener that resumes the thread waiting for the channel connection event.
   */
  static class MyChannelFutureListener implements ChannelFutureListener {

    /**
     * the channel connection synchronization lock
     */
    final Semaphore channelConnection_lock;
    /**
     * the IP socket address, host & port
     */
    final InetSocketAddress inetSocketAddress;

    /**
     * Constructs a new MyChannelFutureListener instance.
     *
     * @param channelConnection_lock the channel connection synchronization lock
     * @param inetSocketAddress the IP socket address, host & port
     */
    MyChannelFutureListener(
            final Semaphore channelConnection_lock,
            final InetSocketAddress inetSocketAddress) {
      //Preconditions
      assert channelConnection_lock != null : "channelConnection_lock must not be null";
      assert inetSocketAddress != null : "inetSocketAddress must not be null";

      this.channelConnection_lock = channelConnection_lock;
      this.inetSocketAddress = inetSocketAddress;
    }

    /**
     * Invoked when the I/O operation associated with the {@link ChannelFuture} has been completed.
     *
     * @param channelFuture The source {@link ChannelFuture} which called this callback.
     *
     * @throws java.lang.Exception
     */
    @Override
    @SuppressWarnings("ThrowableResultIgnored")
    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
      //Preconditions
      assert channelFuture != null : "future must not be null";

      if (!channelFuture.isSuccess()) {
        LOGGER.info("cannot connect with " + inetSocketAddress);
        //throw new TexaiException(channelFuture.getCause());
      } else {
        LOGGER.info("client connected to " + channelFuture.getChannel().getRemoteAddress());
        synchronized (connectedChannelDictionary) {
          LOGGER.debug("obtained lock on the connectedChannelDictionary");
          connectedChannelDictionary.put(channelConnection_lock, channelFuture.getChannel());
        }
      }
      LOGGER.debug("releasing the channel connection lock" + channelConnection_lock);
      channelConnection_lock.release(); // release the lock
      LOGGER.debug("released the channel connection lock" + channelConnection_lock);
    }
  }

  /**
   * Opens an HTTP message connection using SSL encryption.
   *
   * @param inetSocketAddress the IP socket address, host & port
   * @param x509SecurityInfo the X.509 security information
   * @param httpResponseHandler the HTTP response message handler
   * @param bossExecutor the Executor which will execute the boss threads
   * @param workerExecutor the Executor which will execute the I/O worker threads
   *
   * @return the communication channel
   */
  @SuppressWarnings("ThrowableResultIgnored")
  public static Channel openHTTPConnection(
          final InetSocketAddress inetSocketAddress,
          final X509SecurityInfo x509SecurityInfo,
          final AbstractHTTPResponseHandler httpResponseHandler,
          final Executor bossExecutor,
          final Executor workerExecutor) {
    //Preconditions
    assert inetSocketAddress != null : "inetSocketAddress must not be null";
    assert x509SecurityInfo != null : "x509SecurityInfo must not be null";
    assert httpResponseHandler != null : "httpResponseHandler must not be null";
    assert bossExecutor != null : "bossExecutor must not be null";
    assert workerExecutor != null : "workerExecutor must not be null";

    final ClientBootstrap clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
            bossExecutor,
            workerExecutor));

    // configure the client pipeline
    final ChannelPipeline channelPipeline = HTTPSClientPipelineFactory.getPipeline(
            httpResponseHandler,
            x509SecurityInfo);
    clientBootstrap.setPipeline(channelPipeline);

    // start the connection attempt
    final ChannelFuture channelFuture = clientBootstrap.connect(inetSocketAddress);

    // wait until the connection attempt succeeds or fails
    final Channel channel = channelFuture.awaitUninterruptibly().getChannel();
    if (!channelFuture.isSuccess()) {
      throw new TexaiException(channelFuture.getCause());
    }
    LOGGER.info("HTTP client connected");
    return channel;
  }
}
