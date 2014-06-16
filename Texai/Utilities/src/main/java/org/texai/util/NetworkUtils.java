/*
 * NetworkUtils.java
 *
 * Created on Jan 12, 2010, 9:17:04 AM
 *
 * Description: NetworkUtils utilities.
 *
 * Copyright (C) Jan 12, 2010 reed.
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
package org.texai.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.apache.log4j.Logger;

/** NetworkUtils utilities.
 *
 * @author reed
 */
@NotThreadSafe
public final class NetworkUtils {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(NetworkUtils.class);
  /** the lower bound of dynamic TCP ports */
  static final int LOWER_PORT_BOUND = 49152;
  /** the upper bound of dynamic TCP ports */
  static final int UPPER_PORT_BOUND = 65535;
  /** the server port number file path */
  static final String SERVER_PORT_PATH = "data/server-port.txt";
  /** the IANA registered service port for Texai: http://www.iana.org/assignments/port-numbers */
  public static final int TEXAI_PORT = 5048;
  /** the launcher port */
  public static final int LAUNCHER_PORT = 5049;
  /** the socket connection timeout */
  public static final int CONNECTION_TIMEOUT = 10000;

  /** Private constructor because this class is never instantiated. */
  private NetworkUtils() {
  }

  /** Returns whether this set of nodes is running as a cloud service.
   *
   * @return whether this set of nodes is running as a cloud service
   */
  public static boolean isCloudService() {
    return true;
  }

  /** Returns whether this set of nodes is running as an Internet-distributed Texai instance.
   *
   * @return whether this set of nodes is running as an Internet-distributed Texai instance
   */
  public static boolean isDistributedTexaiInstance() {
    return !isCloudService();
  }

  /** Gets the host name.
   *
   * @return the host name
   */
  public static String getHostName() {
    final String hostName = getLocalHostAddress().getHostName();
    if (hostName.endsWith(".local")) {
      return hostName.substring(0, hostName.length() - 6);
    } else {
      return hostName;
    }
  }

  /** Obtains the local host address in situations where it cannot be obtained solely from the InetAddress class.
   *
   * @return the local host address
   */
  public static InetAddress getLocalHostAddress() {
    Enumeration<NetworkInterface> networkInterfaces = null;
    try {
      networkInterfaces = NetworkInterface.getNetworkInterfaces();
    } catch (SocketException ex) {
      throw new TexaiException(ex);
    }

    while (networkInterfaces.hasMoreElements()) {
      final NetworkInterface networkInterface = networkInterfaces.nextElement();
      final Enumeration<InetAddress> address = networkInterface.getInetAddresses();
      while (address.hasMoreElements()) {
        final InetAddress inetAddress = address.nextElement();
        if (!inetAddress.isLoopbackAddress()
                && !(inetAddress.getHostAddress().indexOf(":") > -1)) {
          return inetAddress;
        }
      }
    }
    try {
      return InetAddress.getLocalHost();
    } catch (UnknownHostException ex) {
      throw new TexaiException(ex);
    }
  }

  /** Returns whether the given internet address is reserved for private networks, such as those behind a NAT router.
   *
   * @param inetAddress the given internet address
   * @return whether the given internet address is reserved for private networks
   */
  public static boolean isPrivateNetworkAddress(final InetAddress inetAddress) {
    //Preconditions
    assert inetAddress != null : "inetAddress must not be null";

    final String inetAddressString = inetAddress.getHostAddress();
    return inetAddressString.startsWith("192.168.")
            || inetAddressString.startsWith("10.")
            || inetAddressString.startsWith("172.");
  }

  /** Return the MAC address of the current network interface for this computer.
   *
   * @return the MAC address of the current network interface for this computer
   */
  public static List<Byte> getMACAddress() {
    final List<Byte> macAddress = new ArrayList<>();
    try {
      final InetAddress inetAddress = getLocalHostAddress();
      // get NetworkInterface for the current host and then read the hardware address.
      final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(inetAddress);
      if (networkInterface == null) {
        throw new TexaiException("Network Interface for the address " + inetAddress + " is not found.");
      } else {
        final byte[] macAddressBytes = networkInterface.getHardwareAddress();
        if (macAddressBytes == null) {
          throw new TexaiException("Address doesn't exist or is not accessible.");
          // extract each byte of mac address and convert it to hex with the following format 08-00-27-DC-4A-9E.
        } else {
          final int macAddressBytes_len = macAddressBytes.length;
          System.out.println("macAddressBytes_len: " + macAddressBytes_len);
          for (int i = 0; i < macAddressBytes_len; i++) {
            final byte macAddressByte = macAddressBytes[i];
            macAddress.add(macAddressByte);
          }
        }
      }
    } catch (SocketException ex) {
      throw new TexaiException(ex);
    }
    return macAddress;
  }

  /** Returns the string representation of the MAC address of the current network interface for this computer.
   *
   * @return the string representation of the MAC address of the current network interface for this computer
   */
  public static String getMACAddressString() {
    final StringBuilder stringBuilder = new StringBuilder();
    final List<Byte> macAddress = getMACAddress();
    boolean isFirst = true;
    for (final Byte macAddressByte : macAddress) {
      if (isFirst) {
        isFirst = false;
      } else {
        stringBuilder.append('-');
      }
      stringBuilder.append(ByteUtils.toHex(macAddressByte));
    }
    return stringBuilder.toString();
  }

  /** Returns the SSL server port number, which is allocated from the
   * dynamic range: 49152–65535, as described in
   * http://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers
   *
   * @return the SSL server port number
   */
  public static int getDynamicServerPort() {
    final File serverPortFile = new File(SERVER_PORT_PATH);
    int serverPort = 0;
    if (serverPortFile.exists()) {
      // subsequent times use the recorded server port number
      try {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(serverPortFile))) {
          final String line = bufferedReader.readLine();
          if (line == null) {
            throw new TexaiException("missing SSL server port in: " + SERVER_PORT_PATH);
          }
          serverPort = Integer.parseInt(line.trim());
        }
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    } else {
      try {
        final File dataDirectory = new File("data");
        if (!dataDirectory.exists()) {
          LOGGER.info("creating data directory");
          final boolean isDirectoryCreated = dataDirectory.mkdir();
          if (!isDirectoryCreated) {
            throw new TexaiException("cannot create data directory");
          }
        }
        // first time allocate in the dynamic range
        serverPort = getRandomDynamicServerPort();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(serverPortFile))) {
          bufferedWriter.append(String.valueOf(serverPort));
          bufferedWriter.newLine();
        }
      } catch (IOException ex) {
        throw new TexaiException(ex);
      }
    }
    return serverPort;
  }

  /** Returns a dynamic server port number, which is allocated from the
   * dynamic range: 49152–65535, as described in
   * http://en.wikipedia.org/wiki/List_of_TCP_and_UDP_port_numbers
   *
   * @return the SSL server port number
   */
  public static int getRandomDynamicServerPort() {
    return (int) (LOWER_PORT_BOUND + Math.random() * (UPPER_PORT_BOUND - LOWER_PORT_BOUND));
  }

  /** Returns whether the given host is accepting connections on the given port.
   *
   * @param host the given host
   * @param port the given port
   * @return whether the given host is accepting connections on the given port
   */
  public static boolean isHostAvailable(final String host, final int port) {
    //Preconditions
    assert host != null : "the host must not be null";
    assert !host.isEmpty() : "the host must not be empty";
    assert port >= 0 && port <= UPPER_PORT_BOUND : "the port be within the range 0 - 65535";

    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
    } catch (Exception ex) {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ex1) {
        }
      }
      return false;
    }
    try {
      socket.close();
    } catch (IOException ex1) {
      return false;
    }
    return true;
  }

  /** Returns a socket address formed from the given URL string.
   *
   * @param urlString the given URL string
   * @return a socket address formed from the given URL string
   */
  public static InetSocketAddress makeInetSocketAddress(final String urlString) {
    //Preconditions
    assert urlString != null : "the urlString must not be null";
    assert !urlString.isEmpty() : "the urlString must not be empty";

    final URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException ex) {
      throw new TexaiException(ex);
    }
    return new InetSocketAddress(url.getHost(), url.getPort());
  }
}
