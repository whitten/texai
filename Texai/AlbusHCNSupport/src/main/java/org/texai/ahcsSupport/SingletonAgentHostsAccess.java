package org.texai.ahcsSupport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.texai.ahcsSupport.skill.BasicNodeRuntime;
import org.texai.kb.persistence.RDFEntityManager;
import org.texai.ahcsSupport.domainEntity.ContainerInfo;
import org.texai.ahcsSupport.domainEntity.SingletonAgentHosts;
import org.texai.util.StringUtils;

/**
 * SingletonAgentHostsAccess.java
 *
 * Description: Provides convenient access to the SingletonHosts persisted instances.
 *
 * Copyright (C) Mar 31, 2015, Stephen L. Reed.
 */
public class SingletonAgentHostsAccess {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ContainerInfo.class);
  // the RDF entity manager
  private final RDFEntityManager rdfEntityManager;
  // the parent basic node runtime
  private final BasicNodeRuntime basicNodeRuntime;
  // the set of singleton agent hosts
  private final Set< SingletonAgentHosts> singletonAgentHostsSet = new HashSet<>();

  /**
   * Creates a new instance of SingletonAgentHostsAccess.
   *
   * @param rdfEntityManager the RDF entity manager
   * @param basicNodeRuntime the parent basic node runtime
   */
  public SingletonAgentHostsAccess(
          final RDFEntityManager rdfEntityManager,
          final BasicNodeRuntime basicNodeRuntime) {
    //Preconditions
    assert rdfEntityManager != null : "rdfEntityManager must not be null";
    assert basicNodeRuntime != null : "basicNodeRuntime must not be null";

    this.rdfEntityManager = rdfEntityManager;
    this.basicNodeRuntime = basicNodeRuntime;
  }

  /**
   * Initializes the singleton agent hosts.
   */
  public void initializeSingletonAgentsHosts() {
    synchronized (singletonAgentHostsSet) {
      final Map<String, String> singletonAgentDictionary = new HashMap<>();
      singletonAgentDictionary.put("NetworkOperationAgent", "Mint");
      singletonAgentDictionary.put("NetworkSingletonConfigurationAgent", "Mint");
      singletonAgentDictionary.put("TopmostFriendshipAgent", "Mint");
      singletonAgentDictionary.put("AICFinancialAccountingAndControlAgent", "Mint");
      singletonAgentDictionary.put("AICMintAgent", "Mint");
      singletonAgentDictionary.put("AICNetworkEpisodicMemoryAgent", "Mint");
      singletonAgentDictionary.put("AICNetworkOperationAgent", "Mint");
      singletonAgentDictionary.put("AICNetworkSeedAgent", "Mint");
      singletonAgentDictionary.put("AICPrimaryAuditAgent", "Mint");
      singletonAgentDictionary.put("AICRecoveryAgent", "Mint");
      singletonAgentDictionary.put("AICRewardAllocationAgent", "Mint");

      final DateTime effectiveDateTime = new DateTime(
              2015, // year
              01, // monthOfYear,
              1, // dayOfMonth
              12, // hourOfDay
              0, // minuteOfHour,
              0, // secondOfMinute,
              0, // millisOfSecond,
              DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
      final DateTime terminationDateTime = new DateTime(
              2015, // year
              12, // monthOfYear,
              30, // dayOfMonth
              12, // hourOfDay
              15, // minuteOfHour,
              5, // secondOfMinute,
              0, // millisOfSecond,
              DateTimeZone.forTimeZone(TimeZone.getTimeZone("CST"))); // zone
      final SingletonAgentHosts singletonAgentHosts = new SingletonAgentHosts(
              singletonAgentDictionary,
              effectiveDateTime,
              terminationDateTime);
      rdfEntityManager.persist(singletonAgentHosts);
      singletonAgentHostsSet.add(singletonAgentHosts);
    }
  }

  /**
   * Loads the SingletonAgentHosts objects from the persistent store.
   */
  public void loadSingletonAgentHosts() {
    LOGGER.info("Singleton agents and hosts ...");
    synchronized (singletonAgentHostsSet) {
      singletonAgentHostsSet.clear();
      final Iterator<SingletonAgentHosts> singletonAgentHosts_iter = rdfEntityManager.rdfEntityIterator(SingletonAgentHosts.class);
      while (singletonAgentHosts_iter.hasNext()) {
        final SingletonAgentHosts singletonAgentHosts = singletonAgentHosts_iter.next();
        singletonAgentHosts.getId(); // force otherwise lazy instantiation
        LOGGER.info("  " + singletonAgentHosts);
        singletonAgentHostsSet.add(singletonAgentHosts);
      }
    }
  }

  /**
   * Returns whether the given agent is a network singleton currently hosted by this container.
   *
   * @param agentName the given agent's name
   *
   * @return whether the given agent is a network singleton currently hosted by this container
   */
  public boolean isSingletonAgent(final String agentName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(agentName) : "agentName must be a non-empty string";

    for (final SingletonAgentHosts singletonAgentHosts : singletonAgentHostsSet) {
      if (singletonAgentHosts.getEffectiveDateTime().isBeforeNow() && singletonAgentHosts.getTerminationDateTime().isAfterNow()) {
        final String singletonAgentContainerName = singletonAgentHosts.getSingletonAgentDictionary().get(agentName);
        if (singletonAgentContainerName != null && singletonAgentContainerName.equals(basicNodeRuntime.getContainerName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Gets the effective singleton agent hosts object.
   *
   * @return the effective singleton agent hosts object, or null if none are effective now
   */
  public SingletonAgentHosts getEffectiveSingletonAgentHosts() {
    for (final SingletonAgentHosts singletonAgentHosts : singletonAgentHostsSet) {
      if (singletonAgentHosts.getEffectiveDateTime().isBeforeNow() && singletonAgentHosts.getTerminationDateTime().isAfterNow()) {
        return singletonAgentHosts;
      }
    }
    return null;
  }
  /**
   * Updates the singleton agent hosts.
   *
   * @param singletonAgentHosts the singleton agent hosts
   */
  public void updateSingletonAgentHosts(final SingletonAgentHosts singletonAgentHosts) {
    //Preconditions
    assert singletonAgentHosts != null : "singletonAgentHosts must not be null";

    // remove a possible previous entry with a matching effective date
    singletonAgentHostsSet.remove(singletonAgentHosts);

    singletonAgentHostsSet.add(singletonAgentHosts);
  }
}
