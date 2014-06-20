/*
 * BitTorrentHandshakeMessageTest.java
 *
 * Created on Jun 30, 2008, 11:21:42 PM
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
public class BitTorrentHandshakeMessageTest {

  /** the logger */
  private final static Logger LOGGER = Logger.getLogger(BitTorrentHandshakeMessageTest.class);

  public BitTorrentHandshakeMessageTest() {
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
   * Test of getInfoHash method, of class BitTorrentHandshakeMessage.
   */
  @Test
  public void testGetInfoHash() {
    LOGGER.info("getInfoHash");
    final byte[] infoHash = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};
    final byte[] peerIdBytes = {0x14, 0x13, 0x12, 0x11, 0x10, 0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01};
    BitTorrentHandshakeMessage instance = new BitTorrentHandshakeMessage(infoHash, peerIdBytes);
    assertTrue(ByteUtils.areEqual(infoHash, instance.getInfoHash()));
    assertFalse(ByteUtils.areEqual(peerIdBytes, instance.getInfoHash()));
  }

  /**
   * Test of getPeerIdBytes method, of class BitTorrentHandshakeMessage.
   */
  @Test
  public void testGetPeerIdBytes() {
    LOGGER.info("getPeerIdBytes");
    final byte[] infoHash = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};
    final byte[] peerIdBytes = {0x14, 0x13, 0x12, 0x11, 0x10, 0x0F, 0x0E, 0x0D, 0x0C, 0x0B, 0x0A, 0x09, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01};
    BitTorrentHandshakeMessage instance = new BitTorrentHandshakeMessage(infoHash, peerIdBytes);
    assertFalse(ByteUtils.areEqual(infoHash, instance.getPeerIdBytes()));
    assertTrue(ByteUtils.areEqual(peerIdBytes, instance.getPeerIdBytes()));
  }

  /**
   * Test of toString method, of class BitTorrentHandshakeMessage.
   */
  @Test
  public void testToString() {
    LOGGER.info("toString");
    final byte[] infoHash = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};
    final byte[] peerIdBytes = {0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54};
    BitTorrentHandshakeMessage instance = new BitTorrentHandshakeMessage(infoHash, peerIdBytes);
    assertEquals("[handshake, peer id: ABCDEFGHIJKLMNOPQRST, info-hash: 0102030405060708090a0b0c0d0e0f1011121314]", instance.toString());
  }

  /**
   * Test of encode method, of class BitTorrentHandshakeMessage.
   */
  @Test
  public void testEncode() {
    LOGGER.info("encode");
    final byte[] infoHash = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14};
    final byte[] peerIdBytes = {0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50, 0x51, 0x52, 0x53, 0x54};
    BitTorrentHandshakeMessage instance = new BitTorrentHandshakeMessage(infoHash, peerIdBytes);
    final byte[] serializedHandshake = instance.encode();
    assertEquals("13426974546f7272656e742070726f746f636f6c00000000000000000102030405060708090a0b0c0d0e0f10111213144142434445464748494a4b4c4d4e4f5051525354", ByteUtils.toHex(serializedHandshake));
    assertTrue(BitTorrentUtils.isBitTorrentHandshake(serializedHandshake[0]));
  }

  /**
   * Test of decode method, of class BitTorrentHandshakeMessage.
   */
  @Test
  public void testDecode() {
    LOGGER.info("decode");
    final byte[] bytes = {
      0x13, 0x42, 0x69, 0x74, 0x54, 0x6f, 0x72, 0x72,
      0x65, 0x6e, 0x74, 0x20, 0x70, 0x72, 0x6f, 0x74,
      0x6f, 0x63, 0x6f, 0x6c, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04,
      0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c,
      0x0d, 0x0e, 0x0f, 0x10, 0x11, 0x12, 0x13, 0x14,
      0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48,
      0x49, 0x4A, 0x4B, 0x4C, 0x4D, 0x4E, 0x4F, 0x50,
      0x51, 0x52, 0x53, 0x54};

    BitTorrentHandshakeMessage instance = BitTorrentHandshakeMessage.decode(bytes);
    assertEquals("[handshake, peer id: ABCDEFGHIJKLMNOPQRST, info-hash: 0102030405060708090a0b0c0d0e0f1011121314]", instance.toString());
    final byte[] serializedHandshake = instance.encode();
    assertTrue(ByteUtils.areEqual(bytes, serializedHandshake));
  }
}
