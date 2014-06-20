/*
 * BitTorrentPieceMessageTest.java
 *
 * Created on Jun 30, 2008, 2:29:03 PM
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
public class BitTorrentPieceMessageTest {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(BitTorrentPieceMessageTest.class);
  /** the peer identification bytes */
  private static final byte[] PEER_ID_BYTES = {
    0x41, 0x42, 0x43, 0x44,
    0x45, 0x46, 0x47, 0x48,
    0x49, 0x4A, 0x4B, 0x4C,
    0x4D, 0x4E, 0x4F, 0x50,
    0x51, 0x52, 0x53, 0x54
  };

  public BitTorrentPieceMessageTest() {
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
   * Test of toString method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    assertEquals("[piece: 8, offset: 9, length: 6, peer id: ABCDEFGHIJKLMNOPQRST]", instance.toString());
  }

  /**
   * Test of getPieceIndex method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testGetPieceIndex() {
    LOGGER.info("getPieceIndex");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    assertEquals(8, instance.getPieceIndex());
  }

  /**
   * Test of getOffset method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testGetOffset() {
    LOGGER.info("getOffset");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    assertEquals(9, instance.getOffset());
  }

  /**
   * Test of getChunkBytes method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testGetChunkBytes() {
    LOGGER.info("getChunkBytes");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    assertTrue(ByteUtils.areEqual(dataBlock, instance.getChunkBytes()));
  }

  /**
   * Test of encode method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testEncode() {
    LOGGER.info("encode");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    byte[] result = instance.encode();
    assertEquals("000000230700000008000000090102030405064142434445464748494a4b4c4d4e4f5051525354", ByteUtils.toHex(result));
    assertTrue(BitTorrentUtils.isBitTorrentPiece(result[4]));
    assertEquals(39, result.length);
    final int messageLength = ByteUtils.byteArrayToInt(result, 0);
    assertEquals(result.length, messageLength + 4);
  }

  /**
   * Test of decode method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testDecode() {
    LOGGER.info("decode");
    final byte[] bytes = {
      0x00, 0x00, 0x00, 0x23,
      0x07, 0x00, 0x00, 0x00,
      0x08, 0x00, 0x00, 0x00,
      0x09, 0x01, 0x02, 0x03,
      0x04, 0x05, 0x06,
      0x41, 0x42, 0x43, 0x44,
      0x45, 0x46, 0x47, 0x48,
      0x49, 0x4A, 0x4B, 0x4C,
      0x4D, 0x4E, 0x4F, 0x50,
      0x51, 0x52, 0x53, 0x54
    };
    BitTorrentPieceMessage instance = BitTorrentPieceMessage.decode(bytes);
    assertEquals("[piece: 8, offset: 9, length: 6, peer id: ABCDEFGHIJKLMNOPQRST]", instance.toString());
    byte[] serializedBytes = instance.encode();
    assertTrue(ByteUtils.areEqual(bytes, serializedBytes));
  }

  /**
   * Test of getBitField method, of class BitTorrentPieceMessage.
   */
  @Test
  public void testGetPeerIdBytes() {
    LOGGER.info("getPeerIdBytes");
    final byte[] dataBlock = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06};
    BitTorrentPieceMessage instance = new BitTorrentPieceMessage(
            8, // pieceIndex
            9, // offset
            dataBlock,
            PEER_ID_BYTES);
    byte[] result = instance.getPeerIdBytes();
    assertTrue(ByteUtils.areEqual(PEER_ID_BYTES, result));
  }
}
