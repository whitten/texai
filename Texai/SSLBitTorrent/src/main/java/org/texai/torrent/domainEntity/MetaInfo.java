/*
 * MetaInfo - Holds all information gotten from a torrent file. Copyright (C)
 * 2003 Mark J. Wielaard
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */
package org.texai.torrent.domainEntity;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.net.URLCodec;
import org.texai.torrent.bencode.BDecoder;
import org.texai.torrent.bencode.BEValue;
import org.texai.torrent.bencode.BEncoder;
import org.texai.torrent.bencode.InvalidBEncodingException;
import org.texai.util.TexaiException;

/** Holds all information gotten from a torrent file. */
public final class MetaInfo {

  /** the string representing the URL of the tracker for this torrent */
  private final String announceURLString;
  /** the original 20 byte SHA1 hash over the bencoded info map */
  private final byte[] infoHash;
  /** the requested name for the file or toplevel directory */
  private final String name;
  /** a list of lists of file name hierarchies or null if it is a single name */
  private final List<List<String>> files;
  /** the list individual file sizes, or null if it is a single file */
  private final List<Long> lengths;
  /** the length of a piece, all pieces are of equal length except for the last one */
  private final int pieceLength;
  /** the piece hashes */
  private final byte[] pieceHashes;
  /** the file length */
  private final long length;
  /** the bencoded torrent metainfo data */
  private byte[] torrentData;

  /** Constructs a new MetaInfo instance for a single file torrent.
   *
   * @param announceURLString the string representing the URL of the tracker for this torrent
   * @param name the requested name for the file
   * @param pieceLength the length of a piece, all pieces are of equal length except for the last one
   * @param pieceHashes the piece hashes
   * @param length the file length
   */
  public MetaInfo(
          final String announceURLString,
          final String name,
          final int pieceLength,
          final byte[] pieceHashes,
          final long length) {
    this(
          announceURLString,
          name,
          null,  // files
          null,  // lengths
          pieceLength,
          pieceHashes,
          length);
  }

  /** Constructs a new MetaInfo instance for a multi-file torrent.
   *
   * @param announceURLString the string representing the URL of the tracker for this torrent
   * @param name the requested name for the toplevel directory
   * @param files a list of lists of file name hierarchies
   * @param lengths the list individual file sizes
   * @param pieceLength the length of a piece, all pieces are of equal length except for the last one
   * @param pieceHashes the piece hashes
   * @param length the file length
   */
  public MetaInfo(
          final String announceURLString,
          final String name,
          final List<List<String>> files,
          final List<Long> lengths,
          final int pieceLength,
          final byte[] pieceHashes,
          final long length) {
    //Preconditions
    assert announceURLString != null : "announceURLString must not be null";
    assert !announceURLString.isEmpty() : "announceURLString must not be empty";
    assert name != null : "name must not be null";
    assert !name.isEmpty() : "name must not be empty";
    assert pieceHashes != null : "pieceHashes must not be null";
    assert length > 0 : "length must be positive";

    this.announceURLString = announceURLString;
    this.name = name;
    this.files = files;
    this.lengths = lengths;
    this.pieceLength = pieceLength;
    this.pieceHashes = pieceHashes;
    this.length = length;

    this.infoHash = calculateInfoHash();
  }

  /** Creates a new MetaInfo from the given InputStream. The InputStream must
   * start with a correctly bencoded dictonary describing the torrent.
   *
   * @param inputStream the given InputStream
   * @throws IOException if an input/output error occurs
   */
  public MetaInfo(final InputStream inputStream) throws IOException {
    this(new BDecoder(inputStream));
  }

  /** Creates a new MetaInfo from the given BDecoder. The BDecoder must have a
   * complete dictionary describing the torrent.
   *
   * @param bDecoder the given BDecoder
   * @throws IOException if an input/output error occurs
   */
  @SuppressWarnings("unchecked")
  public MetaInfo(final BDecoder bDecoder) throws IOException {
    // Note that evaluation order matters here...
    this(bDecoder.bdecodeMap().getMap());
  }

  /** Creates a new MetaInfo from a Map of BEValues and the SHA1 over the
   * original bencoded info dictonary (this is a hack, we could reconstruct
   * the bencoded stream and recalculate the hash). Will throw a
   * InvalidBEncodingException if the given map does not contain a valid
   * announce string or info dictonary.
   *
   * @param map the map of BEValues
   * @throws InvalidBEncodingException if an error occurs
   */
  public MetaInfo(final Map<String, BEValue> map) throws InvalidBEncodingException {
    BEValue val = map.get("announce");
    if (val == null) {
      throw new InvalidBEncodingException("Missing announce string");
    }
    this.announceURLString = val.getString();

    val = map.get("info");
    if (val == null) {
      throw new InvalidBEncodingException("Missing info map");
    }
    @SuppressWarnings("unchecked")
    final Map<String, BEValue> info = val.getMap();

    val = info.get("name");
    if (val == null) {
      throw new InvalidBEncodingException("Missing name string");
    }
    name = val.getString();

    val = info.get("piece length");
    if (val == null) {
      throw new InvalidBEncodingException("Missing piece length number");
    }
    pieceLength = val.getInt();

    val = info.get("pieces");
    if (val == null) {
      throw new InvalidBEncodingException("Missing piece bytes");
    }
    pieceHashes = val.getBytes();

    val = info.get("length");
    if (val == null) {
      // Multi file case.
      val = info.get("files");
      if (val == null) {
        throw new InvalidBEncodingException(
                "Missing length number and/or files list");
      }

      final List<BEValue> list = val.getList();
      final int size = list.size();
      if (size == 0) {
        throw new InvalidBEncodingException("zero size files list");
      }

      files = new ArrayList<>(size);
      lengths = new ArrayList<>(size);
      long length1 = 0;
      for (BEValue list1 : list) {
        @SuppressWarnings(value = "unchecked")
        final Map<String, BEValue> desc = list1.getMap();
        val = desc.get("length");
        if (val == null) {
          throw new InvalidBEncodingException("Missing length number");
        }
        final long len = val.getLong();
        lengths.add(len);
        length1 += len;
        val = desc.get("path");
        if (val == null) {
          throw new InvalidBEncodingException("Missing path list");
        }
        final List<BEValue> path_list = val.getList();
        final int path_length = path_list.size();
        if (path_length == 0) {
          throw new InvalidBEncodingException(
                  "zero size file path list");
        }
        final List<String> file = new ArrayList<>(path_length);
        for (BEValue value : path_list) {
          file.add(value.getString());
        }
        files.add(file);
      }
      length = length1;
    } else {
      // Single file case.
      length = val.getLong();
      files = null;
      lengths = null;
    }

    infoHash = calculateInfoHash();
  }

  /** Gets the string representing the URL of the tracker for this torrent.
   *
   * @return the string representing the URL of the tracker for this torrent
   */
  public String getAnnounceURLString() {
    return announceURLString;
  }

  /** Returns the original 20 byte SHA1 hash over the bencoded info map.
   *
   * @return the original 20 byte SHA1 hash over the bencoded info map
   */
  public byte[] getInfoHash() {
    // XXX - Should we return a clone, just to be sure?
    return infoHash;
  }

  /** Gets the url encoded info hash.
   *
   * @return the url encoded info hash
   */
  public String getURLEncodedInfoHash() {
    return new String((new URLCodec()).encode(infoHash));
  }

  /** Returns the piece hashes. Only used by storage so package local.
   *
   * @return the piece hashes
   */
  public byte[] getPieceHashes() {
    return pieceHashes;
  }

  /** Returns the requested name for the file or toplevel directory. If it is a
   * toplevel directory name getFiles() will return a non-null List of file
   * name hierarchy name.
   *
   * @return the requested name for the file or toplevel directory
   */
  public String getName() {
    return name;
  }

  /** Returns a list of lists of file name hierarchies or null if it is a
   * single name. It has the same size as the list returned by getLengths().
   *
   * @return a list of lists of file name hierarchies or null if it is a
   * single name
   */
  public List<List<String>> getFiles() {
    return files;
  }

  /** Returns a list of Longs indication the size of the individual files, or
   * null if it is a single file. It has the same size as the list returned by
   * getFiles().
   *
   * @return the list of the individual file sizes, or null if a single file
   */
  public List<Long> getLengths() {
    // XXX - Immutable?
    return lengths;
  }

  /** Returns the number of pieces.
   *
   * @return the number of pieces
   */
  public int getNbrPieces() {
    return pieceHashes.length / 20;
  }

  /** Return the length of a piece. All pieces are of equal length except for
   * the last one (<code>getPieces()-1</code>).
   *
   * @param pieceIndex the piece index
   * @return the length of a piece
   */
  public int getPieceLength(final int pieceIndex) {
    final int nbrPieces = getNbrPieces();
    if (pieceIndex >= 0 && pieceIndex < nbrPieces - 1) {
      return pieceLength;
    } else if (pieceIndex == nbrPieces - 1) {
      return (int) (length - (long) pieceIndex * pieceLength);
    } else {
      throw new IndexOutOfBoundsException("no piece: " + pieceIndex);
    }
  }

  /** Checks that the given piece has the same SHA1 hash as the given byte
   * array. Returns random results or IndexOutOfBoundsExceptions when the
   * piece number is unknown.
   *
   * @param pieceIndex the piece index
   * @param pieceBuffer the array of bytes
   * @param offset the offset to start from in the array of bytes
   * @param length the number of bytes to use, starting at the offset
   * @return whether the given piece has the same SHA1 hash as the given byte array
   */
  public boolean checkPiece(
          final int pieceIndex,
          final byte[] pieceBuffer,
          final int offset,
          final int length) {
    // Check digest
    final MessageDigest sha1;
    try {
      sha1 = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException ex) {
      throw new TexaiException(ex);
    }

    sha1.update(pieceBuffer, offset, length);
    final byte[] hash = sha1.digest();
    for (int i = 0; i < 20; i++) {
      if (hash[i] != pieceHashes[20 * pieceIndex + i]) {
        return false;
      }
    }
    return true;
  }

  /** Gets the total length of the torrent in bytes.
   *
   * @return the total length of the torrent in bytes
   */
  public long getTotalLength() {
    return length;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "MetaInfo[info_hash='" +
            getURLEncodedInfoHash() +
            "', announce='" +
            announceURLString +
            "', name='" + name +
            "', files=" + files +
            ", lengths=" + lengths +
            ", #pieces='" +
            pieceHashes.length / 20 +
            "', piece_length='" + pieceLength +
            "', length='" +
            length +
            "']";
  }

  /** Encode a byte array as a hex encoded string.
   *
   * @param byteArray the byte array
   * @return a hex encoded string
   */
  private static String hexEncode(final byte[] byteArray) {
    final StringBuffer stringBuffer = new StringBuffer(byteArray.length * 2);
    for (byte element : byteArray) {
      final int hexDigit = element & 0xFF;
      if (hexDigit < 16) {
        stringBuffer.append('0');
      }
      stringBuffer.append(Integer.toHexString(hexDigit));
    }

    return stringBuffer.toString();
  }

  /** Creates a copy of this MetaInfo that shares everything except the announce URL.
   *
   * @param announce the string representing the URL of the tracker for this torrent
   * @return a copy of this MetaInfo that shares everything except the announce URL
   */
  public MetaInfo reannounce(final String announce) {
    return new MetaInfo(
            announce,
            name,
            files,
            lengths,
            pieceLength,
            pieceHashes,
            length);
  }

  /** Gets the bencoded torrent metainfo data.
   *
   * @return the bencoded torrent metainfo data
   */
  public byte[] getTorrentData() {
    if (torrentData == null) {
      final Map<String, Object> map = new HashMap<>();
      map.put("announce", announceURLString);
      final Map<String, Object> info = createInfoMap();
      map.put("info", info);
      System.out.println("torrent data: " + map);
      torrentData = BEncoder.bencode(map);
    }
    return torrentData;
  }

  /** Creates the info map.
   *
   * @return  the info map
   */
  private Map<String, Object> createInfoMap() {
    final Map<String, Object> info = new HashMap<>();
    info.put("name", name);
    info.put("piece length", pieceLength);
    info.put("pieces", pieceHashes);
    if (files == null) {
      info.put("length", length);
    } else {
      final List<Map<String, Object>> mapList = new ArrayList<>();
      for (int i = 0; i < files.size(); i++) {
        final Map<String, Object> fileMap = new HashMap<>();
        fileMap.put("path", files.get(i));
        fileMap.put("length", lengths.get(i));
        mapList.add(fileMap);
      }
      info.put("files", mapList);
    }
    return info;
  }

  /** Calculates the info hash.
   *
   * @return the info hash
   */
  private byte[] calculateInfoHash() {
    final Map<String, Object> info = createInfoMap();
    final byte[] infoBytes = BEncoder.bencode(info);
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA");
      return digest.digest(infoBytes);
    } catch (final NoSuchAlgorithmException nsa) {
      throw new InternalError(nsa.toString());    // NOPMD
    }
  }

  /** Returns the hex-encoded digest of the piece hashes.
   *
   * @return the hex-encoded digest of the piece hashes
   */
  public String getDigestedPieceHashes() {
    try {
      final MessageDigest digest = MessageDigest.getInstance("SHA");
      return hexEncode(digest.digest(pieceHashes));
    } catch (final NoSuchAlgorithmException nsa) {
      throw new InternalError(nsa.toString());    // NOPMD
    }
  }
}
