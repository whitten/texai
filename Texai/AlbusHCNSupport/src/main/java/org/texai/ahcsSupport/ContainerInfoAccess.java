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
    synchronized (containerInfoDictionary) {
      if (networkName.equals(NetworkUtils.TEXAI_MAINNET)) {
        LOGGER.info("");
        final ContainerInfo mintNodeInfo = new ContainerInfo("Mint");
        mintNodeInfo.setIpAddress("texai.dyndns.org");
        mintNodeInfo.setIsSuperPeer(true);
        containerInfoDictionary.put("Mint", mintNodeInfo);
      } else {
        assert networkName.equals(NetworkUtils.TEXAI_TESTNET);
        final ContainerInfo mintNodeInfo = new ContainerInfo("Mint");
        mintNodeInfo.setIpAddress("Mint");
        mintNodeInfo.setIsSuperPeer(true);
        final ContainerInfo aliceNodeInfo = new ContainerInfo("Alice");
        aliceNodeInfo.setIpAddress("Alice");
        aliceNodeInfo.setIsSuperPeer(true);
        containerInfoDictionary.put("Alice", aliceNodeInfo);
      }
    }
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
          loadedContainerInfo.setIsSuperPeer(containerInfo.isSuperPeer());
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

}
