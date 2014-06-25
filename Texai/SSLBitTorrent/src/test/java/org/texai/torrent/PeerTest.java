/*
 * PeerTest.java
 *
 * Created on Feb 2, 2010, 10:26:58 AM
 *
 * Description: Provides a test of two bit torrent peers, in which one is the leecher and the other is the seed.
 *
 * Copyright (C) Feb 2, 2010 reed.
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
package org.texai.torrent;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.network.netty.handler.AbstractBitTorrentHandlerFactory;
import org.texai.network.netty.handler.AbstractHTTPRequestHandlerFactory;
import org.texai.network.netty.handler.BitTorrentEncoder;
import org.texai.network.netty.handler.HTTPRequestHandler;
import org.texai.network.netty.handler.HTTPRequestHandlerFactory;
import org.texai.network.netty.pipeline.PortUnificationChannelPipelineFactory;
import org.texai.torrent.domainEntity.MetaInfo;
import org.texai.util.ThreadUtils;
import org.texai.x509.KeyStoreTestUtils;
import org.texai.x509.X509SecurityInfo;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public final class PeerTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(PeerTest.class);
  /** the seed and tracker connection-accepting port */
  private static final int SEED_SERVER_PORT = 8088;
  /** the downloader connection-accepting port */
  private static final int DOWNLOADER_SERVER_PORT = 8089;

  /** sets debugging */
//  static {
//    System.setProperty("javax.net.debug", "all");
//  }

  public PeerTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
    final File downloadFile = new File("data/download/SentinelDome.jpg");
    if (downloadFile.exists()) {
      final boolean isDeleted = downloadFile.delete();
      if (isDeleted) {
        LOGGER.info("deleted " + downloadFile);
      }
    }
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of bit torrent peers and tracker.
   */
  @Test
  @SuppressWarnings({"null", "SleepWhileInLoop"})
  public void testBitTorrent() {
    LOGGER.info("leech peer & seed peer & tracker");

    // configure logging
    Logger.getLogger(BitTorrentEncoder.class).setLevel(Level.WARN);
    Logger.getLogger(BitTorrentHandler.class).setLevel(Level.WARN);

    // configure the HTTP request handler by registering the tracker
    final HTTPRequestHandler httpRequestHandler = HTTPRequestHandler.getInstance();
    final Tracker tracker = new Tracker();
    httpRequestHandler.register(tracker);

    // seed peer SSL torrent
      final X509SecurityInfo clientX509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
      final SSLTorrent sslTorrent = new SSLTorrent(
              SEED_SERVER_PORT,
              clientX509SecurityInfo);

    // configure the server channel pipeline factory
    final AbstractBitTorrentHandlerFactory bitTorrentHandlerFactory = new BitTorrentHandlerFactory(sslTorrent);
    final AbstractHTTPRequestHandlerFactory httpRequestHandlerFactory = new HTTPRequestHandlerFactory();
    final X509SecurityInfo serverX509SecurityInfo = KeyStoreTestUtils.getServerX509SecurityInfo();
    final ChannelPipelineFactory channelPipelineFactory = new PortUnificationChannelPipelineFactory(
            null, // albusHCNMessageHandlerFactory,
            bitTorrentHandlerFactory,
            httpRequestHandlerFactory,
            serverX509SecurityInfo);

    // configure the server
    final ServerBootstrap serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            Executors.newCachedThreadPool(),
            Executors.newCachedThreadPool()));

    assertEquals("{}", serverBootstrap.getOptions().toString());
    serverBootstrap.setPipelineFactory(channelPipelineFactory);

    // bind and start to accept incoming connections
    serverBootstrap.bind(new InetSocketAddress(SEED_SERVER_PORT));

    final Object testDone_lock = new Object();

    // set up torrent metainfo
    MetaInfo metaInfo;
    Storage storage = null;
    try {
      storage = new Storage(
              new File("data/upload/SentinelDome.jpg"),
              "http://127.0.0.1:" + SEED_SERVER_PORT + "/torrent-tracker/announce",
              true);  // the indicator whether hidden files are excluded
      storage.createPieceHashes();
    } catch (IOException ex) {
      ex.printStackTrace();
      fail(ex.getMessage());
    }
    assertTrue(storage.isComplete());
    metaInfo = storage.getMetaInfo();
    LOGGER.info("metaInfo: " + metaInfo);
    tracker.addMetainfo(metaInfo);
    assertTrue(tracker.isTracking(metaInfo.getURLEncodedInfoHash()));
    assertFalse(tracker.hasPeers(metaInfo.getURLEncodedInfoHash()));
    LOGGER.info("***************************************************************************************");

    // start seeding peer
    final SeedPeer seedPeer = new SeedPeer(metaInfo, storage, sslTorrent);
    final Thread seedPeerThread = new Thread(seedPeer);
    seedPeerThread.start();
    while (true) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ex) {
      }
      if (tracker.hasPeers(metaInfo.getURLEncodedInfoHash())) {
        break;
      }
    }
    LOGGER.info("seed peer updated the tracker");
    LOGGER.info("***************************************************************************************");

    // start leech peer
    final DownloadPeer downloadPeer = new DownloadPeer(metaInfo, testDone_lock);
    final Thread downloadPeerThread = new Thread(downloadPeer);
    downloadPeerThread.start();

    // wait at most a minute for the test to complete
    synchronized (testDone_lock) {
      try {
        testDone_lock.wait(60000);
      } catch (InterruptedException ex) {
        LOGGER.info("interrupted");
      }
    }
    LOGGER.info("test is done");
    seedPeer.quit();

    final Timer timer = new Timer();
    timer.schedule(new ShutdownTimerTask(), 5000);

    // shut down executor threads to exit
    LOGGER.info("releasing server resources");
    serverBootstrap.releaseExternalResources();
    timer.cancel();
  }

  /** Provides a process to run the seeding peer at 127.0.0.1:8088. */
  private static final class SeedPeer implements Runnable {

    /** the indicator that the beginSeeding peer should be halted */
    private final AtomicBoolean isQuit = new AtomicBoolean(false);
    /** the metainfo */
    private final MetaInfo metaInfo;
    /** the torrent file/directory storage */
    private final Storage storage;
    /** the SSL torrent */
    private final SSLTorrent sslTorrent;

    /** Constructs a new SeedPeer instance.
     *
     * @param metaInfo the metainfo
     * @param storage the storage
     * @param sslTorrent he SSL torrent
     */
    SeedPeer(
            final MetaInfo metaInfo,
            final Storage storage,
            final SSLTorrent sslTorrent) {
      this.metaInfo = metaInfo;
      this.storage = storage;
      this.sslTorrent = sslTorrent;
    }

    /** Runs the seeding peer. */
    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
      Thread.currentThread().setName("seed peer");
      sslTorrent.beginSeeding(metaInfo, storage);
      assertTrue(storage.isComplete());
      assertTrue(sslTorrent.isSeeding(metaInfo));

      while (!isQuit.get()) {
        try {
          Thread.sleep(500);
        } catch (InterruptedException ex) {
        }
      }
      sslTorrent.endSeeding(metaInfo);
      assertFalse(sslTorrent.isSeeding(metaInfo));
      LOGGER.info("seed peer completed");
    }

    /** Quits the seeding peer. */
    void quit() {
      isQuit.set(true);
    }
  }

  /** Provides a process to run the download peer at 127.0.0.1:8089. */
  private static final class DownloadPeer implements Runnable, DownloadListener {

    /** the metainfo */
    private final MetaInfo metaInfo;
    /** the lock upon which the main thread will await test completion */
    private final Object testDone_lock;

    /** Constructs a new LeechPeer instance.
     *
     * @param metaInfo
     * @param testDone_lock the lock upon which the main thread will await test completion
     */
    DownloadPeer(final MetaInfo metaInfo, final Object testDone_lock) {
      this.metaInfo = metaInfo;
      this.testDone_lock = testDone_lock;
    }

    /** Runs the download peer. */
    @Override
    public void run() {
      Thread.currentThread().setName("download peer");
      final X509SecurityInfo x509SecurityInfo = KeyStoreTestUtils.getClientX509SecurityInfo();
      final SSLTorrent sslTorrent = new SSLTorrent(
              DOWNLOADER_SERVER_PORT,
              x509SecurityInfo);
      LOGGER.info("download peer: " + sslTorrent.getOurTrackedPeerInfo());
      sslTorrent.download(metaInfo, "data/download/", this);
      assertTrue(sslTorrent.isDownloading(metaInfo));

      try {
        Thread.sleep(5000);
      } catch (InterruptedException ex) {
      }

      LOGGER.info("download peer completed");
      synchronized (testDone_lock) {
        testDone_lock.notifyAll();
      }
    }

    /** Receives notification that the associated download has completed.
     *
     * @param metaInfo the meta info
     */
    @Override
    public void downloadCompleted(final MetaInfo metaInfo) {
      LOGGER.info("download completed: " + metaInfo.getName());
    }
  }

  /** Provides a task to run when the external resources cannot be released. */
  private static final class ShutdownTimerTask extends TimerTask {

    /** Runs the timer task. */
    @Override
    public void run() {
      LOGGER.info("server resources not released");
      Thread.currentThread().setName("shutdown timer task");
      ThreadUtils.logThreads();
      System.exit(0);
    }
  }
}
