package org.texai.ahcsSupport.seed;

import java.net.InetAddress;
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

    // the demo mint peer
    try {
    String qualifiedName = "Mint.SingletonConfigurationRole";
    InetAddress inetAddress = InetAddress.getLocalHost();
    int port = 35048;
    String cerfificateFilePath = "/home/reed/docker/Mint/";
    X509Certificate x509Certificate = X509Utils.readX509Certificate(null);



    } catch (Exception ex) {
      throw new TexaiException(ex);
    }



  }

  /**
   * Executes this application.
   *
   * @param args the command line arguments - unused
   */
  public void main(final String[] args) {
    final SeedNodeInfosInitializer seedNodeInfosInitializer = new SeedNodeInfosInitializer();
    seedNodeInfosInitializer.process();
  }
}
