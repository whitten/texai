package org.texai.ahcsSupport.seed;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.texai.util.ArraySet;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

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
  // the network name, mainnet or testnet
  private final String networkName;
  // the seed containers and host names
  private final List<SeedInfo> seedInfos = new ArrayList<>();

  /**
   * Creates a new instance of SeedNodeInfosInitializer.\
   *
   * @param networkName the network name, mainnet or testnet
   */
  public SeedNodeInfosInitializer(final String networkName) {
    assert NetworkUtils.TEXAI_MAINNET.equals(networkName) || NetworkUtils.TEXAI_TESTNET.equals(networkName) :
            "network name must be " + NetworkUtils.TEXAI_MAINNET + " or " + NetworkUtils.TEXAI_TESTNET;

    this.networkName = networkName;
  }

  /**
   * Initializes the locations of the network seed nodes.
   *
   * @return
   */
  public Set<SeedNodeInfo> process() {

    LOGGER.info("Reading network.conf file...");
    try (
            final BufferedReader bufferedReader
            = new BufferedReader(new InputStreamReader(new FileInputStream("network.conf"), "UTF-8"))) {
      int lineCnt = 0;
      while (true) {
        String line = bufferedReader.readLine();
        if (line == null) {
          break;
        }
        lineCnt++;
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("input: '" + line + "'");
        }
        line = line.trim();
        if (line.startsWith("#") || line.isEmpty()) {
          continue;
        }
        if (line.indexOf('=') == -1) {
          throw new TexaiException("invalid network.conf line, missing '=' (" + lineCnt + "): " + line);
        }
        final String[] parts = line.split("=");
        if (parts.length != 2) {
          throw new TexaiException("invalid network.conf line, one '=' required (" + lineCnt + "): " + line);
        }
        final String command = parts[0].trim();
        final String operand = parts[1].trim();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("command: '" + command + "'");
          LOGGER.debug("operand: '" + operand + "'");
        }
        if (command.isEmpty()) {
          throw new TexaiException("invalid network.conf line, operator is missing (" + lineCnt + "): " + line);
        }
        if (operand.isEmpty()) {
          throw new TexaiException("invalid network.conf line, operand is missing (" + lineCnt + "): " + line);
        }

        switch (command) {
          case "network":
            if (!operand.equals(networkName)) {
              throw new TexaiException("invalid network.conf line, network must be " + networkName + " (" + lineCnt + "): " + line);
            }
            break;
          case "seed":
            if (!operand.contains(",")) {
              throw new TexaiException("invalid network.conf line, seed info missing ',' (" + lineCnt + "): " + line);
            }
            final String seedInfoParts[] = operand.split(",");
            final String containerName = seedInfoParts[0].trim();
            final String hostName = seedInfoParts[1].trim();
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("containerName: '" + containerName + "'");
              LOGGER.debug("hostName: '" + hostName + "'");
            }
            if (containerName.isEmpty()) {
              throw new TexaiException("invalid network.conf line, containerName is missing (" + lineCnt + "): " + line);
            }
            if (hostName.isEmpty()) {
              throw new TexaiException("invalid network.conf line, hostName is missing (" + lineCnt + "): " + line);
            }

            seedInfos.add(new SeedInfo(containerName, hostName));
            break;
          default:
            throw new TexaiException("invalid network.conf line, invalid command (" + lineCnt + "): " + line);
        }
        final int port = NetworkUtils.toNetworkPort(networkName);
        seedInfos.stream().sorted().forEach((SeedInfo seedInfo) -> {
          final String qualifiedName = seedInfo.containerName + ".ContainerOperationAgent.ContainerSingletonConfigurationRole";
          final SeedNodeInfo seedNodeInfo = new SeedNodeInfo(
                  qualifiedName,
                  seedInfo.hostName,
                  port);
          LOGGER.info(seedNodeInfo);
          seedNodeInfos.add(seedNodeInfo);
        });

      }
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
    return seedNodeInfos;
  }

  /**
   * Provides a container for seed information.
   */
  class SeedInfo {

    // the container name
    private final String containerName;
    // the host name
    private final String hostName;

    /**
     * Creates a new SeedInfo instance.
     *
     * @param containerName the container name
     * @param hostName the host name
     */
    SeedInfo(
            final String containerName,
            final String hostName) {
      //Preconditions
      assert StringUtils.isNonEmptyString(containerName) : "containerName must not be null";
      assert StringUtils.isNonEmptyString(hostName) : "hostName must not be null";

      this.containerName = containerName;
      this.hostName = hostName;
    }
  }
}
