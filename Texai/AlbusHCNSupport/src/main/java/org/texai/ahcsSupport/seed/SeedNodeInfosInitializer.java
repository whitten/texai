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
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation,
 * Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
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
      String cerfificateFilePath = "/home/reed/docker/Mint/Main-1.0/data/SingletonConfiguration.crt";
      if (!(new File(cerfificateFilePath)).exists()) {
        throw new TexaiException("cerfificate path not found " + cerfificateFilePath);
      }
      X509Certificate x509Certificate = X509Utils.readX509Certificate(new FileInputStream(cerfificateFilePath));

      // Within a set of docker containers hosted on the development server, the hostname Mint resolves to the
      // docker subnet IP address of the Mint container in that set.

      // Otherwise Within the development LAN, the Mint name resolves to the IP address of the docker host, one of whose containers is the Mint
      // container.
      SeedNodeInfo seedNodeInfo = new SeedNodeInfo(
              qualifiedName,
              "Mint", // hostName,
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
