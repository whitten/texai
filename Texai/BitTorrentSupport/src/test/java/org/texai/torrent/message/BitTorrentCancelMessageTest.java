/*
 * BitTorrentCancelMessageTest.java
 *
 * Created on Jun 30, 2008, 2:44:03 PM
 *
 * Description: .
 *
 * Copyright (C) Feb 10, 2010 reed.
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
package org.texai.torrent.message;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.torrent.support.BitTorrentUtils;
import org.texai.util.ByteUtils;
import static org.junit.Assert.*;

/**
 *
 * @author reed
 */
public class BitTorrentCancelMessageTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentCancelMessageTest.class);
  /** the peer identification bytes */
  private static final byte[] PEER_ID_BYTES = {
    0x41, 0x42, 0x43, 0x44,
    0x45, 0x46, 0x47, 0x48,
    0x49, 0x4A, 0x4B, 0x4C,
    0x4D, 0x4E, 0x4F, 0x50,
    0x51, 0x52, 0x53, 0x54
  };

  public BitTorrentCancelMessageTest() {
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
   * Test of toString method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    assertEquals("[cancel piece: 5, offset: 3, length: 7, peer id: ABCDEFGHIJKLMNOPQRST]", instance.toString());
  }

  /**
   * Test of encode method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testEncode() {
    LOGGER.info("encode");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    byte[] result = instance.encode();
    assertEquals("00000021070000000500000003000000074142434445464748494a4b4c4d4e4f5051525354", ByteUtils.toHex(result));
    assertTrue(BitTorrentUtils.isBitTorrentCancel(result[0], result[1], result[2], result[3], result[4]));
  }

  /**
   * Test of getPieceIndex method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testGetPieceIndex() {
    LOGGER.info("getPieceIndex");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    assertEquals(5, instance.getPieceIndex());
  }

  /**
   * Test of getOffset method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testGetOffset() {
    LOGGER.info("getOffset");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    assertEquals(3, instance.getOffset());
  }

  /**
   * Test of getLength method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testGetLength() {
    LOGGER.info("getLength");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    assertEquals(7, instance.getLength());
  }

  /**
   * Test of getPeerIdBytes method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testGetPeerIdBytes() {
    LOGGER.info("getPeerIdBytes");
    BitTorrentCancelMessage instance = new BitTorrentCancelMessage(
            5, // pieceIndex
            3, // offset
            7, // length
            PEER_ID_BYTES);
    assertTrue(ByteUtils.areEqual(PEER_ID_BYTES, instance.getPeerIdBytes()));
  }

  /**
   * Test of decode method, of class BitTorrentCancelMessage.
   */
  @Test
  public void testDecode() {
    LOGGER.info("decode");
    final byte[] bytes = {
      0x00, 0x00, 0x00, 0x21,
      0x07, 0x00, 0x00, 0x00,
      0x05, 0x00, 0x00, 0x00,
      0x03, 0x00, 0x00, 0x00,
      0x07,
      0x41, 0x42, 0x43, 0x44,
      0x45, 0x46, 0x47, 0x48,
      0x49, 0x4A, 0x4B, 0x4C,
      0x4D, 0x4E, 0x4F, 0x50,
      0x51, 0x52, 0x53, 0x54
    };
    BitTorrentCancelMessage instance = BitTorrentCancelMessage.decode(bytes);
    assertEquals("[cancel piece: 5, offset: 3, length: 7, peer id: ABCDEFGHIJKLMNOPQRST]", instance.toString());
    byte[] serializedBytes = instance.encode();
    assertTrue(ByteUtils.areEqual(bytes, serializedBytes));
  }
}
