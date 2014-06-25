/*
 * Storage - Class used to store and retrieve pieces. Copyright (C) 2003 Mark J.
 * Wielaard
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
package org.texai.torrent;

import org.texai.torrent.domainEntity.MetaInfo;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**  Maintains file pieces on disk. Can be used to store and retrieve pieces. */
public final class Storage {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(Storage.class);
  /** the metainfo */
  private MetaInfo metaInfo;
  /** the file lengths */
  private long[] randomAccessFileLengths;
  /** the random access files */
  private RandomAccessFile[] randomAccessFiles;
  /** the file names */
  private String[] fileNames;
  /** the bitfield */
  private final BitField bitField;
  /** the number of needed pieces */
  private int nbrNeededPieces;
  // XXX - Not always set correctly
  /** the piece size */
  private int pieceSize;
  /** the number of pieces */
  private int nbrPieces;
  /** the default piece size */
  private static final int MIN_PIECE_SIZE = 256 * 1024;
  /** the maximum number of pieces in a torrent */
  private static final long MAX_PIECES = 100 * 1024 / 20;
  /**  */
  private final String downloadDirectoryPath;

  /** Creates a new storage based on the supplied MetaInfo. This will try to
   * createPieceHashes and/or check all needed files in the MetaInfo.
   *
   * @param metaInfo the torrent metainfo
   * @param downloadDirectoryPath the download directory, which when empty indicates the current working directory
   * @throws IOException when an input/output error occurs
   */
  public Storage(
          final MetaInfo metaInfo,
          final String downloadDirectoryPath) throws IOException {
    //Preconditions
    assert metaInfo != null : "metaInfo must not be null";
    assert downloadDirectoryPath != null : "downloadDirectoryPath must not be null";

    this.metaInfo = metaInfo;
    this.downloadDirectoryPath = downloadDirectoryPath;
    nbrNeededPieces = metaInfo.getNbrPieces();
    bitField = new BitField(nbrNeededPieces);
    createAndCheckFiles();
  }

  /** Creates a storage from the existing file or directory together with an
   * appropriate MetaInfo file as can be announced on the given announce
   * String location.
   *
   * @param baseFile the base file
   * @param announceURLString the announce URL string
   * @param areHiddenFilesExcluded the indicator whether hidden files are excluded
   * @throws IOException when an input/output error occurs
   */
  public Storage(
          final File baseFile,
          final String announceURLString,
          final boolean areHiddenFilesExcluded) throws IOException {
    //Preconditions
    assert baseFile != null : "baseFile must not be null";
    assert announceURLString != null : "announceURLString must not be null";
    assert !announceURLString.isEmpty() : "announceURLString must not be empty";

    this.downloadDirectoryPath = null;
    // Create names, random access files, and lengths arrays.
    getFiles(baseFile, areHiddenFilesExcluded);

    long total = 0;
    final ArrayList<Long> lengthsList = new ArrayList<>();
    for (long length : randomAccessFileLengths) {
      total += length;
      lengthsList.add(length);
    }

    pieceSize = MIN_PIECE_SIZE;
    nbrPieces = (int) ((total - 1) / pieceSize) + 1;
    while (nbrPieces > MAX_PIECES) {
      pieceSize = pieceSize * 2;
      nbrPieces = (int) ((total - 1) / pieceSize) + 1;
    }

    // Note that piece_hashes and the bitfield will be filled after
    // the MetaInfo is created.
    final byte[] piece_hashes = new byte[20 * nbrPieces];
    bitField = new BitField(nbrPieces);
    nbrNeededPieces = 0;

    final List<List<String>> files = new ArrayList<>();
    for (String element : fileNames) {
      final List<String> file = new ArrayList<>();
      final StringTokenizer stringTokenizer = new StringTokenizer(element, File.separator);
      while (stringTokenizer.hasMoreTokens()) {
        final String part = stringTokenizer.nextToken();
        file.add(part);
      }
      files.add(file);
    }

    // Note that the piece_hashes are not correctly setup yet.
    if (files.size() == 1) {
      metaInfo = new MetaInfo(
              announceURLString,
              baseFile.getName(),
              pieceSize,
              piece_hashes,
              total);
    } else {
      metaInfo = new MetaInfo(
              announceURLString,
              baseFile.getName(),
              files,
              lengthsList,
              pieceSize,
              piece_hashes,
              total);
    }
  }

  /** Creates piece hashes for a new storage.
   *
   * @throws IOException when an input/output error occurs
   */
  public void createPieceHashes() throws IOException {
    // Calculate piece_hashes
    MessageDigest digest = null;
    try {
      digest = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException nsa) {
      throw new InternalError(nsa.toString());    // NOPMD
    }

    final byte[] piece_hashes = metaInfo.getPieceHashes();

    final byte[] pieceBuffer = new byte[pieceSize];
    // i pieces
    for (int i = 0; i < nbrPieces; i++) {
      final int length = getUncheckedPiece(
              i, // the piece sequence number
              pieceBuffer, // the given buffer into which the piece is stored
              0); // the offest
      digest.update(
              pieceBuffer, // the array of bytes to hash
              0, // offset
              length); // length
      final byte[] piece_hash = digest.digest();
      // 20 bytes in a particular piece hash
      System.arraycopy(piece_hash, 0, piece_hashes, 20 * i, 20);

      bitField.set(i);
    }

    // Reannounce to force recalculating the info_hash.
    metaInfo = metaInfo.reannounce(metaInfo.getAnnounceURLString());
  }

  /** Gets the files.
   *
   * @param base the base file
   * @param areHiddenFilesExcluded the indicator whether hidden files are excluded
   * @throws IOException when an input/output error occurs
   */
  private void getFiles(
          final File base,
          final boolean areHiddenFilesExcluded) throws IOException {
    final ArrayList<File> files = new ArrayList<>();
    addFiles(files, base, areHiddenFilesExcluded);

    Collections.sort(files, new FilePathComparator());

    final int size = files.size();
    fileNames = new String[size];
    randomAccessFileLengths = new long[size];
    randomAccessFiles = new RandomAccessFile[size];

    int index = 0;
    final Iterator<File> files_iter = files.iterator();
    while (files_iter.hasNext()) {
      final File file = files_iter.next();
      fileNames[index] = file.getPath();
      randomAccessFileLengths[index] = file.length();
      randomAccessFiles[index] = new RandomAccessFile(file, "r");
      index++;
    }
  }

  /** Recursively adds the given file or directory.
   *
   * @param files1 the provided list into which the files are added
   * @param file the file or directory to add
   * @param areHiddenFilesExcluded the indicator whether hidden files are excluded
   */
  private static void addFiles(
          final List<File> files1,
          final File file,
          final boolean areHiddenFilesExcluded) {
    if (areHiddenFilesExcluded && file.getName().charAt(0) == '.') {
      return;
    } else if (file.isDirectory()) {
      final File[] files = file.listFiles();
      if (files == null) {
        LOGGER.log(Level.WARN, "skipping '" + file + "' not a normal file");
        return;
      }
      for (File element : files) {
        addFiles(files1, element, areHiddenFilesExcluded);
      }
    } else {
      files1.add(file);
    }
  }

  /** Returns the MetaInfo associated with this Storage.
   *
   * @return the torrent metainfo
   */
  public MetaInfo getMetaInfo() {
    return metaInfo;
  }

  /** Returns how many pieces are still missing from this storage.
   *
   * @return how many pieces are still missing from this storage
   */
  public int getNbrNeededPieces() {
    return nbrNeededPieces;
  }

  /** Returns whether or not this storage contains all pieces in the MetaInfo.
   *
   * @return whether or not this storage contains all pieces in the MetaInfo
   */
  public boolean isComplete() {
    return nbrNeededPieces == 0;
  }

  /** Returns the BitField that tells which pieces this storage contains. Do not change
   * this since this is the current state of the storage.
   *
   * @return the BitField that tells which pieces this storage contains
   */
  public BitField getBitField() {
    return bitField;
  }

  /** Creates new files from the metainfo file list when needed, and then checks them.
   *
   * @throws IOException when an input/output error occurs
   */
  @SuppressWarnings("UnnecessaryContinue")
  private void createAndCheckFiles() throws IOException {
    final String basePath;
    if (downloadDirectoryPath.endsWith(System.getProperty("file.separator"))) {
      basePath = downloadDirectoryPath + filterName(metaInfo.getName());
    } else {
      basePath = downloadDirectoryPath + "/" + filterName(metaInfo.getName());
    }
    final File base = new File(basePath);

    final List<List<String>> files = metaInfo.getFiles();
    if (files == null) {
      // Create base as file.
      LOGGER.info("creating/Checking file: " + base);
      if (!base.createNewFile() && !base.exists()) {
        throw new IOException("could not create file " + base);
      }

      randomAccessFileLengths = new long[1];
      randomAccessFiles = new RandomAccessFile[1];
      fileNames = new String[1];
      randomAccessFileLengths[0] = metaInfo.getTotalLength();
      randomAccessFiles[0] = new RandomAccessFile(base, "rw");
      fileNames[0] = base.getName();
    } else {
      // Create base as dir.
      LOGGER.info("creating/Checking directory: " + base);
      if (!base.mkdir() && !base.isDirectory()) {
        throw new IOException("could not create directory " + base);
      }

      final List<Long> fileLengths = metaInfo.getLengths();
      final int size = files.size();
      long total = 0;
      randomAccessFileLengths = new long[size];
      randomAccessFiles = new RandomAccessFile[size];
      fileNames = new String[size];
      for (int i = 0; i < size; i++) {
        @SuppressWarnings("unchecked")
        final File file = createFileFromNames(base, files.get(i));
        randomAccessFileLengths[i] = (fileLengths.get(i));
        total += randomAccessFileLengths[i];
        randomAccessFiles[i] = new RandomAccessFile(file, "rw");
        fileNames[i] = file.getName();
      }

      // Sanity check for metainfo file.
      final long metalength = metaInfo.getTotalLength();
      if (total != metalength) {
        throw new IOException("file lengths do not add up " + total + " != " + metalength);
      }
    }

    // Make sure all files are available and of correct length
    for (int i = 0; i < randomAccessFiles.length; i++) {
      final long length = randomAccessFiles[i].length();
      if (length == randomAccessFileLengths[i]) {
        continue;
      } else if (length == 0) {
        allocateFile(i);
      } else {
        LOGGER.log(Level.DEBUG, "truncating '" + fileNames[i] + "' from " + length + " to " + randomAccessFileLengths[i] + "bytes");
        randomAccessFiles[i].setLength(randomAccessFileLengths[i]);
        allocateFile(i);
      }
    }

    // Check which pieces match and which don't
    nbrPieces = metaInfo.getNbrPieces();
    final byte[] pieceBytes = new byte[metaInfo.getPieceLength(0)];
    for (int pieceIndex = 0; pieceIndex < nbrPieces; pieceIndex++) {
      final int length = getUncheckedPiece(pieceIndex, pieceBytes, 0);
      final boolean isCorrentHash = metaInfo.checkPiece(pieceIndex, pieceBytes, 0, length);
      if (isCorrentHash) {
        bitField.set(pieceIndex);
        nbrNeededPieces--;
      }
    }
  }

  /** Removes 'suspicious' characters from the give file name.
   *
   * @param fileName the given file name
   * @return the filtered file name
   */
  private String filterName(final String fileName) {
    return fileName.replace(File.separatorChar, '_');
  }

  /** Creates a file from the given names.
   *
   * @param directory the initial base directory
   * @param names the file names in which all but the last name indicate the directory hierarchy and in which the
   * last name is the file
   * @return a file from the given names
   * @throws IOException when an input/output error occurs
   */
  private File createFileFromNames(
          final File directory,
          final List<String> names) throws IOException {
    File baseDirectory = directory;
    File file = null;
    final Iterator<String> names_iter = names.iterator();
    while (names_iter.hasNext()) {
      final String name = filterName(names_iter.next());
      if (names_iter.hasNext()) {
        // Another dir in the hierarchy.
        file = new File(baseDirectory, name);
        if (!file.mkdir() && !file.isDirectory()) {
          throw new IOException("could not create directory " + file);
        }
        baseDirectory = file;
      } else {
        // The final element (file) in the hierarchy.
        file = new File(baseDirectory, name);
        if (!file.createNewFile() && !file.exists()) {
          throw new IOException("could not create file " + file);
        }
      }
    }
    return file;
  }

  /** Allocates the file.
   *
   * @param fileIndex the file index
   * @throws IOException when an input/output error occurs
   */
  private void allocateFile(final int fileIndex) throws IOException {
    // XXX - Is this the best way to make sure we have enough space for
    // the whole file?
    final int zeroBlockSize = metaInfo.getPieceLength(0);
    final byte[] zeros = new byte[zeroBlockSize];
    int index;
    for (index = 0; index < randomAccessFileLengths[fileIndex] / zeroBlockSize; index++) {
      randomAccessFiles[fileIndex].write(zeros);
    }
    final int size = (int) (randomAccessFileLengths[fileIndex] - index * zeroBlockSize);
    randomAccessFiles[fileIndex].write(zeros, 0, size);
  }

  /** Closes the Storage and makes sure that all RandomAccessFiles are closed.
   * The Storage is unusable after this.
   *
   * @throws IOException when an input/output error occurs
   */
  public void close() throws IOException {
    for (RandomAccessFile element : randomAccessFiles) {
      synchronized (element) {
        element.close();
      }
    }
  }

  /**  Returns a byte array containing the requested piece or null if the
   * storage doesn't contain the piece yet.
   *
   * @param pieceIndex the piece index
   * @return a byte array containing the requested piece or null if the
   * storage doesn't contain the piece yet
   * @throws IOException when an input/output error occurs
   */
  public byte[] getPiece(final int pieceIndex) throws IOException {
    if (!bitField.get(pieceIndex)) {
      return null;
    }

    final byte[] pieceBuffer = new byte[metaInfo.getPieceLength(pieceIndex)];
    getUncheckedPiece(pieceIndex, pieceBuffer, 0);
    return pieceBuffer;
  }

  /** Puts the piece in the Storage if it is correct.
   *
   * @param pieceIndex the piece index
   * @param pieceBuffer the given buffer into which the piece is stored
   * @return true if the piece was correct (sha metainfo hash matches), otherwise false.
   * @exception IOException when some storage related error occurs.
   */
  public boolean putPiece(
          final int pieceIndex,
          final byte[] pieceBuffer) throws IOException {
    //Preconditions
    assert pieceIndex >= 0 : "pieceIndex must not be negative";
    assert pieceBuffer != null : "pieceBuffer must not be null";
    assert pieceBuffer.length > 0 : "pieceBuffer must not be empty";
    assert randomAccessFileLengths != null : "randomAccessFileLengths must not be null";

    final int length = pieceBuffer.length;
    // check piece against its expected hash
    final boolean isCorrentHash = metaInfo.checkPiece(
            pieceIndex,
            pieceBuffer,
            0, // offset
            length);
    if (!isCorrentHash) {
      return false;
    }

    synchronized (bitField) {
      if (bitField.get(pieceIndex)) {
        LOGGER.info("bit field has piece already present");
        return true; // No need to store twice.
      } else {
        bitField.set(pieceIndex);
        nbrNeededPieces--;
      }
    }

    long start = (long) pieceIndex * (long) metaInfo.getPieceLength(0);
    int index = 0;
    long randomAccessFileLength = randomAccessFileLengths[index];
    while (start > randomAccessFileLength) {
      index++;
      start -= randomAccessFileLength;
      randomAccessFileLength = randomAccessFileLengths[index];
    }

    int nbrBytesWritten = 0;
    final int offset = 0;
    while (nbrBytesWritten < length) {
      final int need = length - nbrBytesWritten;
      final int nbrBytesToWrite = (start + need < randomAccessFileLength) ? need : (int) (randomAccessFileLength - start);
      synchronized (randomAccessFiles[index]) {
        randomAccessFiles[index].seek(start);
        randomAccessFiles[index].write(
                pieceBuffer, // the data
                offset + nbrBytesWritten, // the start offset in the data
                nbrBytesToWrite); // the number of bytes to write
      }
      nbrBytesWritten += nbrBytesToWrite;
      if (need - nbrBytesToWrite > 0) {
        index++;
        randomAccessFileLength = randomAccessFileLengths[index];
        start = 0;
      }
    }

    return true;
  }

  /** Gets the unchecked piece into the given buffer.
   *
   * @param pieceIndex the piece index
   * @param pieceBuffer the given buffer into which the piece is stored
   * @param offset the offest
   * @return the length of the piece
   * @throws IOException when an input/output error occurs
   */
  private int getUncheckedPiece(
          final int pieceIndex,
          final byte[] pieceBuffer,
          final int offset)
          throws IOException {
    long start = (long) pieceIndex * (long) metaInfo.getPieceLength(0);
    final int pieceLength = metaInfo.getPieceLength(pieceIndex);
    int index = 0;
    long randomAccessFileLength = randomAccessFileLengths[index];
    while (start > randomAccessFileLength) {
      index++;
      start -= randomAccessFileLength;
      randomAccessFileLength = randomAccessFileLengths[index];
    }

    int nbrBytesRead = 0;
    while (nbrBytesRead < pieceLength) {
      final int need = pieceLength - nbrBytesRead;
      final int nbrBytesToRead = (start + need < randomAccessFileLength) ? need : (int) (randomAccessFileLength - start);
      synchronized (randomAccessFiles[index]) {
        randomAccessFiles[index].seek(start);
        randomAccessFiles[index].readFully(
                pieceBuffer, // the buffer into which the data is read
                offset + nbrBytesRead, // the start offset of the data
                nbrBytesToRead); // the number of bytes to read
      }
      nbrBytesRead += nbrBytesToRead;
      if (need - nbrBytesToRead > 0) {
        index++;
        randomAccessFileLength = randomAccessFileLengths[index];
        start = 0;
      }
    }

    return pieceLength;
  }

  /** Provides a way to canonically arrange the files list. */
  private static final class FilePathComparator implements Comparator<File> {

    /** Compares the two given files.
     *
     * @param file1 the first file
     * @param file2 the second file
     * @return -1 if the path name of the first file is less than the path name of the second file, 0 if they are equal, otherwise
     * return +1
     */
    @Override
    public int compare(final File file1, final File file2) {
      return file1.toString().compareTo(file2.toString());
    }
  }
}
