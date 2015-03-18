/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.texai.skill.aicoin.support;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.GetDataMessage;
import com.google.bitcoin.core.Message;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.Transaction;
import java.util.List;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class LocalBitcoindAdapterTest {

  // the logger

  public static final Logger LOGGER = Logger.getLogger(LocalBitcoindAdapterTest.class);

  public LocalBitcoindAdapterTest() {
  }

  @BeforeClass
  public static void setUpClass() {
    String[] cmdArray = {
      "sh",
      "-c",
      ""
    };
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("~/docker/SuperPeer/bin/aicoin-qt -debug -shrinkdebugfile=1 -datadir=").append(System.getProperty("user.home")).append("/.aicoin");
    cmdArray[2] = stringBuilder.toString();
    LOGGER.info("Launching the aicoin-qt instance.");
    LOGGER.info("  shell cmd: " + cmdArray[2]);
    AICoinUtils.executeHostCommandWithoutWaitForCompletion(cmdArray);
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

}
