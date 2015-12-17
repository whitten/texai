package org.texai.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Logger;

/**
 * ZipUtils.java
 *
 * Description: Provides compression and decompression utility methods.
 *
 * Copyright (C) Jan 21, 2015, Stephen L. Reed.
 */
public class ZipUtils {

  // the logger
  private static final Logger LOGGER = Logger.getLogger(ZipUtils.class);

  /**
   * Prevents instantiation of this utility class.
   */
  private ZipUtils() {
  }

  /**
   * Archives the files in the given directory into a zip-format byte array.
   *
   * @param directory the given directory
   *
   * @return a byte array in ZIP archive format
   */
  public static byte[] archiveFilesToByteArray(final File directory) {
    //Preconditions
    assert directory != null : "directory must not be null";
    assert directory.isDirectory() : "directory must be a directory";

    LOGGER.info("creating ziped byte array from files in directory " + directory);
    final int bufferSize = 4096;
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(byteArrayOutputStream))) {
        byte data[] = new byte[bufferSize];
        for (final File file : directory.listFiles()) {
          LOGGER.info("  adding: " + file.getName());
          try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file), bufferSize)) {
            ZipEntry entry = new ZipEntry(file.getName());
            zipOutputStream.putNextEntry(entry);
            int count;
            while ((count = bufferedInputStream.read(data, 0, bufferSize)) != -1) {
              zipOutputStream.write(data, 0, count);
            }
          }
        }
      }
      return byteArrayOutputStream.toByteArray();
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns a temporary ZipFile from the given zip-format byte array.
   *
   * @param bytes a byte array in ZIP archive format
   *
   * @return a temporary ZipFile
   */
  public static ZipFile temporaryZipFile(final byte[] bytes) {
    //Preconditions
    assert bytes != null : "bytes must not be null";

    LOGGER.info("bytes length: " + bytes.length);
    try {
      final File zipFile = File.createTempFile("archive", ".zip");
      try (FileOutputStream fileOuputStream = new FileOutputStream(zipFile)) {
        fileOuputStream.write(bytes);
      }
      assert zipFile.exists();
      assert zipFile.isFile();
      LOGGER.info("zipFile: " + zipFile);

      return new ZipFile(zipFile);
    } catch (IOException ex) {
      throw new TexaiException(ex);
    }
  }

  /**
   * Returns true if the given zip archive file is not corrupted, with regard to reading its catelog and reading its entries.
   *
   * @param zipFilePathName the path to the given zip archive file
   *
   * @return true if the given zip archive file is not corrupted
   */
  public static boolean verify(final String zipFilePathName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(zipFilePathName) : "zipFilePathName must be a non-empty string";
    assert (new File(zipFilePathName)).exists() : zipFilePathName + " must exist";
    
    ZipFile zipFile = null;
    int zipEntryCnt = 0;
    int zipEntryActualCnt = 0;
    try {
      zipFile = new ZipFile(zipFilePathName);
      zipEntryCnt = zipFile.size();

      final Enumeration<? extends ZipEntry> zipFile_enum = zipFile.entries();
      final byte[] buffer = new byte[4096];
      while (zipFile_enum.hasMoreElements()) {
        final ZipEntry zipEntry = zipFile_enum.nextElement();
        zipEntryActualCnt++;
        // read the contents of the zip file entry
        final InputStream inputStream = zipFile.getInputStream(zipEntry);
        while (true) {
          final int bytesRead = inputStream.read(buffer);
          if (bytesRead == -1) {
            inputStream.close();
            break;
          }
        }
      }
    } catch (IOException e) {
      zipEntryCnt = -1;
      LOGGER.info(StringUtils.getStackTraceAsString(e));
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (IOException ex) {
          throw new TexaiException(ex);
        }
      }
    }
    if (zipEntryCnt > 0 && zipEntryCnt != zipEntryActualCnt) {
      LOGGER.info("Expected " + zipEntryCnt + " zip entries, but found " + zipEntryActualCnt);
      return false;
    } else {
      return zipEntryCnt > 0;
    }
  }

}
