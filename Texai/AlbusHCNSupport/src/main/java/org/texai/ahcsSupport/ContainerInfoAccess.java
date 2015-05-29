package org.texai.ahcsSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.util.NetworkUtils;
import org.texai.util.StringUtils;

/**
 * ContainerInfoAccess.java
 *
 * Description: Provides convenient methods to access ContainerInfo persistent objects.
 *
 * Copyright (C) Mar 31, 2015, Stephen L. Reed.
 */
public class ContainerInfoAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerInfo.class);
  // the RDF entity manager which handles object persistence
  private final RDFEntityManager rdfEntityManager;
  // the cache of container infos
  private final Map<String, ContainerInfo> containerInfoDictionary = new HashMap<>();
  // the network name, mainnet or testnet
  private final String networkName;
  // all of the super peer container names
  private final Set<String> allSuperPeerContainerNames = new HashSet<>();

  /**
   * Creates a new instance of ContainerInfoAccess.
   *
   * @param rdfEntityManager the RDF entity manager which handles object persistence
   * @param networkName the network name, mainnet or testnet
   */
  public ContainerInfoAccess(
          final RDFEntityManager rdfEntityManager,
          final String networkName) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";
    assert networkName.equals(NetworkUtils.TEXAI_MAINNET) || networkName.equals(NetworkUtils.TEXAI_TESTNET) :
            "invalid network name " + networkName;

    this.rdfEntityManager = rdfEntityManager;
    this.networkName = networkName;
  }

  /**
   * Initializes the container information dictionary.
   */
  public void initializeContainerInfos() {
    LOGGER.info("Initializing the network container configuration.");
    synchronized (containerInfoDictionary) {
      if (networkName.equals(NetworkUtils.TEXAI_MAINNET)) {
//        initializeContainerInfo(
//                "aicoin01-a", // containerName
//                null, // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isClientGateway
//                false, // isFirstContainer
//                false, // isBlockExplorer
//                null); // superPeerContainers
//        initializeContainerInfo(
//                "aicpeer-bravo1030", // containerName
//                null, // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isFirstContainer
//                false, // isClientGateway
//                false, // isBlockExplorer
//                null); // superPeerContainers
        initializeContainerInfo(
                "crawfish", // containerName
                "lacrawfish.ddns.net", // ipAddress
                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
                true, // isSuperPeer
                false, // isFirstContainer
                false, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
//        initializeContainerInfo(
//                "mediamaven", // containerName
//                null, // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isFirstContainer
//                false, // isClientGateway
//                false, // isBlockExplorer
//                null); // superPeerContainers
//        initializeContainerInfo(
//                "nexus", // containerName
//                "nexus.asuscomm.com", // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isFirstContainer
//                false, // isClientGateway
//                false, // isBlockExplorer
//                null); // superPeerContainers
//        initializeContainerInfo(
//                "pegasus", // containerName
//                null, // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isFirstContainer
//                false, // isClientGateway
//                false, // isBlockExplorer
//                null); // superPeerContainers
//        initializeContainerInfo(
//                "matrix", // containerName
//                null, // ipAddress
//                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
//                true, // isSuperPeer
//                false, // isFirstContainer
//                false, // isClientGateway
//                false, // isBlockExplorer
//                null); // superPeerContainers
        initializeContainerInfo(
                "turing", // containerName
                "texai.dyndns.org", // ipAddress
                NetworkUtils.TEXAI_MAINNET_PORT, // texai protocol port
                true, // isSuperPeer
                true, // isFirstContainer
                true, // isClientGateway
                true, // isBlockExplorer
                null); // superPeerContainers
        initializeContainerInfo(
                "alice", // containerName, on the same host as turing
                "alice", // ipAddress, actually the local docker container name
                NetworkUtils.TEXAI_MAINNET_PORT + 1, // texai protocol port
                true, // isSuperPeer
                false, // isFirstContainer
                false, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
        ContainerInfo containerInfo = initializeContainerInfo(
                "bob", // containerName, on the same host as turing
                "bob", // ipAddress, actually the local docker container name
                NetworkUtils.TEXAI_MAINNET_PORT + 2, // texai protocol port
                false, // isSuperPeer
                false, // isFirstContainer
                false, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
        // bob is an ordinary peer having connections to the super peers turing and alice
        containerInfo.addSuperPeerContainerName("turing");
        containerInfo.addSuperPeerContainerName("alice");
      } else {
        assert networkName.equals(NetworkUtils.TEXAI_TESTNET);
        initializeContainerInfo(
                "TestAlice", // containerName
                "TestAlice", // ipAddress
                45049, // texai protocol port
                true, // isSuperPeer
                false, // isFirstContainer
                true, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
        initializeContainerInfo(
                "TestBlockchainExplorer", // containerName
                "TestBlockchainExplorer", // ipAddress
                45050, // texai protocol port
                true, // isSuperPeer
                false, // isFirstContainer
                false, // isClientGateway
                true, // isBlockExplorer
                null); // superPeerContainers
        ContainerInfo containerInfo = initializeContainerInfo(
                "TestBob", // containerName
                "TestBob", // ipAddress
                45051, // texai protocol port
                false, // isSuperPeer
                false, // isFirstContainer
                true, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
        containerInfo.addSuperPeerContainerName("TestMint");
        containerInfo.addSuperPeerContainerName("TestAlice");
        initializeContainerInfo(
                "TestMint", // containerName
                "TestMint", // ipAddress
                45048, // texai protocol port
                true, // isSuperPeer
                true, // isFirstContainer
                false, // isClientGateway
                false, // isBlockExplorer
                null); // superPeerContainers
      }
    }
    final List<String> sortedAllSuperPeerContainerNames = new ArrayList<>(allSuperPeerContainerNames);
    Collections.sort(sortedAllSuperPeerContainerNames);
    LOGGER.info("All super peer container names: " + sortedAllSuperPeerContainerNames);
  }

  /**
   * Initializes a container info.
   *
   * @param containerName the container name
   * @param ipAddress the ip address
   * @param port the port, usually 5048 for mainnet and 45048 for testnet
   * @param isSuperPeer the indicator whether this container is a super peer
   * @param isFirstContainer the indicator whether this container is the first host of all the network singleton agents, when the network
   * restarts
   * @param isClientGateway the indicator whether this node is a client gateway, accepting connections from wallet clients
   * @param isBlockExplorer the indicator whether this node is a block explorer, serving the Insight application
   * @param superPeerContainerNames the names of the few super peer containers to which a non-super peer container connects
   *
   * @return the initialized container info
   */
  private ContainerInfo initializeContainerInfo(
          final String containerName,
          final String ipAddress,
          final int port,
          final boolean isSuperPeer,
          final boolean isFirstContainer,
          final boolean isClientGateway,
          final boolean isBlockExplorer,
          final Set<String> superPeerContainerNames) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must be a non-empty string";

    if (isSuperPeer) {
      allSuperPeerContainerNames.add(containerName);
    }
    final ContainerInfo containerInfo = new ContainerInfo(
            containerName,
            isSuperPeer,
            isFirstContainer,
            isClientGateway,
            isBlockExplorer);
    if (StringUtils.isNonEmptyString(ipAddress)) {
      containerInfo.setIpAddress(ipAddress);
    }
    containerInfo.setTexaiProtocolPort(port);
    if (superPeerContainerNames != null) {
      containerInfo.getSuperPeerContainerNames().addAll(superPeerContainerNames);
    }
    containerInfoDictionary.put(containerName, containerInfo);
    LOGGER.info("  initialized container information " + containerInfo);
    return containerInfo;
  }

  /**
   * Persists the ContainerInfo objects.
   */
  public void persistContainerInfos() {
    LOGGER.info("Persisting ContainerInfos ...");
    synchronized (containerInfoDictionary) {
      final Set<ContainerInfo> newContainerInfos = new HashSet<>(containerInfoDictionary.values());
      final Iterator<ContainerInfo> containerInfos_iter = rdfEntityManager.rdfEntityIterator(ContainerInfo.class);
      while (containerInfos_iter.hasNext()) {
        final ContainerInfo loadedContainerInfo = containerInfos_iter.next();
        loadedContainerInfo.getId(); // force otherwise lazy instantiation
        final ContainerInfo containerInfo = containerInfoDictionary.get(loadedContainerInfo.getContainerName());
        if (containerInfo == null) {
          // found and no longer used
          rdfEntityManager.remove(loadedContainerInfo);
        } else {
          // found and used
          LOGGER.info("  " + loadedContainerInfo);
          newContainerInfos.remove(containerInfo);
          // update and persist
          if (StringUtils.isNonEmptyString(containerInfo.getIpAddress())) {
            loadedContainerInfo.setIpAddress(containerInfo.getIpAddress());
          }
          loadedContainerInfo.setIsAlive(containerInfo.isAlive());
          rdfEntityManager.persist(loadedContainerInfo);
        }
      }
      // persist new container infos
      for (final ContainerInfo containerInfo : newContainerInfos) {
        LOGGER.info("  " + containerInfo);
        rdfEntityManager.persist(containerInfo);
      }
    }
  }

  /**
   * Loads the ContainerInfo objects from the persistent store.
   */
  public void loadContainerInfos() {
    LOGGER.info("Loading ContainerInfos ...");
    synchronized (containerInfoDictionary) {
      containerInfoDictionary.clear();
      final Iterator<ContainerInfo> containerInfo_iter = rdfEntityManager.rdfEntityIterator(ContainerInfo.class);
      while (containerInfo_iter.hasNext()) {
        final ContainerInfo containerInfo = containerInfo_iter.next();
        containerInfo.getId(); // force otherwise lazy instantiation
        LOGGER.info("  " + containerInfo);
        containerInfoDictionary.put(containerInfo.getContainerName(), containerInfo);
      }
    }
  }

  /**
   * Adds a new container information object.
   *
   * @param containerInfo the new container information object
   */
  public void addContainerInfo(final ContainerInfo containerInfo) {
    //Preconditions
    assert containerInfo != null : "containerInfo must not be null";

    synchronized (containerInfoDictionary) {
      containerInfoDictionary.put(containerInfo.getContainerName(), containerInfo);
    }
  }

  /**
   * Gets the container info by its name.
   *
   * @param containerName the container name
   *
   * @return the container info
   */
  public ContainerInfo getContainerInfo(final String containerName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(containerName) : "containerName must a non-empty string";

    synchronized (containerInfoDictionary) {
      return containerInfoDictionary.get(containerName);
    }
  }

  /**
   * Gets the container info objects.
   *
   * @return the container infos
   */
  public List<ContainerInfo> getContainerInfos() {
    synchronized (containerInfoDictionary) {
      final List<ContainerInfo> containerInfos = new ArrayList<>(containerInfoDictionary.values());
      Collections.sort(containerInfos);
      return containerInfos;
    }
  }

  /**
   * Replaces the current container infos with the given ones.
   *
   * @param containerInfos the given container infos
   */
  public void updateContainerInfos(final Collection<ContainerInfo> containerInfos) {
    //Preconditions
    assert containerInfos != null : "containerInfos must not be null";

    synchronized (containerInfoDictionary) {
      containerInfoDictionary.clear();
      for (final ContainerInfo containerInfo : containerInfos) {
        containerInfoDictionary.put(containerInfo.getContainerName(), containerInfo);
      }
    }
  }

  /**
   * Returns whether the container infos are consistent with one another.
   *
   * @return whether the container infos are consistent
   */
  public boolean areContainerInfosConsistent() {
    synchronized (containerInfoDictionary) {
      final Iterator<ContainerInfo> containerInfos_iter = containerInfoDictionary.values().iterator();
      while (containerInfos_iter.hasNext()) {
        final ContainerInfo containerInfo = containerInfos_iter.next();
        for (final String containerName : containerInfo.getSuperPeerContainerNames()) {
          if (!containerInfoDictionary.containsKey(containerName)) {
            LOGGER.info("invalid super peer container name '" + containerName + "' referenced in " + containerInfo);
            return false;
          }
        }
      }
    }
    return true;
  }

  /** Gets all the super peer container names.
   *
   * @return all the super peer container names
   */
  public Set<String> getAllSuperPeerContainerNames() {
    return allSuperPeerContainerNames;
  }

}
