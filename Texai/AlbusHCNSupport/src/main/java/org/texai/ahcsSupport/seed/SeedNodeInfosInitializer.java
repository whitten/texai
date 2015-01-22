package org.texai.ahcsSupport.seed;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;
import org.apache.log4j.Logger;
import org.texai.util.ArraySet;
import org.texai.util.TexaiException;
import org.texai.x509.X509Utils;

/**
 * SeedNodeInfosInitializer.java
 *
 * Description: Initializes the locations and credentials of the network seed nodes.
 *
 * Copyright (C) Nov 11, 2014, Stephen L. Reed, Texai.org.
 */
public class SeedNodeInfosInitializer {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(SeedNodeInfosInitializer.class);
  // the seed node infos
  private final Set<SeedNodeInfo> seedNodeInfos = new ArraySet<>();

  /**
   * Creates a new instance of SeedNodeInfosInitializer.
   */
  public SeedNodeInfosInitializer() {
  }

  /**
   * Initializes the locations and credentials of the network seed nodes.
   *
   */
  private void process() {

    try {
      // the demo mint peer
      String qualifiedName = "Mint.SingletonConfigurationAgent.SingletonConfigurationRole";
      int port = 5048;
      String cerfificateFilePath = "../Main/data/SingletonConfiguration.crt";
      if (!(new File(cerfificateFilePath)).exists()) {
        throw new TexaiException("cerfificate path not found " + cerfificateFilePath);
      }
      X509Certificate x509Certificate = X509Utils.readX509Certificate(new FileInputStream(cerfificateFilePath));

      // Within a set of docker containers hosted on the development server, the hostname Mint resolves to the
      // docker subnet IP address of the Mint container in that set.
      SeedNodeInfo seedNodeInfo = new SeedNodeInfo(
              qualifiedName,
              "Mint", // hostName,
              port,
              x509Certificate);
      seedNodeInfos.add(seedNodeInfo);
      LOGGER.info(seedNodeInfo);

      // Otherwise Within the development LAN, the IP address is for the docker host, one of whose containers is the Mint
      // container.
      seedNodeInfo = new SeedNodeInfo(
              qualifiedName,
              "192.168.0.7", // hostName,
              port,
              x509Certificate);
      seedNodeInfos.add(seedNodeInfo);
      LOGGER.info(seedNodeInfo);

      // On the internet the texai.dyndns.org name resolves to the IP address of the docker host, one of whose containers is the Mint
      // container.
      seedNodeInfo = new SeedNodeInfo(
              qualifiedName,
              "texai.dyndns.org", // hostName,
              port,
              x509Certificate);
      seedNodeInfos.add(seedNodeInfo);
      LOGGER.info(seedNodeInfo);

      // other seeds ...
      final String seedNodeInfosFilePath = "../Main/data/SeedNodeInfos.ser";
      LOGGER.info("writing " + seedNodeInfosFilePath);
      try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(seedNodeInfosFilePath))) {
        objectOutputStream.writeObject(seedNodeInfos);
      }
    } catch (CertificateException | NoSuchProviderException | IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public static void main(final String[] args) {
    final SeedNodeInfosInitializer seedNodeInfosInitializer = new SeedNodeInfosInitializer();
    seedNodeInfosInitializer.process();
  }
}
