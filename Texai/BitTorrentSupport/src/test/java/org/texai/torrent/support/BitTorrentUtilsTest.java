/*
 * BitTorrentUtilsTest.java
 *
 * Created on Jun 30, 2008, 5:16:14 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 9, 2010 reed.
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
package org.texai.torrent.support;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.util.ByteUtils;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class BitTorrentUtilsTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentUtilsTest.class);

  public BitTorrentUtilsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of isBitTorrentHandshake method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentHandshake() {
    LOGGER.info("isBitTorrentHandshake");
    int magic1 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentHandshake(magic1));
    magic1 = BitTorrentConstants.BIT_TORRENT_HANDSHAKE_PROTOCOL;
    assertTrue(BitTorrentUtils.isBitTorrentHandshake(magic1));
  }

  /**
   * Test of isBitTorrentKeepAlive method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentKeepAlive() {
    LOGGER.info("isBitTorrentKeepAlive");
    int magic1 = 1;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentKeepAlive(magic1, magic2, magic3, magic4));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = 0;
    assertTrue(BitTorrentUtils.isBitTorrentKeepAlive(magic1, magic2, magic3, magic4));
  }

  /**
   * Test of isBitTorrentChoke method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentChoke() {
    LOGGER.info("isBitTorrentChoke");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentChoke(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_CHOKE_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_CHOKE_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentChoke(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentUnchoke method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentUnchoke() {
    LOGGER.info("isBitTorrentUnchoke");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentUnchoke(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_UNCHOKE_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentUnchoke(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentInterested method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentInterested() {
    LOGGER.info("isBitTorrentInterested");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentInterested(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_INTERESTED_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_INTERESTED_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentInterested(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentNotInterested method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentNotInterested() {
    LOGGER.info("isBitTorrentNotInterested");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentNotInterested(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_NOT_INTERESTED_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_NOT_INTERESTED_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentNotInterested(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentHave method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentHave() {
    LOGGER.info("isBitTorrentHave");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentHave(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_HAVE_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentHave(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentRequest method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentRequest() {
    LOGGER.info("isBitTorrentRequest");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentRequest(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_REQUEST_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentRequest(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentCancel method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentCancel() {
    LOGGER.info("isBitTorrentCancel");
    int magic1 = 0;
    int magic2 = 0;
    int magic3 = 0;
    int magic4 = 0;
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentCancel(magic1, magic2, magic3, magic4, magic5));
    magic1 = 0;
    magic2 = 0;
    magic3 = 0;
    magic4 = BitTorrentConstants.BIT_TORRENT_CANCEL_MESSAGE_LENGTH;
    magic5 = BitTorrentConstants.BIT_TORRENT_CANCEL_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentCancel(magic1, magic2, magic3, magic4, magic5));
  }

  /**
   * Test of isBitTorrentBitfield method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentBitfield() {
    LOGGER.info("isBitTorrentBitfield");
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentBitfield(magic5));
    magic5 = BitTorrentConstants.BIT_TORRENT_BITFIELD_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentBitfield(magic5));
  }

  /**
   * Test of isBitTorrentPiece method, of class BitTorrentUtils.
   */
  @Test
  public void testIsBitTorrentPiece() {
    LOGGER.info("isBitTorrentPiece");
    int magic5 = 0;
    assertFalse(BitTorrentUtils.isBitTorrentPiece(magic5));
    magic5 = BitTorrentConstants.BIT_TORRENT_PIECE_MESSAGE_ID;
    assertTrue(BitTorrentUtils.isBitTorrentPiece(magic5));
  }

  /**
   * Test of generateRandomPeerIdBytes method, of class BitTorrentUtils.
   */
  @Test
  public void testGenerateRandomPeerIdBytes() {
    LOGGER.info("generateRandomPeerIdBytes");
    byte[] peerIdBytes = BitTorrentUtils.generateRandomPeerIdBytes();
    LOGGER.info("random peer id bytes: " + ByteUtils.toHex(peerIdBytes));
    peerIdBytes = BitTorrentUtils.generateRandomPeerIdBytes();
    LOGGER.info("random peer id bytes: " + ByteUtils.toHex(peerIdBytes));
    assertEquals('-', peerIdBytes[0]);
    assertEquals('S', peerIdBytes[1]);
    assertEquals('N', peerIdBytes[2]);
    assertEquals('1', peerIdBytes[3]);
    assertEquals('0', peerIdBytes[4]);
    assertEquals('0', peerIdBytes[5]);
    assertEquals('0', peerIdBytes[6]);
    assertEquals('-', peerIdBytes[7]);
  }
}
